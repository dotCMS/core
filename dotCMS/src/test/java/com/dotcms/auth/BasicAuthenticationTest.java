package com.dotcms.auth;

import static org.junit.Assert.assertEquals;

import com.dotmarketing.util.Config;
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