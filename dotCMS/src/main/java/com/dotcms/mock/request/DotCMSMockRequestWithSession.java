package com.dotcms.mock.request;

import javax.servlet.http.HttpSession;

/**
 * Pretty much the same of {@link DotCMSMockRequest} but includes a session
 */
public class DotCMSMockRequestWithSession extends DotCMSMockRequest {

    private final HttpSession session;
    private final boolean isSecure;
    public DotCMSMockRequestWithSession(final HttpSession session, final boolean isSecure) {

        this.session  = session;
        this.isSecure = isSecure;
    }

    @Override
    public HttpSession getSession(boolean b) {
        return getSession();
    }

    @Override
    public HttpSession getSession() {
        return session;
    }

    @Override
    public boolean isSecure() {
        return isSecure;
    }
}