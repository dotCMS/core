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
     * Verifies the ObjectMapper has a raised string-length cap to handle large contentlet fields.
     * Jackson 2.15+ defaults to 20 MB; without an explicit override PopulateContentletAsJSONJob
     * aborts when VersionedModelSerializer re-parses the serialized JSON during version migration.
     *
     * @see <a href="https://github.com/dotCMS/core/issues/35394">Issue #35394</a>
     */
    @Test
    public void testObjectMapper_WhenConfigured_ShouldHaveRaisedStringReadConstraint() {
        final StreamReadConstraints constraints = ContentletJsonHelper.INSTANCE.get()
                .objectMapper()
                .getFactory()
                .streamReadConstraints();

        assertNotNull("StreamReadConstraints must be configured on the ObjectMapper factory", constraints);
        // Default Jackson 2.15 cap is 20,000,000 — must be higher than that
        assertEquals(100 * 1024 * 1024, constraints.getMaxStringLength());
    }

    @Test
    public void testWriteAsString_WhenValueExceeds20MBLimit_ShouldNotThrowStreamConstraintsException()
            throws JsonProcessingException {
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
