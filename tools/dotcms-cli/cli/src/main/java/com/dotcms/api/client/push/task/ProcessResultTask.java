package com.dotcms.api.client.push.task;

import com.dotcms.api.client.push.exception.PushException;
import com.dotcms.model.push.PushAnalysisResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.RecursiveAction;
import org.jboss.logging.Logger;

/**
 * Represents a task for processing a {@link PushAnalysisResult}.
 * <p>
 * This task extends {@link RecursiveAction} and is used to perform the necessary actions based on
 * the result of the push analysis.
 *
 * @param <T> the type of content being pushed
 */
public class ProcessResultTask<T> extends RecursiveAction {

    private final ProcessResultTaskParams<T> params;

    private final Logger logger;

    public ProcessResultTask(final ProcessResultTaskParams<T> params) {
        this.params = params;
        this.logger = params.logger();
    }

    /**
     * This method is responsible for performing the push operation based on the given this.params.result(). It
     * handles different actions such as adding, updating, removing, or no action required.
     *
     * @throws PushException if there is an error while performing the push operation
     */
    @Override
    protected void compute() {

        try {

            T localContent = null;
            if (this.params.result().localFile().isPresent()) {
                localContent = this.params.mapperService().map(
                        this.params.result().localFile().get(),
                        this.params.pushHandler().type()
                );
            }

            switch (this.params.result().action()) {
                case ADD:

                    logger.debug(String.format("Pushing file [%s] for [%s] operation",
                            this.params.result().localFile().get().getAbsolutePath(),
                            this.params.result().action()));

                    if (this.params.result().localFile().isPresent()) {

                        final var addedContent = this.params.pushHandler()
                                .add(this.params.result().localFile().get(), localContent,
                                        this.params.customOptions());

                        if (!this.params.disableAutoUpdate()) {
                            updateFile(addedContent, this.params.result().localFile().get());
                        }

                    } else {
                        var message = "Local file is missing for add action";
                        logger.error(message);
                        throw new PushException(message);
                    }
                    break;
                case UPDATE:

                    logger.debug(String.format("Pushing file [%s] for [%s] operation",
                            this.params.result().localFile().get().getAbsolutePath(),
                            this.params.result().action()));

                    if (this.params.result().localFile().isPresent() &&
                            this.params.result().serverContent().isPresent()) {

                        final var updatedContent = this.params.pushHandler()
                                .edit(this.params.result().localFile().get(), localContent,
                                        this.params.result().serverContent().get(),
                                        this.params.customOptions());

                        if (!this.params.disableAutoUpdate()) {
                            updateFile(updatedContent, this.params.result().localFile().get());
                        }

                    } else {
                        String message = "Local file or server content is missing for update action";
                        logger.error(message);
                        throw new PushException(message);
                    }
                    break;
                case REMOVE:

                    if (this.params.allowRemove()) {

                        if (this.params.result().serverContent().isPresent()) {

                            logger.debug(
                                    String.format("Pushing [%s] operation for [%s]",
                                            this.params.result().action(),
                                            this.params.pushHandler().contentSimpleDisplay(
                                                    this.params.result().serverContent().get())
                                    )
                            );

                            this.params.pushHandler()
                                    .remove(this.params.result().serverContent().get(),
                                            this.params.customOptions());
                        } else {
                            var message = "Server content is missing for remove action";
                            logger.error(message);
                            throw new PushException(message);
                        }
                    }
                    break;
                case NO_ACTION:

                    if (this.params.result().localFile().isPresent()) {
                        logger.debug(String.format("File [%s] requires no action",
                                this.params.result().localFile().get().getAbsolutePath()));
                    }

                    // Do nothing for now
                    break;
                default:
                    logger.error("Unknown action: " + this.params.result().action());
                    break;
            }
        } catch (Exception e) {

            var message = String.format(
                    "Error pushing content for operation [%s]",
                    this.params.result().action()
            );
            if (this.params.result().localFile().isPresent()) {
                message = String.format(
                        "Error pushing file [%s] for operation [%s] - [%s]",
                        this.params.result().localFile().get().getAbsolutePath(),
                        this.params.result().action(),
                        e.getMessage()
                );
            } else if (this.params.result().serverContent().isPresent()) {
                message = String.format(
                        "Error pushing [%s] for operation [%s] - [%s]",
                        this.params.pushHandler().contentSimpleDisplay(
                                this.params.result().serverContent().get()),
                        this.params.result().action(), e.getMessage()
                );
            }

            logger.error(message, e);
            throw new PushException(message, e);
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

