package com.dotcms.auth;

import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Decides whether an account may be used as the identity of a client request or session.
 * Stub for the Red gate: no behavior yet.
 */
public final class InteractiveUserPolicy {

    private InteractiveUserPolicy() {
    }

    public static boolean isEligible(final User user) {
        return true;
    }

    public static User refuseIfNotEligible(final User user, final HttpServletRequest request,
            final HttpSession session) {
        return user;
    }

    static int disabledWarningCount() {
        return 0;
    }

    static void resetDisabledWarningForTests() {
    }
}
