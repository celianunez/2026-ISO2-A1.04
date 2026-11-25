package es.uclm.sescam.configuration.api;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

/**
 * Public contract of the system parameters (CU-17): room schedules, appointment policies, priority levels and the
 * reminder lead time.
 *
 * <p>Every change has a validity date and applies only from that date, so appointments that are already booked are
 * never altered. The getters without a date return the values in force today. Only administrators may change the
 * parameters (Spring Security, D-09), and every change is audited.
 */
public interface IConfigurationService {

    /**
     * Returns the opening hours of a room that are in force on a date.
     *
     * @param room room code
     * @param date date for which the schedule is wanted
     * @return the opening hours of each weekday the room opens, from Monday to Sunday; empty if it never opens
     * @throws es.uclm.sescam.common.api.NotFoundException if the room does not exist
     */
    List<OpeningHoursDto> getRoomSchedule(String room, LocalDate date);

    /**
     * Returns the appointment policy in force today.
     *
     * @return the current appointment policy
     */
    AppointmentPolicyDto getAppointmentPolicy();

    /**
     * Returns the priority levels in force today, for example urgent, preferent and routine.
     *
     * @return the priority levels, from the most urgent to the least urgent
     */
    List<PriorityLevelDto> getPriorityLevels();

    /**
     * Returns how long before an appointment the patient receives the reminder (CU-02).
     *
     * @return the reminder lead time in force today
     */
    Duration getReminderLeadTime();

    /**
     * Replaces the opening hours of a room from a date on.
     *
     * @param room      room code
     * @param schedule  new opening hours, at most one entry per weekday
     * @param validFrom first day on which the new schedule applies; today or later
     * @throws InvalidParameterException if a closing time is not after its opening time, a weekday is repeated or
     *                                   {@code validFrom} is in the past
     */
    void updateRoomSchedule(String room, List<OpeningHoursDto> schedule, LocalDate validFrom);

    /**
     * Replaces the appointment policy from a date on.
     *
     * @param policy    new policy
     * @param validFrom first day on which the new policy applies; today or later
     * @throws InvalidParameterException if a value is negative, the minimum notice exceeds the maximum horizon or
     *                                   {@code validFrom} is in the past
     */
    void updateAppointmentPolicy(AppointmentPolicyDto policy, LocalDate validFrom);

    /**
     * Replaces the priority levels from a date on.
     *
     * @param levels    new levels; codes and ranks must be unique
     * @param validFrom first day on which the new levels apply; today or later
     * @throws InvalidParameterException if the list is empty, a code or rank is repeated or {@code validFrom} is in
     *                                   the past
     */
    void updatePriorityLevels(List<PriorityLevelDto> levels, LocalDate validFrom);

    /**
     * Replaces the reminder lead time from a date on.
     *
     * @param leadTime  new lead time; positive
     * @param validFrom first day on which the new lead time applies; today or later
     * @throws InvalidParameterException if the lead time is zero or negative or {@code validFrom} is in the past
     */
    void updateReminderLeadTime(Duration leadTime, LocalDate validFrom);
}
