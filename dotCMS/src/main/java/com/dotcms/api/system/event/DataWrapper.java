package com.dotcms.api.system.event;

import java.io.Serializable;

/**
 * Wrapper for the {@link Payload}'s data
 */
public interface DataWrapper<T>  extends Serializable {

    public T getData();
}
