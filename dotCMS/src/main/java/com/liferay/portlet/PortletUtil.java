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

    /**
     * Validates if the logged in user has the required permissions to access the users portlet
     *
     * @param loggedInUser User to validate
     */
    public static void validateUsersPortletPermissions(User loggedInUser)
            throws DotSecurityException, DotDataException, SystemException, PortalException {

        validatePortletPermissions("users", loggedInUser);
    }

    /**
     * Validates if the logged in user has the required permissions to access the roles portlet
     *
     * @param loggedInUser User to validate
     */
    public static void validateRolesPortletPermissions(User loggedInUser)
            throws DotSecurityException, DotDataException, SystemException, PortalException {

        validatePortletPermissions("roles", loggedInUser);
    }

    /**
     * Validates if the logged in user has the required permissions to access the given portlet
     *
     * @param portletId Portlet to validate
     * @param loggedInUser User to validate
     */
    public static void validatePortletPermissions(String portletId, User loggedInUser)
            throws DotSecurityException, DotDataException, SystemException, PortalException {

        if (null == loggedInUser) {
            throw new DotSecurityException(
                    String.format("A logged in User is required to access to the [%s] Portlet",
                            portletId));
        }
        if (!APILocator.getLayoutAPI().doesUserHaveAccessToPortlet(portletId, loggedInUser)) {

            String errorMessage = String.format(
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

        WebContext ctx = WebContextFactory.get();
        HttpServletRequest request = ctx.getHttpServletRequest();

        User loggedInUser = WebAPILocator.getUserWebAPI().getLoggedInUser(request);

        String remoteIp = request.getRemoteHost();
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