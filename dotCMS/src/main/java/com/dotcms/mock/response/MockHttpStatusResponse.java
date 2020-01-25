package com.dotcms.mock.response;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.File;
import java.io.IOException;

public class MockHttpStatusResponse extends HttpServletResponseWrapper implements MockResponse {

    private int status = HttpServletResponse.SC_OK;

    public MockHttpStatusResponse(HttpServletResponse response) {
        super(response);
    }

    @Override
    public HttpServletResponse response() {
        return this;
    }

    @Override
    public void sendError(int status) throws IOException {
        this.status = status;
    }

    @Override
    public void setStatus(int status) {
        this.status = status;
    }

    @Override
    public int getStatus() {
        return status;
    }

}
