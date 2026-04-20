package com.dotcms.content.business.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadConstraints;
import org.junit.Test;

/**
 * Unit tests for {@link ContentletJsonHelper}.
 */
public class ContentletJsonHelperTest {

    /**
     * Verifies that the ObjectMapper's factory has no effective string-length cap
     * (Integer.MAX_VALUE), so that contentlets with very large field values (e.g. >20 MB)
     * do not fail with a StreamConstraintsException during JSON serialization.
     *
     * <p>Background: Jackson 2.15 introduced a default 20,000,000-character limit via
     * {@link StreamReadConstraints}. The {@code VersionedModelSerializer} re-parses the
     * serialized JSON during version migration, triggering the read constraint.  Without
     * an explicit override, any contentlet whose JSON representation exceeds ~20 MB will
     * cause {@code PopulateContentletAsJSONJob} to abort entirely.
     *
     * @see <a href="https://github.com/dotCMS/core/issues/35394">Issue #35394</a>
     */
    @Test
    public void testObjectMapperHasUnboundedStringReadConstraint() {
        final StreamReadConstraints constraints = ContentletJsonHelper.INSTANCE.get()
                .objectMapper()
                .getFactory()
                .streamReadConstraints();

        assertNotNull("StreamReadConstraints must be configured on the ObjectMapper factory", constraints);
        assertEquals(
                "maxStringLength must be Integer.MAX_VALUE to support large contentlet fields (>20 MB)",
                Integer.MAX_VALUE,
                constraints.getMaxStringLength());
    }

    /**
     * Verifies that {@link ContentletJsonHelper#writeAsString(Object)} can serialize a plain
     * string value that exceeds Jackson's previous default 20 MB limit without throwing a
     * {@link com.fasterxml.jackson.core.exc.StreamConstraintsException}.
     */
    @Test
    public void testWriteAsStringHandlesValueExceeding20MBLimit() throws JsonProcessingException {
        // Build a string just over the old 20,000,000-character default limit
        final int targetLength = 20_100_000;
        final String largeValue = "x".repeat(targetLength);

        // Must not throw StreamConstraintsException
        final String json = ContentletJsonHelper.INSTANCE.get().writeAsString(largeValue);

        assertNotNull("Serialized JSON must not be null", json);
        // The resulting JSON wraps the string in quotes, so length > targetLength
        assertEquals(targetLength + 2, json.length());
    }
}
