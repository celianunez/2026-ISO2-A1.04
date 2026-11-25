package es.uclm.sescam.audit.application;

import es.uclm.sescam.audit.api.AuditEntryDto;
import es.uclm.sescam.audit.api.AuditQuery;
import es.uclm.sescam.audit.api.AuditResult;
import es.uclm.sescam.audit.api.IAuditService;
import es.uclm.sescam.audit.api.InvalidAuditQueryException;
import es.uclm.sescam.audit.domain.AuditEntry;
import es.uclm.sescam.audit.infrastructure.AuditRepository;
import es.uclm.sescam.common.api.Role;
import es.uclm.sescam.common.api.UserRef;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuditServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-25T10:00:00Z");
    private static final UserRef PHYSICIAN = new UserRef("u-phys-01", "Dr. Test", Role.PHYSICIAN);

    private AuditRepository repository;
    private AuditService service;

    @BeforeEach
    void setUp() {
        repository = mock(AuditRepository.class);
        when(repository.save(any(AuditEntry.class))).thenAnswer(call -> stored(call.getArgument(0), 1L));
        service = new AuditService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    /** Simulates what the database does on insert: assigns the identifier. */
    private static AuditEntry stored(AuditEntry entry, long id) {
        ReflectionTestUtils.setField(entry, "id", id);
        return entry;
    }

    private AuditEntry savedEntry() {
        ArgumentCaptor<AuditEntry> saved = ArgumentCaptor.forClass(AuditEntry.class);
        verify(repository).save(saved.capture());
        return saved.getValue();
    }

    @Test
    void recordsSuccessfulAccessWithClockTimestamp() {
        service.record(PHYSICIAN, "READ", "patient/P-0001");

        AuditEntry entry = savedEntry();
        assertThat(entry.getTimestamp()).isEqualTo(NOW);
        assertThat(entry.getUserId()).isEqualTo("u-phys-01");
        assertThat(entry.getRole()).contains(Role.PHYSICIAN);
        assertThat(entry.getAction()).isEqualTo("READ");
        assertThat(entry.getResource()).isEqualTo("patient/P-0001");
        assertThat(entry.getResult()).isEqualTo(AuditResult.SUCCESS);
    }

    @Test
    void recordsFailedAccess() {
        service.record(PHYSICIAN, "READ", "report/R-1", AuditResult.FAILURE);

        assertThat(savedEntry().getResult()).isEqualTo(AuditResult.FAILURE);
    }

    @Test
    void recordsActionWithoutUserAsAnonymous() {
        service.record(null, "LOGIN", "auth/login", AuditResult.FAILURE);

        AuditEntry entry = savedEntry();
        assertThat(entry.getUserId()).isEqualTo(IAuditService.ANONYMOUS_USER_ID);
        assertThat(entry.getRole()).isEmpty();
    }

    @Test
    void rejectsBlankActionOrResourceAndMissingResult() {
        assertThatThrownBy(() -> service.record(PHYSICIAN, " ", "patient/P-0001"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.record(PHYSICIAN, "READ", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.record(PHYSICIAN, "READ", "patient/P-0001", null))
                .isInstanceOf(IllegalArgumentException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void queryWithoutFiltersSearchesEverything() {
        when(repository.search(isNull(), eq(AuditService.EARLIEST), eq(AuditService.LATEST), eq("%")))
                .thenReturn(List.of());

        assertThat(service.query(AuditQuery.all())).isEmpty();
        assertThat(service.query(null)).isEmpty();
        assertThat(service.query(new AuditQuery(" ", null, null, " "))).isEmpty();
    }

    @Test
    void queryPassesEveryFilterToTheStore() {
        Instant from = Instant.parse("2026-11-01T00:00:00Z");
        Instant to = Instant.parse("2026-11-30T00:00:00Z");
        AuditEntry storedEntry = stored(AuditEntry.create(NOW, "u-phys-01", Role.PHYSICIAN, "READ",
                "patient/P-0001", AuditResult.SUCCESS), 42L);
        when(repository.search("u-phys-01", from, to, "patient/P-0001%")).thenReturn(List.of(storedEntry));

        List<AuditEntryDto> result = service.query(new AuditQuery(" u-phys-01 ", from, to, "patient/P-0001"));

        assertThat(result).singleElement().satisfies(e -> {
            assertThat(e.id()).isEqualTo(42L);
            assertThat(e.userId()).isEqualTo("u-phys-01");
            assertThat(e.role()).contains(Role.PHYSICIAN);
            assertThat(e.action()).isEqualTo("READ");
        });
    }

    @Test
    void rejectsQueryWithFromAfterTo() {
        AuditQuery query = new AuditQuery(null, NOW, NOW.minusSeconds(1), null);

        assertThatThrownBy(() -> service.query(query)).isInstanceOf(InvalidAuditQueryException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void escapesLikeWildcardsInResourcePrefix() {
        assertThat(AuditService.likePrefix("a%b_c!d")).isEqualTo("a!%b!_c!!d%");
        assertThat(AuditService.likePrefix("")).isEqualTo("%");
        assertThat(AuditService.likePrefix(null)).isEqualTo("%");
    }
}
