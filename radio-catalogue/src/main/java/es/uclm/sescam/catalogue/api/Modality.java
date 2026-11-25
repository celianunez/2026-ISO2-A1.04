package es.uclm.sescam.catalogue.api;

/**
 * Imaging modality of a test type (initial catalogue of the user stories: X-ray, ultrasound, mammography, CT, MRI).
 */
public enum Modality {

    /** Conventional radiography. */
    XRAY,

    /** Ultrasound scan. */
    ULTRASOUND,

    /** Mammography. */
    MAMMOGRAPHY,

    /** Computed tomography. */
    CT,

    /** Magnetic resonance imaging. */
    MRI
}
