package es.uclm.sescam.catalogue.application;

import es.uclm.sescam.audit.api.IAuditService;
import es.uclm.sescam.catalogue.api.DuplicateTestTypeException;
import es.uclm.sescam.catalogue.api.InvalidTestTypeException;
import es.uclm.sescam.catalogue.api.Modality;
import es.uclm.sescam.catalogue.api.TestTypeData;
import es.uclm.sescam.catalogue.api.TestTypeDto;
import es.uclm.sescam.catalogue.domain.TestType;
import es.uclm.sescam.catalogue.infrastructure.TestTypeRepository;
import es.uclm.sescam.common.api.ForbiddenOperationException;
import es.uclm.sescam.common.api.NotFoundException;
import es.uclm.sescam.common.api.Role;
import es.uclm.sescam.common.api.UserRef;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Rules of {@link CatalogueService} in isolation: validation, uniqueness, logical deactivation and
 * the audit entry of every change. The behaviour against the real store is
 * {@code CatalogueServiceContractTest}.
 */
class CatalogueServiceTest {

    private static final String ADMIN_ID = "u-admin-01";

    private TestTypeRepository repository;
    private IAuditService audit;
    private CatalogueService service;

    @BeforeEach
    void setUp() {
        repository = mock(TestTypeRepository.class);
        audit = mock(IAuditService.class);
        when(repository.save(any(TestType.class))).thenAnswer(call -> call.getArgument(0));
        authenticateAsAdministrator();
        service = new CatalogueService(repository, audit);
    }

    @AfterEach
    void clearTheSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createsAnActiveTestTypeAndRecordsTheAuditEntry() {
        TestTypeDto created = service.create("RX-THORAX", valid());

        assertThat(created.code()).isEqualTo("RX-THORAX");
        assertThat(created.name()).isEqualTo("Chest X-ray");
        assertThat(created.modality()).isEqualTo(Modality.XRAY);
        assertThat(created.active()).isTrue();
        verify(repository).save(any(TestType.class));
        verify(audit).record(new UserRef(ADMIN_ID, ADMIN_ID, Role.ADMINISTRATOR),
                "CREATE", "catalogue/RX-THORAX");
    }

    @ParameterizedTest
    @MethodSource("invalidAttributes")
    void rejectsAnInvalidTestTypeWithoutStoringOrAuditingIt(TestTypeData invalid) {
        assertThatThrownBy(() -> service.create("RX-NEW", invalid))
                .isInstanceOf(InvalidTestTypeException.class);

        verify(repository, never()).save(any());
        verifyNoInteractions(audit);
    }

    static Stream<TestTypeData> invalidAttributes() {
        return Stream.of(
                new TestTypeData(null, Modality.XRAY, "RX-1", 10, "", false, false, "", false),
                new TestTypeData("  ", Modality.XRAY, "RX-1", 10, "", false, false, "", false),
                new TestTypeData("Chest X-ray", null, "RX-1", 10, "", false, false, "", false),
                new TestTypeData("Chest X-ray", Modality.XRAY, "   ", 10, "", false, false, "", false),
                new TestTypeData("Chest X-ray", Modality.XRAY, "RX-1", 0, "", false, false, "", false),
                new TestTypeData("Chest X-ray", Modality.XRAY, "RX-1", -5, "", false, false, "", false));
    }

    @Test
    void rejectsADuplicateCodeWithoutStoringOrAuditingIt() {
        when(repository.findById("RX-THORAX")).thenReturn(Optional.of(TestType.create("RX-THORAX", valid())));

        assertThatThrownBy(() -> service.create("RX-THORAX", valid()))
                .isInstanceOf(DuplicateTestTypeException.class);

        verify(repository, never()).save(any());
        verifyNoInteractions(audit);
    }

    @Test
    void rejectsAMalformedCode() {
        assertThatThrownBy(() -> service.create("not a code", valid()))
                .isInstanceOf(InvalidTestTypeException.class);
        assertThatThrownBy(() -> service.create("   ", valid()))
                .isInstanceOf(InvalidTestTypeException.class);

        verify(repository, never()).save(any());
        verifyNoInteractions(audit);
    }

    @Test
    void normalisesTheCodeWhenCreating() {
        service.create("  rx-thorax ", valid());

        ArgumentCaptor<TestType> saved = ArgumentCaptor.forClass(TestType.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getCode()).isEqualTo("RX-THORAX");
    }

    @Test
    void updatesTheAttributesKeepingCodeAndActiveFlagAndAuditsIt() {
        TestType existing = TestType.create("RX-THORAX", valid());
        when(repository.findById("RX-THORAX")).thenReturn(Optional.of(existing));

        TestTypeDto updated = service.update("RX-THORAX",
                new TestTypeData("Chest X-ray 2", Modality.CT, "CT-2", 20, "", true, true, "Pregnancy", true));

        assertThat(updated.code()).isEqualTo("RX-THORAX");
        assertThat(updated.name()).isEqualTo("Chest X-ray 2");
        assertThat(updated.modality()).isEqualTo(Modality.CT);
        assertThat(updated.active()).isTrue();
        verify(audit).record(any(UserRef.class), eq("UPDATE"), eq("catalogue/RX-THORAX"));
    }

