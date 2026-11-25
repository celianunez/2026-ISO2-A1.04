package es.uclm.sescam.audit.api;

import es.uclm.sescam.audit.application.AuditService;
import es.uclm.sescam.common.api.Role;
import es.uclm.sescam.common.api.UserRef;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contract test of {@link IAuditService} against the real store (H2): it behaves as its Javadoc says.
 * Covers the acceptance criteria of CU-19 that do not need HTTP.
 */
@DataJpaTest
@Import({AuditService.class, AuditServiceContractTest.MovableClockConfig.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AuditServiceContractTest {

    private static final UserRef PHYSICIAN = new UserRef("u-phys-01", "Dr. Test", Role.PHYSICIAN);
    private static final UserRef ADMISSIONS = new UserRef("u-adm-01", "Admissions Test", Role.ADMISSIONS);

    @Autowired
    private IAuditService auditService;

    @Autowired
    private MovableClock clock;

    @Autowired
    private JdbcTemplate jdbc;

    /**
     * The test is not transactional (entries are written in their own transaction), so the table is emptied
     * here. This is test cleanup only: the production code has no way to delete entries.
     */
    @BeforeEach
    void emptyTheLog() {
        jdbc.update("delete from audit_entry");
    }

    private void recordAt(String instant, UserRef user, String action, String resource) {
        clock.set(Instant.parse(instant));
        auditService.record(user, action, resource);
    }

    @Test
    void recordedAccessIsStoredWithUserDateActionAndResource() {
        recordAt("2026-11-25T09:00:00Z", PHYSICIAN, "READ", "patient/P-1001");

        List<AuditEntryDto> found = auditService.query(new AuditQuery("u-phys-01", null, null, "patient/P-1001"));

        assertThat(found).singleElement().satisfies(entry -> {
            assertThat(entry.id()).isPositive();
            assertThat(entry.timestamp()).isEqualTo(Instant.parse("2026-11-25T09:00:00Z"));
            assertThat(entry.userId()).isEqualTo("u-phys-01");
            assertThat(entry.role()).contains(Role.PHYSICIAN);
            assertThat(entry.action()).isEqualTo("READ");
            assertThat(entry.resource()).isEqualTo("patient/P-1001");
            assertThat(entry.result()).isEqualTo(AuditResult.SUCCESS);
        });
    }

    @Test
    void failedActionWithoutUserIsStoredAsAnonymous() {
        clock.set(Instant.parse("2026-11-25T09:30:00Z"));
        auditService.record(null, "LOGIN", "auth/login", AuditResult.FAILURE);

        List<AuditEntryDto> found = auditService.query(
                new AuditQuery(IAuditService.ANONYMOUS_USER_ID, null, null, null));

        assertThat(found).singleElement().satisfies(entry -> {
            assertThat(entry.role()).isEqualTo(Optional.empty());
            assertThat(entry.result()).isEqualTo(AuditResult.FAILURE);
        });
    }

    @Test
    void filterByUserAndDatesReturnsOnlyMatchingEntriesNewestFirst() {
        recordAt("2026-11-10T08:00:00Z", PHYSICIAN, "READ", "patient/P-2001");
        recordAt("2026-11-12T08:00:00Z", PHYSICIAN, "READ", "report/R-2001");
        recordAt("2026-11-13T08:00:00Z", PHYSICIAN, "UPDATE", "report/R-2001");
        recordAt("2026-11-12T09:00:00Z", ADMISSIONS, "READ", "patient/P-2001");
        recordAt("2026-11-20T08:00:00Z", PHYSICIAN, "READ", "patient/P-2001");

        List<AuditEntryDto> found = auditService.query(new AuditQuery("u-phys-01",
                Instant.parse("2026-11-11T00:00:00Z"), Instant.parse("2026-11-14T00:00:00Z"), null));

        assertThat(found).extracting(AuditEntryDto::timestamp).containsExactly(
                Instant.parse("2026-11-13T08:00:00Z"), Instant.parse("2026-11-12T08:00:00Z"));
    }

    @Test
    void dateBoundsAreInclusive() {
        recordAt("2026-11-14T08:00:00Z", PHYSICIAN, "READ", "patient/P-2501");

        Instant exact = Instant.parse("2026-11-14T08:00:00Z");
        assertThat(auditService.query(new AuditQuery(null, exact, exact, null))).hasSize(1);
    }

    @Test
    void filterByResourcePrefix() {
        recordAt("2026-11-15T08:00:00Z", PHYSICIAN, "READ", "patient/P-3001");
        recordAt("2026-11-15T08:01:00Z", ADMISSIONS, "CREATE", "patient/P-3002");
        recordAt("2026-11-15T08:02:00Z", ADMISSIONS, "CREATE", "request/Q-3002");

        List<AuditEntryDto> found = auditService.query(new AuditQuery(null, null, null, "patient/"));

        assertThat(found).extracting(AuditEntryDto::resource).containsExactly("patient/P-3002", "patient/P-3001");
    }

    @Test
    void wildcardsInResourceFilterAreTakenLiterally() {
        recordAt("2026-11-16T08:00:00Z", PHYSICIAN, "READ", "image/I-4001");

        assertThat(auditService.query(new AuditQuery(null, null, null, "%"))).isEmpty();
    }

    @Test
    void queryWithNoMatchReturnsEmptyList() {
        assertThat(auditService.query(new AuditQuery("nobody", null, null, null))).isEmpty();
    }

    @Test
    void queryWithFromAfterToIsRejected() {
        AuditQuery invalid = new AuditQuery(null, Instant.parse("2026-11-20T00:00:00Z"),
                Instant.parse("2026-11-10T00:00:00Z"), null);

        assertThatThrownBy(() -> auditService.query(invalid)).isInstanceOf(InvalidAuditQueryException.class);
    }

    /** Clock whose time the test can set. */
    static final class MovableClock extends Clock {

        private final AtomicReference<Instant> now = new AtomicReference<>(Instant.EPOCH);

        void set(Instant instant) {
            now.set(instant);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now.get();
        }
    }

    @TestConfiguration
    static class MovableClockConfig {

        @Bean
        MovableClock movableClock() {
            return new MovableClock();
        }
    }
}
