package com.dotmarketing.util;

public class FieldNameUtils {

    private  FieldNameUtils() {
        // Private constructor
    }

    /**
     * Converts a field class name to its simple lowercase representation, removing special characters.
     * Examples: 
     * - com.dotcms.contenttype.model.field.TextField -> "text"
     * - Custom-Field -> "custom"
     * - Some_Special$Field -> "somespecial"
     */
    public static String convertFieldClassName(String fullClassName) {
        // Null or empty check
        if (fullClassName == null || fullClassName.trim().isEmpty()) {
            return "";
        }

        String trimmed = fullClassName.replaceAll("\\s+", "").toLowerCase();

        // Check if a string contains at least one dot

        final String[] parts = trimmed.split("\\.");
        if (parts.length > 1) {
            trimmed = parts[parts.length - 1];
        }

        String className = trimmed;
        // Validate that we have a non-empty class name
        if (className.isEmpty()) {
            return "";
        }

        // Remove "Field" suffix if present and valid
        if (className.endsWith("field")) {
            className = className.replaceAll("(?i)field$", "");
        }

        // Remove special characters (keep only letters and numbers)
        className = className.replaceAll("[^a-zA-Z0-9]", "");

        // Final validation - ensure we still have content after removing "Field" and special characters
        if (className.isEmpty()) {
            return "";
        }

        // Convert to lowercase
        return className.toLowerCase();
    }

    // Alternative version with Class<?> parameter
    public static String convertFieldClassName(Class<?> fieldClass) {
        return convertFieldClassName(fieldClass.getSimpleName());
    }


}
