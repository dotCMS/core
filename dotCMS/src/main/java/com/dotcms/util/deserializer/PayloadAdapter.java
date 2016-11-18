package com.dotcms.util.deserializer;


import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.Visibility;
import com.dotcms.repackage.com.fasterxml.jackson.core.JsonParser;
import com.dotcms.repackage.com.fasterxml.jackson.core.JsonProcessingException;
import com.dotcms.repackage.com.fasterxml.jackson.databind.DeserializationContext;
import com.dotcms.repackage.com.google.gson.*;
import com.liferay.portal.model.User;

import java.io.IOException;
import java.lang.reflect.Type;

import static com.dotcms.util.ReflectionUtils.getClassFor;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PayloadAdapter implements JsonDeserializer<Payload>,JsonSerializer<Payload> {

    public static final String TYPE = "type";
    public static final String VISIBILITY = "visibility";
    public static final String VISIBILITY_ID = "visibilityId";
    public static final String DATA = "data";
    public static final String USER = "user";

    @Override
    public Payload deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = null;
        String payloadType = null;
        Payload payload = null;
        Class clazz = null;
        Object payloadData = null;
        Visibility visibility = null;
        String visibilityName = null;
        String visibilityId = null;
        final Map<String, Object> user = new HashMap<>();

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

                if(jsonObject.has(USER)) {
                    ((JsonObject) jsonObject.get(USER)).entrySet().stream().
                            forEach(entry -> user.put(entry.getKey(), entry.getValue().getAsString()));
                }

                if (null != visibilityName) {

                    visibility =
                            Visibility.valueOf(visibilityName);
                }

                clazz = getClassFor(payloadType);
                payloadData = context.deserialize(jsonObject.get(DATA), clazz);
                payload = new Payload(payloadData, visibility, visibilityId, user);
            }
        }

        return payload;
    }

    @Override
    public JsonElement serialize(Payload payload, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject jsonElement = new JsonObject();
        jsonElement.addProperty(TYPE, payload.getType());
        jsonElement.addProperty(VISIBILITY, payload.getVisibility().name());
        jsonElement.addProperty(VISIBILITY_ID, payload.getVisibilityId());
        jsonElement.add(DATA, jsonSerializationContext.serialize(payload.getData()));

        JsonObject user = new JsonObject();
        payload.getUser().entrySet().stream().
                forEach( entry -> {
                    if (entry.getValue() != null) {
                        user.addProperty(entry.getKey(), entry.getValue().toString());
                    }
                } );

        jsonElement.add(USER, user);
        return jsonElement;
    }
}
