package com.dotcms.mock.request;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;


/**
 * 
 * Mocks a full featured request to a specific host / resource
 *
 */
public class MockServletPathRequest extends HttpServletRequestWrapper implements MockRequest {


  final String servletPath;
  public MockServletPathRequest(HttpServletRequest request, String servletPath) {
      super(request);
      this.servletPath =servletPath;
  }

  public HttpServletRequest request() {
      return this;
  }

  @Override
  public String getServletPath() {
    return servletPath;
  }




}
