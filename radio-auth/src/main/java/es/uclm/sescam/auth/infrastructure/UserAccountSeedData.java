package es.uclm.sescam.auth.infrastructure;

import es.uclm.sescam.auth.domain.UserAccount;
import es.uclm.sescam.common.api.Role;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/** Loads fictitious users with the IDs agreed with the SSO simulator in F-2. */
@Component
@Profile({"dev", "demo"})
public class UserAccountSeedData implements CommandLineRunner {

    private final UserAccountRepository accounts;

    public UserAccountSeedData(UserAccountRepository accounts) {
        this.accounts = accounts;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seed("patient01", "Demo Patient One", Role.PATIENT);
        seed("patient02", "Demo Patient Two", Role.PATIENT);
        seed("physician01", "Demo Physician One", Role.PHYSICIAN);
        seed("physician02", "Demo Physician Two", Role.PHYSICIAN);
        seed("admissions01", "Demo Admissions One", Role.ADMISSIONS);
        seed("admin01", "Demo Administrator", Role.ADMINISTRATOR);
        seed("physicianadmin01", "Demo Physician Administrator", Role.PHYSICIAN, Role.ADMINISTRATOR);
        seed("norole01", "Demo No Role");
    }

    private void seed(String id, String name, Role... roles) {
        if (!accounts.existsById(id)) {
            accounts.save(new UserAccount(id, name, true, Set.of(roles)));
        }
    }
}
