package com.dotmarketing.beans;

import java.util.Map;

import javax.servlet.http.HttpSession;

public class Request {
	private String requestURL;
	private String requestURI;
	private HttpSession session;
	private Map parameterMap;
	
	public String getRequestURL() {
		return requestURL;
	}
	
	public void setRequestURL(String requestURL) {
		this.requestURL = requestURL;
	}
	
	public HttpSession getSession() {
		return session;
	}
	
	public void setSession(HttpSession session) {
		this.session = session;
	}
	
	public String getRequestURI() {
		return requestURI;
	}
	
	public void setRequestURI(String requestURI) {
		this.requestURI = requestURI;
	}
	
	public Map getParameterMap() {
		return parameterMap;
	}
	
	public void setParameterMap(Map parameterMap) {
		this.parameterMap = parameterMap;
	}
}