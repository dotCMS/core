package com.dotmarketing.cmis.proxy;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/*
 * This is  Proxy for HTTPServletResponse, This extends Map to put and get objects.
 * 
 */

public interface DotResponseProxy extends HttpServletResponse,Map{
	
	public static final Map<String,Object> map = new HashMap<String, Object>();

}
