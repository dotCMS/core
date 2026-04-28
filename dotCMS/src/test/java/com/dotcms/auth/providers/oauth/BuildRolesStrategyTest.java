package com.dotcms.auth.providers.oauth;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dotcms.auth.providers.oauth.OAuthHelper.BuildRolesStrategy;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link BuildRolesStrategy#resolve(String)} — the parser that maps
 * the dotAuth {@code buildRolesStrategy} value to one of the enum values, with
 * {@link BuildRolesStrategy#ALL} as the safe fallback on unset / unrecognized input.
 */
class BuildRolesStrategyTest {

    @Test
    void recognizedStrategies_allParse() {
        for (final BuildRolesStrategy s : BuildRolesStrategy.values()) {
            assertEquals(s, BuildRolesStrategy.resolve(s.name()));
        }
    }

    @Test
    void lowercaseStrategy_isAccepted() {
        assertEquals(BuildRolesStrategy.STATICADD, BuildRolesStrategy.resolve("staticadd"));
    }

    @Test
    void paddedStrategy_isTrimmed() {
        assertEquals(BuildRolesStrategy.IDP, BuildRolesStrategy.resolve("  IDP  "));
    }

    @Test
    void unknownStrategy_fallsBackToAllRatherThanThrowing() {
        // The whole point of the Try-based parse is to NOT bubble a misconfig up as a
        // runtime exception during login — the safe fallback is ALL.
        assertEquals(BuildRolesStrategy.ALL, BuildRolesStrategy.resolve("NOT_A_REAL_STRATEGY"));
    }

    @Test
    void blankStrategy_fallsBackToAll() {
        assertEquals(BuildRolesStrategy.ALL, BuildRolesStrategy.resolve("   "));
    }

    @Test
    void nullStrategy_fallsBackToAll() {
        assertEquals(BuildRolesStrategy.ALL, BuildRolesStrategy.resolve(null));
    }
}
