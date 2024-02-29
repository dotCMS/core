package com.dotcms.api.client.pull.task;

import com.dotcms.api.client.MapperService;
import com.dotcms.api.client.pull.exception.PullException;
import com.dotcms.api.client.task.TaskProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.function.Function;
import javax.enterprise.context.Dependent;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.logging.Logger;

/**
 * Represents a task that handles the pulled content.
 *
 * @param <T> the type of content being pulled and processed
 */
@Dependent
public class PullTask<T> extends TaskProcessor<PullTaskParams<T>,List<Exception>> {

    private PullTaskParams<T> params;

    private final Logger logger;

    private final MapperService mapperService;

    private final ManagedExecutor executor;

    public PullTask(final Logger logger, final MapperService mapperService,
            final ManagedExecutor executor) {
        this.logger = logger;
        this.mapperService = mapperService;
        this.executor = executor;
    }

    /**
     * Sets the parameters for the PullTask. This method provides a way to inject necessary
     * configuration after the instance of PullTask has been created by the container, which is a
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
    public void setTaskParams(final PullTaskParams<T> params) {
        this.params = params;
    }

    /**
     * Computes the contents to pull
     */
    @Override
    public List<Exception> compute() {

        CompletionService<List<Exception>> completionService =
                new ExecutorCompletionService<>(executor);

        var errors = new ArrayList<Exception>();

        if (this.params.contents().size() <= THRESHOLD) {

            // If the list is small enough, process sequentially
            for (var content : this.params.contents()) {
                try {
                    toDiskContent(content);
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

            // If the list is large, split it into smaller tasks
            int toProcessCount = splitTasks(completionService);

            // Wait for all tasks to complete and gather the results
            Function<List<Exception>, Void> processFunction = taskResult -> {
                errors.addAll(taskResult);
                return null;
            };
            processTasks(toProcessCount, completionService, processFunction);
        }

        return errors;
    }

    /**
     * Splits a list of T objects into separate tasks.
     *
     * @param completionService The CompletionService to submit tasks to.
     * @return The number of tasks to process.
     */
    private int splitTasks(final CompletionService<List<Exception>> completionService) {

        int mid = this.params.contents().size() / 2;
        var paramsTask1 = this.params.withContents(
                this.params.contents().subList(0, mid)
        );
        var paramsTask2 = this.params.withContents(
                this.params.contents().subList(mid, this.params.contents().size())
        );

        PullTask<T> task1 = new PullTask<>(logger, mapperService, executor);
        task1.setTaskParams(paramsTask1);

        PullTask<T> task2 = new PullTask<>(logger, mapperService, executor);
        task2.setTaskParams(paramsTask2);

        completionService.submit(task1::compute);
        completionService.submit(task2::compute);

        return 2;
    }

    /**
     * Processes the given content by saving it to a file.
     *
     * @param content The content to be processed.
     * @throws PullException If an error occurs while processing the content.
     */
    private void toDiskContent(final T content) {

        try {

            logger.debug(String.format("Pulling content [%s] to [%s]",
                    this.params.pullHandler().displayName(content),
                    this.params.destination().getAbsolutePath())
            );

            // Save the content to a file

            ObjectMapper objectMapper = mapperService.objectMapper(this.params.format());
            final String asString = objectMapper.writeValueAsString(content);
            if (this.params.output().isVerbose()) {
                this.params.output().info( String.format("%n%s", asString) );
            }

            final String fileName = String.format(
                    "%s.%s",
                    this.params.pullHandler().fileName(content),
                    this.params.format().getExtension()
            );
            final Path path = Path.of(this.params.destination().getAbsolutePath(), fileName);
            Files.writeString(path, asString);

        } catch (Exception e) {

            var message = String.format(
                    "Error pulling content [%s] to [%s] - [%s]",
                    this.params.pullHandler().displayName(content),
                    this.params.destination().getAbsolutePath(),
                    e.getMessage()
            );

            logger.error(message, e);
            throw new PullException(message, e);
        }
    }

}
