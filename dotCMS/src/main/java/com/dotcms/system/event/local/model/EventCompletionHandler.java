package com.dotcms.system.event.local.model;


/**
 * This EventCompletionHandler is consumer that will notify any event-broadcaster of the event completion
 * Meaning once all the subscribers are done with the event processing this event functional interface will be invoked.
 * It's mostly meant for non-blocking cleanup after asynchronous events are consumed.
 */
@FunctionalInterface
public interface EventCompletionHandler {

    /**
     * The event completed method handler
     * @param event
     */
    void onComplete(Object event);

}
