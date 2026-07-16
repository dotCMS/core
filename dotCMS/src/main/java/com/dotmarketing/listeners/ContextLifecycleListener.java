package com.dotmarketing.listeners;

import com.dotcms.business.bytebuddy.ByteBuddyFactory;
import com.dotcms.rest.api.v1.system.websocket.SystemEventsWebSocketEndPoint;
import com.dotcms.shutdown.ShutdownCoordinator;
import com.dotcms.util.AsciiArt;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.websocket.server.ServerContainer;

/**
 *
 * @author Andres Olarte
 *
 */
public class ContextLifecycleListener implements ServletContextListener {

	public ContextLifecycleListener() {
	    AsciiArt.doArt();
	}

    public void contextDestroyed(ServletContextEvent arg0) {
        Logger.info(this, "Context destruction triggered - checking coordinated shutdown status");
        
        try {
            ShutdownCoordinator.ShutdownStatus status = ShutdownCoordinator.getShutdownStatus();
            
            if (status.isShutdownCompleted()) {
                Logger.info(this, "Coordinated shutdown already completed by signal handler");
                return;
            }
            
            if (status.isShutdownInProgress()) {
                Logger.info(this, "Coordinated shutdown in progress from signal handler, waiting for completion");
                // Wait briefly for shutdown to complete
                try {
                    Thread.sleep(2000); // Give shutdown time to complete
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return;
            }
            
            // If we get here, shutdown was not triggered by signal handler (unusual)
            Logger.warn(this, "Shutdown not yet started by signal handler, initiating fallback from context destroyed");
            ShutdownCoordinator coordinator = ShutdownCoordinator.getInstance();
            boolean success = coordinator.shutdown();
            
            if (success) {
                Logger.info(this, "Fallback coordinated shutdown completed successfully");
            } else {
                Logger.warn(this, "Fallback coordinated shutdown completed with warnings");
            }
            
        } catch (Exception e) {
            Logger.error(this, "Error during coordinated shutdown in context destruction: " + e.getMessage(), e);
        }
    }

	public void contextInitialized(ServletContextEvent arg0) {
        Logger.info(this,"ContextLifecycleListener contextInitialized called");
        ByteBuddyFactory.init();

		Config.setMyApp(arg0.getServletContext());

        installWebSocket(arg0.getServletContext());
        Logger.info(this,"ContextLifecycleListener contextInitialized completed");
	}

    private void installWebSocket(final ServletContext serverContext) {

        try {

            Logger.info(this, "Installing the web socket");
            var container = (ServerContainer) serverContext.getAttribute("javax.websocket.server.ServerContainer");
            container.addEndpoint(SystemEventsWebSocketEndPoint.class);
        } catch (Throwable e) {

            Logger.error(this, e.getMessage());
            Logger.debug(this, e.getMessage(), e);
        }
    }

}
