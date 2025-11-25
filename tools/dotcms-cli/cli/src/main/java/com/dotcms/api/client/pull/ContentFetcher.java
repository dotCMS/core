package com.dotcms.api.client.pull;

import java.util.List;
import java.util.Map;
import jakarta.ws.rs.NotFoundException;

/**
 * The ContentFetcher interface provides a contract for classes that can fetch content of type T.
 *
 * @param <T> The type of content to fetch.
 */
public interface ContentFetcher<T> {

    /**
     * Fetches a list of elements of type T.
     *
     * @return The fetched list of elements.
     */
    List<T> fetch(boolean failFast, Map<String, Object> customOptions);

    /**
     * Fetches a single element of type T by its key.
     *
     * @param key The key of the element to fetch.
     * @return The fetched element.
     * @throws NotFoundException If the element is not found.
     */
    T fetchByKey(String key, boolean failFast, Map<String, Object> customOptions) throws NotFoundException;

}
