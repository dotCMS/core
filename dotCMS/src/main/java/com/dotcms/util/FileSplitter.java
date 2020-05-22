package com.dotcms.util;

import com.dotcms.util.user.FileJoiner;
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
     * Size for the chuck files.
     */
    private final int chuckSize;

    private Closeable closeable;

    /**
     * Defaut size for the chucks, 2mb
     */
    private static final int CHUNK_SIZE = Config.getIntProperty("FILE_SPLITTER_CHUCK_SIZE", 2097152); // 2mb

    public FileSplitter(final File file) {

        this (file, CHUNK_SIZE);
    }

    public FileSplitter(final File file, final int chuckSize) {

        this (file, chuckSize, FileSplitter::createTempSubFile);
    }

    public FileSplitter(final File file, final int chuckSize, final BiFunction<String, Integer, File> createSubFileFunction) {

        this (file, chuckSize, createSubFileFunction, FileSplitter::createCompressorOutputStream);
    }

    private static File createTempSubFile (final String originalFatFileName, final int chuckIndex) {

        return Try.of(()->FileUtil.createTemporalFile( // todo: use a pattern from config
                originalFatFileName + StringPool.DASH + chuckIndex, ".bin")).getOrElseThrow(DotRuntimeException::new);  // get a pattern from config
    }

    private static OutputStream createCompressorOutputStream (final File file) {

        final String compressor   = Config.getStringProperty("FILE_SPLITTER_COMPRESSOR", "gzip");
        return Try.of(()->com.liferay.util.FileUtil.createOutputStream(file.toPath(), compressor)).getOrElseThrow(DotRuntimeException::new);
    }

    public FileSplitter(final File file, final int chuckSize,
                        final BiFunction<String, Integer, File> createSubFileFunction,
                        final Function<File, OutputStream> outputStreamCreatorFunction) {

        this.createSubFileFunction = createSubFileFunction;
        this.outputStreamCreatorFunction = outputStreamCreatorFunction;
        this.file = file;
        this.chuckSize = chuckSize;
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
        private final byte[]      chuckbuffer;

        public FileSplitterIterator() {

            this.index       = 0;
            this.cursor      = 0;
            this.fileLength  = file.length();
            this.stream      = Try.of(()->com.liferay.util.FileUtil.createInputStream
                    (file.toPath(), "none")).getOrElseThrow(DotRuntimeException::new);
            this.chuckbuffer = new byte[FileSplitter.this.chuckSize];
        }

        @Override
        public boolean hasNext() {
            return this.cursor < this.fileLength;
        }

        @Override
        public File next() {

            if (this.hasNext()) {
                // check byte count from cursor...
                final int readBytes  = Try.of(()->this.stream.read(this.chuckbuffer)).getOrElseThrow(DotRuntimeException::new);
                final File chuckFile = FileSplitter.this.createSubFileFunction.apply(file.getName(), this.index);  // this file should have the chuck in the file and readBytes
                try (OutputStream outputStream = FileSplitter.this.outputStreamCreatorFunction.apply(chuckFile)) {

                    outputStream.write(this.chuckbuffer, 0, readBytes);
                    outputStream.flush();
                } catch (IOException e) {

                    throw new DotRuntimeException(e);
                }

                this.cursor         += readBytes;
                this.index++;

                return chuckFile;
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

    public static void main(String[] args) {

        List<File> files = new ArrayList<>();
        File file = new File ("/Users/jsanca/Downloads/[JAVA][Beginning Java 8 Games Development].pdf" );

        try (FileSplitter fileSplitter = new FileSplitter(file, 1048576, // 1 mb
                (filename, index)-> new File("/Users/jsanca/Downloads/tmp/" + index + "-" + filename + ".bin"))) {

            for (final File partitionFile : fileSplitter) {

                files.add(partitionFile);
                System.out.println(partitionFile.getAbsolutePath());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (final FileJoiner fileJoiner = new FileJoiner(new File("/Users/jsanca/Downloads/tmp/ensambled.pdf"))){
            for (final File chuckFile : files) {

                fileJoiner.join(chuckFile);
                fileJoiner.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("DONE");
        System.exit(0);
    }
}
