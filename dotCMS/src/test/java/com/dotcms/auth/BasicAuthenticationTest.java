package com.dotcms.auth;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.config.SystemTableConfigSource;
import com.dotcms.featureflag.FeatureFlagName;
import com.dotmarketing.util.Config;
import java.lang.reflect.Field;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Covers the configuration fallback of the shared Basic-authentication policy: an absent
 * property resolves to {@link BasicAuthentication.Mode#JWT_TOKEN} — an API JWT as the Basic
 * password validates the same persisted token a Bearer header would, without password
 * verification — while explicit {@code FALSE}, empty and unrecognized values fail closed to
 * {@link BasicAuthentication.Mode#NONE}.
 */
public class BasicAuthenticationTest {

    private String previousMode;

    @Before
    public void saveMode() {
        previousMode = Config.getStringProperty("ALLOW_BASIC_AUTH", null);
    }

    @After
    public void restoreMode() {
        Config.setProperty("ALLOW_BASIC_AUTH", previousMode);
    }

    /** The absent-property default is the token posture, not a legacy password posture. */
    @Test
    public void absentPropertyDefaultsToJwtToken() {
        Config.setProperty("ALLOW_BASIC_AUTH", null);
        assertEquals(BasicAuthentication.Mode.JWT_TOKEN, BasicAuthentication.mode());
    }

    /** Whitespace and case never turn a recognized value into an unrecognized one. */
    @Test
    public void tokenModeToleratesWhitespaceAndCase() {
        Config.setProperty("ALLOW_BASIC_AUTH", " jwt_token ");
        assertEquals(BasicAuthentication.Mode.JWT_TOKEN, BasicAuthentication.mode());
        Config.setProperty("ALLOW_BASIC_AUTH", "jwt_Token");
        assertEquals(BasicAuthentication.Mode.JWT_TOKEN, BasicAuthentication.mode());
    }

    /** System-table booleans must be read without dropping malformed trailing comma fields. */
    @Test
    public void urlOptInRejectsMalformedSystemTableValues() throws Exception {
        final Field sourceField = Config.class.getDeclaredField("systemTableConfigSource");
        sourceField.setAccessible(true);
        final Object previousSource = sourceField.get(null);
        final boolean previousEnabled = Config.enableSystemTableConfigSource;
        final SystemTableConfigSource source = mock(SystemTableConfigSource.class);
        try {
            sourceField.set(null, source);
            Config.enableSystemTableConfigSource = true;
            for (final String value : new String[]{"true", "TRUE", "false", "invalid", "true,",
                    "true,,", "true,false", ",true", ""}) {
                when(source.getValue(FeatureFlagName.ALLOW_USERNAME_PASSWORD_AS_URL_PARAMS))
                        .thenReturn(value);
                assertEquals("System-table value: " + value, Boolean.parseBoolean(value),
                        BasicAuthentication.allowUrlCredentials());
            }
        } finally {
            sourceField.set(null, previousSource);
            Config.enableSystemTableConfigSource = previousEnabled;
        }
    }

    /** Explicit NONE, FALSE, empty and unrecognized values fail closed, never to a legacy mode. */
    @Test
    public void explicitDisableAndUnrecognizedValuesFailClosedToNone() {
        for (final String mode : new String[]{"NONE", "FALSE", "false", "", "invalid"}) {
            Config.setProperty("ALLOW_BASIC_AUTH", mode);
            assertEquals("mode '" + mode + "' must disable credential transports",
                    BasicAuthentication.Mode.NONE, BasicAuthentication.mode());
        }
    }
}