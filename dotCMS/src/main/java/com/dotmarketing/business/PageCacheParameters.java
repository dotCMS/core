package com.dotmarketing.business;

import com.dotmarketing.util.Logger;

/**
 * Utility class used to keep the parameters used to identify a page cache.
 * 
 * @author Jose Castro
 * @version 1.0
 * @since 10-17-2014
 *
 */
public class PageCacheParameters {
    final private String[] params;

    /**
     * Creates an object with a series of page-specific parameters to try to uniquely identify a page
     * request.
     * 
     * @param userId - The ID of the current user.
     * @param language - The language ID.
     * @param urlMap
     * @param queryString - The current query String in the page URL.
     */
    public PageCacheParameters(final String... values) {
        this.params = values;
    }

    /**
     * Generates the page subkey that will be used in the page cache. This key will represent a specific
     * version of the page.
     * 
     * @return The subkey which is specific for a page.
     */
    public String getKey() {
        final String key = String.join("_", this.params);
        Logger.debug(this.getClass(), ()-> "page_cache_key:" + key);
        return key;
    }

}
