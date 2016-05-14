package com.dotcms.proxy.response;

import javax.servlet.http.HttpServletResponse;

public class MockHttpResponse implements MockResponse {

	final HttpServletResponse base;
	public MockHttpResponse(HttpServletResponse response){
		base = response;
	}
	
	
	@Override
	public HttpServletResponse response() {
		return base;
	}
	

}
