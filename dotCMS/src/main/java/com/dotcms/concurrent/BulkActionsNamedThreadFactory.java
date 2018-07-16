package com.dotcms.concurrent;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class BulkActionsNamedThreadFactory implements ThreadFactory {

    private static final String THREAD_NAME_FORMAT = "bulk_action_thread_%d";

    private final AtomicInteger threadCount = new AtomicInteger(0);

    @Override
    public Thread newThread(final Runnable runnable) {
        final Thread thread = Executors.defaultThreadFactory().newThread(runnable);
        thread.setName(String.format(THREAD_NAME_FORMAT, threadCount.getAndIncrement()));
        return thread;
    }
}
