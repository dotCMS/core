package com.dotmarketing.listeners;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.OSGIUtil;
import com.dotmarketing.util.WebKeys;

public class OsgiFelixListener implements ServletContextListener {

	/**
	 * @see ServletContextListener#contextInitialized(ServletContextEvent)
	 */
	public void contextInitialized(ServletContextEvent context) {

		// delay init 30 sec
		final int delay = Config.getIntProperty("felix.osgi.init.delay", 0);
		
		final boolean waitForDotCMS = Config.getBooleanProperty("felix.osgi.wait.for.dotcms", false);
		
		
		if (Config.getBooleanProperty("felix.osgi.enable", true)) {
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
						
						initializeOsgi(context);
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
		if (!Config.getBooleanProperty("felix.osgi.enable", true)) {
			System.clearProperty(WebKeys.OSGI_ENABLED);
			return;
		}
		System.setProperty(WebKeys.OSGI_ENABLED, "true");
		OSGIUtil.getInstance().initializeFramework(context);
		

	}

}