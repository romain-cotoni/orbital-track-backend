package space.satellite.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;
import space.satellite.entities.Satellite;
import space.satellite.record.SatelliteRecord;

import java.time.Instant;

/**
 * Spring Batch ItemProcessor that validates and transforms satellite records.
 * <p>
 * Performs TLE format validation on each record and converts valid records
 * to {@link Satellite} entities. Invalid records are filtered out by returning null.
 * </p>
 */
@Component
@Slf4j
public class SatelliteItemProcessor implements ItemProcessor<SatelliteRecord, Satellite> {

    /**
     * Validates the TLE lines and builds a Satellite entity.
     * Returns null (skip) if TLE lines are missing or malformed.
     */
    @Override
    public Satellite process(SatelliteRecord record) {
        if (!isValidTle(record)) {
            log.warn("Invalid TLE for NORAD ID {}: skipping", record.noradCatId());
            return null;
        }

        return Satellite.builder()
                .noradCatId(record.noradCatId())
                .name(record.name())
                .objectType(record.objectType())
                .countryCode(record.countryCode())
                .launchDate(record.launchDate())
                .decayDate(record.decayDate())
                .rcsSize(record.rcsSize())
                .orbitRegime(record.orbitRegime())
                .epoch(record.epoch())
                .line1(record.line1())
                .line2(record.line2())
                .fetchedAt(Instant.now())
                .source(record.source())
                .build();
    }

    private boolean isValidTle(SatelliteRecord record) {
        if (record == null) return false;
        if (record.noradCatId() == null || record.noradCatId() <= 0) return false;
        if (record.line1() == null || record.line1().length() < 69) return false;
        if (record.line2() == null || record.line2().length() < 69) return false;
        if (!record.line1().startsWith("1 ")) return false;
        if (!record.line2().startsWith("2 ")) return false;
        return true;
    }
}
