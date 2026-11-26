package es.uclm.sescam.auth;

import es.uclm.sescam.audit.api.AuditResult;
import es.uclm.sescam.audit.api.IAuditService;
import es.uclm.sescam.auth.domain.UserAccount;
import es.uclm.sescam.auth.infrastructure.UserAccountRepository;
import es.uclm.sescam.common.api.Role;
import es.uclm.sescam.common.api.UserRef;
import es.uclm.sescam.ssoadapter.infrastructure.SimulatedSsoGateway;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Set;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end login inside {@code radio-auth} with the SSO simulator and the real roles of CU-18 on H2 (CU-20-S,
 * acceptance criterion 4; test cases 6, 12 and 13). Only the audit is a mock: it belongs to another module.
 */
@SpringBootTest(classes = AuthTestApplication.class)
@AutoConfigureMockMvc
@Import(SimulatedSsoGateway.class)
class LoginFlowIT {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserAccountRepository accounts;

    @MockitoBean
    private IAuditService audit;

    @BeforeEach
    void seedFictitiousUsers() {
        seed("physician01", "Demo Physician One", Role.PHYSICIAN);
        seed("physician02", "Demo Physician Two", Role.PHYSICIAN);
        seed("admin01", "Demo Administrator", Role.ADMINISTRATOR);
        seed("norole01", "Demo No Role");
    }

    @Test
    void aPhysicianLogsInWithTheSimulatorButCannotManageRoles() throws Exception {
        MockHttpSession physician = new MockHttpSession();

        login("token-physician01", physician)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("PHYSICIAN"));
        mvc.perform(get("/api/auth/me").session(physician))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("physician01"));
        mvc.perform(get("/api/users").session(physician))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("forbidden"));

        verify(audit).record(new UserRef("physician01", "Demo Physician One", Role.PHYSICIAN), "LOGIN", "auth/login",
                AuditResult.SUCCESS);
    }

    @Test
    void anAdministratorLogsInWithTheSimulatorAndManagesRoles() throws Exception {
        MockHttpSession administrator = new MockHttpSession();

        login("token-admin01", administrator).andExpect(status().isOk());
        mvc.perform(get("/api/users").session(administrator)).andExpect(status().isOk());
    }

    @Test
    void anIdentityWithoutRoleIsDeniedThroughTheWholeStack() throws Exception {
        login("token-norole01", new MockHttpSession())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("no_role"));

        verify(audit).record(new UserRef("norole01", "Demo No Role", null), "LOGIN", "auth/login",
                AuditResult.FAILURE);
    }

    @Test
    void anExpiredTokenOfTheSimulatorIsRejected() throws Exception {
        login("token-expired", new MockHttpSession())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid_token"));

        verify(audit).record(isNull(), eq("LOGIN"), eq("auth/login"), eq(AuditResult.FAILURE));
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void aRoleChangeTakesEffectAtTheNextLogin() throws Exception {
        MockHttpSession physicianBefore = new MockHttpSession();
        MockHttpSession administrator = new MockHttpSession();
        login("token-physician02", physicianBefore).andExpect(status().isOk());
        Cookie csrfCookie = login("token-admin01", administrator).andExpect(status().isOk())
                .andReturn().getResponse().getCookie("XSRF-TOKEN");

        // Same as the shared js/api.js: the value of the XSRF-TOKEN cookie goes back in the X-XSRF-TOKEN header.
        mvc.perform(put("/api/users/physician02/roles/ADMINISTRATOR").session(administrator).cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue()))
                .andExpect(status().isNoContent());

        // The session opened before the change keeps the old roles (spec #26, step 6)...
        mvc.perform(get("/api/users").session(physicianBefore)).andExpect(status().isForbidden());
        // ...and the next login gets the new ones.
        MockHttpSession physicianAfter = new MockHttpSession();
        login("token-physician02", physicianAfter)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles.length()").value(2));
        mvc.perform(get("/api/users").session(physicianAfter)).andExpect(status().isOk());
    }

    private ResultActions login(String token, MockHttpSession session) throws Exception {
        return mvc.perform(post("/api/auth/login").session(session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\"}"));
    }

    private void seed(String id, String displayName, Role... roles) {
        if (!accounts.existsById(id)) {
            accounts.save(new UserAccount(id, displayName, true, Set.of(roles)));
        }
    }
}
