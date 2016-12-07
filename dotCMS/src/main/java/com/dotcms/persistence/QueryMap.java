package com.dotcms.persistence;

import java.io.Serializable;
import java.util.Map;

/**
 * Returns a map of queries, implement this class if you want to return a map of queries using
 * just a class.
 *
 * The class needs to follow a convention to be loaded.
 * @author jsanca
 */
public interface QueryMap extends Serializable {

    /**
     * Get the Query Map
     * @return Map
     */
    Map<String, String> getQueryMap ();
} // E:O:F:QueryMap.
