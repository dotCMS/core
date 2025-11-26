package com.dotcms.api.client.pull;

import com.dotcms.api.client.MapperService;
import com.dotcms.api.client.pull.task.PullTask;
import com.dotcms.api.client.pull.task.PullTaskParams;
import com.dotcms.api.client.util.ErrorHandlingUtil;
import com.dotcms.cli.common.ConsoleProgressBar;
import com.dotcms.cli.common.InputOutputFormat;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.model.pull.PullOptions;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import jakarta.inject.Inject;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.logging.Logger;

/**
 * A generic pull handler that can be used to pull any type of content. When using this handler, the
 * PullService will pull automatically the fetched content using ObjectMappers with the default
 * implementation of the pull method.
 * <p>
 * Useful for pulling descriptors for elements and not special treatment is needed.
 *
 * @param <T>
 */
public abstract class GeneralPullHandler<T> extends PullHandler<T> {

    @Inject
    MapperService mapperService;

    @Inject
    Logger logger;

    @Inject
    ErrorHandlingUtil errorHandlerUtil;

    @Inject
    ManagedExecutor executor;

    /**
     * Returns a display name of a given T element. Used for logging purposes.
     */
    public abstract String displayName(T content);

    /**
     * Returns the file name for a given T elements used to save the content to a file.
     *
     * @param content the content to be saved to a file.
     */
    public abstract String fileName(T content);

    public int pull(List<T> contents,
            PullOptions pullOptions,
            OutputOptionMixin output) throws ExecutionException, InterruptedException {

        output.info(startPullingHeader(contents));

        // ConsoleProgressBar instance to handle the push progress bar
        ConsoleProgressBar progressBar = new ConsoleProgressBar(output);
        // Calculating the total number of steps
        progressBar.setTotalSteps(
                contents.size()
        );

        CompletableFuture<List<Exception>> pullFuture = executor.supplyAsync(
                () -> {

                    final var format = InputOutputFormat.valueOf(
                            pullOptions.outputFormat()
                                    .orElse(InputOutputFormat.defaultFormat().toString())
                    );

                    PullTask<T> task = new PullTask<>(
                            logger,
                            mapperService,
                            executor
                    );

                    task.setTaskParams(PullTaskParams.<T>builder().
                            destination(pullOptions.destination()).
                            contents(contents).
                            format(format).
                            failFast(pullOptions.failFast()).
                            pullHandler(this).
                            output(output).
                            progressBar(progressBar).build()
                    );

                    return task.compute().join();
                });
        progressBar.setFuture(pullFuture);

        CompletableFuture<Void> animationFuture = executor.runAsync(
                progressBar
        );

        // Waits for the completion of both the push process and console progress bar animation tasks.
        // This line blocks the current thread until both CompletableFuture instances
        // (pullFuture and animationFuture) have completed.
        CompletableFuture.allOf(pullFuture, animationFuture).join();

        var errors = pullFuture.get();
        return errorHandlerUtil.handlePullExceptions(errors, output);
    }

}
