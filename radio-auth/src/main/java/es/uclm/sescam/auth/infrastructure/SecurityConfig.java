package es.uclm.sescam.auth.infrastructure;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

import java.io.IOException;

/**
 * Security of the whole application (D-09, CU-20-S).
 *
 * <ul>
 *   <li>Everything under {@code /api/**} needs a session (deny by default), except {@code POST /api/auth/login}.
 *       The static client (HTML, JS, CSS) is public.</li>
 *   <li>Permissions are checked on the server with {@code @PreAuthorize} (method security on).</li>
 *   <li>CSRF: the token travels in the {@code XSRF-TOKEN} cookie and comes back in the {@code X-XSRF-TOKEN} header
 *       (shared {@code js/api.js}). The login is the only POST without it: the client has no token yet.</li>
 *   <li>No login form and no redirects: 401 and 403 answer with JSON {@code { "error", "message" }}.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final PathPatternRequestMatcher.Builder PATHS = PathPatternRequestMatcher.withDefaults();

    /**
     * Where the logged-in user is kept between requests: the HTTP session.
     *
     * @return the repository shared by the filter chain and the login controller
     */
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    /**
     * Rules applied to every request.
     *
     * @param http                      the builder provided by Spring Security
     * @param securityContextRepository where the session keeps the logged-in user
     * @return the filter chain of the application
     * @throws Exception if Spring Security cannot build the chain
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            SecurityContextRepository securityContextRepository) throws Exception {
        http
                .securityContext(context -> context.securityContextRepository(securityContextRepository))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(PATHS.matcher(HttpMethod.POST, "/api/auth/login")).permitAll()
                        .requestMatchers(PATHS.matcher("/api/**")).authenticated()
                        .anyRequest().permitAll())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
                        .ignoringRequestMatchers(PATHS.matcher(HttpMethod.POST, "/api/auth/login"),
                                PATHS.matcher("/h2-console/**")))
                // The H2 console (dev profile only) is drawn in frames of the same origin.
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) -> writeError(response,
                                HttpServletResponse.SC_UNAUTHORIZED, "unauthenticated", "Login required"))
                        .accessDeniedHandler((request, response, exception) -> writeError(response,
                                HttpServletResponse.SC_FORBIDDEN, "forbidden", "You are not allowed to do this")))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable);
        return http.build();
    }

    private static void writeError(HttpServletResponse response, int status, String error, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        // Fixed texts only, so no escaping is needed.
        response.getWriter().write("{\"error\":\"" + error + "\",\"message\":\"" + message + "\"}");
    }
}
