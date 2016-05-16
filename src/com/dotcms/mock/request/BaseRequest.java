package com.dotcms.mock.request;

import javax.servlet.http.HttpServletRequest;

import org.mockito.Mockito;

public class BaseRequest implements MockRequest {


	public HttpServletRequest request() {
		return Mockito.mock(HttpServletRequest.class);
	}

	
	
	
}
