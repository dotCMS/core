package com.dotcms.api.client.push;

import java.util.List;

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
    List<T> fetch();
    
}
