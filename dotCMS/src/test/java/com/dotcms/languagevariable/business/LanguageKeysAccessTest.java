package com.dotcms.languagevariable.business;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.RETURNS_DEFAULTS;
import static org.mockito.Mockito.mockStatic;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.SecurityLogger;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Unit tests for {@link LanguageKeysAccess} when there is no user. The cases with a user are
 * covered through its callers, {@code LanguageAjaxTest} and {@code EditLanguageKeysActionTest}.
 */
class LanguageKeysAccessTest {

    /** Names of the {@link SecurityLogger} methods called during a test. */
    private final List<String> securityLoggerCalls = new ArrayList<>();

    private MockedStatic<APILocator> apiLocator;
    private MockedStatic<SecurityLogger> securityLogger;

    @BeforeEach
    void setUp() {
        apiLocator = mockStatic(APILocator.class);
        securityLogger = mockStatic(SecurityLogger.class, invocation -> {
            securityLoggerCalls.add(invocation.getMethod().getName());
            return RETURNS_DEFAULTS.answer(invocation);
        });
    }

    @AfterEach
    void tearDown() {
        securityLogger.close();
        apiLocator.close();
    }

    /**
     * Given: no user, as the Struts language keys action passes when nobody is logged in
     * When: requireLanguagesOrLocalesPortlet is called
     * Then: a DotSecurityException is thrown, the rejection is written once to the security log
     * and no portlet is looked up
     */
    @Test
    void requireLanguagesOrLocalesPortlet_rejectsMissingUser() {
        assertThrows(DotSecurityException.class,
                () -> LanguageKeysAccess.requireLanguagesOrLocalesPortlet(null));

        assertEquals(1, securityLoggerCalls.stream().filter(name -> name.startsWith("log")).count(),
                "one security log entry per rejection, got " + securityLoggerCalls);
        apiLocator.verifyNoInteractions();
    }
}
