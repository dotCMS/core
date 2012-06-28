package com.dotmarketing.listeners;

import com.dotmarketing.osgi.FrameworkService;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Created by Jonathan Gamba
 * Date: 6/27/12
 */
public final class FelixStartupListener implements ServletContextListener {

    private FrameworkService service;

    public void contextInitialized ( ServletContextEvent event ) {

        this.service = new FrameworkService( event.getServletContext() );
        this.service.start();
    }

    public void contextDestroyed ( ServletContextEvent event ) {

        this.service.stop();
    }

}