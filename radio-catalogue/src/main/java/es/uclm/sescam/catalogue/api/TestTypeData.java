package es.uclm.sescam.catalogue.api;

/**
 * Attributes of a test type that an administrator enters when creating or editing it (everything except the code
 * and the active flag).
 *
 * @param name                    name of the test, for example "Chest X-ray"; mandatory
 * @param modality                imaging modality; mandatory
 * @param room                    code of the room or equipment where the test is done, as configured in
 *                                {@code radio-configuration}; mandatory
 * @param durationMinutes         length of the test in minutes; positive
 * @param preparationInstructions what the patient must do before the test (fasting…); empty if nothing
 * @param needsContrast           whether the test uses a contrast agent
 * @param needsConsent            whether the patient must sign an informed consent
 * @param restrictions            safety restrictions, for example pregnancy or pacemaker; empty if none
 * @param suitableForFollowUp     whether the test can be ordered as a periodic follow-up (CU-07)
 */
public record TestTypeData(
        String name,
        Modality modality,
        String room,
        int durationMinutes,
        String preparationInstructions,
        boolean needsContrast,
        boolean needsConsent,
        String restrictions,
        boolean suitableForFollowUp) {
}
