package es.uclm.sescam.modulea.internal;

import es.uclm.sescam.modulea.api.Greeter;

/**
 * Default implementation of {@link Greeter}. Internal to module A.
 */
public class DefaultGreeter implements Greeter {

    @Override
    public String greet(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be null or blank");
        }
        return "Hello, " + name + "!";
    }
}
