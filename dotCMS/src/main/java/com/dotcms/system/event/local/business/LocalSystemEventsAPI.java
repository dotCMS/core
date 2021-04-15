package com.dotcms.system.event.local.business;

import com.dotcms.system.event.local.model.DefaultOrphanEventSubscriber;
import com.dotcms.system.event.local.model.EventCompletionHandler;
import com.dotcms.system.event.local.model.EventSubscriber;
import com.dotcms.system.event.local.type.OrphanEvent;

/**
 * A local system event allows to add classes, annotated by Subscriber annotation.
 * The method must have just one parameter that will be the Event Type, when this event type is notified, all class subscribed will
 * be triggered.
 * @author jsanca
 */
public interface LocalSystemEventsAPI {

    /**
     * Do the subscription of all methods annotated with Subscriber.
     * The method must have just one parameter which is the event to be received when the notification is triggered.
     * @param subscriber
     */
    void subscribe (Object subscriber);

    /**
     * Do the subscription for an event
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
     * Unsubscribes all methods annotated with Subscriber.
     * @param subscriber {@link Object}
     */
    boolean unsubscribe (Object subscriber);

    /**
     * Unsubscribes a specific subscriber
     * @param eventType {@link Class}
     * @param subscriberId {@link String}
     */
    boolean unsubscribe (final Class<?> eventType, String subscriberId);

    /**
     * This method brings the possibility for event broadcasters to be non-blocking notified
     * when all subscribers are done consuming their events.
     * @param event {@link Object}
     * @param completionHandler {@link EventCompletionHandler}
     */
    void asyncNotify(final Object event, final EventCompletionHandler completionHandler);

    /**
     * Creates an async notification to all registered handlers
     * @param event {@link Object}
     */
    void asyncNotify (Object event);


    /**
     * Creates a sync notification to all registered handlers.
     * @param event
     */
    public void notify(final Object event);

    /**
     * Finds the subscriber
     * @param eventType {@link Class}
     * @param subscriberId {@link String}
     * @return EventSubscriber
     */
    EventSubscriber findSubscriber (final Class<?> eventType, String subscriberId);

    /**
     * Sets the Event Subscriber for the Orphan Events (events with not subscriber associated)
     * By default the subscriber is {@link DefaultOrphanEventSubscriber}
     *
     * @param orphanEventSubscriber {@link EventSubscriber}
     */
    void setOrphanEventSubscriber (EventSubscriber<OrphanEvent> orphanEventSubscriber);
} // E:O:F:LocalSystemEventsAPI.
