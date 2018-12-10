package com.dotmarketing.portlets.folders.business;

/**
 * Listener to handle Folder events, by now just handle the child deletes
 * @author jsanca
 */
public interface FolderListener {

    /**
     * Gets the identifier, by default the class name
     * @return String
     */
    default String getId () {
        return this.getClass().getName();
    }

    /**
     * When a child is deleted this event is triggered
     * @param folderEvent {@link FolderEvent}
     */
    default void folderChildDeleted(FolderEvent folderEvent) { }

    /**
     * When a child is being modified this event is triggered
     * @param folderEvent {@link FolderEvent}
     */
    default void folderChildModified(FolderEvent folderEvent) {}
}
