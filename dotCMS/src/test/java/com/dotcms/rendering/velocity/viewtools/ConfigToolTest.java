package com.dotcms.rendering.velocity.viewtools;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.dotmarketing.util.Config;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Test;

/**
 * Contract test for the {@code $config} Velocity tool ({@link ConfigTool}).
 *
 * <p>Templates may read ordinary configuration values through the tool, but properties whose
 * name marks them as credentials (passwords, secrets, tokens, keys, ...) are withheld: the tool
 * answers with the caller's default value instead. Operators can carve specific keys back out
 * with {@code velocity.configtool.allowed.keys} and withhold extra keys with
 * {@code velocity.configtool.restricted.keys}. Key matching follows the same normalization
 * {@link Config} uses, so dotted, dashed, upper-case and {@code DOT_}-prefixed spellings of a key
 * are treated alike.
 */
public class ConfigToolTest {

    private static final String ALLOWED_KEYS_PROP = "velocity.configtool.allowed.keys";
    private static final String RESTRICTED_KEYS_PROP = "velocity.configtool.restricted.keys";

    private final List<String> propertiesToClear = new ArrayList<>();
    private final ConfigTool configTool = new ConfigTool();

    @After
    public void clearProperties() {
        propertiesToClear.forEach(key -> Config.setProperty(key, null));
        propertiesToClear.clear();
    }

    /**
     * Stores a value under its environment-style key so that every spelling of the key resolves
     * to it through {@link Config}.
     */
    private void setEnvStyleProperty(final String envKey, final Object value) {
        propertiesToClear.add(envKey);
        Config.setProperty(envKey, value);
    }

    private void setProperty(final String key, final Object value) {
        propertiesToClear.add(key);
        Config.setProperty(key, value);
    }

    /**
     * Ordinary configuration values remain readable from templates, including the keys used by
     * the templates that ship with dotCMS.
     */
    @Test
    public void ordinary_properties_remain_readable() {
        setEnvStyleProperty("DOT_CONFIGTOOL_TEST_SITE_TITLE", "My Site");
        setEnvStyleProperty("DOT_CONFIGTOOL_TEST_PAGE_SIZE", "25");
        setEnvStyleProperty("DOT_CONFIGTOOL_TEST_FEATURE_ENABLED", "true");

        assertEquals("My Site", configTool.getStringProperty("configtool.test.site.title"));
        assertEquals("My Site", configTool.getStringProperty("CONFIGTOOL_TEST_SITE_TITLE", "x"));
        assertEquals(25, configTool.getIntProperty("configtool.test.page.size"));
        assertEquals(25L, configTool.getLongProperty("configtool.test.page.size"));
        assertTrue(configTool.getBooleanProperty("configtool.test.feature.enabled"));

        // keys read by the shipped templates come back exactly as Config holds them
        assertEquals(Config.getIntProperty("DEFAULT_PAGE_CACHE_SECONDS", 15),
                configTool.getIntProperty("DEFAULT_PAGE_CACHE_SECONDS", 15));
        assertEquals(Config.getStringProperty("DEFAULT_TEMPLATE_NAME", "System Template"),
                configTool.getStringProperty("DEFAULT_TEMPLATE_NAME", "System Template"));
        assertEquals(Config.getBooleanProperty("DWRSESSIONID_SECURE", false),
                configTool.getBooleanProperty("DWRSESSIONID_SECURE", false));
        assertEquals(Config.getStringProperty("DWRSESSIONID_SAMESITE", ""),
                configTool.getStringProperty("DWRSESSIONID_SAMESITE", ""));
    }

