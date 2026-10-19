package com.dotcms.enterprise.achecker.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dotcms.enterprise.achecker.ACheckerAPI;
import com.dotcms.enterprise.achecker.ACheckerRequestValidator;
import com.dotmarketing.business.DotValidationException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the request validation performed by {@link ACheckerAPIImpl#validate(java.util.Map)}
 * before any guideline lookup.
 */
class ACheckerAPIImplTest {

    private static final String CONTENT = "<p>Hello</p>";

    @Test
    void rejectedGuidelineTokenFailsBeforeLookup() {
        final Map<String, String> validationData = new HashMap<>();
        validationData.put(ACheckerAPI.CONTENT, CONTENT);
        validationData.put(ACheckerAPI.GUIDELINES, "WCAG2-AA,bad value");
        validationData.put(ACheckerAPI.LANG, "en");

        final DotValidationException exception = assertThrows(DotValidationException.class,
                () -> new ACheckerAPIImpl().validate(validationData));

        assertEquals(ACheckerRequestValidator.UNSUPPORTED_GUIDELINES_MESSAGE, exception.getMessage());
        assertNull(exception.getCause(), "The rejection must not wrap a lookup failure");
    }

    @Test
    void rejectedLangFailsBeforeLookup() {
        final Map<String, String> validationData = new HashMap<>();
        validationData.put(ACheckerAPI.CONTENT, CONTENT);
        validationData.put(ACheckerAPI.GUIDELINES, "WCAG2-AA");
        validationData.put(ACheckerAPI.LANG, "engl");

        final DotValidationException exception = assertThrows(DotValidationException.class,
                () -> new ACheckerAPIImpl().validate(validationData));

        assertEquals(ACheckerRequestValidator.UNSUPPORTED_LANG_MESSAGE, exception.getMessage());
        assertNull(exception.getCause(), "The rejection must not wrap a lookup failure");
    }
}
