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

    public static class JSONValidationResult {
        public final String errorMessage;
        public final int line;
        public final int column;
        public final JsonNode node;

        public JSONValidationResult(String errorMessage, int line, int column, JsonNode node) {
            this.errorMessage = errorMessage;
            this.line = line;
            this.column = column;
            this.node = node;
        }

        //
        public JSONValidationResult(String errorMessage, int line, int column) {
            this(errorMessage, line, column, null);
        }

        //
        public JSONValidationResult(JsonNode node) {
            this(null, -1, -1, node);
        }

        //
        public boolean isValid() {
            return node != null && !node.isMissingNode();
        }
    }

    public static JSONValidationResult validateJSON(final String fieldValue) {
        try {
            JsonNode node = JSON_MAPPER.readTree(fieldValue);
            if (node != null && !node.isMissingNode()) {
                return new JSONValidationResult(node);
            } else {
                return new JSONValidationResult("Json Node is null or missing", -1, -1);
            }
        } catch (final JacksonException e) {
            JsonLocation location = e.getLocation();
            return new JSONValidationResult(
                    e.getMessage(),
                    location != null ? (int)location.getLineNr() : -1,
                    location != null ? (int)location.getColumnNr() : -1
            );
        }
    }
}
