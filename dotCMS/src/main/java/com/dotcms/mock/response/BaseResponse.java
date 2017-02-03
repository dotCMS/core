package com.dotcms.mock.response;

import javax.servlet.http.HttpServletResponse;

import org.mockito.Mockito;

public class BaseResponse implements MockResponse {

	private final HttpServletResponse base;

	public BaseResponse() {
		base = Mockito.mock(HttpServletResponse.class);
	}

	@Override
	public HttpServletResponse response() {
		return base;
	}

}
