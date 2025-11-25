package com.dotcms.api.client.push.task;

import com.dotcms.api.client.MapperService;
import com.dotcms.api.client.push.exception.PushException;
import com.dotcms.api.client.task.TaskProcessor;
import com.dotcms.model.push.PushAnalysisResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import jakarta.enterprise.context.Dependent;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.logging.Logger;

/**
 * Represents a task for pushing analysis results using a specified push handler.
 *
 * @param <T> the type of analysis result
 */
@Dependent
public class PushTask<T> extends
        TaskProcessor<PushTaskParams<T>, CompletableFuture<List<Exception>>> {

    private PushTaskParams<T> params;

    private final Logger logger;

    private final ManagedExecutor executor;

    private final MapperService mapperService;

    public PushTask(final Logger logger,
            final MapperService mapperService, final ManagedExecutor executor) {
        this.logger = logger;
        this.executor = executor;
        this.mapperService = mapperService;
    }

    /**
     * Sets the parameters for the PushTask. This method provides a way to inject necessary
     * configuration after the instance of PushTask has been created by the container, which is a
     * common pattern when working with frameworks like Quarkus that manage object creation and
     * dependency injection in a specific manner.
     * <p>
     * This method is used as an alternative to constructor injection, which is not feasible due to
     * the limitations or constraints of the framework's dependency injection mechanism. It allows
     * for the explicit setting of traversal parameters after the object's instantiation, ensuring
     * that the executor is properly configured before use.
     *
     * @param params The parameters for the PullTask
     */
    @Override
    public void setTaskParams(final PushTaskParams<T> params) {
        this.params = params;
    }

    /**
     * Computes the analysis results and returns a list of exceptions.
     *
     * @return a list of exceptions encountered during the computation
     */
    @Override
    public CompletableFuture<List<Exception>> compute() {

        if (this.params.results().size() <= THRESHOLD) {

            return CompletableFuture.supplyAsync(() -> {

                List<Exception> errors = new ArrayList<>();

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
                return errors;
            }, executor).exceptionally(e -> {
                if (e.getCause() instanceof PushException) {
                    throw (PushException) e.getCause();
                } else {
                    throw new PushException(e.getCause().getMessage(), e.getCause());
                }
            });

        }

        // If the list is large, split it into smaller tasks
        return splitTasks();
    }

    /**
     * Splits a list of T objects into separate tasks.
     *
     * @return A CompletableFuture representing the combined results of the separate tasks.
     */
    private CompletableFuture<List<Exception>> splitTasks() {

        int mid = this.params.results().size() / 2;
        var paramsTask1 = this.params.withResults(
                this.params.results().subList(0, mid)
        );
        var paramsTask2 = this.params.withResults(
                this.params.results().subList(mid, this.params.results().size())
        );

        PushTask<T> task1 = new PushTask<>(logger, mapperService, executor);
        task1.setTaskParams(paramsTask1);
        var futureTask1 = task1.compute();

        PushTask<T> task2 = new PushTask<>(logger, mapperService, executor);
        task2.setTaskParams(paramsTask2);
        var futureTask2 = task2.compute();

        return futureTask1.thenCombine(futureTask2, (list1, list2) -> {
            var combinedList = new ArrayList<>(list1);
            combinedList.addAll(list2);
            return combinedList;
        });
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

            ObjectMapper objectMapper = mapperService.objectMapper(localFile);
            final String asString = objectMapper.writeValueAsString(content);

            final Path path = Path.of(localFile.getAbsolutePath());
            Files.writeString(path, asString);

            // Check if we need to rename the file
            String localFileName = localFile.getName();
            int lastDotPosition = localFileName.lastIndexOf('.');
            String localDileNameWithoutExtension = localFileName.substring(0, lastDotPosition);
            String localFileNameExtension = localFileName.substring(lastDotPosition + 1);

            final var expectedFileName = this.params.pushHandler().fileName(content);
            if (!localDileNameWithoutExtension.equals(expectedFileName)) {

                // Something changed in the content that requires a file rename

                final String renamedFileName = String.format(
                        "%s.%s", expectedFileName, localFileNameExtension
                );

                final var renamedFile = new File(localFile.getParent(), renamedFileName);
                Files.move(path, renamedFile.toPath());
            }

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
