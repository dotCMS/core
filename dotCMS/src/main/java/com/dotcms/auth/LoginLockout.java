package com.dotcms.auth;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.cms.login.factories.LoginFactory;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PropsUtil;
import com.liferay.util.GetterUtil;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.LongSupplier;

/**
 * Per-account login lockout. Once an account reaches the configured number of consecutive failed
 * logins ({@code auth.max.failures.limit}) it is locked for a period that doubles with each further
 * lockout, up to a maximum. While an account is locked every password check fails, including the
 * correct password, so guessing cannot continue in the background.
 *
 * <p>State lives in the {@code login_lockout} table so all nodes of a cluster share it. A successful
 * login, a password reset, or a new password set by an administrator clears it.
 *
 * <p>The main login flow ({@code UserManagerImpl#authenticate}) reads the state with {@link #check(String)}.
 * Other places that check a password directly (WebDAV, the legacy {@code LoginFactory.doLogin} methods)
 * go through {@link #passwordMatch(String, User, BiPredicate)}, so a locked account is refused there too.
 *
 * <p>Configuration:
 * <ul>
 *     <li>{@code AUTH_LOCKOUT_ENABLED} (default {@code true})</li>
 *     <li>{@code AUTH_LOCKOUT_DURATION_SECONDS}: first lockout length (default 900)</li>
 *     <li>{@code AUTH_LOCKOUT_MAX_DURATION_SECONDS}: cap for the doubling (default 86400)</li>
 * </ul>
 */
public final class LoginLockout {

    public static final String LOCKOUT_ENABLED = "AUTH_LOCKOUT_ENABLED";
    public static final String LOCKOUT_DURATION_SECONDS = "AUTH_LOCKOUT_DURATION_SECONDS";
    public static final String LOCKOUT_MAX_DURATION_SECONDS = "AUTH_LOCKOUT_MAX_DURATION_SECONDS";

