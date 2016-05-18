package com.dotmarketing.cmis.proxy;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

/**
 * This is Proxy for HTTPServletRequest, This extends Map to put and get objects.
 * 
 * @deprecated As of release 3.6 use {@link com.dotcms.mock.request.MockHttpRequest#request()}
 *             instead
 */
@Deprecated
public interface DotRequestProxy extends HttpServletRequest,Map{
	
}
