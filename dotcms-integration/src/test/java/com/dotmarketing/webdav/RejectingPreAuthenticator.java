package com.dotmarketing.webdav;

import com.liferay.portal.auth.Authenticator;

/**
 * Test pre-authenticator that turns every login down by returning {@link Authenticator#FAILURE},
 * the way a directory authenticator answers when it does not accept the credentials.
 */
public class RejectingPreAuthenticator implements Authenticator {

    /**
     * Always rejects the login.
     *
     * @return {@link Authenticator#FAILURE}
     */
    @Override
    public int authenticateByEmailAddress(final String companyId, final String emailAddress,
            final String password) {
        return FAILURE;
    }

    /**
     * Always rejects the login.
     *
     * @return {@link Authenticator#FAILURE}
     */
    @Override
    public int authenticateByUserId(final String companyId, final String userId,
            final String password) {
        return FAILURE;
    }
}
