package com.dotmarketing.listeners;

import com.dotcms.util.AsciiArt;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.reindex.ReindexThread;
import com.dotmarketing.loggers.Log4jUtil;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.File;

/**
 *
 * @author Andres Olarte
 *
 */
public class ContextLifecycleListener implements ServletContextListener {

	public ContextLifecycleListener() {
		//Config.initializeConfig();
//		System.setProperty("DOTCMS_LOGGING_HOME", ConfigUtils.getDynamicContentPath() + File.separator + "logs");

	}

	public void contextDestroyed(ServletContextEvent arg0) {
		Logger.info(this, "Shutdown event received, executing a clean shutdown.");
		try {
			QuartzUtils.stopSchedulers();
		} catch (Exception e) {
			Logger.warn(this, "A error ocurred trying to shutdown the Schedulers.");
		}
        try {
        	ReindexThread.shutdownThread();

        } catch (Exception e) {
            Logger.warn(this, "A error ocurred trying to shutdown the ReindexThread.");
        }

        try {
        	CacheLocator.getCacheAdministrator().shutdown();
        } catch (Exception e) {
            Logger.warn(this, "A error ocurred trying to shutdown the Cache subsystem.");
        }

		Logger.info(this, "Finished shuting down.");

	}

	public void contextInitialized(ServletContextEvent contextEvent) {

		Config.setMyApp(contextEvent.getServletContext());

    	System.setProperty("DOTCMS_LOGGING_HOME", Config.getStringProperty("DOTCMS_LOGGING_HOME",ConfigUtils.getDynamicContentPath() + File.separator + "logs"));

		File file = new File(Config.CONTEXT.getRealPath("/WEB-INF/log4j/log4j2.xml"));


		//Initialises/reconfigures log4j based on a given log4j configuration file
		Log4jUtil.initializeFromPath(file.getAbsolutePath());

    	Logger.clearLoggers();
    	AsciiArt.doArt();
	}

}
