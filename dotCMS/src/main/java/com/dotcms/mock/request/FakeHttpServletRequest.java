package com.dotcms.mock.request;

import com.dotcms.repackage.org.directwebremoting.util.FakeHttpSession;

import javax.servlet.http.HttpSession;

public class FakeHttpServletRequest extends com.dotcms.repackage.org.directwebremoting.util.FakeHttpServletRequest {

    private final HttpSession session;

    public FakeHttpServletRequest() {

        session = new FakeHttpSession();
    }

    public FakeHttpServletRequest(final HttpSession session) {

        this.session = session;
    }

    @Override
    public HttpSession getSession(boolean create) {

        return null == session?
                super.getSession(create): session;
    }

    @Override
    public HttpSession getSession() {
        return session;
    }
}
