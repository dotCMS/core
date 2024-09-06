package com.dotcms.api.client.push;

import static com.dotcms.cli.command.files.TreePrinter.COLOR_DELETED;
import static com.dotcms.cli.command.files.TreePrinter.COLOR_MODIFIED;
import static com.dotcms.cli.command.files.TreePrinter.COLOR_NEW;

import com.dotcms.api.client.MapperService;
import com.dotcms.api.client.push.exception.PushException;
import com.dotcms.api.client.push.task.PushTask;
import com.dotcms.api.client.push.task.PushTaskParams;
import com.dotcms.api.client.util.ErrorHandlingUtil;
import com.dotcms.cli.common.ConsoleLoadingAnimation;
import com.dotcms.cli.common.ConsoleProgressBar;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.cli.exception.ForceSilentExitException;
import com.dotcms.model.push.PushAnalysisResult;
import com.dotcms.model.push.PushOptions;
import io.quarkus.arc.DefaultBean;
import java.io.File;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.logging.Logger;
import picocli.CommandLine.ExitCode;

/**
 * Implementation of the PushService interface for performing push operations.
 */
@DefaultBean
@Dependent
public class PushServiceImpl implements PushService {

    @Inject
    PushAnalysisService pushAnalysisService;

    @Inject
    MapperService mapperService;

    @Inject
    ErrorHandlingUtil errorHandlerUtil;

    @Inject
    FormatStatus formatStatus;

    @Inject
    Logger logger;

    @Inject
    ManagedExecutor executor;

    /**
     * Analyzes and pushes the changes to a remote repository.
     *
     * @param localFileOrFolder The local file or folder to push.
     * @param options           The push options.
     * @param output            The output option mixin.
     * @param provider          The content fetcher provider.
     * @param comparator        The content comparator.
     * @param pushHandler       The push handler.
     */
    @ActivateRequestContext
    @Override
    public <T> void push(final File localFileOrFolder, final PushOptions options,
            final OutputOptionMixin output, final ContentFetcher<T> provider,
            final ContentComparator<T> comparator, final PushHandler<T> pushHandler) {

        push(localFileOrFolder, options, output, provider, comparator, pushHandler,
                new HashMap<>());
    }

    /**
     * Analyzes and pushes the changes to a remote repository.
     *
     * @param localFileOrFolder The local file or folder to push.
     * @param options           The push options.
     * @param output            The output option mixin.
     * @param provider          The content fetcher provider.
     * @param comparator        The content comparator.
     * @param pushHandler       The push handler.
     * @param customOptions     the custom options for the push operation that may be used by each
     *                          push handler implementation
     */
    @ActivateRequestContext
    @Override
    public <T> void push(final File localFileOrFolder, final PushOptions options,
            final OutputOptionMixin output, final ContentFetcher<T> provider,
            final ContentComparator<T> comparator, final PushHandler<T> pushHandler,
            final Map<String, Object> customOptions) {

        // ---
        // Analyzing what push operations need to be performed
        var results = analyze(localFileOrFolder, options, output, provider, comparator);
        var analysisResults = results.getLeft();
        var summary = results.getRight();

        var outputBuilder = new StringBuilder();

        outputBuilder.append("\r\n").
                append(" ──────\n");

        if (summary.total > 0 && summary.total != summary.noActions) {

            // Sorting analysisResults by action
            analysisResults.sort(Comparator.comparing(PushAnalysisResult::action));

            outputBuilder.append(String.format(" Push Data: " +
                            "@|bold [%d]|@ %s to push: " +
                            "(@|bold," + COLOR_NEW + " %d|@ New " +
                            "- @|bold," + COLOR_MODIFIED + " %d|@ Modified)",
                    (summary.total - summary.noActions),
                    pushHandler.title(),
                    summary.adds,
                    summary.updates));

            if (options.allowRemove()) {
                outputBuilder.append(
                        String.format(" - @|bold," + COLOR_DELETED + " %d|@ to Delete%n%n",
                                summary.removes));
            } else {
                outputBuilder.append(String.format("%n%n"));
            }

            if (options.dryRun()) {
                outputBuilder.append(formatStatus.format(analysisResults, pushHandler, true));
            }

            output.info(outputBuilder.toString());

            // ---
            // Pushing the changes
            if (!options.dryRun()) {
                processPush(analysisResults, summary, options, output, pushHandler, customOptions);
            }

        } else {
            outputBuilder.append(
                    String.format(" No changes in %s to push%n%n", pushHandler.title()));
            output.info(outputBuilder.toString());
        }

    }

