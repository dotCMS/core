package com.dotcms.api.client.pull;

import com.dotcms.api.client.MapperService;
import com.dotcms.api.client.files.traversal.exception.TraversalTaskException;
import com.dotcms.api.client.pull.exception.PullException;
import com.dotcms.api.client.pull.task.PullTask;
import com.dotcms.api.client.pull.task.PullTaskParams;
import com.dotcms.cli.common.ConsoleLoadingAnimation;
import com.dotcms.cli.common.ConsoleProgressBar;
import com.dotcms.cli.common.InputOutputFormat;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.model.pull.PullOptions;
import io.quarkus.arc.DefaultBean;
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

/**
 * PullServiceImpl is a class that implements the PullService interface. It provides methods for
 * pulling content from a provider and handling the pulled content.
 */
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
    public <T> void pull(PullOptions options, OutputOptionMixin output, ContentFetcher<T> provider,
            PullHandler<T> pullHandler) {

        pull(options, output, provider, pullHandler, new HashMap<>());
    }

    /**
     * {@inheritDoc}
     */
    @ActivateRequestContext
    @Override
    public <T> void pull(PullOptions pullOptions, OutputOptionMixin output,
            ContentFetcher<T> provider, PullHandler<T> pullHandler,
            Map<String, Object> customOptions) {

        // ---
        // Fetching the contents
        var contents = fetch(pullOptions, output, provider, pullHandler, customOptions);

        var outputBuilder = new StringBuilder();

        outputBuilder.append("\r\n").
                append(" ──────\n");

        if (!contents.isEmpty()) {

            if (pullOptions.isShortOutput()) {

                // ---
                // Just print the short format
                pullToConsole(contents, pullOptions, output, pullHandler, customOptions);
            } else {

                // ---
                // Storing the contents to disk
                pullToDisk(contents, pullOptions, output, pullHandler, customOptions);

                output.info(String.format("%n%n Output has been written to [%s].%n%n",
                        pullOptions.destination().getAbsolutePath()));
            }

        } else {
            outputBuilder.append(String.format("%n%n No %s to pull", pullHandler.title()));
            output.info(outputBuilder.toString());
        }

    }

    /**
     * This method fetches content using the specified options, output, provider, pullHandler, and
     * customOptions. It can fetch a specific content identified by contentKey or all contents.
     *
     * @param pullOptions   The pull options.
     * @param output        The output option mixin.
     * @param provider      The content fetcher.
     * @param pullHandler   The pull handler.
     * @param customOptions The custom options.
     * @param <T>           The type of content to fetch.
     * @return The list of fetched contents.
     * @throws PullException If an error occurs while fetching the contents.
     */
    private <T> List<T> fetch(
            final PullOptions pullOptions,
            final OutputOptionMixin output,
            final ContentFetcher<T> provider,
            final PullHandler<T> pullHandler,
            final Map<String, Object> customOptions) {

        CompletableFuture<List<T>>
                fetcherServiceFuture = CompletableFuture.supplyAsync(
                () -> {

                    // Looking for a specific content
                    if (pullOptions.contentKey().isPresent()) {

                        logger.debug(String.format(
                                "Fetching %s by key [%s].",
                                pullHandler.title(),
                                pullOptions.contentKey().get()
                        ));

                        var foundContent = provider.fetchByKey(pullOptions.contentKey().get(),
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
     * @param pullOptions The pull options.
     * @param output      The output option mixin.
     * @param pullHandler The pull handler.
     * @param <T>         The type of the contents.
     */
    private <T> void pullToConsole(List<T> contents,
            final PullOptions pullOptions,
            final OutputOptionMixin output,
            final PullHandler<T> pullHandler,
            final Map<String, Object> customOptions) {

        output.info(pullHandler.startPullingHeader(contents));

        for (var content : contents) {
            final String shortFormat = pullHandler.shortFormat(content, pullOptions, customOptions);
            output.info(shortFormat);
        }

        output.info(String.format("%n%n"));
    }

    /**
     * This method pulls the contents to disk using the specified contents, pullOptions, output,
     * pullHandler and customOptions. It handles the pull progress and updates the progress bar
     * accordingly.
     *
     * @param contents      The list of contents to pull.
     * @param pullOptions   The pull options.
     * @param output        The output option mixin.
     * @param pullHandler   The pull handler for handling each content.
     * @param customOptions The custom options.
     * @throws PullException If an error occurs while pulling the contents.
     */
    private <T> void pullToDisk(List<T> contents,
            final PullOptions pullOptions,
            final OutputOptionMixin output,
            final PullHandler<T> pullHandler,
            final Map<String, Object> customOptions) {

        var maxRetryAttempts = pullOptions.maxRetryAttempts();
        var failed = false;
        var retryAttempts = 0;

        do {

            if (retryAttempts > 0) {
                output.info(
                        String.format(
                                "%n↺ Retrying pull process [%d of %d]...",
                                retryAttempts,
                                maxRetryAttempts
                        )
                );
            }

            var foundErrors = performPull(
                    contents,
                    pullOptions,
                    output,
                    pullHandler,
                    customOptions,
                    retryAttempts,
                    maxRetryAttempts
            );
            if (foundErrors) {
                failed = true;
            }

        } while (failed && retryAttempts++ < maxRetryAttempts);
    }

    /**
     * This method performs the pull operation for the specified contents using the given
     * pullOptions, output, pullHandler, and customOptions. It handles the custom and generic pull
     * handlers to execute the pull process.
     *
     * @param contents         The list of contents to pull.
     * @param pullOptions      The pull options.
     * @param output           The output option mixin.
     * @param pullHandler      The pull handler for handling each content.
     * @param customOptions    The custom options.
     * @param retryAttempts    The number of retry attempts made during the pull process.
     * @param maxRetryAttempts The maximum number of retry attempts allowed.
     * @return True if errors were found during the pull process, false otherwise.
     * @throws PullException        If an error occurs while pulling the contents.
     * @throws InterruptedException If the thread is interrupted during the pull process.
     * @throws ExecutionException   If an error occurs during the execution of the pull process.
     */
    private <T> boolean performPull(List<T> contents, final PullOptions pullOptions,
            final OutputOptionMixin output, final PullHandler<T> pullHandler,
            final Map<String, Object> customOptions, int retryAttempts, int maxRetryAttempts) {

        try {

            boolean foundErrors;

            if (pullHandler instanceof CustomPullHandler) {

                foundErrors = customPull(
                        contents,
                        pullOptions,
                        output,
                        (CustomPullHandler<T>) pullHandler,
                        customOptions
                );

            } else {

                foundErrors = genericPull(
                        contents,
                        pullOptions,
                        output,
                        (GenericPullHandler<T>) pullHandler
                );

            }

            return foundErrors;

        } catch (InterruptedException | ExecutionException e) {

            var errorMessage = String.format("Error occurred while pulling contents: [%s].",
                    e.getMessage());
            logger.error(errorMessage, e);
            throw new PullException(errorMessage, e);
        } catch (Exception e) { // Fail fast

            if (retryAttempts + 1 <= maxRetryAttempts) {
                output.info("\n\nFound errors during the pull process:");
                output.error(e.getMessage());

                return true;
            } else {
                throw e;
            }
        }
    }

    /**
     * This method pulls the contents to disk delegating the pull process to the custom pull handler
     * implementation.
     *
     * @param contents      The list of contents to pull.
     * @param pullOptions   The pull options.
     * @param output        The output option mixin.
     * @param pullHandler   The pull handler for handling each pulled content.
     * @param customOptions The custom options.
     * @return true if there are errors during the pull process; false otherwise.
     */
    private <T> boolean customPull(List<T> contents,
            final PullOptions pullOptions,
            final OutputOptionMixin output,
            final CustomPullHandler<T> pullHandler,
            final Map<String, Object> customOptions) {

        var failed = false;

        output.info(pullHandler.startPullingHeader(contents));

        for (var content : contents) {

            var errors = pullHandler.pull(
                    content,
                    pullOptions,
                    customOptions,
                    output
            );

            printErrors(errors, output);
            if (!errors.isEmpty()) {
                failed = true;
            }
        }

        return failed;
    }

    /**
     * This method handles the pull process for the given contents using the specified pullOptions,
     * output, pullHandler and customOptions. It provides options for handling the pull process,
     * tracking progress, and handling errors.
     *
     * @param contents    The list of contents to pull.
     * @param pullOptions The pull options for configuring the pull process.
     * @param output      The output option mixin for providing output messages.
     * @param pullHandler The pull handler for handling each pulled content.
     * @return true if there are errors during the pull process; false otherwise.
     * @throws ExecutionException   If any exception occurred during the pull process.
     * @throws InterruptedException If the pull process was interrupted.
     */
    private <T> boolean genericPull(List<T> contents,
            final PullOptions pullOptions,
            final OutputOptionMixin output,
            final GenericPullHandler<T> pullHandler)
            throws ExecutionException, InterruptedException {

        var failed = false;

        output.info(pullHandler.startPullingHeader(contents));

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
                            pullHandler(pullHandler).
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

    /**
     * Prints the errors encountered during the pull process.
     *
     * @param errors The list of errors to be printed.
     * @param output The output option mixin for providing output messages.
     */
    private void printErrors(List<Exception> errors, OutputOptionMixin output) {

        if (!errors.isEmpty()) {

            output.info(
                    String.format(
                            "%n%nFound [@|bold,red %s|@] errors during the pull process:",
                            errors.size()
                    )
            );
            for (var error : errors) {
                if (error instanceof TraversalTaskException || error instanceof PullException) {
                    output.error(
                            String.format(
                                    "%s --- %s",
                                    error.getMessage(),
                                    error.getCause().getMessage()
                            )
                    );
                } else {
                    output.error(error.getMessage());
                }
            }
        }

    }

}
