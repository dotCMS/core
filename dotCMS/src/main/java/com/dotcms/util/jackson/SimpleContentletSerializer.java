package com.dotcms.util.jackson;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.vavr.control.Try;
import java.io.IOException;

/**
 * This is simplified serializer
 * only includes whatever can be accessed via getMap + isArchived + isLowIndexPriority
 * This serializer has a purpose.
 * We could have let the Marshaller serialize the whole thing but that has implications
 * A few examples:
 *    Calling Contentlet.islive can easily break the serialization if no VersionInfo is Set.
 *    Calling Contentlet.getTitleImage makes use of the metadatda which forces the generation of it earlier that we expect.
 */
public class SimpleContentletSerializer extends JsonSerializer<Contentlet> {

    /**
     * simplified serialize
     * @param value
     * @param generator
     * @param provider
     * @throws IOException
     */
    @Override
    public void serialize(final Contentlet value, final JsonGenerator generator, final SerializerProvider provider)
            throws IOException {
        generator.writeObject(value.getMap());
        generator.writeBooleanField("lowIndexPriority",value.isLowIndexPriority());
        generator.writeBooleanField(Contentlet.ARCHIVED_KEY, Try.of(value::isArchived).getOrElse(false));
    }
}
