package es.uclm.sescam.catalogue.domain;

import es.uclm.sescam.catalogue.api.InvalidTestTypeException;
import es.uclm.sescam.catalogue.api.Modality;
import es.uclm.sescam.catalogue.api.TestTypeData;
import es.uclm.sescam.catalogue.api.TestTypeDto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.util.Locale;

/**
 * A test type of the catalogue (HU-23): what the physician prescribes and what admissions books.
 *
 * <p>The code is the identifier and never changes. Deactivation is logical: {@link #deactivate()}
 * only clears the active flag, so old requests keep their history while the type disappears from
 * {@code listActiveTestTypes} and can no longer be requested (A2 of the specification).
 *
 * <p>All the business rules live here, so they are enforced whatever the entry point is (REST,
 * seed data or another module calling {@code ICatalogueService}).
 */
@Entity
@Table(name = "test_type")
public class TestType implements Persistable<String> {

    /** Longest code that is stored, for example {@code RX-THORAX}. */
    public static final int MAX_CODE_LENGTH = 32;

    /** Longest name that is stored. */
    public static final int MAX_NAME_LENGTH = 120;

    /** Longest room or equipment code that is stored. */
    public static final int MAX_ROOM_LENGTH = 32;

    /** Longest preparation instructions that are stored. */
    public static final int MAX_PREPARATION_LENGTH = 500;

    /** Longest safety restrictions that are stored. */
    public static final int MAX_RESTRICTIONS_LENGTH = 500;

    @Id
    @Column(name = "code", nullable = false, length = MAX_CODE_LENGTH)
    private String code;

