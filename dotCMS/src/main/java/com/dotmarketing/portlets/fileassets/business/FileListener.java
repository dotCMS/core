package com.dotmarketing.portlets.fileassets.business;

/**
 * Listener to handle a File events
 * @author jsanca
 */
public interface FileListener {

    /**
     * Gets the identifier, by default the class name
     * @return String
     */
    default String getId () {
        return this.getClass().getName();
    }

    /**
     * When a file that is being modified this event is triggered, match the listener criteria,
     * @param fileEvent {@link FileEvent}
     */
    default void fileModify(FileEvent fileEvent) {}
}
