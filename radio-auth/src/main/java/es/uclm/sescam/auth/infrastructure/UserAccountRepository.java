package es.uclm.sescam.auth.infrastructure;

import es.uclm.sescam.auth.domain.UserAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** Persistence operations for local user accounts. */
public interface UserAccountRepository extends Repository<UserAccount, String> {

    Optional<UserAccount> findById(String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select account from UserAccount account where account.id = :id")
    Optional<UserAccount> findForRoleChange(@Param("id") String id);

    List<UserAccount> findByIdContainingIgnoreCaseOrDisplayNameContainingIgnoreCaseOrderByDisplayNameAscIdAsc(
            String id, String displayName);

    boolean existsById(String id);

    UserAccount save(UserAccount account);
}
