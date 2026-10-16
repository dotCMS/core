package com.dotcms.auth;

import com.dotcms.datagen.UserDataGen;
import com.dotcms.rest.api.v1.authentication.ResetPasswordTokenUtil;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.cms.login.factories.LoginFactory;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.WebKeys;
import com.dotmarketing.webdav.DotWebdavHelper;
import com.liferay.portal.auth.Authenticator;
import com.liferay.portal.ejb.UserManagerFactory;
import com.liferay.portal.ejb.UserManagerUtil;
import com.liferay.portal.ejb.UserUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration tests for {@link LoginLockout} through the real authentication paths: the main login flow, WebDAV,
 * the legacy {@link LoginFactory} logins, and the password reset and update flows.
 */
public class LoginLockoutTest {

    private static final String PASSWORD = "Lockout-Test-9!xQ-correct";
    private static final String NEW_PASSWORD = "Lockout-Test-7#zR-renewed";
    private static final int LIMIT = 5;
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

    @After
    public void closeConnection() {
        DbConnectionFactory.closeSilently();
    }

    /**
     * Each login here stands for one request. A request ends by closing its DB connection and Hibernate session;
     * without that, the main login path would keep reading the user row it loaded earlier on this thread and miss a
     * password changed since through {@code UserAPI}.
     */
    private static int login(final User user, final String password) throws Exception {
        try {
            return UserManagerUtil.authenticateByUserId(
                    PublicCompanyFactory.getDefaultCompany().getCompanyId(), user.getUserId(), password);
        } finally {
            DbConnectionFactory.closeSilently();
        }
    }

    /**
     * The legacy login used by the publishing and maintenance ajax endpoints, as one request.
     */
    private static boolean legacyLogin(final User user, final String password) throws Exception {
        try {
            return LoginFactory.doLogin(loginName(user), password);
        } finally {
            DbConnectionFactory.closeSilently();
        }
    }

    /**
     * The name WebDAV and the legacy logins look the user up by: the user ID or the email, per the company setting.
     */
    private static String loginName(final User user) {
        return Company.AUTH_TYPE_ID.equals(PublicCompanyFactory.getDefaultCompany().getAuthType())
                ? user.getUserId() : user.getEmailAddress();
    }

    private static void lockByFailedLogins(final User user) throws Exception {
        for (int i = 0; i < LIMIT; i++) {
            assertEquals(Authenticator.FAILURE, login(user, "wrong-" + i));
        }
        assertTrue(LoginLockout.getInstance().isLocked(user.getUserId()));
    }

    private static boolean webdavLogin(final User user, final String password) throws Exception {
        try {
            return null != new DotWebdavHelper().authorizePrincipal(loginName(user), password);
        } catch (final DotSecurityException e) {
            return false;
        } finally {
            DbConnectionFactory.closeSilently();
        }
    }

    private static int failedLoginAttempts(final User user) throws Exception {
        return APILocator.getUserAPI().loadUserById(user.getUserId()).getFailedLoginAttempts();
    }

    private static Map<String, Object> lockoutRow(final User user) throws Exception {
        try {
            final List<Map<String, Object>> rows = new DotConnect()
                    .setSQL("SELECT locked_until, lockout_count FROM login_lockout WHERE user_id = ?")
                    .addParam(user.getUserId())
                    .loadObjectResults();
            return rows.isEmpty() ? Map.of() : rows.get(0);
        } finally {
            DbConnectionFactory.closeSilently();
        }
    }

