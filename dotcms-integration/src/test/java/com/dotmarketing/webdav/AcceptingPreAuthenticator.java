package com.dotmarketing.webdav;

import com.liferay.portal.auth.Authenticator;

/**
 * Test pre-authenticator that lets every login continue by returning
 * {@link Authenticator#SUCCESS}, leaving the password check to dotCMS.
 */
public class AcceptingPreAuthenticator implements Authenticator {

    /**
     * Always lets the login continue.
     *
     * @return {@link Authenticator#SUCCESS}
     */
    @Override
    public int authenticateByEmailAddress(final String companyId, final String emailAddress,
            final String password) {
        return SUCCESS;
    }

    /**
     * Always lets the login continue.
     *
     * @return {@link Authenticator#SUCCESS}
     */
    @Override
    public int authenticateByUserId(final String companyId, final String userId,
            final String password) {
        return SUCCESS;
    }
}
