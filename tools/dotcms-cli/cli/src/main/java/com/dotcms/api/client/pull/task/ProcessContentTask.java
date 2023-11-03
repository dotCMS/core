package com.dotcms.api.client.pull.task;

import com.dotcms.api.client.pull.exception.PullException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.RecursiveAction;
import org.jboss.logging.Logger;

/**
 * A ForkJoinTask that handles the pulling of a single content item.
 *
 * @param <T>
 */
public class ProcessContentTask<T> extends RecursiveAction {

    private final ProcessContentTaskParams<T> params;

    private final Logger logger;

    public ProcessContentTask(final ProcessContentTaskParams<T> params) {

        this.params = params;
        this.logger = params.logger();
    }

    /**
     * Handles the pulling of a single content item by saving it to a file.
     */
    @Override
    protected void compute() {

        try {

            logger.debug(String.format("Pulling content [%s] to [%s]",
                    this.params.pullHandler().displayName(this.params.content()),
                    this.params.destination().getAbsolutePath())
            );

            // Save the content to a file

            ObjectMapper objectMapper = this.params.mapperService()
                    .objectMapper(this.params.format());
            final String asString = objectMapper.writeValueAsString(this.params.content());
            if (this.params.output().isVerbose()) {
                this.params.output().info(asString);
            }

            final String fileName = String.format(
                    "%s.%s",
                    this.params.pullHandler().fileName(this.params.content()),
                    this.params.format().getExtension()
            );
            final Path path = Path.of(this.params.destination().getAbsolutePath(), fileName);
            Files.writeString(path, asString);

        } catch (Exception e) {

            var message = String.format(
                    "Error pulling content [%s] to [%s] - [%s]",
                    this.params.pullHandler().displayName(this.params.content()),
                    this.params.destination().getAbsolutePath(),
                    e.getMessage()
            );

            logger.error(message, e);
            throw new PullException(message, e);
        }

    }

}