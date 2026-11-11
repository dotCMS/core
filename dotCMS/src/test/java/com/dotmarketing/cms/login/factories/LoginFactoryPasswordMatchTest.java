package com.dotmarketing.cms.login.factories;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.UnitTestBase;
import com.dotcms.enterprise.PasswordFactoryProxy;
import com.dotmarketing.portal.struts.DotCustomLoginPostAction;
import com.liferay.portal.model.User;
import org.junit.Test;

/**
 * Native password-verification coverage retained after removing directory authentication.
 */
public class LoginFactoryPasswordMatchTest extends UnitTestBase {

    private static final String RIGHT_PASSWORD = "right-password";
    private static final String WRONG_PASSWORD = "wrong-password";

    @Test
    public void correctPasswordIsAccepted() throws Exception {
        assertTrue(LoginFactory.passwordMatch(RIGHT_PASSWORD, userWithPassword(RIGHT_PASSWORD)));
    }

    @Test
    public void wrongPasswordIsRefused() throws Exception {
        assertFalse(LoginFactory.passwordMatch(WRONG_PASSWORD, userWithPassword(RIGHT_PASSWORD)));
    }

    @Test
    public void missingStoredPasswordIsRefused() {
        final User user = new User();
        user.setUserId("user-without-password");

        assertFalse(LoginFactory.passwordMatch(RIGHT_PASSWORD, user));
    }

    @Test
    public void historicalDirectoryPasswordIsRefused() throws Exception {
        final String reserved = DotCustomLoginPostAction.FAKE_PASSWORD;

        assertFalse(LoginFactory.passwordMatch(reserved, userWithPassword(reserved)));
    }

    private static User userWithPassword(final String password) throws Exception {
        final User user = new User();
        user.setUserId("test-user");
        user.setPassword(PasswordFactoryProxy.generateHash(password));
        user.setPasswordEncrypted(true);
        return user;
    }
}
