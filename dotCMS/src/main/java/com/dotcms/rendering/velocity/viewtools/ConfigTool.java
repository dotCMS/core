package com.dotcms.rendering.velocity.viewtools;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

/**
 * The {@code $config} Velocity tool: read-only access to the configuration properties meant for
 * templates.
 *
 * <p>Templates may read properties whose name starts with {@value #TEMPLATE_KEY_PREFIX} (set as
 * {@code DOT_VELOCITY_PUBLIC_*} environment variables or {@code VELOCITY_PUBLIC_*} properties), the
 * properties the templates shipped with dotCMS read, and the keys listed in
 * {@value #ALLOWED_KEYS_PROP}. Every other property returns the caller's default value, whoever
 * wrote or reads the template; a warning names the withheld key once an hour. Keys are compared in
 * the normalized form {@link Config#envKey(String)} produces, so every spelling Config accepts for
 * a key is treated alike. A template that needs a value beyond these should get it from
 * {@code $dotsecrets}.
 *
 * <p>While existing templates are migrated, {@value #LEGACY_ACCESS_PROP} restores the earlier
 * rule: every property is readable unless its name marks it as a credential or it is listed in
 * {@value #RESTRICTED_KEYS_PROP}. The setting is meant to be temporary.
 */
public class ConfigTool implements ViewTool {

    /**
     * Prefix of the property names that templates may always read, in the {@code VELOCITY_PUBLIC_X}
     * spelling; the environment form is {@code DOT_VELOCITY_PUBLIC_X}.
     */
    public static final String TEMPLATE_KEY_PREFIX = "VELOCITY_PUBLIC_";

    /**
     * dotCMS Config key (env: {@code DOT_VELOCITY_CONFIGTOOL_ALLOWED_KEYS}): comma-separated keys
     * templates may read in addition to the {@value #TEMPLATE_KEY_PREFIX} ones, such as a public
     * browser API key. The System Table works without a restart.
     */
    public static final String ALLOWED_KEYS_PROP = "velocity.configtool.allowed.keys";

    /**
     * dotCMS Config key (env: {@code DOT_VELOCITY_CONFIGTOOL_LEGACY_ACCESS}). Default {@code false}:
     * templates read only {@value #TEMPLATE_KEY_PREFIX} properties, the bundled keys and
     * {@value #ALLOWED_KEYS_PROP}. Set it to {@code true} to let templates read every other property
     * too, except the ones whose name marks them as a credential and the ones listed in
     * {@value #RESTRICTED_KEYS_PROP}, while templates are migrated. Deprecated: planned for removal.
     */
    public static final String LEGACY_ACCESS_PROP = "velocity.configtool.legacy.access";

    /**
     * dotCMS Config key (env: {@code DOT_VELOCITY_CONFIGTOOL_RESTRICTED_KEYS}): comma-separated
     * additional keys templates may not read while {@value #LEGACY_ACCESS_PROP} is on. It never
     * reduces the keys readable by default.
     */
    public static final String RESTRICTED_KEYS_PROP = "velocity.configtool.restricted.keys";

    /** Fragments that mark a key as a credential wherever they appear in the normalized key. */
    private static final List<String> CREDENTIAL_FRAGMENTS = List.of(
            "PASSWORD", "PASSWD", "PASSPHRASE", "SECRET", "TOKEN", "CREDENTIAL", "APIKEY",
            "ACCESSKEY", "PRIVATE");

    /** Short words that mark a key as a credential when they form a whole segment of the key. */
    private static final Set<String> CREDENTIAL_SEGMENTS = Set.of(
            "PWD", "PASS", "KEY", "KEYS", "SALT", "AUTH", "JWT", "KEYSTORE", "TRUSTSTORE");

    private static final AtomicBoolean legacyAccessAnnounced = new AtomicBoolean(false);

    /** Normalized form of the prefix, as {@link #normalize(String)} produces it. */
    private static final String NORMALIZED_PREFIX = normalize(TEMPLATE_KEY_PREFIX + "X")
            .replaceAll("X$", "");

    /** Properties read by the templates shipped with dotCMS, always readable. */
    private static final Set<String> BUNDLED_TEMPLATE_KEYS = Arrays.stream(new String[]{
            "DEFAULT_TEMPLATE_NAME", "DEFAULT_PAGE_CACHE_SECONDS", "DWRSESSIONID_SECURE",
            "DWRSESSIONID_SAMESITE", "path.to.dojo"})
            .map(ConfigTool::normalize)
            .collect(Collectors.toUnmodifiableSet());

