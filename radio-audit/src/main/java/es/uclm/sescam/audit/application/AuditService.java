package es.uclm.sescam.audit.application;

import es.uclm.sescam.audit.api.AuditEntryDto;
import es.uclm.sescam.audit.api.AuditQuery;
import es.uclm.sescam.audit.api.AuditResult;
import es.uclm.sescam.audit.api.IAuditService;
import es.uclm.sescam.audit.api.InvalidAuditQueryException;
import es.uclm.sescam.audit.domain.AuditEntry;
import es.uclm.sescam.audit.infrastructure.AuditRepository;
import es.uclm.sescam.common.api.Role;
import es.uclm.sescam.common.api.UserRef;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/** Implementation of {@link IAuditService}: append-only, timestamps from the injected {@link Clock}. */
@Service
public class AuditService implements IAuditService {

    /** Lower bound used when the query has no {@code from}. */
    static final Instant EARLIEST = Instant.EPOCH;

    /** Upper bound used when the query has no {@code to}. */
    static final Instant LATEST = Instant.parse("9999-12-31T23:59:59Z");

    private static final char LIKE_ESCAPE = '!';

    private final AuditRepository repository;
    private final Clock clock;

    /**
     * Creates the service used by Spring. Uses the application's {@link Clock} bean if there is exactly one,
     * otherwise the UTC system clock, so this module never forces {@code radio-app} to define a clock.
     *
     * @param repository the audit store
     * @param clock      provider of the application's clock, if any
     */
    @Autowired
    public AuditService(AuditRepository repository, ObjectProvider<Clock> clock) {
        this(repository, clock.getIfUnique(Clock::systemUTC));
    }

    /**
     * Creates the service with an explicit clock (used by the tests).
     *
     * @param repository the audit store
     * @param clock      source of the timestamps
     */
    AuditService(AuditRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Runs in its own transaction so that the entry is kept even if the caller's transaction is rolled back
     * (a refused or failed access must still leave a trace).
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(UserRef user, String action, String resource, AuditResult result) {
        String userId = user == null || user.id() == null || user.id().isBlank() ? ANONYMOUS_USER_ID : user.id();
        Role role = user == null ? null : user.role();
        repository.save(AuditEntry.create(Instant.now(clock), userId, role, action, resource, result));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditEntryDto> query(AuditQuery filters) {
        AuditQuery query = filters == null ? AuditQuery.all() : filters;
        Instant from = query.from() == null ? EARLIEST : query.from();
        Instant to = query.to() == null ? LATEST : query.to();
        if (from.isAfter(to)) {
            throw new InvalidAuditQueryException("'from' must not be after 'to'");
        }
        return repository.search(blankToNull(query.userId()), from, to, likePrefix(query.resource()))
                .stream()
                .map(AuditService::toDto)
                .toList();
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.strip();
    }

    /**
     * Escapes the LIKE wildcards of the given text and appends {@code %}, so the text is a literal prefix.
     * A {@code null} or blank text gives {@code %}, which matches every resource.
     */
    static String likePrefix(String prefix) {
        String clean = blankToNull(prefix);
        if (clean == null) {
            return "%";
        }
        StringBuilder pattern = new StringBuilder(clean.length() + 1);
        for (char c : clean.toCharArray()) {
            if (c == '%' || c == '_' || c == LIKE_ESCAPE) {
                pattern.append(LIKE_ESCAPE);
            }
            pattern.append(c);
        }
        return pattern.append('%').toString();
    }

    private static AuditEntryDto toDto(AuditEntry entry) {
        return new AuditEntryDto(entry.getId(), entry.getTimestamp(), entry.getUserId(), entry.getRole(),
                entry.getAction(), entry.getResource(), entry.getResult());
    }
}
