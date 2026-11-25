package es.uclm.sescam.configuration.api;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Opening hours of a room on one weekday.
 *
 * @param weekday day of the week
 * @param opening time the room opens
 * @param closing time the room closes; after {@code opening}
 */
public record OpeningHoursDto(DayOfWeek weekday, LocalTime opening, LocalTime closing) {
}