    private static final String[] NO_VALUES = new String[0];

    private static final int WITHHELD_KEY_WARNING_MILLIS = 60 * 60 * 1000;

    @Override
    public void init(Object initData) {
        // nothing to keep: what the tool returns does not depend on the request
    }

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
        return getStringPropertyForTemplate(name);
    }

    /**
     * Returns a string property under the same rules, and with the same warnings, as
     * {@code $config}. Other view tools that hand configuration values to templates use it.
     *
     * @param name the property name
     * @return the property value, or {@code null} if it is not set or not readable from templates
     */
    public static String getStringPropertyForTemplate(final String name) {
        return isReadable(name) ? Config.getStringProperty(name, null) : null;
    }

    /**
     * Returns a multi-valued string property.
     *
     * @param name the property name
     * @return the property values, or an empty array if the property is not readable from templates
     */
    public String[] getStringArrayProperty(String name) {
        return isReadable(name) ? Config.getStringArrayProperty(name) : NO_VALUES;
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
     * Tells whether templates may read the given property. By default that is when its name starts
     * with {@value #TEMPLATE_KEY_PREFIX}, it is one of the properties the bundled templates read, or
     * it is listed in {@value #ALLOWED_KEYS_PROP}. While {@value #LEGACY_ACCESS_PROP} is on, every
     * other property is readable too unless its name marks it as a credential or it is listed in
     * {@value #RESTRICTED_KEYS_PROP}. Any spelling Config accepts for the name works.
     *
     * @param name the property name as the template passed it
     * @return {@code true} if the property may be returned to the template
     */
    public static boolean isAllowedKey(final String name) {
        if (!UtilMethods.isSet(name) || UtilMethods.isNotSet(name.trim())) {
            return false;
        }
        final String key = normalize(name);
        if (isAllowedByDefault(key)) {
            return true;
        }
        if (!legacyAccess()) {
            return false;
        }
        return !listedIn(RESTRICTED_KEYS_PROP, key) && !looksLikeCredential(key);
    }

    /**
     * @param key a normalized key
     * @return {@code true} if the key is readable without {@value #LEGACY_ACCESS_PROP}
     */
    private static boolean isAllowedByDefault(final String key) {
        return key.startsWith(NORMALIZED_PREFIX)
                || BUNDLED_TEMPLATE_KEYS.contains(key)
                || listedIn(ALLOWED_KEYS_PROP, key);
    }

    private static boolean legacyAccess() {
        final boolean on = Config.getBooleanProperty(LEGACY_ACCESS_PROP, false);
        if (on && legacyAccessAnnounced.compareAndSet(false, true)) {
            Logger.warn(ConfigTool.class, String.format("'%s' is on: templates may read every "
                    + "configuration property except credential-like and restricted ones. Name the "
                    + "properties templates need %s* or list them in '%s', then turn it off.",
                    LEGACY_ACCESS_PROP, TEMPLATE_KEY_PREFIX, ALLOWED_KEYS_PROP));
        }
        return on;
    }

    /**
     * Decides whether templates may read the given property and warns, once an hour per key: when
     * the property is withheld, and when it is readable only because {@value #LEGACY_ACCESS_PROP}
     * is on, so that the logs say what to do next.
     */
    private static boolean isReadable(final String name) {
        if (!isAllowedKey(name)) {
            if (UtilMethods.isSet(name)) {
                Logger.warnEvery(ConfigTool.class, "configtool-withheld-" + normalize(name),
                        String.format("Config property '%s' is not readable from templates. Name it "
                                + "%s*, list it in '%s', or set '%s=true' temporarily while templates "
                                + "are migrated.", name, TEMPLATE_KEY_PREFIX, ALLOWED_KEYS_PROP,
                                LEGACY_ACCESS_PROP),
                        WITHHELD_KEY_WARNING_MILLIS);
            }
            return false;
        }
        final String key = normalize(name);
        if (!isAllowedByDefault(key)) {
            Logger.warnEvery(ConfigTool.class, "configtool-legacy-" + key,
                    String.format("Config property '%s' is readable from templates only because '%s' "
                            + "is true. Name it %s* or list it in '%s' before turning that off.",
                            name, LEGACY_ACCESS_PROP, TEMPLATE_KEY_PREFIX, ALLOWED_KEYS_PROP),
                    WITHHELD_KEY_WARNING_MILLIS);
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
        return Arrays.stream(Config.getStringArrayProperty(listProperty, NO_VALUES))
                .filter(UtilMethods::isSet)
                .map(ConfigTool::normalize)
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
