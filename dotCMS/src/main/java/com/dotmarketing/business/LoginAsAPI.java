package com.dotmarketing.business;

import com.dotcms.system.AppContext;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;

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
    public User getPrincipalUser(AppContext context);

    /**
     * Return true if exists a LoginAs user
     *
     * @return
     */
    public boolean isLoginAsUser(AppContext context);
}
