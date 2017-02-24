package com.dotcms.mock.response;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;

import com.dotmarketing.business.DotStateException;

/**
 * A mock Outputstream that will write the output of a servlet to a fileoutputstream
 * 
 * @author will
 *
 */
class MockServletOutputStream extends ServletOutputStream {

  public MockServletOutputStream(File destPath) {
    try {
      fos = new FileOutputStream(destPath);
    } catch (FileNotFoundException e) {
      throw new DotStateException(e.getMessage(), e);
    }

  }

  FileOutputStream fos = null;

  @Override
  public void close() throws IOException {
    super.close();
    fos.close();

  }

  @Override
  public void flush() throws IOException {
    super.flush();
    fos.flush();

  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    fos.write(b, off, len);
  }

  @Override
  public void write(int b) throws IOException {
    fos.write(b);
  }

  @Override
  public boolean isReady() {
    return false;
  }

  @Override
  public void setWriteListener(WriteListener writeListener) {

  }
}

