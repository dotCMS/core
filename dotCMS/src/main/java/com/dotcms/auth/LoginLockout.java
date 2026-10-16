package com.dotcms.auth;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.business.CloseDBIfOpened;
import com.dotmarketing.cms.login.factories.LoginFactory;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PropsUtil;
import com.liferay.util.GetterUtil;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.LongSupplier;

/**
 * Login lockout keyed on the account and the client address, so failed logins from one place cannot
 * lock the account owner out from another. Two counts are kept in the {@code login_lockout} table,
 * shared by every node of a cluster:
 * <ul>
 *     <li><b>Per account and client address.</b> Each address gets {@code auth.max.failures.limit}
 *     password attempts within the counting window. When they are used up without a success, that
 *     address is locked out of that account. Other addresses, including the owner's, are not affected.
 *     WebDAV is counted apart from other logins ({@value #WEBDAV_SOURCE_PREFIX}address), so a mounted
 *     drive retrying a stale password locks only WebDAV from that machine.</li>
 *     <li><b>Per account, all addresses together</b> (source {@value #ACCOUNT_WIDE}). A looser backstop:
 *     once failures from every address add up to {@code AUTH_LOCKOUT_ACCOUNT_MAX_FAILURES} within the
 *     window, the account is locked for everyone. This caps guessing spread over many addresses.</li>
 * </ul>
 * An attempt is counted against its address before the password is checked ({@link #begin}), so
 * parallel requests cannot all pass a check made before any of them was counted. Locks last a fixed
 * time and do not grow. While a lock applies every password check fails, including the correct
 * password, and nothing is counted. A successful login clears the count for its own address; a password
 * reset or a new password set by an administrator clears everything for the account.
 *
 * <p>The client address is {@link HttpServletRequest#getRemoteAddr()}. Behind a proxy it is the real
 * client only when Tomcat's {@code RemoteIpValve} trusts that proxy (the bundled {@code server.xml}
 * trusts private ranges by default); otherwise every login shares the proxy's address.
 *
 * <p>The main login flow ({@code UserManagerImpl#authenticate}) uses {@link #begin},
 * {@link #succeeded} and {@link #failed} directly. Other places that check a password (WebDAV, the
 * legacy {@code LoginFactory.doLogin} methods) go through
 * {@link #passwordMatch(String, User, String, BiPredicate)}.
 *
 * <p>Configuration:
 * <ul>
 *     <li>{@code AUTH_LOCKOUT_ENABLED} (default {@code true})</li>
 *     <li>{@code auth.max.failures.limit}: attempts per address (portal property, default 5)</li>
 *     <li>{@code AUTH_LOCKOUT_ACCOUNT_MAX_FAILURES}: failures per account from all addresses
 *     (default 100; 0 switches the account-wide lock off)</li>
 *     <li>{@code AUTH_LOCKOUT_WINDOW_SECONDS}: how long attempts are counted together (default 3600)</li>
 *     <li>{@code AUTH_LOCKOUT_DURATION_SECONDS}: lock length (default 900)</li>
 * </ul>
 */
public final class LoginLockout {

    public static final String LOCKOUT_ENABLED = "AUTH_LOCKOUT_ENABLED";
    public static final String LOCKOUT_DURATION_SECONDS = "AUTH_LOCKOUT_DURATION_SECONDS";
    public static final String LOCKOUT_WINDOW_SECONDS = "AUTH_LOCKOUT_WINDOW_SECONDS";
    public static final String LOCKOUT_ACCOUNT_MAX_FAILURES = "AUTH_LOCKOUT_ACCOUNT_MAX_FAILURES";

    /** Source of the account-wide row. Never a client address. */
    public static final String ACCOUNT_WIDE = "*";

    /** Source used when there is no request to take an address from. */
    public static final String UNKNOWN_SOURCE = "unknown";

    /** Prefix that keeps WebDAV attempts apart from other logins from the same address. */
    public static final String WEBDAV_SOURCE_PREFIX = "webdav:";

