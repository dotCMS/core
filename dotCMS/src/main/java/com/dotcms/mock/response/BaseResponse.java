package com.dotcms.mock.response;

import javax.servlet.http.HttpServletResponse;

public class BaseResponse implements MockResponse {

	private final HttpServletResponse base;

	public BaseResponse() {
		base = new DotCMSMockResponse();
	}

	@Override
	public HttpServletResponse response() {
		return base;
	}

}
