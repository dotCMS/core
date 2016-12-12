package com.dotcms.util.deserializer;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;

import com.dotmarketing.util.Logger;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ServiceLoader;

import static com.dotcms.util.ReflectionUtils.getClassFor;

/**
 * This class will work as a Proxy before generated GsonAdapters classes (from @Gson.TypeAdapters annotation). The
 * main purpose is to be able to serialize and deserialize JSON using the TypeAdapterFactory implementations. The format
 * of the json is the follow:<br>
 *
 * <code>
 * {
 *     type: fully qualified name
 *     data: use the TypeAdapterFactory of that type
 * }
 * </code>
 *
 * Created by Oscar Arrieta
 */
public class ImmutableTypeAdapter<T> implements JsonDeserializer<T>,JsonSerializer<T> {

    public static final String TYPE = "type";
    public static final String DATA = "data";
    private final GsonBuilder gsonBuilder;

    public ImmutableTypeAdapter(final GsonBuilder gsonBuilder){
        this.gsonBuilder = gsonBuilder;
    }

    @Override
    public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
        throws JsonParseException {

        if (json == null)
            throw new JsonParseException("Cannot deserialize null JSON element");

        JsonObject jsonObject = json.getAsJsonObject();
        if (null == jsonObject || !jsonObject.has(TYPE))
            throw new JsonParseException("Cannot deserialize JSON element with no metadata");


        String type = jsonObject.getAsJsonPrimitive(TYPE).getAsString();
        Class clazz = getClassFor(type);

        if (clazz != null) {
            for (TypeAdapterFactory factory : ServiceLoader.load(TypeAdapterFactory.class)) {

                TypeAdapter typeAdapter = factory.create(gsonBuilder.create(), TypeToken.get(clazz));

                if (typeAdapter != null) {
                    try {
                        return (T) typeAdapter.fromJson(jsonObject.get(DATA).getAsJsonObject().toString());
                    } catch (IOException e) {
                        Logger.error(this, "Error trying to get JSON for Class: " + type, e);

                        throw new JsonParseException(e);
                    }
                }
            }
        }

        throw new JsonParseException("Cannot find type adapter for class "+ clazz);
    }

    @Override
    public JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context) {

        JsonObject jsonElement = new JsonObject();
        jsonElement.addProperty(TYPE, src.getClass().getName());

        for (TypeAdapterFactory factory : ServiceLoader.load(TypeAdapterFactory.class)) {

            TypeAdapter typeAdapter = factory.create(gsonBuilder.create(), TypeToken.get(src.getClass()));

            if (typeAdapter != null){
                jsonElement.add(DATA, typeAdapter.toJsonTree(src));
                break;
            }
        }

        if (!jsonElement.has(DATA)) {
            throw new IllegalArgumentException("Cannot find type adapter for class "+ src.getClass().getName());
        }

        return jsonElement;
    }
}
