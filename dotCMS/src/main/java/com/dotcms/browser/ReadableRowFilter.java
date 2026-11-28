package com.dotcms.browser;

import com.dotmarketing.beans.PermissionableProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Keeps the export rows a user can read.
 * <p>
 * Rows are checked in chunks with one batch permission query each. The batch query can wrongly
 * deny a row whose inherited permissions were never computed, or that the user reads only as its
 * owner, so every row it rejects is checked again one at a time before it is dropped.
 */
public class ReadableRowFilter {

    /** Rows per batch permission query. */
    static final int CHUNK_SIZE = 500;

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
     * Returns the rows {@code user} can read, in their input order. A CMS Administrator and the
     * system user read everything, so no permission query is made for them.
     *
     * @param rows the candidate rows
     * @param user the {@link User} the export runs as
     * @return the readable rows
     * @throws DotDataException if the permissions cannot be read
     */
    public List<ExportRow> filter(final List<ExportRow> rows, final User user)
            throws DotDataException {
        if (rows.isEmpty() || readsEverything(user)) {
            return new ArrayList<>(rows);
        }
        final List<ExportRow> readable = new ArrayList<>(rows.size());
        for (int start = 0; start < rows.size(); start += CHUNK_SIZE) {
            final List<ExportRow> chunk = rows.subList(start, Math.min(start + CHUNK_SIZE, rows.size()));
            final Set<String> allowed = readableIdentifiers(chunk, user);
            for (final ExportRow row : chunk) {
                if (allowed.contains(row.identifier())) {
                    readable.add(row);
                }
            }
        }
        return readable;
    }

    /**
     * Whether the user reads every row without a check: the system user or a CMS Administrator.
     *
     * @param user the {@link User} the export runs as
     * @return {@code true} when no permission check is needed
     */
    private static boolean readsEverything(final User user) {
        return APILocator.systemUser().getUserId().equals(user.getUserId()) || user.isAdmin();
    }

    /**
     * Checks one chunk: one batch query, then a per-item check of what it rejected.
     *
     * @param chunk the rows to check
     * @param user  the {@link User} the export runs as
     * @return the identifiers of the readable rows of the chunk
     * @throws DotDataException if the permissions cannot be read
     */
    private Set<String> readableIdentifiers(final List<ExportRow> chunk, final User user)
            throws DotDataException {
        // Permissions are kept per identifier, so the languages of one contentlet share one check
        final List<PermissionableProxy> proxies = chunk.stream().map(ExportRow::identifier)
                .distinct().map(ReadableRowFilter::proxy).collect(Collectors.toList());
        final Set<String> allowed = new HashSet<>();
        try {
            permissionAPI.filterCollection(proxies, PermissionAPI.PERMISSION_READ, user, false)
                    .forEach(proxy -> allowed.add(proxy.getIdentifier()));
        } catch (final DotSecurityException e) {
            throw new DotDataException(e.getMessage(), e);
        }

        final List<String> rejectedInodes = new ArrayList<>();
        final Set<String> rejectedIdentifiers = new HashSet<>();
        for (final ExportRow row : chunk) {
            if (!allowed.contains(row.identifier()) && rejectedIdentifiers.add(row.identifier())) {
                rejectedInodes.add(row.inode());
            }
        }
        if (rejectedInodes.isEmpty()) {
            return allowed;
        }
        final List<Contentlet> rejected;
        try {
            rejected = contentletAPI.findContentlets(rejectedInodes);
        } catch (final DotSecurityException e) {
            throw new DotDataException(e.getMessage(), e);
        }
        for (final Contentlet contentlet : rejected) {
            if (permissionAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_READ,
                    user, false)) {
                allowed.add(contentlet.getIdentifier());
            }
        }
        return allowed;
    }

    /**
     * A stand-in for a contentlet in the batch query, which reads only its permission id and type.
     *
     * @param identifier the contentlet identifier
     * @return the proxy
     */
    private static PermissionableProxy proxy(final String identifier) {
        final PermissionableProxy proxy = new PermissionableProxy();
        proxy.setIdentifier(identifier);
        proxy.setInode(identifier);
        proxy.setType(Contentlet.class.getCanonicalName());
        return proxy;
    }
}
