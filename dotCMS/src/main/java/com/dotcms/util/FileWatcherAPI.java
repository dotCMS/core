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
public class FileWatcherAPI implements Serializable, Closeable {

    private   final ExecutorService executor;
    protected final WatchService    watcher;
    protected final ConcurrentMap<WatchKey, Path>            watchKeyPathMap;
    protected final ConcurrentMap<Path, FileModifiedWatcher> pathModFileWatcherMap;

    public FileWatcherAPI() throws IOException {

        this (Executors.newSingleThreadExecutor(), FileSystems.getDefault().newWatchService());
    }


    @VisibleForTesting
    public FileWatcherAPI(final ExecutorService executor,
                          final WatchService watcher) {

        this.executor = executor;
        this.watcher  = watcher;
        this.watchKeyPathMap       = new ConcurrentHashMap<>();
        this.pathModFileWatcherMap = new ConcurrentHashMap<>();
        this.startWatcherService ();
    }

    private void startWatcherService() {

        this.executor.submit(() -> {
            while (Thread.interrupted() == false) {

                this.processEvents ();
            }
        });
    } // startWatcherService.

    /**
     * Run the process events
     */
    protected void processEvents() {

        WatchKey   key   = null;
        final Path path;

        try {
            // wait for events.
            key = this.watcher.take();
        } catch (InterruptedException x) {

            return;
        }

        path = this.watchKeyPathMap.get(key); // get the directory associated tot he watchkey.

        if (null == path) {

            Logger.debug(this, "Path not recognized, for watchkey: " + key);
            return;
        }

        key.pollEvents().stream()
                .filter(event  -> event.kind() != OVERFLOW)
                .forEach(event -> this.processEvent(event, path));

        if (!key.reset()) {

            this.pathModFileWatcherMap.remove
                    (this.watchKeyPathMap.remove(key));
        }
    } // processEvents.

    /**
     * Process an event
     * @param event {@link WatchKey}
     * @param path  {@link Path}
     */
    protected void processEvent(final WatchEvent<?> event, final Path path) {

        if (event.kind() == ENTRY_MODIFY) {

            this.processModifyEvent(path);
        }
    } // processEvent.

    /**
     * Process a modify event path
     * @param path {@link Path}
     */
    protected void processModifyEvent(final Path path) {

        final FileModifiedWatcher fileModifiedWatcher =
                this.pathModFileWatcherMap.get(path);

        Logger.debug(this, "The path: " + path + " has changed");

        if (null != fileModifiedWatcher) {

            fileModifiedWatcher.onModified();
        } else {

            Logger.debug(this, "Not FileModifiedWatcher associated to the path: " + path);
        }
    } // processModifyEvent.

    /**
     * Watch a single file triggered a modified event to the fileWatcher.
     * @param file {@link String}
     * @param fileWatcher {@link FileModifiedWatcher}
     */
    public void watchFile (final String file, final FileModifiedWatcher fileWatcher) throws IOException {

        this.watchFile(new File(file), fileWatcher);
    } // watchFile
    /**
     * Watch a single file triggered a modified event to the fileWatcher.
     * @param file {@link File}
     * @param fileWatcher {@link FileModifiedWatcher}
     */
    public void watchFile (final File file, final FileModifiedWatcher fileWatcher) throws IOException {

        if (null != file && file.exists() && file.canRead()) {

            this.addFileModifiedWatcher (file, fileWatcher);
        } else {

            Logger.debug(this, "The file: " + file + ", is not a valid parameter to watch");
            throw new IllegalArgumentException(
                    "File parameter is not valid, could be null, does not exists or can not be read");
        }
    } // watchFile.

    /**
     * Stop watching a file, it just removes the Watcher handler, since there is not way to stop the path watch registration over the path
     * @param watchFile
     */
    public void stopWatchingFile (final File watchFile) {

        final Path     path     = Paths.get(watchFile.getAbsolutePath());
        Path           pathItem = null;
        WatchKey       watchKey = null;

        for (WatchKey key : this.watchKeyPathMap.keySet()) {

            pathItem = this.watchKeyPathMap.get(key);
            if (null != pathItem && pathItem.equals(path)) {

                watchKey = key; break;
            }
        }

        if (null != watchKey) {

            this.watchKeyPathMap.remove(watchKey);
        }

        this.pathModFileWatcherMap.remove(watchFile);

        // todo: research how to unregister the events over the file.
    } // stopWatchingFile.

    protected void addFileModifiedWatcher(final File watchFile, final FileModifiedWatcher fileModifiedWatcher) throws IOException {

        final Path     path     = Paths.get(watchFile.getAbsolutePath());
        final WatchKey watchKey = path.getParent().register(this.watcher, ENTRY_MODIFY);

        this.watchKeyPathMap.put(watchKey, path);
        this.pathModFileWatcherMap.put(path, fileModifiedWatcher);
    } // doWatchFile.

    @Override
    public void close() throws IOException {

        try {

            this.watcher.close();
        } catch (IOException e) {

            Logger.error(this, "Error closing watcher service", e);
        }

        try {

            this.executor.shutdown();
        } catch (Exception e) {

            Logger.error(this, "Error closing executors for Watcher Service", e);
        }
    } // close.
} // E:O:F:FileWatcherAPI.
