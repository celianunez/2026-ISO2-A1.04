package es.uclm.sescam.configuration.api;

/**
 * A priority level of the diagnostic requests (P-03: the levels are configured here; requests and the waiting list
 * use them).
 *
 * @param code stable code stored in the requests, for example {@code URGENT}
 * @param name name to show, for example "Urgent"
 * @param rank position in the order of urgency; 1 is the most urgent
 */
public record PriorityLevelDto(String code, String name, int rank) {
}
