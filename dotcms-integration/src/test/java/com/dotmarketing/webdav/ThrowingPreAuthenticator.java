package com.dotmarketing.webdav;

import com.liferay.portal.auth.AuthException;
import com.liferay.portal.auth.Authenticator;

/**
 * Test pre-authenticator that turns every login down by throwing {@link AuthException}, the way a
 * directory authenticator answers when it explicitly refuses the credentials rather than being
 * unable to validate them.
 */
public class ThrowingPreAuthenticator implements Authenticator {

    /**
     * Always rejects the login.
     *
     * @throws AuthException always
     */
    @Override
    public int authenticateByEmailAddress(final String companyId, final String emailAddress,
            final String password) throws AuthException {
        throw new AuthException("Rejected by " + ThrowingPreAuthenticator.class.getSimpleName());
    }

    /**
     * Always rejects the login.
     *
     * @throws AuthException always
     */
    @Override
    public int authenticateByUserId(final String companyId, final String userId,
            final String password) throws AuthException {
        throw new AuthException("Rejected by " + ThrowingPreAuthenticator.class.getSimpleName());
    }
}
