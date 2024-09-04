package com.dotcms.rest.config;

import com.dotmarketing.util.Logger;
import java.util.Objects;
import java.util.Set;
import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.ext.Provider;
import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEvent.Type;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;

@Provider
@ApplicationScoped
public class JerseyApplicationEventListener implements ApplicationEventListener {

    @Override
    public void onEvent(ApplicationEvent event) {
        if (Objects.requireNonNull(event.getType()) == Type.INITIALIZATION_FINISHED) {
            logLoadedClasses(event);
        }
    }

    private void logLoadedClasses(ApplicationEvent event) {
        Set<Class<?>> resourceClasses = event.getResourceConfig().getClasses();
        Logger.info(JerseyApplicationEventListener.class,"Loaded resource classes:");
        resourceClasses.forEach(clazz -> Logger.info(JerseyApplicationEventListener.class,clazz.getName()));

        Set<Object> providerClasses = event.getResourceConfig().getInstances();
        Logger.info(JerseyApplicationEventListener.class,"Loaded provider classes:");
        providerClasses.forEach(instance -> Logger.info(JerseyApplicationEventListener.class,instance.getClass().getName()));
    }

    @Override
    public RequestEventListener onRequest(RequestEvent requestEvent) {
        return null; // No specific request events handling
    }
}