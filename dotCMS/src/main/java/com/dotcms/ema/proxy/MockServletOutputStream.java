package com.dotcms.ema.proxy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;

/**
 * A mock Outputstream that will write the output of a servlet to a outputstream
 * 
 * @author will
 *
 */
class MockServletOutputStream extends ServletOutputStream {
    final OutputStream os;
    
    public MockServletOutputStream(ByteArrayOutputStream captureMe) {
        os = captureMe;
    }
    



    @Override
    public void close() throws IOException {
        super.close();
        os.close();

    }

    @Override
    public void flush() throws IOException {
        super.flush();
        os.flush();

    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        os.write(b, off, len);
    }

    @Override
    public void write(int b) throws IOException {
        os.write(b);
    }

    @Override
    public boolean isReady() {
        return false;
    }

    @Override
    public void setWriteListener(WriteListener writeListener) {

    }
}

