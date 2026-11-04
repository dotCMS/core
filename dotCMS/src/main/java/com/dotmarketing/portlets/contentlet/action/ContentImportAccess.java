package com.dotmarketing.portlets.contentlet.action;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.liferay.portal.model.User;

/**
 * Decides whether a user may start a CSV import into a Content Type.
 *
 * <p>The bar is WRITE on the Content Type, which is what {@code ContentletAPI.checkin} already
 * requires of a brand-new contentlet. Asking for it up front therefore cannot refuse anyone who
 * would have been able to finish the import anyway — it only moves the refusal to before the
 * upload is written and the rows are read, instead of part-way through.</p>
 *
 * <p>Deliberately not a portlet check. The import screen is reached from the Content portlet, but
 * custom content portlets exist and a user may legitimately hold only one of those, so keying the
 * decision to a portlet id would lock out real users. The Content Type is what the import actually
 * writes to, and it is the same whichever screen the user came from.</p>
 */
public final class ContentImportAccess {

    private ContentImportAccess() {
        throw new AssertionError("ContentImportAccess is a utility class");
    }

    /**
     * Tells whether the given user may import content into the given Content Type.
     *
     * <p>A missing Content Type or user is refused, as is a lookup that fails: not being able to
     * decide is not permission.</p>
     *
     * @param contentType the {@link ContentType} the import would create content from;
     *                    {@code null} is refused
     * @param user        the {@link User} making the request; {@code null} is refused
     * @return {@code true} when the user may create content of that type
     */
    public static boolean canImportInto(final ContentType contentType, final User user) {
        if (null == contentType || null == user) {
            return false;
        }
        try {
            if (APILocator.getPermissionAPI().doesUserHavePermission(contentType,
                    PermissionAPI.PERMISSION_WRITE, user, false)) {
                return true;
            }
            SecurityLogger.logInfo(ContentImportAccess.class,
                    String.format("User '%s' is not allowed to import content of type '%s'",
                            user.getUserId(), contentType.variable()));
            return false;
        } catch (final Exception e) {
            // Reading the permission failed rather than came back negative. Refuse, but record
            // that this was a failure to decide so it is not mistaken later for a missing grant.
            Logger.warn(ContentImportAccess.class,
                    String.format("Unable to resolve import access for Content Type '%s'",
                            contentType.variable()), e);
            SecurityLogger.logInfo(ContentImportAccess.class,
                    String.format("Refusing the import of type '%s' for user '%s': access could "
                            + "not be resolved (%s)", contentType.variable(), user.getUserId(), e));
            return false;
        }
    }
}
