package com.dotcms.api.client.push;

import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.model.push.PushOptions;
import java.io.File;
import java.util.Map;

/**
 * Represents a service for pushing content from a local file or folder to a remote destination.
 */
public interface PushService {

    /**
     * Pushes the local file or folder to a remote location using the provided options and
     * handlers.
     *
     * @param localFileOrFolder the local file or folder to be pushed
     * @param options           the options for the push operation
     * @param output            the output options for the push operation
     * @param provider          the content fetcher for retrieving the content to be pushed
     * @param comparator        the comparator for comparing the content to be pushed with the
     *                          remote content
     * @param pushHandler       the push handler for handling the push operations
     */
    <T> void push(File localFileOrFolder,
            PushOptions options,
            OutputOptionMixin output,
            ContentFetcher<T> provider,
            ContentComparator<T> comparator,
            PushHandler<T> pushHandler);

    /**
     * Pushes the local file or folder to a remote location using the provided options and
     * handlers.
     *
     * @param localFileOrFolder the local file or folder to be pushed
     * @param options           the options for the push operation
     * @param output            the output options for the push operation
     * @param provider          the content fetcher for retrieving the content to be pushed
     * @param comparator        the comparator for comparing the content to be pushed with the
     *                          remote content
     * @param pushHandler       the push handler for handling the push operations
     * @param customOptions     the custom options for the push operation that may be used by each
     *                          push handler implementation
     */
    <T> void push(File localFileOrFolder,
            PushOptions options,
            OutputOptionMixin output,
            ContentFetcher<T> provider,
            ContentComparator<T> comparator,
            PushHandler<T> pushHandler,
            Map<String, Object> customOptions);

}
