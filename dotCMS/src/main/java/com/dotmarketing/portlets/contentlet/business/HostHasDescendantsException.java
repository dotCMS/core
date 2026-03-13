package com.dotmarketing.portlets.contentlet.business;

import com.dotmarketing.business.DotStateException;

/**
 * Exception thrown when an attempt is made to delete a Host that has descendant hosts or folders.
 * Deletion is blocked until all descendant hosts have been removed first.
 *
 * @author dotCMS
 * @since Mar 2026
 */
public class HostHasDescendantsException extends DotStateException {

    private static final long serialVersionUID = 1L;

    private final String siteId;
    private final long descendantHostCount;
    private final long descendantFolderCount;

    /**
     * Creates a new exception with the given site ID and descendant counts.
     *
     * @param siteId               The identifier of the site being deleted.
     * @param descendantHostCount  The number of descendant hosts that must be removed first.
     * @param descendantFolderCount The number of folders belonging to those descendant hosts.
     */
    public HostHasDescendantsException(final String siteId, final long descendantHostCount,
            final long descendantFolderCount) {
        super(String.format(
                "Site '%s' cannot be deleted because it has %d descendant host(s) and %d folder(s). "
                        + "Remove all descendant hosts first.",
                siteId, descendantHostCount, descendantFolderCount));
        this.siteId = siteId;
        this.descendantHostCount = descendantHostCount;
        this.descendantFolderCount = descendantFolderCount;
    }

    /**
     * Returns the identifier of the site that could not be deleted.
     *
     * @return The site identifier.
     */
    public String getSiteId() {
        return siteId;
    }

    /**
     * Returns the number of descendant hosts that prevent deletion.
     *
     * @return The descendant host count.
     */
    public long getDescendantHostCount() {
        return descendantHostCount;
    }

    /**
     * Returns the number of folders across all descendant hosts.
     *
     * @return The descendant folder count.
     */
    public long getDescendantFolderCount() {
        return descendantFolderCount;
    }

    /**
     * Returns the combined total of descendant hosts and folders.
     *
     * @return The total descendant count.
     */
    public long getTotalDescendantCount() {
        return descendantHostCount + descendantFolderCount;
    }

}
