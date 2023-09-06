package com.dotcms.cdi;

import com.dotmarketing.listeners.ContextLifecycleListener.StartupEvent;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.CDI;

/**
 * CDI Utils
 */
@ApplicationScoped
public class CDIUtils {

    /**
     * Get a bean from CDI
     * @param clazz
     * @return
     * @param <T>
     */
    public static <T> T getBean(final Class<T> clazz) {
        return CDI.current().select(clazz).get();
    }

    // todo: if there is a listener may need a fire event here?


    public void listen(@Observes StartupEvent event) {
        Logger.info(Config.class, "Starting Config Service");
        Config.initializeConfig();
    }
}
