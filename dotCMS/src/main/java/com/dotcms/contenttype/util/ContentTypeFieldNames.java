package com.dotcms.contenttype.util;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Centralized utility for normalizing Content Type field names between API and database representations.
 * <p>
 * This class provides a single source of truth for field name mappings, ensuring consistent handling
 * across API endpoints, database queries, and business logic. It uses rule-based conversion to handle
 * common naming conventions automatically.
 * <p>
 * Conversion rules:
 * <ul>
 *   <li>camelCase → snake_case (e.g., modDate → mod_date)</li>
 *   <li>Already snake_case → unchanged (e.g., mod_date → mod_date)</li>
 *   <li>Special case: "variable" → "velocity_var_name"</li>
 * </ul>
 * <p>
 * All field name operations are case-insensitive by design.
 *
 * @author dotCMS
 * @since 25.11.08
 */
public final class ContentTypeFieldNames {

    // Database column names (source of truth)
    public static final String NAME = "name";
    public static final String VARIABLE = "variable";
    public static final String VELOCITY_VAR_NAME = "velocity_var_name";
    public static final String MOD_DATE = "mod_date";
    public static final String DESCRIPTION = "description";
    public static final String DEFAULT_TYPE = "default_type";
    public static final String OWNER = "owner";
    public static final String URL_MAP_PATTERN = "url_map_pattern";
    public static final String PUBLISH_DATE_VAR = "publish_date_var";
    public static final String EXPIRE_DATE_VAR = "expire_date_var";
    public static final String STRUCTURE_TYPE = "structuretype";
    public static final String SYSTEM = "system";
    public static final String FIXED = "fixed";
    public static final String HOST = "host";
    public static final String FOLDER = "folder";
    public static final String INODE = "inode";

    /**
     * Set of valid database column names for Content Types.
     * Used for security validation in SQL queries.
     */
    private static final Set<String> VALID_DB_COLUMNS = Set.of(
            NAME,
            VELOCITY_VAR_NAME,
            MOD_DATE,
            DESCRIPTION,
            DEFAULT_TYPE,
            OWNER,
            URL_MAP_PATTERN,
            PUBLISH_DATE_VAR,
            EXPIRE_DATE_VAR,
            STRUCTURE_TYPE,
            SYSTEM,
            FIXED,
            HOST,
            FOLDER,
            INODE
    );

    /**
     * Pattern to detect camelCase words (capital letter followed by lowercase).
     */
    private static final Pattern CAMEL_CASE_PATTERN = Pattern.compile("([a-z])([A-Z])");

    /**
     * Normalizes an API field name to its canonical database column name using rule-based conversion.
     * <p>
     * Conversion rules:
     * <ul>
     *   <li>Special case: "variable" → "velocity_var_name"</li>
     *   <li>camelCase is converted to snake_case (e.g., modDate → mod_date)</li>
     *   <li>Already snake_case is unchanged (e.g., mod_date → mod_date)</li>
     *   <li>All input is lowercased and trimmed</li>
     * </ul>
     * <p>
     * All comparisons are case-insensitive.
     *
     * @param apiFieldName The field name from the API (may be camelCase, snake_case, etc.)
     * @return The normalized database column name, or the converted input if no special mapping exists
     */
    public static String normalize(final String apiFieldName) {
        if (apiFieldName == null || apiFieldName.isBlank()) {
            return null;
        }

        final String lowercase = apiFieldName.toLowerCase(Locale.ROOT).trim();

        // Special case: "variable" is an alias for "velocity_var_name"
        if ("variable".equals(lowercase)) {
            return VELOCITY_VAR_NAME;
        }

        // If it's already in snake_case (contains underscore), return as-is
        if (lowercase.contains("_")) {
            return lowercase;
        }

        // Otherwise, convert camelCase to snake_case
        return camelToSnake(lowercase);
    }

    /**
     * Converts camelCase to snake_case.
     * <p>
     * Examples:
     * <ul>
     *   <li>modDate → mod_date</li>
     *   <li>velocityVarName → velocity_var_name</li>
     *   <li>urlMapPattern → url_map_pattern</li>
     *   <li>name → name (no change)</li>
     * </ul>
     *
     * @param camelCase The camelCase string to convert
     * @return The snake_case equivalent
     */
    private static String camelToSnake(final String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) {
            return camelCase;
        }

        // Insert underscore before capital letters (but only for lowercase followed by capital)
        // This handles: modDate → mod_date, but preserves already lowercase strings
        return CAMEL_CASE_PATTERN.matcher(camelCase).replaceAll("$1_$2");
    }

    /**
     * Validates that a field name is a known database column.
     * <p>
     * Use this for security validation before using field names in SQL ORDER BY or WHERE clauses.
     * The field name is first normalized, then checked against the whitelist of valid columns.
     *
     * @param fieldName The field name to validate
     * @return true if the field is a recognized database column, false otherwise
     */
    public static boolean isValidField(final String fieldName) {
        if (fieldName == null || fieldName.isBlank()) {
            return false;
        }

        final String normalized = normalize(fieldName);
        return VALID_DB_COLUMNS.contains(normalized);
    }

    /**
     * Normalizes an orderBy parameter that may include direction (e.g., "name asc", "modDate:desc").
     * <p>
     * Extracts the field name, normalizes it using rule-based conversion, and preserves the sort direction.
     * <p>
     * Examples:
     * <ul>
     *   <li>modDate desc → mod_date desc</li>
     *   <li>name:asc → name asc</li>
     *   <li>velocityVarName → velocity_var_name</li>
     * </ul>
     *
     * @param orderByParam The orderBy parameter (e.g., "modDate desc", "name:asc")
     * @return The normalized orderBy string (e.g., "mod_date desc", "name asc")
     */
    public static String normalizeOrderBy(final String orderByParam) {
        if (orderByParam == null || orderByParam.isBlank()) {
            return null;
        }

        // Split on space or colon to separate field from direction
        final String[] parts = orderByParam.trim().split("[:\\s]+");
        final String field = normalize(parts[0]);

        if (parts.length > 1) {
            // Preserve the direction (asc/desc)
            return field + " " + parts[1].toLowerCase(Locale.ROOT);
        }

        return field;
    }

    /**
     * Returns the set of valid database column names.
     * <p>
     * This is useful for validation and documentation purposes.
     *
     * @return An immutable set of valid database column names
     */
    public static Set<String> getValidColumns() {
        return VALID_DB_COLUMNS;
    }

    private ContentTypeFieldNames() {
        // Utility class - prevent instantiation
    }
}