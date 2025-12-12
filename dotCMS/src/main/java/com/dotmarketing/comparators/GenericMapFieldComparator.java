package com.dotmarketing.comparators;

import java.util.Comparator;
import java.util.Date;
import java.util.Map;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

/**
 * Generic comparator for Map<String, Object> that can sort by any field with flexible handling
 * of missing fields. Supports ascending/descending order and multiple strategies for null/missing values.
 *
 * Special Features:
 * - FOLDER PRIORITY: Maps with type="folder" always appear first, regardless of sort field or direction
 * - Field fallbacks: Intelligent fallbacks for common field names (name/title, displayName, etc.)
 * - Type-aware comparison: Handles String, Date, Number, Boolean, and Comparable types
 * - User resolution: Resolves user IDs to full names for "modUser" field
 * - Sort order handling: Special handling for "sortOrder" field (0 = last)
 *
 * Missing Field Handling Strategies:
 * 1. NULLS_LAST - Missing/null values are placed at the end
 * 2. NULLS_FIRST - Missing/null values are placed at the beginning
 * 3. USE_DEFAULT - Missing values are replaced with a default value
 * 4. SKIP_MISSING - Items with missing values are treated as equal (return 0)
 *
 * Example usage:
 * // Sort by title descending, folders first, then nulls last
 * Collections.sort(mapList, new GenericMapFieldComparator("title", true));
 *
 * // Sort by modDate ascending, folders first, then nulls first
 * Collections.sort(mapList, new GenericMapFieldComparator("modDate", false, NullHandling.NULLS_FIRST));
 *
 * // Sort by sortOrder ascending, folders first, then missing values = 999999
 * Collections.sort(mapList, new GenericMapFieldComparator("sortOrder", false, "999999"));
 *
 * // Mixed content example:
 * // Input: [{"type":"file", "title":"B"}, {"type":"folder", "title":"Z"}, {"type":"file", "title":"A"}]
 * // Output: [{"type":"folder", "title":"Z"}, {"type":"file", "title":"A"}, {"type":"file", "title":"B"}]
 */
public class GenericMapFieldComparator implements Comparator<Map<String, Object>> {

    /**
     * Strategy for handling null/missing field values
     */
    public enum NullHandling {
        NULLS_LAST,     // Nulls go to end regardless of sort direction
        NULLS_FIRST,    // Nulls go to beginning regardless of sort direction
        USE_DEFAULT,    // Replace nulls with a default value
        SKIP_MISSING    // Treat missing values as equal (return 0)
    }

    private final String fieldName;
    private final boolean descending;
    private final NullHandling nullHandling;
    private final Object defaultValue;

    /**
     * Constructor with default null handling (NULLS_LAST)
     */
    public GenericMapFieldComparator(String fieldName, boolean descending) {
        this(fieldName, descending, NullHandling.NULLS_LAST);
    }

    /**
     * Constructor with specified null handling strategy
     */
    public GenericMapFieldComparator(String fieldName, boolean descending, NullHandling nullHandling) {
        this(fieldName, descending, nullHandling, null);
    }

    /**
     * Constructor with default value for missing fields
     */
    public GenericMapFieldComparator(String fieldName, boolean descending, Object defaultValue) {
        this(fieldName, descending, NullHandling.USE_DEFAULT, defaultValue);
    }

    /**
     * Full constructor
     */
    public GenericMapFieldComparator(String fieldName, boolean descending, NullHandling nullHandling, Object defaultValue) {
        this.fieldName = UtilMethods.isSet(fieldName) ? fieldName : "name";
        this.descending = descending;
        this.nullHandling = nullHandling != null ? nullHandling : NullHandling.NULLS_LAST;
        this.defaultValue = defaultValue;
    }

