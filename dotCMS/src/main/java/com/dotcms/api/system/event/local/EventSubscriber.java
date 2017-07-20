package com.dotcms.api.system.event.local;

/**
 * Interface for the EventSubscriber.
 * @author jsanca
 */
public interface EventSubscriber {

    default String getId () { return  this.getClass().getName(); }

    void notify (Object event);
}
