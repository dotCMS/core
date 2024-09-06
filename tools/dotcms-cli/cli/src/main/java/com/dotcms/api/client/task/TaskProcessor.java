package com.dotcms.api.client.task;

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

}
