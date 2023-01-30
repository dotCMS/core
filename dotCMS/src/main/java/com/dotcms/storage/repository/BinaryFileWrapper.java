package com.dotcms.storage.repository;

import java.io.File;

/**
 * This implementation of a File, make the File object as state pattern
 * - one state is the default File implementation
 * - the other state is a chunk of bytes
 *
 * This pattern allows to cache in memory, the small files; so upper layers may decide if reads the whole file from the file system
 * or just get the mem buffer, this avoids cpu cycles and I/O
 * @author jsanca
 */
public class BinaryFileWrapper extends File {

    private final byte[] bufferByte;
    public BinaryFileWrapper(final File file, final byte[] bufferByte) {
        super(file.getPath());
        this.bufferByte = bufferByte;
    }

    public byte[] getBufferByte() {
        return bufferByte;
    }
}
