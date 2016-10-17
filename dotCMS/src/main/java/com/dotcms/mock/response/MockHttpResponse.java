package com.dotcms.mock.response;

import javax.servlet.http.HttpServletResponse;

/**
 * Proxy for HTTPServletResponse
 *
 */
public class MockHttpResponse implements MockResponse {

    final HttpServletResponse base;

    public MockHttpResponse(HttpServletResponse response) {
        base = response;
    }


    @Override
    public HttpServletResponse response() {
        return base;
    }


}
