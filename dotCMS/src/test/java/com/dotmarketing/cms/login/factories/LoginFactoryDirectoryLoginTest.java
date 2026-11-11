package com.dotmarketing.cms.login.factories;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.UnitTestBase;
import com.dotcms.auth.LoginLockout;
import com.dotcms.enterprise.PasswordFactoryProxy;
import com.dotmarketing.util.Config;
import com.liferay.portal.auth.Authenticator;
import com.liferay.portal.model.User;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Contract test for the login decision taken when a directory service answers for the user.
 *
 * <p>The directory authenticator answers {@link Authenticator#SUCCESS} both for a user it
 * authenticated and for one it knows nothing about, where the answer only means "I did not
 * authenticate this user, decide it yourself". The decision must therefore still require the
 * password stored in dotCMS, the way {@code UserManagerImpl#authenticate} does for the REST,
 * Basic and WebDAV logins. A user the directory did authenticate matches that stored password,
 * because the directory copies the accepted password into dotCMS before the decision runs.</p>
 *
 * <p>Login lockout is switched off here so the password comparison needs no database.</p>
 */
public class LoginFactoryDirectoryLoginTest extends UnitTestBase {

    private static final String RIGHT_PASSWORD = "right-password";
    private static final String WRONG_PASSWORD = "wrong-password";
    private static final String SOURCE = "203.0.113.10";

    private String previousLockoutEnabled;

    /** Keeps the password comparison off the database. */
    @Before
    public void disableLockout() {
        previousLockoutEnabled = Config.getStringProperty(LoginLockout.LOCKOUT_ENABLED, null);
        Config.setProperty(LoginLockout.LOCKOUT_ENABLED, "false");
    }

    /** Restores the lockout setting so an assertion failure cannot leak configuration. */
    @After
    public void restoreLockout() {
        Config.setProperty(LoginLockout.LOCKOUT_ENABLED, previousLockoutEnabled);
    }

    /** A user the directory authenticated signs in: dotCMS holds the password it accepted. */
    @Test
    public void directorySuccessWithTheStoredPasswordIsAccepted() throws Exception {
        assertTrue("A directory login whose password matches the one stored in dotCMS is accepted",
                LoginFactory.directoryLoginAccepted(Authenticator.SUCCESS, RIGHT_PASSWORD,
                        userWithPassword(RIGHT_PASSWORD), SOURCE));
    }

    /** The fallback answer for a user the directory did not authenticate proves no password. */
    @Test
    public void directorySuccessWithAnotherPasswordIsRefused() throws Exception {
        assertFalse("A directory answer is not a password check: the stored password is required",
                LoginFactory.directoryLoginAccepted(Authenticator.SUCCESS, WRONG_PASSWORD,
                        userWithPassword(RIGHT_PASSWORD), SOURCE));
    }

    /** An account with no stored password cannot be signed in by the directory's answer alone. */
    @Test
    public void directorySuccessWithNoStoredPasswordIsRefused() {
        final User user = new User();
        user.setUserId("user-without-password");

        assertFalse("An account with no stored password is refused",
                LoginFactory.directoryLoginAccepted(Authenticator.SUCCESS, RIGHT_PASSWORD, user, SOURCE));
    }

    /** A directory that refused the login is refused here too, right password or not. */
    @Test
    public void directoryFailureIsRefused() throws Exception {
        assertFalse("A directory failure is refused even with the stored password",
                LoginFactory.directoryLoginAccepted(Authenticator.FAILURE, RIGHT_PASSWORD,
                        userWithPassword(RIGHT_PASSWORD), SOURCE));
        assertFalse("A user the directory does not know is refused",
                LoginFactory.directoryLoginAccepted(Authenticator.DNE, RIGHT_PASSWORD,
                        userWithPassword(RIGHT_PASSWORD), SOURCE));
    }

    /** No local account, no login, whatever the directory answered. */
    @Test
    public void unknownLocalUserIsRefused() {
        assertFalse("A login with no local account is refused",
                LoginFactory.directoryLoginAccepted(Authenticator.SUCCESS, RIGHT_PASSWORD, null, SOURCE));
    }

    private static User userWithPassword(final String password) throws Exception {
        final User user = new User();
        user.setUserId("test-user");
        user.setPassword(PasswordFactoryProxy.generateHash(password));
        user.setPasswordEncrypted(true);
        return user;
    }
}
