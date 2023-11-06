package com.dotcms.api.client.pull.task;

import com.dotcms.api.client.pull.exception.PullException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.RecursiveAction;
import org.jboss.logging.Logger;

/**
 * Represents a task that handles the pulled content.
 *
 * @param <T> the type of content being pulled and processed
 */
public class PullTask<T> extends RecursiveAction {

    private final PullTaskParams<T> params;

    private final Logger logger;

    private static final int THRESHOLD = 10;

    public PullTask(final PullTaskParams<T> params) {
        this.params = params;
        this.logger = params.logger();
    }

    /**
     * Computes the contents to pull
     */
    @Override
    protected void compute() {

        if (this.params.contents().size() <= THRESHOLD) {

            // If the list is small enough, process sequentially
            for (var content : this.params.contents()) {
                toDiskContent(content);
            }

        } else {

            // If the list is large, split it into two smaller tasks
            int mid = this.params.contents().size() / 2;
            var paramsTask1 = this.params.withContents(
                    this.params.contents().subList(0, mid)
            );
            var paramsTask2 = this.params.withContents(
                    this.params.contents().subList(mid, this.params.contents().size())
            );

            var task1 = new PullTask<>(paramsTask1);
            var task2 = new PullTask<>(paramsTask2);

            // Start the first subtask in a new thread
            task1.fork();

            // Start and wait for the second subtask to finish
            task2.compute();

            // Wait for the first subtask to finish
            task1.join();
        }
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

            ObjectMapper objectMapper = this.params.mapperService()
                    .objectMapper(this.params.format());
            final String asString = objectMapper.writeValueAsString(content);
            if (this.params.output().isVerbose()) {
                this.params.output().info(asString);
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
