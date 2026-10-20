package com.dotcms.contenttype.business;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.RETURNS_DEFAULTS;
import static org.mockito.Mockito.mockStatic;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.SecurityLogger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Unit tests for {@link FieldReorderHelper} when there is no user. The other rules are covered
 * through its callers, {@code StructureAjaxTest} and {@code EditFieldActionReorderTest}.
 */
class FieldReorderHelperTest {

    private static final Map<String, String> SORT_ORDERS = Map.of("field-inode", "1");

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

    private void assertOneSecurityLogEntry() {
        assertEquals(1, securityLoggerCalls.stream().filter(name -> name.startsWith("log")).count(),
                "one security log entry per rejection, got " + securityLoggerCalls);
    }

    /**
     * Given: no user
     * When: reorder is called for a content type
     * Then: a DotSecurityException is thrown, the rejection is written once to the security log
     * and neither the content type nor its fields are looked up
     */
    @Test
    void reorder_rejectsMissingUser() {
        assertThrows(DotSecurityException.class,
                () -> FieldReorderHelper.reorder(null, "type-inode", SORT_ORDERS));

        assertOneSecurityLogEntry();
        apiLocator.verifyNoInteractions();
    }

    /**
     * Given: no user, as the Struts field action passes when nobody is logged in
     * When: reorderFieldsOfOneType is called
     * Then: a DotSecurityException is thrown, the rejection is written once to the security log
     * and no field is looked up
     */
    @Test
    void reorderFieldsOfOneType_rejectsMissingUser() {
        assertThrows(DotSecurityException.class,
                () -> FieldReorderHelper.reorderFieldsOfOneType(null, SORT_ORDERS));

        assertOneSecurityLogEntry();
        apiLocator.verifyNoInteractions();
    }
}
