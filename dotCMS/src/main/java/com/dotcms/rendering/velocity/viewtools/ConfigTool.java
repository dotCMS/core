package com.dotcms.rendering.velocity.viewtools;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

/**
 * The {@code $config} Velocity tool: read-only access to dotCMS configuration properties.
 *
 * <p>Properties whose name marks them as credentials (for example names containing
 * {@code PASSWORD}, {@code SECRET}, {@code TOKEN} or a {@code KEY} segment) are not readable
 * from templates; the getters return the caller's default value instead. Keys are compared in
 * the normalized form {@link Config#envKey(String)} produces, so every spelling Config accepts
 * for a key is treated alike. Operators can make specific keys readable again with
 * {@value #ALLOWED_KEYS_PROP} and withhold more keys with {@value #RESTRICTED_KEYS_PROP}.
 */
public class ConfigTool implements ViewTool {

    /**
     * dotCMS Config key (env: {@code DOT_VELOCITY_CONFIGTOOL_ALLOWED_KEYS}): comma-separated keys
     * templates may read even though their name marks them as credentials, such as a public
     * browser API key.
     */
    public static final String ALLOWED_KEYS_PROP = "velocity.configtool.allowed.keys";

    /**
     * dotCMS Config key (env: {@code DOT_VELOCITY_CONFIGTOOL_RESTRICTED_KEYS}): comma-separated
     * additional keys templates may not read.
     */
    public static final String RESTRICTED_KEYS_PROP = "velocity.configtool.restricted.keys";

    /** Fragments that mark a key as a credential wherever they appear in the normalized key. */
    private static final List<String> CREDENTIAL_FRAGMENTS = List.of(
            "PASSWORD", "PASSWD", "PASSPHRASE", "SECRET", "TOKEN", "CREDENTIAL", "APIKEY",
            "ACCESSKEY", "PRIVATE");

    /** Short words that mark a key as a credential when they form a whole segment of the key. */
    private static final Set<String> CREDENTIAL_SEGMENTS = Set.of(
            "PWD", "PASS", "KEY", "KEYS", "SALT", "AUTH", "JWT", "KEYSTORE", "TRUSTSTORE");

    private static final int WITHHELD_KEY_WARNING_MILLIS = 60 * 60 * 1000;

    @Override
    public void init(Object initData) {}

    /**
     * Returns a string property.
     *
     * @param name the property name
     * @param defValue the value to return if the property is not set or not readable from templates
     * @return the property value, or {@code defValue}
     */
    public String getStringProperty(String name, String defValue) {
        return isReadable(name) ? Config.getStringProperty(name, defValue) : defValue;
    }

    /**
     * Returns a string property.
     *
     * @param name the property name
     * @return the property value, or {@code null} if it is not set or not readable from templates
     */
    public String getStringProperty(String name) {
        return this.getStringProperty(name, null);
    }

    /**
     * Returns a multi-valued string property.
     *
     * @param name the property name
     * @return the property values, or an empty array if the property is not readable from templates
     */
    public String[] getStringArrayProperty(String name) {
        return isReadable(name) ? Config.getStringArrayProperty(name) : new String[0];
    }

    /**
     * Returns an integer property.
     *
     * @param name the property name
     * @return the property value, or {@code 0} if it is not set or not readable from templates
     */
    public int getIntProperty(String name) {
        return this.getIntProperty(name, 0);
    }

    /**
     * Returns an integer property.
     *
     * @param name the property name
     * @param defValue the value to return if the property is not set or not readable from templates
     * @return the property value, or {@code defValue}
     */
    public int getIntProperty(String name, int defValue) {
        return isReadable(name) ? Config.getIntProperty(name, defValue) : defValue;
    }

    /**
     * Returns a long property.
     *
     * @param name the property name
     * @return the property value, or {@code 0} if it is not set or not readable from templates
     */
    public long getLongProperty(String name) {
        return this.getLongProperty(name, 0);
    }

    /**
     * Returns a long property.
     *
     * @param name the property name
     * @param defValue the value to return if the property is not set or not readable from templates
     * @return the property value, or {@code defValue}
     */
    public long getLongProperty(String name, int defValue) {
        return isReadable(name) ? Config.getLongProperty(name, defValue) : defValue;
    }

    /**
     * Returns a float property.
     *
     * @param name the property name
     * @return the property value, or {@code 0} if it is not set or not readable from templates
     */
    public float getFloatProperty(String name) {
        return this.getFloatProperty(name, 0f);
    }

    /**
     * Returns a float property.
     *
     * @param name the property name
     * @param defaultVal the value to return if the property is not set or not readable from templates
     * @return the property value, or {@code defaultVal}
     */
    public float getFloatProperty(String name, float defaultVal) {
        return isReadable(name) ? Config.getFloatProperty(name, defaultVal) : defaultVal;
    }

    /**
     * Returns a boolean property.
     *
     * @param name the property name
     * @return the property value, or {@code false} if it is not set or not readable from templates
     */
    public boolean getBooleanProperty(String name) {
        return this.getBooleanProperty(name, false);
    }

    /**
     * Returns a boolean property.
     *
     * @param name the property name
     * @param defaultVal the value to return if the property is not set or not readable from templates
     * @return the property value, or {@code defaultVal}
     */
    public boolean getBooleanProperty(String name, boolean defaultVal) {
        return isReadable(name) ? Config.getBooleanProperty(name, defaultVal) : defaultVal;
    }

    /**
     * Decides whether templates may read the given property. Keys listed in
     * {@value #ALLOWED_KEYS_PROP} are readable; keys listed in {@value #RESTRICTED_KEYS_PROP} or
     * whose name marks them as credentials are not; everything else is.
     *
     * @param name the property name as the template passed it
     * @return {@code true} if the property may be returned to the template
     */
    private static boolean isReadable(final String name) {
        if (!UtilMethods.isSet(name)) {
            return false;
        }
        final String key = normalize(name);
        if (listedIn(ALLOWED_KEYS_PROP, key)) {
            return true;
        }
        if (listedIn(RESTRICTED_KEYS_PROP, key) || looksLikeCredential(key)) {
            Logger.warnEvery(ConfigTool.class, "configtool-withheld-" + key,
                    String.format("Config property '%s' is not readable from templates. "
                            + "List it in '%s' to allow it.", name, ALLOWED_KEYS_PROP),
                    WITHHELD_KEY_WARNING_MILLIS);
            return false;
        }
        return true;
    }

    /**
     * @param key a normalized key
     * @return {@code true} if the key's name marks it as a credential
     */
    private static boolean looksLikeCredential(final String key) {
        if (CREDENTIAL_FRAGMENTS.stream().anyMatch(key::contains)) {
            return true;
        }
        return Arrays.stream(key.split("[^A-Z0-9]+")).anyMatch(CREDENTIAL_SEGMENTS::contains);
    }

    /**
     * @param listProperty the Config key holding a comma-separated list of property names
     * @param key a normalized key
     * @return {@code true} if the list contains the key, in any spelling
     */
    private static boolean listedIn(final String listProperty, final String key) {
        return Arrays.stream(Config.getStringArrayProperty(listProperty, new String[0]))
                .filter(UtilMethods::isSet)
                .map(entry -> normalize(entry.trim()))
                .anyMatch(key::equals);
    }

    /**
     * @param name a property name in any spelling Config accepts
     * @return the name in Config's environment-variable form, upper-cased
     */
    private static String normalize(final String name) {
        return Config.envKey(name.trim()).toUpperCase(Locale.ROOT);
    }
}
