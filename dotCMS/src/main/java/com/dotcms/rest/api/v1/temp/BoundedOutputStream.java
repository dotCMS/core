package com.dotcms.rest.api.v1.temp;

import java.io.IOException;
import java.io.OutputStream;

import com.dotmarketing.util.UtilMethods;

public class BoundedOutputStream extends OutputStream {

  private final long maxFileSize;
  private long currentFileSize = 0;
  private final OutputStream out;

  public BoundedOutputStream(final long maxFileSize, final OutputStream outputStream) throws IOException {
    this.out = outputStream;
    this.maxFileSize = maxFileSize;

  }

  @Override
  public void write(final byte[] bytes) throws IOException {
    checkSize(bytes.length);
    out.write(bytes);
  }

  @Override
  public void write(final int b) throws IOException {
    checkSize(1);
    out.write(b);

  }

  @Override
  public void write(final byte[] b, final int off, final int len) throws IOException {
    checkSize(len);
    out.write(b, off, len);
  }

  @Override
  public void close() throws IOException {
    out.close();
  }

  @Override
  public void flush() throws IOException {
    out.flush();
  }
  
  
  private void checkSize(final int len) throws IOException{
    this.currentFileSize += len;
    if (this.maxFileSize > 0 && this.currentFileSize > this.maxFileSize) {
      throw new IOException("File larger than allowed " + UtilMethods.prettyByteify(maxFileSize));
    }
  }
  
  
}
