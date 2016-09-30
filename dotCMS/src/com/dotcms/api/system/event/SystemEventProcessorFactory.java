package com.dotcms.api.system.event;

import com.dotcms.rest.api.v1.system.websocket.SessionWrapper;

import javax.websocket.Session;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory for {@link SystemEventProcessor}
 * @author jsanca
 */
public class SystemEventProcessorFactory implements Serializable {

    private final Map<SystemEventType, SystemEventProcessor> processorInstancesMap =
            new ConcurrentHashMap<>();

    private final static DoNothingSystemEventProcessor DO_NOTHING_SYSTEM_EVENT_PROCESSOR =
            new DoNothingSystemEventProcessor();

    private SystemEventProcessorFactory () {
        // singleton
    }

    private static class SingletonHolder {
        private static final SystemEventProcessorFactory INSTANCE = new SystemEventProcessorFactory();
    }
    /**
     * Get the instance.
     * @return JsonWebTokenFactory
     */
    public static SystemEventProcessorFactory getInstance() {

        return SystemEventProcessorFactory.SingletonHolder.INSTANCE;
    } // getInstance.

    /**
     * Register a new processor
     * @param type {@link SystemEventType}
     * @param processor {@link SystemEventProcessor}
     */
    public void register (final SystemEventType type, SystemEventProcessor processor) {

        this.processorInstancesMap.put(type, processor);
    } // register.

    /**
     * Creates the processor associated to the type, if not any, will return a default processor.
     * @param type {@link SystemEventType}
     * @return SystemEventProcessor
     */
    public SystemEventProcessor createProcessor (final SystemEventType type) {

        return this.processorInstancesMap.containsKey(type)?
                this.processorInstancesMap.get(type):DO_NOTHING_SYSTEM_EVENT_PROCESSOR;
    }

    private static class DoNothingSystemEventProcessor implements SystemEventProcessor {

        @Override
        public SystemEvent process(SystemEvent event, Session session) {
            return event;
        }
    }

} // E:O:F:SystemEventProcessorFactory.
