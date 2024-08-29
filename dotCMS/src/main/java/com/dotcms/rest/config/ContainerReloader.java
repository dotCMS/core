package com.dotcms.rest.config;

import com.dotmarketing.util.Logger;
import java.util.concurrent.atomic.AtomicReference;
import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.ext.Provider;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.spi.AbstractContainerLifecycleListener;
import org.glassfish.jersey.server.spi.Container;

/**
 * A new Reloader will get created on each reload there can only be one container at a time
 */
@Provider
@ApplicationScoped
public class ContainerReloader extends AbstractContainerLifecycleListener {

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
            container.reload(ResourceConfig.forApplicationClass(DotRestApplication.class));
        }
    }
}
