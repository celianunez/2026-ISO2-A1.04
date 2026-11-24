package es.uclm.sescam.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Composition root of the SESCAM Radiodiagnostics modular monolith (D-01, D-19).
 *
 * <p>It only starts the application: it scans the {@code es.uclm.sescam} packages of every module
 * and holds no business logic. Each module keeps its own tables and its own configuration.
 */
@SpringBootApplication(scanBasePackages = "es.uclm.sescam")
@EntityScan("es.uclm.sescam")
@EnableJpaRepositories("es.uclm.sescam")
public class RadioApplication {

    /**
     * @param args command line arguments, used to activate a Spring profile
     */
    public static void main(String[] args) {
        SpringApplication.run(RadioApplication.class, args);
    }
}
