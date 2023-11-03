package com.dotcms.api.client.pull.task;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RecursiveAction;
import org.jboss.logging.Logger;

/**
 * Represents a task that handles the pulled content.
 *
 * @param <T> the type of content being pulled and processed
 */
public class PullTask<T> extends RecursiveAction {

    private final PullTaskParams<T> params;

    private final Logger logger;

    public PullTask(final PullTaskParams<T> params) {
        this.params = params;
        this.logger = params.logger();
    }

    /**
     * Computes the contents to pull
     */
    @Override
    protected void compute() {

        List<RecursiveAction> tasks = new ArrayList<>();

        for (var content : this.params.contents()) {

            // Handling each content as a separate task in order to parallelize the process
            var task = new ProcessContentTask<>(ProcessContentTaskParams.<T>builder().
                    destination(this.params.destination()).
                    content(content).
                    format(this.params.format()).
                    pullHandler(this.params.pullHandler()).
                    mapperService(this.params.mapperService()).
                    output(this.params.output()).
                    logger(logger).build()
            );
            tasks.add(task);
            task.fork();
        }

        // Join all tasks
        for (RecursiveAction task : tasks) {
            try {
                task.join();
            } finally {
                this.params.progressBar().incrementStep();
            }
        }
    }

}
