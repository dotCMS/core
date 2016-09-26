package com.dotcms.util.deserializer;

import com.dotcms.repackage.com.google.gson.*;
import com.dotmarketing.portlets.contentlet.model.Contentlet;

import java.lang.reflect.Type;
import java.util.*;

public class ContentletDeserializer implements JsonDeserializer, JsonSerializer<Contentlet> {

    @Override
    public Object deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context) throws JsonParseException {
        JsonObject map = (JsonObject) jsonElement;

        List<String> disabledWYSIWYG = new ArrayList<>();
        JsonArray disabledWYSIWYGJsonElement = (JsonArray) map.remove(Contentlet.DISABLED_WYSIWYG_KEY);

        if (disabledWYSIWYGJsonElement != null) {
            for (JsonElement element : disabledWYSIWYGJsonElement) {
                disabledWYSIWYG.add(context.deserialize(element, String.class));
            }
        }

        Contentlet contentlet = new Contentlet();
        contentlet.setLowIndexPriority(map.remove("lowIndexPriority").getAsBoolean());
        contentlet.setDisabledWysiwyg(disabledWYSIWYG);

        Set<Map.Entry<String, JsonElement>> contentletAttr = map.entrySet();
        for (Map.Entry<String, JsonElement> attr : contentletAttr) {
            if (attr.getValue() instanceof JsonPrimitive){
                contentlet.setProperty(attr.getKey(), ((JsonPrimitive) attr.getValue()).getAsString());
            }
        }

        return contentlet;
    }

    @Override
    public JsonElement serialize(Contentlet contentlet, Type type, JsonSerializationContext context) {
        JsonObject jsonObject = (JsonObject) context.serialize( contentlet.getMap() );
        jsonObject.add("lowIndexPriority", new JsonPrimitive( contentlet.isLowIndexPriority()));
        return jsonObject;
    }
}
