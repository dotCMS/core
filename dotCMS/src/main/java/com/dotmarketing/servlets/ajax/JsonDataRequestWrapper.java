package com.dotmarketing.servlets.ajax;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import com.google.common.collect.Iterators;

public class JsonDataRequestWrapper extends HttpServletRequestWrapper {

    private final Map<String, String[]> requestParamMap;

    public JsonDataRequestWrapper(final HttpServletRequest request, final Map<String, String[]> jsonData) {
        super(request);
        this.requestParamMap = new HashMap<>();
        requestParamMap.putAll(request.getParameterMap());
        requestParamMap.putAll(jsonData);

    }

    public JsonDataRequestWrapper(HttpServletRequest request) {
        super(request);
        this.requestParamMap = new HashMap<>();
        requestParamMap.putAll(request.getParameterMap());
    }

    @Override
    public String getParameter(final String name) {
        final String[] result = requestParamMap.get(name);
        return result != null && result.length > 0 ? result[0] : null;
    }

    @Override
    public Map<String, String[]> getParameterMap() {

        return requestParamMap;
    }

    @Override
    public Enumeration<String> getParameterNames() {

        return Iterators.asEnumeration(requestParamMap.keySet().iterator());
    }

    @Override
    public String[] getParameterValues(final String name) {

        return requestParamMap.get(name);
    }

}
