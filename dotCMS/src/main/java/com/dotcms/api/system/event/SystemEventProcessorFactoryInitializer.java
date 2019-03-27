package com.dotcms.api.system.event;

import com.dotcms.config.DotInitializer;
import com.dotcms.notifications.BaseContentTypeSystemEventProcessor;
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

        BaseContentTypeSystemEventProcessor baseContentTypeSystemEventProcessor = new BaseContentTypeSystemEventProcessor();

        factory.register(SystemEventType.DELETE_BASE_CONTENT_TYPE, baseContentTypeSystemEventProcessor);
        factory.register(SystemEventType.SAVE_BASE_CONTENT_TYPE, baseContentTypeSystemEventProcessor);
        factory.register(SystemEventType.UPDATE_BASE_CONTENT_TYPE, baseContentTypeSystemEventProcessor);
    }
} // E:O:F:SystemEventProcessorFactoryInitializer.
