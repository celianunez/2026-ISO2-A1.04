package es.uclm.sescam.catalogue.web;

import es.uclm.sescam.catalogue.api.DuplicateTestTypeException;
import es.uclm.sescam.catalogue.api.ICatalogueService;
import es.uclm.sescam.catalogue.api.InvalidTestTypeException;
import es.uclm.sescam.catalogue.api.Modality;
import es.uclm.sescam.catalogue.api.TestTypeData;
import es.uclm.sescam.catalogue.api.TestTypeDto;
import es.uclm.sescam.common.api.DomainException;
import es.uclm.sescam.common.api.NotFoundException;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP behaviour of {@code /api/catalogue/test-types}: reads for every authenticated role, writes for
 * administrators only, and the status of each operation of CU-23.
 *
 * <p>The real advice that maps domain exceptions to HTTP lives in {@code radio-app} and is verified by
 * its own test. This slice imports an identical mapping so that the statuses the client will receive
 * can be asserted here without depending on the application module.
 */
@WebMvcTest(CatalogueController.class)
@Import({CatalogueControllerTest.MethodSecurityConfig.class, CatalogueControllerTest.ErrorAdvice.class})
class CatalogueControllerTest {

    private static final String BASE = "/api/catalogue/test-types";

    /** Attributes of a valid test type as the client sends them (the code travels in the path). */
    private static final String VALID_JSON = """
            {
              "name": "Chest X-ray",
              "modality": "XRAY",
              "room": "RX-1",
              "durationMinutes": 10,
              "preparationInstructions": "Remove metal objects",
              "needsContrast": false,
              "needsConsent": false,
              "restrictions": "",
              "suitableForFollowUp": true
            }
            """;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ICatalogueService catalogueService;

    @Test
    @WithMockUser(username = "u-phys-01", roles = "PHYSICIAN")
    void aPhysicianCanReadTheCatalogue() throws Exception {
        // The service already filters out deactivated types: the controller just returns its list.
        when(catalogueService.listActiveTestTypes()).thenReturn(List.of(active()));

        mvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value("RX-THORAX"))
                .andExpect(jsonPath("$[0].name").value("Chest X-ray"))
                .andExpect(jsonPath("$[0].modality").value("XRAY"))
                .andExpect(jsonPath("$[0].active").value(true));

