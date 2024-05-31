package com.dotcms.api.client.util;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.jboss.logging.Logger;

public class DirectoryWatcherService {

    private final Logger logger = Logger.getLogger(DirectoryWatcherService.class);

    private final Queue<WatchEvent<?>> eventQueue = new ConcurrentLinkedQueue<>();
    private final Map<WatchKey, Path> keys = new HashMap<>();
    private WatchService watchService;
    private ScheduledExecutorService scheduler;

    @SuppressWarnings("unchecked")
    public void watch(final Path path, final long pollInterval, final boolean onlyShowLastEvent, final WatchEventConsumer eventConsumer) throws IOException, InterruptedException {
        watchService = FileSystems.getDefault().newWatchService();
        registerAll(path);

        logger.debug("Watching directory: " + path);

        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            try {
                processEvents(onlyShowLastEvent, eventConsumer);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException | ExecutionException e) {
                Thread.currentThread().interrupt();
            }
        }, pollInterval, pollInterval, TimeUnit.SECONDS);

        while (true) {
            WatchKey key = watchService.take();
            Path dir = keys.get(key);

            if (dir == null) {
                logger.error("WatchKey not recognized!!");
                continue;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();
                if (kind == StandardWatchEventKinds.OVERFLOW) {
                    continue;
                }

                WatchEvent<Path> ev = (WatchEvent<Path>) event;
                Path name = ev.context();
                Path child = dir.resolve(name);

                eventQueue.add(event);
                logger.debug(kind.name() + ": " + child);

                if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                    if (Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS)) {
                        registerAll(child);
                    }
                }
            }

            boolean valid = key.reset();
            if (!valid) {
                keys.remove(key);
                if (keys.isEmpty()) {
                    logger.debug("All directories are inaccessible, stopping watch service.");
                    watchService.close();
                    break;
                }
            }
        }
    }

    private void registerAll(final Path start) throws IOException {
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                register(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void register(Path dir) throws IOException {
        WatchKey key = dir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
        keys.put(key, dir);
        logger.debug("Registered: " + dir);
    }

    @SuppressWarnings("unchecked")
    private void processEvents(final boolean onlyShowLastEvent, WatchEventConsumer eventConsumer)
            throws IOException, InterruptedException, ExecutionException {
        if (onlyShowLastEvent) {
            WatchEvent<?> lastEvent = null;
            WatchEvent<?> event;
            while ((event = eventQueue.poll()) != null) {
                lastEvent = event;
            }
            if (lastEvent != null) {
                eventConsumer.accept((WatchEvent<Path>) lastEvent);
            }
        } else {
            WatchEvent<?> event;
            while ((event = eventQueue.poll()) != null) {
                eventConsumer.accept((WatchEvent<Path>) event);
            }
        }
    }

    public void stopWatching() throws IOException {
        if (scheduler != null) {
            scheduler.shutdown();
        }
        if (watchService != null) {
            watchService.close();
        }
    }

    @FunctionalInterface
    public interface WatchEventConsumer {
        void accept(WatchEvent<Path> event)
                throws IOException, InterruptedException, ExecutionException;
    }
}


