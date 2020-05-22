package com.dotcms.util.user;

import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.FileUtil;
import com.liferay.util.StringPool;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * This {@link Iterable} receives a file and split in into a smaller files (as byte array).
 * Each iteration returns the array with the bytes and the number of bytes read
 * @author jsanca
 */
public class FileByteSplitter implements Iterable<Tuple2<byte[], Integer>>, AutoCloseable, Closeable {

    /**
     * File that wants to be split
     */
    private final File file;

    /**
     * Size for the chuck files.
     */
    private final int chuckSize;

    private Closeable closeable;

    /**
     * Defaut size for the chucks, 2mb
     */
    private static final int CHUNK_SIZE = Config.getIntProperty("FILE_SPLITTER_CHUCK_SIZE", 2097152); // 2mb

    public FileByteSplitter(final File file) {

        this (file, CHUNK_SIZE);
    }

    public FileByteSplitter(final File file, final int chuckSize) {

        this.file = file;
        this.chuckSize = chuckSize;
    }

    @Override
    public Iterator<Tuple2<byte[], Integer>> iterator() {

        final FileSplitterIterator iterator = new FileSplitterIterator();
        this.closeable = iterator;
        return iterator;
    }

    @Override
    public void close() throws IOException {

        this.closeable.close();
    }

    private class FileSplitterIterator implements Iterator<Tuple2<byte[], Integer>>, Closeable {

        private int               cursor;
        private final InputStream stream;
        private final long        fileLength;
        private final byte[]      chuckbuffer;

        public FileSplitterIterator() {

            this.cursor      = 0;
            this.fileLength  = file.length();
            this.stream      = Try.of(()->com.liferay.util.FileUtil.createInputStream
                    (file.toPath(), "none")).getOrElseThrow(DotRuntimeException::new);
            this.chuckbuffer = new byte[FileByteSplitter.this.chuckSize];
        }

        @Override
        public boolean hasNext() {
            return this.cursor < this.fileLength;
        }

        @Override
        public Tuple2<byte[], Integer> next() {

            if (this.hasNext()) {
                // check byte count from cursor...
                final int readBytes  = Try.of(()->this.stream.read(this.chuckbuffer)).getOrElseThrow(DotRuntimeException::new);
                this.cursor         += readBytes;

                return Tuple.of(this.chuckbuffer, readBytes);
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
