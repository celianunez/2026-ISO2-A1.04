package es.uclm.sescam.moduleb;

import es.uclm.sescam.modulea.api.Greeter;

/**
 * Uses module A only through its {@link Greeter} interface.
 */
public class Welcome {

    private final Greeter greeter;

    public Welcome(Greeter greeter) {
        if (greeter == null) {
            throw new IllegalArgumentException("greeter must not be null");
        }
        this.greeter = greeter;
    }

    public String welcome(String name) {
        return greeter.greet(name) + " Welcome to SESCAM Radiodiagnostics.";
    }
}
