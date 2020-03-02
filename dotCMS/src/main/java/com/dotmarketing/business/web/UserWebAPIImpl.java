package com.dotmarketing.business.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import com.dotcms.business.CloseDBIfOpened;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.UserAPIImpl;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;


/**
 */
public class UserWebAPIImpl extends UserAPIImpl implements UserWebAPI {

    public UserWebAPIImpl() {

    }



    @CloseDBIfOpened
    @Override
    public User getUser(HttpServletRequest request) {

        User user = PortalUtil.getUser(request);

        if (user == null) {
            user = APILocator.getUserAPI().getAnonymousUserNoThrow();
        }
        return user;

    }


    @CloseDBIfOpened
    @Override
    public User getLoggedInUser(HttpServletRequest request) {
        User user = PortalUtil.getUser(request);

        return (user != null && !user.isAnonymousUser() && user.isActive() && (user.isBackendUser() || user.isFrontendUser()))
                        ? user
                        : null;
    }

    @Deprecated
    @Override
    public User getLoggedInUser(final HttpSession session) {
        User user = PortalUtil.getUser(session);
        return (user != null && !user.isAnonymousUser() && user.isActive() && (user.isBackendUser() || user.isFrontendUser()))
                        ? user
                        : null;
    }

    @Override
    public boolean isLoggedToBackend(HttpServletRequest request) {
        return PortalUtil.getUser(request) != null && PortalUtil.getUser(request).isBackendUser();
    }

    @Override
    public User getLoggedInFrontendUser(HttpServletRequest request) {
        User user = getLoggedInUser(request);

        return user != null && PortalUtil.getUser(request).isFrontendUser() ? user : null;
    }

    @Override
    public boolean isLoggedToFrontend(HttpServletRequest request) {
        User user = getLoggedInUser(request);
        return user != null && PortalUtil.getUser(request).isFrontendUser();
    }


}
