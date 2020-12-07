package com.dotcms.publisher.bundle.business;

import com.dotcms.publisher.pusher.PushUtils;
import com.dotmarketing.util.Logger;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.*;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;

public class BundleTarGzipCreator {
    private BundleTarGzipMonitor bundleTarGzipMonitor;

    private Path bundleRootDirectory;

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
        bundleTarGzipMonitor.close();
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
                final WatchKey key = bundleRootDirectory.register(watcher, ENTRY_CREATE);
                tarGzipFile = PushUtils.createTarGzipFile(bundleRootDirectory.toFile());

                try(final TarArchiveOutputStream tarArchiveOutputStream =
                            PushUtils.createTarArchiveOutputStream(tarGzipFile)) {

                    for (;;) {
                       try {
                            watcher.take();
                        } catch (InterruptedException e) {
                            Logger.error(BundleTarGzipMonitor.class, e);
                            return;
                        }

                        for (WatchEvent<?> event: key.pollEvents()) {
                            final WatchEvent<Path> ev = (WatchEvent<Path>)event;

                            if (event.kind() == ENTRY_CREATE) {
                                final Path filename = ev.context();

                                final Path filePath = bundleRootDirectory.resolve(filename);
                                PushUtils.addFilesToCompression(tarArchiveOutputStream, filePath.toFile(), ".",
                                        bundleRootDirectory.toFile().getAbsolutePath());
                            }
                        }

                        boolean valid = key.reset();
                        if (!valid) {
                            Logger.info(BundleTarGzipCreator.class, "TERMINO");
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
