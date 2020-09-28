package com.dotcms.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rainerhahnekamp.sneakythrow.Sneaky;

import java.io.IOException;
import java.io.InputStream;

/**
 * Simple implementation that basically converts the input stream into a json
 * @param <T>
 */
public class JsonReaderDelegate<T> implements ObjectReaderDelegate {

    private final Class<T>     valueType;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * costructor
     * @param valueType
     */
    public JsonReaderDelegate(final Class<T> valueType) {

        this.valueType = valueType;
    }

    /**
     * Delegate reader
     * @param stream {@link InputStream}
     * @return
     */
    @Override
    public T read(final InputStream stream) {
        return Sneaky.sneaked(()-> objectMapper.readValue(stream, valueType)).get();
    }
}
