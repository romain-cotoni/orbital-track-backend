package space.satellite.repositories;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;
import space.satellite.entities.Satellite;

public interface SatelliteSpecification {


    static Specification<Satellite> hasName(final String name) {
        return (root, query, cb) -> {
            if(!StringUtils.hasText(name)) {
                return null;
            }
            return cb.equal(root.get("name"), name);
        };
    }

    static Specification<Satellite> hasCatalog(final String catalog) {
        return (root, query, cb) -> {
            if(!StringUtils.hasText(catalog)) {
                return null;
            }
            return cb.equal(root.get("name"), catalog);
        };
    }

}
