package com.dotcms.auth;

import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;

/**
 * Single place that resolves the user of a request. Stub for the Red gate: no behavior yet.
 */
public final class RequestUserResolver {

    private RequestUserResolver() {
    }

    public static User resolveRequestUser(final HttpServletRequest request) {
        return null;
    }
}
