package com.dotcms.api.client.pull;

import com.dotcms.api.client.MapperService;
import com.dotcms.api.client.pull.exception.PullException;
import com.dotcms.api.client.pull.task.PullTask;
import com.dotcms.api.client.pull.task.PullTaskParams;
import com.dotcms.cli.common.ConsoleLoadingAnimation;
import com.dotcms.cli.common.ConsoleProgressBar;
import com.dotcms.cli.common.InputOutputFormat;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.model.pull.PullOptions;
import io.quarkus.arc.DefaultBean;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import org.jboss.logging.Logger;

@DefaultBean
@Dependent
public class PullServiceImpl implements PullService {

    @Inject
    MapperService mapperService;

    @Inject
    Logger logger;

    /**
     * {@inheritDoc}
     */
    @ActivateRequestContext
    @Override
    public <T> void pull(File destination, PullOptions options, OutputOptionMixin output,
            ContentFetcher<T> provider, PullHandler<T> pullHandler) {

        pull(destination, options, output, provider, pullHandler, new HashMap<>());
    }

    /**
     * {@inheritDoc}
     */
    @ActivateRequestContext
    @Override
    public <T> void pull(File destination, PullOptions options, OutputOptionMixin output,
            ContentFetcher<T> provider, PullHandler<T> pullHandler,
            Map<String, Object> customOptions) {

        // ---
        // Fetching the contents
        var contents = fetch(options, output, provider, pullHandler, customOptions);

        var outputBuilder = new StringBuilder();

        outputBuilder.append("\r\n").
                append(" ──────\n");

        if (!contents.isEmpty()) {

            outputBuilder.append(String.format("@|bold [%d]|@ %s to pull",
                    contents.size(),
                    pullHandler.title()
            ));

            output.info(outputBuilder.toString());

            if (options.isShortOutput()) {

                // ---
                // Just print the short format
                pullToConsole(contents, output, pullHandler);
            } else {

                // ---
                // Storing the contents to disk
                pullToDisk(destination, contents, options, output, pullHandler);

                output.info(String.format("%n%n Output has been written to [%s].",
                        destination.getAbsolutePath()));
            }

        } else {
            outputBuilder.append(String.format(" No %s to pull", pullHandler.title()));
            output.info(outputBuilder.toString());
        }

    }

    /**
     * This method fetches content using the specified options, output, provider, pullHandler, and
     * customOptions. It can fetch a specific content identified by contentKey or all contents.
     *
     * @param options       The pull options.
     * @param output        The output option mixin.
     * @param provider      The content fetcher.
     * @param pullHandler   The pull handler.
     * @param customOptions The custom options.
     * @param <T>           The type of content to fetch.
     * @return The list of fetched contents.
     * @throws PullException If an error occurs while fetching the contents.
     */
    private <T> List<T> fetch(
            final PullOptions options,
            final OutputOptionMixin output,
            final ContentFetcher<T> provider,
            final PullHandler<T> pullHandler,
            final Map<String, Object> customOptions) {

        CompletableFuture<List<T>>
                fetcherServiceFuture = CompletableFuture.supplyAsync(
                () -> {

                    // Looking for a specific content
                    if (options.contentKey().isPresent()) {

                        logger.debug(String.format(
                                "Fetching %s by key [%s].",
                                pullHandler.title(),
                                options.contentKey().get()
                        ));

                        var foundContent = provider.fetchByKey(options.contentKey().get(),
                                customOptions);
                        return List.of(foundContent);
                    }

                    // Fetching all contents
                    logger.debug(String.format("Fetching all %s.", pullHandler.title()));
                    return provider.fetch(customOptions);
                });

        // ConsoleLoadingAnimation instance to handle the waiting "animation"
        ConsoleLoadingAnimation consoleLoadingAnimation = new ConsoleLoadingAnimation(
                output,
                fetcherServiceFuture
        );

        CompletableFuture<Void> animationFuture = CompletableFuture.runAsync(
                consoleLoadingAnimation
        );

        final List<T> contents;

        try {
            // Waits for the completion of both the fetch of the contents and console loading animation tasks.
            // This line blocks the current thread until both CompletableFuture instances
            // (fetcherServiceFuture and animationFuture) have completed.
            CompletableFuture.allOf(fetcherServiceFuture, animationFuture).join();
            contents = fetcherServiceFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            var errorMessage = String.format(
                    "Error occurred while fetching [%s]: [%s].",
                    pullHandler.title(), e.getMessage());
            logger.error(errorMessage, e);
            throw new PullException(errorMessage, e);
        }

        return contents;
    }

    /**
     * This method pulls the given contents to the console, using the specified output and
     * pullHandler.
     *
     * @param contents    The contents to be pulled to the console.
     * @param output      The output option mixin.
     * @param pullHandler The pull handler.
     * @param <T>         The type of the contents.
     */
    private <T> void pullToConsole(List<T> contents,
            final OutputOptionMixin output,
            final PullHandler<T> pullHandler) {

        for (var content : contents) {
            final String shortFormat = pullHandler.shortFormat(content);
            output.info(shortFormat);
        }

    }

    /**
     * This method pulls the contents to disk using the specified destination, contents, options,
     * output, and pullHandler. It handles the pull progress and updates the progress bar
     * accordingly.
     *
     * @param destination The destination file to save the pulled contents.
     * @param contents    The list of contents to pull.
     * @param options     The pull options.
     * @param output      The output option mixin.
     * @param pullHandler The pull handler for handling each content.
     * @param <T>         The type of content to pull.
     * @throws PullException If an error occurs while pulling the contents.
     */
    private <T> void pullToDisk(File destination,
            List<T> contents,
            final PullOptions options,
            final OutputOptionMixin output,
            final PullHandler<T> pullHandler) {

        // ConsoleProgressBar instance to handle the push progress bar
        ConsoleProgressBar progressBar = new ConsoleProgressBar(output);
        // Calculating the total number of steps
        progressBar.setTotalSteps(
                contents.size()
        );

        CompletableFuture<Void> pullFuture = CompletableFuture.supplyAsync(
                () -> {

                    final var format = InputOutputFormat.valueOf(options.outputFormat());

                    var forkJoinPool = ForkJoinPool.commonPool();
                    var task = new PullTask<>(PullTaskParams.<T>builder().
                            destination(destination).
                            contents(contents).
                            format(format).
                            pullHandler(pullHandler).
                            mapperService(mapperService).
                            output(output).
                            logger(logger).
                            progressBar(progressBar).build()
                    );
                    forkJoinPool.invoke(task);
                    return null;
                });
        progressBar.setFuture(pullFuture);

        CompletableFuture<Void> animationFuture = CompletableFuture.runAsync(
                progressBar
        );

        try {

            // Waits for the completion of both the push process and console progress bar animation tasks.
            // This line blocks the current thread until both CompletableFuture instances
            // (pullFuture and animationFuture) have completed.
            CompletableFuture.allOf(pullFuture, animationFuture).join();

            pullFuture.get();

        } catch (InterruptedException | ExecutionException e) {

            var errorMessage = String.format("Error occurred while pulling contents: [%s].",
                    e.getMessage());
            logger.error(errorMessage, e);
            throw new PullException(errorMessage, e);
        }
    }

}