    private static final int MAX_ADDRESS_LENGTH = 64;

    /**
     * Counts one attempt for an account and source and returns the new count. Both CASE conditions
     * mean the count starts over: it is older than the window, or its lock has run out. Parameters:
     * user, source, now, mod date, then window cutoff and now (failures), window cutoff, now and now
     * (window start), now (lock), mod date.
     */
    private static final String COUNT_SQL =
            "INSERT INTO login_lockout (user_id, source, failures, window_start, locked_until, mod_date)"
                    + " VALUES (?, ?, 1, ?, 0, ?) ON CONFLICT (user_id, source) DO UPDATE SET"
                    + " failures = CASE WHEN (login_lockout.window_start < ? OR (login_lockout.locked_until > 0"
                    + " AND login_lockout.locked_until <= ?)) THEN 1 ELSE login_lockout.failures + 1 END,"
                    + " window_start = CASE WHEN (login_lockout.window_start < ? OR (login_lockout.locked_until > 0"
                    + " AND login_lockout.locked_until <= ?)) THEN ? ELSE login_lockout.window_start END,"
                    + " locked_until = CASE WHEN login_lockout.locked_until <= ? THEN 0"
                    + " ELSE login_lockout.locked_until END,"
                    + " mod_date = ? RETURNING failures";

    /** Hands back one attempt that turned out not to be a password failure. */
    private static final String UNCOUNT_SQL =
            "UPDATE login_lockout SET failures = failures - 1 WHERE user_id = ? AND source = ? AND failures > 0";

    /** Locks an account and source. Parameters: user, source, now, locked until, mod date, then locked until, now, mod date. */
    private static final String LOCK_SQL =
            "INSERT INTO login_lockout (user_id, source, failures, window_start, locked_until, mod_date)"
                    + " VALUES (?, ?, 0, ?, ?, ?) ON CONFLICT (user_id, source) DO UPDATE SET"
                    + " locked_until = ?, failures = 0, window_start = ?, mod_date = ?";

    /**
     * Lock state of one account as seen from one source.
     */
    public enum Status {
        /** Nothing is recorded for this source and nothing is locked. */
        NONE,
        /** Attempts from this source are recorded but nothing is locked. */
        RECORDED,
        /** This source, or the whole account, is locked. */
        LOCKED
    }

    /**
     * One login attempt, counted against its source before the password is checked. Get it from
     * {@link #begin(String, String)} and finish it with {@link #succeeded}, {@link #failed} or
     * {@link #notAPasswordLogin}.
     */
    public static final class Attempt {

        private static final Attempt UNTRACKED = new Attempt(null, null, false, false, 0);

        private final String userId;
        private final String source;
        private final boolean refused;
        private final boolean counted;
        private final int number;

        private Attempt(final String userId, final String source, final boolean refused, final boolean counted,
                        final int number) {
            this.userId = userId;
            this.source = source;
            this.refused = refused;
            this.counted = counted;
            this.number = number;
        }

        /**
         * @return {@code true} when the password must not be accepted: a lock applies, or this source has
         * used up its attempts for the window
         */
        public boolean isRefused() {
            return refused;
        }
    }

    private static final LoginLockout INSTANCE = new LoginLockout(System::currentTimeMillis);

    private final LongSupplier clock;

    LoginLockout(final LongSupplier clock) {
        this.clock = clock;
    }

    public static LoginLockout getInstance() {
        return INSTANCE;
    }

    /**
     * @return the attempts allowed per source in a window ({@code auth.max.failures.limit}), 0 when unset
     */
    public static int maxFailures() {
        return GetterUtil.get(PropsUtil.get(PropsUtil.AUTH_MAX_FAILURES_LIMIT), 0);
    }

    /**
     * @return the failure limit for an account from all addresses together, 0 or less when switched off
     */
    public static int accountMaxFailures() {
        return Config.getIntProperty(LOCKOUT_ACCOUNT_MAX_FAILURES, 100);
    }

