package com.dotcms.mock.request;

import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * Mock {@link HttpServletRequest} with a {@link Map} as part of the class that will contain the
 * Headers.
 *
 * See an example here: {@link MockHttpRequest#MockHttpRequest(String, String)}
 */
public class MockHeaderRequest extends HttpServletRequestWrapper implements MockRequest {
  // header names are case-insensitive per the servlet spec, so a plain HashMap made lookups
  // such as getHeader("host") miss a header that was stored as "Host"
  final Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);


  public MockHeaderRequest(HttpServletRequest request) {
    super(request);
    if (request.getHeaderNames() != null) {
      Enumeration<String> oldHeaders = request.getHeaderNames();
      while (oldHeaders.hasMoreElements()) {
        String param = oldHeaders.nextElement();
        this.headers.put(param, request.getHeader(param));
      }
    }
  }

  public MockHeaderRequest(HttpServletRequest request, final String key, final String value) {
    this(request);
    headers.put(key, value);
  }

  public HttpServletRequest request() {
    return this;
  }

  @Override
  public String getHeader(String name) {

    return headers.get(name);
  }

  @Override
  public Enumeration<String> getHeaderNames() {
    return new Vector<String>(headers.keySet()).elements();
  }


  public void setHeader(final String name, final String o) {
    headers.put(name, o);
  }

  public MockHeaderRequest addHeader(final String name, final String o) {
    headers.put(name, o);
    return this;
  }

  @Override
  public Enumeration<String> getHeaders(final String name) {

    return super.getHeaders(name);
  }


}
