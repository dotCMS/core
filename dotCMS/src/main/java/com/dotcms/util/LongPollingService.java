package com.dotcms.util;

import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.DotSubmitter;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.system.AppContext;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;

import java.io.Serializable;
import java.util.Date;

/**
 * Encapsulates the logic to do a long polling.
 * Long polling technique basically will wait for N seconds, and them will execute a delegate.
 * The long polling will perform the task based on a thread pool executor.
 * @author jsanca
 */
public class LongPollingService implements Serializable {

    public static final String LONG_POLLING_THREAD_POOL_SUBMITTER_NAME = "longpolling";
    public static final String SYSTEM_LONGPOLLING_DEFAULTMILLIS = "system.longpolling.defaultmillis";

    private final long milliSecondToWait;
    private final Delegate<AppContext> delegate;
    private final DotSubmitter dotSubmitter;

    /**
     * Constructor needs a time to wait (if it is null, will use a default one) and a delegate to perform the task.
     * @param milliSecondToWait Long optional time to wait for the long polling
     * @param delegate {@link Delegate} specific task to execute for the long polling.
     */
    public LongPollingService(final Long milliSecondToWait, final Delegate<AppContext> delegate) {

        this(
              (null != milliSecondToWait)?
                    milliSecondToWait:
                    // by default is 15 seconds.
                    Config.getLongProperty(SYSTEM_LONGPOLLING_DEFAULTMILLIS, 15000),
              delegate,
              DotConcurrentFactory.getInstance()
        );
    } // LongPollingService.

    @VisibleForTesting
    protected LongPollingService(final long milliSecondToWait, final Delegate<AppContext> delegate, final DotConcurrentFactory dotConcurrentFactory) {

        this.milliSecondToWait = milliSecondToWait;
        this.delegate = delegate;
        this.dotSubmitter = dotConcurrentFactory.getSubmitter(LONG_POLLING_THREAD_POOL_SUBMITTER_NAME);
    } // LongPollingService.

    /**
     * Executes a long polling approach in a blocking sync block
     * @param appContext {@link AppContext}
     */
    public void execute (final AppContext appContext) {

        try {

            Logger.debug(this, "Long Polling, sleeping for at: " + new Date() + " for " + this.milliSecondToWait + " milliseconds.");
            Thread.sleep(this.milliSecondToWait);
            Logger.debug(this, "Long Polling, calling the delegate at: " + new Date());
            this.delegate.execute(appContext);
        } catch (InterruptedException e) {

            Logger.debug(this, e.getMessage(), e);
            Thread.currentThread().interrupt();
        }
    } // execute.

    /**
     * Executes a long polling approach in a non-blocking async block.
     * It uses a thread-polling with a default configuration, you can override it b
     * @param appContext
     */
    public void executeAsync (final AppContext appContext) {

        this.dotSubmitter.execute(() -> {

            this.execute(appContext);
        });
    } // executeAsync.
} // E:O:F:LongPollingService.
