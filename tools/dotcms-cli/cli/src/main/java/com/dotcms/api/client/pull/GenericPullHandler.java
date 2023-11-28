package com.dotcms.api.client.pull;

/**
 * A generic pull handler that can be used to pull any type of content. When using this handler, the
 * PullService will pull automatically the fetched content using ObjectMappers. Useful for pulling
 * descriptors for elements and not special treatment is needed.
 *
 * @param <T>
 */
public interface GenericPullHandler<T> extends PullHandler<T> {

    /**
     * Returns a display name of a given T element. Used for logging purposes.
     */
    String displayName(T content);

    /**
     * Returns the file name for a given T elements used to save the content to a file.
     *
     * @param content the content to be saved to a file.
     */
    String fileName(T content);

}
