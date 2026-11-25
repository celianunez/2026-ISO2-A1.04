package es.uclm.sescam.catalogue.web;

import es.uclm.sescam.catalogue.api.ClinicalGuideDto;
import es.uclm.sescam.catalogue.api.ICatalogueService;
import es.uclm.sescam.catalogue.api.TestTypeData;
import es.uclm.sescam.catalogue.api.TestTypeDto;
import es.uclm.sescam.common.api.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST access to the catalogue of test types (CU-23).
 *
 * <p>Reading is open to every authenticated user: physicians and admissions need the catalogue to
 * prescribe and book. Writing is restricted to administrators with {@code @PreAuthorize} (D-09), and
 * every change goes through {@link ICatalogueService}, which records the audit entry (P-12).
 */
@RestController
@RequestMapping("/api/catalogue/test-types")
public class CatalogueController {

    private final ICatalogueService catalogueService;

    /**
     * Creates the controller.
     *
     * @param catalogueService the catalogue service
     */
    public CatalogueController(ICatalogueService catalogueService) {
        this.catalogueService = catalogueService;
    }

    /**
     * Lists the test types that can be requested, ordered by name.
     *
     * @return the active test types; empty if there are none
     */
    @GetMapping
    public List<TestTypeDto> list() {
        return catalogueService.listActiveTestTypes();
    }

    /**
     * Returns one test type, active or not (an administrator edits deactivated ones too).
     *
     * @param code code of the test type
     * @return the test type
     */
    @GetMapping("/{code}")
    public TestTypeDto get(@PathVariable String code) {
        return catalogueService.findTestType(code)
                .orElseThrow(() -> new NotFoundException("No test type with code " + code));
    }

    /**
     * Returns the clinical guides of a test type (CU-05). Empty until iteration 6.
     *
     * @param code code of the test type
     * @return the guides of the test type; empty if it has none or does not exist
     */
    @GetMapping("/{code}/guides")
    public List<ClinicalGuideDto> guides(@PathVariable String code) {
        return catalogueService.getGuides(code);
    }

    /**
     * Creates an active test type. The code travels in the path because it is the identifier and
     * {@link TestTypeData} carries only the attributes.
     *
     * @param code code of the new test type; unique
     * @param data attributes of the test type
     * @return the created test type, with HTTP 201
     */
    @PostMapping("/{code}")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public TestTypeDto create(@PathVariable String code, @RequestBody TestTypeData data) {
        return catalogueService.create(code, data);
    }

    /**
     * Changes the attributes of a test type. The code and the active flag do not change.
     *
     * @param code code of the test type
     * @param data new attributes
     * @return the updated test type
     */
    @PutMapping("/{code}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public TestTypeDto update(@PathVariable String code, @RequestBody TestTypeData data) {
        return catalogueService.update(code, data);
    }

    /**
     * Deactivates a test type. Nothing changes if it is already inactive.
     *
     * @param code code of the test type
     * @return HTTP 204 with an empty body
     */
    @PostMapping("/{code}/deactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public void deactivate(@PathVariable String code) {
        catalogueService.deactivate(code);
    }
}
