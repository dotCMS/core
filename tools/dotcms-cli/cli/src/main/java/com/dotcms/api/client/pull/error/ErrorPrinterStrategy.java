package com.dotcms.api.client.pull.error;

/**
 * Interface for defining a strategy on how to print errors. Each strategy defines conditions under
 * which it is applicable, and a way to create an error message from given exception.
 */
public interface ErrorPrinterStrategy {

    /**
     * Checks if this strategy is applicable to given error.
     *
     * @param error the exception to be checked
     * @return true if the strategy is applicable, false otherwise
     */
    boolean isApplicable(Exception error);

    /**
     * Returns an error message for the given exception.
     *
     * @param error the exception to get message for
     * @return a string representing error message
     */
    String getErrorMessage(Exception error);
    
}
