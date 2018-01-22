package com.dotcms.system.event.local.business;

import com.dotcms.system.event.local.model.EventSubscriber;

import java.util.Map;

/**
 * Based on a strategy will return the event type associated to the EventSubscriber.
 * @author jsanca
 */
public interface EventSubscriberFinder {

    /**
     * Finds a set of {@link EventSubscriber} based on some strategy and returns a map with the event type associated
     * to the {@link EventSubscriber}
     *
     * @param subcriber {@link Object}  object with the subscriber to register
     * @return Map of Class type (event type) associated to the {@link EventSubscriber}
     *
     * @throws IllegalArgumentException if {@code source} is not appropriate for
     *         this strategy (in ways that this interface does not define).
     */
    Map<Class<?>, EventSubscriber> findSubscribers(Object subcriber);
} // E:O:F:EventSubscriberFinder.
