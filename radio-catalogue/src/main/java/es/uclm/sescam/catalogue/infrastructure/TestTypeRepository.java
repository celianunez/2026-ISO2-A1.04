package es.uclm.sescam.catalogue.infrastructure;

import es.uclm.sescam.catalogue.domain.TestType;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Storage of the catalogue of test types. It extends the marker {@link Repository} (not
 * {@code CrudRepository}) so that it exposes only {@code save} and the queries the use case needs:
 * a test type is never deleted, only deactivated logically (A2 of CU-23).
 */
public interface TestTypeRepository extends Repository<TestType, String> {

    /**
     * Finds a test type by its code, active or not.
     *
     * @param code the code, for example {@code RX-THORAX}
     * @return the test type, or empty if no test type has that code
     */
    Optional<TestType> findById(String code);

    /**
     * Returns the test types that can be requested, ordered by name.
     *
     * @return the active test types; empty if there are none
     */
    List<TestType> findByActiveTrueOrderByName();

    /**
     * Stores a new test type or the changes of an existing one (JPA merges on {@code save}).
     *
     * @param testType the test type to store
     * @return the stored test type, with its identifier
     */
    TestType save(TestType testType);

    /**
     * Counts the stored test types, active or not. Used by the initial load to detect an empty
     * store (or a restored database) and avoid duplicating it.
     *
     * @return how many test types are stored
     */
    long count();
}
