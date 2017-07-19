package com.dotcms.contenttype.model.type;

/**
 * This interface implements the default fallback if a content doesn't have
 * a value in the current language to search with the default language
 * Created by oswaldogallango on 2017-06-19.
 */
public interface MultilinguableFallback {

    /**
     * Get if the the search should be done using
     * the default value
     * @return true if should do a search with the defafult value, false if not
     */
    default boolean fallback(){
        return true;
    }
}
