package es.uclm.sescam.common.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RoleTest {

    @Test
    void buildsTheSpringSecurityAuthorityOfEachRole() {
        assertEquals("ROLE_PATIENT", Role.PATIENT.authority());
        assertEquals("ROLE_PHYSICIAN", Role.PHYSICIAN.authority());
        assertEquals("ROLE_ADMISSIONS", Role.ADMISSIONS.authority());
        assertEquals("ROLE_ADMINISTRATOR", Role.ADMINISTRATOR.authority());
    }
}
