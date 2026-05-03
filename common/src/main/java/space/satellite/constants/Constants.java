package space.satellite.constants;

import java.time.Duration;
import java.time.format.DateTimeFormatter;

public final class Constants {

    /**
     * Private constructor to prevent instantiation
     */
    private Constants() {
        throw new UnsupportedOperationException("Constant utility class");
    }

    public static final String OREKIT_DATA_URL = "https://gitlab.orekit.org/orekit/orekit-data/-/archive/main/orekit-data-main.zip";
    public static final String OREKIT_DATA_DIRECTORY = "common/src/main/resources/orekit-data";
    public static final String OREKIT_DATA_CLASSPATH = "orekit-data";

    public static final Duration TLE_CACHE_DURATION = Duration.ofHours(2);

    /** TLEs older than this are too stale for reliable SGP4/SDP4 propagation. */
    public static final Duration MAX_TLE_AGE = Duration.ofDays(60);

    /** GEO satellites update less frequently — allow a longer TLE age window. */
    public static final Duration MAX_TLE_AGE_GEO = Duration.ofDays(180);


    public static final String SPACETRACK_SOURCE = "spacetrack";
    public static final String SPACETRACK_BASE_URL_PRD = "https://www.space-track.org";
    public static final String SPACETRACK_BASE_URL_TEST = "https://for-testing-only.space-track.org";
    public static final String SPACETRACK_LOGIN_URL = "/ajaxauth/login";
    public static final String SPACETRACK_BASE_QUERY_URL = "/basicspacedata/query/class/gp";
    public static final String SPACETRACK_BASE_URL = SPACETRACK_BASE_URL_TEST;


    /**
     * LEO (Low Earth Orbit)
     * Altitude < 2000km
     * Filter Logic: MEAN_MOTION > 11.25 (This means the satellite orbits at least 11.25 times per day).
     */
    public static final String LEO_URL = "/MEAN_MOTION/%3E11.25/ECCENTRICITY/%3C0.25";

    /**
     * MEO (Medium Earth Orbit)
     * Altitude between 2000km and 35786km
     * Filter Logic: A "Period" (time for one orbit) between 600 and 800 minutes is a common MEO window
     */
    public static final String MEO_URL = "/PERIOD/600--800/ECCENTRICITY/%3C0.25";

    /**
     * HEO (Highly Elliptical Orbit)
     * These are "oval" orbits that swing very close and then very far away
     * Definition: High eccentricity.
     * Filter Logic: ECCENTRICITY > 0.25
     */
    public static final String HEO_URL = "/ECCENTRICITY/%3E0.25";

    public static final String WITHIN_24_HOURS_URL = "/epoch/%3Enow-1";

    public static final String WITHIN_72_HOURS_URL = "/epoch/%3Enow-3";

    public static final String WITHIN_30_DAYS_URL  = "/epoch/%3Enow-30";

    public static final String WITHIN_180_DAYS_URL = "/epoch/%3Enow-180";

    public static final String NOT_DECAYED_URL     = "/decay_date/null-val";

    public static final String OBJECT_TYPE_PAYLOAD_URL = "/OBJECT_TYPE/PAYLOAD";

    public static final String OBJECT_TYPE_ROCKET_BODY_URL = "/OBJECT_TYPE/ROCKET BODY";

    public static final String OBJECT_TYPE_DEBRIS_URL = "/OBJECT_TYPE/DEBRIS";

    public static final String FORMAT_JSON = "/format/json";

    public static final DateTimeFormatter EPOCH_FORMATTER = DateTimeFormatter.ofPattern("yyDDD.HHmmssSSS");


    // Examples of queries
    // /basicspacedata/query/class/gp/orderby/NORAD_CAT_ID%20asc/format/3le
    // /basicspacedata/query/class/gp/decay_date/null-val/epoch/%3Enow-3/format/3le
    // /basicspacedata/query/class/gp/NORAD_CAT_ID/~~25544%2C20580%2C48274/orderby/OBJECT_NAME%20asc/

    //Query for active payloads specifically in LEO within last 24 hours :
    // https://www.space-track.org/basicspacedata/query/class/gp/OBJECT_TYPE/PAYLOAD/MEAN_MOTION/%3E11.25/epoch/%3Enow-1/decay_date/null-val/format/json

    // Test queries
    // /basicspacedata/query/class/gp/NORAD_CAT_ID/25544%2C%2020580%2C%2048274/orderby/OBJECT_NAME%20asc/metadata/true/emptyresult/show



}
