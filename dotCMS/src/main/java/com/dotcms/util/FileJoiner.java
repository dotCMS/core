package com.dotcms.util;

import java.io.Closeable;
import java.io.File;
import java.io.Flushable;

/**
 * Abstraction of the FileJoiner, allows to join files or binary block into one single file (or similar)
 * @author jsanca
 */
public interface FileJoiner extends AutoCloseable, Flushable, Closeable {

    /**
     * Join the whole file
     * @param file
     */
    void join(File file);

    /**
     * Join a binary block
     * @param bytes
     * @param offset
     * @param length
     */
    void join(byte[] bytes, int offset, int length);
}
