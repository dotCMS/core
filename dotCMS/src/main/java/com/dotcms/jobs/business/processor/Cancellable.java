package com.dotcms.jobs.business.processor;

import com.dotcms.jobs.business.error.JobCancellationException;
import com.dotcms.jobs.business.job.Job;

/**
 * The Cancellable interface represents a contract for objects that can be canceled,
 * typically long-running operations or jobs.
 * <p>
 * Implementations of this interface should provide a mechanism to interrupt or
 * stop their execution in a controlled manner when the cancel method is invoked.
 * <p>
 * It's important to note that implementing this interface indicates that the object
 * supports cancellation. There is no separate method to check if cancellation is possible;
 * the presence of this interface implies that it is.
 */
public interface Cancellable {

    /**
     * Attempts to cancel the execution of this object.
     * <p>
     * The exact behavior of this method depends on the specific implementation,
     * but it should generally attempt to stop the ongoing operation as quickly
     * and safely as possible. This might involve interrupting threads, closing
     * resources, or setting flags to stop loops.
     * <p>
     * Implementations should ensure that this method can be called safely from
     * another thread while the operation is in progress.
     * <p>
     * After this method is called, the object should make its best effort to
     * terminate, but there's no guarantee about when the termination will occur.
     *
     * @throws JobCancellationException if there is an error during the cancellation process.
     *         This could occur if the job is in a state where it cannot be canceled,
     *         or if there's an unexpected error while attempting to cancel.
     */
    void cancel(Job job) throws JobCancellationException;

}
