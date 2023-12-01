package com.dotcms.api.client.pull;

import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.model.pull.PullOptions;

public interface PullService {

    /**
     * Pulls remote contents of type T to the specified destination using the provided options and
     * handlers.
     *
     * @param options     the options for the pull operation
     * @param output      the output options for the pull operation
     * @param provider    the content fetcher for retrieving the remote contents
     * @param pullHandler the pull handler for handling pull operations
     * @param <T>         the type of the remote contents
     */
    <T> void pull(PullOptions options,
            OutputOptionMixin output,
            ContentFetcher<T> provider,
            PullHandler<T> pullHandler);

}
