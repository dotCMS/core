package com.dotcms.util.marshal;

import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.Visibility;
import com.dotcms.repackage.com.google.gson.*;
import static com.dotcms.util.ReflectionUtils.getClassFor;

import java.lang.reflect.Type;

/**
 * Encapsulates some of the custom type serializer and deserializer for dotcms own types.
 * @author jsanca
 */
public class CustomDotCmsTypeGsonConfigurator implements GsonConfigurator {

    public static final String TYPE = "type";
    public static final String VISIBILITY = "visibility";
    public static final String VISIBILITY_ID = "visibilityId";
    public static final String DATA = "data";

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
                Visibility visibility = null;
                String visibilityName = null;
                String visibilityId = null;

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
        });
    }
} // E:O:F:CustomDotCmsTypeGsonConfigurator.
