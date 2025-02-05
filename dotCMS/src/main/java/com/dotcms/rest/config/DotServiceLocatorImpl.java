package com.dotcms.rest.config;

import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.internal.ServiceLocatorImpl;
import org.jvnet.hk2.internal.Utilities;

@Service
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

    @Override
    public void inject(Object injectMe, String strategy) {
        //Here we check if we're shutting down
        System.out.println(":: DotServiceLocatorImpl.inject!!!!");
        Utilities.justInject(injectMe, this, strategy);
    }

    @Override
    public String toString() {
        return "DotServiceLocatorImpl(" + name + "," + super.getLocatorId() + "," + System.identityHashCode(this) + ")";
    }
}
