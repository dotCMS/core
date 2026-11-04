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
import com.google.common.net.InetAddresses;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PropsUtil;
import com.liferay.util.GetterUtil;

import javax.servlet.http.HttpServletRequest;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
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
 *     IPv6 addresses are grouped by their /64 prefix, which one host can rotate through. WebDAV is
 *     counted apart from other logins ({@value #WEBDAV_SOURCE_PREFIX}address), so a mounted drive
 *     retrying a stale password locks only WebDAV from that machine.</li>
 *     <li><b>Per account, all addresses together</b> (source {@value #ACCOUNT_WIDE}). An optional, looser
 *     backstop, off by default: once failures from every address add up to
 *     {@code AUTH_LOCKOUT_ACCOUNT_MAX_FAILURES} within the window, the account is locked for everyone. It
 *     caps guessing spread over many addresses, but anyone who knows a login name can then trigger it, so
 *     it is opt-in.</li>
 * </ul>
 * An attempt is counted against its address before the password is checked ({@link #begin}), so
 * parallel requests cannot all pass a check made before any of them was counted. A refused attempt whose
 * password was right hands its count back ({@link #release}), so only wrong passwords lead to a lock and
 * a client that sends many requests at once just gets some refusals it can retry. Locks last a fixed
 * time and do not grow. While a lock applies every password check fails, including the correct
 * password, and nothing is counted. A successful login clears the count for its own address; a password
 * reset or a new password set by an administrator clears everything for the account. Times come from
 * the database clock, so nodes whose own clocks differ still agree on when a lock ends.
 *
 * <p>The client address is {@link HttpServletRequest#getRemoteAddr()}. Behind a proxy it is the real
 * client only when Tomcat's {@code RemoteIpValve} trusts that proxy (the bundled {@code server.xml}
 * trusts private ranges by default); otherwise every login shares the proxy's address. A warning is
 * logged, at most once a minute per node, for any address locked out of several accounts, which is what
 * that looks like.
 *
 * <p>The main login flow ({@code UserManagerImpl#authenticate}) uses {@link #begin},
 * {@link #succeeded} and {@link #failed} directly. Other places that check a password (WebDAV, the
 * legacy {@code LoginFactory.doLogin} methods, Login As and the current-password check on a user update)
 * go through {@link #passwordMatch(String, User, String, BiPredicate)}. Passwords checked by an external
 * authenticator such as LDAP are counted with {@link #countFailure(String, String)}.
 *
 * <p>Configuration:
 * <ul>
 *     <li>{@code AUTH_LOCKOUT_ENABLED}: switches lockout on or off (default {@code true})</li>
 *     <li>{@code auth.max.failures.limit}: attempts per address (portal property, default 5)</li>
 *     <li>{@code AUTH_LOCKOUT_ACCOUNT_MAX_FAILURES}: failures per account from all addresses
 *     (default 0, which leaves the account-wide lock off)</li>
 *     <li>{@code AUTH_LOCKOUT_WINDOW_SECONDS}: how long attempts are counted together (default 3600)</li>
 *     <li>{@code AUTH_LOCKOUT_DURATION_SECONDS}: lock length (default 900)</li>
 * </ul>
 */
public final class LoginLockout {

    /**
     * Switches lockout on or off; on when unset. A shipped default that can be overridden, read with
     * {@link Config} like the other {@code AUTH_LOCKOUT_*} settings rather than declared as a feature flag.
     */
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

    /** Bytes of an IPv6 address that identify its /64 network. */
    private static final int IPV6_PREFIX_BYTES = 8;

    /** An address locked out of this many accounts at once is reported as a likely shared proxy address. */
    private static final int SHARED_SOURCE_WARNING_ACCOUNTS = 5;

    private static final long MAINTENANCE_INTERVAL_MILLIS = 60_000L;

    /**
     * Counts one attempt for an account and source and returns the new count and the source's lock.
     * Both CASE conditions mean the count starts over: it is older than the window, or its lock has run
     * out. Parameters: user, source, now, mod date, then window cutoff and now (failures), window
     * cutoff, now and now (window start), now (lock), mod date.
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
                    + " mod_date = ? RETURNING failures, locked_until";

    /** Hands back one attempt that turned out not to be a password failure. */
    private static final String UNCOUNT_SQL =
            "UPDATE login_lockout SET failures = failures - 1 WHERE user_id = ? AND source = ? AND failures > 0";

    /** Locks an account and source. Parameters: user, source, now, locked until, mod date, then locked until, now, mod date. */
    private static final String LOCK_SQL =
            "INSERT INTO login_lockout (user_id, source, failures, window_start, locked_until, mod_date)"
                    + " VALUES (?, ?, 0, ?, ?, ?) ON CONFLICT (user_id, source) DO UPDATE SET"
                    + " locked_until = ?, failures = 0, window_start = ?, mod_date = ?";

    /** Clears one source's count after a success, but never a lock in force. Parameters: user, source, now. */
    private static final String CLEAR_SOURCE_SQL =
            "DELETE FROM login_lockout WHERE user_id = ? AND source = ? AND locked_until <= ?";

    /**
     * Sources locked out of at least a given number of accounts right now, with that number. Parameters: now,
     * account-wide source, minimum accounts.
     */
    private static final String SHARED_SOURCES_SQL =
            "SELECT source, COUNT(DISTINCT user_id) AS accounts FROM login_lockout"
                    + " WHERE locked_until > ? AND source <> ? GROUP BY source HAVING COUNT(DISTINCT user_id) >= ?";

    /**
     * Removes rows that no longer affect any login: their lock has run out (or never existed) and their
     * count is older than the window. Parameters: now, window cutoff.
     */
    private static final String PURGE_SQL =
            "DELETE FROM login_lockout WHERE locked_until <= ? AND window_start < ?";

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
     * {@link #release}.
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

        /**
         * @return {@code true} when the attempt was refused because a lock is in force, as opposed to
         * refused only because too many attempts from its source were in flight at once (no lock is set
         * then, unless the password turns out to be wrong)
         */
        public boolean isLockedOut() {
            return refused && !counted;
        }
    }

    /**
     * Milliseconds since the epoch by the database's clock, so every node agrees on when a count started
     * and when a lock ends even if their own clocks differ. The difference between the database clock and
     * this node's is read at most once a minute; in between, this node's clock carries it forward. If the
     * read fails, the last known difference is kept (none at first) and it is tried again a minute later.
     */
    static final class DatabaseClock implements LongSupplier {

        private static final String NOW_SQL = "SELECT (EXTRACT(EPOCH FROM clock_timestamp()) * 1000)::bigint AS now";

        private static final long SYNC_INTERVAL_MILLIS = 60_000L;

        private volatile long offset;
        private volatile long nextSync;

        /**
         * @return the current time by the database clock, in milliseconds since the epoch
         */
        @Override
        public long getAsLong() {
            final long local = System.currentTimeMillis();
            if (local >= nextSync) {
                nextSync = local + SYNC_INTERVAL_MILLIS;
                try {
                    offset = readDatabaseTime() - System.currentTimeMillis();
                } catch (final DotDataException | RuntimeException e) {
                    Logger.warn(LoginLockout.class, "Unable to read the database clock for login lockout: "
                            + e.getMessage());
                }
            }
            return local + offset;
        }

        /**
         * @return the database's current time, in milliseconds since the epoch
         */
        static long readDatabaseTime() throws DotDataException {
            return ((Number) new DotConnect().setSQL(NOW_SQL).loadObjectResults().get(0).get("now")).longValue();
        }
    }

    private static final LoginLockout INSTANCE = new LoginLockout(new DatabaseClock());

    private final LongSupplier clock;
    private final AtomicLong lastMaintenance = new AtomicLong();

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
     * @return the failure limit for an account from all addresses together; 0 or less (the default) leaves the
     * account-wide lock off
     */
    public static int accountMaxFailures() {
        return Config.getIntProperty(LOCKOUT_ACCOUNT_MAX_FAILURES, 0);
    }

    /**
     * @return the source the current request's logins are counted under, or {@value #UNKNOWN_SOURCE}
     * outside a request
     */
    public static String clientAddress() {
        return clientAddress(HttpServletRequestThreadLocal.INSTANCE.getRequest());
    }

    /**
     * Returns the source a request's logins are counted under: its remote address, with an IPv6
     * address reduced to its /64 network ({@code 2001:db8:1:2::/64}), since a single host can usually
     * pick any address in its /64. An IPv4-mapped IPv6 address counts as the IPv4 address.
     *
     * @return the source, or {@value #UNKNOWN_SOURCE} when there is no request or address
     */
    public static String clientAddress(final HttpServletRequest request) {
        final String address = null != request ? request.getRemoteAddr() : null;
        if (!UtilMethods.isSet(address)) {
            return UNKNOWN_SOURCE;
        }
        final String source = address.indexOf(':') >= 0 ? ipv6Network(address) : address;
        return source.length() > MAX_ADDRESS_LENGTH ? source.substring(0, MAX_ADDRESS_LENGTH) : source;
    }

    /**
     * @return the /64 network of an IPv6 literal, the IPv4 address of an IPv4-mapped one, or the input
     * unchanged when it is not an IP literal. Parsing never does a DNS lookup.
     */
    private static String ipv6Network(final String address) {
        final InetAddress parsed;
        try {
            // Tomcat may report a zone, as in fe80::1%eth0; the zone does not identify the host
            final int zone = address.indexOf('%');
            parsed = InetAddresses.forString(zone >= 0 ? address.substring(0, zone) : address);
        } catch (final IllegalArgumentException e) {
            return address;
        }
        if (!(parsed instanceof Inet6Address)) {
            return parsed.getHostAddress();
        }
        final byte[] network = Arrays.copyOf(Arrays.copyOf(parsed.getAddress(), IPV6_PREFIX_BYTES), 16);
        try {
            // Built from raw bytes, so no lookup either
            return InetAddresses.toAddrString(InetAddress.getByAddress(network)) + "/64";
        } catch (final UnknownHostException e) {
            return address;
        }
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
     * refused if that takes the source past its limit for the window, or if a lock was set on the source
     * between the check and the count; counting first means parallel attempts get at most the limit
     * between them. With lockout switched off, or on a database error, the attempt is neither counted
     * nor refused.
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
            final Map<String, Object> row = count(userId, source);
            if (((Number) row.get("locked_until")).longValue() > clock.getAsLong()) {
                // Locked by a parallel attempt after the check above: refused, and the lock left as it is
                return new Attempt(userId, source, true, false, 0);
            }
            final int number = ((Number) row.get("failures")).intValue();
            return new Attempt(userId, source, number > maxFailures, true, number);
        } catch (final DotDataException | RuntimeException e) {
            Logger.error(this, "Unable to count login attempt for user " + userId + ": " + e.getMessage(), e);
            return Attempt.UNTRACKED;
        }
    }

    /**
     * Finishes an attempt whose password was accepted: earlier attempts from its source no longer count.
     * A lock that a parallel attempt set on the source stays. Nothing is written unless the attempt was
     * counted.
     */
    public void succeeded(final Attempt attempt) {
        if (attempt.counted) {
            succeeded(attempt.userId, attempt.source);
        }
    }

    /**
     * Records a successful password login from the given source that was checked outside {@link #begin}
     * (for example by LDAP): earlier attempts from the source no longer count. A lock in force stays.
     */
    @CloseDBIfOpened
    public void succeeded(final String userId, final String source) {
        try {
            new DotConnect().setSQL(CLEAR_SOURCE_SQL)
                    .addParam(userId).addParam(source).addParam(clock.getAsLong())
                    .loadResult();
        } catch (final DotDataException | RuntimeException e) {
            Logger.error(this, "Unable to clear login lockout state for user " + userId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Finishes an attempt that was not a password failure, handing its count back: the password was
     * right but the attempt was refused (a lock applies, or too many attempts from the source were in
     * flight at once), or a different credential was accepted (WebDAV takes an API token in the password
     * field). Only a wrong password can lead to a lock.
     */
    @CloseDBIfOpened
    public void release(final Attempt attempt) {
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
                final int accountFailures = ((Number) count(attempt.userId, ACCOUNT_WIDE).get("failures")).intValue();
                if (accountFailures >= accountMax) {
                    lock(attempt.userId, ACCOUNT_WIDE, accountFailures);
                }
            }
        } catch (final DotDataException | RuntimeException e) {
            Logger.error(this, "Unable to record failed login for user " + attempt.userId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Counts a failed login whose password was checked by an external authenticator, such as LDAP or a
     * WebDAV pre-authenticator, rather than by dotCMS. Same effect as a wrong password through
     * {@link #begin} and {@link #failed}; nothing is counted while a lock is in force.
     */
    public void countFailure(final String userId, final String source) {
        failed(begin(userId, source));
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
     * Password check for places that do not go through {@code UserManagerImpl#authenticate}: WebDAV, the
     * legacy {@code LoginFactory.doLogin} methods, Login As and the current-password check on a user
     * update. It answers the way that path does:
     * <ul>
     *     <li>while a lock applies to this source or the whole account, or the source has used up its
     *     attempts (including attempts still in flight), every password is refused after the same hashing
     *     work. A refused attempt with the right password hands its count back, so it never sets a lock;
     *     the caller can retry;</li>
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
        if (LoginFactory.passwordMatch(password, user)) {
            if (!attempt.isRefused()) {
                succeeded(attempt);
                return true;
            }
            logRefused(attempt);
            release(attempt);
            return false;
        }
        if (otherCredential.test(password, user)) {
            release(attempt);
            return true;
        }
        if (attempt.isRefused()) {
            logRefused(attempt);
        }
        failed(attempt);
        return false;
    }

    /**
     * Writes the security log line for a refused attempt, saying whether a lock is in force or the source
     * only had too many attempts in flight at once, so support does not look for a lock that is not there.
     */
    public void logRefused(final Attempt attempt) {
        SecurityLogger.logInfo(LoginLockout.class, (attempt.isLockedOut()
                ? "Login refused, locked out: user "
                : "Login refused, too many concurrent attempts: user ")
                + attempt.userId + " from " + attempt.source);
    }

    /**
     * Counts one attempt for the account and source in a single statement, so concurrent attempts on
     * several nodes all count.
     *
     * @return the row after counting: {@code failures} (the count this attempt brought it to) and
     * {@code locked_until}
     */
    private Map<String, Object> count(final String userId, final String source) throws DotDataException {
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
        return rows.get(0);
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
        maintain(now);
    }

    /**
     * Housekeeping run when a lock is set, at most once a minute per node, so it costs nothing on most
     * locks while still running whenever failures are frequent: deletes rows that no longer affect a login,
     * and reports sources that look shared.
     */
    private void maintain(final long now) {
        final long last = lastMaintenance.get();
        if (now - last < MAINTENANCE_INTERVAL_MILLIS || !lastMaintenance.compareAndSet(last, now)) {
            return;
        }
        purgeExpired(now);
        warnAboutSharedSources(now);
    }

    /**
     * Deletes rows, for any account, that no longer affect a login. Sources that fail and never succeed
     * would otherwise stay in the table for good.
     */
    private void purgeExpired(final long now) {
        try {
            new DotConnect().setSQL(PURGE_SQL)
                    .addParam(now).addParam(now - windowSeconds() * 1000L)
                    .loadResult();
        } catch (final DotDataException | RuntimeException e) {
            Logger.warn(this, "Unable to remove expired login lockout rows: " + e.getMessage());
        }
    }

    /**
     * Logs a warning for each source locked out of several accounts at once. Either one client is trying
     * many accounts, or the source is a proxy or load balancer that Tomcat does not trust, so every user
     * behind it is counted as one address.
     */
    private void warnAboutSharedSources(final long now) {
        try {
            final List<Map<String, Object>> rows = new DotConnect().setSQL(SHARED_SOURCES_SQL)
                    .addParam(now).addParam(ACCOUNT_WIDE).addParam(SHARED_SOURCE_WARNING_ACCOUNTS)
                    .loadObjectResults();
            for (final Map<String, Object> row : rows) {
                Logger.warn(this, "Login lockout: " + row.get("source") + " is locked out of " + row.get("accounts")
                        + " accounts. Either one client is trying many accounts, or this is a proxy or load balancer"
                        + " address and every user behind it shares one lockout. If it is a proxy, add it to"
                        + " CMS_REMOTEIP_INTERNALPROXIES so Tomcat uses the real client address.");
            }
        } catch (final DotDataException | RuntimeException e) {
            Logger.warn(this, "Unable to check login lockout sources: " + e.getMessage());
        }
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
