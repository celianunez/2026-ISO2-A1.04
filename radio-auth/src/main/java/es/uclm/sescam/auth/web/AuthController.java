package es.uclm.sescam.auth.web;

import es.uclm.sescam.auth.api.AuthenticatedUserDto;
import es.uclm.sescam.auth.api.AuthenticationFailedException;
import es.uclm.sescam.auth.api.IAuthenticationService;
import es.uclm.sescam.auth.application.AuthenticationService;
import es.uclm.sescam.ssoadapter.api.SsoUnavailableException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Login, current user and logout (CU-20-S). JSON agreed with the client (F-3, CU-20-C): the user is
 * {@code { "id", "displayName", "roles" }} and errors are {@code { "error", "message" }}.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    static final String SSO_UNAVAILABLE_MESSAGE = "Corporate login is not available, try again later";

    private final IAuthenticationService authentication;
    private final SecurityContextRepository securityContextRepository;
    private final SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();

    public AuthController(IAuthenticationService authentication, SecurityContextRepository securityContextRepository) {
        this.authentication = authentication;
        this.securityContextRepository = securityContextRepository;
    }

    /**
     * Logs in with a token of the corporate SSO and starts the session.
     *
     * @param body     {@code { "token": "..." }}
     * @param request  the HTTP request, whose session receives the user
     * @param response the HTTP response
     * @return the logged-in user
     */
    @PostMapping("/login")
    public AuthenticatedUserDto login(@RequestBody LoginRequest body, HttpServletRequest request,
            HttpServletResponse response) {
        if (body.token() == null || body.token().isBlank()) {
            throw new InvalidLoginRequestException("The token is required");
        }
        AuthenticatedUserDto user = authentication.loginWithSso(body.token());

        // A new session id on every login protects against session fixation.
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(AuthenticationService.authenticationFor(user));
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
        return user;
    }

    /**
     * Returns the user of the current session. Without a session Spring Security answers 401 before this method.
     *
     * @return the logged-in user
     */
    @GetMapping("/me")
    public AuthenticatedUserDto me() {
        return authentication.currentUser()
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("Login required"));
    }

    /**
     * Ends the session (audited). Needs the CSRF header like any other POST.
     *
     * @param request  the HTTP request, whose session is invalidated
     * @param response the HTTP response
     */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        Authentication current = SecurityContextHolder.getContext().getAuthentication();
        authentication.logout();
        logoutHandler.logout(request, response, current);
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    ResponseEntity<Map<String, String>> loginRejected(AuthenticationFailedException exception) {
        return switch (exception.reason()) {
            case INVALID_TOKEN -> error(HttpStatus.UNAUTHORIZED, "invalid_token", exception.getMessage());
            case NO_ROLE -> error(HttpStatus.FORBIDDEN, "no_role", exception.getMessage());
        };
    }

    @ExceptionHandler(SsoUnavailableException.class)
    ResponseEntity<Map<String, String>> ssoUnavailable(SsoUnavailableException exception) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, "sso_unavailable", SSO_UNAVAILABLE_MESSAGE);
    }

    @ExceptionHandler({InvalidLoginRequestException.class, HttpMessageNotReadableException.class})
    ResponseEntity<Map<String, String>> badRequest(Exception exception) {
        return error(HttpStatus.BAD_REQUEST, "bad_request", "The body must be { \"token\": \"...\" }");
    }

    private static ResponseEntity<Map<String, String>> error(HttpStatus status, String error, String message) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("error", error);
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }

    /**
     * Body of {@code POST /api/auth/login}.
     *
     * @param token token issued by the corporate SSO
     */
    public record LoginRequest(String token) {
    }

    /** The login body has no token. */
    static final class InvalidLoginRequestException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        InvalidLoginRequestException(String message) {
            super(message);
        }
    }
}
