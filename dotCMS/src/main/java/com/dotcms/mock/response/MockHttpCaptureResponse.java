package com.dotcms.mock.response;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;


/**
 * Proxy for HTTPServletResponse
 *
 */
public class MockHttpCaptureResponse extends HttpServletResponseWrapper implements MockResponse {

    final HttpServletResponse base;
    final File file;
    ServletOutputStream out = null;
    public MockHttpCaptureResponse(HttpServletResponse response, File file) {
        super(response);
        base = response;
        this.file = file;
    }


    @Override
    public HttpServletResponse response() {
        return this;
    }
    
    
    @Override
    public ServletOutputStream getOutputStream(){
      if(out==null){
          out =  new MockServletOutputStream(file);
      }
      return out;
      
    }

    
    
}
