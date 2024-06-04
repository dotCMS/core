package com.dotcms.api.client.util;

import io.quarkus.runtime.ShutdownEvent;
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
import javax.enterprise.context.ApplicationScoped;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.enterprise.event.Observes;
import org.jboss.logging.Logger;

@ApplicationScoped
public class DirectoryWatcherService {

    private final BlockingQueue<WatchEvent<?>> eventQueue = new LinkedBlockingQueue<>();
    private final ScheduledExecutorService scheduler;
    private final WatchService watchService;
    private final Logger logger = Logger.getLogger(DirectoryWatcherService.class);
    private final Set<Path> paths = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean running = new AtomicBoolean(false);

    public DirectoryWatcherService() throws IOException {
        watchService = FileSystems.getDefault().newWatchService();
        scheduler = Executors.newScheduledThreadPool(1);
    }

    public BlockingQueue<WatchEvent<?>> watch(Path path, long pollIntervalSeconds) throws IOException {
        registerAll(path);
        if(running.compareAndSet(false, true)){
            scheduler.scheduleAtFixedRate(this::processEvents, 0, pollIntervalSeconds, TimeUnit.SECONDS);
        }
        return eventQueue;
    }

    private void processEvents() {
        try{
            pollEvents();
        } catch (Exception e) {
            logger.error("Error processing events", e);

        }
    }

    private void pollEvents() {
        final WatchKey key = watchService.poll();
        logger.debug("Processing events  ");
        if (key != null) {
            WatchEvent<?> lastEvent = null;
            for (WatchEvent<?> event : key.pollEvents()) {
                lastEvent = event;
            }
            if (lastEvent != null) {
                eventQueue.add(lastEvent);
            }
            key.reset();
        }
    }

    public boolean isRunning() {
        return running.get();
    }

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