package com.dotcms.util;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.util.Logger;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

/**
 * This service allows to watch single files changed (only file Modifications by now).
 * @author jsanca
 */
public interface FileWatcherAPI extends Serializable {


    /**
     * Watch a single file triggered a modified event to the fileWatcher.
     * @param file {@link String}
     * @param fileWatcher {@link FileModifiedWatcher}
     */
    public void watchFile (final String file, final FileModifiedWatcher fileWatcher) throws IOException;
    /**
     * Watch a single file triggered a modified event to the fileWatcher.
     * @param file {@link File}
     * @param fileWatcher {@link FileModifiedWatcher}
     */
    public void watchFile (final File file, final FileModifiedWatcher fileWatcher) throws IOException;

    /**
     * Stop watching a file, it just removes the Watcher handler, since there is not way to stop the path watch registration over the path
     * @param watchFile
     */
    public void stopWatchingFile (final File watchFile);

} // E:O:F:FileWatcherAPI.
