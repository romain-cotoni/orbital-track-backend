package space.satellite.services;

import jakarta.annotation.PostConstruct;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.ClasspathCrawler;
import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import org.orekit.data.NetworkCrawler;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.springframework.stereotype.Service;
import space.satellite.caches.CachedSatelliteData;
import space.satellite.caches.SatelliteKey;
import space.satellite.dtos.SatelliteRequestDto;
import space.satellite.dtos.SatelliteResponseDto;
import space.satellite.entities.Satellite;
import space.satellite.repositories.SatelliteRepository;

import static space.satellite.constants.Constants.OREKIT_DATA_CLASSPATH;
import static space.satellite.constants.Constants.OREKIT_DATA_DIRECTORY;
import static space.satellite.constants.Constants.OREKIT_DATA_URL;
import static space.satellite.constants.Constants.SPACETRACK_SOURCE;
import static space.satellite.constants.Constants.TLE_CACHE_DURATION;


@Service
@Slf4j
@Getter
@Setter
@RequiredArgsConstructor
public class SatellitePositionService implements PropagatorCacheService {

    private TleService tleService;
    
    private final TleServiceFactory tleServiceFactory;

    private final SatelliteRepository satelliteRepository;

    // Earth ellipsoid
    private BodyShape earth;

    // International Terrestrial Reference Frame
    private Frame itrf;

    // Collection of cached satellite
    private final Map<SatelliteKey, CachedSatelliteData> satelliteCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void initialize() {

        // 1. Set Data provider manager
        DataProvidersManager manager = DataContext.getDefault().getDataProvidersManager();

        // 2. Set Orekit data from directory
        setOrekitDataFromDirectory(manager);

        // 3. Init Earth Frame
        initEarthBodyShape();
    }


    private void initEarthBodyShape() {
        itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        earth = new OneAxisEllipsoid(6378137.0,        // Equatorial radius (meters)
                                      1.0 / 298.257223563, // Flattening (WGS84)
                                      itrf);               // Reference frame
    }


    public SatelliteResponseDto getPosition(SatelliteRequestDto request) {

        SatelliteResponseDto.SatelliteResponseDtoBuilder responseDto = SatelliteResponseDto.builder();

        // 1. Get TLE service (factory)
        tleService = tleServiceFactory.getTleService(SPACETRACK_SOURCE);

        // 2. Managed cached TLEs and TLEPropagators
        int catalogNumber = Integer.getInteger(request.getIdentifier());
        SatelliteKey key = SatelliteKey.norad(catalogNumber);
        CachedSatelliteData cached = satelliteCache.get(key);
        TLE tle;
        TLEPropagator propagator;
        if(cached != null && !cached.isExpired(TLE_CACHE_DURATION)) {
            tle = cached.tle();
            propagator = cached.tlePropagator();
            responseDto.isCached(true);
            log.info("Using cached TLE (age: {} min)", cached.getAgeMinutes());
        } else {
            // Fetch new TLE
            tle = tleService.fetchTle(catalogNumber);
            propagator = TLEPropagator.selectExtrapolator(tle);
            Instant fetchTime = Instant.now();
            // Store in cache
            satelliteCache.put(key, new CachedSatelliteData(tle, propagator, fetchTime));
            log.info("Fetched new TLE for satellite {} at {}", catalogNumber, fetchTime);
        }


        // 3. Get current time
        AbsoluteDate now = new AbsoluteDate(Instant.now(), TimeScalesFactory.getUTC());


        // 4. Get position in EME2000 (Earth Mean Equator 2000 [An inertial frame. Fixed relative to the stars])
        Frame eme2000           = FramesFactory.getEME2000();
        PVCoordinates pvEme2000 = propagator.getPVCoordinates(now, eme2000);


        // 5. Convert to ITRF (International Terrestrial Reference Frame [An Earth-fixed frame. Rotates with the Earth])
        Transform transform  = eme2000.getTransformTo(itrf, now);
        PVCoordinates pvItrf = transform.transformPVCoordinates(pvEme2000);


        // 6. Convert to Lat/Lon/Alt using Earth ellipsoid (WGS84)
        GeodeticPoint geoPoint = earth.transform(pvItrf.getPosition(), itrf, now);


        // 7. Extract Coordinates/Geographic Coordinates
        double latitude  = Math.toDegrees(geoPoint.getLatitude());
        double longitude = Math.toDegrees(geoPoint.getLongitude());
        double altitude  = geoPoint.getAltitude();
        double speed     = pvItrf.getVelocity().getNorm();


        // 8. Build response
        responseDto.name(request.getName());
        responseDto.identifier(request.getIdentifier());

        responseDto.tleEpoch(tle.getDate().toString(TimeScalesFactory.getUTC()));
        responseDto.tleLine1(tle.getLine1());
        responseDto.tleLine2(tle.getLine2());

        responseDto.geodeticLatitude(latitude);
        responseDto.geodeticLongitude(longitude);
        responseDto.geodeticAltitude(altitude);
        responseDto.geodeticSpeed(speed);

        responseDto.cartesianEme2000X(pvEme2000.getPosition().getX());
        responseDto.cartesianEme2000Y(pvEme2000.getPosition().getY());
        responseDto.cartesianEme2000Z(pvEme2000.getPosition().getZ());

        responseDto.cartesianItrfX(pvItrf.getPosition().getX());
        responseDto.cartesianItrfY(pvItrf.getPosition().getY());
        responseDto.cartesianItrfZ(pvItrf.getPosition().getZ());


        return responseDto.build();

    }




