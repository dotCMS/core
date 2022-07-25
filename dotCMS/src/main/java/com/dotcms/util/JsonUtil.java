package com.dotcms.util;

import com.dotcms.content.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;

/**
 * Util class to handle JSON
 */
public class JsonUtil {

    final static ObjectMapper JSON_MAPPER = new ObjectMapper();

    public static Map<String, Object> getJsonFileContent(final String path) throws IOException {
        return JSON_MAPPER.readValue(getJsonFileContentAsString(path), Map.class);
    }

    public static <K, T> Map<K, T> toMap(final String json) throws IOException {
        return JSON_MAPPER.readValue(json, Map.class);
    }

    public static String toJson(final Object object) throws IOException {
        return JSON_MAPPER.writeValueAsString(object);
    }

    public static String getJsonFileContentAsString(final String path) throws IOException {

        ClassLoader classLoader = JsonUtil.class.getClassLoader();

        final URL url = classLoader.getResource(path);
        return new String(com.liferay.util.FileUtil.getBytes(new File(url.getPath())));
    }
}
