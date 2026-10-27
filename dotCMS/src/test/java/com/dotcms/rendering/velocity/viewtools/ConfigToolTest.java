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
 * <p>Templates may read the properties meant for them: those named {@code VELOCITY_PUBLIC_*}
 * (environment form {@code DOT_VELOCITY_PUBLIC_*}), the properties the templates shipped with
 * dotCMS read, and the keys operators list in {@code velocity.configtool.allowed.keys}. Every
 * other property is withheld: the tool answers with the caller's default value instead. Key
 * matching follows the same normalization {@link Config} uses, so dotted, dashed, upper-case and
 * {@code DOT_}-prefixed spellings of a key are treated alike.
 */
public class ConfigToolTest {

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
     * Properties named for templates are readable under every spelling Config accepts.
     */
    @Test
    public void template_properties_are_readable_under_every_spelling() {
        setEnvStyleProperty("DOT_VELOCITY_PUBLIC_TEST_SITE_TITLE", "My Site");

        final String[] spellings = {
                "velocity.public.test.site.title",
                "velocity-public-test-site-title",
                "VELOCITY_PUBLIC_TEST_SITE_TITLE",
                "Velocity.Public.Test.Site.Title",
                "DOT_VELOCITY_PUBLIC_TEST_SITE_TITLE",
                " VELOCITY_PUBLIC_TEST_SITE_TITLE ",
        };

        for (final String spelling : spellings) {
            assertTrue(spelling, ConfigTool.isAllowedKey(spelling));
            assertEquals(spelling, "My Site", configTool.getStringProperty(spelling.trim(), "x"));
        }
    }

    /**
     * Every getter returns the configured value for a template property.
     */
    @Test
    public void every_getter_returns_template_properties() {
        setEnvStyleProperty("DOT_VELOCITY_PUBLIC_TEST_PAGE_SIZE", "25");
        setEnvStyleProperty("DOT_VELOCITY_PUBLIC_TEST_RATIO", "2.5");
        setEnvStyleProperty("DOT_VELOCITY_PUBLIC_TEST_FEATURE_ENABLED", "true");
        setEnvStyleProperty("DOT_VELOCITY_PUBLIC_TEST_LIST", "a,b");

        assertEquals(25, configTool.getIntProperty("velocity.public.test.page.size"));
        assertEquals(25, configTool.getIntProperty("velocity.public.test.page.size", 7));
        assertEquals(25L, configTool.getLongProperty("velocity.public.test.page.size"));
        assertEquals(25L, configTool.getLongProperty("velocity.public.test.page.size", 7));
        assertEquals(2.5f, configTool.getFloatProperty("velocity.public.test.ratio"), 0f);
        assertEquals(2.5f, configTool.getFloatProperty("velocity.public.test.ratio", 1f), 0f);
        assertTrue(configTool.getBooleanProperty("velocity.public.test.feature.enabled"));
        assertTrue(configTool.getBooleanProperty("velocity.public.test.feature.enabled", false));
        assertArrayEquals(new String[]{"a", "b"},
                configTool.getStringArrayProperty("velocity.public.test.list"));
    }

    /**
     * The keys read by the templates shipped with dotCMS come back exactly as Config holds them.
     */
    @Test
    public void bundled_template_keys_remain_readable() {
        for (final String key : new String[]{"DEFAULT_TEMPLATE_NAME", "DEFAULT_PAGE_CACHE_SECONDS",
                "DWRSESSIONID_SECURE", "DWRSESSIONID_SAMESITE", "path.to.dojo", "PATH_TO_DOJO",
                "default.template.name"}) {
            assertTrue(key, ConfigTool.isAllowedKey(key));
        }
        assertEquals(Config.getIntProperty("DEFAULT_PAGE_CACHE_SECONDS", 15),
                configTool.getIntProperty("DEFAULT_PAGE_CACHE_SECONDS", 15));
        assertEquals(Config.getStringProperty("DEFAULT_TEMPLATE_NAME", "System Template"),
                configTool.getStringProperty("DEFAULT_TEMPLATE_NAME", "System Template"));
        assertEquals(Config.getBooleanProperty("DWRSESSIONID_SECURE", false),
                configTool.getBooleanProperty("DWRSESSIONID_SECURE", false));
        assertEquals(Config.getStringProperty("DWRSESSIONID_SAMESITE", ""),
                configTool.getStringProperty("DWRSESSIONID_SAMESITE", ""));
        assertEquals(Config.getStringProperty("path.to.dojo", "/html/js/dojo"),
                configTool.getStringProperty("path.to.dojo", "/html/js/dojo"));
    }

