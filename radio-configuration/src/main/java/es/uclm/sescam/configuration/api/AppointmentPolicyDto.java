package es.uclm.sescam.configuration.api;

/**
 * Rules for booking and cancelling appointments.
 *
 * @param minNoticeHours            minimum time between booking and appointment, in hours
 * @param maxHorizonDays            how far ahead an appointment can be booked, in days
 * @param cancellationDeadlineHours minimum time before an appointment to cancel it, in hours
 */
public record AppointmentPolicyDto(int minNoticeHours, int maxHorizonDays, int cancellationDeadlineHours) {
}
