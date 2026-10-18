package com.dotcms.rendering.velocity.viewtools;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.dotmarketing.util.Config;
import javax.servlet.http.HttpServletRequest;
import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.context.ViewContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Unit tests for {@link ConfigTool}.
 */
public class ConfigToolTest {

    private static final String SECRET_VALUE = "value-from-config";

    private MockedStatic<Config> mockedConfig;
    private MockedStatic<RestrictedViewToolAccess> mockedAccess;
    private ConfigTool configTool;

    @BeforeEach
    public void setUp() {
        mockedConfig = mockStatic(Config.class);
        mockedAccess = mockStatic(RestrictedViewToolAccess.class);

        // the restricted key list uses its default value
        mockedConfig.when(() -> Config.getStringArrayProperty(
                eq(ConfigTool.RESTRICTED_KEYS_PROPERTY), any(String[].class)))
                .thenAnswer(invocation -> invocation.getArgument(1));
        mockedConfig.when(() -> Config.getStringProperty(anyString(), any()))
                .thenReturn(SECRET_VALUE);
        mockedConfig.when(() -> Config.getStringArrayProperty(anyString()))
                .thenReturn(new String[]{SECRET_VALUE});
        mockedConfig.when(() -> Config.getIntProperty(anyString(), any(Integer.class)))
                .thenReturn(42);
        mockedConfig.when(() -> Config.getBooleanProperty(anyString(), any(Boolean.class)))
                .thenReturn(true);

        final ViewContext viewContext = mock(ViewContext.class);
        when(viewContext.getVelocityContext()).thenReturn(mock(Context.class));
        when(viewContext.getRequest()).thenReturn(mock(HttpServletRequest.class));
        configTool = new ConfigTool();
        configTool.init(viewContext);
    }

    @AfterEach
    public void tearDown() {
        mockedConfig.close();
        mockedAccess.close();
    }

    private void givenAccess(final boolean hasAccess) {
        mockedAccess.when(() -> RestrictedViewToolAccess.hasAccess(any(Context.class),
                any(HttpServletRequest.class))).thenReturn(hasAccess);
    }

    /**
     * Method to test: {@link ConfigTool#isRestrictedKey(String)}
     * When: property names refer to credentials, keys, connection settings or internal paths,
     * in any of the supported notations
     * Should: be restricted, while general purpose properties are not
     */
    @Test
    public void isRestrictedKey_detectsSensitiveNames() {
        for (final String key : new String[]{"DOT_INITIAL_ADMIN_PASSWORD", "ES_AUTH_BASIC_PASSWORD",
                "DOT_ES_AUTH_BASIC_USER", "dot.mail.smtp.host", "DOTCMS_CLUSTER_ID",
                "ASSET_REAL_PATH", "DYNAMIC_CONTENT_PATH", "DOT_AWS_S3_SECRET_ACCESS_KEY",
                "openai-api-key", "JSON_WEB_TOKEN_SIGNING_KEY", "DB_BASE_URL", "DOTCMS_CLUSTER_SALT",
                null}) {
            assertTrue(ConfigTool.isRestrictedKey(key), "Should be restricted: " + key);
        }
        for (final String key : new String[]{"DEFAULT_PAGE_CACHE_SECONDS", "DEFAULT_TEMPLATE_NAME",
                "ENABLE_SCRIPTING", "CONTENT_PALETTE_HIDDEN_CONTENT_TYPES"}) {
            assertFalse(ConfigTool.isRestrictedKey(key), "Should not be restricted: " + key);
        }
    }

    /**
     * Method to test: the ConfigTool getters
     * When: a restricted property is read and neither the author nor the user has access
     * Should: return the default value instead of the configured one
     */
    @Test
    public void restrictedProperty_withoutAccess_returnsDefault() {
        givenAccess(false);

        assertNull(configTool.getStringProperty("DOT_INITIAL_ADMIN_PASSWORD"));
        assertEquals("fallback", configTool.getStringProperty("ES_AUTH_BASIC_PASSWORD", "fallback"));
        assertNull(configTool.getStringArrayProperty("DOT_ES_ENDPOINTS"));
        assertEquals(7, configTool.getIntProperty("DB_MAXTOTAL", 7));
        assertFalse(configTool.getBooleanProperty("DOT_SECRET_FLAG", false));
    }

    /**
     * Method to test: the ConfigTool getters
     * When: a restricted property is read and the author or user has access
     * Should: return the configured value
     */
    @Test
    public void restrictedProperty_withAccess_returnsValue() {
        givenAccess(true);

        assertEquals(SECRET_VALUE, configTool.getStringProperty("DOT_INITIAL_ADMIN_PASSWORD"));
        assertArrayEquals(new String[]{SECRET_VALUE}, configTool.getStringArrayProperty("DOT_ES_ENDPOINTS"));
        assertEquals(42, configTool.getIntProperty("DB_MAXTOTAL", 7));
    }

    /**
     * Method to test: the ConfigTool getters
     * When: a general purpose property is read without access
     * Should: return the configured value
     */
    @Test
    public void unrestrictedProperty_withoutAccess_returnsValue() {
        givenAccess(false);

        assertEquals(SECRET_VALUE, configTool.getStringProperty("DEFAULT_TEMPLATE_NAME", "x"));
        assertEquals(42, configTool.getIntProperty("DEFAULT_PAGE_CACHE_SECONDS", 15));
        assertTrue(configTool.getBooleanProperty("DWRSESSIONID_SECURE", false));
    }

}
