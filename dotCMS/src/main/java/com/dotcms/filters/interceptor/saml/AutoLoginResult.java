package com.dotcms.filters.interceptor.saml;

import javax.servlet.http.HttpSession;

public class AutoLoginResult
{
	private final HttpSession session;
	private final boolean autoLogin;

	public AutoLoginResult(HttpSession session, boolean autoLogin )
	{
		this.session = session;
		this.autoLogin = autoLogin;
	}

	public HttpSession getSession()
	{
		return session;
	}

	public boolean isAutoLogin()
	{
		return autoLogin;
	}
}
