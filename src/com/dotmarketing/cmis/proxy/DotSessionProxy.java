package com.dotmarketing.cmis.proxy;

import java.util.Map;

import javax.servlet.http.HttpSession;

/*
 * This is  Proxy for HTTPServletRequest, This extends Map to put and get objects.
 * This uses DotRequestProxy.map to store and retrieve objects.
 */

public interface DotSessionProxy extends HttpSession,Map{

}
