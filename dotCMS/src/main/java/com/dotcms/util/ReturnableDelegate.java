package com.dotcms.util;

/**
 * Returnable delegate without any parameter.
 * @author jsanca
 */
public interface ReturnableDelegate<T> {

    /**
     * This method will be called by the parent routine, delegating the specific job.
     *
     * @return Object the information returned
     */
    public T execute();
} // E:O:F:ReturnableDelegate.
