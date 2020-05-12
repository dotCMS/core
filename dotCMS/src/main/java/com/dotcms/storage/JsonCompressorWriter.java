package com.dotcms.storage;

import com.dotmarketing.util.Config;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liferay.util.FileUtil;

import java.io.Closeable;
import java.io.File;
import java.io.Flushable;
import java.io.IOException;
import java.io.OutputStream;

public class JsonCompressorWriter implements Closeable, Flushable {

    private final OutputStream out;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public JsonCompressorWriter(final File file) throws IOException {

        final String compressor = Config
                .getStringProperty("CONTENT_METADATA_COMPRESSOR", "none");
        this.prepareParent(file);
        this.out =  FileUtil.createOutputStream(file.toPath(), compressor);
    }

    private void prepareParent(final File file) {

        if (!file.getParentFile().exists()) {

            file.getParentFile().mkdirs();
        }
    }

    public JsonCompressorWriter write (final Object object) throws IOException {

        objectMapper.writeValue(out, object);
        return this;
    }

    @Override
    public void flush() throws IOException {

        out.flush();
    }

    @Override
    public void close() throws IOException {

        out.close();
    }
}
