package com.dotcms.ai.exception;

import com.dotmarketing.exception.DotRuntimeException;

/**
 * Exception thrown when all AI models have been exhausted.
 *
 * <p>
 * This exception is used to indicate that all available AI models have been exhausted and no further models
 * are available for processing. It extends the {@link DotRuntimeException} to provide additional context
 * specific to AI model exhaustion scenarios.
 * </p>
 *
 * @author vico
 */
public class DotAIAllModelsExhaustedException extends DotRuntimeException {

    public DotAIAllModelsExhaustedException(final String message) {
        super(message);
    }

}
