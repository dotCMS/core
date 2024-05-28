package com.dotcms.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulates Thread Local context information
 * @author jsanca
 */
public class ThreadContext {

    // by default the api will reindex, set to false if do not want to reindex in an api call.
    private boolean reindex = true;

    // when the reindex happens later, the api call can set this to true in order to tell at the end of the thread process to do reindex including dependencies
    private boolean includeDependencies = false;

    private String tag;

    // helps keep the count if a different events in the current thread
    private Map<String, Integer> counterMap = new HashMap<>();

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

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public Integer getCounter(final String key) {
        return counterMap.getOrDefault(key, 0);
    }

    public void increaseCounter(final String key) {

        this.counterMap.put(key, this.getCounter(key) + 1);
    }

}
