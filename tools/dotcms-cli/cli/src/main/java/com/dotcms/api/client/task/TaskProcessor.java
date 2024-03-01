package com.dotcms.api.client.task;

import java.util.concurrent.CompletableFuture;

public abstract class TaskProcessor<P, R> {

    protected static final int THRESHOLD = 10;

    /**
     * Sets the parameters for the task. This method provides a way to inject necessary
     * configuration after the instance of the task has been created by the container.
     *
     * @param param The parameter for the task.
     */
    public abstract void setTaskParams(final P param);

    /**
     * Computes a task.
     *
     * @return The result of the computation.
     */
    public abstract R compute();

    /**
     * Creates a new CompletableFuture and completes it exceptionally with the given exception.
     *
     * @param e The exception to be completed exceptionally.
     * @return A CompletableFuture that is completed exceptionally with the given exception.
     * @param <T> The type of value that would have been returned by the CompletableFuture if it
     *           not had been completed exceptionally.
     */
    protected <T> CompletableFuture<T> futureWithCompleteExceptionally(Exception e) {
        CompletableFuture<T> futureError = new CompletableFuture<>();
        futureError.completeExceptionally(e);
        return futureError;
    }

}
