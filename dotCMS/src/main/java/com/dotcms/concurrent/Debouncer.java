package com.dotcms.concurrent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import com.dotmarketing.util.Logger;
import com.google.common.annotations.VisibleForTesting;

/**
 * This class implements a debouncer, which is intended to prevent multiple calls to a single method
 * within a set peroid of time. Debouncing ensures that a method doesn’t get called too frequently,
 * but does get called when needed. This is important when trying to prevent run away calls to a
 * particularly expensiveMethod() - for example, calls to flush and rebuild cache, that happen in
 * response to user input or system actions. It allows us to call expensiveMethod() in code 1, 10 or 100
 * times, as many times as needed with confidence that finally, expensiveMethod() will only be called
 * once, after the final call to expensiveMethod() has been made.
 * 
 * 
 * Usage:
 * 
 * final Debouncer debouncer = new Debouncer(); 
 * debouncer.debounce("myRunnableKey",()->{runnable..}, 1000, TimeUnit.MILLISECONDS);
 * 
 */
public class Debouncer {

    @VisibleForTesting
    Debouncer(Long runCount) {
        this.runCount = runCount;
    }

    public Debouncer() {

    }

    @VisibleForTesting
    Long runCount = null;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ConcurrentHashMap<String, Future<?>> delayedMap = new ConcurrentHashMap<>();

    /**
     * Debounces {@code callable} by {@code delay}, i.e., schedules it to be executed after
     * {@code delay}, or cancels its execution if the method is called with the same key within the
     * {@code delay} again.
     */
    public void debounce(final String key, final Runnable runnable, final long delay, final TimeUnit unit) {
        final Future<?> prev = delayedMap.put(key, scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    if (null != runCount) {
                        ++runCount;
                        Logger.info(Debouncer.class, () -> "Debouncer has run : " + runCount + " times");
                    }
                    Logger.info(Debouncer.class, () -> "Debouncing : " + key + " after " + delay + " " + unit);

                    runnable.run();
                } finally {
                    delayedMap.remove(key);
                }
            }
        }, delay, unit));
        if (prev != null) {
            prev.cancel(true);
        }
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }
}
