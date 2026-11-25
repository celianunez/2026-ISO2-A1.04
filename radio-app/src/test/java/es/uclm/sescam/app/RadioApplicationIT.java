package es.uclm.sescam.app;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

/**
 * The application context starts with the {@code test} profile: H2 in memory, no external system,
 * no port to bind. If this test fails, no use case can be tested.
 */
@SpringBootTest
@ActiveProfiles("test")
class RadioApplicationIT {

    @Autowired
    private Environment environment;

    @Test
    void startsTheContextWithTheTestProfile() {
        assertNotNull(environment);
        assertTrue(environment.matchesProfiles("test"),
                "The context must run with the test profile, got: "
                        + String.join(",", environment.getActiveProfiles()));
    }
}
