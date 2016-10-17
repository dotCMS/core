package com.dotcms.api.system.event;

/**
 * Wrapper for the {@link Payload}'s data
 */
public interface DataWrapper<T> {

    public T getData();
}
