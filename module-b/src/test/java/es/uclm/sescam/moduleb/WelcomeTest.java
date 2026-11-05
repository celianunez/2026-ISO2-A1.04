package es.uclm.sescam.moduleb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import es.uclm.sescam.modulea.api.Greeter;
import org.junit.jupiter.api.Test;

class WelcomeTest {

    @Test
    void welcomeUsesTheGreeterInterface() {
        Greeter stub = name -> "Hi " + name;
        Welcome welcome = new Welcome(stub);

        assertEquals("Hi Ana Welcome to SESCAM Radiodiagnostics.", welcome.welcome("Ana"));
    }

    @Test
    void constructorRejectsNullGreeter() {
        assertThrows(IllegalArgumentException.class, () -> new Welcome(null));
    }
}
