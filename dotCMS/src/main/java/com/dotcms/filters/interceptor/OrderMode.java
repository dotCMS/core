package com.dotcms.filters.interceptor;

/**
 * Defines the order to execute the filter pipeline, by default it will be FILO; First in, Last out, it means the
 * first interceptor called before the doFilter, will be the last one called after the doFilter.
 *
 * FILO: First in, Last out
 * FIFO: First in, Fist out.
 */
public enum OrderMode {

    FILO,
    FIFO
}
