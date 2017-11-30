package com.liferay.portlet;

import com.dotcms.repackage.org.directwebremoting.WebContext;
import com.dotcms.repackage.org.directwebremoting.WebContextFactory;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.user.ajax.UserAjax;
import com.dotmarketing.util.SecurityLogger;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;
import javax.servlet.http.HttpServletRequest;

/**
 * @author Jonathan Gamba 11/29/17
 */
public class PortletUtil {

    private PortletUtil() {
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

            SecurityLogger.logInfo(PortletUtil.class, errorMessage);
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
            SecurityLogger.logInfo(UserAjax.class,
                    "unauthorized attempt to call getUserById by user " + userId + " from "
                            + remoteIp);
            throw new DotSecurityException("not authorized");
        }
        return loggedInUser;
    }

}