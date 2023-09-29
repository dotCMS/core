package com.dotcms.api.client.push.task;

import com.dotcms.api.client.push.MapperService;
import com.dotcms.api.client.push.PushHandler;
import com.dotcms.cli.common.ConsoleProgressBar;
import com.dotcms.model.push.PushAnalysisResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RecursiveTask;
import org.jboss.logging.Logger;

/**
 * Represents a task for pushing analysis results using a specified push handler. This class extends
 * the `RecursiveTask` class from the `java.util.concurrent` package.
 *
 * @param <T> the type of analysis result
 */
public class PushTask<T> extends RecursiveTask<List<Exception>> {

    private final List<PushAnalysisResult<T>> analysisResults;

    private final PushHandler<T> pushHandler;

    private final boolean allowRemove;

    private final boolean failFast;

    private final Map<String, Object> customOptions;

    private final MapperService mapperService;

    private final ConsoleProgressBar progressBar;

    private Logger logger;

    public PushTask(
            final List<PushAnalysisResult<T>> analysisResults,
            final boolean allowRemove,
            final Map<String, Object> customOptions,
            final boolean failFast,
            final PushHandler<T> pushHandler,
            final MapperService mapperService,
            final Logger logger,
            final ConsoleProgressBar progressBar) {

        this.analysisResults = analysisResults;
        this.allowRemove = allowRemove;
        this.customOptions = customOptions;
        this.failFast = failFast;
        this.pushHandler = pushHandler;
        this.mapperService = mapperService;
        this.logger = logger;
        this.progressBar = progressBar;
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

        for (var result : analysisResults) {
            var task = new ProcessResultTask<>(
                    result,
                    allowRemove,
                    customOptions,
                    pushHandler,
                    mapperService,
                    logger
            );
            tasks.add(task);
            task.fork();
        }

        // Join all tasks
        for (RecursiveAction task : tasks) {
            try {
                task.join();
            } catch (Exception e) {
                if (failFast) {
                    throw e;
                } else {
                    errors.add(e);
                }
            } finally {
                progressBar.incrementStep();
            }
        }

        return errors;
    }

}
