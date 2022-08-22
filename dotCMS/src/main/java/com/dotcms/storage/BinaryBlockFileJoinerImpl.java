package com.dotcms.storage;

import com.dotcms.util.FileJoiner;
import com.dotcms.util.FileJoinerImpl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Function;

/**
 * Instead of joining into a file will join into a bytes stream
 * The joining in memory will be limited by a given size
 * @author jsanca
 */
public class BinaryBlockFileJoinerImpl extends FileJoinerImpl implements FileJoiner {

    private byte [] buffer;
    private int bufferIndex = 0;

    public BinaryBlockFileJoinerImpl(final File file, final int maxSize) throws IOException {
        super(file);
        this.buffer  = new byte[maxSize];
    }

    public BinaryBlockFileJoinerImpl(OutputStream outputStream, final int maxSize) {
        super(outputStream);
        this.buffer  = new byte[maxSize];
    }

    public BinaryBlockFileJoinerImpl(OutputStream outputStream, int bufferSize, final int maxSize) {
        super(outputStream, bufferSize);
        this.buffer  = new byte[maxSize];
    }

    public BinaryBlockFileJoinerImpl(OutputStream outputStream, Function<File, InputStream> inputStreamCreatorFunction, int bufferSize, final int maxSize) {
        super(outputStream, inputStreamCreatorFunction, bufferSize);
        this.buffer  = new byte[maxSize];
    }

    @Override
    public void join(final byte[] bytes, final int offset, final int length) {

        int nextSize = this.bufferIndex + length;

        if (nextSize >= bytes.length) {
            if (null != this.buffer) {

                super.join(this.buffer, 0, this.buffer.length);
                this.buffer = null;
            }

            super.join(bytes, offset, length);
        } else {

            for (int i = offset; i < length; ++i) {

                this.buffer[this.bufferIndex++] = bytes[i];
            }
        }
    }

   public byte [] getBytes() {
        return this.buffer;
   }
}
