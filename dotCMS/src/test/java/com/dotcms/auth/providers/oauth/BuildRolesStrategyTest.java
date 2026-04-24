package com.dotcms.auth.providers.oauth;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dotcms.auth.providers.oauth.OAuthHelper.BuildRolesStrategy;
import com.dotmarketing.util.Config;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * Unit tests for {@link BuildRolesStrategy#resolve()} — the parser that maps the
 * {@code OAUTH_BUILD_ROLES_STRATEGY} config string to one of the enum values, with
 * {@link BuildRolesStrategy#ALL} as the safe fallback on unset / unrecognized input.
 */
class BuildRolesStrategyTest {

    private static final String PROP = "OAUTH_BUILD_ROLES_STRATEGY";

    private static void withConfig(final String configured, final Runnable body) {
        try (MockedStatic<Config> cfg = Mockito.mockStatic(Config.class)) {
            cfg.when(() -> Config.getStringProperty(PROP, BuildRolesStrategy.ALL.name()))
                    .thenReturn(configured);
            body.run();
        }
    }

    @Test
    void unsetProperty_fallsBackToAll() {
        // When the property is not configured, Config returns the default ALL.name()
        withConfig(BuildRolesStrategy.ALL.name(),
                () -> assertEquals(BuildRolesStrategy.ALL, BuildRolesStrategy.resolve()));
    }

    @Test
    void recognizedStrategies_allParse() {
        for (final BuildRolesStrategy s : BuildRolesStrategy.values()) {
            withConfig(s.name(), () -> assertEquals(s, BuildRolesStrategy.resolve()));
        }
    }

    @Test
    void lowercaseStrategy_isAccepted() {
        withConfig("staticadd",
                () -> assertEquals(BuildRolesStrategy.STATICADD, BuildRolesStrategy.resolve()));
    }

    @Test
    void paddedStrategy_isTrimmed() {
        withConfig("  IDP  ",
                () -> assertEquals(BuildRolesStrategy.IDP, BuildRolesStrategy.resolve()));
    }

    @Test
    void unknownStrategy_fallsBackToAllRatherThanThrowing() {
        // The whole point of the Try-based parse is to NOT bubble a misconfig up as a
        // runtime exception during login — the safe fallback is ALL.
        withConfig("NOT_A_REAL_STRATEGY",
                () -> assertEquals(BuildRolesStrategy.ALL, BuildRolesStrategy.resolve()));
    }

    @Test
    void blankStrategy_fallsBackToAll() {
        withConfig("   ",
                () -> assertEquals(BuildRolesStrategy.ALL, BuildRolesStrategy.resolve()));
    }
}
