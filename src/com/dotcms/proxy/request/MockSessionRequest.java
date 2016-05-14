package com.dotcms.proxy.request;

import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;

public class MockSessionRequest extends HttpServletRequestWrapper implements MockRequest {

	MockSession session = null;

	public MockSessionRequest(HttpServletRequest request) {
		super(request);

	}

	public HttpServletRequest request() {
		return this;
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
		return (create) ? getSession() : null;
	}
	
 
}