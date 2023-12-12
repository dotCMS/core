package com.dotcms.api.client.pull.error.strategy;

import com.dotcms.api.client.pull.error.ErrorPrinterStrategy;

/**
 * Default strategy for handling and printing all kinds of errors. If no specific strategy is
 * applicable, this default strategy is used.
 */
public class DefaultErrorPrinterStrategy implements ErrorPrinterStrategy {

    @Override
    public boolean isApplicable(Exception error) {
        return true;
    }

    @Override
    public String getErrorMessage(Exception error) {
        return error.getMessage();
    }

}