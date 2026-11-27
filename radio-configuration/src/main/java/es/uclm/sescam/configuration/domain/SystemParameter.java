package es.uclm.sescam.configuration.domain;

import es.uclm.sescam.configuration.api.AppointmentPolicyDto;
import es.uclm.sescam.configuration.api.InvalidParameterException;
import es.uclm.sescam.configuration.api.OpeningHoursDto;
import es.uclm.sescam.configuration.api.PriorityLevelDto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** A version of a system parameter, effective from a given date. */
@Entity
@Immutable
@Table(name = "system_parameter", indexes = @Index(name = "idx_system_parameter_validity",
        columnList = "parameter_group, parameter_key, valid_from, id"))
public class SystemParameter {

    public enum Group {
        ROOM_SCHEDULE, APPOINTMENT_POLICY, PRIORITY_LEVELS, REMINDER_LEAD_TIME
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "parameter_group", nullable = false, updatable = false)
    private Group parameterGroup;

    @Column(name = "parameter_key", nullable = false, updatable = false)
    private String parameterKey;

    @Column(name = "valid_from", nullable = false, updatable = false)
    private LocalDate validFrom;

    @Lob
    @Column(name = "parameter_value", nullable = false, updatable = false)
    private String value;

    protected SystemParameter() {
    }

    public SystemParameter(Group parameterGroup, String parameterKey, LocalDate validFrom, String value) {
        this.parameterGroup = parameterGroup;
        this.parameterKey = parameterKey;
        this.validFrom = validFrom;
        this.value = value;
    }

    public Long getId() {
        return id;
    }

    public String getValue() {
        return value;
    }

    public static void validateValidityDate(LocalDate validFrom, LocalDate today) {
        if (validFrom == null || validFrom.isBefore(today)) {
            throw new InvalidParameterException("The validity date must be today or later");
        }
    }

    public static List<OpeningHoursDto> validateSchedule(List<OpeningHoursDto> schedule) {
        if (schedule == null) {
            throw new InvalidParameterException("A room schedule is required");
        }
        Set<DayOfWeek> weekdays = EnumSet.noneOf(DayOfWeek.class);
        for (OpeningHoursDto hours : schedule) {
            if (hours == null || hours.weekday() == null || hours.opening() == null || hours.closing() == null) {
                throw new InvalidParameterException("A weekday, opening time and closing time are required");
            }
            if (!hours.closing().isAfter(hours.opening())) {
                throw new InvalidParameterException("The closing time must be after the opening time");
            }
            if (!weekdays.add(hours.weekday())) {
                throw new InvalidParameterException("Opening hours may be defined only once per weekday");
            }
        }
        return schedule.stream().sorted(Comparator.comparing(OpeningHoursDto::weekday)).toList();
    }

    public static void validateAppointmentPolicy(AppointmentPolicyDto policy) {
        if (policy == null || policy.minNoticeHours() < 0 || policy.maxHorizonDays() < 0
                || policy.cancellationDeadlineHours() < 0) {
            throw new InvalidParameterException("Appointment policy values must be non-negative");
        }
        if (policy.minNoticeHours() > 24L * policy.maxHorizonDays()) {
            throw new InvalidParameterException("The minimum booking notice must not exceed the maximum horizon");
        }
    }

    public static List<PriorityLevelDto> validatePriorities(List<PriorityLevelDto> priorities) {
        if (priorities == null || priorities.isEmpty()) {
            throw new InvalidParameterException("At least one priority level is required");
        }
        Set<String> codes = new HashSet<>();
        Set<Integer> ranks = new HashSet<>();
        for (PriorityLevelDto priority : priorities) {
            if (priority == null || priority.code() == null || priority.code().isBlank()
                    || priority.name() == null || priority.name().isBlank() || priority.rank() < 1) {
                throw new InvalidParameterException("Each priority requires a code, a name and a positive rank");
            }
            if (!codes.add(priority.code()) || !ranks.add(priority.rank())) {
                throw new InvalidParameterException("Priority codes and ranks must be unique");
            }
        }
        return priorities.stream().sorted(Comparator.comparingInt(PriorityLevelDto::rank)).toList();
    }

    public static void validateReminderLeadTime(Duration leadTime) {
        if (leadTime == null || leadTime.isZero() || leadTime.isNegative()) {
            throw new InvalidParameterException("The reminder lead time must be positive");
        }
    }
}