    private static void cleanUp(final User user) {
        LoginLockout.getInstance().clear(user.getUserId());
        UserDataGen.remove(user);
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
            lockByFailedLogins(user);
            assertEquals(Authenticator.FAILURE, login(user, PASSWORD));
            assertEquals(Authenticator.FAILURE, login(user, "wrong-again"));
        } finally {
            cleanUp(user);
        }
    }

    /**
     * Given: a locked account
     * When: further logins are tried, with right and wrong passwords
     * Then: they fail without moving the failure counter or extending the lock
     */
    @Test
    public void test_failure_counter_does_not_move_while_locked() throws Exception {
        final User user = newUser();
        try {
            lockByFailedLogins(user);
            assertEquals(0, failedLoginAttempts(user));
            final Map<String, Object> rowAtLock = lockoutRow(user);

            assertEquals(Authenticator.FAILURE, login(user, "wrong-while-locked"));
            assertEquals(Authenticator.FAILURE, login(user, PASSWORD));
            assertFalse(webdavLogin(user, "wrong-while-locked"));
            assertFalse(legacyLogin(user, "wrong-while-locked"));

            assertEquals(0, failedLoginAttempts(user));
            assertEquals(rowAtLock, lockoutRow(user));
        } finally {
            cleanUp(user);
        }
    }

    /**
     * Given: a locked account
     * When: the lock is cleared directly
     * Then: the correct password works again and a success leaves no lock state behind
     */
    @Test
    public void test_clear_unlocks_and_success_resets_state() throws Exception {
        final User user = newUser();
        try {
            lockByFailedLogins(user);
            LoginLockout.getInstance().clear(user.getUserId());
            assertEquals(Authenticator.SUCCESS, login(user, PASSWORD));
            assertEquals(LoginLockout.Status.NONE, LoginLockout.getInstance().status(user.getUserId()));
        } finally {
            cleanUp(user);
        }
    }

    /**
     * Given: an account whose lock has run out
     * When: the correct password is used
     * Then: the check reports the stored row as expired, the login succeeds, and the row is removed
     */
    @Test
    public void test_success_clears_expired_lock() throws Exception {
        final User user = newUser();
        try {
            assertEquals(LoginLockout.Status.NONE, LoginLockout.getInstance().check(user.getUserId()));
            lockByFailedLogins(user);
            assertEquals(LoginLockout.Status.LOCKED, LoginLockout.getInstance().check(user.getUserId()));
            new DotConnect().setSQL("UPDATE login_lockout SET locked_until = ? WHERE user_id = ?")
                    .addParam(System.currentTimeMillis() - 1000L).addParam(user.getUserId()).loadResult();
            DbConnectionFactory.closeSilently();
            assertEquals(LoginLockout.Status.EXPIRED, LoginLockout.getInstance().check(user.getUserId()));

            assertEquals(Authenticator.SUCCESS, login(user, PASSWORD));
            assertEquals(LoginLockout.Status.NONE, LoginLockout.getInstance().status(user.getUserId()));
        } finally {
            cleanUp(user);
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
            for (int i = 0; i < LIMIT - 1; i++) {
                login(user, "wrong-" + i);
            }
            assertFalse(LoginLockout.getInstance().isLocked(user.getUserId()));
            assertEquals(Authenticator.SUCCESS, login(user, PASSWORD));
        } finally {
            cleanUp(user);
        }
    }

    /**
     * Given: a locked account
     * When: the correct password is sent over WebDAV
     * Then: it is refused like a wrong password, and accepted again once the lock is cleared
     */
    @Test
    public void test_webdav_refuses_locked_account() throws Exception {
        final User user = newUser();
        try {
            assertTrue(webdavLogin(user, PASSWORD));
            lockByFailedLogins(user);
            assertThrows(DotSecurityException.class,
                    () -> new DotWebdavHelper().authorizePrincipal(loginName(user), PASSWORD));
            LoginLockout.getInstance().clear(user.getUserId());
            assertTrue(webdavLogin(user, PASSWORD));
        } finally {
            cleanUp(user);
        }
    }

    /**
     * Given: an account
     * When: the limit of wrong passwords is sent over WebDAV
     * Then: the account is locked, for every login path
     */
    @Test
    public void test_webdav_failures_count_toward_lock() throws Exception {
        final User user = newUser();
        try {
            for (int i = 0; i < LIMIT; i++) {
                assertFalse(webdavLogin(user, "wrong-" + i));
            }
            assertTrue(LoginLockout.getInstance().isLocked(user.getUserId()));
            assertEquals(Authenticator.FAILURE, login(user, PASSWORD));
        } finally {
            cleanUp(user);
        }
    }

    /**
     * Given: a locked account
     * When: the correct password is sent to the legacy {@link LoginFactory} logins used by the ajax endpoints
     * Then: both overloads refuse it, and accept it again once the lock is cleared
     */
    @Test
    public void test_legacy_doLogin_refuses_locked_account() throws Exception {
        final User user = newUser();
        try {
            assertTrue(legacyLogin(user, PASSWORD));
            lockByFailedLogins(user);

            assertFalse(legacyLogin(user, PASSWORD));
            final HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");
            assertFalse(LoginFactory.doLogin(loginName(user), PASSWORD, false, request,
                    mock(HttpServletResponse.class)));
            DbConnectionFactory.closeSilently();

            LoginLockout.getInstance().clear(user.getUserId());
            assertTrue(legacyLogin(user, PASSWORD));
        } finally {
            cleanUp(user);
        }
    }

    /**
     * Given: an account
     * When: the limit of wrong passwords is sent to the legacy {@link LoginFactory} login
     * Then: the account is locked
     */
    @Test
    public void test_legacy_doLogin_failures_count_toward_lock() throws Exception {
        final User user = newUser();
        try {
            for (int i = 0; i < LIMIT; i++) {
                assertFalse(legacyLogin(user, "wrong-" + i));
            }
            assertTrue(LoginLockout.getInstance().isLocked(user.getUserId()));
        } finally {
            cleanUp(user);
        }
    }

    /**
     * Given: a locked account
     * When: the user resets the password with an emailed reset token
     * Then: the lock is cleared and the new password works
     */
    @Test
    public void test_password_reset_clears_lock() throws Exception {
        final Object tokenDelayBefore = Config.getStringProperty("RECOVER_PASSWORD_TOKEN_AUTH_DELAY", "2000");
        Config.setProperty("RECOVER_PASSWORD_TOKEN_AUTH_DELAY", 0);
        final User user = newUser();
        try {
            lockByFailedLogins(user);
            final String token = ResetPasswordTokenUtil.createToken();
            final User withToken = APILocator.getUserAPI().loadUserById(user.getUserId());
            withToken.setIcqId(token);
            UserUtil.update(withToken);

            UserManagerFactory.getManager().resetPassword(user.getUserId(), token, NEW_PASSWORD);

            assertEquals(LoginLockout.Status.NONE, LoginLockout.getInstance().status(user.getUserId()));
            assertEquals(Authenticator.SUCCESS, login(user, NEW_PASSWORD));
        } finally {
            Config.setProperty("RECOVER_PASSWORD_TOKEN_AUTH_DELAY", tokenDelayBefore);
            cleanUp(user);
        }
    }

    /**
     * Given: a locked account
     * When: an administrator sets a new password through {@code UserAPI.updatePassword}
     * Then: the lock and the failure count are cleared and the new password works
     */
    @Test
    public void test_admin_update_password_clears_lock() throws Exception {
        final User user = newUser();
        try {
            lockByFailedLogins(user);
            // A count left over from failures before the lock; a new password resets it too.
            final User counted = APILocator.getUserAPI().loadUserById(user.getUserId());
            counted.setFailedLoginAttempts(LIMIT - 1);
            APILocator.getUserAPI().save(counted, APILocator.systemUser(), false);
            assertEquals(LIMIT - 1, failedLoginAttempts(user));

            APILocator.getUserAPI().updatePassword(APILocator.getUserAPI().loadUserById(user.getUserId()),
                    NEW_PASSWORD, APILocator.systemUser(), false);

            assertEquals(LoginLockout.Status.NONE, LoginLockout.getInstance().status(user.getUserId()));
            assertEquals(0, failedLoginAttempts(user));
            assertEquals(Authenticator.SUCCESS, login(user, NEW_PASSWORD));
        } finally {
            cleanUp(user);
        }
    }

    /**
     * Given: a locked account
     * When: an administrator saves the user with a new password, as the Users portlet and the REST user update do
     * Then: the lock is cleared and the new password works
     */
    @Test
    public void test_admin_save_with_new_password_clears_lock() throws Exception {
        final User user = newUser();
        try {
            lockByFailedLogins(user);

            final User toSave = APILocator.getUserAPI().loadUserById(user.getUserId());
            toSave.setPassword(NEW_PASSWORD);
            APILocator.getUserAPI().save(toSave, APILocator.systemUser(), true, false);

            assertEquals(LoginLockout.Status.NONE, LoginLockout.getInstance().status(user.getUserId()));
            assertEquals(Authenticator.SUCCESS, login(user, NEW_PASSWORD));
        } finally {
            cleanUp(user);
        }
    }

    /**
     * Given: lockout switched off with {@code AUTH_LOCKOUT_ENABLED=false}
     * When: more wrong passwords than the limit are tried, on the main login and over WebDAV
     * Then: nothing is locked and the correct password still works on every path
     */
    @Test
    public void test_disabled_does_not_lock() throws Exception {
        final boolean enabledBefore = Config.getBooleanProperty(LoginLockout.LOCKOUT_ENABLED, true);
        Config.setProperty(LoginLockout.LOCKOUT_ENABLED, false);
        final User user = newUser();
        try {
            assertFalse(LoginLockout.getInstance().isEnabled());
            for (int i = 0; i <= LIMIT; i++) {
                assertEquals(Authenticator.FAILURE, login(user, "wrong-" + i));
                assertFalse(webdavLogin(user, "wrong-" + i));
            }
            assertEquals(LoginLockout.Status.NONE, LoginLockout.getInstance().status(user.getUserId()));
            assertTrue(webdavLogin(user, PASSWORD));
            assertTrue(legacyLogin(user, PASSWORD));
            assertEquals(Authenticator.SUCCESS, login(user, PASSWORD));
        } finally {
            Config.setProperty(LoginLockout.LOCKOUT_ENABLED, enabledBefore);
            cleanUp(user);
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
