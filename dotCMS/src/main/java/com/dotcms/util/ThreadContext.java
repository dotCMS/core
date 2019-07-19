package com.dotcms.util;

/**
 * Encapsulates Thread Local context information
 * @author jsanca
 */
public class ThreadContext {

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
}
