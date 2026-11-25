package es.uclm.sescam.catalogue.infrastructure;

import es.uclm.sescam.catalogue.api.Modality;
import es.uclm.sescam.catalogue.domain.TestType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.Profile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Rules of the initial load of the catalogue: fictitious data, all five modalities, and no change
 * when the store already has test types.
 */
class CatalogueSeedDataTest {

    private final TestTypeRepository repository = mock(TestTypeRepository.class);
    private final CatalogueSeedData seed = new CatalogueSeedData(repository);

    @Test
    void anEmptyCatalogueGetsFictitiousTestTypesOfEveryModality() {
        when(repository.count()).thenReturn(0L);

        seed.run();

        ArgumentCaptor<TestType> saved = ArgumentCaptor.forClass(TestType.class);
        verify(repository, times(5)).save(saved.capture());
        List<TestType> types = saved.getAllValues();
        assertThat(types).extracting(TestType::getCode)
                .containsExactly("RX-THORAX", "US-ABD", "CT-HEAD", "MG-BREAST", "MR-KNEE");
        assertThat(types).extracting(TestType::getModality)
                .containsExactly(Modality.XRAY, Modality.ULTRASOUND, Modality.CT,
                        Modality.MAMMOGRAPHY, Modality.MRI);
        assertThat(types).allSatisfy(type -> assertThat(type.isActive()).isTrue());
    }

    @Test
    void anExistingCatalogueIsLeftUntouched() {
        when(repository.count()).thenReturn(3L);

        seed.run();

        verify(repository).count();
        verify(repository, never()).save(any());
        verifyNoMoreInteractions(repository);
    }

    @Test
    void itOnlyRunsInDevelopmentAndDemo() {
        Profile profile = CatalogueSeedData.class.getAnnotation(Profile.class);

        assertThat(profile).isNotNull();
        assertThat(profile.value()).containsExactlyInAnyOrder("dev", "demo");
    }
}
