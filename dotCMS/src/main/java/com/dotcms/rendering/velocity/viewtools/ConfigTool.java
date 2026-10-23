package com.dotcms.rendering.velocity.viewtools;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import java.util.Locale;
import org.apache.velocity.tools.view.tools.ViewTool;

/**
 * Exposes configuration properties to Velocity as {@code $config}.
 * <p>
 * Only the properties meant for templates are returned: those whose name starts with
 * {@value #TEMPLATE_KEY_PREFIX} (set as {@code DOT_VELOCITY_PUBLIC_*} environment variables or as
 * {@code VELOCITY_PUBLIC_*} properties) and those listed in {@link #ALLOWED_KEYS_PROPERTY}. Any
 * other property returns the default value, whoever wrote or reads the template. A template that
 * needs a value beyond these should get it from {@code $dotsecrets}.
 */
public class ConfigTool implements ViewTool {

    /**
     * Prefix of the property names that templates may always read.
     */
    public static final String TEMPLATE_KEY_PREFIX = "VELOCITY_PUBLIC_";

    /**
     * Comma-separated list of the other property names that templates may read. When set, it
     * replaces the default list (the properties read by the templates shipped with dotCMS).
     * Names are compared after normalization: upper case, with {@code .} and {@code -} replaced
     * by {@code _}.
     */
    public static final String ALLOWED_KEYS_PROPERTY = "VELOCITY_CONFIG_TOOL_ALLOWED_KEYS";

    private static final String[] DEFAULT_ALLOWED_KEYS = {"DEFAULT_TEMPLATE_NAME",
            "DEFAULT_PAGE_CACHE_SECONDS", "DWRSESSIONID_SECURE", "DWRSESSIONID_SAMESITE", "PATH_TO_DOJO"};

    private static final String ENV_TEMPLATE_KEY_PREFIX = "DOT_" + TEMPLATE_KEY_PREFIX;

    private static final String[] NO_VALUES = new String[0];

    @Override
    public void init(final Object initData) {
        // nothing to keep: what the tool returns does not depend on the request
    }

    /**
     * Tells whether templates may read the given property.
     *
     * @param name the property name, in any of the supported notations
     *             ({@code VELOCITY_PUBLIC_X}, {@code velocity.public.x}, {@code DOT_VELOCITY_PUBLIC_X})
     *
     * @return {@code true} if the name starts with {@value #TEMPLATE_KEY_PREFIX} or is in the
     * allowed list
     */
    public static boolean isAllowedKey(final String name) {
        final String normalized = normalizeKey(name);
        if (normalized.isEmpty()) {
            return false;
        }
        if (normalized.startsWith(TEMPLATE_KEY_PREFIX) || normalized.startsWith(ENV_TEMPLATE_KEY_PREFIX)) {
            return true;
        }
        return isListedKey(normalized);
    }

    private static boolean isListedKey(final String normalized) {
        final String[] keys = Config.getStringArrayProperty(ALLOWED_KEYS_PROPERTY, DEFAULT_ALLOWED_KEYS);
        if (null == keys) {
            return false;
        }
        for (final String key : keys) {
            if (normalized.equals(normalizeKey(key))) {
                return true;
            }
        }
        return false;
    }

    static String normalizeKey(final String key) {
        return null == key ? "" : key.trim().toUpperCase(Locale.ROOT).replace('.', '_').replace('-', '_');
    }

    private static boolean canRead(final String name) {
        if (isAllowedKey(name)) {
            return true;
        }
        Logger.debug(ConfigTool.class.getName(),
                () -> "Property '" + name + "' is not available to templates");
        return false;
    }

    /**
     * @param name     the property name
     * @param defValue the value to return when the property is not set or not available
     * @return the property value, or {@code defValue}
     */
    public String getStringProperty(String name, String defValue) {
        return canRead(name) ? Config.getStringProperty(name, defValue) : defValue;
    }

    /**
     * @param name the property name
     * @return the property value, or {@code null} when it is not set or not available
     */
    public String getStringProperty(String name) {
        return this.getStringProperty(name, null);
    }

    /**
     * @param name the property name
     * @return the comma-separated values of the property, or an empty array when it is not
     * available
     */
    public String[] getStringArrayProperty(String name) {
        return canRead(name) ? Config.getStringArrayProperty(name) : NO_VALUES;
    }

    /**
     * @param name the property name
     * @return the property value, or {@code 0} when it is not set or not available
     */
    public int getIntProperty(String name) {
        return this.getIntProperty(name, 0);
    }

    /**
     * @param name     the property name
     * @param defValue the value to return when the property is not set or not available
     * @return the property value, or {@code defValue}
     */
    public int getIntProperty(String name, int defValue) {
        return canRead(name) ? Config.getIntProperty(name, defValue) : defValue;
    }

    /**
     * @param name the property name
     * @return the property value, or {@code 0} when it is not set or not available
     */
    public long getLongProperty(String name) {
        return this.getLongProperty(name, 0);
    }

    /**
     * @param name     the property name
     * @param defValue the value to return when the property is not set or not available
     * @return the property value, or {@code defValue}
     */
    public long getLongProperty(String name, int defValue) {
        return canRead(name) ? Config.getLongProperty(name, defValue) : defValue;
    }

    /**
     * @param name the property name
     * @return the property value, or {@code 0} when it is not set or not available
     */
    public float getFloatProperty(String name) {
        return this.getFloatProperty(name, 0f);
    }

    /**
     * @param name       the property name
     * @param defaultVal the value to return when the property is not set or not available
     * @return the property value, or {@code defaultVal}
     */
    public float getFloatProperty(String name, float defaultVal) {
        return canRead(name) ? Config.getFloatProperty(name, defaultVal) : defaultVal;
    }

    /**
     * @param name the property name
     * @return the property value, or {@code false} when it is not set or not available
     */
    public boolean getBooleanProperty(String name) {
        return this.getBooleanProperty(name, false);
    }

    /**
     * @param name       the property name
     * @param defaultVal the value to return when the property is not set or not available
     * @return the property value, or {@code defaultVal}
     */
    public boolean getBooleanProperty(String name, boolean defaultVal) {
        return canRead(name) ? Config.getBooleanProperty(name, defaultVal) : defaultVal;
    }
}
