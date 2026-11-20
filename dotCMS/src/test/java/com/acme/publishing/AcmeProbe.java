package com.acme.publishing;

/**
 * Stands in for a type a customer plugin contributes to a bundle. Its package is deliberately
 * outside every namespace dotCMS ships, so the only way it can be read back is through the
 * configured extension to the trusted list.
 */
public class AcmeProbe {

    private String name;

    /**
     * Returns the value carried through the round trip.
     *
     * @return the name this probe was built with
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value carried through the round trip.
     *
     * @param name the name to carry
     */
    public void setName(final String name) {
        this.name = name;
    }
}
