package es.uclm.sescam.catalogue.api;

/**
 * A test type of the catalogue, as returned by {@link ICatalogueService}.
 *
 * @param code                    unique code, for example {@code RX-THORAX}
 * @param name                    name of the test
 * @param modality                imaging modality
 * @param room                    code of the room or equipment where the test is done
 * @param durationMinutes         length of the test in minutes
 * @param preparationInstructions what the patient must do before the test; empty if nothing
 * @param needsContrast           whether the test uses a contrast agent
 * @param needsConsent            whether the patient must sign an informed consent
 * @param restrictions            safety restrictions; empty if none
 * @param suitableForFollowUp     whether the test can be ordered as a periodic follow-up
 * @param active                  {@code false} once the test type has been deactivated
 */
public record TestTypeDto(
        String code,
        String name,
        Modality modality,
        String room,
        int durationMinutes,
        String preparationInstructions,
        boolean needsContrast,
        boolean needsConsent,
        String restrictions,
        boolean suitableForFollowUp,
        boolean active) {
}
