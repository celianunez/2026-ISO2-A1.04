package es.uclm.sescam.auth.web;

import es.uclm.sescam.auth.application.AuthorizationService;
import es.uclm.sescam.auth.application.AuthorizationService.UserSummary;
import es.uclm.sescam.common.api.DomainException;
import es.uclm.sescam.common.api.NotFoundException;
import es.uclm.sescam.common.api.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP behaviour of {@code /api/users} (CU-18-S): every endpoint is for administrators only, even when the API
 * is called directly, and an unknown user is 404.
 *
 * <p>The real advice that maps domain exceptions to HTTP lives in {@code radio-app} and is verified by
 * {@code ApiExceptionHandlerTest}; {@link ErrorAdvice} reproduces its mapping for this slice.
 */
@WebMvcTest(UserRoleController.class)
@Import({UserRoleControllerTest.MethodSecurityConfig.class, UserRoleControllerTest.ErrorAdvice.class})
class UserRoleControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AuthorizationService authorization;

    @Test
    @WithMockUser(username = "admin01", roles = "ADMINISTRATOR")
    void administratorSearchesUsers() throws Exception {
        when(authorization.searchUsers("phys")).thenReturn(List.of(
                new UserSummary("physician01", "Demo Physician One", true, Set.of(Role.PHYSICIAN))));

        mvc.perform(get("/api/users").param("query", "phys"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("physician01"))
                .andExpect(jsonPath("$[0].displayName").value("Demo Physician One"))
                .andExpect(jsonPath("$[0].active").value(true))
                .andExpect(jsonPath("$[0].roles[0]").value("PHYSICIAN"));
    }

    @Test
    @WithMockUser(username = "admin01", roles = "ADMINISTRATOR")
    void searchWithoutQueryListsEveryone() throws Exception {
        when(authorization.searchUsers("")).thenReturn(List.of());

        mvc.perform(get("/api/users")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));

        verify(authorization).searchUsers("");
    }

    @Test
    @WithMockUser(username = "admin01", roles = "ADMINISTRATOR")
    void administratorAssignsARole() throws Exception {
        mvc.perform(put("/api/users/physician01/roles/ADMINISTRATOR").with(csrf()))
                .andExpect(status().isNoContent());

        verify(authorization).assignRole("physician01", Role.ADMINISTRATOR);
    }

    @Test
    @WithMockUser(username = "admin01", roles = "ADMINISTRATOR")
    void administratorWithdrawsARole() throws Exception {
        mvc.perform(delete("/api/users/physicianadmin01/roles/ADMINISTRATOR").with(csrf()))
                .andExpect(status().isNoContent());

        verify(authorization).removeRole("physicianadmin01", Role.ADMINISTRATOR);
    }

    @Test
    @WithMockUser(username = "admin01", roles = "ADMINISTRATOR")
    void changingTheRolesOfAnUnknownUserIsNotFound() throws Exception {
        doThrow(new NotFoundException("No user with id nobody")).when(authorization)
                .assignRole("nobody", Role.PATIENT);
        doThrow(new NotFoundException("No user with id nobody")).when(authorization)
                .removeRole("nobody", Role.PATIENT);

        mvc.perform(put("/api/users/nobody/roles/PATIENT").with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"));
        mvc.perform(delete("/api/users/nobody/roles/PATIENT").with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin01", roles = "ADMINISTRATOR")
    void anUnknownRoleIsBadRequest() throws Exception {
        mvc.perform(put("/api/users/physician01/roles/RADIOLOGIST").with(csrf()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authorization);
    }

    @ParameterizedTest
    @ValueSource(strings = {"PATIENT", "PHYSICIAN", "ADMISSIONS"})
    void aNonAdministratorIsForbiddenOnEveryEndpoint(String role) throws Exception {
        mvc.perform(get("/api/users").with(user("someone").roles(role)))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/users/patient01/roles/ADMINISTRATOR").with(user("someone").roles(role)).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/users/admin01/roles/ADMINISTRATOR").with(user("someone").roles(role)).with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(authorization);
    }

    @Test
    void anAnonymousUserIsUnauthorized() throws Exception {
        mvc.perform(get("/api/users")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/users/patient01/roles/ADMINISTRATOR").with(csrf()))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/users/admin01/roles/ADMINISTRATOR").with(csrf()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(authorization);
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityConfig {
    }

    /** Same mapping as {@code radio-app}'s {@code ApiExceptionHandler}: 404 / 400 with {@code {error, message}}. */
    @RestControllerAdvice
    static class ErrorAdvice {

        @ExceptionHandler(NotFoundException.class)
        ResponseEntity<Map<String, String>> notFound(NotFoundException exception) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "not_found", "message", exception.getMessage()));
        }

        @ExceptionHandler(DomainException.class)
        ResponseEntity<Map<String, String>> badRequest(DomainException exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "bad_request", "message", exception.getMessage()));
        }
    }
}
