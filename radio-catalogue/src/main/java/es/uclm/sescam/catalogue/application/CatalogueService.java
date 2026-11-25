package es.uclm.sescam.catalogue.application;

import es.uclm.sescam.audit.api.IAuditService;
import es.uclm.sescam.catalogue.api.ClinicalGuideDto;
import es.uclm.sescam.catalogue.api.DuplicateTestTypeException;
import es.uclm.sescam.catalogue.api.ICatalogueService;
import es.uclm.sescam.catalogue.api.TestTypeData;
import es.uclm.sescam.catalogue.api.TestTypeDto;
import es.uclm.sescam.catalogue.domain.TestType;
import es.uclm.sescam.catalogue.infrastructure.TestTypeRepository;
import es.uclm.sescam.common.api.CurrentUser;
import es.uclm.sescam.common.api.NotFoundException;
import es.uclm.sescam.common.api.Role;
import es.uclm.sescam.common.api.UserRef;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of {@link ICatalogueService} (CU-23): the catalogue of test types, with logical
 * deactivation and an audit entry for every change (P-12).
 *
 * <p>Reading is open to every authenticated user; writing is restricted to administrators by
 * {@code @PreAuthorize} on the controller (D-09). The audit entry is recorded after the change is
 * saved, but {@link IAuditService#record} runs in its own transaction (CU-19), so the entry is kept
 * even if the change is later rolled back: in that rare case the log shows an attempted change that
 * was not stored, never a stored change that is missing from the log.
 */
@Service
public class CatalogueService implements ICatalogueService {

    /** Action recorded when a test type is created. */
    static final String CREATE_ACTION = "CREATE";

    /** Action recorded when the attributes of a test type change. */
    static final String UPDATE_ACTION = "UPDATE";

    /** Action recorded when a test type is deactivated. */
    static final String DEACTIVATE_ACTION = "DEACTIVATE";

    private static final String RESOURCE_PREFIX = "catalogue/";

    private final TestTypeRepository repository;
    private final IAuditService audit;

    /**
     * Creates the service.
     *
     * @param repository the store of test types
     * @param audit      the audit log where every change is recorded
     */
    public CatalogueService(TestTypeRepository repository, IAuditService audit) {
        this.repository = repository;
        this.audit = audit;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<TestTypeDto> findTestType(String code) {
        String normalized = TestType.normalize(code);
        if (normalized == null) {
            return Optional.empty();
        }
        return repository.findById(normalized).map(TestType::toDto);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<TestTypeDto> listActiveTestTypes() {
        return repository.findByActiveTrueOrderByName().stream().map(TestType::toDto).toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public boolean isRequestable(String code) {
        String normalized = TestType.normalize(code);
        if (normalized == null) {
            return false;
        }
        return repository.findById(normalized).map(TestType::isActive).orElse(false);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Clinical guides arrive in iteration 6 (CU-05), so the list is always empty for now; the
     * method exists from iteration 1 so that the contract never changes.
     */
    @Override
    @Transactional(readOnly = true)
    public List<ClinicalGuideDto> getGuides(String testTypeCode) {
        return List.of();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public TestTypeDto create(String code, TestTypeData data) {
        String normalized = TestType.requireCode(code);
        UserRef user = currentUser();
        if (repository.findById(normalized).isPresent()) {
            throw new DuplicateTestTypeException(normalized);
        }
        TestType stored = repository.save(TestType.create(normalized, data));
        audit.record(user, CREATE_ACTION, RESOURCE_PREFIX + normalized);
        return stored.toDto();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public TestTypeDto update(String code, TestTypeData data) {
        String normalized = TestType.requireCode(code);
        UserRef user = currentUser();
        TestType testType = requireTestType(normalized);
        testType.changeAttributes(data);
        TestType stored = repository.save(testType);
        audit.record(user, UPDATE_ACTION, RESOURCE_PREFIX + normalized);
        return stored.toDto();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Deactivating an already deactivated test type records no second entry: nothing changed.
     */
    @Override
    @Transactional
    public void deactivate(String code) {
        String normalized = TestType.requireCode(code);
        UserRef user = currentUser();
        TestType testType = requireTestType(normalized);
        if (!testType.isActive()) {
            return;
        }
        testType.deactivate();
        repository.save(testType);
        audit.record(user, DEACTIVATE_ACTION, RESOURCE_PREFIX + normalized);
    }

    private TestType requireTestType(String code) {
        return repository.findById(code).orElseThrow(
                () -> new NotFoundException("No test type with code " + code));
    }

    /**
     * The administrator behind the request, for the audit entry. The controller has already checked
     * the role with {@code @PreAuthorize}; the reference is only what the log stores.
     *
     * @return the current user as an administrator
     */
    private UserRef currentUser() {
        String id = CurrentUser.requireUserId();
        return new UserRef(id, id, Role.ADMINISTRATOR);
    }
}
