package com.dotcms.concurrent;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class BulkActionsNamedThreadFactory implements ThreadFactory {

    private static final String THREAD_NAME_FORMAT = "bulk_action_thread_%d";

    private final AtomicInteger threadCount = new AtomicInteger(0);

    @Override
    public Thread newThread(final Runnable r) {
        final Thread t = Executors.defaultThreadFactory().newThread(r);
        t.setName(String.format(THREAD_NAME_FORMAT, threadCount.getAndIncrement()));
        return t;
    }
}
