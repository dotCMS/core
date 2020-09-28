package com.dotcms.storage;

import java.io.OutputStream;
import java.io.Serializable;

/**
 * Simple method to write an object into the stream
 * @author jsanca
 */
@FunctionalInterface
public interface ObjectWriterDelegate {

    /**
     * Writes the object into the stream
     * @param stream {@link OutputStream}
     * @param object {@link Serializable}
     */
    void write(final OutputStream stream, Serializable object);
}
