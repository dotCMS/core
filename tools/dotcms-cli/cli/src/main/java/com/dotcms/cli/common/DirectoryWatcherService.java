package com.dotcms.cli.common;

import io.quarkus.runtime.ShutdownEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.jboss.logging.Logger;

/**
 * Service that watches a directory for changes and registers events in a queue.
 * You can not subscribe to events, but you can poll the queue for changes.
 * This is by design to avoid losing the current thread context.
 */
@ApplicationScoped
public class DirectoryWatcherService {

    private final BlockingQueue<WatchEvent<?>> eventQueue = new LinkedBlockingQueue<>();
    private final ScheduledExecutorService scheduler;
    private final WatchService watchService;
    private final Logger logger = Logger.getLogger(DirectoryWatcherService.class);
    private final Set<Path> paths = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean suspended = new AtomicBoolean(false);


    /**
     * Creates a new DirectoryWatcherService
     * @throws IOException if an I/O error occurs
     */
    public DirectoryWatcherService() throws IOException {
        watchService = FileSystems.getDefault().newWatchService();
        scheduler = Executors.newScheduledThreadPool(1);
    }


    /**
     * Watch a directory for changes
     * @param path the directory to watch
     * @param pollIntervalSeconds the interval in seconds to poll for changes
     * @return the event queue
     * @throws IOException if an I/O error occurs
     */
    @SuppressWarnings("java:S1452")
    public BlockingQueue<WatchEvent<?>> watch(Path path, long pollIntervalSeconds) throws IOException {
        registerAll(path);
        if(running.compareAndSet(false, true)){
            scheduler.scheduleAtFixedRate(this::processEvents, 0, pollIntervalSeconds, TimeUnit.SECONDS);
        }
        return eventQueue;
    }

    /**
     * Process events
     */
    private void processEvents() {
        try{
            if (!suspended.get()) {
                pollEvents();
            }
        } catch (Exception e) {
            logger.error("Error processing events", e);
        }
    }

    /**
     * Poll events internally and add them to the queue
     */
    private void pollEvents() {
        final WatchKey key = watchService.poll();
        logger.debug("Processing events  ");
        if (key != null) {
            WatchEvent<?> lastEvent = null;
            for (WatchEvent<?> event : key.pollEvents()) {
                logger.debug("Event kind:" + event.kind() + ". File affected: " + event.context());
                lastEvent = event;
            }
            if (lastEvent != null) {
                eventQueue.add(lastEvent);
            }
            key.reset();
        }
    }

    /**
     * Check if the service is running
     * @return true if the service is running
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Check if the service is suspended (Not processing events) but still running
     * @return true if the service is suspended
     */
    public boolean isSuspended() {
        return suspended.get();
    }

    /**
     * Suspend the service (Stop processing events)
     */
    public void suspend() {
        suspended.set(true);
    }

    /**
     * Resume the service (Start processing events)
     */
    public void resume() {
        suspended.set(false);
    }

    /**
     * Stop the service
     */
    public void stop() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }

    private void registerAll(final Path start) throws IOException {
        if(paths.contains(start)){
            return;
        }
        Files.walkFileTree(start, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                register(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void register(Path dir) throws IOException {
        dir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
        paths.add(dir);
    }

    void onShutdown(@Observes ShutdownEvent event) {
        stop();
    }
}