    /**
     * @return the client address of the current request, or {@value #UNKNOWN_SOURCE} outside a request
     */
    public static String clientAddress() {
        return clientAddress(HttpServletRequestThreadLocal.INSTANCE.getRequest());
    }

    /**
     * @return the client address of the given request, or {@value #UNKNOWN_SOURCE} when there is none
     */
    public static String clientAddress(final HttpServletRequest request) {
        final String address = null != request ? request.getRemoteAddr() : null;
        if (!UtilMethods.isSet(address)) {
            return UNKNOWN_SOURCE;
        }
        return address.length() > MAX_ADDRESS_LENGTH ? address.substring(0, MAX_ADDRESS_LENGTH) : address;
    }

    /**
     * @return the source WebDAV logins from the current request are counted under
     */
    public static String webdavSource() {
        return WEBDAV_SOURCE_PREFIX + clientAddress();
    }

    /**
     * @return {@code true} when lockout is switched on and a failure limit is configured
     */
    public boolean isEnabled() {
        return isEnabled(maxFailures());
    }

    /**
     * @return {@code true} when lockout is switched on and a failure limit is configured
     */
    public boolean isEnabled(final int maxFailures) {
        return maxFailures > 0 && Config.getBooleanProperty(LOCKOUT_ENABLED, true);
    }

    /**
     * Returns the lock state to enforce for a login to the account from the given source:
     * {@link Status#NONE} when lockout is switched off, otherwise what is stored.
     */
    public Status check(final String userId, final String source) {
        return isEnabled() ? status(userId, source) : Status.NONE;
    }

    /**
     * Returns the stored lock state for the account and source, whether or not lockout is switched on.
     * A database error is treated as {@link Status#NONE} so an outage of this table never blocks every
     * login; the error is logged.
     */
    @CloseDBIfOpened
    public Status status(final String userId, final String source) {
        try {
            final List<Map<String, Object>> rows = new DotConnect()
                    .setSQL("SELECT source, locked_until FROM login_lockout WHERE user_id = ? AND source IN (?, ?)")
                    .addParam(userId).addParam(source).addParam(ACCOUNT_WIDE)
                    .loadObjectResults();
            final long now = clock.getAsLong();
            boolean recorded = false;
            for (final Map<String, Object> row : rows) {
                if (((Number) row.get("locked_until")).longValue() > now) {
                    return Status.LOCKED;
                }
                recorded |= source.equals(row.get("source"));
            }
            return recorded ? Status.RECORDED : Status.NONE;
        } catch (final DotDataException | RuntimeException e) {
            Logger.error(this, "Unable to read login lockout state for user " + userId + ": " + e.getMessage(), e);
            return Status.NONE;
        }
    }

    /**
     * Returns whether a login to the account from the given source would be refused right now, whether
     * or not lockout is switched on.
     */
    public boolean isLocked(final String userId, final String source) {
        return status(userId, source) == Status.LOCKED;
    }

