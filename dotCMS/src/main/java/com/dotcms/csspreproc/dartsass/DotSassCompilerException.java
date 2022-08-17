package com.dotcms.csspreproc.dartsass;

import com.dotmarketing.exception.DotDataException;

/**
 * Signals a problem related to the compilation process of SASS Files in dotCMS.
 * <p>It's worth noting that this type of exception is meant to point out issues with the setup, input/output
 * parameters, or configuration parameters for the SASS compiler itself. Specific compile errors are not reported via
 * this exception, but through the {@link DartSassCompiler} class.</p>
 *
 * @author Jose Castro
 * @since Jul 27th, 2022
 */
public class DotSassCompilerException extends DotDataException {

    /**
     * Creates an instance of this exception with the given error message.
     *
     * @param message The human-readable error message.
     */
    public DotSassCompilerException(final String message) {
        super(message);
    }

    /**
     * Creates an instance of this exception with the given error message and the associated exception that caused it.
     *
     * @param message The human-readable error message.
     * @param e       The original exception
     */
    public DotSassCompilerException(final String message, final Throwable e) {
        super(message, e);
    }

}
