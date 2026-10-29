package com.dotcms.rendering.velocity.viewtools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.dotmarketing.business.UserAPI;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
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

    /**
     * Method to test: {@link RestrictedViewToolAccess#hasAccess(User)}
     * When: the user is the system user, which is an administrator
     * Should: not grant access, because content it saved does not tell who wrote the code
     */
    @Test
    public void hasAccess_systemUser_isDenied() {
        final User system = mock(User.class);
        when(system.getUserId()).thenReturn(UserAPI.SYSTEM_USER_ID);
        when(system.isAdmin()).thenReturn(true);

        assertFalse(RestrictedViewToolAccess.hasAccess(system));
    }
}