    /**
     * Any other property is withheld under every spelling, so a template cannot reach it by
     * changing case, separators or the {@code DOT_} prefix.
     */
    @Test
    public void other_properties_are_withheld_under_every_spelling() {
        setEnvStyleProperty("DOT_CONFIGTOOL_TEST_SITE_TITLE", "stored-value");

        final String[] spellings = {
                "configtool.test.site.title",
                "configtool-test-site-title",
                "CONFIGTOOL_TEST_SITE_TITLE",
                "Configtool.Test.Site.Title",
                "DOT_CONFIGTOOL_TEST_SITE_TITLE",
        };

        for (final String spelling : spellings) {
            // premise: the value is really there and Config resolves it by this spelling
            assertEquals(spelling, "stored-value", Config.getStringProperty(spelling, null));
            assertFalse(spelling, ConfigTool.isAllowedKey(spelling));
            assertEquals(spelling, "fallback", configTool.getStringProperty(spelling, "fallback"));
            assertNull(spelling, configTool.getStringProperty(spelling));
            assertArrayEquals(spelling, new String[0], configTool.getStringArrayProperty(spelling));
        }
    }

    /**
     * Names that only resemble the template prefix, the engine's own {@code VELOCITY_*}
     * properties, the tool's own setting and blank names are not readable.
     */
    @Test
    public void prefix_must_start_the_name_and_engine_properties_are_withheld() {
        for (final String key : new String[]{"VELOCITY_PUBLICX", "VELOCITY_PUBLIC", "MY_VELOCITY_PUBLIC_KEY",
                "DOTVELOCITY_PUBLIC_X", "VELOCITY_ROOT", "VELOCITY_INTROSPECTOR_RESTRICT_CLASSES",
                "velocity.introspector.allow.classes", "VELOCITY_FIELD_RENDER_RESTRICTED_VIEWTOOLS",
                ConfigTool.ALLOWED_KEYS_PROP, "", "  ", null}) {
            assertFalse(String.valueOf(key), ConfigTool.isAllowedKey(key));
        }
    }

    /**
     * The numeric and boolean getters apply the same rule as the string getters.
     */
    @Test
    public void every_getter_withholds_other_properties() {
        setEnvStyleProperty("DOT_CONFIGTOOL_TEST_NUMBER", "1234");
        setEnvStyleProperty("DOT_CONFIGTOOL_TEST_FLAG", "true");

        assertEquals(1234, Config.getIntProperty("configtool.test.number", 0));
        assertEquals(7, configTool.getIntProperty("configtool.test.number", 7));
        assertEquals(0, configTool.getIntProperty("configtool.test.number"));
        assertEquals(0L, configTool.getLongProperty("configtool.test.number"));
        assertEquals(7L, configTool.getLongProperty("configtool.test.number", 7));
        assertEquals(0f, configTool.getFloatProperty("configtool.test.number"), 0f);
        assertEquals(0.7f, configTool.getFloatProperty("configtool.test.number", 0.7f), 0f);

        assertTrue(Config.getBooleanProperty("configtool.test.flag", false));
        assertFalse(configTool.getBooleanProperty("configtool.test.flag"));
        assertFalse(configTool.getBooleanProperty("configtool.test.flag", false));
    }

    /**
     * Operators can let templates read an extra key, such as a public browser API key, by listing
     * it in {@code velocity.configtool.allowed.keys}; the bundled keys stay readable.
     */
    @Test
    public void allowed_keys_setting_adds_readable_keys() {
        setEnvStyleProperty("DOT_CONFIGTOOL_TEST_MAPS_KEY", "public-browser-key");
        assertEquals("fallback", configTool.getStringProperty("configtool.test.maps.key", "fallback"));

        setProperty(ConfigTool.ALLOWED_KEYS_PROP, " configtool.test.maps.key , other-key ");

        assertEquals("public-browser-key", configTool.getStringProperty("configtool.test.maps.key"));
        assertEquals("public-browser-key", configTool.getStringProperty("CONFIGTOOL_TEST_MAPS_KEY"));
        assertTrue(ConfigTool.isAllowedKey("OTHER_KEY"));
        assertTrue(ConfigTool.isAllowedKey("DEFAULT_TEMPLATE_NAME"));
        assertFalse(ConfigTool.isAllowedKey("configtool.test.site.title"));
    }
}
