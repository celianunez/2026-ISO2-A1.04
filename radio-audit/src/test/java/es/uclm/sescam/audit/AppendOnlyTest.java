package es.uclm.sescam.audit;

import es.uclm.sescam.audit.api.IAuditService;
import es.uclm.sescam.audit.domain.AuditEntry;
import es.uclm.sescam.audit.infrastructure.AuditRepository;
import es.uclm.sescam.audit.web.AuditController;
import org.junit.jupiter.api.Test;
import org.springframework.data.repository.CrudRepository;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/** CU-19 acceptance criterion 3 and D-10: nothing in the module can update or delete an audit entry. */
class AppendOnlyTest {

    private static final List<String> FORBIDDEN_PREFIXES = List.of("update", "delete", "remove", "edit", "modify",
            "set", "clear", "purge");

    private static List<String> forbiddenMethods(Class<?> type) {
        return Arrays.stream(type.getMethods())
                .filter(m -> m.getDeclaringClass() != Object.class)
                .map(Method::getName)
                .filter(name -> FORBIDDEN_PREFIXES.stream()
                        .anyMatch(prefix -> name.toLowerCase(Locale.ROOT).startsWith(prefix)))
                .toList();
    }

    @Test
    void auditServiceApiHasNoUpdateOrDeleteOperation() {
        assertThat(forbiddenMethods(IAuditService.class)).isEmpty();
    }

    @Test
    void repositoryOffersOnlySaveAndQueries() {
        assertThat(forbiddenMethods(AuditRepository.class)).isEmpty();
        assertThat(CrudRepository.class.isAssignableFrom(AuditRepository.class)).isFalse();
    }

    @Test
    void entityHasNoSetters() {
        assertThat(forbiddenMethods(AuditEntry.class)).isEmpty();
        assertThat(Arrays.stream(AuditEntry.class.getDeclaredFields())
                .filter(f -> !Modifier.isStatic(f.getModifiers()))
                .noneMatch(f -> Modifier.isPublic(f.getModifiers()))).isTrue();
    }

    @Test
    void controllerHasNoWritingEndpoint() {
        assertThat(Arrays.stream(AuditController.class.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(PostMapping.class) || m.isAnnotationPresent(PutMapping.class)
                        || m.isAnnotationPresent(PatchMapping.class) || m.isAnnotationPresent(DeleteMapping.class))
                .toList()).isEmpty();
    }
}