        verify(catalogueService).listActiveTestTypes();
    }

    @ParameterizedTest
    @ValueSource(strings = {"PATIENT", "PHYSICIAN", "ADMISSIONS", "ADMINISTRATOR"})
    void anyAuthenticatedUserCanReadOneTestType(String role) throws Exception {
        when(catalogueService.findTestType("RX-THORAX")).thenReturn(Optional.of(active()));

        mvc.perform(get(BASE + "/RX-THORAX").with(user("u-01").roles(role)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("RX-THORAX"));

        verify(catalogueService).findTestType("RX-THORAX");
    }

    @Test
    @WithMockUser(username = "u-admin-01", roles = "ADMINISTRATOR")
    void anUnknownTestTypeIsNotFound() throws Exception {
        when(catalogueService.findTestType("NOPE")).thenReturn(Optional.empty());

        mvc.perform(get(BASE + "/NOPE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"));
    }

    @Test
    @WithMockUser(username = "u-phys-01", roles = "PHYSICIAN")
    void theGuidesEndpointReturnsAnEmptyListUntilIterationSix() throws Exception {
        when(catalogueService.getGuides("RX-THORAX")).thenReturn(List.of());

        mvc.perform(get(BASE + "/RX-THORAX/guides"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(catalogueService).getGuides("RX-THORAX");
    }

    @Test
    @WithMockUser(username = "u-admin-01", roles = "ADMINISTRATOR")
    void anAdministratorCreatesATestTypeWithStatusCreated() throws Exception {
        when(catalogueService.create(eq("RX-THORAX"), any(TestTypeData.class))).thenReturn(active());

        mvc.perform(post(BASE + "/RX-THORAX").with(csrf())
                        .contentType("application/json").content(VALID_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("RX-THORAX"))
                .andExpect(jsonPath("$.name").value("Chest X-ray"))
                .andExpect(jsonPath("$.active").value(true));

        verify(catalogueService).create(eq("RX-THORAX"), any(TestTypeData.class));
    }

    @Test
    @WithMockUser(username = "u-admin-01", roles = "ADMINISTRATOR")
    void anAdministratorUpdatesATestTypeKeepingItsCode() throws Exception {
        TestTypeDto updated = new TestTypeDto("RX-THORAX", "Chest X-ray 2", Modality.CT, "CT-2", 20,
                "", true, true, "Pregnancy", true, true);
        when(catalogueService.update(eq("RX-THORAX"), any(TestTypeData.class))).thenReturn(updated);

        mvc.perform(put(BASE + "/RX-THORAX").with(csrf())
                        .contentType("application/json").content(VALID_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("RX-THORAX"))
                .andExpect(jsonPath("$.name").value("Chest X-ray 2"))
                .andExpect(jsonPath("$.modality").value("CT"));

        verify(catalogueService).update(eq("RX-THORAX"), any(TestTypeData.class));
    }

    @Test
    @WithMockUser(username = "u-admin-01", roles = "ADMINISTRATOR")
    void anAdministratorDeactivatesATestTypeWithNoContent() throws Exception {
        mvc.perform(post(BASE + "/RX-THORAX/deactivate").with(csrf()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(catalogueService).deactivate("RX-THORAX");
    }

    @ParameterizedTest
    @ValueSource(strings = {"PATIENT", "PHYSICIAN", "ADMISSIONS"})
    void aNonAdministratorCannotWrite(String role) throws Exception {
        mvc.perform(post(BASE + "/RX-NEW").with(csrf()).with(user("u-other").roles(role))
                        .contentType("application/json").content(VALID_JSON))
                .andExpect(status().isForbidden());
        mvc.perform(put(BASE + "/RX-THORAX").with(csrf()).with(user("u-other").roles(role))
                        .contentType("application/json").content(VALID_JSON))
                .andExpect(status().isForbidden());
        mvc.perform(post(BASE + "/RX-THORAX/deactivate").with(csrf()).with(user("u-other").roles(role)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(catalogueService);
    }

    @Test
    void anAnonymousUserCannotWrite() throws Exception {
        mvc.perform(post(BASE + "/RX-NEW").with(csrf())
                        .contentType("application/json").content(VALID_JSON))
                .andExpect(status().isUnauthorized());
        mvc.perform(put(BASE + "/RX-THORAX").with(csrf())
                        .contentType("application/json").content(VALID_JSON))
                .andExpect(status().isUnauthorized());
        mvc.perform(post(BASE + "/RX-THORAX/deactivate").with(csrf()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(catalogueService);
    }

    @Test
    @WithMockUser(username = "u-admin-01", roles = "ADMINISTRATOR")
    void invalidAttributesAreBadRequest() throws Exception {
        when(catalogueService.create(eq("RX-NEW"), any(TestTypeData.class)))
                .thenThrow(new InvalidTestTypeException("name is required"));

        mvc.perform(post(BASE + "/RX-NEW").with(csrf())
                        .contentType("application/json").content(VALID_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("bad_request"))
                .andExpect(jsonPath("$.message").value("name is required"));
    }

    @Test
    @WithMockUser(username = "u-admin-01", roles = "ADMINISTRATOR")
    void aDuplicateCodeIsBadRequest() throws Exception {
        when(catalogueService.create(eq("RX-THORAX"), any(TestTypeData.class)))
                .thenThrow(new DuplicateTestTypeException("RX-THORAX"));

        mvc.perform(post(BASE + "/RX-THORAX").with(csrf())
                        .contentType("application/json").content(VALID_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("bad_request"));
    }

    @Test
    @WithMockUser(username = "u-admin-01", roles = "ADMINISTRATOR")
    void changingOrDeactivatingAnUnknownCodeIsNotFound() throws Exception {
        when(catalogueService.update(eq("NOPE"), any(TestTypeData.class)))
                .thenThrow(new NotFoundException("No test type with code NOPE"));
        org.mockito.Mockito.doThrow(new NotFoundException("No test type with code NOPE"))
                .when(catalogueService).deactivate("NOPE");

        mvc.perform(put(BASE + "/NOPE").with(csrf())
                        .contentType("application/json").content(VALID_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"));
        mvc.perform(post(BASE + "/NOPE/deactivate").with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "u-admin-01", roles = "ADMINISTRATOR")
    void aTestTypeCannotBeDeleted() throws Exception {
        mvc.perform(delete(BASE + "/RX-THORAX").with(csrf())).andExpect(status().isMethodNotAllowed());

        verifyNoInteractions(catalogueService);
    }

    private static TestTypeDto active() {
        return new TestTypeDto("RX-THORAX", "Chest X-ray", Modality.XRAY, "RX-1", 10,
                "Remove metal objects", false, false, "", true, true);
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityConfig {
    }

    /** Mirror of {@code radio-app}'s advice: only the mapping of domain errors to HTTP status. */
    @RestControllerAdvice
    static class ErrorAdvice {

        @ExceptionHandler(NotFoundException.class)
        ResponseEntity<Map<String, String>> notFound(NotFoundException exception) {
            return body(HttpStatus.NOT_FOUND, "not_found", exception.getMessage());
        }

        @ExceptionHandler(DomainException.class)
        ResponseEntity<Map<String, String>> badRequest(DomainException exception) {
            return body(HttpStatus.BAD_REQUEST, "bad_request", exception.getMessage());
        }

        private ResponseEntity<Map<String, String>> body(HttpStatus status, String error, String message) {
            Map<String, String> payload = new LinkedHashMap<>();
            payload.put("error", error);
            payload.put("message", message);
            return ResponseEntity.status(status).body(payload);
        }
    }
}
