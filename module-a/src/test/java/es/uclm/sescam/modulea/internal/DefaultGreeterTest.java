package es.uclm.sescam.modulea.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import es.uclm.sescam.modulea.api.Greeter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class DefaultGreeterTest {

    private final Greeter greeter = new DefaultGreeter();

    @Test
    void greetReturnsGreetingForValidName() {
        assertEquals("Hello, Ana!", greeter.greet("Ana"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void greetRejectsNullOrBlankName(String name) {
        assertThrows(IllegalArgumentException.class, () -> greeter.greet(name));
    }
}
