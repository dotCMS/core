package com.dotcms.auth;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Wraps a request and its session so the user keys never hold an account that cannot be used interactively.
 * Stub for the Red gate: no behavior yet.
 */
public final class InteractiveIdentityWrapper {

    private InteractiveIdentityWrapper() {
    }

    public static HttpServletRequest wrap(final HttpServletRequest request) {
        return request;
    }

    public static HttpSession wrap(final HttpSession session) {
        return session;
    }
}
