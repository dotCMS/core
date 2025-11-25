package com.dotcms.jobs.business.api.events;

import java.lang.annotation.Annotation;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;

/**
 * A producer class for CDI events. This class provides a centralized way to obtain event objects
 * for firing events within the application.
 *
 * <p>This class is application scoped, ensuring a single instance is used throughout
 * the application's lifecycle.</p>
 */
@ApplicationScoped
public class EventProducer {

    private BeanManager beanManager;

    public EventProducer() {
        // Default constructor for CDI
    }

    /**
     * Constructs a new EventProducer.
     *
     * @param beanManager The CDI BeanManager, injected by the container.
     */
    @Inject
    public EventProducer(BeanManager beanManager) {
        this.beanManager = beanManager;
    }

    /**
     * Retrieves an Event object for the specified event type and qualifiers.
     *
     * <p>This method allows for type-safe event firing. It uses the BeanManager to
     * create an Event object that can be used to fire events of the specified type.</p>
     *
     * <p>Usage example:</p>
     * <pre>
     * EventProducer producer = ...;
     * Event<MyCustomEvent> event = producer.getEvent(MyCustomEvent.class);
     * event.fire(new MyCustomEvent(...));
     * </pre>
     *
     * @param <T>        The type of the event.
     * @param eventType  The Class object representing the event type.
     * @param qualifiers Optional qualifiers for the event.
     * @return An Event object that can be used to fire events of the specified type.
     */
    public <T> Event<T> getEvent(Class<T> eventType, Annotation... qualifiers) {
        return beanManager.getEvent().select(eventType, qualifiers);
    }

}