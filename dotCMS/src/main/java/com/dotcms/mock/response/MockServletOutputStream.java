package com.dotcms.mock.response;

import com.dotmarketing.business.DotStateException;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;

/**
 * A mock Outputstream that will write the output of a servlet to a outputstream
 * 
 * @author will
 *
 */
class MockServletOutputStream extends ServletOutputStream {

  public MockServletOutputStream(File destPath) {
    try {
      os = Files.newOutputStream(destPath.toPath());
    } catch (IOException e) {
      throw new DotStateException(e.getMessage(), e);
    }

  }

  public MockServletOutputStream(OutputStream os) {
      this.os = os;

  }

  OutputStream os = null;

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

