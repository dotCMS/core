package com.dotcms.util;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liferay.util.StringPool;
import io.vavr.control.Try;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;

/**
 * This utility class exposes different methods that allow you to transform JSON Strings into Java
 * Objects and vice versa, as well as methods to validate JSON.
 *
 * @author Freddy Rodriguez
 * @since Jun 8th, 2022
 */
public class JsonUtil {

    public final static ObjectMapper JSON_MAPPER = new ObjectMapper();

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getJsonFileContent(final String path) throws IOException {
        return JSON_MAPPER.readValue(getJsonFileContentAsString(path), Map.class);
    }

    @SuppressWarnings("unchecked")
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
     * Transforms the specified object into a prettified JSON String.
     *
     * @param object The object to be transformed.
     *
     * @return The prettified JSON String.
     */
    public static String getPrettyJsonStringFromObject(final Object object) {
        return Try.of(() ->
                JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(object))
                .getOrElse(StringPool.BLANK);
    }

    /**
     * DTO for the Json Validation Error to travel and make it to the surface
     */
    public static class JSONValidationResult {
        public final String errorMessage;
        public final int line;
        public final int column;
        public final JsonNode node;

        /**
         * Constructor
         * @param errorMessage
         * @param line
         * @param column
         * @param node
         */
        public JSONValidationResult(String errorMessage, int line, int column, JsonNode node) {
            this.errorMessage = errorMessage;
            this.line = line;
            this.column = column;
            this.node = node;
        }

        /**
         * Constructor
         * @param errorMessage
         * @param line
         * @param column
         */
        public JSONValidationResult(String errorMessage, int line, int column) {
            this(errorMessage, line, column, null);
        }

        /**
         * Constructor
         * @param node
         */
        public JSONValidationResult(JsonNode node) {
            this(null, -1, -1, node);
        }

        /**
         * Quick way to know if the json is valid
         * @return
         */
        public boolean isValid() {
            return node != null && !node.isMissingNode();
        }
    }

    /**
     * This validation method provides more info and tells you right out of the box if the json is valid or not
     * @param fieldValue
     * @return
     */
    public static JSONValidationResult validateJSON(final String fieldValue) {
        if (fieldValue == null || fieldValue.isEmpty()) {
            return new JSONValidationResult("Json is empty", -1, -1);
        }
        try {
            JsonNode node = JSON_MAPPER.readTree(fieldValue);
            if (node != null && !node.isMissingNode()) {
                // Only accept objects {} or arrays []
                if (node.isObject() || node.isArray()) {
                    return new JSONValidationResult(node);
                } else {
                    return new JSONValidationResult("JSON must be an object or array, not a primitive value", -1, -1);
                }
            } else {
                return new JSONValidationResult("Json Node is null or missing", -1, -1);
            }
        } catch (final JacksonException e) {
            JsonLocation location = e.getLocation();
            return new JSONValidationResult(
                    e.getMessage(),
                    location != null ? location.getLineNr() : -1,
                    location != null ? location.getColumnNr() : -1
            );
        }
    }

}
