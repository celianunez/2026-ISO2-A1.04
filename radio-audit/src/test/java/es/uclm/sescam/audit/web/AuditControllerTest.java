package es.uclm.sescam.audit.web;

import es.uclm.sescam.audit.api.AuditEntryDto;
import es.uclm.sescam.audit.api.AuditQuery;
import es.uclm.sescam.audit.api.AuditResult;
import es.uclm.sescam.audit.api.IAuditService;
import es.uclm.sescam.common.api.Role;
import es.uclm.sescam.common.api.UserRef;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HTTP behaviour of {@code /api/audit}: administrators only, read only, and the query itself is audited. */
@WebMvcTest(AuditController.class)
@Import(AuditControllerTest.MethodSecurityConfig.class)
class AuditControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private IAuditService auditService;

    @Test
    @WithMockUser(username = "u-admin-01", roles = "ADMINISTRATOR")
    void administratorFiltersByUserDatesAndResource() throws Exception {
        AuditEntryDto entry = new AuditEntryDto(7L, Instant.parse("2026-11-12T08:00:00Z"), "u-phys-01",
                Optional.of(Role.PHYSICIAN), "READ", "patient/P-2001", AuditResult.SUCCESS);
        AuditQuery expected = new AuditQuery("u-phys-01", Instant.parse("2026-11-11T00:00:00Z"),
                Instant.parse("2026-11-14T00:00:00Z"), "patient/");
        when(auditService.query(expected)).thenReturn(List.of(entry));

        mvc.perform(get("/api/audit")
                        .param("user", "u-phys-01")
                        .param("from", "2026-11-11T00:00:00Z")
                        .param("to", "2026-11-14T00:00:00Z")
                        .param("resource", "patient/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value("u-phys-01"))
                .andExpect(jsonPath("$[0].role").value("PHYSICIAN"))
                .andExpect(jsonPath("$[0].action").value("READ"))
                .andExpect(jsonPath("$[0].resource").value("patient/P-2001"));

        verify(auditService).query(expected);
    }

    @Test
    @WithMockUser(username = "u-admin-01", roles = "ADMINISTRATOR")
    void consultingTheLogIsItselfAudited() throws Exception {
        when(auditService.query(any())).thenReturn(List.of());

        mvc.perform(get("/api/audit")).andExpect(status().isOk());

        verify(auditService).record(new UserRef("u-admin-01", "u-admin-01", Role.ADMINISTRATOR),
                AuditController.READ_ACTION, AuditController.AUDIT_LOG_RESOURCE);
    }

    @Test
    @WithMockUser(username = "u-admin-01", roles = "ADMINISTRATOR")
    void malformedDateIsBadRequest() throws Exception {
        mvc.perform(get("/api/audit").param("from", "yesterday")).andExpect(status().isBadRequest());
        verifyNoInteractions(auditService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"PATIENT", "PHYSICIAN", "ADMISSIONS"})
    void nonAdministratorIsForbidden(String role) throws Exception {
        mvc.perform(get("/api/audit").with(user("u-other").roles(role)))
                .andExpect(status().isForbidden());
        verifyNoInteractions(auditService);
    }

    @Test
    void anonymousUserIsUnauthorized() throws Exception {
        mvc.perform(get("/api/audit")).andExpect(status().isUnauthorized());
        verifyNoInteractions(auditService);
    }

    @Test
    @WithMockUser(username = "u-admin-01", roles = "ADMINISTRATOR")
    void entriesCannotBeCreatedChangedOrDeletedOverHttp() throws Exception {
        mvc.perform(post("/api/audit").with(csrf()).contentType("application/json").content("{}"))
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(put("/api/audit").with(csrf()).contentType("application/json").content("{}"))
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(patch("/api/audit").with(csrf()).contentType("application/json").content("{}"))
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(delete("/api/audit").with(csrf())).andExpect(status().isMethodNotAllowed());
        verifyNoInteractions(auditService);
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityConfig {
    }
}
