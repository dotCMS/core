package com.dotcms.auth;

import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.auth.Authenticator;
import com.liferay.portal.ejb.UserManagerUtil;
import com.liferay.portal.model.User;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Integration tests for {@link LoginLockout} through the real authentication path.
 */
public class LoginLockoutTest {

    private static final String PASSWORD = "Lockout-Test-9!xQ-correct";
    private static Object delayEnabledBefore;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        delayEnabledBefore = Config.getStringProperty(WebKeys.AUTH_FAILED_ATTEMPTS_DELAY_STRATEGY_ENABLED, "true");
        // The per-attempt sleep is not under test and would make each case take tens of seconds.
        Config.setProperty(WebKeys.AUTH_FAILED_ATTEMPTS_DELAY_STRATEGY_ENABLED, false);
    }

    @AfterClass
    public static void restore() {
        Config.setProperty(WebKeys.AUTH_FAILED_ATTEMPTS_DELAY_STRATEGY_ENABLED, delayEnabledBefore);
    }

    private static User newUser() throws Exception {
        final User user = new UserDataGen().nextPersisted();
        APILocator.getUserAPI().updatePassword(user, PASSWORD, APILocator.systemUser(), false);
        return user;
    }

    private static int login(final User user, final String password) throws Exception {
        return UserManagerUtil.authenticateByUserId(
                PublicCompanyFactory.getDefaultCompany().getCompanyId(), user.getUserId(), password);
    }

    /**
     * Given: an account and the default limit of 5 failures
     * When: 5 wrong passwords are tried
     * Then: the account is locked and the correct password is refused with the same result as a wrong one
     */
    @Test
    public void test_lockout_after_limit_refuses_correct_password() throws Exception {
        final User user = newUser();
        try {
            assertEquals(Authenticator.SUCCESS, login(user, PASSWORD));
            for (int i = 0; i < 5; i++) {
                assertEquals(Authenticator.FAILURE, login(user, "wrong-" + i));
            }
            assertTrue(LoginLockout.getInstance().isLocked(user.getUserId()));
            assertEquals(Authenticator.FAILURE, login(user, PASSWORD));
            assertEquals(Authenticator.FAILURE, login(user, "wrong-again"));
        } finally {
            LoginLockout.getInstance().clear(user.getUserId());
            UserDataGen.remove(user);
        }
    }

    /**
     * Given: a locked account
     * When: the lock is cleared (as an administrator unlock or a password reset does)
     * Then: the correct password works again and a success leaves no lock state behind
     */
    @Test
    public void test_clear_unlocks_and_success_resets_state() throws Exception {
        final User user = newUser();
        try {
            for (int i = 0; i < 5; i++) {
                login(user, "wrong-" + i);
            }
            assertTrue(LoginLockout.getInstance().isLocked(user.getUserId()));
            LoginLockout.getInstance().clear(user.getUserId());
            assertEquals(Authenticator.SUCCESS, login(user, PASSWORD));
            assertFalse(LoginLockout.getInstance().isLocked(user.getUserId()));
        } finally {
            LoginLockout.getInstance().clear(user.getUserId());
            UserDataGen.remove(user);
        }
    }

    /**
     * Given: fewer failures than the limit
     * When: the correct password is used
     * Then: login succeeds and no lock is set
     */
    @Test
    public void test_below_limit_does_not_lock() throws Exception {
        final User user = newUser();
        try {
            for (int i = 0; i < 4; i++) {
                login(user, "wrong-" + i);
            }
            assertFalse(LoginLockout.getInstance().isLocked(user.getUserId()));
            assertEquals(Authenticator.SUCCESS, login(user, PASSWORD));
        } finally {
            UserDataGen.remove(user);
        }
    }

    /**
     * The lock length doubles with each lockout and stops at the maximum.
     */
    @Test
    public void test_lock_duration_doubles_and_is_capped() {
        final LoginLockout lockout = new LoginLockout(System::currentTimeMillis);
        assertEquals(900, lockout.lockSeconds(0));
        assertEquals(1800, lockout.lockSeconds(1));
        assertEquals(3600, lockout.lockSeconds(2));
        assertEquals(86400, lockout.lockSeconds(10));
        assertEquals(86400, lockout.lockSeconds(Integer.MAX_VALUE));
    }
}
