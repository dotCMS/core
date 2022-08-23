package com.dotcms.storage.repository;

import java.io.File;

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
