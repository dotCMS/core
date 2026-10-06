package com.dotcms.ai.rest;

import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.AppKeys;
import com.dotcms.security.apps.Secret;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CompletionsResource#isInheritedConfig(String, AppConfig)}.
 *
 * The rule is small but it decides a label the user reads on the Config Values screen, and the
 * "nothing configured anywhere" case is the one it exists to get right.
 */
public class CompletionsResourceTest {

    private static final String SITE = "demo.dotcms.com";
    private static final String SYSTEM_HOST = "System Host";
    private static final String PROVIDER_CONFIG = "{\"chat\":{\"provider\":\"openai\"}}";

    /**
     * The site has its own configuration, so nothing was inherited.
     */
    @Test
    public void test_isInheritedConfig_ownConfig() {
        assertFalse(CompletionsResource.isInheritedConfig(SITE, appConfig(SITE, PROVIDER_CONFIG)));
    }

    /**
     * ConfigService fell back to the System Host's secrets and found some, which is what
     * "inherited" means.
     */
    @Test
    public void test_isInheritedConfig_fellBackToSystemHost() {
        assertTrue(CompletionsResource.isInheritedConfig(
                SITE, appConfig(SYSTEM_HOST, PROVIDER_CONFIG)));
    }

    /**
     * Nothing is configured anywhere. ConfigService still reports the System Host as the
     * resolved host, so without the blank check this claimed to have inherited settings it
     * never found.
     */
    @Test
    public void test_isInheritedConfig_nothingConfiguredAnywhere() {
        assertFalse(CompletionsResource.isInheritedConfig(SITE, appConfig(SYSTEM_HOST, null)));
        assertFalse(CompletionsResource.isInheritedConfig(SITE, appConfig(SYSTEM_HOST, "  ")));
    }

    /**
     * Hostnames are compared without regard to case, as hostnames are.
     */
    @Test
    public void test_isInheritedConfig_hostnameCaseIsNotAChange() {
        assertFalse(CompletionsResource.isInheritedConfig(
                "DEMO.dotCMS.com", appConfig(SITE, PROVIDER_CONFIG)));
    }

    private static AppConfig appConfig(final String host, final String providerConfigJson) {
        final Map<String, Secret> secrets = new HashMap<>();
        if (providerConfigJson != null) {
            final Secret secret = mock(Secret.class);
            when(secret.getString()).thenReturn(providerConfigJson);
            secrets.put(AppKeys.PROVIDER_CONFIG.key, secret);
        }

        return new AppConfig(host, secrets);
    }
}
