package com.dotcms.api.client.pull;

import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.model.pull.PullOptions;
import java.util.List;

/**
 * A custom pull handler that can be used to pull any type of content. Use this handler when special
 * treatment is needed in order to pull the fetched content.
 *
 * @param <T>
 */
public interface CustomPullHandler<T> extends PullHandler<T> {

    /**
     * This method pulls the content using the given options and returns a list of exceptions, if
     * any. Useful when special treatment is needed in order to pull the fetched content.
     *
     * @param content       The content to pull.
     * @param pullOptions   The options for pulling the content.
     * @param customOptions The custom options for pulling the content.
     * @param output        The output options for pulling the content.
     * @return A list of exceptions encountered during the pull operation.
     */
    List<Exception> pull(
            T content,
            final PullOptions pullOptions,
            final OutputOptionMixin output
    );

}
