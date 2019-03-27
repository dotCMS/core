package com.dotcms.mock.request;

import javax.servlet.http.HttpServletRequest;

/**
 * How to implement, see: {@link MockHttpRequest}
 */
public interface MockRequest  {

	HttpServletRequest request();
}
