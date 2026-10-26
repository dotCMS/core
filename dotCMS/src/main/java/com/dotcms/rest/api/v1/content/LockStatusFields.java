package com.dotcms.rest.api.v1.content;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.liferay.portal.model.User;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Builds the lock fields of a lock-status response ({@code lockedOn}, {@code lockedBy},
 * {@code lockedByName}), shared by the lock-status endpoints of the content REST resources.
 *
 * @author hassandotcms
 */
public final class LockStatusFields {

    private LockStatusFields() {
    }

    /**
     * Returns the lock fields for the given contentlet, as seen by the given caller.
     * <p>
     * Nothing is returned when the contentlet is not locked. When it is locked, every caller gets
     * {@code lockedOn}. Only back-end users also get {@code lockedBy} (the lock owner's user id)
     * and {@code lockedByName} (the lock owner's full name, as text).
     *
     * @param contentlet the {@link Contentlet} whose lock is described
     * @param caller     the {@link User} making the request; may be {@code null} for an
     *                   anonymous request
     * @return the lock fields, in a mutable map; empty when the contentlet is not locked
     * @throws DotDataException     if the lock information or the lock owner cannot be loaded
     * @throws DotSecurityException if the lock owner cannot be loaded
     * @throws com.dotmarketing.business.NoSuchUserException (unchecked) when a back-end caller
     *                              asks about a lock whose owner no longer exists
     */
    public static Map<String, Object> of(final Contentlet contentlet, final User caller)
            throws DotDataException, DotSecurityException {

        final Map<String, Object> fields = new LinkedHashMap<>();
        final Optional<ContentletVersionInfo> versionInfo = APILocator.getVersionableAPI()
                .getContentletVersionInfo(contentlet.getIdentifier(), contentlet.getLanguageId());
        if (versionInfo.isEmpty() || !versionInfo.get().isLocked()) {
            return fields;
        }

        fields.put("lockedOn", versionInfo.get().getLockedOn());
        if (null != caller && caller.isBackendUser()) {
            final String ownerId = versionInfo.get().getLockedBy();
            fields.put("lockedBy", ownerId);
            fields.put("lockedByName", APILocator.getUserAPI().loadUserById(ownerId).getFullName());
        }
        return fields;
    }
}
