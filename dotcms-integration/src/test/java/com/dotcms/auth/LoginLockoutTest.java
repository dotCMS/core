package com.dotcms.auth;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.auth.providers.jwt.beans.ApiToken;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotcms.rest.api.v1.authentication.IncorrectPasswordException;
import com.dotcms.rest.api.v1.authentication.ResetPasswordTokenUtil;
import com.dotcms.rest.api.v1.user.UpdateCurrentUserForm;
import com.dotcms.rest.api.v1.user.UserResourceHelper;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.cms.login.factories.LoginFactory;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.startup.runonce.Task261104CreateLoginLockoutTable;
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
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration tests for {@link LoginLockout} through the real authentication paths: the main login flow, WebDAV,
 * the legacy {@link LoginFactory} logins, and the password reset and update flows. Logins come from a client
 * address set on the request thread-local, as the servlet container does for real requests.
 */
public class LoginLockoutTest {

    private static final String PASSWORD = "Lockout-Test-9!xQ-correct";
    private static final String NEW_PASSWORD = "Lockout-Test-7#zR-renewed";
    private static final int LIMIT = 5;
    // Documentation address ranges (RFC 5737), so they never collide with a real client.
    private static final String ADDRESS = "203.0.113.10";
    private static final String OTHER_ADDRESS = "198.51.100.20";
    private static final String THIRD_ADDRESS = "192.0.2.30";
    private static Object delayEnabledBefore;
    private static String basicAuthBefore;

