package es.uclm.sescam.modulea.api;

/**
 * Public contract of module A. Other modules depend only on this package.
 */
public interface Greeter {

    /**
     * Builds a greeting for the given name.
     *
     * @param name the name to greet; must not be null or blank
     * @return the greeting
     * @throws IllegalArgumentException if the name is null or blank
     */
    String greet(String name);
}
