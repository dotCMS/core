package com.dotmarketing.portlets.contentlet.action;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.util.Constants;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.liferay.portal.model.User;

/**
 * Decides whether a user may start a CSV import.
 *
 * <p>A user passes with WRITE on the chosen Content Type, or with add-children on the site the
 * import is running against. Those are the two ways the import itself can succeed, so the check
 * admits everyone the import would have admitted:</p>
 *
 * <ul>
 *   <li>A row that creates content takes the Content Type branch of
 *   {@code ESContentletAPIImpl.checkPermission}, which asks for WRITE on the type.</li>
 *   <li>A row that updates existing content takes the contentlet branch instead, and a contentlet
 *   inherits its permissions from its folder and then its site, falling back to the type only when
 *   neither is set. {@code ImportUtil.setSiteAndFolder} independently requires add-children on the
 *   folder or the site for every row. So a user with add-children on the site but nothing on the
 *   type can run an update-only import today, and keeps being able to.</li>
 * </ul>
 *
 * <p>Deliberately not a portlet check. The import screen is reached from the Content portlet, but
 * custom content portlets exist and a user may legitimately hold only one of those, so keying the
 * decision to a portlet id would lock out real users.</p>
 *
 * <p>This is a gate, not a substitute for the per-row checks: it stops a caller who cannot import
 * anything before the upload is stored and the file is read, and leaves the row-level decisions
 * where they already are.</p>
 */
public final class ContentImportAccess {

    private ContentImportAccess() {
        throw new AssertionError("ContentImportAccess is a utility class");
    }

    /**
     * Tells whether the given user may import content into the given Content Type, on the given
     * site.
     *
     * <p>A missing user is refused, as is a lookup that fails: not being able to decide is not
     * permission. A missing site only removes that half of the question; a missing Content Type
     * removes the other half, and when both are missing the answer is no.</p>
     *
     * @param contentType the {@link ContentType} the import would create content from, or
     *                    {@code null} when it could not be resolved
     * @param site        the {@link Host} the import is running against, or {@code null} when no
     *                    site is selected
     * @param user        the {@link User} making the request; {@code null} is refused
     * @return {@code true} when the user may create content of that type, or add content to that
     *         site
     */
    public static boolean canImportInto(final ContentType contentType, final Host site,
            final User user) {
        if (null == user) {
            return false;
        }
        try {
            final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
            if (null != contentType && permissionAPI.doesUserHavePermission(contentType,
                    PermissionAPI.PERMISSION_WRITE, user,
                    Constants.DONT_RESPECT_FRONT_END_ROLES)) {
                return true;
            }
            if (null != site && permissionAPI.doesUserHavePermission(site,
                    PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, user,
                    Constants.DONT_RESPECT_FRONT_END_ROLES)) {
                return true;
            }
            SecurityLogger.logInfo(ContentImportAccess.class,
                    String.format("User '%s' is not allowed to import content of type '%s' on "
                                    + "site '%s'", user.getUserId(), describe(contentType),
                            describe(site)));
            return false;
        } catch (final Exception e) {
            // Reading the permission failed rather than came back negative. Refuse, but record
            // that this was a failure to decide so it is not mistaken later for a missing grant.
            Logger.warn(ContentImportAccess.class,
                    String.format("Unable to resolve import access for Content Type '%s'",
                            describe(contentType)), e);
            SecurityLogger.logInfo(ContentImportAccess.class,
                    String.format("Refusing the import of type '%s' for user '%s': access could "
                            + "not be resolved (%s)", describe(contentType), user.getUserId(), e));
            return false;
        }
    }

    /**
     * Names a Content Type for a log line, without throwing when it is missing.
     *
     * @param contentType the {@link ContentType} to name, possibly {@code null}
     * @return the type's variable name, or a placeholder
     */
    private static String describe(final ContentType contentType) {
        return null != contentType ? contentType.variable() : "<unresolved>";
    }

    /**
     * Names a site for a log line, without throwing when it is missing.
     *
     * @param site the {@link Host} to name, possibly {@code null}
     * @return the site's host name, or a placeholder
     */
    private static String describe(final Host site) {
        return null != site ? site.getHostname() : "<none>";
    }
}
