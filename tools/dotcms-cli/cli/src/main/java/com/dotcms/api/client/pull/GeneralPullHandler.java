package com.dotcms.api.client.pull;

import com.dotcms.api.client.MapperService;
import com.dotcms.api.client.pull.task.PullTask;
import com.dotcms.api.client.pull.task.PullTaskParams;
import com.dotcms.cli.common.ConsoleProgressBar;
import com.dotcms.cli.common.InputOutputFormat;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.model.pull.PullOptions;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import javax.inject.Inject;
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

    public boolean pull(List<T> contents,
            PullOptions pullOptions,
            OutputOptionMixin output) throws ExecutionException, InterruptedException {

        var failed = false;

        output.info(startPullingHeader(contents));

        // ConsoleProgressBar instance to handle the push progress bar
        ConsoleProgressBar progressBar = new ConsoleProgressBar(output);
        // Calculating the total number of steps
        progressBar.setTotalSteps(
                contents.size()
        );

        CompletableFuture<List<Exception>> pullFuture = CompletableFuture.supplyAsync(
                () -> {

                    final var format = InputOutputFormat.valueOf(
                            pullOptions.outputFormat()
                                    .orElse(InputOutputFormat.defaultFormat().toString())
                    );

                    var forkJoinPool = ForkJoinPool.commonPool();
                    var task = new PullTask<>(PullTaskParams.<T>builder().
                            destination(pullOptions.destination()).
                            contents(contents).
                            format(format).
                            failFast(pullOptions.failFast()).
                            pullHandler(this).
                            mapperService(mapperService).
                            output(output).
                            logger(logger).
                            progressBar(progressBar).build()
                    );
                    return forkJoinPool.invoke(task);
                });
        progressBar.setFuture(pullFuture);

        CompletableFuture<Void> animationFuture = CompletableFuture.runAsync(
                progressBar
        );

        // Waits for the completion of both the push process and console progress bar animation tasks.
        // This line blocks the current thread until both CompletableFuture instances
        // (pullFuture and animationFuture) have completed.
        CompletableFuture.allOf(pullFuture, animationFuture).join();

        var errors = pullFuture.get();

        printErrors(errors, output);
        if (!errors.isEmpty()) {
            failed = true;
        }

        return failed;
    }

}
