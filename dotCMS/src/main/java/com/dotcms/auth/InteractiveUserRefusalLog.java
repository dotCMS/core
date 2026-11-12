package com.dotcms.auth;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Writes the security-log entry for an identity that was refused because it cannot be used interactively.
 * Stub for the Red gate: no behavior yet.
 */
public final class InteractiveUserRefusalLog {

    /** Text of the entry; contains neither a session identifier nor a credential. */
    static final String MESSAGE = "user not eligible for interactive use";

    private InteractiveUserRefusalLog() {
    }

    public static void record(final HttpServletRequest request, final HttpSession session) {
    }

    static int maxTrackedSessions() {
        return 0;
    }

    static int trackedSessions() {
        return 0;
    }

    static void resetForTests() {
    }
}
