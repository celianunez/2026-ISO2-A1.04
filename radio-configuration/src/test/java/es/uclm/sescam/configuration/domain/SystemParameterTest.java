package es.uclm.sescam.configuration.domain;

import es.uclm.sescam.configuration.api.InvalidParameterException;
import es.uclm.sescam.configuration.api.OpeningHoursDto;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

class SystemParameterTest {

    @Test
    void rejectsClosingBeforeOpening() {
        var hours = new OpeningHoursDto(DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(9, 0));

        assertThrows(InvalidParameterException.class, () -> SystemParameter.validateSchedule(List.of(hours)));
    }

    @Test
    void rejectsNegativeReminderLeadTime() {
        var leadTime = Duration.ofHours(-1);

        assertThrows(InvalidParameterException.class, () -> SystemParameter.validateReminderLeadTime(leadTime));
    }
}
