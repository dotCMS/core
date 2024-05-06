package com.dotcms.ai.util;

import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.app.ConfigService;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class OpenAIThreadPool {
    private OpenAIThreadPool(){

    }

    private static final AtomicBoolean running = new AtomicBoolean(true);
    private static ScheduledExecutorService service;

    public static void schedule(Runnable task, long increment, TimeUnit unit) {
        threadPool().schedule(task, increment, unit);
    }

    private static ScheduledExecutorService threadPool() {

        if (running.get() && service != null && !service.isShutdown() && !service.isTerminated()) {
            return service;
        }

        synchronized (OpenAIThreadPool.class) {
            if (running.get() && service != null && !service.isShutdown() && !service.isTerminated()) {
                return service;
            }
            int threads = ConfigService.INSTANCE.config().getConfigInteger(AppKeys.EMBEDDINGS_THREADS);
            int maxThreads = ConfigService.INSTANCE.config().getConfigInteger(AppKeys.EMBEDDINGS_THREADS_MAX);


            service = Executors.newScheduledThreadPool(Math.max(threads, maxThreads), new SimpleThreadFactory());

        }
        return service;
    }

    public static void submit(Runnable task) {
        threadPool().schedule(task, 0, TimeUnit.MILLISECONDS);
    }


    public static void shutdown() {
        if (!running.getAndSet(false)) {
            return;
        }
        if (service != null) {
            service.shutdown();
        }
    }

    private static class SimpleThreadFactory implements ThreadFactory {

        private final AtomicInteger threadNumber = new AtomicInteger(1);


        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "dotAI-thread-" + threadNumber.getAndIncrement());
            t.setDaemon(true);
            return t;
        }
    }

}
