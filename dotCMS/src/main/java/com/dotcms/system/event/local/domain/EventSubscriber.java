package com.dotcms.system.event.local.domain;

/**
 * Interface for the EventSubscriber.
 * @author jsanca
 */
public interface EventSubscriber<T> {

    default String getId () { return  this.getClass().getName(); }

    void notify (T event);
}
