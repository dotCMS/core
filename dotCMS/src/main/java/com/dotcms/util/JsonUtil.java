package com.dotcms.util;

import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONObject;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liferay.util.StringPool;
import io.vavr.control.Try;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Util class to handle JSON
 *
 * @author Freddy Rodriguez
 * @since Jun 8th, 2022
 */
public class JsonUtil {

    public final static ObjectMapper JSON_MAPPER = new ObjectMapper();

    public static Map<String, Object> getJsonFileContent(final String path) throws IOException {
        return JSON_MAPPER.readValue(getJsonFileContentAsString(path), Map.class);
    }

    public static Map<String, Object> getJsonFromString(final String json) throws IOException {
        return JSON_MAPPER.readValue(json, Map.class);
    }

    /**
     * Takes a JSON value in the form of a String, and maps its attributes into the specified Java Class.
     *
     * @param json  The original JSON as String.
     * @param clazz The Class that the JSON will be transformed into.
     * @param <T>   The type of the resulting object
     *
     * @return The resulting object with the JSON data
     *
     * @throws IOException An error occurred when transforming the JSON String.
     */
    public static <T> T getObjectFromJson(final String json, Class<T> clazz) throws IOException {
        return JSON_MAPPER.readValue(json, clazz);
    }

    public static String getJsonAsString(final Map<String, Object> json) throws IOException {
        return JSON_MAPPER.writeValueAsString(json);
    }

    public static String getJsonFileContentAsString(final String path) throws IOException {

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        final URL url = classLoader.getResource(path);
        return new String(com.liferay.util.FileUtil.getBytes(new File(url.getPath())));
    }

    /**
     * Checks whether the provided String represents valid JSON data or not.
     *
     * @param fieldValue The String containing potential JSON data.
     *
     * @return If the String represents JSON data and has the appropriate format, returns {@code true}.
     */
    public static boolean isValidJSON(final String fieldValue) {
        try {
            JsonNode node = JSON_MAPPER.readTree(fieldValue);
            return node != null && !node.isMissingNode();
        } catch (final JacksonException e) {
            return false;
        }
    }

    public static String getJsonStringFromObject(final Object object) {
        final String json = Try.of(
                () -> JSON_MAPPER.writeValueAsString(object)).getOrElse(StringPool.BLANK);

        return json;
    }

    /**
     * Converts the JSON object into a Map.
     * @param jsonObject
     * @return
     */
    public static Map<String, Object> jsonToMap(final JSONObject jsonObject) {

        final Map<String, Object> map = new HashMap<>();
        final Iterator<String> keys = jsonObject.keys();

        while (keys.hasNext()) {
            final String key = keys.next();
            final Object value = jsonObject.get(key);

            if (value instanceof JSONObject) {
                map.put(key, jsonToMap((JSONObject) value));
            } else if (value instanceof JSONArray) {
                map.put(key, jsonToList((JSONArray) value));
            } else {
                map.put(key, value);
            }
        }
        return map;
    }

    /**
     * Converts the JSON array into a List.
     * @param jsonArray
     * @return
     */
    public static List<Object> jsonToList(final JSONArray jsonArray) {

        final List<Object> list = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            final Object value = jsonArray.get(i);

            if (value instanceof JSONObject) {
                list.add(jsonToMap((JSONObject) value));
            } else if (value instanceof JSONArray) {
                list.add(jsonToList((JSONArray) value));
            } else {
                list.add(value);
            }
        }
        return list;
    }
}
