package com.dotmarketing.cmis.proxy;

import java.util.Map;

import javax.servlet.http.HttpServletResponse;

/*
 * This is  Proxy for HTTPServletResponse, This extends Map to put and get objects.
 * 
 */

public interface DotResponseProxy extends HttpServletResponse,Map{
	
}
