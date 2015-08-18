package com.dotmarketing.listeners;

import java.io.File;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.dotcms.repackage.org.apache.log4j.BasicConfigurator;
import com.dotcms.repackage.org.apache.log4j.xml.DOMConfigurator;

import com.dotcms.enterprise.ClusterThreadProxy;
import com.dotcms.util.AsciiArt;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.reindex.ReindexThread;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;

/**
 *
 * @author Andres Olarte
 *
 */
public class ContextLifecycleListener implements ServletContextListener {

	public ContextLifecycleListener() {
//		Config.initializeConfig();
//		System.setProperty("DOTCMS_LOGGING_HOME", ConfigUtils.getDynamicContentPath() + File.separator + "logs");

	}

	public void contextDestroyed(ServletContextEvent arg0) {
		Logger.info(this, "Shutdown event received, executing a clean shutdown.");
		try {
			QuartzUtils.stopSchedulers();
		} catch (Exception e) {
			Logger.error(this, "A error ocurred trying to shutdown the Schedulers.");
		}
        try {
        	ReindexThread.shutdownThread();

        } catch (Exception e) {
            Logger.error(this, "A error ocurred trying to shutdown the ReindexThread.");
        }

        try {
        	ClusterThreadProxy.shutdownThread();

        } catch (Exception e) {
            Logger.error(this, "A error ocurred trying to shutdown the ClusterThread.");
        }

        try {
        	CacheLocator.getCacheAdministrator().shutdown();
        } catch (Exception e) {
            Logger.error(this, "A error ocurred trying to shutdown the Cache subsystem.");
        }

		Logger.info(this, "Finished shuting down.");

	}

	public void contextInitialized(ServletContextEvent arg0) {

		Config.setMyApp(arg0.getServletContext());
		System.setProperty("DOTCMS_LOGGING_HOME", ConfigUtils.getDynamicContentPath() + File.separator + "logs");
		
		
	    String path = null;
		try {

            String contextPath = Config.CONTEXT_PATH;
            if ( !contextPath.endsWith( File.separator ) ) {
                contextPath += File.separator;
            }
            String defaultPath = contextPath + "WEB-INF" + File.separator + "log4j" + File.separator + "log4j.xml";
            path = Config.getStringProperty("DOTCMS_LOG4J_XML_PATH", defaultPath);
            
        } catch (Exception e) {
			Logger.error(this,e.getMessage(),e);
		}

    	BasicConfigurator.resetConfiguration();
    	DOMConfigurator.configure(path);
    	Logger.clearLoggers();
    	AsciiArt.doArt();
	}

}
