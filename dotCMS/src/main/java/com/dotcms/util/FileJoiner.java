package com.dotcms.util;

import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.liferay.util.FileUtil;
import io.vavr.control.Try;

import java.io.Closeable;
import java.io.File;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Function;

/**
 * A File Joiner is a class take chucks of a file (they could be an actual file [previously splitted]) or a collection of byte chucks.
 * or both.
 * In case you are gonna use the file instead of the raw bytes, you should need to define a buffer previous by passing the chuck file of the
 * diff pieces (by default is 2mb) witch is the same default of the {@link FileSplitter} and {@link FileByteSplitter}
 * @author jsanca
 */
public class FileJoiner implements AutoCloseable, Flushable, Closeable {

    private final OutputStream outputStream;
    private final byte []      buffer;
    private final Function<File, InputStream> inputStreamCreatorFunction;

    /**
     * Defaut size for the chucks, 2mb
     */
    private static final int CHUNK_SIZE = Config.getIntProperty("FILE_SPLITTER_CHUCK_SIZE", 2097152); // 2mb


    private static InputStream createCompressorOutputStream (final File file) {

        final String compressor   = Config.getStringProperty("FILE_SPLITTER_COMPRESSOR", "gzip");
        return Try.of(()->com.liferay.util.FileUtil.createInputStream(file.toPath(), compressor)).getOrElseThrow(DotRuntimeException::new);
    }

    public FileJoiner(final File file) throws IOException {
        this (FileUtil.createOutputStream(file), FileJoiner::createCompressorOutputStream, CHUNK_SIZE);
    }

    public FileJoiner(final OutputStream outputStream) {
        this (outputStream, FileJoiner::createCompressorOutputStream, CHUNK_SIZE);
    }

    public FileJoiner(final OutputStream outputStream, final int bufferSize) {
        this (outputStream, FileJoiner::createCompressorOutputStream, bufferSize);
    }

    public FileJoiner(final OutputStream outputStream,
                      final Function<File, InputStream> inputStreamCreatorFunction,
                      final int bufferSize) {

        this.outputStream               = outputStream;
        this.inputStreamCreatorFunction = inputStreamCreatorFunction;
        this.buffer                     = bufferSize>0?new byte[bufferSize]:
                                            new byte[1]; // probably won't need any buffer to join.
    }

    public void join (final File file) {

        try (InputStream inputStream = this.inputStreamCreatorFunction.apply(file)) {

            int bytesRead = 0;
            while ((bytesRead = inputStream.read(this.buffer)) > 0) {

                this.join(this.buffer, 0, bytesRead);
            }
        } catch (IOException e) {

            throw new DotRuntimeException(e);
        }
    }

    public void join (final byte[] bytes, final int offset, final int length) {

        try {

            this.outputStream.write(bytes, offset, length);
        } catch (IOException e) {

            throw new DotRuntimeException(e);
        }
    }

    @Override
    public void close() throws IOException {

        if (null != this.outputStream) {

            this.outputStream.close();
        }
    }

    @Override
    public void flush() throws IOException {

        this.outputStream.flush();
    }
}
