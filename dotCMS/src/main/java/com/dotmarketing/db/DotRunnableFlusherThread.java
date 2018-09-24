package com.dotmarketing.db;

import com.dotmarketing.business.DotStateException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

import java.util.List;
import java.util.stream.Collectors;

/**
 * This thread is charge of running the cache flusher commit listener in async or sync mode.
 */
public class DotRunnableFlusherThread implements Runnable {

    private final List<Runnable> flushers;
    private final boolean        isSync;


    public DotRunnableFlusherThread(final List<Runnable> allListeners) {
        this(allListeners, false);
    }

    public DotRunnableFlusherThread(final List<Runnable> allListeners, final boolean isSync) {
        this.isSync         = isSync;
        this.flushers       = getFlushers(allListeners);
    }

    private void runNetworkflowCacheFlushThread() {

        flushers.forEach(runner -> runner.run());
    }

    @Override
    public void run() {

        try {

            Logger.debug(this, ()-> "Running the Flushers thread: "
                    + Thread.currentThread().getName() + (this.isSync?" in Sync":"in Async")
                    + " Mode");

            if (UtilMethods.isSet(this.flushers)) {

                this.runNetworkflowCacheFlushThread();
            }
        } catch (Exception dde) {
            throw new DotStateException(dde);
        }
    }



    private List<Runnable> getFlushers(final List<Runnable> allListeners) {
        return allListeners.stream().filter(this::isFlushCacheRunnable).collect(Collectors.toList());
    }

    private boolean isFlushCacheRunnable(final Runnable listener) {

        return (
                listener instanceof FlushCacheRunnable ||
                        (listener instanceof HibernateUtil.DotOrderedRunnable
                                && HibernateUtil.DotOrderedRunnable.class.cast(listener).getRunnable() instanceof FlushCacheRunnable) ||
                        (listener instanceof HibernateUtil.DotSyncRunnable
                                && HibernateUtil.DotSyncRunnable.class.cast(listener).getRunnable() instanceof FlushCacheRunnable)
        );
    }
}
