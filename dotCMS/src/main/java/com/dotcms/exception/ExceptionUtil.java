package com.dotcms.exception;

/**
 * Exception Utils
 * @author andrecurione
 */
public class ExceptionUtil {

    /**
     * Returns true if the Throwable is instance or contains a cause of the specified ExceptionClass
     * @param e
     * @param exceptionClass
     * @return boolean
     */
    public static boolean causedBy(Throwable e, Class exceptionClass) {

        Throwable t = e;
        while (t != null) {
            if (t.getClass().equals(exceptionClass)) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }
}
