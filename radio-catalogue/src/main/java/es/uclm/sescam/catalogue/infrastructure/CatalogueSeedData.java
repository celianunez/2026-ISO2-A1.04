package es.uclm.sescam.catalogue.infrastructure;

import es.uclm.sescam.catalogue.api.Modality;
import es.uclm.sescam.catalogue.api.TestTypeData;
import es.uclm.sescam.catalogue.domain.TestType;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Loads a small catalogue of fictitious test types for local development and the demo (CU-23-S).
 *
 * <p>It only runs in the {@code dev} and {@code demo} profiles, like the H2 databases of
 * {@code radio-app}. It does nothing when the catalogue already has data (a restore of the demo
 * database, for example), and it never deletes anything. The values are invented: no real patient or
 * clinical data is involved.
 *
 * <p>The types are created through {@link TestType#create} so that the seed obeys the same rules as
 * an administrator's input. They are not audited because no user performed a change: this is the
 * initial load of the store, not an operation on clinical data.
 */
@Component
@Profile({"dev", "demo"})
public class CatalogueSeedData implements CommandLineRunner {

    private final TestTypeRepository repository;

    /**
     * Creates the seed loader.
     *
     * @param repository the store of test types
     */
    public CatalogueSeedData(TestTypeRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }
        List<TestType> fictitious = List.of(
                TestType.create("RX-THORAX", new TestTypeData("Chest X-ray", Modality.XRAY, "RX-1", 10,
                        "Remove metal objects", false, false, "", true)),
                TestType.create("US-ABD", new TestTypeData("Abdominal ultrasound", Modality.ULTRASOUND,
                        "US-2", 20, "", false, false, "", true)),
                TestType.create("CT-HEAD", new TestTypeData("Head CT", Modality.CT, "CT-1", 25,
                        "", false, false, "Pregnancy", false)),
                TestType.create("MG-BREAST", new TestTypeData("Mammography", Modality.MAMMOGRAPHY,
                        "MG-1", 15, "", false, true, "", true)),
                TestType.create("MR-KNEE", new TestTypeData("Knee MRI", Modality.MRI, "MR-1", 40,
                        "Remove metal objects", false, false, "Pacemaker", false)));
        fictitious.forEach(repository::save);
    }
}
