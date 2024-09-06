package com.dotcms.contenttype.model.field;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;

/**
 * The RelationshipCardinalityOrdinalSerializer class is a JsonSerializer implementation that
 * serializes a RelationshipCardinality enum value by writing its ordinal value as a number.
 */
public class RelationshipCardinalityOrdinalSerializer extends
        JsonSerializer<RelationshipCardinality> {

    @Override
    public void serialize(RelationshipCardinality value, JsonGenerator gen,
            SerializerProvider serializers) throws IOException {

        gen.writeNumber(value.ordinal());
    }

}