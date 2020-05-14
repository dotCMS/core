package org.apache.felix.framework;

import javax.servlet.ServletContext;
import org.apache.felix.http.proxy.DispatcherTracker;
import org.osgi.framework.BundleContext;
import com.dotmarketing.osgi.OSGIProxyServlet;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.WebKeys;

/**
 * @author Jonathan Gamba 10/3/18
 */
public class OSGIUtils {

    /**
     * Initialize the OSGI felix framework in case it was not already started
     */
    public static void initializeOsgi(final ServletContext context) {

        //First verify if OSGI was already initialized
        final Boolean osgiInitialized = OSGIUtil.getInstance().isInitialized();
        if (!osgiInitialized) {

            if (Config.getBooleanProperty(WebKeys.OSGI_ENABLED, true)) {
                OSGIUtil.getInstance().initializeFramework(context);
            } else {
                System.clearProperty(WebKeys.OSGI_ENABLED);
            }
        }

        //Prepare the proxy servlet in case it was not properly initialized
        initOsgiProxyTracker(context);
    }

    /**
     * Sets the bundle context to the OSGIProxyServlet
     */
    private static void initOsgiProxyTracker(final ServletContext context) {
        
        if(Config.getBooleanProperty("felix.felix.enable.osgi.proxyservlet", false)) {
            if (OSGIProxyServlet.bundleContext == null) {

                final Object bundleContext = context.getAttribute(BundleContext.class.getName());
                if (bundleContext instanceof BundleContext) {

                    OSGIProxyServlet.bundleContext = (BundleContext) bundleContext;

                    try {
                        OSGIProxyServlet.tracker =
                                new DispatcherTracker(OSGIProxyServlet.bundleContext, null,
                                        OSGIProxyServlet.servletConfig);
                    } catch (Exception e) {
                        Logger.error(OSGIUtils.class, "Error loading HttpService.", e);
                        return;
                    }

                    OSGIProxyServlet.tracker.open();
                }
            }
        } else {
            System.clearProperty(WebKeys.OSGI_ENABLED);
        }

    }

}