package com.dotcms.graphql;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import com.dotcms.mock.response.MockResponse;


/**
 * Proxy for HTTPServletResponse
 *
 */
public class MockHttpCaptureResponse extends HttpServletResponseWrapper implements MockResponse {


    public StringWriter writer = new StringWriter();


    
    final HttpServletResponse originalResponse;
    public MockHttpCaptureResponse(HttpServletResponse response) {
        super(response);
        originalResponse=response;

    }


    @Override
    public HttpServletResponse response() {
        return this;
    }





    @Override
    public PrintWriter getWriter() throws IOException {

        return new PrintWriter(writer);
    }

}