package com.dotcms.rest.config;

import com.dotmarketing.util.Logger;
import java.util.concurrent.atomic.AtomicReference;
import javax.enterprise.context.ApplicationScoped;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.ws.rs.ext.Provider;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.spi.AbstractContainerLifecycleListener;
import org.glassfish.jersey.server.spi.Container;

/**
 * A new Reloader will get created on each reload there can only be one container at a time
 * Now with Weld CDI management capabilities
 */
@Provider
@ApplicationScoped
public class ContainerReloader extends AbstractContainerLifecycleListener implements
        ServletContextListener {

    private static final AtomicReference<Container> containerRef = new AtomicReference<>();

    @Override
    public void onStartup(Container container) {
        Logger.debug(ContainerReloader.class, "Jersey Container started");
        containerRef.set(container);
    }

    @Override
    public void onShutdown(Container container) {
        Logger.debug(ContainerReloader.class, "Jersey Container shutdown");
        containerRef.set(null);

    }

    @Override
    public void onReload(Container container) {
        Logger.debug(ContainerReloader.class, "Jersey Container reloaded");
    }


    public void reload() {
        Container container = containerRef.get();
        Logger.debug(ContainerReloader.class, "Jersey Reloading request");
        if (container != null) {
            final ResourceConfig config = ResourceConfig.forApplicationClass(DotRestApplication.class);
            container.reload(config);
        } else {
            Logger.error(ContainerReloader.class, "Jersey Container not available");
        }
    }

    public void reload(ResourceConfig customConfig) {
        Container container = containerRef.get();
        Logger.debug(ContainerReloader.class, "Jersey Reloading request");
        if (container != null) {
            container.reload(customConfig);
        }
    }


    @Override
    public void contextInitialized(ServletContextEvent sce) {
        Logger.debug(ContainerReloader.class, "Jersey Container context initialized");
        // This is where you can initialize any resources or configurations needed at startup
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        Logger.debug(ContainerReloader.class, "Jersey Container context destroyed!");
    }
}