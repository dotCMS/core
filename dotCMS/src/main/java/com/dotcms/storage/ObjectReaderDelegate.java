package com.dotcms.storage;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Simple method to read an object into the stream
 * @author jsanca
 */
@FunctionalInterface
public interface ObjectReaderDelegate {

    /**
     * Writes the object into the stream
     * @param stream {@link InputStream}
     * @return  object {@link Object}
     */
    Object read(final InputStream stream);
}