    @Test
    void updatingAnUnknownTestTypeIsNotFound() {
        when(repository.findById("NOPE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update("NOPE", valid())).isInstanceOf(NotFoundException.class);

        verify(repository, never()).save(any());
        verifyNoInteractions(audit);
    }

    @Test
    void deactivatesLogicallyAndAuditsIt() {
        TestType existing = TestType.create("RX-THORAX", valid());
        when(repository.findById("RX-THORAX")).thenReturn(Optional.of(existing));

        service.deactivate("RX-THORAX");

        assertThat(existing.isActive()).isFalse();
        verify(repository).save(existing);
        verify(audit).record(any(UserRef.class), eq("DEACTIVATE"), eq("catalogue/RX-THORAX"));
    }

    @Test
    void deactivatingTwiceChangesNothingTheSecondTime() {
        TestType alreadyInactive = TestType.create("RX-THORAX", valid());
        alreadyInactive.deactivate();
        when(repository.findById("RX-THORAX")).thenReturn(Optional.of(alreadyInactive));

        service.deactivate("RX-THORAX");

        verify(repository, never()).save(any());
        verifyNoInteractions(audit);
    }

    @Test
    void deactivatingAnUnknownTestTypeIsNotFound() {
        when(repository.findById("NOPE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deactivate("NOPE")).isInstanceOf(NotFoundException.class);

        verify(repository, never()).save(any());
        verifyNoInteractions(audit);
    }

    @Test
    void isRequestableOnlyForActiveAndExistingTestTypes() {
        TestType active = TestType.create("RX-THORAX", valid());
        TestType inactive = TestType.create("US-ABD", valid());
        inactive.deactivate();
        when(repository.findById("RX-THORAX")).thenReturn(Optional.of(active));
        when(repository.findById("US-ABD")).thenReturn(Optional.of(inactive));
        when(repository.findById("MISSING")).thenReturn(Optional.empty());

        assertThat(service.isRequestable("RX-THORAX")).isTrue();
        assertThat(service.isRequestable("US-ABD")).isFalse();
        assertThat(service.isRequestable("MISSING")).isFalse();
        // A code that no test type could have does not even reach the store.
        assertThat(service.isRequestable("not a code")).isFalse();
        verify(repository, never()).findById("NOT A CODE");
    }

    @Test
    void listsOnlyWhatTheRepositoryReturnsAsActiveTestTypes() {
        when(repository.findByActiveTrueOrderByName()).thenReturn(List.of(
                TestType.create("RX-THORAX", valid()),
                TestType.create("US-ABD", valid())));

        assertThat(service.listActiveTestTypes())
                .extracting(TestTypeDto::code)
                .containsExactly("RX-THORAX", "US-ABD");
        verifyNoInteractions(audit);
    }

    @Test
    void findTestTypeIsEmptyForAnUnknownOrMalformedCode() {
        when(repository.findById("MISSING")).thenReturn(Optional.empty());

        assertThat(service.findTestType("MISSING")).isEmpty();
        assertThat(service.findTestType("not a code")).isEmpty();
        verify(repository, never()).findById("NOT A CODE");
    }

    @Test
    void guidesAreEmptyUntilTheSixthIteration() {
        assertThat(service.getGuides("RX-THORAX")).isEmpty();
        verifyNoInteractions(repository, audit);
    }

    @Test
    void aChangeRequiresAnAuthenticatedUser() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> service.create("RX-NEW", valid()))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> service.update("RX-NEW", valid()))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> service.deactivate("RX-NEW"))
                .isInstanceOf(ForbiddenOperationException.class);

        verify(repository, never()).save(any());
        verifyNoInteractions(audit);
    }

    @Test
    void readsDoNotRequireAnAuthenticatedUser() {
        SecurityContextHolder.clearContext();
        when(repository.findByActiveTrueOrderByName()).thenReturn(List.of());

        assertThat(service.listActiveTestTypes()).isEmpty();
        assertThat(service.findTestType("RX-THORAX")).isEmpty();
        assertThat(service.isRequestable("RX-THORAX")).isFalse();
        verifyNoInteractions(audit);
    }

    private static void authenticateAsAdministrator() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                ADMIN_ID, "n/a", List.of(new SimpleGrantedAuthority(Role.ADMINISTRATOR.authority()))));
    }

    private static TestTypeData valid() {
        return new TestTypeData("Chest X-ray", Modality.XRAY, "RX-1", 10, "Remove metal objects",
                false, false, "", true);
    }
}
