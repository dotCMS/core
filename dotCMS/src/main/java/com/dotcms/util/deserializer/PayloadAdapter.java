package com.dotcms.util.deserializer;


import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.Visibility;
import com.dotcms.repackage.com.fasterxml.jackson.core.JsonParser;
import com.dotcms.repackage.com.fasterxml.jackson.core.JsonProcessingException;
import com.dotcms.repackage.com.fasterxml.jackson.databind.DeserializationContext;
import com.google.gson.*;
import com.liferay.portal.model.User;

import java.io.IOException;
import java.lang.reflect.Type;

import static com.dotcms.util.ReflectionUtils.getClassFor;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Json deserializer and serializer for {@link Payload} objects, the format of the json is the follow:<br>
 *
 * <code>
 * {
 *     type: #Payload's data fully qualified name
 *     visibility: #Payload's {@link Visibility}'s name
 *     visibilityValue: #Visibility value, this value is used by the {@link com.dotcms.api.system.event.PayloadVerifier}
 *     visibilityType: #Visibility value's data fully qualified name
 *     data: #Payload's data
 * }
 * </code>
 */
public class PayloadAdapter implements JsonDeserializer<Payload>,JsonSerializer<Payload> {

    public static final String TYPE = "type";
    public static final String VISIBILITY = "visibility";
    public static final String VISIBILITY_VALUE = "visibilityValue";
    public static final String VISIBILITY_TYPE = "visibilityType";
    public static final String DATA = "data";

    @Override
    public Payload deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = null;
        String payloadType = null;
        Payload payload = null;
        Class clazz = null;
        Object payloadData = null;
        Visibility visibility = null;
        String visibilityName = null;
        Object visibilityValue = null;

        if (null != json) {

            jsonObject = json.getAsJsonObject();

            if (null != jsonObject && jsonObject.has(TYPE)) {

                payloadType = jsonObject.getAsJsonPrimitive
                        (TYPE).getAsString();

                if(jsonObject.has(VISIBILITY)) {
                    visibilityName = jsonObject.getAsJsonPrimitive
                            (VISIBILITY).getAsString();
                }

                if(jsonObject.has(VISIBILITY_VALUE)) {
                    String visibilityType = jsonObject.getAsJsonPrimitive
                            (VISIBILITY_TYPE).getAsString();
                    visibilityValue = context.deserialize(jsonObject.get(VISIBILITY_VALUE), getClassFor(visibilityType));
                }

                if (null != visibilityName) {

                    visibility =
                            Visibility.valueOf(visibilityName);
                }

                clazz = getClassFor(payloadType);
                payloadData = context.deserialize(jsonObject.get(DATA), clazz);
                payload = new Payload(payloadData, visibility, visibilityValue);
            }
        }

        return payload;
    }

    @Override
    public JsonElement serialize(Payload payload, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject jsonElement = new JsonObject();
        jsonElement.addProperty(TYPE, payload.getRawData().getClass().getName());
        jsonElement.addProperty(VISIBILITY, payload.getVisibility().name());
        Object visibilityValue = payload.getVisibilityValue();

        if (visibilityValue != null) {
            jsonElement.add(VISIBILITY_VALUE,  jsonSerializationContext.serialize(visibilityValue));
            jsonElement.addProperty(VISIBILITY_TYPE, visibilityValue.getClass().getName());
        }

        jsonElement.add(DATA, jsonSerializationContext.serialize(payload.getRawData()));
        return jsonElement;
    }
}
