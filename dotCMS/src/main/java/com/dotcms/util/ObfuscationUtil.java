package com.dotcms.util;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;
import java.util.regex.Pattern;

/**
 * Central place for the key-name based obfuscation rules used when rendering potentially
 * sensitive configuration to users — the JVM info screen ({@code /api/v1/jvm}), the
 * configuration REST endpoint, security logging, etc.
 * <p>
 * A value is masked when its key matches either the built-in {@link #DEFAULT_OBFUSCATE_PATTERN}
 * or the deployment-specific pattern configured via the
 * {@code OBFUSCATE_SYSTEM_ENVIRONMENTAL_VARIABLES} property.
 */
public final class ObfuscationUtil {

    public static final String DEFAULT_OBFUSCATE_PATTERN = "passw|pass|passwd|secret|key|token";

    /**
     * The always-on rules — not overridable, so a misconfigured custom pattern can never
     * un-mask the obvious cases.
     */
    public static final Pattern BASE_PATTERN =
            Pattern.compile(DEFAULT_OBFUSCATE_PATTERN, Pattern.CASE_INSENSITIVE);

    /**
     * Deployment-specific rules from {@code OBFUSCATE_SYSTEM_ENVIRONMENTAL_VARIABLES};
     * defaults to the same expression as {@link #BASE_PATTERN}.
     */
    public static final Pattern CUSTOM_PATTERN = Pattern.compile(
            Config.getStringProperty("OBFUSCATE_SYSTEM_ENVIRONMENTAL_VARIABLES",
                    DEFAULT_OBFUSCATE_PATTERN),
            Pattern.CASE_INSENSITIVE);

    private ObfuscationUtil() {
    }

    /**
     * @return true when the key matches the built-in or the configured custom pattern
     */
    public static boolean shouldObfuscate(final String key) {
        return BASE_PATTERN.matcher(key).find() || CUSTOM_PATTERN.matcher(key).find();
    }

    /**
     * @return true when the key matches only the configured custom pattern (used by callers
     * that intentionally honor just the {@code OBFUSCATE_SYSTEM_ENVIRONMENTAL_VARIABLES} rules)
     */
    public static boolean matchesCustomPattern(final String key) {
        return CUSTOM_PATTERN.matcher(key).find();
    }

    /**
     * Masks the value when the key matches the obfuscation rules; returns it untouched otherwise.
     */
    public static String obfuscateIfNeeded(final String key, final Object valueObject) {
        final String value = (String) valueObject;
        if (UtilMethods.isEmpty(value)) {
            return "";
        }
        return shouldObfuscate(key) ? obfuscate(value) : value;
    }

    /**
     * Masks a value keeping only its first and last character, e.g. {@code s*********t}.
     */
    public static String obfuscate(final String value) {
        return value.charAt(0)
                + "*********"
                + value.charAt(value.length() - 1);
    }

}
