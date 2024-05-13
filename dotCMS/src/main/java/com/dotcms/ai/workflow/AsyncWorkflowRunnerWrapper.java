package com.dotcms.ai.workflow;

import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.ai.util.OpenAIThreadPool;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.db.LocalTransaction;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import io.vavr.control.Try;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;


/**
 * The purpose of this class is to only have one Async runner working on a piece of content at a time. It does this by
 * using the identifier + language as a key, and checks if another job is running against that key.  If so, it
 * reschedules the job to try to run again  5 seconds in the future.  It will keep rescheduling for up to an hour at
 * which point the job expires.
 */
public class AsyncWorkflowRunnerWrapper implements Runnable {

    public static final int MAX_RESCHEDULES = 720;
    private static final Set<String> RUNNING_CONTENT = ConcurrentHashMap.newKeySet();

    private final AsyncWorkflowRunner asyncWorkflowRunner;
    private final String contentletKey;
    private int rescheduled;

    AsyncWorkflowRunnerWrapper(AsyncWorkflowRunner runner) {
        this(runner, 0);

    }

    AsyncWorkflowRunnerWrapper(final AsyncWorkflowRunner runner, final int runNumber) {
        this.asyncWorkflowRunner = runner;
        this.contentletKey = Try
                .of(() -> asyncWorkflowRunner.getIdentifier() + asyncWorkflowRunner.getLanguage())
                .getOrElseThrow(DotRuntimeException::new);
        if (UtilMethods.isEmpty(asyncWorkflowRunner.getIdentifier())) {
            throw new DotRuntimeException(
                    "Content must be saved before it can be run async - no identifier");
        }
        this.rescheduled = runNumber;
    }

    public String getSessionId() {
        return Try
                .of(() -> HttpServletRequestThreadLocal.INSTANCE.getRequest().getSession().getId())
                .getOrElse("unknown");
    }

    @Override
    public void run() {
        boolean runningNow = false;
        try {
            runningNow = shouldRunNow();
            if (!runningNow) {
                return;
            }

            LocalTransaction.wrap(asyncWorkflowRunner::runInternal);
            HibernateUtil.commitTransaction();
        } catch (BadAIJsonFormatException e) {
            // OpenAI generally outputs valid json but sometimes it goes crazy and spits out a mess.
            // If this happens, we try our request again.
            Logger.warn(this.getClass(), "got bad json, rescheduling request");
            Logger.warn(this.getClass(), "- error was :" + e.getMessage());
            OpenAIThreadPool.schedule(
                    new AsyncWorkflowRunnerWrapper(asyncWorkflowRunner, ++rescheduled), 5,
                    TimeUnit.SECONDS);
        } catch (Throwable e) { //NOSONAR - catches throwable because a throwable destroys the whole thread pool.
            Logger.warn(this.getClass(), e.getMessage());
            if (ConfigService.INSTANCE.config().getConfigBoolean(AppKeys.DEBUG_LOGGING)) {
                Logger.warn(this.getClass(), e.getMessage(), e);
            }
        } finally {
            if (runningNow) {
                RUNNING_CONTENT.remove(contentletKey);
            }
            HibernateUtil.closeSessionSilently();
        }
    }

    private synchronized boolean shouldRunNow() {
        if (!RUNNING_CONTENT.contains(contentletKey)) {
            RUNNING_CONTENT.add(contentletKey);
            return true;
        }
        runLater();
        return false;
    }

    private void runLater() {
        if (rescheduled > MAX_RESCHEDULES) {
            RUNNING_CONTENT.remove(contentletKey);
            Logger.warn(this.getClass(),
                    "Unable to schedule " + this.getClass().getSimpleName() + " for content id:"
                            + contentletKey);
            return;
        }
        OpenAIThreadPool.schedule(
                new AsyncWorkflowRunnerWrapper(asyncWorkflowRunner, ++rescheduled), 5,
                TimeUnit.SECONDS);
    }

}