    /**
     * Set Orekit data from network (Gitlab)
     */
    private void setOrekitDataFromNetwork(DataProvidersManager manager) {
        // 1. SetUp Orekit url
        URL url = null;
        try {
            url = URI.create(OREKIT_DATA_URL).toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid url: " + OREKIT_DATA_URL, e);
        }

        // 2. Get Orekit data from network (Gitlab)
        manager.addProvider(new NetworkCrawler(url));
    }

    /**
     * Set Orekit data from local directory path
     */
    private void setOrekitDataFromDirectory(DataProvidersManager manager) {
        File orekitData = new File(OREKIT_DATA_DIRECTORY);
        manager.addProvider(new DirectoryCrawler(orekitData));
    }

    /**
     * Set Orekit data from classpath (search for local directory path)
     */
    private void setOrekitDataFromClasspath(DataProvidersManager manager) {
        manager.addProvider(new ClasspathCrawler(OREKIT_DATA_CLASSPATH));
    }




    /**
     * Retrieves the current position and velocity of a satellite using its TLE (Two-Line Element) data.
     * 1. Get the TLE service provider
     * 2. Steps for each satellite:
     *   1. Fetches or retrieves cached TLE (Two-Line Element) data.
     *   2. Uses a TLEPropagator (SGP4 algorithm) to propagate the orbit and calculate the satellite's position and velocity.
     *   3. Converts coordinates from the EME2000 inertial frame to ITRF (Earth-fixed)
     *   4. Converts coordinates from the ITRF (Earth-fixed) to geodetic (latitude/longitude/altitude).
     *   5. Returns a response DTO containing TLE metadata, Cartesian coordinates (EME2000/ITRF), and geodetic coordinates.
     *
     * @param satellites Satellite position request containing the satellite identifier and name.
     * @param tleProvider Name of TLE service provider
     * @return SatellitePositionResponseDto
     */
    public List<SatelliteResponseDto> getPositions(List<SatelliteRequestDto> satellites, String tleProvider) {
        List<SatelliteResponseDto> positions = new ArrayList<>();

        // 1. Get TLE service (factory)
        tleService = tleServiceFactory.getTleService(tleProvider);

        // 2. For each satellite
        for (SatelliteRequestDto satellite : satellites) {
            SatelliteResponseDto.SatelliteResponseDtoBuilder builder = SatelliteResponseDto.builder();
            // Get TLE (Two-Line Element) data

            positions.add(builder.build());
        }

        return positions;
    }


    private TLE getTle(SatelliteRequestDto satellite) {
        TLE tle = null;
        // Select cached TLE or TLEPropagator

        // Fetch new TLE

        // Store in cache

        // Get TLE from cache

        return tle;
    }

    private TLEPropagator getTlePropagator() {
        TLEPropagator tlePropagator = null;

        return tlePropagator;
    }

    @Override
    public void rebuildCache() {
        log.info("Rebuilding propagator cache from database TLEs...");

        // Clear existing cache
        int previousSize = satelliteCache.size();
        satelliteCache.clear();
        log.info("Cleared {} cached entries", previousSize);

        // Load all satellites from database and rebuild cache
        List<Satellite> allSatellites = satelliteRepository.findAll();
        int successCount = 0;
        int errorCount = 0;

        for (Satellite tleEntity : allSatellites) {
            try {
                TLE orekitTle = new TLE(tleEntity.getLine1(), tleEntity.getLine2());
                TLEPropagator propagator = TLEPropagator.selectExtrapolator(orekitTle);

                SatelliteKey key = SatelliteKey.norad(tleEntity.getNoradCatId());
                Instant fetchTime = tleEntity.getFetchedAt() != null ? tleEntity.getFetchedAt() : Instant.now();

                satelliteCache.put(key, new CachedSatelliteData(orekitTle, propagator, fetchTime));
                successCount++;
            } catch (Exception e) {
                log.warn("Failed to create propagator for NORAD ID {}: {}",
                        tleEntity.getNoradCatId(), e.getMessage());
                errorCount++;
            }
        }

        log.info("Propagator cache rebuilt: {} satellites loaded, {} errors", successCount, errorCount);
    }
}
