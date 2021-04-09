package com.dotcms.mock.response;

import java.io.File;
import java.io.OutputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;


/**
 * Proxy for HTTPServletResponse
 *
 */
public class MockHttpCaptureResponse extends HttpServletResponseWrapper implements MockResponse {

    final HttpServletResponse base;
    final OutputStream outputStream;
    ServletOutputStream out = null;
    public MockHttpCaptureResponse(final HttpServletResponse response, final OutputStream outputStream) {
        super(response);
        base = response;
        this.outputStream = outputStream;
    }


    @Override
    public HttpServletResponse response() {
        return this;
    }
    
    
    @Override
    public ServletOutputStream getOutputStream(){
      if(out==null){
          out =  new MockServletOutputStream(outputStream);
      }
      return out;
      
    }

    
    
}
