package es.uclm.sescam.audit.web;

import es.uclm.sescam.audit.api.AuditEntryDto;
import es.uclm.sescam.audit.api.AuditQuery;
import es.uclm.sescam.audit.api.IAuditService;
import es.uclm.sescam.common.api.CurrentUser;
import es.uclm.sescam.common.api.Role;
import es.uclm.sescam.common.api.UserRef;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * REST access to the audit log (CU-19). Read only: there is no POST, PUT, PATCH or DELETE mapping, so any
 * attempt to change an entry through HTTP is refused. Entries are written only through {@link IAuditService}.
 */
@RestController
@RequestMapping("/api/audit")
public class AuditController {

    /** Action recorded when the audit log itself is consulted. */
    static final String READ_ACTION = "READ";

    /** Resource recorded when the audit log itself is consulted. */
    static final String AUDIT_LOG_RESOURCE = "audit/log";

    private final IAuditService auditService;

    /**
     * Creates the controller.
     *
     * @param auditService the audit service
     */
    public AuditController(IAuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * Lists the audit entries that match the filters, newest first. Administrators only.
     * Consulting the log is itself recorded, because the log describes access to clinical data.
     *
     * @param user     exact user identifier
     * @param from     ISO-8601 instant, for example {@code 2026-11-25T00:00:00Z} (inclusive)
     * @param to       ISO-8601 instant (inclusive)
     * @param resource prefix of the resource, for example {@code patient/P-0001}
     * @return matching entries
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public List<AuditEntryDto> list(
            @RequestParam(name = "user", required = false) String user,
            @RequestParam(name = "from", required = false) Instant from,
            @RequestParam(name = "to", required = false) Instant to,
            @RequestParam(name = "resource", required = false) String resource) {
        List<AuditEntryDto> entries = auditService.query(new AuditQuery(user, from, to, resource));
        String adminId = CurrentUser.requireUserId();
        auditService.record(new UserRef(adminId, adminId, Role.ADMINISTRATOR), READ_ACTION, AUDIT_LOG_RESOURCE);
        return entries;
    }
}
