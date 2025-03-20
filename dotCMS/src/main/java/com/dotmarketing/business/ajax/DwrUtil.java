package com.dotmarketing.business.ajax;

import com.dotcms.repackage.org.directwebremoting.WebContext;
import com.dotcms.repackage.org.directwebremoting.WebContextFactory;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.browser.ajax.BrowserAjax;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Provides utility methods for DWR-related classes that allow developers to retrieve common-use
 * information such as:
 * <ul>
 *     <li>The current Session, Request, Servlet Context, and DWR objects.</li>
 *     <li>The currently logged-in User and their Roles.</li>
 *     <li>Portlet validation data.</li>>
 * </ul>
 * Available methods can be added as required.
 *
 * @author Jonathan Gamba 11/29/17
 */
public class DwrUtil {

    private DwrUtil() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Validates if the logged in user has the required permissions to access the users portlet
     *
     * @param loggedInUser User to validate
     */
    public static void validateUsersPortletPermissions(final User loggedInUser)
            throws DotSecurityException, DotDataException {

        validatePortletPermissions("users", loggedInUser);
    }

    /**
     * Validates if the logged in user has the required permissions to access the roles portlet
     *
     * @param loggedInUser User to validate
     */
    public static void validateRolesPortletPermissions(final User loggedInUser)
            throws DotSecurityException, DotDataException {

        validatePortletPermissions("roles", loggedInUser);
    }

    /**
     * Validates if the logged in user has the required permissions to access the given portlet
     *
     * @param portletId Portlet to validate
     * @param loggedInUser User to validate
     */
    public static void validatePortletPermissions(final String portletId, final User loggedInUser)
            throws DotSecurityException, DotDataException {

        if (null == loggedInUser) {
            throw new DotSecurityException(
                    String.format("A logged in User is required to access to the [%s] Portlet",
                            portletId));
        }
        if (!APILocator.getLayoutAPI().doesUserHaveAccessToPortlet(portletId, loggedInUser)) {

            final String errorMessage = String.format(
                    "User [%s] does not have access to the [%s] Portlet",
                    loggedInUser.getEmailAddress(), portletId);

            SecurityLogger.logInfo(DwrUtil.class, errorMessage);
            throw new DotSecurityException(errorMessage);
        }
    }

    /**
     * Returns the logged in user.
     * <p><strong>Note:</strong> This method should be use on DWR classes ONLY</p>
     */
    public static User getLoggedInUser()
            throws PortalException, SystemException, DotSecurityException {

        final WebContext ctx = WebContextFactory.get();
        final HttpServletRequest request = ctx.getHttpServletRequest();

        final User loggedInUser = WebAPILocator.getUserWebAPI().getLoggedInUser(request);
        final String remoteIp = request.getRemoteHost();

        String userId = "[not logged in]";
        if (loggedInUser != null && loggedInUser.getUserId() != null) {
            userId = loggedInUser.getUserId();
        }
        if (loggedInUser == null) {
            SecurityLogger.logInfo(DwrUtil.class,
                    "Unauthorized attempt to call getLoggedInUser by user " + userId + " from "
                            + remoteIp);
            throw new DotSecurityException("not authorized");
        }
        return loggedInUser;
    }

    /**
     * Returns the Roles that are assigned to a given user in the form of an array.
     *
     * @param user The {@link User} whose Roles are being retrieved.
     *
     * @return The array or {@link Role} objects.
     */
    public static Role[] getUserRoles(final User user) {
        Role[] roles = new Role[]{};
        try {
            roles = com.dotmarketing.business.APILocator.getRoleAPI().loadRolesForUser(user.getUserId()).toArray(new Role[0]);
        } catch (final DotDataException e) {
            Logger.error(BrowserAjax.class, String.format("Could not get Roles for User '%s': %s", user.getUserId(),
                    e.getMessage()), e);
        }
        return roles;
    }

    /**
     * Returns the list of permissions that a given User has on a given Permissionable object, based on an array of
     * specific Roles. The Permissionable object can be a Site or a Folder.
     *
     * @param permissionable The {@link Permissionable} whose User permissions will be checked.
     * @param roles          The array of {@link Role} objects that will be used to check the User permissions.
     * @param user           The {@link User} whose permissions will be checked.
     *
     * @return The {@link Optional} containing the list of permissions that the User has on the Permissionable object.
     */
    public static Optional<List<Integer>> getPermissions(final Permissionable permissionable, final Role[] roles, final User user) {
        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        try {
            final List<Integer> permissions = permissionAPI.getPermissionIdsFromRoles(permissionable, roles, user);
            return Optional.of(permissions);
        } catch (final DotDataException e) {
            Logger.error(DwrUtil.class, String.format("Failed to retrieve permissions of User '%s' on parent '%s' for" +
                                                              " Roles [ %s ]: %s", user.getUserId(),
                    permissionable.getPermissionId(), Arrays.toString(roles), e.getMessage()), e);
            return Optional.empty();
        }
    }

    /**
     * Returns the current HTTP Session object from the Web Context Factory.
     *
     * @return The {@link HttpSession} object.
     */
    public static HttpSession getSession() {
        final WebContext ctx = WebContextFactory.get();
        final HttpServletRequest request = ctx.getHttpServletRequest();
        return request.getSession();
    }

    /**
     * Returns the current Servlet Context object from the DWR Web Context Factory.
     *
     * @return The current instance of the {@link ServletContext} object.
     */
    public static ServletContext getServletContext() {
        final WebContext ctx = WebContextFactory.get();
        return ctx.getServletContext();
    }

    /**
     * Returns the current HTTP Request object from the DWR Web Context Factory.
     *
     * @return The current instance of the {@link HttpServletRequest} object.
     */
    public static HttpServletRequest getHttpServletRequest() {
        final WebContext ctx = WebContextFactory.get();
        return ctx.getHttpServletRequest();
    }

}
