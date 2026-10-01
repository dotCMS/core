package com.dotcms.util;

import static org.junit.Assert.assertEquals;

import com.dotmarketing.util.Config;
import org.junit.Test;

/**
 * Verifies the key-based obfuscation rules centralized in {@link ObfuscationUtil}.
 */
public class ObfuscationUtilTest {

    @Test
    public void obfuscatesValueWhenKeyMatchesBuiltInPattern() {

        assertEquals("s*********e", ObfuscationUtil.obfuscateIfNeeded("password", "secretValue"));
        assertEquals("s*********e", ObfuscationUtil.obfuscateIfNeeded("api.token", "secretValue"));
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

        assertEquals("1*********2", ObfuscationUtil.obfuscateIfNeeded("secret.number", 1234567892));
    }

    @Test
    public void customPatternPropertyIsHonored() {

        final String customPattern = Config.getStringProperty(
                "OBFUSCATE_SYSTEM_ENVIRONMENTAL_VARIABLES", ObfuscationUtil.DEFAULT_OBFUSCATE_PATTERN);
        assertEquals(ObfuscationUtil.CUSTOM_PATTERN.pattern(), customPattern);
        assertEquals(ObfuscationUtil.BASE_PATTERN.pattern(), ObfuscationUtil.DEFAULT_OBFUSCATE_PATTERN);
    }

}
