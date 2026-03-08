package space.satellite.repositories;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;
import space.satellite.entities.Satellite;

public interface SatelliteSpecification {

    static Specification<Satellite> hasName(final String name) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(name)) {
                return null;
            }
            return cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
        };
    }

    static Specification<Satellite> hasObjectType(final String objectType) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(objectType)) {
                return null;
            }
            return cb.equal(root.get("objectType"), objectType);
        };
    }

    static Specification<Satellite> hasCountryCode(final String countryCode) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(countryCode)) {
                return null;
            }
            return cb.equal(root.get("countryCode"), countryCode);
        };
    }

    static Specification<Satellite> hasOrbitRegime(final String orbitRegime) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(orbitRegime)) {
                return null;
            }
            return cb.equal(root.get("orbitRegime"), orbitRegime);
        };
    }

}