    @Column(name = "name", nullable = false, length = MAX_NAME_LENGTH)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "modality", nullable = false, length = 16)
    private Modality modality;

    @Column(name = "room", nullable = false, length = MAX_ROOM_LENGTH)
    private String room;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "preparation_instructions", length = MAX_PREPARATION_LENGTH)
    private String preparationInstructions;

    @Column(name = "needs_contrast", nullable = false)
    private boolean needsContrast;

    @Column(name = "needs_consent", nullable = false)
    private boolean needsConsent;

    @Column(name = "restrictions", length = MAX_RESTRICTIONS_LENGTH)
    private String restrictions;

    @Column(name = "suitable_for_follow_up", nullable = false)
    private boolean suitableForFollowUp;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    /**
     * {@code true} until the entity is stored or loaded. The code is chosen by the user, so without
     * this flag Spring Data would treat every new test type as existing and {@code save} would merge
     * it, silently overwriting a test type created at the same moment with the same code. With it,
     * a new test type is persisted and such a race fails on the primary key.
     */
    @Transient
    private boolean isNew = true;

    /** For JPA only. */
    protected TestType() {
    }

    /** Marks the entity as stored once JPA has loaded or inserted it. */
    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    /**
     * The identifier for Spring Data: the code.
     *
     * @return the code of the test type
     */
    @Override
    public String getId() {
        return code;
    }

    /**
     * Whether the entity has not been stored yet, so that {@code save} persists instead of merging.
     *
     * @return {@code true} for a test type that is not stored yet
     */
    @Override
    public boolean isNew() {
        return isNew;
    }

    private TestType(String code, TestTypeData data) {
        this.code = code;
        apply(data);
    }

    /**
     * Creates a test type after checking every business rule.
     *
     * @param code unique code; required, upper-case letters, digits and hyphens, at most
     *     {@value #MAX_CODE_LENGTH} characters
     * @param data attributes of the test type; required
     * @return the new test type, always active
     * @throws InvalidTestTypeException if an attribute is missing or a value is not valid
     */
    public static TestType create(String code, TestTypeData data) {
        return new TestType(requireCode(code), data);
    }

    /**
     * Changes the attributes. The code and the active flag do not change.
     *
     * @param data new attributes; required
     * @throws InvalidTestTypeException if an attribute is missing or a value is not valid
     */
    public void changeAttributes(TestTypeData data) {
        apply(data);
    }

    /**
     * Deactivates the test type. Calling it twice changes nothing the second time.
     */
    public void deactivate() {
        this.active = false;
    }

    private void apply(TestTypeData data) {
        if (data == null) {
            throw new InvalidTestTypeException("attributes are required");
        }
        this.name = requireText(data.name(), "name", MAX_NAME_LENGTH);
        if (data.modality() == null) {
            throw new InvalidTestTypeException("modality is required");
        }
        this.modality = data.modality();
        this.room = requireText(data.room(), "room", MAX_ROOM_LENGTH);
        if (data.durationMinutes() <= 0) {
            throw new InvalidTestTypeException("durationMinutes must be greater than 0");
        }
        this.durationMinutes = data.durationMinutes();
        this.preparationInstructions = optionalText(
                data.preparationInstructions(), MAX_PREPARATION_LENGTH, "preparationInstructions");
        this.restrictions = optionalText(data.restrictions(), MAX_RESTRICTIONS_LENGTH, "restrictions");
        this.needsContrast = data.needsContrast();
        this.needsConsent = data.needsConsent();
        this.suitableForFollowUp = data.suitableForFollowUp();
    }

    /**
     * Validates and normalises a code: trimmed and upper case, so {@code rx-thorax} and
     * {@code RX-THORAX} are the same test type.
     *
     * @param code the code as given by the caller
     * @return the normalised code
     * @throws InvalidTestTypeException if it is blank, too long or has another format
     */
    public static String requireCode(String code) {
        String clean = normalize(code);
        if (clean == null) {
            throw new InvalidTestTypeException(
                    "code is required and must contain only upper-case letters, digits and hyphens");
        }
        return clean;
    }

    /**
     * Lenient version of {@link #requireCode(String)} for the read operations: a code that cannot
     * exist matches no test type, so the reads answer "not found" instead of failing (the contract
     * of {@code ICatalogueService.findTestType} says empty when no test type has that code).
     *
     * @param code the code as given by the caller
     * @return the normalised code, or {@code null} when no test type could have that code
     */
    public static String normalize(String code) {
        if (code == null) {
            return null;
        }
        String clean = code.strip().toUpperCase(Locale.ROOT);
        if (clean.isEmpty() || clean.length() > MAX_CODE_LENGTH || !clean.matches("[A-Z0-9][A-Z0-9-]*")) {
            return null;
        }
        return clean;
    }

    private static String requireText(String text, String name, int maxLength) {
        if (text == null || text.isBlank()) {
            throw new InvalidTestTypeException(name + " is required");
        }
        String clean = text.strip();
        if (clean.length() > maxLength) {
            throw new InvalidTestTypeException(name + " is longer than " + maxLength + " characters");
        }
        return clean;
    }

    /** An optional text: {@code null} becomes the empty string, so the DTO never carries null. */
    private static String optionalText(String text, int maxLength, String name) {
        if (text == null) {
            return "";
        }
        String clean = text.strip();
        if (clean.length() > maxLength) {
            throw new InvalidTestTypeException(name + " is longer than " + maxLength + " characters");
        }
        return clean;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public Modality getModality() {
        return modality;
    }

    public String getRoom() {
        return room;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public String getPreparationInstructions() {
        return preparationInstructions;
    }

    public boolean isNeedsContrast() {
        return needsContrast;
    }

    public boolean isNeedsConsent() {
        return needsConsent;
    }

    public String getRestrictions() {
        return restrictions;
    }

    public boolean isSuitableForFollowUp() {
        return suitableForFollowUp;
    }

    public boolean isActive() {
        return active;
    }

    /**
     * @return this test type as the DTO of the public contract
     */
    public TestTypeDto toDto() {
        return new TestTypeDto(code, name, modality, room, durationMinutes, preparationInstructions,
                needsContrast, needsConsent, restrictions, suitableForFollowUp, active);
    }
}
