package com.dotcms.api.client.pull;

import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.model.pull.PullOptions;
import java.io.File;
import java.util.Map;

public interface PullService {

    /**
     * Pulls remote contents of type T to the specified destination using the provided options and
     * handlers.
     *
     * @param destination the destination to pull the remote contents to
     * @param options     the options for the pull operation
     * @param output      the output options for the pull operation
     * @param provider    the content fetcher for retrieving the remote contents
     * @param pullHandler the pull handler for handling pull operations
     * @param <T>         the type of the remote contents
     */
    <T> void pull(File destination,
            PullOptions options,
            OutputOptionMixin output,
            ContentFetcher<T> provider,
            PullHandler<T> pullHandler);

    /**
     * Pulls remote contents of type T to the specified destination using the provided options and
     * handlers.
     *
     * @param destination   the destination to pull the remote contents to
     * @param options       the options for the pull operation
     * @param output        the output options for the pull operation
     * @param provider      the content fetcher for retrieving the remote contents
     * @param pullHandler   the pull handler for handling pull operations
     * @param <T>           the type of the remote contents
     * @param customOptions the custom options for the pull operation
     */
    <T> void pull(File destination,
            PullOptions options,
            OutputOptionMixin output,
            ContentFetcher<T> provider,
            PullHandler<T> pullHandler,
            Map<String, Object> customOptions);

}