    @Override
    public int compare(Map<String, Object> map1, Map<String, Object> map2) {
        // Priority sorting: folders always come first regardless of sort field or direction
        int folderComparison = compareFolderPriority(map1, map2);
        if (folderComparison != 0) {
            return folderComparison;
        }

        Object value1 = getFieldValue(map1, fieldName);
        Object value2 = getFieldValue(map2, fieldName);

        // Handle null/missing values according to strategy
        int nullComparison = handleNullValues(value1, value2);
        if (nullComparison != Integer.MAX_VALUE) {
            return nullComparison;
        }

        // Both values are non-null, proceed with comparison
        return compareNonNullValues(value1, value2);
    }

    /**
     * Priority comparison to ensure folders always appear first in sorting results.
     * This method checks if either map contains a "type" field with value "folder"
     * and prioritizes those entries regardless of sort field or direction.
     *
     * @param map1 first map to compare
     * @param map2 second map to compare
     * @return -1 if map1 is folder and map2 is not, +1 if map2 is folder and map1 is not, 0 if both same type
     */
    private int compareFolderPriority(Map<String, Object> map1, Map<String, Object> map2) {
        if (map1 == null || map2 == null) {
            return 0;
        }

        boolean isFolder1 = isFolderType(map1);
        boolean isFolder2 = isFolderType(map2);

        // If both are folders or both are non-folders, no priority difference
        if (isFolder1 == isFolder2) {
            return 0;
        }

        // Folder always comes first, regardless of sort direction
        return isFolder1 ? -1 : 1;
    }

    /**
     * Determines if a map represents a folder by checking the "type" field.
     *
     * @param map the map to check
     * @return true if the map has type="folder", false otherwise
     */
    private boolean isFolderType(Map<String, Object> map) {
        Object type = map.get("type");
        return "folder".equals(type);
    }

    /**
     * Extract field value from map with intelligent fallback strategies
     */
    private Object getFieldValue(Map<String, Object> map, String field) {
        if (map == null) {
            return null;
        }

        Object value = map.get(field);

        // Smart fallbacks for common field names
        if (!UtilMethods.isSet(value)) {
            value = applyFieldFallbacks(map, field);
        }

        return value;
    }

    /**
     * Apply intelligent fallbacks for common field names
     */
    private Object applyFieldFallbacks(Map<String, Object> map, String field) {
        switch (field) {
            case "name":
                // For name field, try title as fallback
                return map.get("title");
            case "title":
                // For title field, try name as fallback
                return map.get("name");
            case "displayName":
                // Try multiple common display name fields
                Object value = map.get("title");
                if (!UtilMethods.isSet(value)) {
                    value = map.get("name");
                }
                if (!UtilMethods.isSet(value)) {
                    value = map.get("fileName");
                }
                return value;
            case "dateModified":
                // Try common date field variations
                Object dateValue = map.get("modDate");
                if (!UtilMethods.isSet(dateValue)) {
                    dateValue = map.get("lastModified");
                }
                return dateValue;
            default:
                return null;
        }
    }

    /**
     * Handle null/missing values according to the configured strategy
     * Returns Integer.MAX_VALUE if both values are non-null and comparison should proceed
     */
    private int handleNullValues(Object value1, Object value2) {
        boolean isNull1 = !UtilMethods.isSet(value1);
        boolean isNull2 = !UtilMethods.isSet(value2);

        if (!isNull1 && !isNull2) {
            return Integer.MAX_VALUE; // Signal to proceed with normal comparison
        }

        switch (nullHandling) {
            case NULLS_LAST:
                if (isNull1 && isNull2) return 0;
                if (isNull1) return descending ? -1 : 1;  // null goes to end
                if (isNull2) return descending ? 1 : -1;  // null goes to end
                break;

            case NULLS_FIRST:
                if (isNull1 && isNull2) return 0;
                if (isNull1) return descending ? 1 : -1;  // null goes to beginning
                if (isNull2) return descending ? -1 : 1;  // null goes to beginning
                break;

            case USE_DEFAULT:
                if (isNull1 && defaultValue != null) value1 = defaultValue;
                if (isNull2 && defaultValue != null) value2 = defaultValue;
                // If still null after default, treat as equal
                if (!UtilMethods.isSet(value1) || !UtilMethods.isSet(value2)) {
                    return 0;
                }
                return Integer.MAX_VALUE; // Proceed with comparison using defaults

            case SKIP_MISSING:
                if (isNull1 || isNull2) return 0;
                break;
        }

        return 0;
    }

