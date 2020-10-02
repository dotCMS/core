package com.dotcms.concurrent;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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
     * Put the runnable on a delay queue, eventually when the delay is done
     * @param task
     * @param delay
     * @param unit
     */
    void delay(final Runnable task, final long delay, final TimeUnit unit);

    /**
     * Submit a {@link Runnable}, returning the {@link Future}
     * It does a delay inside the runnable before starts the task
     * @param command {@link Runnable}
     * @param delay   {@link Long} unit for the {@link TimeUnit}
     * @param unit    {@link TimeUnit}
     * @return Future
     */
    Future<?> submit(Runnable command, long delay, TimeUnit unit);


    /**
     * Submit a {@link Callable}, returning the {@link Future}
     * @param callable {@link Callable}
     * @param <T>
     * @return Future
     */
    <T> Future<T> submit(Callable<T> callable);

    /**
     * Submit a {@link Runnable}, returning the {@link Future}
     * It does a delay inside the task before starts the task
     * @param callable {@link Runnable}
     * @param delay   {@link Long} unit for the {@link TimeUnit}
     * @param unit    {@link TimeUnit}
     * @return Future
     */
    <T> Future<T> submit(Callable<T> callable, long delay, TimeUnit unit);


    /**
     * Return the active count threads
     * @return Integer active count threads
     */
    public int getActiveCount();

    /**
     * Gets the pool size configured
     * @return int
     */
    public  int getPoolSize();

    /**
     * Gets the Max pool size configured
     * @return int
     */
    public  int getMaxPoolSize();

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

    /**
     * Checks if the thread pool is terminated terminating or shutdown
     * @return
     */
    public boolean isAborting();


} // E:O:F:DotExecutor.
