package com.dotmarketing.servlets;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
//import javax.servlet.annotation.WebServlet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * This servlet is used to perform final startup tasks after all other servlets have loaded.
 * It runs with the highest loadOnStartup value to ensure it executes last.
 * 
 * Current responsibilities:
 * 1. Refresh health checks to notify external systems that the server is ready
 * 2. Check if the server should be shutdown on startup (shutdown-on-startup property)
 */
public class FinalStartupServlet extends HttpServlet {

    // init method
    @Override
    public void init(ServletConfig cfg) throws ServletException {
        super.init(cfg);
        Logger.info(this, "Starting FinalStartupServlet");
        
        // Force refresh servlet container health check now that startup is truly complete
        // This ensures HTTP connectors are detected as ready without refreshing slower checks
        try {
            CompletableFuture
                .delayedExecutor(3, TimeUnit.SECONDS)
                .execute(() -> {
                    try {
                        // Only refresh the servlet-container health check specifically (blocking mode)
                        // This is the check that needs to detect HTTP connectors as ready
                        // Use blocking mode to ensure cache is updated before any probes can run
                        APILocator.getHealthService().forceRefreshHealthCheck("servlet-container", true);
                        Logger.info(FinalStartupServlet.class, "Servlet container health check refreshed after final startup completion");
                    } catch (Exception e) {
                        Logger.warn(FinalStartupServlet.class, "Failed to refresh servlet container health check after final startup: " + e.getMessage());
                    }
                });
            Logger.info(this, "Servlet container health check refresh scheduled for 3 seconds after final startup");
        } catch (Exception e) {
            Logger.warn(this, "Failed to schedule servlet container health check refresh after final startup: " + e.getMessage());
        }
        
        if (Config.getBooleanProperty("shutdown-on-startup", false)) {
            Logger.info(this, "shutdown-on-startup is true, shutting down the server");
            com.dotcms.shutdown.SystemExitManager.shutdownOnStartupExit("shutdown-on-startup property is enabled");
        }
    }

    @Override
    public void destroy() {
        Logger.info(this, "FinalStartupServlet destroyed");
    }

}