    /**
     * Compare two non-null values with type-aware comparison
     */
    private int compareNonNullValues(Object value1, Object value2) {
        int result = 0;

        try {
            // Handle special field types
            if ("modUser".equals(fieldName)) {
                result = compareUsers(value1, value2);
            } else if ("sortOrder".equals(fieldName)) {
                result = compareSortOrder(value1, value2);
            } else {
                result = compareGenericValues(value1, value2);
            }

            // Apply descending order if needed
            return descending ? -result : result;

        } catch (Exception e) {
            Logger.warn(this, String.format("Error comparing values for field '%s': %s",
                fieldName, e.getMessage()));
            return 0;
        }
    }

    /**
     * Generic value comparison with automatic type detection
     */
    private int compareGenericValues(Object value1, Object value2) {
        // String comparison (most common)
        if (value1 instanceof String && value2 instanceof String) {
            String str1 = (String) value1;
            String str2 = (String) value2;
            return str1.toLowerCase().compareTo(str2.toLowerCase());
        }

        // Date comparison
        if (value1 instanceof Date && value2 instanceof Date) {
            return ((Date) value1).compareTo((Date) value2);
        }

        // Numeric comparison
        if (value1 instanceof Number && value2 instanceof Number) {
            Double double1 = ((Number) value1).doubleValue();
            Double double2 = ((Number) value2).doubleValue();
            return double1.compareTo(double2);
        }

        // Boolean comparison
        if (value1 instanceof Boolean && value2 instanceof Boolean) {
            return ((Boolean) value1).compareTo((Boolean) value2);
        }

        // Comparable interface fallback
        if (value1 instanceof Comparable && value2 instanceof Comparable &&
            value1.getClass().equals(value2.getClass())) {
            return ((Comparable) value1).compareTo(value2);
        }

        // Final fallback - string representation comparison
        String str1 = value1.toString();
        String str2 = value2.toString();
        return str1.toLowerCase().compareTo(str2.toLowerCase());
    }

    /**
     * Compare user IDs by resolving to user full names
     */
    private int compareUsers(Object userId1, Object userId2) {
        try {
            String id1 = userId1.toString();
            String id2 = userId2.toString();

            User user1 = APILocator.getUserAPI().loadUserById(id1, APILocator.getUserAPI().getSystemUser(), false);
            User user2 = APILocator.getUserAPI().loadUserById(id2, APILocator.getUserAPI().getSystemUser(), false);

            String name1 = UtilMethods.isSet(user1.getFullName()) ? user1.getFullName() : id1;
            String name2 = UtilMethods.isSet(user2.getFullName()) ? user2.getFullName() : id2;

            return name1.toLowerCase().compareTo(name2.toLowerCase());

        } catch (Exception e) {
            Logger.warn(this, "Error comparing users: " + e.getMessage());
            return userId1.toString().compareTo(userId2.toString());
        }
    }

    /**
     * Compare sort order values with zero-handling (0 means max value for sorting)
     */
    private int compareSortOrder(Object value1, Object value2) {
        long long1 = convertToLong(value1);
        long long2 = convertToLong(value2);
        return Long.compare(long1, long2);
    }

    /**
     * Convert value to long with special handling for sort order (0 = max value)
     */
    private long convertToLong(Object value) {
        try {
            long result;
            if (value instanceof Number) {
                result = ((Number) value).longValue();
            } else {
                result = Long.parseLong(value.toString());
            }
            // In sort order context, 0 typically means "put at end"
            return result == 0 ? Long.MAX_VALUE : result;
        } catch (NumberFormatException e) {
            return Long.MAX_VALUE;
        }
    }

    // Getters for testing and debugging
    public String getFieldName() { return fieldName; }
    public boolean isDescending() { return descending; }
    public NullHandling getNullHandling() { return nullHandling; }
    public Object getDefaultValue() { return defaultValue; }
}