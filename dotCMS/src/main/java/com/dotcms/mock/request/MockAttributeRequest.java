package com.dotcms.mock.request;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * Creates a Mock of {@link HttpServletRequest} and has a {@link Map} for the attributes. Constructor
 * will iterate over the AttributeNames from the param request and will put them also in the class map.
 *
 * See an example here: {@link MockHttpRequest#MockHttpRequest(String, String)}
 */
public class MockAttributeRequest extends HttpServletRequestWrapper implements MockRequest {
    final Map<String, Object> attributes = new HashMap<>();

    public MockAttributeRequest(HttpServletRequest request) {
        super(request);
        Enumeration<String> attrs = request.getAttributeNames();
        while (attrs != null && attrs.hasMoreElements()) {
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

    @Override
    public void removeAttribute(String name) {
        attributes.remove(name);
    }

}
