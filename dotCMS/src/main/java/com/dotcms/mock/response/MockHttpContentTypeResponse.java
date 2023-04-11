package com.dotcms.mock.response;

import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

public class MockHttpContentTypeResponse extends HttpServletResponseWrapper implements MockResponse {

    private String contentType;

    public MockHttpContentTypeResponse(final HttpServletResponse response) {
        super(response);
    }

    @Override
    public HttpServletResponse response() {
        return this;
    }

    @Override
    public void setContentType(final String contentType) {
        this.contentType = contentType;
    }

    @Override
    public String getContentType() {
        return this.contentType;
    }

}