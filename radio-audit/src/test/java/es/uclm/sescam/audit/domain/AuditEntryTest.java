package es.uclm.sescam.audit.domain;

import es.uclm.sescam.audit.api.AuditResult;
import es.uclm.sescam.common.api.Role;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuditEntryTest {

    private static final Instant NOW = Instant.parse("2026-11-25T10:00:00Z");

    @Test
    void createsEntryWithAllFields() {
        AuditEntry entry = AuditEntry.create(NOW, " u-phys-01 ", Role.PHYSICIAN, " READ ", " patient/P-0001 ",
                AuditResult.SUCCESS);

        assertThat(entry.getId()).isNull();
        assertThat(entry.getTimestamp()).isEqualTo(NOW);
        assertThat(entry.getUserId()).isEqualTo("u-phys-01");
        assertThat(entry.getRole()).contains(Role.PHYSICIAN);
        assertThat(entry.getAction()).isEqualTo("READ");
        assertThat(entry.getResource()).isEqualTo("patient/P-0001");
        assertThat(entry.getResult()).isEqualTo(AuditResult.SUCCESS);
    }

    @Test
    void acceptsEntryWithoutRole() {
        AuditEntry entry = AuditEntry.create(NOW, "anonymous", null, "LOGIN", "auth/login", AuditResult.FAILURE);

        assertThat(entry.getRole()).isEmpty();
    }

    @Test
    void rejectsBlankUser() {
        assertThatThrownBy(() -> AuditEntry.create(NOW, " ", Role.PATIENT, "READ", "report/R-1",
                AuditResult.SUCCESS)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("userId");
    }

    @Test
    void rejectsBlankAction() {
        assertThatThrownBy(() -> AuditEntry.create(NOW, "u-1", Role.PATIENT, " ", "report/R-1",
                AuditResult.SUCCESS)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("action");
        assertThatThrownBy(() -> AuditEntry.create(NOW, "u-1", Role.PATIENT, null, "report/R-1",
                AuditResult.SUCCESS)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("action");
    }

    @Test
    void rejectsMissingResource() {
        assertThatThrownBy(() -> AuditEntry.create(NOW, "u-1", Role.PATIENT, "READ", "",
                AuditResult.SUCCESS)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("resource");
    }

    @Test
    void rejectsTooLongTexts() {
        String longResource = "patient/" + "x".repeat(AuditEntry.MAX_RESOURCE_LENGTH);
        String longAction = "A".repeat(AuditEntry.MAX_ACTION_LENGTH + 1);
        String longUser = "u".repeat(AuditEntry.MAX_USER_ID_LENGTH + 1);

        assertThatThrownBy(() -> AuditEntry.create(NOW, "u-1", Role.PATIENT, "READ", longResource,
                AuditResult.SUCCESS)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AuditEntry.create(NOW, "u-1", Role.PATIENT, longAction, "report/R-1",
                AuditResult.SUCCESS)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AuditEntry.create(NOW, longUser, Role.PATIENT, "READ", "report/R-1",
                AuditResult.SUCCESS)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingTimestampOrResult() {
        assertThatThrownBy(() -> AuditEntry.create(null, "u-1", null, "READ", "r/1", AuditResult.SUCCESS))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AuditEntry.create(NOW, "u-1", null, "READ", "r/1", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void entriesAreEqualOnlyWhenStoredWithSameId() {
        AuditEntry a = AuditEntry.create(NOW, "u-1", null, "READ", "r/1", AuditResult.SUCCESS);
        AuditEntry b = AuditEntry.create(NOW, "u-1", null, "READ", "r/1", AuditResult.SUCCESS);

        assertThat(a).isEqualTo(a).isNotEqualTo(b).isNotEqualTo("r/1");
    }
}
