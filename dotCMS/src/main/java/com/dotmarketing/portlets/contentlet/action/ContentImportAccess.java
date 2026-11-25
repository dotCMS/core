package com.dotmarketing.portlets.contentlet.action;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Constants;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Decides whether a user may start a CSV import.
 *
 * <p>This is a gate, not the enforcement. Every row of the file is authorized again as it is
 * imported, and that is what decides what actually lands. The gate exists so that a caller who
 * cannot import anything is turned away before the upload is stored and the file is parsed, and so
 * that they are told why. It therefore has to admit everyone the per-row checks would have
 * admitted: a gate that is stricter than the import takes away an import that works today.</p>
 *
 * <p>There are three ways in, one per way a row can be authorized:</p>
 *
 * <ul>
 *   <li>A row that creates content takes the Content Type branch of
 *   {@code ESContentletAPIImpl.checkPermission}, which asks for WRITE on the type.</li>
 *   <li>A row that updates existing content takes the contentlet branch instead, and a contentlet
 *   inherits its permissions from its folder and then its site. {@code ImportUtil.setSiteAndFolder}
 *   independently requires add-children, and it asks the <em>folder</em> first, falling back to the
 *   site only when the row names no folder.</li>
 *   <li>So add-children on the site is one way in, and add-children on a folder of that site is
 *   another. Delegating a section by breaking permissions on a folder is the ordinary way to share
 *   a site in dotCMS, and such a user imports updates today.</li>
 * </ul>
 *
 * <p>Deliberately not a portlet check. The import screen is reached from the Content portlet, but
 * custom content portlets exist and a user may legitimately hold only one of those, so keying the
 * decision to a portlet id would lock out real users.</p>
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
     * permission. A missing site is not a refusal on its own — the back end has no site selected
     * when the user is on "All Sites", and the question then becomes whether they may add content
     * to any site they can reach.</p>
     *
     * @param contentType the {@link ContentType} the import would create content from, or
     *                    {@code null} when it could not be resolved
     * @param site        the {@link Host} selected in the back end, or {@code null} when none is
     * @param user        the {@link User} making the request; {@code null} is refused
     * @return {@code true} when the user may create content of that type, or add content anywhere
     *         the import could put it
     */
    public static boolean canImportInto(final ContentType contentType, final Host site,
            final User user) {
        if (null == user) {
            return false;
        }
        try {
            if (null != contentType && APILocator.getPermissionAPI().doesUserHavePermission(
                    contentType, PermissionAPI.PERMISSION_WRITE, user,
                    Constants.DONT_RESPECT_FRONT_END_ROLES)) {
                return true;
            }
            if (canAddContentSomewhere(site, user)) {
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
     * Tells whether the user may add content anywhere the import could put it: on one of the sites
     * in scope, or in any folder of one of them.
     *
     * <p>Scope is the selected site when there is one, and every site the user can read when there
     * is not, which is what the back end shows them on "All Sites". Scoping to what the user can
     * read is also what keeps this cheap for a caller who holds nothing: their list is empty and
     * the answer costs one query, rather than a walk of every folder in the installation.</p>
     *
     * <p>The site check comes first because it is a single batch call and it is how most users are
     * granted; the folder walk only runs for the user whose grant is on a folder, and stops at the
     * first site that answers yes. It costs one recursive folder read per site in scope, paid once
     * per import attempt rather than per row.</p>
     *
     * @param site the {@link Host} selected in the back end, or {@code null} when none is
     * @param user the {@link User} making the request
     * @return {@code true} when the user holds add-children on a site or folder in scope
     */
    private static boolean canAddContentSomewhere(final Host site, final User user)
            throws Exception {
        final List<Host> sites = null != site
                ? Collections.singletonList(site)
                : APILocator.getHostAPI().findAll(user, Constants.DONT_RESPECT_FRONT_END_ROLES);
        if (null == sites || sites.isEmpty()) {
            return false;
        }
        if (holdsAddChildren(new ArrayList<Permissionable>(sites), user)) {
            return true;
        }
        for (final Host candidate : sites) {
            // A site the user cannot read throws rather than returning nothing, and that is not an
            // answer about the other sites, so it is taken as a no for this one and the walk goes
            // on.
            final List<Folder> folders = Try.of(() -> APILocator.getFolderAPI()
                            .findSubFoldersRecursively(candidate, user,
                                    Constants.DONT_RESPECT_FRONT_END_ROLES))
                    .getOrElse(Collections::emptyList);
            if (holdsAddChildren(new ArrayList<Permissionable>(folders), user)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tells whether the user holds add-children on any of the given objects, in one round trip
     * rather than one per object.
     *
     * @param permissionables the objects to test; an empty list is {@code false}
     * @param user            the {@link User} making the request
     * @return {@code true} when at least one of them is granted
     */
    private static boolean holdsAddChildren(final List<Permissionable> permissionables,
            final User user) throws Exception {
        return !permissionables.isEmpty() && !APILocator.getPermissionAPI()
                .filterCollection(permissionables, PermissionAPI.PERMISSION_CAN_ADD_CHILDREN,
                        Constants.DONT_RESPECT_FRONT_END_ROLES, user).isEmpty();
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
        return null != site ? site.getHostname() : "<none selected>";
    }
}