    /**
     * Properties whose names mark them as credentials are withheld: the tool returns the
     * caller's default even though {@link Config} holds a value.
     */
    @Test
    public void credential_properties_return_the_default_value() {
        final String[][] credentialKeys = {
                {"DOT_CONFIGTOOL_TEST_SMTP_PASSWORD", "configtool.test.smtp.password"},
                {"DOT_CONFIGTOOL_TEST_DB_PWD", "configtool.test.db.pwd"},
                {"DOT_CONFIGTOOL_TEST_CLIENT_SECRET", "configtool.test.client.secret"},
                {"DOT_CONFIGTOOL_TEST_ACCESS_TOKEN", "configtool.test.access.token"},
                {"DOT_CONFIGTOOL_TEST_API_KEY", "configtool.test.api.key"},
                {"DOT_CONFIGTOOL_TEST_APIKEY", "configtool.test.apikey"},
                {"DOT_CONFIGTOOL_TEST_SECRETKEY", "configtool.test.secretkey"},
                {"DOT_CONFIGTOOL_TEST_SERVICE_CREDENTIALS", "configtool.test.service.credentials"},
                {"DOT_CONFIGTOOL_TEST_HASH_SALT", "configtool.test.hash.salt"},
                {"DOT_CONFIGTOOL_TEST_PRIVATE_PEM", "configtool.test.private.pem"},
                {"DOT_CONFIGTOOL_TEST_BASIC_AUTH_USER", "configtool.test.basic.auth.user"},
        };

        for (final String[] key : credentialKeys) {
            setEnvStyleProperty(key[0], "stored-value");
            // premise: the value is really there and Config resolves it by its dotted name
            assertEquals(key[1], "stored-value", Config.getStringProperty(key[1], null));

            assertEquals(key[1], "fallback", configTool.getStringProperty(key[1], "fallback"));
            assertNull(key[1], configTool.getStringProperty(key[1]));
            assertArrayEquals(key[1], new String[0], configTool.getStringArrayProperty(key[1]));
        }
    }

    /**
     * Every spelling Config accepts for a key is withheld alike, so a template cannot reach a
     * credential by changing case, separators or the {@code DOT_} prefix.
     */
    @Test
    public void credential_properties_are_withheld_under_every_spelling() {
        setEnvStyleProperty("DOT_CONFIGTOOL_TEST_MAIL_PASSWORD", "stored-value");

        final String[] spellings = {
                "configtool.test.mail.password",
                "configtool-test-mail-password",
                "CONFIGTOOL_TEST_MAIL_PASSWORD",
                "Configtool.Test.Mail.Password",
                "DOT_CONFIGTOOL_TEST_MAIL_PASSWORD",
        };

        for (final String spelling : spellings) {
            assertEquals(spelling, "stored-value", Config.getStringProperty(spelling, null));
            assertEquals(spelling, "fallback", configTool.getStringProperty(spelling, "fallback"));
        }
    }

    /**
     * The numeric and boolean getters apply the same rule as the string getters.
     */
    @Test
    public void every_getter_withholds_credential_properties() {
        setEnvStyleProperty("DOT_CONFIGTOOL_TEST_NUMERIC_PASSWORD", "1234");
        setEnvStyleProperty("DOT_CONFIGTOOL_TEST_SECRET_ENABLED", "true");

        assertEquals(1234, Config.getIntProperty("configtool.test.numeric.password", 0));
        assertEquals(7, configTool.getIntProperty("configtool.test.numeric.password", 7));
        assertEquals(0, configTool.getIntProperty("configtool.test.numeric.password"));
        assertEquals(0L, configTool.getLongProperty("configtool.test.numeric.password"));
        assertEquals(0f, configTool.getFloatProperty("configtool.test.numeric.password"), 0f);

        assertTrue(Config.getBooleanProperty("configtool.test.secret.enabled", false));
        assertFalse(configTool.getBooleanProperty("configtool.test.secret.enabled"));
        assertFalse(configTool.getBooleanProperty("configtool.test.secret.enabled", false));
    }

    /**
     * Operators can allow templates to read a key that would otherwise be withheld, such as a
     * public browser API key, by listing it in {@code velocity.configtool.allowed.keys}.
     */
    @Test
    public void allowed_keys_setting_makes_a_withheld_key_readable() {
        setEnvStyleProperty("DOT_CONFIGTOOL_TEST_MAPS_API_KEY", "public-browser-key");
        assertEquals("fallback", configTool.getStringProperty("configtool.test.maps.api.key", "fallback"));

        setProperty(ALLOWED_KEYS_PROP, "configtool.test.maps.api.key");

        assertEquals("public-browser-key", configTool.getStringProperty("configtool.test.maps.api.key"));
        assertEquals("public-browser-key", configTool.getStringProperty("CONFIGTOOL_TEST_MAPS_API_KEY"));
    }

    /**
     * Operators can withhold additional keys whose names do not look like credentials by
     * listing them in {@code velocity.configtool.restricted.keys}.
     */
    @Test
    public void restricted_keys_setting_withholds_additional_keys() {
        setEnvStyleProperty("DOT_CONFIGTOOL_TEST_INTERNAL_ENDPOINT", "http://internal.example");
        assertEquals("http://internal.example",
                configTool.getStringProperty("configtool.test.internal.endpoint"));

        setProperty(RESTRICTED_KEYS_PROP, "CONFIGTOOL_TEST_INTERNAL_ENDPOINT");

        assertEquals("fallback", configTool.getStringProperty("configtool.test.internal.endpoint", "fallback"));
    }
}
