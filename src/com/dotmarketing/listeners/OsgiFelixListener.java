package com.dotmarketing.listeners;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.OSGIUtil;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class OsgiFelixListener implements ServletContextListener {

    /**
     * @see ServletContextListener#contextInitialized(ServletContextEvent)
     */
    public void contextInitialized ( ServletContextEvent context ) {
    	if(Config.getBooleanProperty("felix.osgi.enable", true)){
    		OSGIUtil.getInstance().initializeFramework( context );
    	}
    	else{
    		Logger.info(this.getClass(), "OSGI Disabled");
    		
    	}
    	
    }

    /**
     * @see ServletContextListener#contextDestroyed(ServletContextEvent)
     */
    public void contextDestroyed ( ServletContextEvent context ) {
    	if(Config.getBooleanProperty("felix.osgi.enable", true)){
    		OSGIUtil.getInstance().stopFramework();
    	}
    }

}