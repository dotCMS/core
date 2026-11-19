package com.dotcms.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.dotmarketing.util.Config;
import org.junit.Test;

/**
 * Verifies the key-based obfuscation rules centralized in {@link ObfuscationUtil}.
 */
public class ObfuscationUtilTest {

    private static final String MASK = "*****";

    /** Both legacy substring matches and whole-word matches redact the entire value. */
    @Test
    public void masksBuiltInKeys() {
        assertEquals(MASK, ObfuscationUtil.obfuscateIfNeeded("password", "secretValue"));
        assertEquals(MASK, ObfuscationUtil.obfuscateIfNeeded("api.token", "secretValue"));
        assertEquals(MASK, ObfuscationUtil.obfuscateIfNeeded("DB_CREDENTIALS", "secretValue"));
    }

    @Test
    public void returnsValueUntouchedWhenKeyDoesNotMatch() {

        assertEquals("plainValue", ObfuscationUtil.obfuscateIfNeeded("some.random.setting", "plainValue"));
    }

    @Test
    public void handlesEmptyAndNullValues() {

        assertEquals("", ObfuscationUtil.obfuscateIfNeeded("password", ""));
        assertEquals("", ObfuscationUtil.obfuscateIfNeeded("password", (Object) null));
    }

    /**
     * The {@link Object} parameter is kept for backwards compatibility with the deprecated
     * {@code JVMInfoResource#obfuscateIfNeeded} signature; non-String values must not throw
     * a {@link ClassCastException}.
     */
    @Test
    public void handlesNonStringValues() {

        assertEquals(MASK, ObfuscationUtil.obfuscateIfNeeded("secret.number", 1234567892));
    }

    /** The custom-only filter does not inherit the always-on rules. */
    @Test
    public void keepsCustomRulesSeparate() {
        final String customPattern = Config.getStringProperty(
                "OBFUSCATE_SYSTEM_ENVIRONMENTAL_VARIABLES", ObfuscationUtil.DEFAULT_OBFUSCATE_PATTERN);
        assertEquals(customPattern, ObfuscationUtil.CUSTOM_PATTERN.pattern());
        if (ObfuscationUtil.DEFAULT_OBFUSCATE_PATTERN.equals(customPattern)) {
            assertFalse(ObfuscationUtil.matchesCustomPattern("DB_CREDENTIALS"));
        }
    }

}
