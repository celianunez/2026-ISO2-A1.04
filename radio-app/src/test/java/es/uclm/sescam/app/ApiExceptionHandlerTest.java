package es.uclm.sescam.app;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import es.uclm.sescam.common.api.DomainException;
import es.uclm.sescam.common.api.ForbiddenOperationException;
import es.uclm.sescam.common.api.NotFoundException;

/**
 * The JSON contract of {@link ApiExceptionHandler}: {@code { "error": ..., "message": ... }} with
 * 404, 403 and 400. It uses a standalone {@link MockMvc} with a controller that only throws, so
 * the test does not need the Spring Security or the persistence configuration.
 */
class ApiExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FailingController())
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void mapsNotFoundTo404() throws Exception {
        mockMvc.perform(get("/tests/42"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"))
                .andExpect(jsonPath("$.message").value("Test type 42 not found"));
    }

    @Test
    void mapsForbiddenOperationTo403() throws Exception {
        mockMvc.perform(get("/tests"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("forbidden"))
                .andExpect(jsonPath("$.message").value("Only an administrator may list test types"));
    }

    @Test
    void mapsDomainExceptionTo400() throws Exception {
        mockMvc.perform(post("/tests").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"T01\",\"code\":\"T01\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("bad_request"))
                .andExpect(jsonPath("$.message").value("Test type T01 already exists"));
    }

    @Test
    void mapsValidationErrorsTo400WithTheViolatedField() throws Exception {
        mockMvc.perform(post("/tests/validate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_error"))
                .andExpect(jsonPath("$.message").value("code: must not be blank"));
    }

    /** Controller that only raises the exceptions the advice has to translate. */
    @RestController
    static class FailingController {

        @GetMapping("/tests/{id}")
        public void find() {
            throw new NotFoundException("Test type 42 not found");
        }

        @GetMapping("/tests")
        public void list() {
            throw new ForbiddenOperationException("Only an administrator may list test types");
        }

        @PostMapping("/tests")
        public void create() {
            throw new DomainException("Test type T01 already exists");
        }

        @PostMapping("/tests/validate")
        public void validate(@Valid @RequestBody TestTypeCommand command) {
            // The binding of @Valid is what raises the exception; the body is never reached.
        }
    }

    record TestTypeCommand(@NotBlank String code) {
    }
}
