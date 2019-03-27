package com.dotcms.rest;

import com.dotcms.repackage.javax.ws.rs.WebApplicationException;
import com.dotcms.repackage.javax.ws.rs.core.StreamingOutput;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;

/**
 * This is a custom implementation of a {@link StreamingOutput} that delegates the work of marshalling to non-repackage jackson.
 * @author jsanca
 */
public class DotStreamingOutput<T> implements StreamingOutput {

    private final T entity;
    private final ObjectMapper objectMapper;


    public DotStreamingOutput(final T entity, final ObjectMapper objectMapper) {

        this.entity  = entity;
        this.objectMapper = objectMapper;
    }

    @Override
    public void write(final OutputStream outputStream) throws IOException, WebApplicationException {

        this.objectMapper.writeValue (outputStream, entity);
    } // write.
} // E:O:F:DotStreamingOutput.
