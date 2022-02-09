package com.dotcms.util.marshal;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.SqlTimeSerializer;
import java.io.IOException;

/**
 * Serialize java.sql.Time as a long
 */
public class SqlTimeStampSerializer extends SqlTimeSerializer {

    /**
     * SQL Time as timestamp serializer
     * @param value
     * @param generator
     * @param provider
     * @throws IOException
     */
    @Override
    public void serialize(final java.sql.Time value, final JsonGenerator generator, final SerializerProvider provider) throws IOException {
        generator.writeNumber(value.getTime());
    }

}
