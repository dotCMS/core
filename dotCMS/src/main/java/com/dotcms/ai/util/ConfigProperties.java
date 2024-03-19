package com.dotcms.ai.util;

import com.dotmarketing.util.UtilMethods;
import io.vavr.control.Try;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * This class reads configuration values from config.properties file.
 */
public class ConfigProperties {
    private static final String PROPERTY_FILE_NAME = "plugin.properties";
    private static final Properties properties;
    private final static String ENV_PREFIX = "DOT_";

    static {
        properties = new Properties();
        try (InputStream in = ConfigProperties.class.getResourceAsStream("/" + PROPERTY_FILE_NAME)) {
            properties.load(in);
        } catch (Exception e) {
            System.out.println("IOException : Can't read " + PROPERTY_FILE_NAME);
            e.printStackTrace();
        }
    }

    public static String[] getArrayProperty(String key, String[] defaultValue) {
        String notSplit = getProperty(key);
        if (UtilMethods.isEmpty(notSplit)) {
            return defaultValue;
        }
        return Arrays.stream(notSplit.split(",")).filter(s -> UtilMethods.isSet(s)).map(s -> s.trim()).collect(Collectors.toList()).toArray(new String[0]);
    }

    public static String getProperty(String key) {
        return getProperty(key, null);
    }

    public static String getProperty(String key, String defaultValue) {
        return System.getenv(envKey(key)) != null ? System.getenv(envKey(key)) : properties.getProperty(key) != null ? properties.getProperty(key) : defaultValue;
    }

    private static String envKey(final String theKey) {

        String envKey = ENV_PREFIX + theKey.toUpperCase().replace(".", "_");
        while (envKey.contains("__")) {
            envKey = envKey.replace("__", "_");
        }
        return envKey.endsWith("_") ? envKey.substring(0, envKey.length() - 1) : envKey;

    }

    public static boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = getProperty(key, String.valueOf(defaultValue));
        return Try.of(() -> Boolean.parseBoolean(value)).getOrElse(defaultValue);
    }

    public static int getIntProperty(final String key, final int defaultValue) {
        String value = getProperty(key, String.valueOf(defaultValue));
        return Try.of(() -> Integer.parseInt(value)).getOrElse(defaultValue);
    }

    public static float getFloatProperty(final String key, final float defaultValue) {
        String value = getProperty(key, String.valueOf(defaultValue));
        return Try.of(() -> Float.parseFloat(value)).getOrElse(defaultValue);
    }
}
