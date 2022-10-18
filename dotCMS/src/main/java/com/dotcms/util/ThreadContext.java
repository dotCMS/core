package com.dotcms.util;

import org.apache.commons.collections.map.CompositeMap;

import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulates Thread Local context information
 * @author jsanca
 */
public class ThreadContext {

    private Map<String, Object> contextMap = null;
    // by default the api will reindex, set to false if do not want to reindex in an api call.
    private boolean reindex = true;

    // when the reindex happens later, the api call can set this to true in order to tell at the end of the thread process to do reindex including dependencies
    private boolean includeDependencies = false;

    public boolean isReindex() {
        return reindex;
    }

    public void setReindex(boolean reindex) {
        this.reindex = reindex;
    }

    public boolean isIncludeDependencies() {
        return includeDependencies;
    }

    public void setIncludeDependencies(boolean includeDependencies) {
        this.includeDependencies = includeDependencies;
    }

    /**
     * Get Context Map
     * @return Map
     */
    public Map<String, Object> getContextMap() {
        return hasContextMap()? contextMap: newContextMap();
    }

    /**
     * Returns true if has context map
     * @return Boolean
     */
    public boolean hasContextMap() {

        return null != contextMap;
    }

    private Map<String, Object> newContextMap() {
        contextMap = new HashMap<>();
        return contextMap;
    }

    /**
     * Merge a context Map
     * @param newMap
     */
    public void addContextMap(final Map<String, Object> newMap) {

        if (hasContextMap()) {

            if (this.contextMap instanceof CompositeMap) {
                CompositeMap.class.cast(this.contextMap).addComposited(newMap);
            } else {
                this.contextMap = new CompositeMap(this.contextMap, newMap);
            }
        } else {

            this.contextMap = newMap;
        }
    }
}
