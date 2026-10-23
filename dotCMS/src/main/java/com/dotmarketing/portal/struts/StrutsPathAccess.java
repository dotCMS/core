package com.dotmarketing.portal.struts;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Layout;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PortletID;
import com.dotmarketing.util.SecurityLogger;
import com.liferay.portal.model.User;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Restricts the administrative legacy Struts action directories to the portlets that own them.
 *
 * <p>{@link #RESTRICTED_DIRECTORIES} names the action directories whose screens administer the
 * instance itself — scheduled jobs, maintenance, sites, users, roles and languages — together with
 * the portlets that may reach them. CMS administrators may reach all of them.</p>
 *
 * <p>Anything not named here is left as it was. That is deliberate: the legacy screens link to each
 * other across directories (the Sites screen opens the content editor, Site Browser opens the link
 * and container editors, several screens open the event editor), so a list of what is allowed
 * cannot be completed by reading the code, while a list of what is restricted can be verified one
 * directory at a time. Before adding a directory here, check which screens link into it — a
 * directory reached from another portlet's screen needs that portlet listed too.</p>
 */
public final class StrutsPathAccess {

    /**
     * Beta-period ids of the Users and Roles portlets. {@code portlet.xml} declares {@code
     * users-beta} and {@code roles-beta} alongside the shipped {@code users} and {@code roles},
     * and a user may hold only the beta one; the legacy screens below are the same screens either
     * way. {@link PortletID} has no constant for them yet, so they are named here as literals.
     * Remove when the beta portlets are promoted and their ids disappear from {@code portlet.xml}.
     */
    private static final String USERS_BETA = "users-beta";
    private static final String ROLES_BETA = "roles-beta";

    /**
     * Administrative action directories and the portlets allowed to reach them. Keys have no
     * leading slash and carry no action name, for example {@code ext/scheduler}.
     */
    private static final Map<String, Set<String>> RESTRICTED_DIRECTORIES = Map.of(
            "ext/scheduler", Set.of(PortletID.JOBS.toString()),
            "ext/cmsmaintenance", Set.of(PortletID.MAINTENANCE.toString()),
            "ext/hostadmin", Set.of(PortletID.SITES.toString()),
            "ext/useradmin", Set.of(PortletID.USERS.toString(), USERS_BETA),
            "ext/userfilter", Set.of(PortletID.USERS.toString(), USERS_BETA),
            "ext/roleadmin", Set.of(PortletID.USERS.toString(), USERS_BETA,
                    PortletID.ROLES.toString(), ROLES_BETA),
            "ext/languages_manager", Set.of(PortletID.LANGUAGES.toString(),
                    PortletID.LOCALES.toString()));

    private StrutsPathAccess() {
        throw new AssertionError("StrutsPathAccess is a utility class");
    }

    /**
     * Tells whether the given user may run the given Struts action path.
     *
     * <p>A path outside the restricted directories is allowed, as it was before. Inside one, the
     * user needs to be a CMS administrator or to hold one of the portlets listed for that
     * directory.</p>
     *
     * @param path the Struts action path as declared in {@code struts-config.xml}, for example
     *             {@code /ext/scheduler/edit_scheduler}
     * @param user the {@link User} making the request; {@code null} cannot reach a restricted
     *             directory
     * @return {@code true} when the user may run the path
     */
    public static boolean canAccess(final String path, final User user) {
        final Set<String> allowedPortlets = allowedPortlets(path);
        if (null == allowedPortlets) {
            return true;
        }
        if (null == user) {
            return false;
        }
        try {
            if (APILocator.getUserAPI().isCMSAdmin(user)) {
                return true;
            }
            if (user.isBackendUser() && holdsAny(portletIdsOf(user), allowedPortlets)) {
                return true;
            }
        } catch (final Exception e) {
            // Resolving access failed rather than came back negative — a database blip locks
            // everyone out, administrators included, behind the same "not allowed" page. Refuse,
            // but say in the audit trail that this was a failure to decide, not a decision, so the
            // lockout is not mistaken for a permissions problem.
            Logger.warn(StrutsPathAccess.class,
                    String.format("Unable to resolve portlet access for path '%s'", path), e);
            SecurityLogger.logInfo(StrutsPathAccess.class,
                    String.format("Refusing the struts path '%s' for user '%s': portlet access "
                            + "could not be resolved (%s)", path, user.getUserId(), e));
            return false;
        }
        SecurityLogger.logInfo(StrutsPathAccess.class,
                String.format("User '%s' is not allowed to run the struts path '%s'",
                        user.getUserId(), path));
        return false;
    }

    /**
     * Returns the portlets allowed for the directory of the given path, or {@code null} when that
     * directory is not restricted.
     *
     * @param path the Struts action path, with or without its leading slash
     * @return the allowed portlet ids, or {@code null} when the path is not restricted
     */
    private static Set<String> allowedPortlets(final String path) {
        if (null == path) {
            return null;
        }
        final String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
        final int lastSlash = normalizedPath.lastIndexOf('/');
        if (lastSlash < 1) {
            return null;
        }
        return RESTRICTED_DIRECTORIES.get(normalizedPath.substring(0, lastSlash));
    }

    /**
     * Collects the ids of every portlet the user holds through any of their layouts, in one read.
     *
     * @param user the {@link User} whose layouts are read
     * @return the ids of the portlets the user holds
     * @throws Exception if the user's layouts cannot be read
     */
    private static Set<String> portletIdsOf(final User user) throws Exception {
        final Set<String> portletIds = new HashSet<>();
        for (final Layout layout : APILocator.getLayoutAPI().loadLayoutsForUser(user)) {
            portletIds.addAll(layout.getPortletIds());
        }
        return portletIds;
    }

    /**
     * Tells whether the user holds at least one of the given portlets.
     *
     * @param userPortletIds the ids of the portlets the user holds
     * @param allowed        the ids that grant the directory
     * @return {@code true} when the two sets intersect
     */
    private static boolean holdsAny(final Set<String> userPortletIds, final Set<String> allowed) {
        for (final String portletId : allowed) {
            if (userPortletIds.contains(portletId)) {
                return true;
            }
        }
        return false;
    }
}
