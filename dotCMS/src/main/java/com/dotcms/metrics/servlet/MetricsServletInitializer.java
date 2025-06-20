package com.dotcms.metrics.servlet;

import com.dotcms.metrics.MetricsConfig;
import com.dotmarketing.util.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRegistration;
import javax.servlet.annotation.WebListener;

/**
 * Servlet context listener that registers the Prometheus metrics servlet
 * programmatically when the application starts up.
 * 
 * This approach allows for conditional registration based on configuration
 * and avoids modifying the web.xml file directly.
 */
@WebListener
public class MetricsServletInitializer implements ServletContextListener {
    
    private static final String CLASS_NAME = MetricsServletInitializer.class.getSimpleName();
    
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext servletContext = sce.getServletContext();
        
        try {
            // Only register if metrics and Prometheus are enabled
            if (!MetricsConfig.ENABLED) {
                Logger.info(this, "Metrics disabled - skipping Prometheus servlet registration");
                return;
            }
            
            if (!MetricsConfig.PROMETHEUS_ENABLED) {
                Logger.info(this, "Prometheus disabled - skipping servlet registration");
                return;
            }
            
            // Register the Prometheus metrics servlet
            String servletName = "PrometheusMetricsServlet";
            String urlPattern = MetricsConfig.PROMETHEUS_ENDPOINT;
            
            ServletRegistration.Dynamic registration = servletContext.addServlet(
                servletName, 
                PrometheusMetricsServlet.class
            );
            
            if (registration != null) {
                registration.addMapping(urlPattern);
                registration.setAsyncSupported(true);
                registration.setLoadOnStartup(50); // Load after core servlets but before final startup
                
                Logger.info(this, "Registered Prometheus metrics servlet at: " + urlPattern);
            } else {
                // Servlet may already be registered
                Logger.debug(this, "Prometheus metrics servlet may already be registered");
            }
            
        } catch (Exception e) {
            Logger.error(this, "Failed to register Prometheus metrics servlet: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        Logger.debug(this, "Metrics servlet context destroyed");
    }
}