package com.dotcms.contenttype.model.field;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;

/**
 * The RelationshipCardinalityDeserializer class is a custom deserializer for JSON data representing
 * the cardinality of a relationship between entities. It extends the JsonDeserializer class and
 * provides a method for deserializing the JSON data into a RelationshipCardinality object.
 * <p>
 * The deserialize() method takes a JsonParser and a DeserializationContext as parameters and
 * returns a RelationshipCardinality object. The method first checks the type of the current token
 * in the JSON data. If it is a numeric value, it calls the RelationshipCardinality.fromOrdinal()
 * method to get the corresponding RelationshipCardinality enum constant. If it is a scalar value,
 * it calls the RelationshipCardinality.fromName() method to get the corresponding
 * RelationshipCardinality enum constant. If the token type is neither numeric nor scalar, an
 * IOException is thrown with an error message stating the unexpected token type.
 *
 * @throws IOException if an I/O error occurs during deserialization or the JSON data has an
 * unexpected format
 */
public class RelationshipCardinalityDeserializer extends
        JsonDeserializer<RelationshipCardinality> {

    @Override
    public RelationshipCardinality deserialize(
            JsonParser p, DeserializationContext ctxt) throws IOException {

        if (p.getCurrentToken().isNumeric()) {
            return RelationshipCardinality.fromOrdinal(p.getIntValue());
        } else if (p.getCurrentToken().isScalarValue()) {
            return RelationshipCardinality.fromName(p.getText());
        } else {
            throw new IOException("Unexpected token type: " + p.getCurrentToken());
        }
    }

}