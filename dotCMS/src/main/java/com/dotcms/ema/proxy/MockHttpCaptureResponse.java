package com.dotcms.ema.proxy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import com.dotcms.mock.response.MockResponse;


/**
 * Proxy for HTTPServletResponse
 *
 */
public class MockHttpCaptureResponse extends HttpServletResponseWrapper implements MockResponse {

    final ByteArrayOutputStream bout = new ByteArrayOutputStream(4096*8);
    ServletOutputStream out = null;

    public MockHttpCaptureResponse(HttpServletResponse response) {
        super(response);
    }

    @Override
    public HttpServletResponse response() {
        return this;
    }


    @Override
    public ServletOutputStream getOutputStream() {
        if (out == null) {
            out = new MockServletOutputStream(bout);
        }
        return out;
    }

    public byte[] getBytes() {
        return bout.toByteArray();
    }

}
