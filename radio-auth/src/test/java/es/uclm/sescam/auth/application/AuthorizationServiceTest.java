package es.uclm.sescam.auth.application;

import es.uclm.sescam.audit.api.IAuditService;
import es.uclm.sescam.auth.api.IAuthorizationService;
import es.uclm.sescam.auth.domain.UserAccount;
import es.uclm.sescam.auth.infrastructure.UserAccountRepository;
import es.uclm.sescam.common.api.ForbiddenOperationException;
import es.uclm.sescam.common.api.NotFoundException;
import es.uclm.sescam.common.api.Role;
import es.uclm.sescam.common.api.UserRef;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthorizationServiceTest {

    private static final String PATIENT_ID = "patient01";
    private static final String ADMIN_ID = "admin01";
    private static final UserRef ADMIN = new UserRef(ADMIN_ID, ADMIN_ID, Role.ADMINISTRATOR);
    private final UserAccountRepository accounts = mock(UserAccountRepository.class);
    private final IAuditService audit = mock(IAuditService.class);
    private final EntityManager entityManager = mock(EntityManager.class);
    private final IAuthorizationService authorization = new AuthorizationService(accounts, audit, entityManager);

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsStoredRolesForActiveAccount() {
        when(accounts.findById(PATIENT_ID)).thenReturn(Optional.of(account(true, Role.PATIENT, Role.PHYSICIAN)));

        assertEquals(Set.of(Role.PATIENT, Role.PHYSICIAN), authorization.getRoles(PATIENT_ID));
    }

    @Test
    void returnsNoRolesForUnknownInactiveOrRolelessAccounts() {
        when(accounts.findById("unknown")).thenReturn(Optional.empty());
        when(accounts.findById("inactive")).thenReturn(Optional.of(account(false, Role.PATIENT)));
        when(accounts.findById("norole01")).thenReturn(Optional.of(account(true)));

        assertEquals(Set.of(), authorization.getRoles("unknown"));
        assertEquals(Set.of(), authorization.getRoles("inactive"));
        assertEquals(Set.of(), authorization.getRoles("norole01"));
    }

    @Test
    void assignsRoleAndAuditsOnlyTheChange() {
        authenticate(ADMIN_ID, Role.ADMINISTRATOR);
        UserAccount patient = account(true, Role.PATIENT);
        when(accounts.findForRoleChange(PATIENT_ID)).thenReturn(Optional.of(patient));
        when(accounts.findById(PATIENT_ID)).thenReturn(Optional.of(patient));

        authorization.assignRole(PATIENT_ID, Role.PHYSICIAN);
        authorization.assignRole(PATIENT_ID, Role.PHYSICIAN);

        assertEquals(Set.of(Role.PATIENT, Role.PHYSICIAN), authorization.getRoles(PATIENT_ID));
        var writes = inOrder(entityManager, audit);
        writes.verify(entityManager).flush();
        writes.verify(audit).record(ADMIN, "ASSIGN_ROLE", "user/patient01/roles/PHYSICIAN");
        writes.verifyNoMoreInteractions();
    }

    @Test
    void withdrawsRoleAndAuditsOnlyTheChange() {
        authenticate(ADMIN_ID, Role.ADMINISTRATOR);
        UserAccount patient = account(true, Role.PATIENT, Role.PHYSICIAN);
        when(accounts.findForRoleChange(PATIENT_ID)).thenReturn(Optional.of(patient));

        authorization.removeRole(PATIENT_ID, Role.PHYSICIAN);
        authorization.removeRole(PATIENT_ID, Role.PHYSICIAN);

        assertEquals(Set.of(Role.PATIENT), patient.roles());
        var writes = inOrder(entityManager, audit);
        writes.verify(entityManager).flush();
        writes.verify(audit).record(ADMIN, "REMOVE_ROLE", "user/patient01/roles/PHYSICIAN");
        writes.verifyNoMoreInteractions();
    }

    @Test
    void rejectsUnknownUserWithoutRecordingSuccess() {
        authenticate(ADMIN_ID, Role.ADMINISTRATOR);
        when(accounts.findForRoleChange("unknown")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> authorization.assignRole("unknown", Role.PHYSICIAN));
        assertThrows(NotFoundException.class, () -> authorization.removeRole("unknown", Role.PHYSICIAN));
        verifyNoInteractions(entityManager, audit);
    }

    @Test
    void forbidsDirectRoleChangesByNonAdministrators() {
        authenticate(PATIENT_ID, Role.PATIENT);

        assertThrows(ForbiddenOperationException.class, () -> authorization.assignRole(PATIENT_ID, Role.PHYSICIAN));
        assertThrows(ForbiddenOperationException.class, () -> authorization.removeRole(PATIENT_ID, Role.PATIENT));
        verifyNoInteractions(accounts, entityManager, audit);
    }

    @Test
    void grantsOnlyThePermissionsOfTheUsersRoles() {
        when(accounts.findById(ADMIN_ID)).thenReturn(Optional.of(account(true, Role.PHYSICIAN, Role.ADMINISTRATOR)));
        when(accounts.findById(PATIENT_ID)).thenReturn(Optional.of(account(true, Role.PATIENT)));
        when(accounts.findById("unknown")).thenReturn(Optional.empty());

        assertTrue(authorization.hasPermission(ADMIN_ID, "CATALOGUE_EDIT"));
        assertTrue(authorization.hasPermission(ADMIN_ID, "REPORT_SIGN"));
        assertTrue(authorization.hasPermission(PATIENT_ID, "OWN_HISTORY_READ"));
        assertFalse(authorization.hasPermission(PATIENT_ID, "CATALOGUE_EDIT"));
        assertFalse(authorization.hasPermission(ADMIN_ID, "UNKNOWN_PERMISSION"));
        assertFalse(authorization.hasPermission("unknown", "CATALOGUE_EDIT"));
        assertFalse(authorization.hasPermission(ADMIN_ID, null));
    }

    @Test
    void usesCurrentAuthoritiesUntilTheUserStartsANewSession() {
        when(accounts.findById(PATIENT_ID)).thenReturn(Optional.of(account(true, Role.PATIENT, Role.ADMINISTRATOR)));
        authenticate(PATIENT_ID, Role.PATIENT);

        assertFalse(authorization.hasPermission(PATIENT_ID, "CATALOGUE_EDIT"));
        verifyNoInteractions(accounts);

        SecurityContextHolder.clearContext();
        assertTrue(authorization.hasPermission(PATIENT_ID, "CATALOGUE_EDIT"));
    }

    private static UserAccount account(boolean active, Role... roles) {
        return new UserAccount(PATIENT_ID, "Demo Patient One", active, Set.of(roles));
    }

    private static void authenticate(String id, Role role) {
        var authentication = new UsernamePasswordAuthenticationToken(
                id, null, List.of(new SimpleGrantedAuthority(role.authority())));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
