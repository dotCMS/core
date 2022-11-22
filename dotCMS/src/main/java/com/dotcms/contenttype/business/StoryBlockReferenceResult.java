package com.dotcms.contenttype.business;

/**
 * Result of the refresh references
 * - refreshed is true if there were something to refresh
 * - value would be the original value (if not refresh) or the value with the refreshed contentlets if something has change
 */
public class StoryBlockReferenceResult {

    private final boolean refreshed;
    private final Object  value;

    public StoryBlockReferenceResult(final boolean refreshed, final Object value) {
        this.refreshed = refreshed;
        this.value = value;
    }

    public boolean isRefreshed() {
        return refreshed;
    }

    public Object getValue() {
        return value;
    }
}
