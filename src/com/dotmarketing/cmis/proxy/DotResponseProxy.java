package com.dotmarketing.cmis.proxy;

import java.util.Map;

import javax.servlet.http.HttpServletResponse;

/**
 * This is Proxy for HTTPServletResponse, This extends Map to put and get objects.
 * 
 * @deprecated As of release 3.6 use {@link com.dotcms.mock.response.MockHttpResponse#response()}
 *             instead
 */
@Deprecated
public interface DotResponseProxy extends HttpServletResponse, Map {

}
