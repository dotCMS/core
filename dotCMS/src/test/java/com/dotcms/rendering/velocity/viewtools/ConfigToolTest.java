package com.dotcms.rendering.velocity.viewtools;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;

import com.dotmarketing.util.Config;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Unit tests for {@link ConfigTool}.
 */
public class ConfigToolTest {

    private static final String CONFIGURED_VALUE = "value-from-config";

    private MockedStatic<Config> mockedConfig;
    private ConfigTool configTool;

    @BeforeEach
    public void setUp() {
        mockedConfig = mockStatic(Config.class);

        // the allowed key list uses its default value unless a test says otherwise
        mockedConfig.when(() -> Config.getStringArrayProperty(
                eq(ConfigTool.ALLOWED_KEYS_PROPERTY), any(String[].class)))
                .thenAnswer(invocation -> invocation.getArgument(1));
        mockedConfig.when(() -> Config.getStringProperty(anyString(), any()))
                .thenReturn(CONFIGURED_VALUE);
        mockedConfig.when(() -> Config.getStringArrayProperty(anyString()))
                .thenReturn(new String[]{CONFIGURED_VALUE});
        mockedConfig.when(() -> Config.getIntProperty(anyString(), anyInt())).thenReturn(42);
        mockedConfig.when(() -> Config.getLongProperty(anyString(), anyLong())).thenReturn(42L);
        mockedConfig.when(() -> Config.getFloatProperty(anyString(), anyFloat())).thenReturn(4.2f);
        mockedConfig.when(() -> Config.getBooleanProperty(anyString(), anyBoolean())).thenReturn(true);

        // no init(): the tool must behave the same with or without a request
        configTool = new ConfigTool();
    }

    @AfterEach
    public void tearDown() {
        mockedConfig.close();
    }

    /**
     * Method to test: {@link ConfigTool#isAllowedKey(String)}
     * When: property names start with the template prefix (in any notation, with or without the
     * environment prefix) or belong to the default list
     * Should: be allowed
     */
    @Test
    public void isAllowedKey_acceptsTemplatePrefixAndDefaults() {
        for (final String key : new String[]{"VELOCITY_PUBLIC_ENV_NAME", "velocity.public.env.name",
                "velocity-public-env-name", " VELOCITY_PUBLIC_X ", "DOT_VELOCITY_PUBLIC_ENV_NAME",
                "dot.velocity.public.env.name", "DEFAULT_TEMPLATE_NAME", "DEFAULT_PAGE_CACHE_SECONDS",
                "DWRSESSIONID_SECURE", "DWRSESSIONID_SAMESITE", "path.to.dojo", "PATH_TO_DOJO"}) {
            assertTrue(ConfigTool.isAllowedKey(key), "Should be allowed: " + key);
        }
    }

    /**
     * Method to test: {@link ConfigTool#isAllowedKey(String)}
     * When: property names do not start with the template prefix and are not in the list; this
     * includes the engine's own VELOCITY_* properties and names with the prefix somewhere else
     * Should: not be allowed
     */
    @Test
    public void isAllowedKey_rejectsOtherNames() {
        for (final String key : new String[]{"SOME_OTHER_KEY", "some.other.key", "DOT_SOME_OTHER_KEY",
                "ENABLE_SCRIPTING", "DB_BASE_URL", "ES_ENDPOINTS", "VELOCITY_ROOT",
                "VELOCITY_INTROSPECTOR_RESTRICT_CLASSES", "velocity.introspector.allow.classes",
                "VELOCITY_CONFIG_TOOL_ALLOWED_KEYS", "VELOCITY_FIELD_RENDER_RESTRICTED_VIEWTOOLS",
                "VELOCITY_PUBLICX", "MY_VELOCITY_PUBLIC_KEY", "DOTVELOCITY_PUBLIC_X", "", "  ", null}) {
            assertFalse(ConfigTool.isAllowedKey(key), "Should not be allowed: " + key);
        }
    }

    /**
     * Method to test: {@link ConfigTool#isAllowedKey(String)}
     * When: {@link ConfigTool#ALLOWED_KEYS_PROPERTY} is set
     * Should: allow its names, in any notation, instead of the default list, and keep the
     * template prefix working
     */
    @Test
    public void allowedKeysProperty_replacesDefaultList() {
        mockedConfig.when(() -> Config.getStringArrayProperty(
                eq(ConfigTool.ALLOWED_KEYS_PROPERTY), any(String[].class)))
                .thenReturn(new String[]{"some.other.key", " CUSTOM_FLAG "});

        assertTrue(ConfigTool.isAllowedKey("SOME_OTHER_KEY"));
        assertTrue(ConfigTool.isAllowedKey("custom-flag"));
        assertTrue(ConfigTool.isAllowedKey("VELOCITY_PUBLIC_ENV_NAME"));
        assertFalse(ConfigTool.isAllowedKey("DEFAULT_TEMPLATE_NAME"),
                "The configured list replaces the default list");
        assertFalse(ConfigTool.isAllowedKey("ANOTHER_KEY"));
    }

    /**
     * Method to test: the ConfigTool getters
     * When: a property that is not allowed is read
     * Should: return the default value (an empty array for the array getter), never the
     * configured one
     */
    @Test
    public void notAllowedProperty_returnsDefault() {
        assertNull(configTool.getStringProperty("SOME_OTHER_KEY"));
        assertEquals("fallback", configTool.getStringProperty("SOME_OTHER_KEY", "fallback"));
        assertArrayEquals(new String[0], configTool.getStringArrayProperty("SOME_OTHER_KEY"));
        assertEquals(0, configTool.getIntProperty("SOME_OTHER_KEY"));
        assertEquals(7, configTool.getIntProperty("SOME_OTHER_KEY", 7));
        assertEquals(0L, configTool.getLongProperty("SOME_OTHER_KEY"));
        assertEquals(7L, configTool.getLongProperty("SOME_OTHER_KEY", 7));
        assertEquals(0f, configTool.getFloatProperty("SOME_OTHER_KEY"));
        assertEquals(0.7f, configTool.getFloatProperty("SOME_OTHER_KEY", 0.7f));
        assertFalse(configTool.getBooleanProperty("SOME_OTHER_KEY"));
        assertFalse(configTool.getBooleanProperty("SOME_OTHER_KEY", false));
    }

    /**
     * Method to test: the ConfigTool getters
     * When: an allowed property is read
     * Should: return the configured value
     */
    @Test
    public void allowedProperty_returnsValue() {
        assertEquals(CONFIGURED_VALUE, configTool.getStringProperty("VELOCITY_PUBLIC_ENV_NAME"));
        assertEquals(CONFIGURED_VALUE, configTool.getStringProperty("velocity.public.env.name", "x"));
        assertArrayEquals(new String[]{CONFIGURED_VALUE},
                configTool.getStringArrayProperty("VELOCITY_PUBLIC_LIST"));
        assertEquals(42, configTool.getIntProperty("DEFAULT_PAGE_CACHE_SECONDS", 15));
        assertEquals(42L, configTool.getLongProperty("VELOCITY_PUBLIC_LIMIT", 1));
        assertEquals(4.2f, configTool.getFloatProperty("VELOCITY_PUBLIC_RATIO", 1f));
        assertTrue(configTool.getBooleanProperty("DWRSESSIONID_SECURE", false));
        assertEquals(CONFIGURED_VALUE, configTool.getStringProperty("DEFAULT_TEMPLATE_NAME", "x"));
    }

}
