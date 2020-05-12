package com.dotcms.storage;

import com.dotmarketing.util.Config;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liferay.util.FileUtil;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class JsonCompressorReader implements Closeable {

    private final InputStream input;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public JsonCompressorReader(final File file) throws IOException {

        final String compressor = Config
                .getStringProperty("CONTENT_METADATA_COMPRESSOR", "none");
        input                   = FileUtil.createInputStream(file.toPath(), compressor);
    }

    public <T> T read(final Class<T> valueType)
            throws IOException {

        return objectMapper.readValue(input, valueType);
    }

    @Override
    public void close() throws IOException {

        input.close();
    }

}
