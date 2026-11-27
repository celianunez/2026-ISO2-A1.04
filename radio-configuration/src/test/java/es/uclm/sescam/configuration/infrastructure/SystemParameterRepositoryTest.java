package es.uclm.sescam.configuration.infrastructure;

import es.uclm.sescam.configuration.domain.SystemParameter;
import es.uclm.sescam.configuration.domain.SystemParameter.Group;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@ContextConfiguration(classes = SystemParameterRepositoryTest.PersistenceConfig.class)
class SystemParameterRepositoryTest {

    private static final String ROOM = "TEST-ROOM";

    @Autowired
    private SystemParameterRepository repository;

    @Test
    void returnsRoomScheduleEffectiveOnRequestedDate() {
        var initialDate = LocalDate.of(2026, 11, 27);
        var changeDate = initialDate.plusDays(7);
        var initialHours = "Monday 08:00-14:00";
        var updatedHours = "Monday 09:00-15:00";
        repository.save(new SystemParameter(Group.ROOM_SCHEDULE, ROOM, initialDate, initialHours));
        repository.save(new SystemParameter(Group.ROOM_SCHEDULE, ROOM, changeDate, updatedHours));

        assertEquals(initialHours, hoursEffectiveOn(changeDate.minusDays(1)));
        assertEquals(updatedHours, hoursEffectiveOn(changeDate));
    }

    private String hoursEffectiveOn(LocalDate effectiveDate) {
        return repository.findTopByParameterGroupAndParameterKeyAndValidFromLessThanEqualOrderByValidFromDescIdDesc(
                Group.ROOM_SCHEDULE, ROOM, effectiveDate).orElseThrow().getValue();
    }

    @Configuration(proxyBeanMethods = false)
    @EntityScan(basePackageClasses = SystemParameter.class)
    @EnableJpaRepositories(basePackageClasses = SystemParameterRepository.class)
    static class PersistenceConfig {
    }
}
