package es.uclm.sescam.catalogue.api;

import java.util.List;
import java.util.Optional;

/**
 * Public contract of the catalogue of test types (CU-23) and of their clinical guides (CU-05).
 *
 * <p>Every authenticated user may read the catalogue; only administrators may change it (Spring Security, D-09).
 * Every change is audited (P-12). Deactivation is logical: a deactivated test type is kept for the old requests but
 * can no longer be requested.
 */
public interface ICatalogueService {

    /**
     * Finds a test type by its code, whether it is active or not.
     *
     * @param code code of the test type, for example {@code RX-THORAX}
     * @return the test type, or empty if no test type has that code
     */
    Optional<TestTypeDto> findTestType(String code);

    /**
     * Returns the test types that can be requested.
     *
     * @return the active test types, ordered by name; empty if there are none
     */
    List<TestTypeDto> listActiveTestTypes();

    /**
     * Tells whether a test type can be requested by a physician (CU-06).
     *
     * @param code code of the test type
     * @return {@code true} if the test type exists and is active; {@code false} otherwise
     */
    boolean isRequestable(String code);

    /**
     * Returns the clinical guides of a test type (CU-05). The guides are added in iteration 6; until then the list is
     * empty.
     *
     * @param testTypeCode code of the test type
     * @return the guides of the test type; empty if it has none or does not exist
     */
    List<ClinicalGuideDto> getGuides(String testTypeCode);

    /**
     * Creates an active test type.
     *
     * @param code code of the new test type; unique
     * @param data attributes of the test type
     * @return the created test type
     * @throws DuplicateTestTypeException if a test type with that code already exists
     * @throws InvalidTestTypeException   if a mandatory attribute is missing or a value is not valid
     */
    TestTypeDto create(String code, TestTypeData data);

    /**
     * Changes the attributes of a test type. The code and the active flag do not change.
     *
     * @param code code of the test type
     * @param data new attributes
     * @return the updated test type
     * @throws es.uclm.sescam.common.api.NotFoundException if no test type has that code
     * @throws InvalidTestTypeException if a mandatory attribute is missing or a value is not valid
     */
    TestTypeDto update(String code, TestTypeData data);

    /**
     * Deactivates a test type. Nothing changes if it is already inactive.
     *
     * @param code code of the test type
     * @throws es.uclm.sescam.common.api.NotFoundException if no test type has that code
     */
    void deactivate(String code);
}
