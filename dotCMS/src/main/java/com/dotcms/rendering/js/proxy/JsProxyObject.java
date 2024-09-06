package com.dotcms.rendering.js.proxy;

/**
 * Common interface for all the proxy objects used in the JS context.
 * @param <T>
 */
public interface JsProxyObject<T> {

    /**
     * Returns the wrapped object by the proxy
     * @return
     */
    T getWrappedObject();
}
