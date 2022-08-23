package com.dotcms.storage;

import com.dotcms.util.FileJoiner;
import com.dotcms.util.FileJoinerImpl;
import com.dotmarketing.util.Logger;
import io.vavr.Lazy;
import org.apache.commons.lang3.mutable.MutableBoolean;

import java.io.File;
import java.io.IOException;

/**
 * Instead of joining into a file will join into a bytes stream
 * The joining in memory will be limited by a given size
 * @author jsanca
 */
public class BinaryBlockFileJoinerImpl implements FileJoiner {

    private final byte [] buffer;
    private final File file;
    private final Lazy<FileJoinerImpl> innerFileJoiner = Lazy.of(()->this.createFileJoinerImpl());
    private final MutableBoolean flushed = new MutableBoolean(false);
    private int bufferIndex = 0;

    private FileJoinerImpl createFileJoinerImpl() {

        try {
            return new FileJoinerImpl(this.file);
        } catch (IOException e) {
            Logger.error(this, e.getMessage(), e);
            return null;
        }
    }

    public BinaryBlockFileJoinerImpl(final File file, final int maxSize) throws IOException {
        this.file = file;
        this.buffer  = new byte[maxSize];
    }


    @Override
    public void join(File file) {

        this.innerFileJoiner.get().join(this.buffer, 0, this.buffer.length);
    }

    @Override
    public void join(final byte[] bytes, final int offset, final int length) {

        int nextSize = this.bufferIndex + length;

        if (nextSize >= this.buffer.length || this.bufferIndex == Integer.MAX_VALUE) {
            if (this.flushed.isFalse()) { // not flushed yet

                this.innerFileJoiner.get().join(this.buffer, 0, this.bufferIndex); // flush
                this.flushed.setValue(true);
                this.bufferIndex = Integer.MAX_VALUE;
            }

            this.innerFileJoiner.get().join(bytes, offset, length);
        } else {

            for (int i = offset; i < length; ++i) {

                this.buffer[this.bufferIndex++] = bytes[i];
            }
        }
    }

   public byte [] getBytes() {
        return this.flushed.isFalse()?this.buffer:null;
   }

    @Override
    public void flush() throws IOException {

        this.innerFileJoiner.get().flush();
    }

    @Override
    public void close() throws IOException {

        this.innerFileJoiner.get().close();
    }
}
