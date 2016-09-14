package com.dotcms.mock.request;

import javax.servlet.http.HttpServletRequest;

/**
 * Mocks a full featured request to http://127.0.0.1/
 *
 *  See an example here: ContentletAPITest widgetInvalidateAllLang()
 */
public class MockInternalRequest implements MockRequest {

	@Override
	public HttpServletRequest request() {

		return new MockHttpRequest("127.0.0.1", "/").request();
	}

}
