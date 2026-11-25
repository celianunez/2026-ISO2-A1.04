package es.uclm.sescam.app;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import es.uclm.sescam.common.api.DomainException;
import es.uclm.sescam.common.api.ForbiddenOperationException;
import es.uclm.sescam.common.api.NotFoundException;

/**
 * Translates the domain exceptions of {@code radio-common} into HTTP responses with a JSON body
 * of the shape {@code { "error": "...", "message": "..." }}.
 *
 * <p>One handler per exception type, from the most specific to the most generic: Spring picks the
 * most specific one, so {@link NotFoundException} never falls into {@link DomainException}.
 *
 * <p>Unhandled exceptions are left to Spring Boot's default handler (HTTP 500), which logs the
 * stack trace; this advice only covers the errors the client is supposed to understand.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    /**
     * @param exception the error raised by a module
     * @return HTTP 404 with the JSON body
     */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, String>> notFound(NotFoundException exception) {
        return body(HttpStatus.NOT_FOUND, "not_found", exception);
    }

    /**
     * @param exception the error raised by a module
     * @return HTTP 403 with the JSON body
     */
    @ExceptionHandler(ForbiddenOperationException.class)
    public ResponseEntity<Map<String, String>> forbidden(ForbiddenOperationException exception) {
        return body(HttpStatus.FORBIDDEN, "forbidden", exception);
    }

    /**
     * @param exception the error raised by a module
     * @return HTTP 400 with the JSON body
     */
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<Map<String, String>> badRequest(DomainException exception) {
        return body(HttpStatus.BAD_REQUEST, "bad_request", exception);
    }

    /**
     * Bean validation of a request body or a request parameter (DTOs are records, so they carry
     * their constraints).
     *
     * @param exception the validation error reported by the binding
     * @return HTTP 400 with the JSON body, message = the first violated field
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> validation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");
        return body(HttpStatus.BAD_REQUEST, "validation_error", exception, message);
    }

    private ResponseEntity<Map<String, String>> body(HttpStatus status, String error,
            Exception exception) {
        return body(status, error, exception, exception.getMessage());
    }

    private ResponseEntity<Map<String, String>> body(HttpStatus status, String error,
            Exception exception, String message) {
        log.debug("{} on {}: {}", error, exception.getClass().getSimpleName(), exception.getMessage());
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("error", error);
        payload.put("message", message == null || message.isBlank() ? status.getReasonPhrase() : message);
        return ResponseEntity.status(status).body(payload);
    }
}
