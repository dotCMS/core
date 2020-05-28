package com.dotcms.storage;

import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;

/**
 * Simple implementation that basically converts the object into a json
 * @author jsanca
 */
public class JsonWriterDelegate implements ObjectWriterDelegate {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public JsonWriterDelegate() {
    }

    @Override
    public void write(final OutputStream out, final Serializable object) {
        //Sneaky.sneaked(()-> objectMapper.writeValue(out, object));
        try {
            this.objectMapper.writeValue(out, object);
        } catch (IOException e) {
            Logger.error(this, e.getMessage(), e);
        }
    }
}
