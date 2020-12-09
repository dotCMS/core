package com.dotcms.publisher.bundle.business;

import com.dotcms.publisher.pusher.PushUtils;
import com.dotmarketing.util.Logger;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

public class BundleTarGzipCreator {
    private BundleTarGzipMonitor bundleTarGzipMonitor;

    private Path bundleRootDirectory;
    private boolean close = false;

    private BundleTarGzipCreator(final Path bundleRootDirectory){
        this.bundleRootDirectory = bundleRootDirectory;
        bundleTarGzipMonitor = new BundleTarGzipMonitor();
        bundleTarGzipMonitor.start();
    }

    public static BundleTarGzipCreator start(final Path bundlePath){
        return new BundleTarGzipCreator(bundlePath);
    }

    public File get(){
        return bundleTarGzipMonitor.get();
    }

    public void close() {
        close = true;
    }

    public void join() {
        try {
            bundleTarGzipMonitor.thread.join();
        } catch (InterruptedException e) {
            Logger.error(BundleTarGzipCreator.class, e);
        }
    }

    private class BundleTarGzipMonitor implements Runnable{
        private File tarGzipFile;
        private WatchService watcher;
        private Thread thread;

        public void start() {
            thread = new Thread(this);
            thread.start();
        }

        @Override
        public void run() {
            try {
                watcher = FileSystems.getDefault().newWatchService();

                final Path bundleRootDirectory = BundleTarGzipCreator.this.bundleRootDirectory;
                WatchKey key = bundleRootDirectory.register(watcher, ENTRY_CREATE, ENTRY_MODIFY);
                tarGzipFile = PushUtils.createTarGzipFile(bundleRootDirectory.toFile());

                try(final TarArchiveOutputStream tarArchiveOutputStream =
                            PushUtils.createTarArchiveOutputStream(tarGzipFile)) {

                    for (;;) {
                        WatchKey curentKey = null;

                       try {
                           Logger.info(BundleTarGzipMonitor.class, "Waiting for new files");

                           if (BundleTarGzipCreator.this.close) {
                               Logger.info(BundleTarGzipMonitor.class, "... and it is close");

                               curentKey = watcher.poll(1, TimeUnit.SECONDS);

                               Logger.info(BundleTarGzipMonitor.class, curentKey == null ?
                                       "...and not have nothing else to process" : "... and have more");

                               if (curentKey == null) {
                                   this.watcher.close();
                                   break;
                               }
                           } else {
                               curentKey = watcher.take();
                           }
                       } catch(ClosedWatchServiceException e){
                           Logger.info(BundleTarGzipMonitor.class, "ClosedWatchServiceException...");
                           if (!close) {
                               Logger.info(BundleTarGzipMonitor.class, "... and it is close");
                               Logger.error(BundleTarGzipMonitor.class, e);
                           }
                       } catch (InterruptedException e) {
                            Logger.error(BundleTarGzipMonitor.class, e);
                            return;
                       }

                        for (final WatchEvent<?> event: curentKey.pollEvents()) {
                            final WatchEvent<Path> ev = (WatchEvent<Path>)event;
                            Logger.info(BundleTarGzipCreator.class, "Event " + event.kind());
                            if (event.kind() == ENTRY_CREATE || event.kind() == ENTRY_MODIFY) {
                                final Path filename = ev.context();
                                Logger.info(BundleTarGzipCreator.class, "filename " + ev.context());
                                final Path filePath = bundleRootDirectory.resolve(filename);
                                PushUtils.addFilesToCompression(tarArchiveOutputStream, filePath.toFile(), ".",
                                        bundleRootDirectory.toFile().getAbsolutePath());

                                if (filePath.toFile().isDirectory()) {
                                    filePath.register(watcher, ENTRY_CREATE, ENTRY_MODIFY);
                                }
                            }
                        }

                        boolean valid = key.reset();
                        if (!valid) {
                            Logger.info(BundleTarGzipMonitor.class, "is it close " + BundleTarGzipCreator.this.close);
                            Logger.info(BundleTarGzipCreator.class, "TERMINO");

                            try {
                                this.watcher.close();
                            } catch(ClosedWatchServiceException e) {
                                Logger.warn(BundleTarGzipMonitor.class, e.getMessage(), e);
                            }

                            break;
                        }
                    }
                }
            } catch (IOException e) {
                Logger.error(BundleTarGzipMonitor.class, e);
            }
        }

        public File get(){
            return tarGzipFile;
        }

        public void close() {
            try {
                watcher.close();
            } catch (IOException e) {
                Logger.error(BundleTarGzipMonitor.class, e);
            }
        }
    }
}
