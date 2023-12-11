package com.dotcms.api.client.push;

import static com.dotcms.cli.command.files.TreePrinter.COLOR_DELETED;
import static com.dotcms.cli.command.files.TreePrinter.COLOR_MODIFIED;
import static com.dotcms.cli.command.files.TreePrinter.COLOR_NEW;

import com.dotcms.api.client.MapperService;
import com.dotcms.api.client.push.exception.PushException;
import com.dotcms.api.client.push.task.PushTask;
import com.dotcms.cli.common.ConsoleLoadingAnimation;
import com.dotcms.cli.common.ConsoleProgressBar;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.model.push.PushAnalysisResult;
import com.dotcms.model.push.PushOptions;
import io.quarkus.arc.DefaultBean;
import java.io.File;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import org.apache.commons.lang3.tuple.Pair;
import org.jboss.logging.Logger;

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
    FormatStatus formatStatus;

    @Inject
    Logger logger;

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
                pushAnalysisServiceFuture = CompletableFuture.supplyAsync(
                () -> {
                    // Analyzing what push operations need to be performed
                    return pushAnalysisService.analyze(
                            localFileOrFolder,
                            options.allowRemove(),
                            provider,
                            comparator
                    );
                });

        // ConsoleLoadingAnimation instance to handle the waiting "animation"
        ConsoleLoadingAnimation consoleLoadingAnimation = new ConsoleLoadingAnimation(
                output,
                pushAnalysisServiceFuture
        );

        CompletableFuture<Void> animationFuture = CompletableFuture.runAsync(
                consoleLoadingAnimation
        );

        final List<PushAnalysisResult<T>> analysisResults;

        try {
            // Waits for the completion of both the push analysis service and console loading animation tasks.
            // This line blocks the current thread until both CompletableFuture instances
            // (pushAnalysisServiceFuture and animationFuture) have completed.
            CompletableFuture.allOf(pushAnalysisServiceFuture, animationFuture).join();
            analysisResults = pushAnalysisServiceFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            var errorMessage = String.format(
                    "Error occurred while analysing push data for [%s]: [%s].",
                    localFileOrFolder.getAbsolutePath(), e.getMessage());
            logger.error(errorMessage, e);
            throw new PushException(errorMessage, e);
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

        var retryAttempts = 0;
        var failed = false;

        do {

            if (retryAttempts > 0) {
                output.info(String.format("%n↺ Retrying push process [%d of %d]...", retryAttempts,
                        options.maxRetryAttempts()));
            }

            // ConsoleProgressBar instance to handle the push progress bar
            ConsoleProgressBar progressBar = new ConsoleProgressBar(output);
            // Calculating the total number of steps
            progressBar.setTotalSteps(
                    summary.total
            );

            CompletableFuture<List<Exception>> pushFuture = CompletableFuture.supplyAsync(
                    () -> {
                        var forkJoinPool = ForkJoinPool.commonPool();
                        var task = new PushTask<>(
                                analysisResults,
                                options.allowRemove(),
                                customOptions,
                                options.failFast(),
                                pushHandler,
                                mapperService,
                                logger,
                                progressBar
                        );
                        return forkJoinPool.invoke(task);
                    });
            progressBar.setFuture(pushFuture);

            CompletableFuture<Void> animationFuture = CompletableFuture.runAsync(
                    progressBar
            );

            try {

                // Waits for the completion of both the push process and console progress bar animation tasks.
                // This line blocks the current thread until both CompletableFuture instances
                // (pushFuture and animationFuture) have completed.
                CompletableFuture.allOf(pushFuture, animationFuture).join();

                var errors = pushFuture.get();
                if (!errors.isEmpty()) {

                    failed = true;
                    output.info(String.format(
                            "%n%nFound [@|bold,red %s|@] errors during the push process:",
                            errors.size()));
                    long count = errors.stream().filter(PushException.class::isInstance).count();
                    int c = 0;
                    for (final var error : errors) {
                        c++;
                        output.handleCommandException(error,
                                String.format("%s %n", error.getMessage()), c == count);
                    }
                }

            } catch (InterruptedException | ExecutionException e) {

                var errorMessage = String.format("Error occurred while pushing contents: [%s].",
                        e.getMessage());
                logger.error(errorMessage, e);
                throw new PushException(errorMessage, e);
            } catch (Exception e) {// Fail fast

                failed = true;
                if (retryAttempts + 1 <= options.maxRetryAttempts()) {
                    output.info("\n\nFound errors during the push process:");
                    output.error(e.getMessage());
                } else {
                    throw e;
                }
            }
        } while (failed && retryAttempts++ < options.maxRetryAttempts());
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
