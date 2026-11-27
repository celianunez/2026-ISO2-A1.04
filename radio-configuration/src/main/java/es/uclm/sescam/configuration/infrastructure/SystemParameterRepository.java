package es.uclm.sescam.configuration.infrastructure;

import es.uclm.sescam.configuration.domain.SystemParameter;
import es.uclm.sescam.configuration.domain.SystemParameter.Group;
import org.springframework.data.repository.Repository;

import java.time.LocalDate;
import java.util.Optional;

/** Stores configuration versions and resolves the value effective on a given date. */
public interface SystemParameterRepository extends Repository<SystemParameter, Long> {

    SystemParameter save(SystemParameter parameter);

    boolean existsByParameterGroupAndParameterKey(Group group, String key);

    Optional<SystemParameter> findTopByParameterGroupAndParameterKeyAndValidFromLessThanEqualOrderByValidFromDescIdDesc(
            Group group, String key, LocalDate effectiveDate);
}
