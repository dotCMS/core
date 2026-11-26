package com.dotcms.browser;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.liferay.portal.model.User;
import java.util.List;

/**
 * Keeps the export rows a user can read.
 * <p>
 * Rows are checked in chunks with one batch permission query each. The batch query can wrongly
 * deny a row whose inherited permissions were never computed, or that the user reads only as its
 * owner, so every row it rejects is checked again one at a time before it is dropped.
 */
public class ReadableRowFilter {

    private final PermissionAPI permissionAPI;
    private final ContentletAPI contentletAPI;

    /**
     * Creates a filter that uses the standard dotCMS APIs.
     */
    public ReadableRowFilter() {
        this(APILocator.getPermissionAPI(), APILocator.getContentletAPI());
    }

    /**
     * Creates a filter with the given APIs. Tests use it to observe the permission calls.
     *
     * @param permissionAPI checks READ, in batch and per item
     * @param contentletAPI loads the rows the batch check rejected, for the per-item check
     */
    ReadableRowFilter(final PermissionAPI permissionAPI, final ContentletAPI contentletAPI) {
        this.permissionAPI = permissionAPI;
        this.contentletAPI = contentletAPI;
    }

    /**
     * Returns the rows {@code user} can read, in their input order.
     *
     * @param rows the candidate rows
     * @param user the {@link User} the export runs as
     * @return the readable rows
     * @throws DotDataException if the permissions cannot be read
     */
    public List<ExportRow> filter(final List<ExportRow> rows, final User user)
            throws DotDataException {
        throw new UnsupportedOperationException("not implemented");
    }
}
