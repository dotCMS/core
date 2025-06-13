package com.dotmarketing.servlets;

import javax.servlet.annotation.WebListener;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletContextEvent;

@WebListener
public class IIOProviderContextListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // Initialize ImageIO providers
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // Cleanup ImageIO providers
    }
} 