    /**
     * Analyzes the push data for a local file or folder.
     *
     * @param localFileOrFolder the local file or folder to analyze
     * @param options           the push options
     * @param output            the output option mixin to use for displaying progress
     * @param provider          the content fetcher used to fetch content for analysis
     * @param comparator        the content comparator used to compare content for analysis
     * @return a pair containing the list of push analysis results and the push analysis summary
     * @throws PushException if an error occurs during the analysis
     */
    private <T> Pair<List<PushAnalysisResult<T>>, PushAnalysisSummary<T>> analyze(
            final File localFileOrFolder,
            final PushOptions options,
            final OutputOptionMixin output,
            final ContentFetcher<T> provider,
            final ContentComparator<T> comparator) {

        CompletableFuture<List<PushAnalysisResult<T>>>
                pushAnalysisServiceFuture = executor.supplyAsync(
                () ->
                        // Analyzing what push operations need to be performed
                        pushAnalysisService.analyze(
                                localFileOrFolder,
                                options.allowRemove(),
                                provider,
                                comparator
                        )
        );

        // ConsoleLoadingAnimation instance to handle the waiting "animation"
        ConsoleLoadingAnimation consoleLoadingAnimation = new ConsoleLoadingAnimation(
                output,
                pushAnalysisServiceFuture
        );

        CompletableFuture<Void> animationFuture = executor.runAsync(
                consoleLoadingAnimation
        );

        final List<PushAnalysisResult<T>> analysisResults;

        try {
            // Waits for the completion of both the push analysis service and console loading animation tasks.
            // This line blocks the current thread until both CompletableFuture instances
            // (pushAnalysisServiceFuture and animationFuture) have completed.
            CompletableFuture.allOf(pushAnalysisServiceFuture, animationFuture).join();
            analysisResults = pushAnalysisServiceFuture.get();
        } catch (InterruptedException e) {
            var errorMessage = String.format(
                    "Error occurred while analysing push data for [%s]: [%s].",
                    localFileOrFolder.getAbsolutePath(), e.getMessage());
            logger.error(errorMessage, e);
            Thread.currentThread().interrupt();
            throw new PushException(errorMessage, e);
        } catch (ExecutionException | CompletionException e) {
            var cause = e.getCause();
            throw errorHandlerUtil.mapPushException(cause);
        }

        var summary = new PushAnalysisSummary<>(analysisResults);
        return Pair.of(analysisResults, summary);
    }

    /**
     * Processes the push operation based on the given analysis results, summary, options, output,
     * and push handler. The method handles retrying the push process, displaying progress bar, and
     * catching any exceptions.
     *
     * @param analysisResults the list of push analysis results
     * @param summary         the push analysis summary
     * @param options         the push options
     * @param output          the output option mixin
     * @param pushHandler     the push handler for handling the push operations
     * @param customOptions   the custom options for the push operation that may be used by each
     *                        push handler implementation
     */
    private <T> void processPush(List<PushAnalysisResult<T>> analysisResults,
            PushAnalysisSummary<T> summary,
            final PushOptions options,
            final OutputOptionMixin output,
            final PushHandler<T> pushHandler,
            final Map<String, Object> customOptions) {

        var maxRetryAttempts = options.maxRetryAttempts();
        var failed = false;
        var retryAttempts = 0;
        var errorCode = ExitCode.OK;

        do {

            if (retryAttempts > 0) {
                output.info(
                        String.format(
                                "%n↺ Retrying push process [%d of %d]...",
                                retryAttempts,
                                options.maxRetryAttempts()
                        )
                );
            }

            var e = processPushAttempt(
                    analysisResults,
                    summary,
                    options,
                    output,
                    pushHandler,
                    customOptions,
                    retryAttempts
            );
            errorCode = Math.max(errorCode, e);
            if (errorCode > ExitCode.OK) {
                failed = true;
            }

        } while (failed && retryAttempts++ < maxRetryAttempts);
        if (errorCode > ExitCode.OK) {
            //All exceptions are already handled and logged, so we can just throw a generic exception to force exit
            throw new ForceSilentExitException(errorCode);
        }
    }

