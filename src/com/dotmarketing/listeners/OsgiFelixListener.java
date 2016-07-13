package com.dotmarketing.listeners;

import com.dotcms.repackage.org.apache.felix.http.proxy.DispatcherTracker;
import com.dotcms.repackage.org.osgi.framework.BundleContext;
import com.dotmarketing.osgi.OSGIProxyServlet;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.OSGIUtil;
import com.dotmarketing.util.WebKeys;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class OsgiFelixListener implements ServletContextListener {

	// delay init 30 sec
	private final int delay = Config.getIntProperty("felix.osgi.init.delay", 0);

	private final boolean waitForDotCMS = Config.getBooleanProperty("felix.osgi.wait.for.dotcms", true);

	/**
	 * @see ServletContextListener#contextInitialized(ServletContextEvent)
	 */
	public void contextInitialized(ServletContextEvent context) {

		if (Config.getBooleanProperty(WebKeys.OSGI_ENABLED, true)) {
			
			if (delay > 0 || waitForDotCMS) { // if we spin up a new thread to init OSGI
				class OsgiInitThread extends Thread {
					 OsgiInitThread(){
						 if(waitForDotCMS){
							 super.setName("OSGI Startup Thread - delaying init " + delay + "ms");
						 }
						 else{
							 super.setName("OSGI Startup Thread - waiting for dotCMS init");
						 }
						 
					 }
					@Override
					public void run() {
						
						if(waitForDotCMS){
							while((System.getProperty(WebKeys.DOTCMS_STARTED_UP) == null)){
								try {
									Thread.sleep(1000);
								} catch (InterruptedException e) {
									Logger.error(OsgiFelixListener.class, e.getMessage(), e);
								}
							}
						}
						
						try {
							Thread.sleep(delay);
						} catch (InterruptedException e) {
							Logger.error(OsgiFelixListener.class, e.getMessage(), e);
							//throw new DotRuntimeException(e.getMessage(),e);
						}
						long start = System.currentTimeMillis();
						initializeOsgi(context);
						System.setProperty(WebKeys.DOTCMS_STARTUP_TIME_OSGI, String.valueOf(System.currentTimeMillis() - start));
					}
				}
				OsgiInitThread thread = new OsgiInitThread();
				thread.start();
			} else { // if inline the osgi init
				initializeOsgi(context);
			}

		} else {
			Logger.info(this.getClass(), "OSGI Disabled");
		}
	}

	/**
	 * @see ServletContextListener#contextDestroyed(ServletContextEvent)
	 */
	public void contextDestroyed(ServletContextEvent context) {
		if (System.getProperty(WebKeys.OSGI_ENABLED) != null) {
			OSGIUtil.getInstance().stopFramework();
		}
	}

    private void initializeOsgi(ServletContextEvent context) {
        if (!Config.getBooleanProperty(WebKeys.OSGI_ENABLED, true)) {
            System.clearProperty(WebKeys.OSGI_ENABLED);
            return;
        }
        OSGIUtil.getInstance().initializeFramework(context);

        if (OSGIProxyServlet.bundleContext == null && (delay > 0 || waitForDotCMS)) {
            Object bundleContext = context.getServletContext().getAttribute(BundleContext.class.getName());
            if (bundleContext instanceof BundleContext) {
                OSGIProxyServlet.bundleContext = (BundleContext) bundleContext;
                try {
                    OSGIProxyServlet.tracker =
                        new DispatcherTracker(OSGIProxyServlet.bundleContext, null, OSGIProxyServlet.servletConfig);
                } catch (Exception e) {
                    Logger.error(this, "Error loading HttpService.", e);
                    return;
                }
                OSGIProxyServlet.tracker.open();
            }
        }

        System.setProperty(WebKeys.OSGI_ENABLED, "true");
    }

}