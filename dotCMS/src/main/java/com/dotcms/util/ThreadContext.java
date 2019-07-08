package com.dotcms.util;

/**
 * Encapsulates Thread Local context information
 * @author jsanca
 */
public class ThreadContext {

    // by default the api will reindex
    private boolean reindex = true;

    public boolean isReindex() {
        return reindex;
    }

    public void setReindex(boolean reindex) {
        this.reindex = reindex;
    }
}
