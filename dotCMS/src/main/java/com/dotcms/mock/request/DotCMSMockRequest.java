package com.dotcms.mock.request;

import com.dotcms.api.vtl.model.DotJSON;
import com.dotcms.repackage.org.directwebremoting.util.FakeHttpServletRequest;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class DotCMSMockRequest extends FakeHttpServletRequest {

    private String uri;
    private StringBuffer requestURL;
    private String serverName;
    private String remoteAddr;
    private String remoteHost;
    private String queryString;
    private Map<String, String[]> paramMap = new HashMap<>();
    private Map<String, String> headers = new HashMap<>();
    private String servletPath;
    private Map<String, Object> attributes;

    @Override
    public String getRequestURI() {
        return uri;
    }

    @Override
    public StringBuffer getRequestURL() {
        return requestURL;
    }

    @Override
    public String getServerName() {
        return serverName;
    }

    @Override
    public String getRemoteAddr() {
        return remoteAddr;
    }

    @Override
    public String getRemoteHost() {
        return remoteHost;
    }

    @Override
    public String getQueryString() {
        return queryString;
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return paramMap;
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(paramMap.keySet());
    }

    @Override
    public String getParameter(final String name) {
        final String[] answer = paramMap.get(name);
        return answer!=null && answer.length > 0 ? answer[0] : null;
    }

    @Override
    public String getHeader(final String name) {
        return headers.get(name);
    }

    @Override
    public String getServletPath() {
        return servletPath;
    }

    @Override
    public Object getAttribute(String name) {
        return this.attributes.get(name);
    }

    public void setAttribute(String name, Object value) {
        this.attributes.put(name, value);
    }

    public void addHeader(final String name, final String value) {
        this.headers.put(name, value);
    }

    public void setRequestURI(final String uri) {
        this.uri = uri;
    }

    public void setRequestURL(final StringBuffer requestURL) {
        this.requestURL = requestURL;
    }

    public void setServerName(final String serverName) {
        this.serverName = serverName;
    }

    public void setRemoteAddr(final String remoteAddr) {
        this.remoteAddr = remoteAddr;
    }

    public void setRemoteHost(final String remoteHost) {
        this.remoteHost = remoteHost;
    }

    public void setQueryString(final String queryString) {
        this.queryString = queryString;
    }

    public void setParameterMap(final Map<String, String[]> paramMap) {
        this.paramMap = paramMap;
    }

    public void setServletPath(String servletPath) {
        this.servletPath = servletPath;
    }
}