package com.dotcms.api.client.push.task;

import com.dotcms.api.client.push.MapperService;
import com.dotcms.api.client.push.PushHandler;
import com.dotcms.api.client.push.exception.PushException;
import com.dotcms.model.push.PushAnalysisResult;
import java.util.Map;
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

    private final PushAnalysisResult<T> result;

    private final PushHandler<T> pushHandler;

    private final MapperService mapperService;

    private final Map<String, Object> customOptions;

    final boolean allowRemove;

    private final Logger logger;

    public ProcessResultTask(final PushAnalysisResult<T> result, final boolean allowRemove,
            final Map<String, Object> customOptions, final PushHandler<T> pushHandler,
            final MapperService mapperService, final Logger logger) {

        this.result = result;
        this.allowRemove = allowRemove;
        this.customOptions = customOptions;
        this.pushHandler = pushHandler;
        this.mapperService = mapperService;
        this.logger = logger;
    }

    /**
     * This method is responsible for performing the push operation based on the given result. It
     * handles different actions such as adding, updating, removing, or no action required.
     *
     * @throws PushException if there is an error while performing the push operation
     */
    @Override
    protected void compute() {

        try {

            T localContent = null;
            if (result.localFile().isPresent()) {
                localContent = this.mapperService.map(
                        result.localFile().get(),
                        this.pushHandler.type()
                );
            }

            switch (result.action()) {
                case ADD:

                    logger.debug(String.format("Pushing file [%s] for [%s] operation",
                            result.localFile().get().getAbsolutePath(), result.action()));

                    if (result.localFile().isPresent()) {
                        this.pushHandler.add(result.localFile().get(), localContent, customOptions);
                    } else {
                        var message = "Local file is missing for add action";
                        logger.error(message);
                        throw new PushException(message);
                    }
                    break;
                case UPDATE:

                    logger.debug(String.format("Pushing file [%s] for [%s] operation",
                            result.localFile().get().getAbsolutePath(), result.action()));

                    if (result.localFile().isPresent() && result.serverContent().isPresent()) {
                        this.pushHandler.edit(result.localFile().get(), localContent,
                                result.serverContent().get(), customOptions);
                    } else {
                        String message = "Local file or server content is missing for update action";
                        logger.error(message);
                        throw new PushException(message);
                    }
                    break;
                case REMOVE:

                    if (this.allowRemove) {
                        logger.debug(
                                String.format("Pushing [%s] operation for [%s]",
                                        result.action(),
                                        this.pushHandler.contentSimpleDisplay(
                                                result.serverContent().get())
                                )
                        );

                        if (result.serverContent().isPresent()) {

                            logger.debug(
                                    String.format("Pushing [%s] operation for [%s]",
                                            result.action(),
                                            this.pushHandler.contentSimpleDisplay(
                                                    result.serverContent().get())
                                    )
                            );

                            this.pushHandler.remove(result.serverContent().get(), customOptions);
                        } else {
                            var message = "Server content is missing for remove action";
                            logger.error(message);
                            throw new PushException(message);
                        }
                    }
                    break;
                case NO_ACTION:

                    if (result.localFile().isPresent()) {
                        logger.debug(String.format("File [%s] requires no action",
                                result.localFile().get().getAbsolutePath()));
                    }

                    // Do nothing for now
                    break;
                default:
                    logger.error("Unknown action: " + result.action());
                    break;
            }
        } catch (Exception e) {

            var message = String.format(
                    "Error pushing content for operation [%s]",
                    result.action()
            );
            if (result.localFile().isPresent()) {
                message = String.format(
                        "Error pushing file [%s] for operation [%s] - [%s]",
                        result.localFile().get().getAbsolutePath(),
                        result.action(),
                        e.getMessage()
                );
            } else if (result.serverContent().isPresent()) {
                message = String.format(
                        "Error pushing [%s] for operation [%s] - [%s]",
                        this.pushHandler.contentSimpleDisplay(result.serverContent().get()),
                        result.action(), e.getMessage()
                );
            }

            logger.error(message, e);
            throw new PushException(message, e);
        }

    }
}