    /**
     * Lock state of one account, as stored in {@code login_lockout}.
     */
    public enum Status {
        /** Nothing is stored for the account, so a successful login has nothing to clear. */
        NONE,
        /** A row is stored but its lock has run out. It still holds the lockout count. */
        EXPIRED,
        /** The account is locked now. */
        LOCKED
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
     * @return the configured failure limit ({@code auth.max.failures.limit}), 0 when unset
     */
    public static int maxFailures() {
        return GetterUtil.get(PropsUtil.get(PropsUtil.AUTH_MAX_FAILURES_LIMIT), 0);
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
     * Returns the lock state to enforce for the account: {@link Status#NONE} when lockout is switched
     * off, otherwise what is stored. One read answers both whether to refuse the login and whether a
     * successful login has anything to clear.
     */
    public Status check(final String userId) {
        return isEnabled() ? status(userId) : Status.NONE;
    }

    /**
     * Returns the stored lock state, whether or not lockout is switched on. A database error is treated
     * as {@link Status#NONE} so an outage of this table never blocks every login; the error is logged.
     */
    public Status status(final String userId) {
        try {
            final List<Map<String, Object>> rows = new DotConnect()
                    .setSQL("SELECT locked_until FROM login_lockout WHERE user_id = ?")
                    .addParam(userId)
                    .loadObjectResults();
            if (rows.isEmpty()) {
                return Status.NONE;
            }
            return ((Number) rows.get(0).get("locked_until")).longValue() > clock.getAsLong()
                    ? Status.LOCKED : Status.EXPIRED;
        } catch (final DotDataException | RuntimeException e) {
            Logger.error(this, "Unable to read login lockout state for user " + userId + ": " + e.getMessage(), e);
            return Status.NONE;
        }
    }

    /**
     * Returns whether the account is locked right now, whether or not lockout is switched on.
     */
    public boolean isLocked(final String userId) {
        return status(userId) == Status.LOCKED;
    }

    /**
     * Locks the account. The length is the base duration doubled for each previous lockout that has
     * not been cleared by a successful login, capped at the maximum.
     *
     * @return the lock length in seconds
     */
    public long lock(final String userId, final String remoteAddress) throws DotDataException {
        final List<Map<String, Object>> rows = new DotConnect()
                .setSQL("SELECT lockout_count FROM login_lockout WHERE user_id = ?")
                .addParam(userId)
                .loadObjectResults();
        final int previous = rows.isEmpty() ? 0 : ((Number) rows.get(0).get("lockout_count")).intValue();
        final long seconds = lockSeconds(previous);
        final long now = clock.getAsLong();
        final long lockedUntil = now + seconds * 1000L;
        new DotConnect()
                .setSQL("INSERT INTO login_lockout (user_id, locked_until, lockout_count, mod_date) VALUES (?, ?, ?, ?) ON CONFLICT (user_id) DO UPDATE SET locked_until = ?, lockout_count = ?, mod_date = ?")
                .addParam(userId).addParam(lockedUntil).addParam(previous + 1).addParam(new java.util.Date(now))
                .addParam(lockedUntil).addParam(previous + 1).addParam(new java.util.Date(now))
                .loadResult();
        SecurityLogger.logInfo(LoginLockout.class, "Account locked after repeated failed logins: user " + userId
                + " for " + seconds + "s (lockout #" + (previous + 1) + "), last attempt from " + remoteAddress);
        return seconds;
    }

    /**
     * Clears any lockout state for the account. Login paths call this only when {@link #check(String)}
     * found a row, so an ordinary successful login does not write to the table.
     */
    public void clear(final String userId) {
        try {
            new DotConnect().setSQL("DELETE FROM login_lockout WHERE user_id = ?").addParam(userId).loadResult();
        } catch (final DotDataException | RuntimeException e) {
            Logger.error(this, "Unable to clear login lockout state for user " + userId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Same as {@link #passwordMatch(String, User, BiPredicate)} for callers that accept only a password.
     */
    public boolean passwordMatch(final String password, final User user) {
        return passwordMatch(password, user, (secret, candidate) -> false);
    }

    /**
     * Password check for login paths that do not go through {@code UserManagerImpl#authenticate}: WebDAV
     * and the legacy {@code LoginFactory.doLogin} methods. It answers the way that path does:
     * <ul>
     *     <li>while the account is locked every password is refused, after the same hashing work, and
     *     the failure count does not move;</li>
     *     <li>a wrong password counts as a failed login, and locks the account once the limit is
     *     reached;</li>
     *     <li>a right password resets the failure count and clears any stored lock state.</li>
     * </ul>
     * With lockout switched off this is a plain {@link LoginFactory#passwordMatch(String, User)}.
     *
     * @param otherCredential checked when the password does not match, for callers that also accept
     *                        another secret in the password field (WebDAV takes an API token). A match
     *                        there is not a password login, so it is accepted while locked and leaves the
     *                        lock state and failure count alone.
     * @return whether the caller should treat the user as authenticated
     */
    public boolean passwordMatch(final String password, final User user,
                                 final BiPredicate<String, User> otherCredential) {
        final int maxFailures = maxFailures();
        if (!isEnabled(maxFailures)) {
            return LoginFactory.passwordMatch(password, user) || otherCredential.test(password, user);
        }
        final Status status = status(user.getUserId());
        // Check the password even while locked, so a locked account takes the same time to answer and
        // gives no signal about whether the guess was right.
        final boolean match = LoginFactory.passwordMatch(password, user) && status != Status.LOCKED;
        if (match) {
            recordSuccess(user, status);
            return true;
        }
        if (otherCredential.test(password, user)) {
            return true;
        }
        if (status == Status.LOCKED) {
            SecurityLogger.logInfo(LoginLockout.class, "Login refused, account locked: user " + user.getUserId()
                    + " from " + remoteAddress());
        } else {
            recordFailure(user, maxFailures);
        }
        return false;
    }

    private void recordSuccess(final User user, final Status status) {
        if (status != Status.NONE) {
            clear(user.getUserId());
        }
        if (user.getFailedLoginAttempts() > 0) {
            user.setFailedLoginAttempts(0);
            saveUser(user);
        }
    }

    private void recordFailure(final User user, final int maxFailures) {
        user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
        if (user.getFailedLoginAttempts() >= maxFailures) {
            try {
                lock(user.getUserId(), remoteAddress());
                // The next window starts from zero; repeated lockouts get longer instead.
                user.setFailedLoginAttempts(0);
            } catch (final DotDataException e) {
                Logger.error(this, "Unable to lock account " + user.getUserId() + ": " + e.getMessage(), e);
            }
        }
        saveUser(user);
    }

    private void saveUser(final User user) {
        try {
            APILocator.getUserAPI().save(user, APILocator.getUserAPI().getSystemUser(), false);
        } catch (final DotDataException | DotSecurityException | DotStateException e) {
            Logger.error(this, "Unable to save failed login count for user " + user.getUserId() + ": "
                    + e.getMessage(), e);
        }
    }

    private static String remoteAddress() {
        final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        return null != request ? request.getRemoteAddr() : "unknown";
    }

    long lockSeconds(final int previousLockouts) {
        final long base = Math.max(1, Config.getLongProperty(LOCKOUT_DURATION_SECONDS, 900));
        final long max = Math.max(base, Config.getLongProperty(LOCKOUT_MAX_DURATION_SECONDS, 86400));
        final int shift = Math.min(previousLockouts, 30);
        return Math.min(max, base << shift);
    }
}
