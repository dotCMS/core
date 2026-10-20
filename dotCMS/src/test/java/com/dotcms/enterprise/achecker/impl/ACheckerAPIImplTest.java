package com.dotcms.enterprise.achecker.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dotcms.enterprise.achecker.ACheckerAPI;
import com.dotcms.enterprise.achecker.ACheckerRequestValidator;
import com.dotcms.exception.ExceptionUtil;
import com.dotmarketing.business.DotValidationException;
import com.dotmarketing.exception.DotRuntimeException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ACheckerAPIImpl#validate(java.util.Map)}: request values are validated before
 * any guideline lookup, and failures of the check itself are reported as a server error.
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

    @Test
    void checkFailureIsReportedAsServerError() {
        final ACheckerAPIImpl api = new ACheckerAPIImpl(request -> {
            throw new SQLException("database is not available");
        });

        final DotRuntimeException exception = assertThrows(DotRuntimeException.class,
                () -> api.validate(validData()));

        assertEquals(ACheckerAPIImpl.CHECK_FAILED_MESSAGE, exception.getMessage());
        assertFalse(exception instanceof DotValidationException,
                "A failure of the check itself must not be reported as rejected input");
        assertNull(exception.getCause(), "The underlying error must not be attached");
        assertFalse(ExceptionUtil.causedBy(exception, ExceptionUtil.BAD_REQUEST_EXCEPTIONS),
                "The REST layer must not answer a failure of the check itself with a client error");
    }

    @Test
    void rejectionFromTheCheckIsPassedThrough() {
        final DotValidationException rejection =
                new DotValidationException(ACheckerRequestValidator.UNSUPPORTED_GUIDELINES_MESSAGE);
        final ACheckerAPIImpl api = new ACheckerAPIImpl(request -> {
            throw rejection;
        });

        final DotValidationException exception = assertThrows(DotValidationException.class,
                () -> api.validate(validData()));

        assertSame(rejection, exception);
    }

    private static Map<String, String> validData() {
        final Map<String, String> validationData = new HashMap<>();
        validationData.put(ACheckerAPI.CONTENT, CONTENT);
        validationData.put(ACheckerAPI.GUIDELINES, "WCAG2-AA");
        validationData.put(ACheckerAPI.LANG, "en");
        return validationData;
    }
}
