package com.dotcms.api.client.push.task;

import com.dotcms.api.client.push.exception.PushException;
import com.dotcms.model.push.PushAnalysisResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RecursiveTask;
import org.jboss.logging.Logger;

/**
 * Represents a task for pushing analysis results using a specified push handler. This class extends
 * the `RecursiveTask` class from the `java.util.concurrent` package.
 *
 * @param <T> the type of analysis result
 */
public class PushTask<T> extends RecursiveTask<List<Exception>> {

    private final PushTaskParams<T> params;

    private final Logger logger;

    private static final int THRESHOLD = 10;

    public PushTask(final PushTaskParams<T> params) {
        this.params = params;
        this.logger = params.logger();
    }

    /**
     * Computes the analysis results and returns a list of exceptions.
     *
     * @return a list of exceptions encountered during the computation
     */
    @Override
    protected List<Exception> compute() {

        var errors = new ArrayList<Exception>();

        if (this.params.results().size() <= THRESHOLD) {

            // If the list is small enough, process sequentially
            for (var result : this.params.results()) {

                try {
                    processAnalysisResult(result);
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

        } else {

            // If the list is large, split it into two smaller tasks
            int mid = this.params.results().size() / 2;
            var paramsTask1 = this.params.withResults(
                    this.params.results().subList(0, mid)
            );
            var paramsTask2 = this.params.withResults(
                    this.params.results().subList(mid, this.params.results().size())
            );

            var task1 = new PushTask<>(paramsTask1);
            var task2 = new PushTask<>(paramsTask2);

            // Start the first subtask in a new thread
            task1.fork();

            // Start and wait for the second subtask to finish
            errors.addAll(task2.compute());

            // Wait for the first subtask to finish
            errors.addAll(task1.join());
        }

        return errors;
    }

    /**
     * This method is responsible for performing the push operation based on the given
     * {@link PushAnalysisResult}. It handles different actions such as adding, updating, removing,
     * or no action required.
     *
     * @throws PushException if there is an error while performing the push operation
     */
    private void processAnalysisResult(final PushAnalysisResult<T> result) {

        final T localContent = result.localContent().orElse(null);
        final T serverContent = result.serverContent().orElse(null);
        final var localFile = result.localFile().orElse(null);

        try {

            switch (result.action()) {
                case ADD:
                    processAddAction(localContent, localFile);
                    break;
                case UPDATE:
                    processUpdateAction(localContent, serverContent, localFile);
                    break;
                case REMOVE:
                    processRemoveAction(serverContent);
                    break;
                case NO_ACTION:
                    processNoAction(localFile);
                    break;
                default:
                    logger.error("Unknown action: " + result.action());
                    break;
            }
        } catch (Exception e) {

            if (e instanceof PushException) {
                throw e;
            }

            var message = String.format(
                    "Error pushing content for operation [%s]",
                    result.action()
            );
            if (localFile != null) {
                message = String.format(
                        "Error pushing file [%s] for operation [%s] - [%s]",
                        localFile.getAbsolutePath(),
                        result.action(),
                        e.getMessage()
                );
            } else if (result.serverContent().isPresent()) {
                message = String.format(
                        "Error pushing [%s] for operation [%s] - [%s]",
                        this.params.pushHandler().contentSimpleDisplay(serverContent),
                        result.action(), e.getMessage()
                );
            }

            logger.error(message, e);
            throw new PushException(message, e);
        }
    }

    /**
     * Processes the ADD action by pushing the local content to the server.
     *
     * @param localContent the local content to be added
     * @param localFile    the local file representing the content to be added
     * @throws PushException if there is an error while performing the add operation
     */
    private void processAddAction(T localContent, File localFile) {

        if (localFile != null) {

            logger.debug(String.format("Pushing file [%s] for [ADD] operation",
                    localFile.getAbsolutePath()));

            final var addedContent = this.params.pushHandler()
                    .add(localFile, localContent, this.params.customOptions());

            if (!this.params.disableAutoUpdate()) {
                updateFile(addedContent, localFile);
            }

        } else {
            String message = "Local file is missing for add action";
            logger.error(message);
            throw new PushException(message);
        }
    }

    /**
     * Processes the update action by pushing the local content to the server.
     *
     * @param localContent  the local content to be updated
     * @param serverContent the existing server content
     * @param localFile     the local file representing the content to be updated
     * @throws PushException if there is an error while processing the update action
     */
    private void processUpdateAction(T localContent, T serverContent, File localFile) {

        if (localFile != null && serverContent != null) {

            logger.debug(String.format("Pushing file [%s] for [UPDATE] operation",
                    localFile.getAbsolutePath()));

            final var updatedContent = this.params.pushHandler()
                    .edit(localFile, localContent, serverContent, this.params.customOptions());

            if (!this.params.disableAutoUpdate()) {
                updateFile(updatedContent, localFile);
            }

        } else {
            String message = "Local file or server content is missing for update action";
            logger.error(message);
            throw new PushException(message);
        }
    }

    /**
     * Processes the remove action by pushing the server content to be removed.
     *
     * @param serverContent the server content to be removed
     * @throws PushException if there is an error while performing the remove operation
     */
    private void processRemoveAction(T serverContent) {

        // process remove action...
        if (this.params.allowRemove()) {

            if (serverContent != null) {

                logger.debug(
                        String.format("Pushing [REMOVE] operation for [%s]",
                                this.params.pushHandler().contentSimpleDisplay(
                                        serverContent
                                )
                        )
                );

                this.params.pushHandler().remove(serverContent, this.params.customOptions());
            } else {
                String message = "Server content is missing for remove action";
                logger.error(message);
                throw new PushException(message);
            }
        }
    }

    /**
     * Processes the "no action" case for a given local file.
     *
     * @param localFile the local file for which no action is required
     */
    private void processNoAction(File localFile) {

        // Process the no action case...
        if (localFile != null) {
            logger.debug(String.format("File [%s] requires no action",
                    localFile.getAbsolutePath()));
        }
    }

    /**
     * Updates the content of a file.
     *
     * @param content   the new content to be written to the file
     * @param localFile the file to be updated
     * @throws PushException if there is an error while updating the file
     */
    private void updateFile(final T content, final File localFile) {

        try {

            logger.debug(String.format("Pulling latest version of [%s]",
                    localFile.getAbsolutePath())
            );

            // Updating the file content

            ObjectMapper objectMapper = this.params.mapperService().objectMapper(localFile);
            final String asString = objectMapper.writeValueAsString(content);

            final Path path = Path.of(localFile.getAbsolutePath());
            Files.writeString(path, asString);

        } catch (Exception e) {

            var message = String.format(
                    "Error pulling latest version of [%s] - [%s]",
                    localFile.getAbsolutePath(),
                    e.getMessage()
            );

            logger.error(message, e);
            throw new PushException(message, e);
        }
    }

}
