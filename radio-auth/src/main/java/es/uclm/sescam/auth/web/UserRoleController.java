package es.uclm.sescam.auth.web;

import es.uclm.sescam.auth.application.AuthorizationService;
import es.uclm.sescam.auth.application.AuthorizationService.UserSummary;
import es.uclm.sescam.common.api.Role;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Administrative user and role endpoints for CU-18. */
@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMINISTRATOR')")
public class UserRoleController {

    private final AuthorizationService authorization;

    public UserRoleController(AuthorizationService authorization) {
        this.authorization = authorization;
    }

    @GetMapping
    public List<UserSummary> search(@RequestParam(defaultValue = "") String query) {
        return authorization.searchUsers(query);
    }

    @PutMapping("/{id}/roles/{role}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assign(@PathVariable String id, @PathVariable Role role) {
        authorization.assignRole(id, role);
    }

    @DeleteMapping("/{id}/roles/{role}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable String id, @PathVariable Role role) {
        authorization.removeRole(id, role);
    }
}
