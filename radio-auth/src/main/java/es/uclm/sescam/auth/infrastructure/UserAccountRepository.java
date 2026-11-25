package es.uclm.sescam.auth.infrastructure;

import es.uclm.sescam.auth.domain.UserAccount;
import org.springframework.data.repository.Repository;

import java.util.Optional;

/** Persistence operations for local user accounts. */
public interface UserAccountRepository extends Repository<UserAccount, String> {

    Optional<UserAccount> findById(String id);

    boolean existsById(String id);

    UserAccount save(UserAccount account);
}
