package com.dotcms.util;

import com.dotcms.content.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.core.JacksonException;
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

    public static String getJsonFileContentAsString(final String path) throws IOException {

        ClassLoader classLoader = JsonUtil.class.getClassLoader();

        final URL url = classLoader.getResource(path);
        return new String(com.liferay.util.FileUtil.getBytes(new File(url.getPath())));
    }

    public static boolean isValidJSON(final String fieldValue) {
        try {
            JSON_MAPPER.readTree(fieldValue);
        } catch (JacksonException e) {
            return false;
        }
        return true;
    }
}
