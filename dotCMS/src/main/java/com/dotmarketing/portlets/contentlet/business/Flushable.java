package com.dotmarketing.portlets.contentlet.business;

/**
 * A Flushable is a API that handle cache and provided public mehod to flush the content away
 * @param <T>
 */
public interface Flushable<T> {

    /**
     * Flush all the cache away
     */
    void flushAll();

    /**
     * Flush one cache's item away
     * @param t
     */
    void flush(T t);
}
