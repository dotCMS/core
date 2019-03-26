package com.dotcms.mock.request;

import javax.servlet.http.HttpServletRequest;
import org.mockito.Mockito;

/**
 * Primary call to {@link Mockito} in order to create the {@link HttpServletRequest}
 *
 * <p>See an example here: {@link MockHttpRequest#MockHttpRequest(String, String)}
 */
public class BaseRequest implements MockRequest {

  public HttpServletRequest request() {
    return Mockito.mock(HttpServletRequest.class);
  }
}
