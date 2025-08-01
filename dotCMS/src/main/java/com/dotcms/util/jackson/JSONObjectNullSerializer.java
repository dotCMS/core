package com.dotcms.util.jackson;

import com.dotmarketing.util.json.JSONObject;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;

/**
 * Why do we need this??
 * Because we still have two JSON frameworks coexisting
 * KeyValues are Read using JSONObject then they are saved using jackson in the contentlet as Json
 * At save time Jackson does not know how to deal with the null marker and throws an exception
 * And that is why we need this here
 */
public class JSONObjectNullSerializer extends JsonSerializer<Object> {

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers)
            throws IOException {
        // Check if the object is the Null marker
        if (isNullMarker(value)) {
            gen.writeNull();
        }
    }

    /**
     * Check if the object is a Null marker
     * This method identifies the Null marker by checking:
     */
    private boolean isNullMarker(Object value) {
        if (value == null) {
            return false;
        }

        return value == JSONObject.NULL;

    }

}
