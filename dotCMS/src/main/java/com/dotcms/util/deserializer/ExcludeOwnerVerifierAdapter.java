package com.dotcms.util.deserializer;


import com.dotcms.api.system.event.Visibility;
import com.dotcms.api.system.event.verifier.ExcludeOwnerVerifierBean;
import com.google.gson.*;

import java.lang.reflect.Type;

import static com.dotcms.util.ReflectionUtils.getClassFor;

/**
 * Json deserializer and serializer for {@link com.dotcms.api.system.event.verifier.ExcludeOwnerVerifierBean} objects.
 * Basically the visibility value needs to add a type
 * </code>
 */
public class ExcludeOwnerVerifierAdapter implements JsonDeserializer<ExcludeOwnerVerifierBean>,JsonSerializer<ExcludeOwnerVerifierBean> {

    public static final String USER_ID = "userId";
    public static final String VISIBILITY = "visibility";
    public static final String VISIBILITY_VALUE = "visibilityValue";
    public static final String VISIBILITY_TYPE = "visibilityType";

    @Override
    public ExcludeOwnerVerifierBean deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {

        JsonObject jsonObject = null;
        Visibility visibility = null;
        String visibilityName = null;
        String userId = null;
        Object visibilityValue = null;
        String visibilityType  = null;
        ExcludeOwnerVerifierBean excludeOwnerVerifierBean = null;

        if (null != json) {

            jsonObject = json.getAsJsonObject();

            if(jsonObject.has(VISIBILITY)) {

                visibilityName = jsonObject.getAsJsonPrimitive
                        (VISIBILITY).getAsString();
            }

            if(jsonObject.has(USER_ID)) {

                userId = jsonObject.getAsJsonPrimitive
                        (USER_ID).getAsString();
            }

            if(jsonObject.has(VISIBILITY_VALUE)) {

                visibilityType  = jsonObject.getAsJsonPrimitive
                        (VISIBILITY_TYPE).getAsString();
                visibilityValue = context.deserialize(jsonObject.get(VISIBILITY_VALUE),
                        getClassFor(visibilityType));
            }

            if (null != visibilityName) {

                visibility =
                        Visibility.valueOf(visibilityName);
            }

            excludeOwnerVerifierBean = new ExcludeOwnerVerifierBean(userId, visibilityValue, visibility);
        }

        return excludeOwnerVerifierBean;
    }

    @Override
    public JsonElement serialize(ExcludeOwnerVerifierBean excludeOwnerVerifierBean, Type type, JsonSerializationContext jsonSerializationContext) {

        final JsonObject jsonElement = new JsonObject();
        final Object visibilityValue = excludeOwnerVerifierBean.getVisibilityValue();

        jsonElement.addProperty(USER_ID, excludeOwnerVerifierBean.getUserId());
        jsonElement.addProperty(VISIBILITY, excludeOwnerVerifierBean.getVisibility().name());

        if (visibilityValue != null) {
            jsonElement.add(VISIBILITY_VALUE,  jsonSerializationContext.serialize(visibilityValue));
            jsonElement.addProperty(VISIBILITY_TYPE, visibilityValue.getClass().getName());
        }

        return jsonElement;
    }
}
