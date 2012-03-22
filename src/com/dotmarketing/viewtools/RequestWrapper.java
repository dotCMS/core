package com.dotmarketing.viewtools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.dotmarketing.util.UtilMethods;
import com.liferay.util.Xss;

public class RequestWrapper implements HttpServletRequest{

	private HttpServletRequest _request;
	
	public RequestWrapper(HttpServletRequest req) {
		this._request = req;
	}
	
	public String getActualParameter(String arg0) {
		return _request.getParameter(arg0);
	}
	
	public String getAuthType() {
		return _request.getAuthType();
	}

	public String getContextPath() {
		return _request.getContextPath();
	}

	public Cookie[] getCookies() {
		return _request.getCookies();
	}

	public long getDateHeader(String arg0) {
		return _request.getDateHeader(arg0);
	}

	public String getHeader(String arg0) {
		return _request.getHeader(arg0);
	}

	public Enumeration getHeaderNames() {
		return _request.getHeaderNames();
	}

	public Enumeration getHeaders(String arg0) {
		return _request.getHeaders(arg0);
	}

	public int getIntHeader(String arg0) {
		return _request.getIntHeader(arg0);
	}

	public String getMethod() {
		return _request.getMethod();
	}

	public String getPathInfo() {
		return _request.getPathInfo();
	}

	public String getPathTranslated() {
		return _request.getPathTranslated();
	}

	public String getQueryString() {
		return _request.getQueryString();
	}

	public String getRemoteUser() {
		return _request.getRemoteUser();
	}

	public String getRequestURI() {
		return _request.getRequestURI();
	}

	public StringBuffer getRequestURL() {
		return _request.getRequestURL();
	}

	public String getRequestedSessionId() {
		return _request.getRequestedSessionId();
	}

	public String getServletPath() {
		return _request.getServletPath();
	}

	public HttpSession getSession() {
		return _request.getSession();
	}

	public HttpSession getSession(boolean arg0) {
		return _request.getSession(arg0);
	}

	public Principal getUserPrincipal() {
		return _request.getUserPrincipal();
	}

	public boolean isRequestedSessionIdFromCookie() {
		return _request.isRequestedSessionIdFromCookie();
	}

	public boolean isRequestedSessionIdFromURL() {
		return _request.isRequestedSessionIdFromURL();
	}

	public boolean isRequestedSessionIdFromUrl() {
		return _request.isRequestedSessionIdFromUrl();
	}

	public boolean isRequestedSessionIdValid() {
		return _request.isRequestedSessionIdValid();
	}

	public boolean isUserInRole(String arg0) {
		return _request.isUserInRole(arg0);
	}

	public Object getAttribute(String arg0) {
		return _request.getAttribute(arg0);
	}

	public Enumeration getAttributeNames() {
		return _request.getAttributeNames();
	}

	public String getCharacterEncoding() {
		return _request.getCharacterEncoding();
	}

	public int getContentLength() {
		return _request.getContentLength();
	}

	public String getContentType() {
		return _request.getContentType();
	}

	public ServletInputStream getInputStream() throws IOException {
		return _request.getInputStream();
	}

	public String getLocalAddr() {
		return _request.getLocalAddr();
	}

	public String getLocalName() {
		return _request.getLocalName();
	}

	public int getLocalPort() {
		return _request.getLocalPort();
	}

	public Locale getLocale() {
		return _request.getLocale();
	}

	public Enumeration getLocales() {
		return _request.getLocales();
	}

	public String getParameter(String arg0) {
		String ret = _request.getParameter(arg0);
		if(UtilMethods.isSet(ret) && Xss.URLHasXSS(ret)){
			ret = UtilMethods.htmlifyString(ret);
		}
		return ret;
	}

	public Map getParameterMap() {
		return _request.getParameterMap();
	}

	public Enumeration getParameterNames() {
		return _request.getParameterNames();
	}

	public String[] getParameterValues(String arg0) {
		return _request.getParameterValues(arg0);
	}

	public String getProtocol() {
		return _request.getProtocol();
	}

	public BufferedReader getReader() throws IOException {
		return _request.getReader();
	}

	public String getRealPath(String arg0) {
		return _request.getRealPath(arg0);
	}

	public String getRemoteAddr() {
		return _request.getRemoteAddr();
	}

	public String getRemoteHost() {
		return _request.getRemoteHost();
	}

	public int getRemotePort() {
		return _request.getRemotePort();
	}

	public RequestDispatcher getRequestDispatcher(String arg0) {
		return _request.getRequestDispatcher(arg0);
	}

	public String getScheme() {
		return _request.getScheme();
	}

	public String getServerName() {
		return _request.getServerName();
	}

	public int getServerPort() {
		return _request.getServerPort();
	}

	public boolean isSecure() {
		return _request.isSecure();
	}

	public void removeAttribute(String arg0) {
		// TODO Auto-generated method stub
		
	}

	public void setAttribute(String arg0, Object arg1) {
		_request.setAttribute(arg0, arg1);
	}

	public void setCharacterEncoding(String arg0) throws UnsupportedEncodingException {
		_request.setCharacterEncoding(arg0);
	}

}