    /**
     * Returns whether any lock on the account is in force right now: for some source, or account-wide.
     */
    @CloseDBIfOpened
    public boolean isLocked(final String userId) {
        try {
            return !new DotConnect()
                    .setSQL("SELECT source FROM login_lockout WHERE user_id = ? AND locked_until > ?")
                    .addParam(userId).addParam(clock.getAsLong())
                    .loadObjectResults().isEmpty();
        } catch (final DotDataException | RuntimeException e) {
            Logger.error(this, "Unable to read login lockout state for user " + userId + ": " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Starts a login attempt from the given source, before its password is checked. When a lock is in
     * force the attempt is refused and nothing is counted. Otherwise the attempt is counted now, and
     * refused if that takes the source past its limit for the window; counting first means parallel
     * attempts get at most the limit between them. With lockout switched off, or on a database error,
     * the attempt is neither counted nor refused.
     */
    @CloseDBIfOpened
    public Attempt begin(final String userId, final String source) {
        final int maxFailures = maxFailures();
        if (!isEnabled(maxFailures)) {
            return Attempt.UNTRACKED;
        }
        if (status(userId, source) == Status.LOCKED) {
            return new Attempt(userId, source, true, false, 0);
        }
        try {
            final int number = count(userId, source);
            return new Attempt(userId, source, number > maxFailures, true, number);
        } catch (final DotDataException | RuntimeException e) {
            Logger.error(this, "Unable to count login attempt for user " + userId + ": " + e.getMessage(), e);
            return Attempt.UNTRACKED;
        }
    }

    /**
     * Finishes an attempt whose password was accepted: earlier attempts from its source no longer count.
     * Nothing is written unless the attempt was counted.
     */
    public void succeeded(final Attempt attempt) {
        if (attempt.counted) {
            clear(attempt.userId, attempt.source);
        }
    }

    /**
     * Finishes an attempt that was not a password failure (for example, WebDAV accepted an API token in
     * the password field): its count is handed back.
     */
    @CloseDBIfOpened
    public void notAPasswordLogin(final Attempt attempt) {
        if (!attempt.counted) {
            return;
        }
        try {
            new DotConnect().setSQL(UNCOUNT_SQL).addParam(attempt.userId).addParam(attempt.source).loadResult();
        } catch (final DotDataException | RuntimeException e) {
            Logger.error(this, "Unable to update login lockout state for user " + attempt.userId + ": "
                    + e.getMessage(), e);
        }
    }

    /**
     * Finishes an attempt whose password was refused or wrong. Locks the source when it has used up its
     * attempts, and counts a wrong password toward the account-wide limit, locking the account when that
     * is reached. An attempt refused under a lock, or never counted, changes nothing. Errors are logged,
     * never thrown, so a problem with this table never breaks a login.
     */
    @CloseDBIfOpened
    public void failed(final Attempt attempt) {
        if (!attempt.counted) {
            return;
        }
        try {
            if (attempt.number >= maxFailures()) {
                lock(attempt.userId, attempt.source, attempt.number);
            }
            final int accountMax = accountMaxFailures();
            if (!attempt.refused && accountMax > 0) {
                final int accountFailures = count(attempt.userId, ACCOUNT_WIDE);
                if (accountFailures >= accountMax) {
                    lock(attempt.userId, ACCOUNT_WIDE, accountFailures);
                }
            }
        } catch (final DotDataException | RuntimeException e) {
            Logger.error(this, "Unable to record failed login for user " + attempt.userId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Clears the count for one source. The account-wide count is left to run out with its window.
     */
    @CloseDBIfOpened
    public void clear(final String userId, final String source) {
        try {
            new DotConnect().setSQL("DELETE FROM login_lockout WHERE user_id = ? AND source = ?")
                    .addParam(userId).addParam(source).loadResult();
        } catch (final DotDataException | RuntimeException e) {
            Logger.error(this, "Unable to clear login lockout state for user " + userId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Clears all counts and locks for the account, from every source. Used when a new password is set.
     */
    @CloseDBIfOpened
    public void clear(final String userId) {
        try {
            new DotConnect().setSQL("DELETE FROM login_lockout WHERE user_id = ?").addParam(userId).loadResult();
        } catch (final DotDataException | RuntimeException e) {
            Logger.error(this, "Unable to clear login lockout state for user " + userId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Same as {@link #passwordMatch(String, User, String, BiPredicate)} for a password-only login from
     * the current request's address.
     */
    public boolean passwordMatch(final String password, final User user) {
        return passwordMatch(password, user, clientAddress(), (secret, candidate) -> false);
    }

    /**
     * Password check for login paths that do not go through {@code UserManagerImpl#authenticate}: WebDAV
     * and the legacy {@code LoginFactory.doLogin} methods. It answers the way that path does:
     * <ul>
     *     <li>while a lock applies to this source or the whole account, or the source has used up its
     *     attempts, every password is refused after the same hashing work;</li>
     *     <li>a wrong password counts as a failed login from this source;</li>
     *     <li>a right password clears the count for this source.</li>
     * </ul>
     * With lockout switched off this is a plain {@link LoginFactory#passwordMatch(String, User)}.
     *
     * @param source          what attempts are counted under: the client address, or
     *                        {@link #webdavSource()} for WebDAV
     * @param otherCredential checked when the password does not match, for callers that also accept
     *                        another secret in the password field (WebDAV takes an API token). A match
     *                        there is not a password login, so it is accepted while locked and its count
     *                        is handed back.
     * @return whether the caller should treat the user as authenticated
     */
    public boolean passwordMatch(final String password, final User user, final String source,
                                 final BiPredicate<String, User> otherCredential) {
        if (!isEnabled()) {
            return LoginFactory.passwordMatch(password, user) || otherCredential.test(password, user);
        }
        final Attempt attempt = begin(user.getUserId(), source);
        // Check the password even when refused, so a refused login takes the same time to answer and
        // gives no signal about whether the guess was right.
        final boolean match = LoginFactory.passwordMatch(password, user) && !attempt.isRefused();
        if (match) {
            succeeded(attempt);
            return true;
        }
        if (otherCredential.test(password, user)) {
            notAPasswordLogin(attempt);
            return true;
        }
        if (attempt.isRefused()) {
            SecurityLogger.logInfo(LoginLockout.class, "Login refused, locked out: user " + user.getUserId()
                    + " from " + source);
        }
        failed(attempt);
        return false;
    }

    /**
     * Counts one attempt for the account and source in a single statement, so concurrent attempts on
     * several nodes all count, and returns the count this attempt brought it to.
     */
    private int count(final String userId, final String source) throws DotDataException {
        final long now = clock.getAsLong();
        final long windowCutoff = now - windowSeconds() * 1000L;
        final Date modDate = new Date(now);
        final List<Map<String, Object>> rows = new DotConnect()
                .setSQL(COUNT_SQL)
                .addParam(userId).addParam(source).addParam(now).addParam(modDate)
                .addParam(windowCutoff).addParam(now)
                .addParam(windowCutoff).addParam(now).addParam(now)
                .addParam(now)
                .addParam(modDate)
                .loadObjectResults();
        if (rows.isEmpty()) {
            throw new DotDataException("No count returned for login attempt of user " + userId);
        }
        return ((Number) rows.get(0).get("failures")).intValue();
    }

    private void lock(final String userId, final String source, final int attempts) throws DotDataException {
        final long now = clock.getAsLong();
        final long seconds = lockSeconds();
        final long lockedUntil = now + seconds * 1000L;
        final Date modDate = new Date(now);
        new DotConnect()
                .setSQL(LOCK_SQL)
                .addParam(userId).addParam(source).addParam(now).addParam(lockedUntil).addParam(modDate)
                .addParam(lockedUntil).addParam(now).addParam(modDate)
                .loadResult();
        SecurityLogger.logInfo(LoginLockout.class, ACCOUNT_WIDE.equals(source)
                ? "Account locked for all addresses after " + attempts + " failed logins: user " + userId
                        + " for " + seconds + "s, last attempt from " + clientAddress()
                : "Source locked out of account after " + attempts + " login attempts: user " + userId
                        + " from " + source + " for " + seconds + "s");
    }

    /**
     * @return the lock length in seconds; fixed, so repeated locks do not grow
     */
    long lockSeconds() {
        return Math.max(1, Config.getLongProperty(LOCKOUT_DURATION_SECONDS, 900));
    }

    /**
     * @return how long attempts are counted together, in seconds
     */
    long windowSeconds() {
        return Math.max(1, Config.getLongProperty(LOCKOUT_WINDOW_SECONDS, 3600));
    }
}
