package com.dotcms.auth;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;

import java.util.List;
import java.util.Map;
import java.util.function.LongSupplier;

/**
 * Per-account login lockout. Once an account reaches the configured number of consecutive failed
 * logins ({@code auth.max.failures.limit}) it is locked for a period that doubles with each further
 * lockout, up to a maximum. While an account is locked every password check fails, including the
 * correct password, so guessing cannot continue in the background.
 *
 * <p>State lives in the {@code login_lockout} table so all nodes of a cluster share it. A successful
 * login, or a password reset, clears it.
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

    private static final LoginLockout INSTANCE = new LoginLockout(System::currentTimeMillis);

    private final LongSupplier clock;

    LoginLockout(final LongSupplier clock) {
        this.clock = clock;
    }

    public static LoginLockout getInstance() {
        return INSTANCE;
    }

    /**
     * @return {@code true} when lockout is switched on and a failure limit is configured
     */
    public boolean isEnabled(final int maxFailures) {
        return maxFailures > 0 && Config.getBooleanProperty(LOCKOUT_ENABLED, true);
    }

    /**
     * Returns whether the account is locked right now. A database error is treated as not locked so
     * an outage of this table never blocks every login; the error is logged.
     */
    public boolean isLocked(final String userId) {
        try {
            final List<Map<String, Object>> rows = new DotConnect()
                    .setSQL("SELECT locked_until FROM login_lockout WHERE user_id = ?")
                    .addParam(userId)
                    .loadObjectResults();
            return !rows.isEmpty() && ((Number) rows.get(0).get("locked_until")).longValue() > clock.getAsLong();
        } catch (final DotDataException | RuntimeException e) {
            Logger.error(this, "Unable to read login lockout state for user " + userId + ": " + e.getMessage(), e);
            return false;
        }
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
     * Clears any lockout state for the account, after a successful login or a password reset.
     */
    public void clear(final String userId) {
        try {
            new DotConnect().setSQL("DELETE FROM login_lockout WHERE user_id = ?").addParam(userId).loadResult();
        } catch (final DotDataException | RuntimeException e) {
            Logger.error(this, "Unable to clear login lockout state for user " + userId + ": " + e.getMessage(), e);
        }
    }

    long lockSeconds(final int previousLockouts) {
        final long base = Math.max(1, Config.getLongProperty(LOCKOUT_DURATION_SECONDS, 900));
        final long max = Math.max(base, Config.getLongProperty(LOCKOUT_MAX_DURATION_SECONDS, 86400));
        final int shift = Math.min(previousLockouts, 30);
        return Math.min(max, base << shift);
    }
}
