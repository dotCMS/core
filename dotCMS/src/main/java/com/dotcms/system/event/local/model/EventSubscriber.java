package com.dotcms.system.event.local.model;

/**
 * Interface for the EventSubscriber.
 * @author jsanca
 */
public interface EventSubscriber<T> {

    default String getId () { return  this.getClass().getName(); }

    void notify (T event);
}
