package com.dotcms.rendering.velocity.viewtools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;

import com.dotmarketing.util.Config;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Unit tests for the configuration defaults of {@link RestrictedViewToolAccess}.
 */
public class RestrictedViewToolAccessDefaultsTest {

    private MockedStatic<Config> mockedConfig;

    @BeforeEach
    public void setUp() {
        mockedConfig = mockStatic(Config.class);
        mockedConfig.when(() -> Config.getStringArrayProperty(
                eq(RestrictedViewToolAccess.RESTRICTED_TOOLS_PROPERTY), any(String[].class)))
                .thenAnswer(invocation -> invocation.getArgument(1));
    }

    @AfterEach
    public void tearDown() {
        mockedConfig.close();
    }

    /**
     * Method to test: {@link RestrictedViewToolAccess#restrictedToolKeys()}
     * When: nothing is configured
     * Should: list the tools that work outside of the caller's permissions, and not the
     * configuration tool, which limits what it returns on its own
     */
    @Test
    public void restrictedToolKeys_defaultList() {
        assertEquals(List.of("filetool", "macro", "mailer"),
                List.copyOf(RestrictedViewToolAccess.restrictedToolKeys()));
    }

    /**
     * Method to test: {@link RestrictedViewToolAccess#restrictedToolKeys()}
     * When: {@link RestrictedViewToolAccess#RESTRICTED_TOOLS_PROPERTY} is set
     * Should: use it, trimmed and without blanks
     */
    @Test
    public void restrictedToolKeys_configuredList() {
        mockedConfig.when(() -> Config.getStringArrayProperty(
                eq(RestrictedViewToolAccess.RESTRICTED_TOOLS_PROPERTY), any(String[].class)))
                .thenReturn(new String[]{" macro ", "", "mailer"});

        final Set<String> keys = RestrictedViewToolAccess.restrictedToolKeys();
        assertEquals(Set.of("macro", "mailer"), keys);
        assertTrue(keys.stream().noneMatch(String::isBlank));
    }
}
