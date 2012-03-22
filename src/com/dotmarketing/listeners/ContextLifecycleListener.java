package com.dotmarketing.listeners;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.dotcms.enterprise.ClusterThreadProxy;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.reindex.ReindexThread;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.util.Logger;

/**
 * 
 * @author Andres Olarte
 *
 */
public class ContextLifecycleListener implements ServletContextListener {

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
		
		
	}

}
