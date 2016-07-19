package com.dotcms.util.marshal;

import com.dotcms.api.system.event.Payload;
import com.dotcms.repackage.com.google.gson.*;
import static com.dotcms.util.ReflectionUtils.getClassFor;

import java.lang.reflect.Type;

/**
 * Encapsulates some of the custom type serializer and deserializer for dotcms own types.
 * @author jsanca
 */
public class CustomDotCmsTypeGsonConfigurator implements GsonConfigurator {

    @Override
    public void configure(final GsonBuilder gsonBuilder) {

        this.addPayloadConfiguration(gsonBuilder);
    }

    @Override
    public boolean excludeDefaultConfiguration() {

        return false;
    }

    /**
     *
     * @param gsonBuilder
     */
    protected void addPayloadConfiguration(final GsonBuilder gsonBuilder) {

        gsonBuilder.registerTypeAdapter(Payload.class, new JsonDeserializer<Payload>() {

            @Override
            public Payload deserialize(JsonElement json, Type type, JsonDeserializationContext context) {

                JsonObject jsonObject = null;
                String payloadType = null;
                Payload payload = null;
                Class clazz = null;
                Object payloadData = null;

                if (null != json) {

                    jsonObject = json.getAsJsonObject();
                    if (null != jsonObject && jsonObject.has("type")) {

                        payloadType = jsonObject.getAsJsonPrimitive
                                ("type").getAsString();
                        clazz = getClassFor(payloadType);
                        payloadData = context.deserialize(jsonObject.get("data"), clazz);
                        payload = new Payload(payloadData);
                    }
                }

                return payload;
            }
        });
    }
} // E:O:F:CustomDotCmsTypeGsonConfigurator.
