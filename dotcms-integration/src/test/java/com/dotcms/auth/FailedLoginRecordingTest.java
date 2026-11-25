package com.dotcms.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.SecurityUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.UserFactory;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.auth.Authenticator;
import com.liferay.portal.ejb.UserManagerUtil;
import com.liferay.portal.model.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import javax.servlet.http.HttpServletRequest;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.MockedStatic;

/**
 * Integration tests for how the main login flow records failed and successful sign-in attempts on the user
 * record. A failed login waits for the failed-login delay before it stores its count, so these tests check that
 * storing the count leaves every other user field as it is in the database at that moment, including changes
 * committed by someone else during the delay.
 *
 * <p>The delay is replaced by a hook for the duration of one login: the hook runs the concurrent change on another
 * thread and waits for it to commit, so the change always lands inside the delay without depending on timing.
 */
public class FailedLoginRecordingTest {

    private static final String PASSWORD = "Recording-Test-4!kP-original";
    private static final String NEW_PASSWORD = "Recording-Test-8#wM-changed";
    // Documentation address range (RFC 5737), so it never collides with a real client.
    private static final String ADDRESS = "203.0.113.44";
    private static String delayEnabledBefore;
    private static String delayStrategyBefore;
    private static ExecutorService otherRequest;

    /**
     * Starts the services, keeps the failed-login delay switched on (a hook replaces it in the tests that change the
     * user during the delay) and sets it to zero length so the other failed logins return at once.
     */
    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        delayEnabledBefore = Config.getStringProperty(WebKeys.AUTH_FAILED_ATTEMPTS_DELAY_STRATEGY_ENABLED, "true");
        delayStrategyBefore = Config.getStringProperty(WebKeys.AUTH_FAILED_ATTEMPTS_DELAY_STRATEGY, "pow");
        Config.setProperty(WebKeys.AUTH_FAILED_ATTEMPTS_DELAY_STRATEGY_ENABLED, true);
        Config.setProperty(WebKeys.AUTH_FAILED_ATTEMPTS_DELAY_STRATEGY, "TIME_MILLS:0");
        otherRequest = Executors.newSingleThreadExecutor();
    }

    /** Restores the delay settings and stops the thread used for concurrent changes. */
    @AfterClass
    public static void restore() {
        Config.setProperty(WebKeys.AUTH_FAILED_ATTEMPTS_DELAY_STRATEGY_ENABLED, delayEnabledBefore);
        Config.setProperty(WebKeys.AUTH_FAILED_ATTEMPTS_DELAY_STRATEGY, delayStrategyBefore);
        otherRequest.shutdownNow();
    }

    /** Makes the logins in each test come from a fixed client address, as the servlet container does. */
    @Before
    public void fromAddress() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn(ADDRESS);
        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);
    }

    /** Ends the request: closes the connection and clears the request thread-local. */
    @After
    public void endRequest() {
        DbConnectionFactory.closeSilently();
        HttpServletRequestThreadLocal.INSTANCE.setRequest(null);
    }

    /**
     * Given: an account, and a login with a wrong password that is waiting in its failed-login delay
     * When: an administrator changes the account's password during that delay
     * Then: the new password is still in force after the failed login finishes, and the old one is refused
     */
    @Test
    public void test_password_change_during_failed_login_delay_is_kept() throws Exception {
        final User user = newUser();
        try {
            failedLoginWhile(user, () -> {
                // The users REST endpoint changes the password on a copy of the loaded user
                final User copy = (User) APILocator.getUserAPI().loadUserById(user.getUserId()).clone();
                copy.setPassword(NEW_PASSWORD);
                APILocator.getUserAPI().save(copy, APILocator.systemUser(), true, false);
            });

            assertEquals("the changed password logs in", Authenticator.SUCCESS, login(user, NEW_PASSWORD));
            assertEquals("the replaced password is refused", Authenticator.FAILURE, login(user, PASSWORD));
        } finally {
            cleanUp(user);
        }
    }

    /**
     * Given: an account, and a login with a wrong password that is waiting in its failed-login delay
     * When: an administrator deactivates the account during that delay, working on its own copy of the user rather
     * than the instance the login holds (as on another cluster node)
     * Then: the account is still inactive after the failed login finishes, and its count of failures is stored
     */
    @Test
    public void test_deactivation_during_failed_login_delay_is_kept() throws Exception {
        final User user = newUser();
        try {
            failedLoginWhile(user, () -> {
                final User copy = (User) APILocator.getUserAPI().loadUserById(user.getUserId()).clone();
                copy.setActive(false);
                APILocator.getUserAPI().save(copy, APILocator.systemUser(), false);
            });

            final Map<String, Object> row = storedRow(user);
            assertFalse("the account stays inactive", toBoolean(row.get("active_")));
            assertEquals("the failed login is counted", 1, ((Number) row.get("failedloginattempts")).intValue());
            assertFalse("the cached user is not active either",
                    APILocator.getUserAPI().loadUserById(user.getUserId()).isActive());
        } finally {
            cleanUp(user);
        }
    }

    /**
     * Given: an account
     * When: two logins with a wrong password fail, then the right password is used
     * Then: each failure adds one to the stored count, the cached user shows the stored count, and the successful
     * login sets it back to zero
     */
    @Test
    public void test_failures_are_counted_and_success_resets_the_count() throws Exception {
        final User user = newUser();
        try {
            // Load the user so it is cached before the failures
            assertEquals(0, APILocator.getUserAPI().loadUserById(user.getUserId()).getFailedLoginAttempts());

            assertEquals(Authenticator.FAILURE, login(user, "wrong-1"));
            assertEquals(1, storedFailures(user));
            assertEquals("the cache does not keep the old count", 1,
                    APILocator.getUserAPI().loadUserById(user.getUserId()).getFailedLoginAttempts());

            assertEquals(Authenticator.FAILURE, login(user, "wrong-2"));
            assertEquals(2, storedFailures(user));

            assertEquals(Authenticator.SUCCESS, login(user, PASSWORD));
            assertEquals(0, storedFailures(user));
            assertEquals("the cache does not keep the old count", 0,
                    APILocator.getUserAPI().loadUserById(user.getUserId()).getFailedLoginAttempts());
        } finally {
            cleanUp(user);
        }
    }

    /**
     * Given: an account with login lockout on and the default limit of 5 failures
     * When: 5 logins with a wrong password fail from one address
     * Then: the address is locked out, and the account's count of failures starts over at zero
     */
    @Test
    public void test_count_starts_over_when_the_limit_is_reached() throws Exception {
        final User user = newUser();
        final int limit = LoginLockout.maxFailures();
        try {
            assertTrue("lockout is on by default", LoginLockout.getInstance().isEnabled(limit));
            for (int i = 1; i < limit; i++) {
                assertEquals(Authenticator.FAILURE, login(user, "wrong-" + i));
                assertEquals(i, storedFailures(user));
            }
            assertEquals(Authenticator.FAILURE, login(user, "wrong-" + limit));
            assertEquals(0, storedFailures(user));
            assertTrue(LoginLockout.getInstance().isLocked(user.getUserId(), ADDRESS));
        } finally {
            cleanUp(user);
        }
    }

    /**
     * Given: an account with login lockout on, one failure short of the limit
     * When: the failure that reaches the limit is stored, and another failed login for the account stores its own
     * failure right after that increment, before the count starts over
     * Then: the row is locked against other connections until the count has started over, and the other failure
     * is kept, so the new count is 1 rather than 0
     */
    @Test
    public void test_failure_stored_while_the_count_starts_over_is_kept() throws Exception {
        final User user = newUser();
        final int limit = LoginLockout.maxFailures();
        final UserFactory realFactory = FactoryLocator.getUserFactory();
        final UserFactory factory = spy(realFactory);
        final Future<?>[] concurrent = new Future<?>[1];
        doAnswer(invocation -> {
            final Object count = invocation.callRealMethod();
            if (concurrent[0] == null) {
                assertRowLocked(user);
                concurrent[0] = otherRequest.submit(() -> {
                    try {
                        return realFactory.incrementFailedLoginAttempts(user.getUserId());
                    } finally {
                        DbConnectionFactory.closeSilently();
                    }
                });
                try {
                    // Lets the other failure finish first when nothing holds it back
                    concurrent[0].get(2, TimeUnit.SECONDS);
                } catch (final TimeoutException e) {
                    // Held back until the count has started over, which is what keeps it
                }
            }
            return count;
        }).when(factory).incrementFailedLoginAttempts(user.getUserId());
        try {
            for (int i = 1; i < limit; i++) {
                assertEquals(Authenticator.FAILURE, login(user, "wrong-" + i));
            }
            try (MockedStatic<FactoryLocator> locator = mockStatic(FactoryLocator.class, CALLS_REAL_METHODS)) {
                locator.when(FactoryLocator::getUserFactory).thenReturn(factory);
                assertEquals(Authenticator.FAILURE, login(user, "wrong-" + limit));
            }
            concurrent[0].get(60, TimeUnit.SECONDS);
            assertEquals("the other failure is kept", 1, storedFailures(user));
        } finally {
            cleanUp(user);
        }
    }

    /**
     * Given: an account whose password has expired
     * When: it logs in with the right password
     * Then: the login succeeds and the account is flagged to choose a new password
     */
    @Test
    public void test_expired_password_login_flags_a_password_reset() throws Exception {
        final User user = newUser();
        try {
            expirePassword(user);

            assertEquals(Authenticator.SUCCESS, login(user, PASSWORD));
            assertTrue("the reset is required", toBoolean(storedRow(user).get("passwordreset")));
        } finally {
            cleanUp(user);
        }
    }

    /**
     * Given: an account whose password has expired and is stored in the older digest format
     * When: it logs in with the right password, so the password is hashed again and the user saved
     * Then: the account is still flagged to choose a new password
     */
    @Test
    public void test_password_reset_flag_survives_the_rehash_on_login() throws Exception {
        final User user = newUser();
        final String digest = PublicEncryptionFactory.digestString(PASSWORD);
        try {
            new DotConnect().executeUpdate("UPDATE user_ SET password_ = ? WHERE userid = ?",
                    digest, user.getUserId());
            expirePassword(user);

            assertEquals(Authenticator.SUCCESS, login(user, PASSWORD));
            final Map<String, Object> row = storedRow(user);
            assertNotEquals("the password was hashed again", digest, row.get("password_"));
            assertTrue("the reset is still required", toBoolean(row.get("passwordreset")));
        } finally {
            cleanUp(user);
        }
    }

    /**
     * Asserts that the user's row is locked by another transaction: a probe from a separate pooled connection that
     * asks for the row lock without waiting is refused with PostgreSQL's {@code lock_not_available} (55P03).
     */
    private static void assertRowLocked(final User user) throws SQLException {
        try (Connection probe = DbConnectionFactory.getDataSource().getConnection();
             PreparedStatement statement = probe.prepareStatement(
                     "SELECT userid FROM user_ WHERE userid = ? FOR UPDATE NOWAIT")) {
            statement.setString(1, user.getUserId().toLowerCase());
            statement.executeQuery().close();
            fail("the row is not locked while the count starts over");
        } catch (final SQLException e) {
            assertEquals("the row is locked while the count starts over", "55P03", e.getSQLState());
        }
    }

    /** Sets the user's password expiration date to yesterday and drops the cached copy. */
    private static void expirePassword(final User user) throws Exception {
        try {
            new DotConnect().executeUpdate("UPDATE user_ SET passwordexpirationdate = ? WHERE userid = ?",
                    new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)), user.getUserId());
            CacheLocator.getUserCache().remove(user.getUserId());
        } finally {
            DbConnectionFactory.closeSilently();
        }
    }

    /**
     * Runs one login with a wrong password for the user. When the login reaches its failed-login delay, the
     * {@code change} runs on another thread, as a separate request, and the login waits for it to commit before
     * going on. Fails if the login never reaches the delay.
     */
    private static void failedLoginWhile(final User user, final ThrowingRunnable change) throws Exception {
        final AtomicInteger delays = new AtomicInteger();
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class, CALLS_REAL_METHODS)) {
            security.when(() -> SecurityUtils.delayRequest(anyLong(), any())).thenAnswer(invocation -> {
                delays.incrementAndGet();
                otherRequest.submit(() -> {
                    try {
                        change.run();
                    } finally {
                        DbConnectionFactory.closeSilently();
                    }
                    return null;
                }).get(60, TimeUnit.SECONDS);
                return null;
            });
            assertEquals(Authenticator.FAILURE, login(user, "wrong-password"));
        }
        assertEquals("the failed login waited once", 1, delays.get());
    }

    private static User newUser() throws Exception {
        final User user = new UserDataGen().nextPersisted();
        APILocator.getUserAPI().updatePassword(user, PASSWORD, APILocator.systemUser(), false);
        return user;
    }

    /** One login through the main login flow, as one request that ends by closing its connection. */
    private static int login(final User user, final String password) throws Exception {
        try {
            return UserManagerUtil.authenticateByUserId(
                    PublicCompanyFactory.getDefaultCompany().getCompanyId(), user.getUserId(), password);
        } finally {
            DbConnectionFactory.closeSilently();
        }
    }

    /** Reads the user's row straight from the database, without going through the user cache. */
    private static Map<String, Object> storedRow(final User user) throws Exception {
        try {
            final List<Map<String, Object>> rows = new DotConnect()
                    .setSQL("SELECT active_, failedloginattempts, passwordreset, password_ FROM user_ WHERE userid = ?")
                    .addParam(user.getUserId())
                    .loadObjectResults();
            assertEquals(1, rows.size());
            return rows.get(0);
        } finally {
            DbConnectionFactory.closeSilently();
        }
    }

    private static int storedFailures(final User user) throws Exception {
        return ((Number) storedRow(user).get("failedloginattempts")).intValue();
    }

    private static boolean toBoolean(final Object value) {
        return value instanceof Boolean ? (Boolean) value : Boolean.parseBoolean(String.valueOf(value));
    }

    private static void cleanUp(final User user) {
        LoginLockout.getInstance().clear(user.getUserId());
        UserDataGen.remove(user);
    }

    /** A change made by another request; may throw. */
    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
