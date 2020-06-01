package com.dotcms.util;

import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.FileUtil;
import com.liferay.util.StringPool;
import io.vavr.control.Try;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * This {@link Iterable} receives a file and split in into a smaller files.
 * @author jsanca
 */
public class FileSplitter implements Iterable<File>, AutoCloseable, Closeable {

    /**
     * This function receives the original file and the current chuck index, will return a new file to store the chuck.
     */
    private final BiFunction<String, Integer, File> createSubFileFunction;

    /**
     * Delegates on this function the creation of the output stream to store into the chuck file
     */
    private final Function<File, OutputStream> outputStreamCreatorFunction;

    /**
     * File that wants to be split
     */
    private final File file;

    /**
     * Size for the chunk files.
     */
    private final int chunkSize;

    private Closeable closeable;

    /**
     * Defaut size for the chunks, 2mb
     */
    private static final int CHUNK_SIZE = Config.getIntProperty("FILE_SPLITTER_CHUNK_SIZE", 2097152); // 2mb

    public FileSplitter(final File file) {

        this (file, CHUNK_SIZE);
    }

    public FileSplitter(final File file, final int chunkSize) {

        this (file, chunkSize, FileSplitter::createTempSubFile);
    }

    public FileSplitter(final File file, final int chunkSize, final BiFunction<String, Integer, File> createSubFileFunction) {

        this (file, chunkSize, createSubFileFunction, FileSplitter::createCompressorOutputStream);
    }

    private static File createTempSubFile (final String originalFatFileName, final int chunkIndex) {

        return Try.of(()->FileUtil.createTemporalFile( // todo: use a pattern from config
                originalFatFileName + StringPool.DASH + chunkIndex, ".bin")).getOrElseThrow(DotRuntimeException::new);  // get a pattern from config
    }

    private static OutputStream createCompressorOutputStream (final File file) {

        final String compressor   = Config.getStringProperty("FILE_SPLITTER_COMPRESSOR", "gzip");
        return Try.of(()->com.liferay.util.FileUtil.createOutputStream(file.toPath(), compressor)).getOrElseThrow(DotRuntimeException::new);
    }

    public FileSplitter(final File file, final int chunkSize,
                        final BiFunction<String, Integer, File> createSubFileFunction,
                        final Function<File, OutputStream> outputStreamCreatorFunction) {

        this.createSubFileFunction = createSubFileFunction;
        this.outputStreamCreatorFunction = outputStreamCreatorFunction;
        this.file = file;
        this.chunkSize = chunkSize;
    }

    @Override
    public Iterator<File> iterator() {

        final FileSplitterIterator iterator = new FileSplitterIterator();
        this.closeable = iterator;
        return iterator;
    }

    @Override
    public void close() throws IOException {

        this.closeable.close();
    }

    private class FileSplitterIterator implements Iterator<File>, Closeable {

        private int               index;
        private int               cursor;
        private final InputStream stream;
        private final long        fileLength;
        private final byte[]      chunkbuffer;

        public FileSplitterIterator() {

            this.index       = 0;
            this.cursor      = 0;
            this.fileLength  = file.length();
            this.stream      = Try.of(()->com.liferay.util.FileUtil.createInputStream
                    (file.toPath(), "none")).getOrElseThrow(DotRuntimeException::new);
            this.chunkbuffer = new byte[FileSplitter.this.chunkSize];
        }

        @Override
        public boolean hasNext() {
            return this.cursor < this.fileLength;
        }

        @Override
        public File next() {

            if (this.hasNext()) {
                // check byte count from cursor...
                final int readBytes  = Try.of(()->this.stream.read(this.chunkbuffer)).getOrElseThrow(DotRuntimeException::new);
                final File chunkFile = FileSplitter.this.createSubFileFunction.apply(file.getName(), this.index);  // this file should have the chunk in the file and readBytes
                try (OutputStream outputStream = FileSplitter.this.outputStreamCreatorFunction.apply(chunkFile)) {

                    outputStream.write(this.chunkbuffer, 0, readBytes);
                    outputStream.flush();
                } catch (IOException e) {

                    throw new DotRuntimeException(e);
                }

                this.cursor         += readBytes;
                this.index++;

                return chunkFile;
            }

            throw new NoSuchElementException();
        }

        @Override
        public void close() throws IOException {
            if (null != this.stream) {
                this.stream.close();
            }
        }
    }
}
