package com.dotcms.util;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;

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

    final static ObjectMapper JSON_MAPPER = new ObjectMapper();

    public static Map<String, Object> getJsonFileContent(final String path) throws IOException {
        return JSON_MAPPER.readValue(getJsonFileContentAsString(path), Map.class);
    }

    public static Map<String, Object> getJsonFromString(final String json) throws IOException {
        return JSON_MAPPER.readValue(json, Map.class);
    }

    public static String getJsonAsString(final Map<String, Object> json) throws IOException {
        return JSON_MAPPER.writeValueAsString(json);
    }

    public static String getJsonFileContentAsString(final String path) throws IOException {

        ClassLoader classLoader = JsonUtil.class.getClassLoader();

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
            JSON_MAPPER.readTree(fieldValue);
        } catch (final JacksonException e) {
            return false;
        }
        return true;
    }

}
