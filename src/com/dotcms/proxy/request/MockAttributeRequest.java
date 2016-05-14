package com.dotcms.proxy.request;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public class MockAttributeRequest extends HttpServletRequestWrapper implements MockRequest {
	final Map<String, Object> attributes = new HashMap<String, Object>();

	public MockAttributeRequest(HttpServletRequest request) {
		super(request);
		Enumeration<String> attrs = request.getAttributeNames();
		while(attrs !=null && attrs.hasMoreElements()){
			String key = attrs.nextElement();
			attributes.put(key, request.getAttribute(key));
		}
	}

	public HttpServletRequest request() {
		return this;
	}

	@Override
	public Object getAttribute(String name) {
		return attributes.get(name);
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		return new Vector<String>(attributes.keySet()).elements();
	}

	@Override
	public void setAttribute(String name, Object o) {
		attributes.put(name, o);
	}

}