    /**
     * Processes the push attempt based on the given analysis results, summary, options, output,
     * push handler, custom options, and retry attempts. The method handles the push process, the
     * console progress bar animation, and exception handling.
     *
     * @param <T>             The type parameter.
     * @param analysisResults The list of push analysis results.
     * @param summary         The push analysis summary.
     * @param options         The push options.
     * @param output          The output option mixin.
     * @param pushHandler     The push handler for handling the push operations.
     * @param customOptions   The custom options for the push operation that may be used by each
     *                        push handler implementation.
     * @param retryAttempts   The number of retry attempts for the push operation.
     * @return The exit code for the push operation.
     * @throws PushException If an error occurs during the push operation.
     */
    private <T> int processPushAttempt(List<PushAnalysisResult<T>> analysisResults,
            PushAnalysisSummary<T> summary,
            final PushOptions options,
            final OutputOptionMixin output,
            final PushHandler<T> pushHandler,
            final Map<String, Object> customOptions,
            int retryAttempts) {

        // ConsoleProgressBar instance to handle the push progress bar
        ConsoleProgressBar progressBar = new ConsoleProgressBar(output);
        // Calculating the total number of steps
        progressBar.setTotalSteps(
                summary.total
        );

        CompletableFuture<List<Exception>> pushFuture = executor.supplyAsync(
                () -> {

                    PushTask<T> task = new PushTask<>(
                            logger, mapperService, executor
                    );

                    task.setTaskParams(PushTaskParams.<T>builder().
                            results(analysisResults).
                            allowRemove(options.allowRemove()).
                            disableAutoUpdate(options.disableAutoUpdate()).
                            failFast(options.failFast()).
                            customOptions(customOptions).
                            pushHandler(pushHandler).
                            progressBar(progressBar).
                            build()
                    );

                    return task.compute().join();
                });
        progressBar.setFuture(pushFuture);

        CompletableFuture<Void> animationFuture = executor.runAsync(
                progressBar
        );

        try {

            // Waits for the completion of both the push process and console progress bar animation tasks.
            // This line blocks the current thread until both CompletableFuture instances
            // (pushFuture and animationFuture) have completed.
            CompletableFuture.allOf(pushFuture, animationFuture).join();

            var errors = pushFuture.get();
            return errorHandlerUtil.handlePushExceptions(errors, output);

        } catch (InterruptedException e) {

            var errorMessage = String.format(
                    "Error occurred while pushing contents: [%s].", e.getMessage()
            );
            logger.error(errorMessage, e);
            Thread.currentThread().interrupt();
            throw new PushException(errorMessage, e);
        } catch (ExecutionException | CompletionException e) {// Fail fast

            var cause = e.getCause();
            var toThrow = errorHandlerUtil.handlePushFailFastException(
                    retryAttempts, options.maxRetryAttempts(), output, cause
            );
            if (toThrow.isPresent()) {
                throw toThrow.get();
            }

            return ExitCode.SOFTWARE;
        }
    }

    /**
     * The PushAnalysisSummary class represents a summary of push analysis results. It counts the
     * number of adds, updates, removes, no actions, and total actions.
     */
    static class PushAnalysisSummary<T> {

        private int adds;
        private int updates;
        private int removes;
        private int noActions;
        private int total;

        public PushAnalysisSummary(List<PushAnalysisResult<T>> results) {
            if (results != null) {
                for (PushAnalysisResult<T> result : results) {
                    switch (result.action()) {
                        case ADD:
                            adds++;
                            break;
                        case UPDATE:
                            updates++;
                            break;
                        case REMOVE:
                            removes++;
                            break;
                        case NO_ACTION:
                            noActions++;
                            break;
                    }
                }

                total = results.size();
            }
        }

        @Override
        public String toString() {
            return "PushAnalysisSummary{" +
                    "adds=" + adds +
                    ", updates=" + updates +
                    ", removes=" + removes +
                    ", noActions=" + noActions +
                    ", total=" + total +
                    '}';
        }
    }
}
