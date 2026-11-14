package com.dotcms.auth;

import com.dotmarketing.business.UserAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Decides whether an account may be used as the identity of a client request or session.
 * <p>
 * The built-in account is not eligible: it is only ever an identity chosen by server code (see
 * {@link SystemUserScope}). The rule can be turned off with {@value #KILL_SWITCH_KEY}, which is read on
 * every call so it takes effect without a restart.
 */
public final class InteractiveUserPolicy {

    /** Config key of the rollback switch; {@code true} (the default) applies the rule. */
    static final String KILL_SWITCH_KEY = "USER_RESOLUTION_EXCLUDE_SYSTEM_USER";

    private static final AtomicInteger DISABLED_WARNINGS = new AtomicInteger();

    private InteractiveUserPolicy() {
    }

    /**
     * Whether the account can be the identity of a client request or session.
     *
     * @param user the account, may be null
     * @return {@code false} only for the built-in account (by id or email, case-insensitive) while the rule
     *         is on; {@code false} for null (there is no account to use); {@code true} for every other account and while the rule is off
     */
    public static boolean isEligible(final User user) {
        if (user == null) {
            return false;
        }
        if (!isRuleOn()) {
            return true;
        }
        return !UserAPI.SYSTEM_USER_ID.equalsIgnoreCase(user.getUserId())
                && !UserAPI.SYSTEM_USER_EMAIL.equalsIgnoreCase(user.getEmailAddress());
    }

    /**
     * Same rule as {@link #isEligible(User)} when only the account id is known.
     *
     * @param userId the account id, may be null
     * @return {@code false} for null and for the built-in account's id while the rule is on
     */
    public static boolean isEligibleUserId(final String userId) {
        if (userId == null) {
            return false;
        }
        return !isRuleOn() || !UserAPI.SYSTEM_USER_ID.equalsIgnoreCase(userId);
    }

    /**
     * Returns the account when it is eligible and {@code null} ("no user") when it is not, recording the
     * refusal in the security log (see {@link InteractiveUserRefusalLog}). Never writes to the request or
     * the session.
     *
     * @param user    the account found, may be null
     * @param request the request it was found for, may be null
     * @param session the session it was found in, may be null; used only to log once per session
     * @return the account, or null when it must not be used as an interactive identity
     */
    public static User refuseIfNotEligible(final User user, final HttpServletRequest request,
            final HttpSession session) {
        if (user == null) {
            return null;
        }
        if (isEligible(user)) {
            return user;
        }
        InteractiveUserRefusalLog.record(request, session);
        return null;
    }

    /**
     * Same as {@link #refuseIfNotEligible(User, HttpServletRequest, HttpSession)} when only the account id
     * is known.
     *
     * @param userId  the account id found, may be null
     * @param request the request it was found for, may be null
     * @param session the session it was found in, may be null; used only to log once per session
     * @return the id, or null when it must not be used as an interactive identity
     */
    public static String refuseIfNotEligibleId(final String userId, final HttpServletRequest request,
            final HttpSession session) {
        if (userId == null) {
            return null;
        }
        if (isEligibleUserId(userId)) {
            return userId;
        }
        InteractiveUserRefusalLog.record(request, session);
        return null;
    }

    /**
     * Whether the rule is applied right now (the rollback switch is on). Read on every call.
     *
     * @return {@code false} when {@value #KILL_SWITCH_KEY} is off, after logging one warning per JVM
     */
    static boolean isRuleOn() {
        if (Config.getBooleanProperty(KILL_SWITCH_KEY, true)) {
            return true;
        }
        if (DISABLED_WARNINGS.compareAndSet(0, 1)) {
            Logger.warn(InteractiveUserPolicy.class, KILL_SWITCH_KEY
                    + " is false: accounts that cannot sign in interactively are accepted as before");
        }
        return false;
    }

    static int disabledWarningCount() {
        return DISABLED_WARNINGS.get();
    }

    static void resetDisabledWarningForTests() {
        DISABLED_WARNINGS.set(0);
    }
}
