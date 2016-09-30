package com.dotcms.api.system.event;

import com.dotcms.config.DotInitializer;
import com.dotcms.notifications.NotificationSystemEventProcessor;

/**
 * This class inits the {@link SystemEventProcessorFactory}
 * @author jsanca
 */
public class SystemEventProcessorFactoryInitializer implements DotInitializer {


    @Override
    public void init() {

        final SystemEventProcessorFactory factory =
                SystemEventProcessorFactory.getInstance();

        factory.register(SystemEventType.NOTIFICATION, new NotificationSystemEventProcessor());
    }
} // E:O:F:SystemEventProcessorFactoryInitializer.
