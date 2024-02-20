package com.dotcms.api.client.pull.error.strategy;

import com.dotcms.api.client.pull.error.ErrorPrinterStrategy;
import com.dotcms.api.client.pull.exception.PullException;

/**
 * Strategy for handling and printing errors that are instances of {@link PullException}.
 */
public class PullErrorPrinterStrategy implements ErrorPrinterStrategy {

    @Override
    public boolean isApplicable(Exception error) {
        return error instanceof PullException;
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