    /** Enables password compatibility only for these password-lockout tests. */
    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        delayEnabledBefore = Config.getStringProperty(WebKeys.AUTH_FAILED_ATTEMPTS_DELAY_STRATEGY_ENABLED, "true");
        basicAuthBefore = Config.getStringProperty("ALLOW_BASIC_AUTH", null);
        Config.setProperty("ALLOW_BASIC_AUTH", "ALL");
        // The per-attempt sleep is not under test and would make each case take tens of seconds.
        Config.setProperty(WebKeys.AUTH_FAILED_ATTEMPTS_DELAY_STRATEGY_ENABLED, false);
    }

    /** Restores authentication policy and delays for the remaining suite. */
    @AfterClass
    public static void restore() {
        Config.setProperty("ALLOW_BASIC_AUTH", basicAuthBefore);
        Config.setProperty(WebKeys.AUTH_FAILED_ATTEMPTS_DELAY_STRATEGY_ENABLED, delayEnabledBefore);
    }

    @Before
    public void fromDefaultAddress() {
        from(ADDRESS);
    }

    @After
    public void closeConnection() {
        DbConnectionFactory.closeSilently();
        HttpServletRequestThreadLocal.INSTANCE.setRequest(null);
    }

    /**
     * Makes the following logins come from the given client address.
     */
    private static void from(final String address) {
        HttpServletRequestThreadLocal.INSTANCE.setRequest(requestFrom(address));
    }

    private static HttpServletRequest requestFrom(final String address) {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn(address);
        return request;
    }

    private static User newUser() throws Exception {
        final User user = new UserDataGen().nextPersisted();
        APILocator.getUserAPI().updatePassword(user, PASSWORD, APILocator.systemUser(), false);
        return user;
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

    private static boolean webdavLogin(final User user, final String password) throws Exception {
        try {
            return null != new DotWebdavHelper().authorizePrincipal(loginName(user), password);
        } catch (final DotSecurityException e) {
            return false;
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

    private static void failLogins(final User user, final int count) throws Exception {
        for (int i = 0; i < count; i++) {
            assertEquals(Authenticator.FAILURE, login(user, "wrong-" + i));
        }
    }

    /**
     * Locks the current address out of the account through the main login flow.
     */
    private static void lockByFailedLogins(final User user) throws Exception {
        failLogins(user, LIMIT);
        assertTrue(LoginLockout.getInstance().isLocked(user.getUserId(), LoginLockout.clientAddress()));
    }

    private static boolean lockedFrom(final User user, final String address) {
        return LoginLockout.getInstance().isLocked(user.getUserId(), address);
    }

    private static String webdav(final String address) {
        return LoginLockout.WEBDAV_SOURCE_PREFIX + address;
    }

    /**
     * Runs the task on many threads at once, each as a request from the given address, and returns the results.
     */
    private static <T> List<T> inParallel(final int threads, final String address, final Callable<T> task)
            throws Exception {
        final ExecutorService pool = Executors.newFixedThreadPool(threads);
        final CountDownLatch start = new CountDownLatch(1);
        try {
            final List<Future<T>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    from(address);
                    start.await();
                    try {
                        return task.call();
                    } finally {
                        DbConnectionFactory.closeSilently();
                        HttpServletRequestThreadLocal.INSTANCE.setRequest(null);
                    }
                }));
            }
            start.countDown();
            final List<T> results = new ArrayList<>();
            for (final Future<T> future : futures) {
                results.add(future.get(2, TimeUnit.MINUTES));
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }

    /**
     * Same as {@link #inParallel}, but returns how long each thread's task took in milliseconds instead of its
     * result. Used to show that concurrent failed logins do not all take the same (minimum) delay.
     */
    private static List<Long> timedInParallel(final int threads, final String address, final Callable<?> task)
            throws Exception {
        final ExecutorService pool = Executors.newFixedThreadPool(threads);
        final CountDownLatch start = new CountDownLatch(1);
        try {
            final List<Future<Long>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    from(address);
                    start.await();
                    final long began = System.currentTimeMillis();
                    try {
                        task.call();
                        return System.currentTimeMillis() - began;
                    } finally {
                        DbConnectionFactory.closeSilently();
                        HttpServletRequestThreadLocal.INSTANCE.setRequest(null);
                    }
                }));
            }
            start.countDown();
            final List<Long> elapsed = new ArrayList<>();
            for (final Future<Long> future : futures) {
                elapsed.add(future.get(2, TimeUnit.MINUTES));
            }
            return elapsed;
        } finally {
            pool.shutdownNow();
        }
    }

    private static int failedLoginAttempts(final User user) throws Exception {
        return APILocator.getUserAPI().loadUserById(user.getUserId()).getFailedLoginAttempts();
    }

    /**
     * @return every lockout row of the account, keyed by source
     */
    private static Map<String, Map<String, Object>> lockoutRows(final User user) throws Exception {
        try {
            final List<Map<String, Object>> rows = new DotConnect()
                    .setSQL("SELECT source, failures, window_start, locked_until FROM login_lockout WHERE user_id = ?")
                    .addParam(user.getUserId())
                    .loadObjectResults();
            final Map<String, Map<String, Object>> bySource = new HashMap<>();
            rows.forEach(row -> bySource.put((String) row.get("source"), row));
            return bySource;
        } finally {
            DbConnectionFactory.closeSilently();
        }
    }

    private static long lockedUntil(final User user, final String source) throws Exception {
        final Map<String, Object> row = lockoutRows(user).get(source);
        assertNotNull("no lockout row for " + source, row);
        return ((Number) row.get("locked_until")).longValue();
    }

    /**
     * @return the database's current time, which the shared lockout instance measures locks against
     */
    private static long databaseNow() throws Exception {
        try {
            return LoginLockout.DatabaseClock.readDatabaseTime();
        } finally {
            DbConnectionFactory.closeSilently();
        }
    }

    /**
     * Makes every lock on the account run out now, as if the lock length had passed.
     */
    private static void expireLocks(final User user) throws Exception {
        try {
            new DotConnect().setSQL("UPDATE login_lockout SET locked_until = ? WHERE user_id = ? AND locked_until > 0")
                    .addParam(System.currentTimeMillis() - 1000L).addParam(user.getUserId()).loadResult();
        } finally {
            DbConnectionFactory.closeSilently();
        }
    }

    private static void cleanUp(final User user) {
        LoginLockout.getInstance().clear(user.getUserId());
        UserDataGen.remove(user);
    }

    /**
     * Given: an account and the default limit of 5 failures per address
     * When: 5 wrong passwords are tried from one address
     * Then: that address is locked out, and the correct password is refused from it with the same result as a wrong one
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
     * Given: an address locked out of an account by failed logins
     * When: the owner logs in from another address, on every login path
     * Then: the owner gets in, and the locked address is still refused
     */
    @Test
    public void test_lock_applies_only_to_the_failing_address() throws Exception {
        final User user = newUser();
        try {
            lockByFailedLogins(user);

            from(OTHER_ADDRESS);
            assertFalse(lockedFrom(user, OTHER_ADDRESS));
            assertEquals(Authenticator.SUCCESS, login(user, PASSWORD));
            assertTrue(webdavLogin(user, PASSWORD));
            assertTrue(legacyLogin(user, PASSWORD));

            from(ADDRESS);
            assertEquals(Authenticator.FAILURE, login(user, PASSWORD));
            assertFalse(legacyLogin(user, PASSWORD));
            // WebDAV is counted on its own, so a lock from backend logins does not reach it
            assertTrue(webdavLogin(user, PASSWORD));
        } finally {
            cleanUp(user);
        }
    }

    /**
     * Given: failures from two addresses, each below the per-address limit
     * When: one address reaches the limit
     * Then: only that address is locked; the other keeps its own count and can still log in
     */
    @Test
    public void test_each_address_counts_separately() throws Exception {
        final User user = newUser();
        try {
            failLogins(user, LIMIT - 1);
            from(OTHER_ADDRESS);
            failLogins(user, LIMIT - 1);
            assertFalse(lockedFrom(user, ADDRESS));
            assertFalse(lockedFrom(user, OTHER_ADDRESS));

            from(ADDRESS);
            failLogins(user, 1);
            assertTrue(lockedFrom(user, ADDRESS));
            assertFalse(lockedFrom(user, OTHER_ADDRESS));

            from(OTHER_ADDRESS);
            assertEquals(Authenticator.SUCCESS, login(user, PASSWORD));
        } finally {
            cleanUp(user);
        }
    }

    /**
     * Given: the default configuration, where the account-wide limit is off
     * When: two addresses each use up their attempts
     * Then: only those addresses are locked out; nothing is counted account-wide, and an address that never
     *       failed still gets in
     */
    @Test
    public void test_account_wide_limit_is_off_by_default() throws Exception {
        assertEquals(0, LoginLockout.accountMaxFailures());
        final User user = newUser();
        try {
            failLogins(user, LIMIT);
            from(OTHER_ADDRESS);
            failLogins(user, LIMIT);
            assertTrue(lockedFrom(user, ADDRESS));
            assertTrue(lockedFrom(user, OTHER_ADDRESS));
            assertFalse(lockoutRows(user).containsKey(LoginLockout.ACCOUNT_WIDE));

            from(THIRD_ADDRESS);
            assertEquals(Authenticator.SUCCESS, login(user, PASSWORD));
        } finally {
            cleanUp(user);
        }
    }

    /**
     * Given: an account-wide limit of 8 and a per-address limit of 5
     * When: two addresses each fail 4 times, so no single address reaches its limit
     * Then: the account is locked for every address, including one that never failed, until a new password is set
     */
    @Test
    public void test_account_wide_limit_locks_every_address() throws Exception {
        final int accountLimitBefore = LoginLockout.accountMaxFailures();
        Config.setProperty(LoginLockout.LOCKOUT_ACCOUNT_MAX_FAILURES, 8);
        final User user = newUser();
        try {
            failLogins(user, 4);
            from(OTHER_ADDRESS);
            failLogins(user, 4);

            assertTrue(LoginLockout.getInstance().isLocked(user.getUserId()));
            assertTrue(lockedFrom(user, THIRD_ADDRESS));
            assertTrue(lockedUntil(user, LoginLockout.ACCOUNT_WIDE) > System.currentTimeMillis());

            from(THIRD_ADDRESS);
            assertEquals(Authenticator.FAILURE, login(user, PASSWORD));
            assertFalse(webdavLogin(user, PASSWORD));

            APILocator.getUserAPI().updatePassword(APILocator.getUserAPI().loadUserById(user.getUserId()),
                    NEW_PASSWORD, APILocator.systemUser(), false);
            assertTrue(lockoutRows(user).isEmpty());
            assertEquals(Authenticator.SUCCESS, login(user, NEW_PASSWORD));
        } finally {
            Config.setProperty(LoginLockout.LOCKOUT_ACCOUNT_MAX_FAILURES, accountLimitBefore);
            cleanUp(user);
        }
    }

    /**
     * Given: an address that has been locked out once and whose lock has run out
     * When: it fails once, and then reaches the limit again
     * Then: one failure does not relock it, and the second lock is the same fixed length as the first,
     *       both measured by the database clock
     */
    @Test
    public void test_lock_length_is_fixed_and_count_starts_over() throws Exception {
        final User user = newUser();
        final long expectedMillis = LoginLockout.getInstance().lockSeconds() * 1000L;
        try {
            long before = databaseNow();
            lockByFailedLogins(user);
            long length = lockedUntil(user, ADDRESS) - before;
            assertTrue("first lock " + length + "ms", length >= expectedMillis && length < expectedMillis + 60_000L);

            expireLocks(user);
            failLogins(user, 1);
            assertFalse(lockedFrom(user, ADDRESS));

            failLogins(user, LIMIT - 1);
            before = databaseNow();
            assertTrue(lockedFrom(user, ADDRESS));
            length = lockedUntil(user, ADDRESS) - before;
            assertTrue("second lock " + length + "ms", length <= expectedMillis && length > expectedMillis - 60_000L);
        } finally {
            cleanUp(user);
        }
    }

    /**
     * Given: a locked address
     * When: further logins are tried from it, with right and wrong passwords, on every login path
     * Then: they fail without moving any failure count or extending the lock
     */
    @Test
    public void test_failure_counts_do_not_move_while_locked() throws Exception {
        final User user = newUser();
        try {
            lockByFailedLogins(user);
            assertEquals(0, failedLoginAttempts(user));
            final Map<String, Map<String, Object>> rowsAtLock = lockoutRows(user);

            assertEquals(Authenticator.FAILURE, login(user, "wrong-while-locked"));
            assertEquals(Authenticator.FAILURE, login(user, PASSWORD));
            assertFalse(legacyLogin(user, "wrong-while-locked"));

            assertEquals(0, failedLoginAttempts(user));
            assertEquals(rowsAtLock, lockoutRows(user));
        } finally {
            cleanUp(user);
        }
    }

    /**
     * Given: a locked address
     * When: the account's lockout state is cleared directly
     * Then: the correct password works again from that address and leaves nothing recorded for it
     */
    @Test
    public void test_clear_unlocks_and_success_resets_state() throws Exception {
        final User user = newUser();
        try {
            lockByFailedLogins(user);
            LoginLockout.getInstance().clear(user.getUserId());
            assertEquals(Authenticator.SUCCESS, login(user, PASSWORD));
            assertEquals(LoginLockout.Status.NONE, LoginLockout.getInstance().status(user.getUserId(), ADDRESS));
        } finally {
            cleanUp(user);
        }
    }

    /**
     * Given: failures recorded for an address, first below the limit and then as a lock that has run out
     * When: the correct password is used from that address
     * Then: the check reports the recorded failures, the login succeeds, and that address's count is removed
     */
    @Test
    public void test_success_clears_the_count_for_its_address() throws Exception {
        final User user = newUser();
        try {
            assertEquals(LoginLockout.Status.NONE, LoginLockout.getInstance().check(user.getUserId(), ADDRESS));
            failLogins(user, 2);
            assertEquals(LoginLockout.Status.RECORDED, LoginLockout.getInstance().check(user.getUserId(), ADDRESS));
            assertEquals(Authenticator.SUCCESS, login(user, PASSWORD));
            assertEquals(LoginLockout.Status.NONE, LoginLockout.getInstance().status(user.getUserId(), ADDRESS));

            lockByFailedLogins(user);
            assertEquals(LoginLockout.Status.LOCKED, LoginLockout.getInstance().check(user.getUserId(), ADDRESS));
            expireLocks(user);
            assertEquals(LoginLockout.Status.RECORDED, LoginLockout.getInstance().check(user.getUserId(), ADDRESS));
            assertEquals(Authenticator.SUCCESS, login(user, PASSWORD));
            assertEquals(LoginLockout.Status.NONE, LoginLockout.getInstance().status(user.getUserId(), ADDRESS));
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
            failLogins(user, LIMIT - 1);
            assertFalse(LoginLockout.getInstance().isLocked(user.getUserId()));
            assertEquals(Authenticator.SUCCESS, login(user, PASSWORD));
        } finally {
            cleanUp(user);
        }
    }

    /**
     * Given: an account
     * When: the limit of wrong passwords is sent over WebDAV from one address, as a client retrying a stale
     *       password does
     * Then: WebDAV from that address is locked out and refuses even the correct password, without counting
     *       further attempts; backend logins from the same address still work; clearing the lock lets WebDAV in
     */
    @Test
    public void test_webdav_failures_lock_only_webdav() throws Exception {
        final User user = newUser();
        try {
            assertTrue(webdavLogin(user, PASSWORD));
            for (int i = 0; i < LIMIT; i++) {
                assertFalse(webdavLogin(user, "wrong-" + i));
            }
            assertTrue(lockedFrom(user, webdav(ADDRESS)));
            assertFalse(lockedFrom(user, ADDRESS));

            final Map<String, Map<String, Object>> rowsAtLock = lockoutRows(user);
            assertThrows(DotSecurityException.class,
                    () -> new DotWebdavHelper().authorizePrincipal(loginName(user), PASSWORD));
            DbConnectionFactory.closeSilently();
            assertFalse(webdavLogin(user, "wrong-while-locked"));
            assertEquals(rowsAtLock, lockoutRows(user));

            assertEquals(Authenticator.SUCCESS, login(user, PASSWORD));
            assertTrue(legacyLogin(user, PASSWORD));

            LoginLockout.getInstance().clear(user.getUserId());
            assertTrue(webdavLogin(user, PASSWORD));
        } finally {
            cleanUp(user);
        }
    }

    /**
     * Given: WebDAV from an address locked out of an account
     * When: one of the user's API tokens is sent in the WebDAV password field
     * Then: it is accepted, since a token is not a password, and nothing is counted for it
     */
    @Test
    public void test_webdav_token_accepted_while_locked() throws Exception {
        final User user = newUser();
        try {
            final ApiToken token = APILocator.getApiTokenAPI().persistApiToken(ApiToken.builder()
                    .withIssuer(ClusterFactory.getClusterId())
                    .withExpires(Date.from(Instant.now().plus(Duration.ofDays(1))))
                    .withUserId(user.getUserId())
                    .withRequestingUserId(APILocator.systemUser().getUserId())
                    .withRequestingIp(ADDRESS)
                    .build(), APILocator.systemUser());
            final String jwt = APILocator.getApiTokenAPI().getJWT(token, APILocator.systemUser());
            DbConnectionFactory.closeSilently();

            assertTrue(webdavLogin(user, jwt));
            for (int i = 0; i < LIMIT; i++) {
                assertFalse(webdavLogin(user, "wrong-" + i));
            }
            assertTrue(lockedFrom(user, webdav(ADDRESS)));
            final Map<String, Map<String, Object>> rowsAtLock = lockoutRows(user);

            assertTrue(webdavLogin(user, jwt));
            assertFalse(webdavLogin(user, PASSWORD));
            assertEquals(rowsAtLock, lockoutRows(user));
        } finally {
            cleanUp(user);
        }
    }

    /**
     * Given: an address with no failures
     * When: 20 login attempts from it start at the same moment, before any password is checked
     * Then: exactly the limit of them may check a password, because each is counted before its check. The
     *       rest are refused for arriving together, which is not reported as a lock until a wrong password
     *       actually sets one
     */
    @Test
    public void test_attempts_are_counted_before_the_password_check() throws Exception {
        final User user = newUser();
        try {
            final List<Boolean> refused = inParallel(20, ADDRESS,
                    () -> LoginLockout.getInstance().begin(user.getUserId(), ADDRESS).isRefused());
            assertEquals(LIMIT, refused.stream().filter(r -> !r).count());

            final LoginLockout.Attempt overLimit = LoginLockout.getInstance().begin(user.getUserId(), ADDRESS);
            assertTrue(overLimit.isRefused());
            assertFalse(overLimit.isLockedOut());
            LoginLockout.getInstance().failed(overLimit);
            assertTrue(LoginLockout.getInstance().begin(user.getUserId(), ADDRESS).isLockedOut());
        } finally {
            cleanUp(user);
        }
    }

    /**
     * Given: an account
     * When: 12 correct-password logins arrive from one address at the same moment, more than the per-address
     *       limit, on the main login flow
     * Then: some may be refused for arriving together, but none sets a lock, and the next login gets in
     */
    @Test
    public void test_parallel_correct_logins_do_not_lock() throws Exception {
        final User user = newUser();
        try {
            final List<Integer> results = inParallel(12, ADDRESS, () -> login(user, PASSWORD));
            assertTrue(results.contains(Authenticator.SUCCESS));
            assertFalse(LoginLockout.getInstance().isLocked(user.getUserId()));
            assertEquals(Authenticator.SUCCESS, login(user, PASSWORD));
        } finally {
            cleanUp(user);
        }
    }

    /**
     * Given: an account
     * When: 12 correct-password logins arrive from one address at the same moment through the legacy login, the
     *       same check WebDAV and the REST current-password check use
     * Then: none sets a lock, and the next login gets in
     */
    @Test
    public void test_parallel_correct_legacy_logins_do_not_lock() throws Exception {
        final User user = newUser();
        try {
            final List<Boolean> results = inParallel(12, ADDRESS, () -> legacyLogin(user, PASSWORD));
            assertTrue(results.contains(true));
            assertFalse(LoginLockout.getInstance().isLocked(user.getUserId()));
            assertTrue(legacyLogin(user, PASSWORD));
        } finally {
            cleanUp(user);
        }
    }

    /**
     * Given: an account
     * When: 10 wrong passwords arrive from one address at the same moment, on the main login flow
     * Then: all fail and the address ends up locked out
     */
    @Test
    public void test_concurrent_wrong_logins_lock_the_address() throws Exception {
        final User user = newUser();
        try {
            final List<Integer> results = inParallel(10, ADDRESS, () -> login(user, "wrong-in-parallel"));
            assertTrue(results.stream().allMatch(r -> r == Authenticator.FAILURE));
            assertTrue(lockedFrom(user, ADDRESS));
            assertEquals(Authenticator.FAILURE, login(user, PASSWORD));
        } finally {
            cleanUp(user);
        }
    }

    /**
     * Given: an account whose stored failed-login count is far above the limit, as dormant accounts can carry
     *        from before this change, and a delay of one millisecond per counted failure
     * When: one wrong password is tried, and the upgrade task runs
     * Then: the wrong password does not lock the account and is delayed only as for a count below the limit,
     *       and the task leaves the stored count alone (the migration does not touch {@code user_})
     */
    @Test
    public void test_counter_above_limit_from_before_upgrade() throws Exception {
        final User user = newUser();
        final Object strategyBefore = Config.getStringProperty(WebKeys.AUTH_FAILED_ATTEMPTS_DELAY_STRATEGY, "pow");
        try {
            final User counted = APILocator.getUserAPI().loadUserById(user.getUserId());
            counted.setFailedLoginAttempts(10_000);
            APILocator.getUserAPI().save(counted, APILocator.systemUser(), false);

            // Uncapped, this failure would sleep for 10 seconds
            Config.setProperty(WebKeys.AUTH_FAILED_ATTEMPTS_DELAY_STRATEGY_ENABLED, true);
            Config.setProperty(WebKeys.AUTH_FAILED_ATTEMPTS_DELAY_STRATEGY, "time_mills");
            final long start = System.currentTimeMillis();
            assertEquals(Authenticator.FAILURE, login(user, "wrong-after-upgrade"));
            final long elapsed = System.currentTimeMillis() - start;
            Config.setProperty(WebKeys.AUTH_FAILED_ATTEMPTS_DELAY_STRATEGY_ENABLED, false);
            assertTrue("failed login took " + elapsed + "ms", elapsed < 5_000L);

            assertFalse(LoginLockout.getInstance().isLocked(user.getUserId()));
            assertEquals(Authenticator.SUCCESS, login(user, PASSWORD));

            final User countedAgain = APILocator.getUserAPI().loadUserById(user.getUserId());
            countedAgain.setFailedLoginAttempts(50);
            APILocator.getUserAPI().save(countedAgain, APILocator.systemUser(), false);
            new Task261104CreateLoginLockoutTable().executeUpgrade();
            DbConnectionFactory.closeSilently();
            CacheLocator.getUserCache().remove(user.getUserId());
            assertEquals(50, failedLoginAttempts(user));
            assertTrue(lockoutRows(user).isEmpty());
            assertEquals(Authenticator.SUCCESS, login(user, PASSWORD));
        } finally {
            Config.setProperty(WebKeys.AUTH_FAILED_ATTEMPTS_DELAY_STRATEGY_ENABLED, false);
            Config.setProperty(WebKeys.AUTH_FAILED_ATTEMPTS_DELAY_STRATEGY, strategyBefore);
            cleanUp(user);
        }
    }

    /**
     * Given: an address locked out of an account
     * When: the correct password is sent from it to the legacy {@link LoginFactory} logins used by the ajax endpoints
     * Then: both overloads refuse it, and accept it again once the lock is cleared
     */
    @Test
    public void test_legacy_doLogin_refuses_locked_address() throws Exception {
        final User user = newUser();
        try {
            assertTrue(legacyLogin(user, PASSWORD));
            lockByFailedLogins(user);

            assertFalse(legacyLogin(user, PASSWORD));
            assertFalse(LoginFactory.doLogin(loginName(user), PASSWORD, false, requestFrom(ADDRESS),
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
     * When: the limit of wrong passwords is sent to the legacy {@link LoginFactory} login from one address
     * Then: that address is locked out
     */
    @Test
    public void test_legacy_doLogin_failures_count_toward_lock() throws Exception {
        final User user = newUser();
        try {
            for (int i = 0; i < LIMIT; i++) {
                assertFalse(legacyLogin(user, "wrong-" + i));
            }
            assertTrue(lockedFrom(user, ADDRESS));
        } finally {
            cleanUp(user);
        }
    }

    /**
     * Given: a locked address
     * When: the user resets the password with an emailed reset token
     * Then: all lockout state for the account is cleared and the new password works
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

            assertTrue(lockoutRows(user).isEmpty());
            assertEquals(Authenticator.SUCCESS, login(user, NEW_PASSWORD));
        } finally {
            Config.setProperty("RECOVER_PASSWORD_TOKEN_AUTH_DELAY", tokenDelayBefore);
            cleanUp(user);
        }
    }

    /**
     * Given: a locked address
     * When: an administrator sets a new password through {@code UserAPI.updatePassword}
     * Then: the lockout state and the failure count are cleared and the new password works
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

            assertTrue(lockoutRows(user).isEmpty());
            assertEquals(0, failedLoginAttempts(user));
            assertEquals(Authenticator.SUCCESS, login(user, NEW_PASSWORD));
        } finally {
            cleanUp(user);
        }
    }

    /**
     * Given: a locked address
     * When: an administrator saves the user with a new password, as the Users portlet and the REST user update do
     * Then: the lockout state is cleared and the new password works
     */
    @Test
    public void test_admin_save_with_new_password_clears_lock() throws Exception {
        final User user = newUser();
        try {
            lockByFailedLogins(user);

            final User toSave = APILocator.getUserAPI().loadUserById(user.getUserId());
            toSave.setPassword(NEW_PASSWORD);
            APILocator.getUserAPI().save(toSave, APILocator.systemUser(), true, false);

            assertTrue(lockoutRows(user).isEmpty());
            assertEquals(Authenticator.SUCCESS, login(user, NEW_PASSWORD));
        } finally {
            cleanUp(user);
        }
    }

    /**
     * Given: a user with lockout rows
     * When: the user is deleted
     * Then: the rows are deleted with them
     */
    @Test
    public void test_user_delete_removes_lockout_rows() throws Exception {
        final User user = newUser();
        final User replacement = new UserDataGen().nextPersisted();
        try {
            lockByFailedLogins(user);
            assertFalse(lockoutRows(user).isEmpty());

            APILocator.getUserAPI().delete(user, replacement, APILocator.systemUser(), false);
            DbConnectionFactory.closeSilently();

            assertTrue(lockoutRows(user).isEmpty());
        } finally {
            LoginLockout.getInstance().clear(user.getUserId());
            UserDataGen.remove(replacement);
        }
    }

    /**
     * Given: failures recorded for one account long ago, never locked and never cleared by a success
     * When: any lock is set
     * Then: those expired rows are removed, so failing addresses do not stay in the table for good
     */
    @Test
    public void test_expired_rows_are_purged_when_a_lock_is_set() throws Exception {
        final User stale = newUser();
        final User user = newUser();
        try {
            failLogins(stale, 2);
            assertFalse(lockoutRows(stale).isEmpty());
            try {
                new DotConnect().setSQL("UPDATE login_lockout SET window_start = ? WHERE user_id = ?")
                        .addParam(System.currentTimeMillis() - 2 * LoginLockout.getInstance().windowSeconds() * 1000L)
                        .addParam(stale.getUserId()).loadResult();
            } finally {
                DbConnectionFactory.closeSilently();
            }

            // A fresh instance, since each one purges at most once a minute
            final LoginLockout lockout = new LoginLockout(System::currentTimeMillis);
            for (int i = 0; i < LIMIT; i++) {
                lockout.failed(lockout.begin(user.getUserId(), ADDRESS));
            }
            assertTrue(lockedFrom(user, ADDRESS));

            assertTrue(lockoutRows(stale).isEmpty());
            assertFalse(lockoutRows(user).isEmpty());
        } finally {
            cleanUp(stale);
            cleanUp(user);
        }
    }

    /**
     * Given: failures from one address below the limit
     * When: the counting window passes with no lock ever set, and the address fails again
     * Then: the old failures no longer count, so the lock comes only at the limit within the new window
     */
    @Test
    public void test_count_ages_out_of_the_window_without_a_lock() throws Exception {
        final User user = newUser();
        final AtomicLong now = new AtomicLong(System.currentTimeMillis());
        final LoginLockout lockout = new LoginLockout(now::get);
        try {
            for (int i = 0; i < LIMIT - 1; i++) {
                lockout.failed(lockout.begin(user.getUserId(), ADDRESS));
            }
            assertFalse(lockout.isLocked(user.getUserId(), ADDRESS));

            now.addAndGet((lockout.windowSeconds() + 1) * 1000L);
            for (int i = 0; i < LIMIT - 1; i++) {
                lockout.failed(lockout.begin(user.getUserId(), ADDRESS));
            }
            assertFalse(lockout.isLocked(user.getUserId(), ADDRESS));

            lockout.failed(lockout.begin(user.getUserId(), ADDRESS));
            assertTrue(lockout.isLocked(user.getUserId(), ADDRESS));
        } finally {
            cleanUp(user);
        }
    }

    /**
     * Given: an attempt that started before parallel failures from the same address locked it
     * When: that attempt's password turns out to be right
     * Then: its success does not remove the lock the others set
     */
    @Test
    public void test_lock_set_during_an_attempt_survives_its_success() throws Exception {
        final User user = newUser();
        final LoginLockout lockout = LoginLockout.getInstance();
        try {
            final LoginLockout.Attempt inFlight = lockout.begin(user.getUserId(), ADDRESS);
            assertFalse(inFlight.isRefused());
            for (int i = 0; i < LIMIT - 1; i++) {
                lockout.failed(lockout.begin(user.getUserId(), ADDRESS));
            }
            assertTrue(lockedFrom(user, ADDRESS));

            lockout.succeeded(inFlight);
            assertTrue(lockedFrom(user, ADDRESS));
        } finally {
            cleanUp(user);
        }
    }

    /**
     * Given: a locked address, and an attempt whose lock check ran before the lock was set
     * When: the attempt is counted
     * Then: the count sees the lock and the attempt is refused
     */
    @Test
    public void test_attempt_counted_after_a_lock_is_refused() throws Exception {
        final User user = newUser();
        try {
            lockByFailedLogins(user);
            // The first reading of the clock (the lock check) falls after the lock has run out, as if the check
            // had happened before the lock was set; every later reading is the real time.
            final Deque<Long> readings = new ArrayDeque<>(
                    List.of(System.currentTimeMillis() + (LoginLockout.getInstance().lockSeconds() + 60) * 1000L));
            final LongSupplier clock = () -> readings.isEmpty() ? System.currentTimeMillis() : readings.poll();
            final LoginLockout.Attempt attempt = new LoginLockout(clock).begin(user.getUserId(), ADDRESS);

            assertTrue(attempt.isRefused());
            assertTrue(lockedFrom(user, ADDRESS));
        } finally {
            cleanUp(user);
        }
    }

    /**
     * Given: an account
     * When: wrong passwords come from different addresses in one IPv6 /64
     * Then: they share one count, so the /64 is locked out while another network still gets in
     */
    @Test
    public void test_ipv6_network_is_locked_as_one_source() throws Exception {
        final User user = newUser();
        try {
            for (int i = 0; i < LIMIT; i++) {
                from("2001:db8:5:6::" + Integer.toHexString(i + 1));
                assertEquals(Authenticator.FAILURE, login(user, "wrong-" + i));
            }
            from("2001:db8:5:6::ffff");
            assertEquals(Authenticator.FAILURE, login(user, PASSWORD));
            from("2001:db8:5:7::1");
            assertEquals(Authenticator.SUCCESS, login(user, PASSWORD));
        } finally {
            cleanUp(user);
        }
    }

    /**
     * Given: a signed-in user changing their own details, which asks for the current password
     * When: the wrong current password is sent up to the limit from one address
     * Then: that address is locked out, and the correct current password is refused there too
     */
    @Test
    public void test_current_password_check_counts_and_refuses_while_locked() throws Exception {
        final User user = newUser();
        try {
            final HttpServletRequest request = requestFrom(ADDRESS);
            when(request.getSession()).thenReturn(mock(HttpSession.class));
            for (int i = 0; i < LIMIT; i++) {
                final UpdateCurrentUserForm form = new UpdateCurrentUserForm.Builder()
                        .userId(user.getUserId()).givenName(user.getFirstName()).surname(user.getLastName())
                        .email(user.getEmailAddress()).currentPassword("wrong-" + i).build();
                assertThrows(IncorrectPasswordException.class,
                        () -> UserResourceHelper.getInstance().updateUser(form, user, request, Locale.US));
                DbConnectionFactory.closeSilently();
            }
            assertTrue(lockedFrom(user, ADDRESS));

            final UpdateCurrentUserForm correct = new UpdateCurrentUserForm.Builder()
                    .userId(user.getUserId()).givenName(user.getFirstName()).surname(user.getLastName())
                    .email(user.getEmailAddress()).currentPassword(PASSWORD).build();
            assertThrows(IncorrectPasswordException.class,
                    () -> UserResourceHelper.getInstance().updateUser(correct, user, request, Locale.US));
            DbConnectionFactory.closeSilently();
            assertEquals(Authenticator.FAILURE, login(user, PASSWORD));
        } finally {
            cleanUp(user);
        }
    }

    /**
     * Given: lockout switched off with {@code AUTH_LOCKOUT_ENABLED=false}
     * When: more wrong passwords than the limit are tried, on the main login and over WebDAV
     * Then: nothing is recorded or locked and the correct password still works on every path
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
            assertTrue(lockoutRows(user).isEmpty());
            assertTrue(webdavLogin(user, PASSWORD));
            assertTrue(legacyLogin(user, PASSWORD));
            assertEquals(Authenticator.SUCCESS, login(user, PASSWORD));
        } finally {
            Config.setProperty(LoginLockout.LOCKOUT_ENABLED, enabledBefore);
            cleanUp(user);
        }
    }

    /**
     * Given: two different accounts, each failing logins from the same address
     * When: the source's aggregate failure count is read
     * Then: it reflects the combined total across both accounts, not just one
     *
     * Covers failures spread across many different accounts from one address building a rising total here,
     * even though each account's own count starts over at 1.
     */
    @Test
    public void test_source_failures_aggregates_across_accounts() throws Exception {
        final User first = newUser();
        final User second = newUser();
        try {
            failLogins(first, 2);
            failLogins(second, 3);
            assertEquals(5, LoginLockout.getInstance().sourceFailures(ADDRESS));
        } finally {
            cleanUp(first);
            cleanUp(second);
        }
    }

    /**
     * Given: one account that has already failed several times from an address
     * When: a second, fresh account fails once from the same address
     * Then: its pacing seed reflects the source's accumulated history, not just its own fresh count of 1
     *
     * Without this, failures spread across many different accounts from one address never see the delay
     * escalate, because each account's own attempt count resets to 1.
     */
    @Test
    public void test_pacing_seed_escalates_across_different_accounts_from_one_source() throws Exception {
        final User first = newUser();
        final User second = newUser();
        try {
            failLogins(first, 3);
            final LoginLockout.Attempt secondAttempt = LoginLockout.getInstance().begin(second.getUserId(), ADDRESS);
            final int seed = LoginLockout.getInstance().pacingSeed(secondAttempt);
            assertTrue("pacing seed " + seed + " should exceed the fresh account's own count of "
                    + secondAttempt.number(), seed > secondAttempt.number());
        } finally {
            cleanUp(first);
            cleanUp(second);
        }
    }

    /**
     * Given: an account, with the default ("pow") delay strategy enabled
     * When: 3 wrong passwords arrive from one address at the same moment
     * Then: they do not all take the same (minimum) delay — each is paced by its own atomically-counted attempt
     *       number, which {@link #begin} already assigns before any password is checked, rather than by a
     *       per-node field a race can leave identical, or unset, for all of them
     */
    @Test
    public void test_concurrent_wrong_logins_get_escalating_delays() throws Exception {
        final Object strategyBefore = Config.getStringProperty(WebKeys.AUTH_FAILED_ATTEMPTS_DELAY_STRATEGY, "pow");
        Config.setProperty(WebKeys.AUTH_FAILED_ATTEMPTS_DELAY_STRATEGY_ENABLED, true);
        Config.setProperty(WebKeys.AUTH_FAILED_ATTEMPTS_DELAY_STRATEGY, "pow");
        final User user = newUser();
        try {
            final List<Long> elapsed = timedInParallel(3, ADDRESS, () -> login(user, "wrong-in-parallel"));
            final long max = elapsed.stream().mapToLong(Long::longValue).max().orElseThrow();
            // Today, every concurrent attempt reads the same fresh (unset) count and sleeps for POW(0) = 0s, so
            // the slowest of the three finishes in well under a second. Once paced by each attempt's own
            // atomically-counted position (1, 2, 3), the slowest sleeps POW(3) = 9s. A relative spread between
            // threads is not used here, since thread-pool and connection jitter alone can produce one: this bound
            // is only clearable by an attempt actually having slept for several seconds.
            assertTrue("slowest of " + elapsed + "ms should reflect a real escalating sleep, not jitter alone",
                    max >= 3_000L);
        } finally {
            Config.setProperty(WebKeys.AUTH_FAILED_ATTEMPTS_DELAY_STRATEGY_ENABLED, false);
            Config.setProperty(WebKeys.AUTH_FAILED_ATTEMPTS_DELAY_STRATEGY, strategyBefore);
            cleanUp(user);
        }
    }

    /**
     * Given: lockout switched off with {@code AUTH_LOCKOUT_ENABLED=false}
     * When: an attempt is started
     * Then: it is reported as untracked — the same sentinel a database error in {@link LoginLockout#begin}
     *       also returns, which lets a caller fall back to a different count rather than treat it as a real,
     *       freshly-counted attempt of 0
     */
    @Test
    public void test_untrackedAttempt_whenLockoutDisabled_isReportedUntracked() throws Exception {
        final boolean enabledBefore = Config.getBooleanProperty(LoginLockout.LOCKOUT_ENABLED, true);
        Config.setProperty(LoginLockout.LOCKOUT_ENABLED, false);
        final User user = newUser();
        try {
            final LoginLockout.Attempt attempt = LoginLockout.getInstance().begin(user.getUserId(), ADDRESS);
            assertTrue(attempt.isUntracked());
        } finally {
            Config.setProperty(LoginLockout.LOCKOUT_ENABLED, enabledBefore);
            cleanUp(user);
        }
    }

    /**
     * Given: lockout switched on
     * When: a normal attempt is started
     * Then: it is reported as tracked, not the sentinel returned when nothing could be recorded
     */
    @Test
    public void test_trackedAttempt_whenLockoutEnabled_isNotReportedUntracked() throws Exception {
        final User user = newUser();
        try {
            final LoginLockout.Attempt attempt = LoginLockout.getInstance().begin(user.getUserId(), ADDRESS);
            assertFalse(attempt.isUntracked());
        } finally {
            cleanUp(user);
        }
    }

    /**
     * Given: two different accounts, each failing logins with no resolvable address ({@link
     *        LoginLockout#UNKNOWN_SOURCE})
     * When: a third, fresh account fails once, also with no resolvable address
     * Then: its pacing seed reflects only its own count, not the other two accounts' failures
     *
     * {@code UNKNOWN_SOURCE} is a shared fallback bucket for callers without a real client address, not a real
     * network origin, so failures recorded under it must not escalate each other's delay the way failures from
     * one real address do.
     */
    @Test
    public void test_pacing_seed_ignores_other_accounts_under_unknown_source() throws Exception {
        final User first = newUser();
        final User second = newUser();
        final User third = newUser();
        try {
            LoginLockout.getInstance().begin(first.getUserId(), LoginLockout.UNKNOWN_SOURCE);
            LoginLockout.getInstance().begin(second.getUserId(), LoginLockout.UNKNOWN_SOURCE);
            final LoginLockout.Attempt thirdAttempt =
                    LoginLockout.getInstance().begin(third.getUserId(), LoginLockout.UNKNOWN_SOURCE);
            assertEquals(thirdAttempt.number(), LoginLockout.getInstance().pacingSeed(thirdAttempt));
        } finally {
            cleanUp(first);
            cleanUp(second);
            cleanUp(third);
        }
    }

    /**
     * Given: an account, with the delay strategy configured as a literal per-unit value ({@code
     *        TIME_MILLS:500})
     * When: 3 wrong passwords arrive from one address at the same moment
     * Then: they do not all take the same flat delay — the literal unit scales by each attempt's own
     *       atomically-counted position, the same way the default ("pow") strategy already does
     */
    @Test
    public void test_concurrent_wrong_logins_get_escalating_delays_with_literal_strategy() throws Exception {
        final Object strategyBefore = Config.getStringProperty(WebKeys.AUTH_FAILED_ATTEMPTS_DELAY_STRATEGY, "pow");
        Config.setProperty(WebKeys.AUTH_FAILED_ATTEMPTS_DELAY_STRATEGY_ENABLED, true);
        Config.setProperty(WebKeys.AUTH_FAILED_ATTEMPTS_DELAY_STRATEGY, "TIME_MILLS:500");
        final User user = newUser();
        try {
            final List<Long> elapsed = timedInParallel(3, ADDRESS, () -> login(user, "wrong-in-parallel"));
            final long max = elapsed.stream().mapToLong(Long::longValue).max().orElseThrow();
            // Today, the configured "500" always replaces the computed seed outright, so every concurrent
            // attempt sleeps the same flat 500ms regardless of its attempt number. Once the literal value
            // scales by each attempt's own atomically-counted position (1, 2, 3) instead of replacing it, the
            // slowest sleeps 3 * 500 = 1500ms. An absolute floor is used for the same reason as the "pow"
            // version of this test: thread-pool and connection jitter alone could satisfy a relative
            // comparison.
            assertTrue("slowest of " + elapsed + "ms should reflect a scaled delay, not a flat one",
                    max >= 1_200L);
        } finally {
            Config.setProperty(WebKeys.AUTH_FAILED_ATTEMPTS_DELAY_STRATEGY_ENABLED, false);
            Config.setProperty(WebKeys.AUTH_FAILED_ATTEMPTS_DELAY_STRATEGY, strategyBefore);
            cleanUp(user);
        }
    }

    /**
     * Given: an address already locked out of an account, and a literal delay strategy ({@code
     *        TIME_MILLS:500})
     * When: one more wrong password is tried from that same, now-locked address
     * Then: it still sleeps close to the configured delay, not next to nothing — a locked-out attempt is
     *       never counted, and the lock itself resets the address's own failure count to 0, so pacing by
     *       the raw count alone would scale the literal value down to zero instead of up
     */
    @Test
    public void test_delay_does_not_collapse_when_an_attempt_is_refused_by_a_lock() throws Exception {
        final Object strategyBefore = Config.getStringProperty(WebKeys.AUTH_FAILED_ATTEMPTS_DELAY_STRATEGY, "pow");
        Config.setProperty(WebKeys.AUTH_FAILED_ATTEMPTS_DELAY_STRATEGY, "TIME_MILLS:500");
        final User user = newUser();
        try {
            lockByFailedLogins(user);

            Config.setProperty(WebKeys.AUTH_FAILED_ATTEMPTS_DELAY_STRATEGY_ENABLED, true);
            final long start = System.currentTimeMillis();
            assertEquals(Authenticator.FAILURE, login(user, "wrong-while-locked"));
            final long elapsed = System.currentTimeMillis() - start;
            assertTrue("failed login while locked took only " + elapsed + "ms, should reflect the configured delay",
                    elapsed >= 400L);
        } finally {
            Config.setProperty(WebKeys.AUTH_FAILED_ATTEMPTS_DELAY_STRATEGY_ENABLED, false);
            Config.setProperty(WebKeys.AUTH_FAILED_ATTEMPTS_DELAY_STRATEGY, strategyBefore);
            cleanUp(user);
        }
    }

    /**
     * Given: lockout switched off, an account whose stored failed-login count is far above the limit, and a
     *        delay strategy with a literal per-unit rate
     * When: one more wrong password is tried
     * Then: the delay is capped the same way the tracked path already is, not scaled by the uncapped stored
     *       count — a strategy that scales by the seed must not be able to sleep arbitrarily long off a count
     *       that, with lockout off, nothing ever resets
     */
    @Test
    public void test_delaySeed_caps_the_untracked_fallback_count() throws Exception {
        final boolean enabledBefore = Config.getBooleanProperty(LoginLockout.LOCKOUT_ENABLED, true);
        final Object strategyBefore = Config.getStringProperty(WebKeys.AUTH_FAILED_ATTEMPTS_DELAY_STRATEGY, "pow");
        Config.setProperty(LoginLockout.LOCKOUT_ENABLED, false);
        Config.setProperty(WebKeys.AUTH_FAILED_ATTEMPTS_DELAY_STRATEGY_ENABLED, true);
        Config.setProperty(WebKeys.AUTH_FAILED_ATTEMPTS_DELAY_STRATEGY, "TIME_MILLS:50");
        final User user = newUser();
        try {
            final User counted = APILocator.getUserAPI().loadUserById(user.getUserId());
            counted.setFailedLoginAttempts(20);
            APILocator.getUserAPI().save(counted, APILocator.systemUser(), false);

            // Uncapped, this failure would sleep 20 * 50 = 1000ms; capped at LIMIT - 1 it sleeps at most 200ms
            final long start = System.currentTimeMillis();
            assertEquals(Authenticator.FAILURE, login(user, "wrong-uncapped"));
            final long elapsed = System.currentTimeMillis() - start;
            assertTrue("failed login took " + elapsed + "ms, should be capped well under the uncapped 1000ms",
                    elapsed < 600L);
        } finally {
            Config.setProperty(LoginLockout.LOCKOUT_ENABLED, enabledBefore);
            Config.setProperty(WebKeys.AUTH_FAILED_ATTEMPTS_DELAY_STRATEGY_ENABLED, false);
            Config.setProperty(WebKeys.AUTH_FAILED_ATTEMPTS_DELAY_STRATEGY, strategyBefore);
            cleanUp(user);
        }
    }
}
