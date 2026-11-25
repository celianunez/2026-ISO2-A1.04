package es.uclm.sescam.catalogue.api;

/**
 * A clinical guide of a test type (CU-05).
 *
 * @param testTypeCode code of the test type the guide belongs to
 * @param title        title of the guide
 * @param content      text of the guide
 */
public record ClinicalGuideDto(String testTypeCode, String title, String content) {
}
