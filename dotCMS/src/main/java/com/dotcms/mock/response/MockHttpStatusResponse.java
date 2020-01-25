package com.dotcms.mock.response;

import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

public class MockHttpStatusResponse extends HttpServletResponseWrapper implements MockResponse {

    private int status = HttpServletResponse.SC_OK;

    public MockHttpStatusResponse(final HttpServletResponse response) {
        super(response);
    }

    @Override
    public HttpServletResponse response() {
        return this;
    }

    @Override
    public void sendError(final int status) throws IOException {
        this.status = status;
    }

    @Override
    public void setStatus(final int status) {
        this.status = status;
    }

    @Override
    public int getStatus() {
        return status;
    }

}
