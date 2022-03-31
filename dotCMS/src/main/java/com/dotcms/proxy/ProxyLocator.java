package com.dotcms.proxy;

import com.dotcms.util.ReflectionUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Locator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;

/**
 * Proxy to access the proxies on EE
 * @author jsanca
 */
public class ProxyLocator extends Locator<ProxyLocator.APIIndex> {

    protected static ProxyLocator instance;

    /**
     * Private constructor for the singleton.
     */
    protected ProxyLocator() {
        super();
    }

    /**
     * Creates a single instance of this class.
     */
    public synchronized static void init(){
        if(instance != null) {
            return;
        }
        if (instance == null) {
            instance = new ProxyLocator();
        }
    }


    /**
     * Creates a unique instance of this ProxyLocator
     *
     * @return A new instance of the {@link ProxyLocator}.
     */
    private static ProxyLocator getAPILocatorInstance() {
        if(instance == null){
            init();
            if(instance == null){
                Logger.fatal(APILocator.class,"PROXY LOCATOR IS NOT INITIALIZED : THIS SHOULD NEVER HAPPEN");
                throw new DotRuntimeException("PROXY LOCATOR IS NOT INITIALIZED : THIS SHOULD NEVER HAPPEN");
            }
        }
        return instance;
    }

    @Override
    protected Locator<APIIndex> getLocatorInstance() {
        return instance;
    }

    @Override
    protected Object createService(APIIndex enumObj) {
        return enumObj.create();
    }

    /**
     * Generates a unique instance of the specified dotCMS API.
     *
     * @param index
     *            - The specified API to retrieve based on the {@link APIIndex}
     *            class.
     * @return A singleton of the API.
     */
    private static Object getInstance(final APIIndex index) {

        final ProxyLocator apiLocatorInstance = getAPILocatorInstance();

        final Object serviceRef = apiLocatorInstance.getServiceInstance(index);

        if( Logger.isDebugEnabled(APILocator.class) ) {
            Logger.debug(APILocator.class, apiLocatorInstance.audit(index));
        }

        return serviceRef;
    }
    /**
     * Creates a single instance of the {@link SiteJobProxy} class.
     *
     * @return The {@link SiteJobProxy} class.
     */
    public static SiteJobProxy getSiteJobProxy() {
        return (SiteJobProxy)getInstance(APIIndex.SITE_JOB_PROXY);
    }


    enum APIIndex {
        SITE_JOB_PROXY;

        // NOTE: try to use always reflection to get the singleton instance to avoid compile dependency with ee
        Object create() {
            switch(this) {
                case SITE_JOB_PROXY: return ReflectionUtils.newInstance("com.dotcms.enterprise.SiteJobProxyImpl");
            }
            throw new AssertionError("Unknown API Proxy index: " + this);
        }
    }
}


