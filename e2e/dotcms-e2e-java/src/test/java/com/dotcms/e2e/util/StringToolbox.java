package com.dotcms.e2e.util;

/**
 * Utility class for string manipulation methods.
 *
 * This class provides static methods to perform common string transformations.
 * It is designed to be used as a utility class and therefore has a private constructor
 * to prevent instantiation.
 *
 * @author vico
 */
public class StringToolbox {

    private StringToolbox() {
    }

    /**
     * Converts a camelCase string to snake_case.
     *
     * @param str the camelCase string to be converted
     * @return the converted snake_case string
     */
    public static String camelToSnake(String str) {
        return str.replaceAll("([a-z])([A-Z]+)", "$1_$2").toUpperCase();
    }

}
