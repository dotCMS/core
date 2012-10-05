package com.dotmarketing.listeners;

import com.dotmarketing.util.OSGIUtil;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class OsgiFelixListener implements ServletContextListener {

    /**
     * @see ServletContextListener#contextInitialized(ServletContextEvent)
     */
    public void contextInitialized ( ServletContextEvent context ) {
        OSGIUtil.getInstance().initializeFramework( context );
    }

    /**
     * @see ServletContextListener#contextDestroyed(ServletContextEvent)
     */
    public void contextDestroyed ( ServletContextEvent context ) {
        OSGIUtil.getInstance().stopFramework();
    }

}