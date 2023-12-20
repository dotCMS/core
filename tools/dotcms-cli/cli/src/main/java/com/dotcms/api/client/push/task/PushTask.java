package com.dotcms.api.client.push.task;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RecursiveTask;

/**
 * Represents a task for pushing analysis results using a specified push handler. This class extends
 * the `RecursiveTask` class from the `java.util.concurrent` package.
 *
 * @param <T> the type of analysis result
 */
public class PushTask<T> extends RecursiveTask<List<Exception>> {

    private final PushTaskParams<T> params;

    public PushTask(final PushTaskParams<T> params) {
        this.params = params;
    }

    /**
     * Computes the analysis results and returns a list of exceptions.
     *
     * @return a list of exceptions encountered during the computation
     */
    @Override
    protected List<Exception> compute() {

        var errors = new ArrayList<Exception>();

        List<RecursiveAction> tasks = new ArrayList<>();

        for (var result : this.params.results()) {

            var task = new ProcessResultTask<>(
                    ProcessResultTaskParams.<T>builder().
                            result(result).
                            allowRemove(this.params.allowRemove()).
                            disableAutoUpdate(this.params.disableAutoUpdate()).
                            customOptions(this.params.customOptions()).
                            pushHandler(this.params.pushHandler()).
                            mapperService(this.params.mapperService()).
                            logger(this.params.logger()).build()
            );
            tasks.add(task);
            task.fork();
        }

        // Join all tasks
        for (RecursiveAction task : tasks) {
            try {
                task.join();
            } catch (Exception e) {
                if (this.params.failFast()) {
                    throw e;
                } else {
                    errors.add(e);
                }
            } finally {
                this.params.progressBar().incrementStep();
            }
        }

        return errors;
    }

}
