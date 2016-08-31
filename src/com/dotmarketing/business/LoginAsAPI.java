package com.dotmarketing.business;

import com.dotcms.AppContext;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;

/**
 * It is intended to be a helper to the LoginAs feature
 */
public interface LoginAsAPI {

    /**
     * Return the principal user in case that exists a LoginAs user, in othercase return null
     *
     * @param context
     * @return
     * @throws DotSecurityException
     * @throws DotDataException
     */
    public User getPrincipalUser(AppContext context) throws DotSecurityException, DotDataException;

    /**
     * Return true if exists a LoginAs user
     *
     * @return
     */
    public boolean isLoginAsUser(AppContext context);
}
