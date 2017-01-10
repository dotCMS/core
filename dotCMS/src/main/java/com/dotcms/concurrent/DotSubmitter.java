package com.dotcms.concurrent;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

/**
 * Encapsulate a Submitter for {@link Runnable} and {@link Callable}, in addition to the usual {@link Executor} functionality.
 * @author jsanca
 */
public interface DotSubmitter extends Executor, Serializable {


    /**
     * Submit a {@link Runnable}, returning the {@link Future}
     * @param command {@link Runnable}
     * @return Future
     */
    Future<?> submit(Runnable command);


    /**
     * Submit a {@link Callable}, returning the {@link Future}
     * @param callable {@link Callable}
     * @param <T>
     * @return Future
     */
    <T> Future<T> submit(Callable<T> callable);


    /**
     * Return the active count threads
     * @return Integer active count threads
     */
    public int getActiveCount();

    /**
     * Initiates an orderly shutdown in which previously submitted
     * tasks are executed, but no new tasks will be accepted.
     * Invocation has no additional effect if already shut down.
     */
    public void shutdown();

    /**
     * Attempts to stop all actively executing tasks, halts the
     * processing of waiting tasks, and returns a list of the tasks
     * that were awaiting execution. These tasks are drained (removed)
     * from the task queue upon return from this method.
     *
     * @return List of Runnables
     */
    public List<Runnable> shutdownNow();

} // E:O:F:DotExecutor.
