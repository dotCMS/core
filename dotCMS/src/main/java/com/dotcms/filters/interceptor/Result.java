package com.dotcms.filters.interceptor;

/**
 * Encapsulates the Result for a filter.
 * @author jsanca
 */
public enum Result {

    /**
     * If the current interceptor wants to allow the call of the next interceptor, return NEXT.
     */
    NEXT,
    /**
     * If the current interceptor want to skip the next interceptors but wants to execute the filter chain call, return SKIP.
     */
    SKIP,
    /**
     * If the current interceptor want to skip the next interceptor and in addition want to avoid the filter chain call, return SKIP_NO_CHAIN
     */
    SKIP_NO_CHAIN;

} // E:O:F:Result.
