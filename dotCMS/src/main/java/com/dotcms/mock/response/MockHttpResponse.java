package com.dotcms.mock.response;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * Proxy for HTTPServletResponse
 *
 */
public class MockHttpResponse extends HttpServletResponseWrapper implements MockResponse {

    final HttpServletResponse base;

    public MockHttpResponse(HttpServletResponse response) {
        super(response);
        base = response;
    }


    @Override
    public HttpServletResponse response() {
        return base;
    }


}
