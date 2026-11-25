package es.uclm.sescam.catalogue.api;

import es.uclm.sescam.audit.api.AuditEntryDto;
import es.uclm.sescam.audit.api.AuditQuery;
import es.uclm.sescam.audit.api.AuditResult;
import es.uclm.sescam.audit.api.IAuditService;
import es.uclm.sescam.audit.application.AuditService;
import es.uclm.sescam.catalogue.application.CatalogueService;
import es.uclm.sescam.catalogue.domain.TestType;
import es.uclm.sescam.catalogue.infrastructure.CatalogueSeedData;
import es.uclm.sescam.catalogue.infrastructure.TestTypeRepository;
import es.uclm.sescam.common.api.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contract test of {@link ICatalogueService} against the real store (H2), with the real audit log of
 * CU-19-S: a change of the catalogue is stored <em>and</em> recorded. It behaves as its Javadoc says.
 */
@DataJpaTest
@Import({CatalogueService.class, AuditService.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CatalogueServiceContractTest {

    private static final String ADMIN_ID = "u-admin-01";

    @Autowired
    private ICatalogueService catalogue;

    @Autowired
    private IAuditService audit;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private TestTypeRepository repository;

    /**
     * The test is not transactional (each change commits on its own, like in production), so the
     * tables are emptied here. This is test cleanup only: the catalogue deactivates instead of
     * deleting, and the audit log is append-only.
     */
    @BeforeEach
    void cleanTheStoresAndAuthenticateAsAdministrator() {
        jdbc.update("delete from test_type");
        jdbc.update("delete from audit_entry");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                ADMIN_ID, "n/a", List.of(new SimpleGrantedAuthority(Role.ADMINISTRATOR.authority()))));
    }

    @AfterEach
    void clearTheSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void aCreatedTestTypeIsStoredAndRecordedInTheAuditLog() {
        TestTypeDto created = catalogue.create("RX-THORAX", valid("Chest X-ray"));

        assertThat(catalogue.findTestType("RX-THORAX")).contains(created);
        assertThat(catalogue.listActiveTestTypes()).extracting(TestTypeDto::code).containsExactly("RX-THORAX");
        assertThat(catalogue.isRequestable("RX-THORAX")).isTrue();

        List<AuditEntryDto> entries = audit.query(new AuditQuery(null, null, null, "catalogue/RX-THORAX"));
        assertThat(entries).singleElement().satisfies(entry -> {
            assertThat(entry.userId()).isEqualTo(ADMIN_ID);
            assertThat(entry.role()).contains(Role.ADMINISTRATOR);
            assertThat(entry.action()).isEqualTo("CREATE");
            assertThat(entry.resource()).isEqualTo("catalogue/RX-THORAX");
            assertThat(entry.result()).isEqualTo(AuditResult.SUCCESS);
        });
    }

    @Test
    void aDuplicateCodeIsRejectedAndNothingChanges() {
        catalogue.create("RX-THORAX", valid("Chest X-ray"));

        assertThatThrownBy(() -> catalogue.create("RX-THORAX", valid("Other")))
                .isInstanceOf(DuplicateTestTypeException.class);

        assertThat(catalogue.listActiveTestTypes()).hasSize(1);
        assertThat(countAuditEntries("catalogue/RX-THORAX")).isOne();
    }

    @Test
    void anInvalidTestTypeIsRejectedAndNeitherStoredNorAudited() {
        TestTypeData withoutName = new TestTypeData(null, Modality.XRAY, "RX-1", 10, "", false, false, "", false);

        assertThatThrownBy(() -> catalogue.create("RX-NEW", withoutName))
                .isInstanceOf(InvalidTestTypeException.class);

        assertThat(catalogue.findTestType("RX-NEW")).isEmpty();
        assertThat(catalogue.listActiveTestTypes()).isEmpty();
        assertThat(countAuditEntries("catalogue/")).isZero();
    }

    @Test
    void deactivationIsLogicalAndKeepsTheHistory() {
        catalogue.create("RX-THORAX", valid("Chest X-ray"));
        catalogue.create("US-ABD", valid("Abdominal ultrasound"));

        catalogue.deactivate("RX-THORAX");

        assertThat(catalogue.listActiveTestTypes()).extracting(TestTypeDto::code).containsExactly("US-ABD");
        assertThat(catalogue.findTestType("RX-THORAX")).hasValueSatisfying(testType -> {
            assertThat(testType.active()).isFalse();
            assertThat(testType.name()).isEqualTo("Chest X-ray");
        });
        assertThat(catalogue.isRequestable("RX-THORAX")).isFalse();
        assertThat(catalogue.isRequestable("US-ABD")).isTrue();
    }

    @Test
    void deactivatingTwiceRecordsOnlyOneAuditEntry() {
        catalogue.create("RX-THORAX", valid("Chest X-ray"));

        catalogue.deactivate("RX-THORAX");
        catalogue.deactivate("RX-THORAX");

        assertThat(audit.query(new AuditQuery(null, null, null, "catalogue/RX-THORAX")))
                .extracting(AuditEntryDto::action)
                .containsExactly("DEACTIVATE", "CREATE");
    }

    @Test
    void anUpdateKeepsCodeAndActiveFlagAndIsAudited() {
        catalogue.create("RX-THORAX", valid("Chest X-ray"));
        catalogue.deactivate("RX-THORAX");

        TestTypeDto updated = catalogue.update("RX-THORAX", valid("Chest X-ray 2"));

        assertThat(updated.code()).isEqualTo("RX-THORAX");
        assertThat(updated.name()).isEqualTo("Chest X-ray 2");
        assertThat(updated.active()).isFalse();

        assertThat(audit.query(new AuditQuery(null, null, null, "catalogue/RX-THORAX")))
                .extracting(AuditEntryDto::action)
                .containsExactly("UPDATE", "DEACTIVATE", "CREATE");
    }

    @Test
    void updateAndDeactivateOfAnUnknownCodeAreNotFound() {
        assertThatThrownBy(() -> catalogue.update("NOPE", valid("Whatever")))
                .isInstanceOf(es.uclm.sescam.common.api.NotFoundException.class);
        assertThatThrownBy(() -> catalogue.deactivate("NOPE"))
                .isInstanceOf(es.uclm.sescam.common.api.NotFoundException.class);

        assertThat(countAuditEntries("catalogue/")).isZero();
    }

    @Test
    void theCodeIsLookedUpRegardlessOfCase() {
        catalogue.create("RX-THORAX", valid("Chest X-ray"));

        assertThat(catalogue.findTestType("rx-thorax")).isPresent();
        assertThat(catalogue.findTestType("RX-THORAX")).isPresent();
        assertThat(catalogue.findTestType("RX-THORAX")).get()
                .extracting(TestTypeDto::code).isEqualTo("RX-THORAX");
    }

    @Test
    void aReadWorksWithoutAnAuthenticatedUser() {
        catalogue.create("RX-THORAX", valid("Chest X-ray"));
        SecurityContextHolder.clearContext();

        assertThat(catalogue.listActiveTestTypes()).hasSize(1);
        assertThat(catalogue.findTestType("RX-THORAX")).isPresent();
        assertThat(catalogue.isRequestable("RX-THORAX")).isTrue();
    }

    @Test
    void guidesAreEmptyUntilTheSixthIteration() {
        assertThat(catalogue.getGuides("RX-THORAX")).isEmpty();
    }

    @Test
    void theSeedLoadsFictitiousTestTypesThatCanBeRequested() {
        new CatalogueSeedData(repository).run();

        assertThat(catalogue.listActiveTestTypes()).extracting(TestTypeDto::code)
                .containsExactly("US-ABD", "RX-THORAX", "CT-HEAD", "MR-KNEE", "MG-BREAST");
        assertThat(catalogue.isRequestable("RX-THORAX")).isTrue();
        assertThat(countAuditEntries("catalogue/")).isZero();

        new CatalogueSeedData(repository).run();
        assertThat(catalogue.listActiveTestTypes()).hasSize(5);
    }

    private long countAuditEntries(String resourcePrefix) {
        List<AuditEntryDto> entries = audit.query(new AuditQuery(null, null, null, resourcePrefix));
        return entries.size();
    }

    private static TestTypeData valid(String name) {
        return new TestTypeData(name, Modality.XRAY, "RX-1", 10, "Remove metal objects",
                false, false, "", true);
    }

    /**
     * Scans the packages of both modules: the catalogue stores its test types and CU-19-S stores the
     * audit entries, and this test needs the two tables to prove that a change reaches both.
     */
    @TestConfiguration
    @EntityScan(basePackages = "es.uclm.sescam")
    @EnableJpaRepositories(basePackages = "es.uclm.sescam")
    static class BothStores {
    }
}
