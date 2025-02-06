package com.dotcms.rest.config;

import org.jvnet.hk2.internal.ServiceLocatorImpl;
import org.jvnet.hk2.internal.Utilities;

/**
 * Here's why we're doing this: We need to override the inject method in the ServiceLocatorImpl
 * as it performs a checkStatus that we don't want to do when we're shutting down
 */
public class DotServiceLocatorImpl extends ServiceLocatorImpl {

    String name;

    /**
     * Called by the Generator, and hence must be a public method
     *
     * @param name   The name of this locator
     * @param parent The parent of this locator (may be null)
     */
    public DotServiceLocatorImpl(String name, ServiceLocatorImpl parent) {
        super(name, parent);
        this.name = name;
    }


    /**
     * Injects the given object using the given strategy
     * @param injectMe The object to be analyzed and injected into
     * @param strategy The name of the {@link ClassAnalyzer} that should be used. If
     * null the default analyzer will be used
     */
    @Override
    public void inject(Object injectMe, String strategy) {
        //Here we check if we're shutting down
        Utilities.justInject(injectMe, this, strategy);
    }

    @Override
    public String toString() {
        return "DotServiceLocatorImpl(" + name + "," + super.getLocatorId() + "," + System.identityHashCode(this) + ")";
    }
}
