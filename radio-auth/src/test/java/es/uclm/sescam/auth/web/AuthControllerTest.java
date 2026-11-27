package es.uclm.sescam.auth.web;

import es.uclm.sescam.auth.api.AuthenticatedUserDto;
import es.uclm.sescam.auth.api.AuthenticationFailedException;
import es.uclm.sescam.auth.api.AuthenticationFailedException.Reason;
import es.uclm.sescam.auth.api.IAuthenticationService;
import es.uclm.sescam.auth.infrastructure.SecurityConfig;
import es.uclm.sescam.common.api.Role;
import es.uclm.sescam.ssoadapter.api.SsoUnavailableException;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The login endpoints with the real security configuration (CU-20-S, test cases 4 to 11 and 14 to 17). The login
 * logic is a mock: it is tested in {@code AuthenticationServiceTest}.
 */
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    private static final AuthenticatedUserDto PHYSICIAN =
            new AuthenticatedUserDto("physician01", "Demo Physician One", Set.of(Role.PHYSICIAN));

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private IAuthenticationService authentication;

    @Test
    void loginReturnsTheUserStartsTheSessionAndSetsTheCsrfCookie() throws Exception {
        when(authentication.loginWithSso("token-physician01")).thenReturn(PHYSICIAN);

        mvc.perform(login("token-physician01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("physician01"))
                .andExpect(jsonPath("$.displayName").value("Demo Physician One"))
                .andExpect(jsonPath("$.roles[0]").value("PHYSICIAN"))
                .andExpect(request().sessionAttribute("SPRING_SECURITY_CONTEXT", notNullValue()))
                .andExpect(cookie().exists("XSRF-TOKEN"));
    }

    @Test
    void theSessionOfTheLoginGivesAccessToMe() throws Exception {
        when(authentication.loginWithSso("token-physician01")).thenReturn(PHYSICIAN);
        when(authentication.currentUser()).thenReturn(Optional.of(PHYSICIAN));
        MockHttpSession session = new MockHttpSession();

        mvc.perform(login("token-physician01").session(session)).andExpect(status().isOk());

        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("physician01"))
                .andExpect(jsonPath("$.roles[0]").value("PHYSICIAN"));
    }

    @Test
    void loginChangesTheSessionId() throws Exception {
        when(authentication.loginWithSso("token-physician01")).thenReturn(PHYSICIAN);
        MockHttpSession session = new MockHttpSession();
        String idBeforeLogin = session.getId();

        mvc.perform(login("token-physician01").session(session)).andExpect(status().isOk());

        assertThat(session.getId()).isNotEqualTo(idBeforeLogin);
    }

    @Test
    void loginRenewsTheCsrfToken() throws Exception {
        when(authentication.loginWithSso("token-physician01")).thenReturn(PHYSICIAN);

        MvcResult loggedIn = mvc.perform(login("token-physician01").cookie(new Cookie("XSRF-TOKEN", "before-login")))
                .andExpect(status().isOk())
                .andReturn();

        Cookie csrfCookie = loggedIn.getResponse().getCookie("XSRF-TOKEN");
        assertThat(csrfCookie).isNotNull();
        assertThat(csrfCookie.getValue()).isNotBlank().isNotEqualTo("before-login");
    }

    @Test
    void anInvalidTokenIsUnauthorized() throws Exception {
        when(authentication.loginWithSso("token-unknown")).thenThrow(
                new AuthenticationFailedException(Reason.INVALID_TOKEN, "The corporate login was not accepted"));

        mvc.perform(login("token-unknown"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid_token"))
                .andExpect(jsonPath("$.message").value("The corporate login was not accepted"))
                .andExpect(request().sessionAttributeDoesNotExist("SPRING_SECURITY_CONTEXT"));
    }

    @Test
    void anIdentityWithoutRoleIsForbidden() throws Exception {
        when(authentication.loginWithSso("token-norole01")).thenThrow(new AuthenticationFailedException(
                Reason.NO_ROLE, "Your corporate user has no access to this application"));

        mvc.perform(login("token-norole01"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("no_role"))
                .andExpect(request().sessionAttributeDoesNotExist("SPRING_SECURITY_CONTEXT"));
    }

    @Test
    void anUnavailableSsoGivesAClearMessage() throws Exception {
        when(authentication.loginWithSso("token-patient01")).thenThrow(new SsoUnavailableException("down"));

        mvc.perform(login("token-patient01"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("sso_unavailable"))
                .andExpect(jsonPath("$.message").value("Corporate login is not available, try again later"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "{}", "{\"token\":\"\"}", "{\"token\":\"   \"}", "not json"})
    void aLoginWithoutTokenIsBadRequest(String body) throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("bad_request"));

        verifyNoInteractions(authentication);
    }

    @Test
    void meWithoutSessionIsUnauthorizedWithJson() throws Exception {
        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthenticated"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/users", "/api/catalogue/test-types", "/api/anything"})
    void everyApiEndpointNeedsASession(String path) throws Exception {
        mvc.perform(get(path))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthenticated"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/", "/index.html", "/js/api.js", "/cu-18/index.html"})
    void theStaticClientIsPublic(String path) throws Exception {
        // The static files live in radio-app, so here the answer is 404: what matters is that it is not 401 or 403.
        mvc.perform(get(path)).andExpect(status().isNotFound());
    }

    @Test
    void logoutWithTheCsrfHeaderEndsTheSession() throws Exception {
        when(authentication.loginWithSso("token-physician01")).thenReturn(PHYSICIAN);
        MockHttpSession session = new MockHttpSession();
        MvcResult loggedIn = mvc.perform(login("token-physician01").session(session)).andReturn();
        Cookie csrfCookie = loggedIn.getResponse().getCookie("XSRF-TOKEN");

        // What the shared js/api.js does: send back the value of the cookie in the header.
        mvc.perform(post("/api/auth/logout").session(session).cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue()))
                .andExpect(status().isNoContent());

        verify(authentication).logout();
        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    void logoutClearsTheCsrfCookie() throws Exception {
        when(authentication.loginWithSso("token-physician01")).thenReturn(PHYSICIAN);
        MockHttpSession session = new MockHttpSession();
        MvcResult loggedIn = mvc.perform(login("token-physician01").session(session)).andReturn();
        Cookie csrfCookie = loggedIn.getResponse().getCookie("XSRF-TOKEN");

        mvc.perform(post("/api/auth/logout").session(session).cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue()))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("XSRF-TOKEN", 0));
    }

    @Test
    void logoutWithoutTheCsrfHeaderIsForbidden() throws Exception {
        when(authentication.loginWithSso("token-physician01")).thenReturn(PHYSICIAN);
        MockHttpSession session = new MockHttpSession();
        MvcResult loggedIn = mvc.perform(login("token-physician01").session(session)).andReturn();
        Cookie csrfCookie = loggedIn.getResponse().getCookie("XSRF-TOKEN");

        mvc.perform(post("/api/auth/logout").session(session).cookie(csrfCookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("forbidden"));

        assertThat(session.isInvalid()).isFalse();
    }

    private static MockHttpServletRequestBuilder login(String token) {
        return post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"token\":\"" + token + "\"}");
    }
}
