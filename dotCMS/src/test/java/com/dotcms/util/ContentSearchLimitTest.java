package com.dotcms.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dotmarketing.util.Config;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the maximum number of results a content search request may ask for: values up to
 * the maximum are kept, larger values are lowered to it, and the maximum can be lowered through
 * configuration but never raised above the default.
 */
class ContentSearchLimitTest {

    private static final String PROPERTY = ContentSearchLimit.MAX_LIMIT_PROPERTY;

    private String previousValue;

    @BeforeEach
    void setUp() {
        previousValue = Config.getStringProperty(PROPERTY, null);
        Config.setProperty(PROPERTY, null);
    }

    @AfterEach
    void tearDown() {
        Config.setProperty(PROPERTY, previousValue);
    }

    @Test
    void cap_keepsAValueBelowTheMaximum() {
        // Given the default configuration
        // When a limit of 50 is capped
        // Then 50 is returned
        assertEquals(50, ContentSearchLimit.cap(50));
    }

    @Test
    void cap_keepsTheSmallestPositiveValue() {
        // Given the default configuration
        // When a limit of 1 is capped
        // Then 1 is returned
        assertEquals(1, ContentSearchLimit.cap(1));
    }

    @Test
    void cap_keepsAValueEqualToTheMaximum() {
        // Given the default configuration (maximum 10000)
        // When a limit of 10000 is capped
        // Then 10000 is returned
        assertEquals(10000, ContentSearchLimit.cap(10000));
    }

    @Test
    void cap_lowersTheFirstValueAboveTheMaximum() {
        // Given the default configuration (maximum 10000)
        // When a limit of 10001 is capped
        // Then 10000 is returned
        assertEquals(10000, ContentSearchLimit.cap(10001));
    }

    @Test
    void cap_lowersTheLargestIntToTheMaximum() {
        // Given the default configuration (maximum 10000)
        // When Integer.MAX_VALUE is capped
        // Then 10000 is returned
        assertEquals(10000, ContentSearchLimit.cap(Integer.MAX_VALUE));
    }

    @Test
    void cap_keepsZeroUnchanged() {
        // Given the default configuration
        // When a limit of 0 is capped
        // Then 0 is returned, so each caller keeps its own meaning for it
        assertEquals(0, ContentSearchLimit.cap(0));
    }

    @Test
    void cap_keepsNegativeValuesUnchanged() {
        // Given the default configuration
        // When -1 and Integer.MIN_VALUE are capped
        // Then each is returned unchanged
        assertEquals(-1, ContentSearchLimit.cap(-1));
        assertEquals(Integer.MIN_VALUE, ContentSearchLimit.cap(Integer.MIN_VALUE));
    }

    @Test
    void cap_usesAConfiguredMaximumBelowTheDefault() {
        // Given a configured maximum of 1000
        Config.setProperty(PROPERTY, "1000");

        // When limits of 1000 and 1001 are capped
        // Then both return 1000
        assertEquals(1000, ContentSearchLimit.cap(1000));
        assertEquals(1000, ContentSearchLimit.cap(1001));
    }

    @Test
    void cap_keepsZeroUnchangedWithAConfiguredMaximum() {
        // Given a configured maximum of 1000
        Config.setProperty(PROPERTY, "1000");

        // When a limit of 0 is capped
        // Then 0 is returned
        assertEquals(0, ContentSearchLimit.cap(0));
    }

    @Test
    void cap_acceptsAConfiguredMaximumOfOne() {
        // Given a configured maximum of 1
        Config.setProperty(PROPERTY, "1");

        // When a limit of 2 is capped
        // Then 1 is returned
        assertEquals(1, ContentSearchLimit.cap(2));
    }

    @Test
    void cap_acceptsAConfiguredMaximumEqualToTheCeiling() {
        // Given a configured maximum of 10000
        Config.setProperty(PROPERTY, "10000");

        // When a limit of 10001 is capped
        // Then 10000 is returned
        assertEquals(10000, ContentSearchLimit.cap(10001));
    }

    @Test
    void cap_neverExceedsTheCeilingWhenTheConfiguredMaximumIsHigher() {
        // Given a configured maximum of 20000
        Config.setProperty(PROPERTY, "20000");

        // When a limit of 15000 is capped
        // Then 10000 is returned
        assertEquals(10000, ContentSearchLimit.cap(15000));
    }

    @Test
    void cap_usesTheDefaultWhenTheConfiguredMaximumIsZero() {
        // Given a configured maximum of 0
        Config.setProperty(PROPERTY, "0");

        // When limits of 5000 and 10001 are capped
        // Then 5000 and 10000 are returned
        assertEquals(5000, ContentSearchLimit.cap(5000));
        assertEquals(10000, ContentSearchLimit.cap(10001));
    }

    @Test
    void cap_usesTheDefaultWhenTheConfiguredMaximumIsNegative() {
        // Given a configured maximum of -5
        Config.setProperty(PROPERTY, "-5");

        // When a limit of 10001 is capped
        // Then 10000 is returned
        assertEquals(10000, ContentSearchLimit.cap(10001));
    }

    @Test
    void cap_usesTheDefaultWhenTheConfiguredMaximumIsNotANumber() {
        // Given a configured maximum that is not a number
        Config.setProperty(PROPERTY, "abc");

        // When a limit of 10001 is capped
        // Then 10000 is returned and nothing is thrown
        assertEquals(10000, ContentSearchLimit.cap(10001));
    }

    @Test
    void cap_acceptsAConfiguredMaximumWithSurroundingSpaces() {
        // Given a configured maximum of " 1000 "
        Config.setProperty(PROPERTY, " 1000 ");

        // When a limit of 1001 is capped
        // Then 1000 is returned
        assertEquals(1000, ContentSearchLimit.cap(1001));
    }

    @Test
    void cap_usesTheDefaultWhenTheConfiguredMaximumIsEmpty() {
        // Given a configured maximum that is an empty value
        Config.setProperty(PROPERTY, "");

        // When a limit of 10001 is capped
        // Then 10000 is returned
        assertEquals(10000, ContentSearchLimit.cap(10001));
    }

    @Test
    void cap_followsAChangeToTheConfiguredMaximum() {
        // Given a configured maximum of 1000
        Config.setProperty(PROPERTY, "1000");

        // When a limit of 5000 is capped, the configured maximum is removed, and 5000 is capped again
        final int withConfiguredMaximum = ContentSearchLimit.cap(5000);
        Config.setProperty(PROPERTY, null);
        final int withDefaultMaximum = ContentSearchLimit.cap(5000);

        // Then the first call returns 1000 and the second 5000
        assertEquals(1000, withConfiguredMaximum);
        assertEquals(5000, withDefaultMaximum);
    }
}
