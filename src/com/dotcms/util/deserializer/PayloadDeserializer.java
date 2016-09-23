package com.dotcms.util.deserializer;


import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.Visibility;
import com.dotcms.repackage.com.fasterxml.jackson.core.JsonParser;
import com.dotcms.repackage.com.fasterxml.jackson.core.JsonProcessingException;
import com.dotcms.repackage.com.fasterxml.jackson.databind.DeserializationContext;
import com.dotcms.repackage.com.google.gson.*;

import java.io.IOException;
import java.lang.reflect.Type;

import static com.dotcms.util.ReflectionUtils.getClassFor;

public class PayloadDeserializer implements JsonDeserializer {

    public static final String TYPE = "type";
    public static final String VISIBILITY = "visibility";
    public static final String VISIBILITY_ID = "visibilityId";
    public static final String DATA = "data";

    @Override
    public Object deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = null;
        String payloadType = null;
        Payload payload = null;
        Class clazz = null;
        Object payloadData = null;
        Visibility visibility = null;
        String visibilityName = null;
        String visibilityId = null;
        String userId = null;

        if (null != json) {

            jsonObject = json.getAsJsonObject();
            if (null != jsonObject && jsonObject.has(TYPE)) {

                payloadType = jsonObject.getAsJsonPrimitive
                        (TYPE).getAsString();

                if(jsonObject.has(VISIBILITY)) {
                    visibilityName = jsonObject.getAsJsonPrimitive
                            (VISIBILITY).getAsString();
                }

                if(jsonObject.has(VISIBILITY_ID)) {
                    visibilityId = jsonObject.getAsJsonPrimitive
                            (VISIBILITY_ID).getAsString();
                }

                if (null != visibilityName) {

                    visibility =
                            Visibility.valueOf(visibilityName);
                }

                clazz = getClassFor(payloadType);
                payloadData = context.deserialize(jsonObject.get(DATA), clazz);
                payload = new Payload(payloadData, visibility, visibilityId);
            }
        }

        return payload;
    }
}
