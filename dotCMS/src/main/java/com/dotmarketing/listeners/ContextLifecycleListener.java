package com.dotmarketing.listeners;

import com.dotcms.business.bytebuddy.ByteBuddyFactory;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.rest.api.v1.system.websocket.SystemEventsWebSocketEndPoint;
import com.dotcms.util.AsciiArt;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.reindex.ReindexThread;
import com.dotmarketing.loggers.Log4jUtil;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import io.vavr.control.Try;

import java.io.Serializable;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.websocket.server.ServerContainer;
import java.io.File;
import org.apache.logging.log4j.core.async.BasicAsyncLoggerContextSelector;

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
        Logger.info(this, "Shutdown : Started, executing a clean shutdown.");

        Try.run(() -> QuartzUtils.stopSchedulers())
                        .onFailure(e -> Logger.warn(ContextLifecycleListener.class, "Shutdown : " + e.getMessage()));
        
        Try.run(() -> LicenseUtil.freeLicenseOnRepo())
                        .onFailure(e -> Logger.warn(ContextLifecycleListener.class, "Shutdown : " + e.getMessage()));

        
        Try.run(() -> CacheLocator.getCacheAdministrator().shutdown())
                        .onFailure(e -> Logger.warn(ContextLifecycleListener.class, "Shutdown : " + e.getMessage()));
        


        Try.run(() -> DotConcurrentFactory.getInstance().shutdownAndDestroy())
                        .onFailure(e -> Logger.warn(ContextLifecycleListener.class, "Shutdown : " + e.getMessage()));

        Try.run(() -> ReindexThread.stopThread())
                        .onFailure(e -> Logger.warn(ContextLifecycleListener.class, "Shutdown : " + e.getMessage()));

        Logger.info(this, "Shutdown : Finished.");

    }

	public void contextInitialized(ServletContextEvent arg0) {

        ByteBuddyFactory.init();

        BeanManager beanManager = CDI.current().getBeanManager();
        if (beanManager != null) {
            beanManager.fireEvent(new StartupEvent());
            Logger.info(this,"beanManager fired StartupEvent.");
        } else {
            Logger.error(this,"beanManager is null.  Cannot fire startup event.");
        }


		Config.setMyApp(arg0.getServletContext());


        String path = null;
		try {

            String contextPath = Config.CONTEXT.getRealPath("/");
            if ( !contextPath.endsWith( File.separator ) ) {
                contextPath += File.separator;
            }
			File file = new File(contextPath + "WEB-INF" + File.separator + "log4j" + File.separator + "log4j2.xml");
			path = file.toURI().toString();

        } catch (Exception e) {
			Logger.error(this,e.getMessage(),e);
		}

		// Do not reconfigure if using global configuration.  Remove this if we move
        // a full global configuration
        if (System.getProperty("Log4jContextSelector").equals(BasicAsyncLoggerContextSelector.class.getName()))
            Log4jUtil.initializeFromPath(path);
        else
            Logger.debug(this, "Reinitializing configuration from "+path);


        installWebSocket(arg0.getServletContext());
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


    public static class StartupEvent implements Serializable {
    }
}
