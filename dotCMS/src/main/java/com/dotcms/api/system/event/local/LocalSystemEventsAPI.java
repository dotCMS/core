package com.dotcms.api.system.event.local;

/**
 * A local system events allows to add classes, annotated by Subscriber annotation.
 * The method must have just one parameter that will be the Event Type, when this event type is notified, all class subscribed will
 * be triggered the event.
 * @author jsanca
 */
public interface LocalSystemEventsAPI {

    /**
     * Do the subscription of all methods annotation with Subscriber.
     * The method most have just one parameter witch is the event to received when the notification is triggered.
     * @param subscriber
     */
    void subscribe (Object subscriber);

    /**
     * Do the subscription for a
     * @param eventType {@link Class} eventType
     * @param subscriber {@link EventSubscriber}
     */
    void subscribe(Class<?> eventType, EventSubscriber subscriber);

    /**
     * Unsubscribes all the {@link EventSubscriber} related to eventType.
     * @param eventType {@link Class}
     */
    boolean unsubscribe (Class<?> eventType);

    /**
     * Unsubscribes a specific subscribed
     * @param eventType {@link Class}
     * @param subscriberId {@link String}
     */
    boolean unsubscribe (final Class<?> eventType, String subscriberId);

    /**
     * Do an async notification to all registered handlers
     * @param event {@link Object}
     */
    void asyncNotify (Object event);


    /**
     * Do sync notification to all registered handlers.
     * @param event
     */
    public void notify(final Object event);

    /**
     * Find the subscriber
     * @param eventType {@link Class}
     * @param subscriberId {@link String}
     * @return EventSubscriber
     */
    EventSubscriber findSubscriber (final Class<?> eventType, String subscriberId);

    /**
     * Set's the Event Subscriber for the Orphan Events (events with not subscriber associated)
     * By default the subscriber is {@link DefaultOrphanEventSubscriber}
     *
     * @param orphanEventSubscriber {@link EventSubscriber}
     */
    void setOrphanEventSubscriber (EventSubscriber<OrphanEvent> orphanEventSubscriber);
} // E:O:F:LocalSystemEventsAPI.
