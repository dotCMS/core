package com.dotcms.api.client.pull.error.strategy;

import com.dotcms.api.client.files.traversal.exception.TraversalTaskException;
import com.dotcms.api.client.pull.error.ErrorPrinterStrategy;

/**
 * Strategy for handling and printing errors that are instances of {@link TraversalTaskException}.
 */
public class TraversalTaskErrorPrinterStrategy implements ErrorPrinterStrategy {

    @Override
    public boolean isApplicable(Exception error) {
        return error instanceof TraversalTaskException;
    }

    @Override
    public String getErrorMessage(Exception error) {
        return String.format(
                "%s --- %s",
                error.getMessage(),
                error.getCause().getMessage()
        );
    }

}
