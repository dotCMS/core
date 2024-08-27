package com.dotcms.ai.exception;

import com.dotmarketing.exception.DotRuntimeException;

/**
 * Exception thrown when an AI model is not found.
 *
 * <p>
 * This exception is used to indicate that a specific AI model could not be found. It extends the {@link DotRuntimeException}
 * to provide additional context specific to AI model not found scenarios.
 * </p>
 *
 * @author vico
 */
public class DotAIModelNotFoundException extends DotRuntimeException {

    public DotAIModelNotFoundException(final String message) {
        super(message);
    }

}
