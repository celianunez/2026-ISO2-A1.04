package es.uclm.sescam.auth.api;

import es.uclm.sescam.audit.api.AuditEntryDto;
import es.uclm.sescam.audit.api.AuditQuery;
import es.uclm.sescam.audit.api.AuditResult;
import es.uclm.sescam.audit.api.IAuditService;
import es.uclm.sescam.audit.application.AuditService;
import es.uclm.sescam.auth.application.AuthorizationService;
import es.uclm.sescam.auth.application.AuthorizationService.UserSummary;
import es.uclm.sescam.auth.domain.UserAccount;
import es.uclm.sescam.auth.infrastructure.UserAccountRepository;
import es.uclm.sescam.common.api.ForbiddenOperationException;
import es.uclm.sescam.common.api.NotFoundException;
import es.uclm.sescam.common.api.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contract test of {@link IAuthorizationService} against the real store (H2), with the real audit log of CU-19-S:
 * a role change is stored <em>and</em> recorded, and it behaves as its Javadoc says. It also runs the queries that
 * the unit test mocks: the locking lookup of a role change and the user search.
 */
@DataJpaTest
@Import({AuthorizationService.class, AuditService.class, AuthorizationServiceContractTest.ScanAllModules.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AuthorizationServiceContractTest {

    private static final String ADMIN_ID = "admin01";

    @Autowired
    private IAuthorizationService authorization;

    @Autowired
    private AuthorizationService service;

    @Autowired
    private IAuditService audit;

    @Autowired
    private UserAccountRepository accounts;

    @Autowired
    private JdbcTemplate jdbc;

    /**
     * The test is not transactional (each change commits on its own, like in production), so the tables are emptied
     * here. This is test cleanup only: the production code has no way to delete accounts or audit entries.
     */
    @BeforeEach
    void loadFictitiousAccounts() {
        jdbc.update("delete from user_account_role");
        jdbc.update("delete from user_account");
        jdbc.update("delete from audit_entry");
        accounts.save(new UserAccount("admin01", "Demo Administrator", true, Set.of(Role.ADMINISTRATOR)));
        accounts.save(new UserAccount("physician01", "Demo Physician One", true, Set.of(Role.PHYSICIAN)));
        accounts.save(new UserAccount("patient01", "Demo Patient One", true, Set.of(Role.PATIENT)));
        accounts.save(new UserAccount("retired01", "Demo Retired Physician", false, Set.of(Role.PHYSICIAN)));
        accounts.save(new UserAccount("norole01", "Demo No Role", true, Set.of()));
        authenticateAs(ADMIN_ID, Role.ADMINISTRATOR);
    }

    @AfterEach
    void clearTheSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private static void authenticateAs(String userId, Role... roles) {
        List<SimpleGrantedAuthority> authorities = Arrays.stream(roles)
                .map(role -> new SimpleGrantedAuthority(role.authority()))
                .toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, "n/a", authorities));
    }

    private List<AuditEntryDto> roleEntriesOf(String userId) {
        return audit.query(new AuditQuery(null, null, null, "user/" + userId + "/roles/"));
    }

    @Test
    void getRolesReturnsTheStoredRolesOfAnActiveUser() {
        assertThat(authorization.getRoles("physician01")).containsExactly(Role.PHYSICIAN);
    }

    @Test
    void getRolesIsEmptyForAnUnknownInactiveRolelessOrBlankUser() {
        assertThat(authorization.getRoles("nobody")).isEmpty();
        assertThat(authorization.getRoles("retired01")).isEmpty();
        assertThat(authorization.getRoles("norole01")).isEmpty();
        assertThat(authorization.getRoles(" ")).isEmpty();
        assertThat(authorization.getRoles(null)).isEmpty();
    }

    @Test
    void anAssignedRoleIsStoredAndRecordedInTheAuditLog() {
        authorization.assignRole("physician01", Role.ADMINISTRATOR);

        assertThat(authorization.getRoles("physician01")).containsExactlyInAnyOrder(Role.PHYSICIAN, Role.ADMINISTRATOR);
        assertThat(roleEntriesOf("physician01")).singleElement().satisfies(entry -> {
            assertThat(entry.userId()).isEqualTo(ADMIN_ID);
            assertThat(entry.role()).contains(Role.ADMINISTRATOR);
            assertThat(entry.action()).isEqualTo("ASSIGN_ROLE");
            assertThat(entry.resource()).isEqualTo("user/physician01/roles/ADMINISTRATOR");
            assertThat(entry.result()).isEqualTo(AuditResult.SUCCESS);
        });
    }

    @Test
    void assigningARoleTheUserAlreadyHasChangesNothingAndIsNotAudited() {
        authorization.assignRole("physician01", Role.PHYSICIAN);

        assertThat(authorization.getRoles("physician01")).containsExactly(Role.PHYSICIAN);
        assertThat(roleEntriesOf("physician01")).isEmpty();
    }

    @Test
    void aWithdrawnRoleIsRemovedAndRecordedInTheAuditLog() {
        authorization.removeRole("patient01", Role.PATIENT);

        assertThat(authorization.getRoles("patient01")).isEmpty();
        assertThat(roleEntriesOf("patient01")).singleElement().satisfies(entry -> {
            assertThat(entry.action()).isEqualTo("REMOVE_ROLE");
            assertThat(entry.resource()).isEqualTo("user/patient01/roles/PATIENT");
        });
    }

    @Test
    void withdrawingARoleTheUserDoesNotHaveChangesNothingAndIsNotAudited() {
        authorization.removeRole("patient01", Role.ADMINISTRATOR);

        assertThat(authorization.getRoles("patient01")).containsExactly(Role.PATIENT);
        assertThat(roleEntriesOf("patient01")).isEmpty();
    }

    @Test
    void changingTheRolesOfAnUnknownUserIsNotFoundAndNotAudited() {
        assertThatThrownBy(() -> authorization.assignRole("nobody", Role.PATIENT))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> authorization.removeRole("nobody", Role.PATIENT))
                .isInstanceOf(NotFoundException.class);

        assertThat(roleEntriesOf("nobody")).isEmpty();
    }

    @Test
    void aNonAdministratorCannotChangeRolesEvenCallingTheServiceDirectly() {
        authenticateAs("physician01", Role.PHYSICIAN);

        assertThatThrownBy(() -> authorization.assignRole("physician01", Role.ADMINISTRATOR))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> authorization.removeRole("admin01", Role.ADMINISTRATOR))
                .isInstanceOf(ForbiddenOperationException.class);

        assertThat(authorization.getRoles("physician01")).containsExactly(Role.PHYSICIAN);
        assertThat(authorization.getRoles("admin01")).containsExactly(Role.ADMINISTRATOR);
        assertThat(audit.query(AuditQuery.all())).isEmpty();
    }

    @Test
    void searchMatchesIdOrDisplayNameIgnoringCaseOrderedByNameAndIsAudited() {
        List<UserSummary> found = service.searchUsers("PHYSICIAN");

        assertThat(found).extracting(UserSummary::id).containsExactly("physician01", "retired01");
        assertThat(found.get(1).active()).isFalse();
        assertThat(service.searchUsers("one")).extracting(UserSummary::id)
                .containsExactly("patient01", "physician01");
        assertThat(service.searchUsers("")).hasSize(5);

        assertThat(audit.query(new AuditQuery(ADMIN_ID, null, null, "user/search"))).hasSize(3)
                .allSatisfy(entry -> assertThat(entry.action()).isEqualTo("READ"));
    }

    @Test
    void searchIsForAdministratorsOnly() {
        authenticateAs("patient01", Role.PATIENT);

        assertThatThrownBy(() -> service.searchUsers("")).isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void hasPermissionFollowsTheStoredRolesOfAnotherUser() {
        assertThat(authorization.hasPermission("physician01", "TEST_PRESCRIBE")).isTrue();
        assertThat(authorization.hasPermission("physician01", "CATALOGUE_EDIT")).isFalse();
        assertThat(authorization.hasPermission("retired01", "TEST_PRESCRIBE")).isFalse();
        assertThat(authorization.hasPermission("nobody", "TEST_PRESCRIBE")).isFalse();
        assertThat(authorization.hasPermission("physician01", "NOT_A_PERMISSION")).isFalse();
    }

    @Test
    void aRoleChangeTakesEffectFromTheUsersNextSession() {
        authorization.removeRole("admin01", Role.ADMINISTRATOR);

        assertThat(authorization.getRoles("admin01")).isEmpty();
        assertThat(authorization.hasPermission("admin01", "CATALOGUE_EDIT"))
                .as("the current session keeps its authorities").isTrue();

        authenticateAs("physician01", Role.PHYSICIAN);
        assertThat(authorization.hasPermission("admin01", "CATALOGUE_EDIT"))
                .as("seen from another session the change already applies").isFalse();
    }

    /** {@code @DataJpaTest} only scans this module; the audit entity and repository of CU-19-S live in radio-audit. */
    @TestConfiguration
    @EntityScan(basePackages = "es.uclm.sescam")
    @EnableJpaRepositories(basePackages = "es.uclm.sescam")
    static class ScanAllModules {
    }
}
