package com.dotcms.mock.response;

import com.dotcms.mock.request.MockHttpRequest;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * Mock {@link HttpServletRequest} with a {@link Map} as part of the class that will contain the
 * Headers.
 *
 * See an example here: {@link MockHttpRequest#MockHttpRequest(String, String)}
 */
public class MockHeaderResponse extends HttpServletResponseWrapper implements MockResponse {
  final Map<String, String> headers = new HashMap<String, String>();

  public MockHeaderResponse(HttpServletResponse response) {
    super(response);
    if (response.getHeaderNames() != null) {
      Collection<String> oldHeaders = response.getHeaderNames();
      oldHeaders.forEach(header->{
        this.headers.put(header, response.getHeader(header));
      });
    }
  }

  public MockHeaderResponse(HttpServletResponse response, final String key, final String value) {
    this(response);
    headers.put(key, value);
  }

  public HttpServletResponse response() {
    return this;
  }

  @Override
  public String getHeader(String name) {
    return headers.get(name);
  }

  @Override
  public Collection<String> getHeaderNames() {
    return headers.keySet();
  }

  public void setHeader(final String name, final String o) {
    headers.put(name, o);
  }

  public void addHeader(final String name, final String o) {
    headers.put(name, o);
  }

  @Override
  public Collection<String> getHeaders(final String name) {
    return super.getHeaders(name);
  }
}
