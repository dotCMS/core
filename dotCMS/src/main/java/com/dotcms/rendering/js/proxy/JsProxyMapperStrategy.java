package com.dotcms.rendering.js.proxy;

/**
 * An implementation of the ProxyMapper will create a proxy object for the javascript graal engine, if the test method
 * returns true
 *
 * Usually the test method will check a Type or hierarchy of types
 * @author jsanca
 */
public interface JsProxyMapperStrategy {

    /**
     * True if the object can be mapped by this mapper
     * @param obj
     * @return
     */
    boolean test(Object obj);

    /**
     * Creates a proxy object for the javascript graal engine
     * @param obj
     * @return
     */
    Object apply(Object obj);

    default int getPriority() {
        return Integer.MAX_VALUE;
    }
}
