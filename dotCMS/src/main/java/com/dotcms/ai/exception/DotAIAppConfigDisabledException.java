package com.dotcms.ai.exception;

import com.dotmarketing.exception.DotRuntimeException;

/**
 * Exception thrown when the AI application configuration is disabled.
 *
 * <p>
 * This exception is used to indicate that the AI application configuration is disabled and cannot be used.
 * It extends the {@link DotRuntimeException} to provide additional context specific to AI application configuration
 * disabled scenarios.
 * </p>
 *
 * @author vico
 */
public class DotAIAppConfigDisabledException extends DotRuntimeException {

    public DotAIAppConfigDisabledException(final String message) {
        super(message);
    }

}
