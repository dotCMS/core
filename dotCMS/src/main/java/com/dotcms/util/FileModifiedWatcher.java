package com.dotcms.util;

import java.io.Serializable;

/**
 * Defines a file watcher contract to see changes (modified events) over the files.
 * @author jsanca
 */
public interface FileModifiedWatcher extends Serializable {

    /**
     * This method will be called when the file wll be changed in the File System.
     */
    void onModified ();

} // E:O:F:FileModifiedWatcher.
