package com.dotcms.rest;

import static com.dotcms.rest.AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;

import com.dotmarketing.util.Config;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.junit.Test;
import org.mockito.MockedStatic;

/** Tests the secure default and explicit opt-in for anonymous API access. */
public class AnonymousAccessTest {

    /** The shipped configuration must require authentication unless explicitly overridden. */
    @Test
    public void testBundledConfigurationDisallowsAnonymousAccess() throws IOException {
        final Properties properties = new Properties();
        try (InputStream stream = getClass().getClassLoader()
                .getResourceAsStream("dotmarketing-config.properties")) {
            assertNotNull("The bundled configuration must be on the classpath", stream);
            properties.load(stream);
        }
        assertEquals("NONE", properties.getProperty(CONTENT_APIS_ALLOW_ANONYMOUS));
    }

    /** Uppercase, lowercase, and mixed-case values must resolve to the same access level. */
    @Test
    public void testAccessValuesAreCaseInsensitive() {
        assertEquals(AnonymousAccess.NONE, AnonymousAccess.from("NONE"));
        assertEquals(AnonymousAccess.NONE, AnonymousAccess.from("none"));
        assertEquals(AnonymousAccess.NONE, AnonymousAccess.from("NoNe"));
        assertEquals(AnonymousAccess.READ, AnonymousAccess.from("READ"));
        assertEquals(AnonymousAccess.READ, AnonymousAccess.from("read"));
        assertEquals(AnonymousAccess.READ, AnonymousAccess.from("ReAd"));
        assertEquals(AnonymousAccess.WRITE, AnonymousAccess.from("WRITE"));
        assertEquals(AnonymousAccess.WRITE, AnonymousAccess.from("write"));
        assertEquals(AnonymousAccess.WRITE, AnonymousAccess.from("WrItE"));
    }

    /** A missing property must use the secure fallback, not enable anonymous reads. */
    @Test
    public void testMissingPropertyDisallowsAnonymousAccess() {
        try (MockedStatic<Config> config = mockStatic(Config.class)) {
            config.when(() -> Config.getStringProperty(eq(CONTENT_APIS_ALLOW_ANONYMOUS),
                    anyString())).thenAnswer(invocation -> invocation.getArgument(1));

            assertEquals(AnonymousAccess.NONE, AnonymousAccess.systemSetting());
        }
    }

    /** Administrators can still explicitly enable anonymous reads, case-insensitively. */
    @Test
    public void testExplicitReadStillAllowsAnonymousReads() {
        try (MockedStatic<Config> config = mockStatic(Config.class)) {
            config.when(() -> Config.getStringProperty(eq(CONTENT_APIS_ALLOW_ANONYMOUS),
                    anyString())).thenReturn("read");

            assertEquals(AnonymousAccess.READ, AnonymousAccess.systemSetting());
        }
    }

    /** Administrators can still explicitly enable anonymous content submissions. */
    @Test
    public void testExplicitWriteStillAllowsAnonymousWrites() {
        try (MockedStatic<Config> config = mockStatic(Config.class)) {
            config.when(() -> Config.getStringProperty(eq(CONTENT_APIS_ALLOW_ANONYMOUS),
                    anyString())).thenReturn("WRITE");

            assertEquals(AnonymousAccess.WRITE, AnonymousAccess.systemSetting());
        }
    }
}
