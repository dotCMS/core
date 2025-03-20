package com.dotcms.mock.request;

import com.dotmarketing.util.Config;

import java.util.Enumeration;
import java.util.UUID;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;

/**
 * Mock {@link HttpServletRequest} with a {@link MockSession} as part of
 * the class, when getting the session this will return the session object of the class or
 * create a new one with new {@link UUID} as ID.
 *
 * See an example here: {@link MockHttpRequest#MockHttpRequest(String, String)}
 */
public class MockSessionRequest extends HttpServletRequestWrapper implements MockRequest {

	HttpSession session = null;

	public MockSessionRequest(HttpServletRequest request) {
		super(request);
		if (request.getSession(false) != null) {
			session = new MockSession(request.getSession());
		}

	}

	public HttpServletRequest request() {
		return this;
	}

	@Override
	public ServletContext getServletContext() {
		return Config.CONTEXT;
	}

	@Override
	public HttpSession getSession() {
		if(session==null){
			session = new MockSession(UUID.randomUUID().toString());
		}
		return session;
	}

	@Override
	public HttpSession getSession(boolean create) {
		return (create)
				? getSession()
				: session;

	}


	public HttpSession setSession(final HttpSession session) {
		this.session = session;
		return this.session;
	}


}