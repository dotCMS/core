package com.dotcms.storage;

import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;

/**
 * Simple implementation that basically converts the object into a json
 *
 * @author jsanca
 */
public class JsonWriterDelegate implements ObjectWriterDelegate {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Delegate writer
     *
     * @param object {@link Serializable}
     */
    @Override
    public void write(final OutputStream out, final Serializable object) throws IOException {

        this.objectMapper.writeValue(out, object);

    }
}
