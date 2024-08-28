package com.dotmarketing.microprofile.config;

import com.dotmarketing.listeners.ContextLifecycleListener.StartupEvent;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.CDI;

@ApplicationScoped
public class CDIUtils {

    public static <T> T getBean(Class<T> clazz) {
        return CDI.current().select(clazz).get();
    }

    public void listen(@Observes StartupEvent event) {
        Logger.info(Config.class, "Starting Config Service");
        Config.initializeConfig();
    }
}
