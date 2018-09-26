package com.dotmarketing.db;

import com.dotmarketing.business.DotStateException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

import java.util.List;

/**
 * This thread is charge of running the cache flusher commit listener in async or sync mode.
 */
public class DotRunnableFlusherThread implements Runnable {

    private final List<Runnable> flushers;
    private final boolean        isSync;


    public DotRunnableFlusherThread(final List<Runnable> flushers) {
        this(flushers, false);
    }

    public DotRunnableFlusherThread(final List<Runnable> flushers, final boolean isSync) {
        this.isSync         = isSync;
        this.flushers       = flushers;
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
}
