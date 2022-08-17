package com.dotcms.csspreproc.dartsass.strategy;

/**
 * Defines the correct way of assembling the Dart SASS command for compiling SCSS files depending on your current
 * environment.
 * <p>The Dart SASS files used to compile such files are specific to an Operating System and architecture, which means
 * that a different binary must be selected if you're running on Mac OS X or Linux, and if they run in an ARM64, X64
 * or any other architecture. This Strategy Pattern makes it easier to assemble the appropriate command and support
 * new Operating Systems and architectures if necessary.</p>
 *
 * @author Jose Castro
 * @since Aug 16th, 2022
 */
public interface SassCommandStrategy {

    /**
     * Verifies if this Strategy is the one that matches the current environment.
     *
     * @return If the current Operating System and architecture are suitable for this Strategy, returns {@code true}.
     */
    boolean test();

    /**
     * Applies the current Strategy, which means building the appropriate Dart SASS compiler command for the current
     * environment.
     *
     * @return The expected compiler command.
     */
    String apply();

}
