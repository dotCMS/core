package com.dotcms.cost;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.dotcms.UnitTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

/**
 * Unit tests for {@link RequestCostSnapshot}. The snapshot is the on-the-wire payload, so the
 * Jackson field-visibility config is the bug surface most likely to break silently — these tests
 * lock the JSON shape down.
 */
public class RequestCostSnapshotTest extends UnitTestBase {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private RequestCostSnapshot sample() {
        return new RequestCostSnapshot(
                "cluster-1",
                "server-7",
                "2026-05-19T18:48:00Z",
                60,
                1234L,
                5678.5d,
                4.6d,
                999_999L,
                12_345_678.25d,
                12.35d);
    }

    @Test
    public void test_serialization_includesAllExpectedFields() throws Exception {
        // When
        final JsonNode json = MAPPER.readTree(MAPPER.writeValueAsString(sample()));

        // Then
        assertTrue("missing clusterId", json.has("clusterId"));
        assertTrue("missing environmentId", json.has("environmentId"));
        assertTrue("missing timestamp", json.has("timestamp"));
        assertTrue("missing windowSeconds", json.has("windowSeconds"));
        assertTrue("missing windowRequests", json.has("windowRequests"));
        assertTrue("missing windowTokens", json.has("windowTokens"));
        assertTrue("missing windowAvgTokensPerRequest", json.has("windowAvgTokensPerRequest"));
        assertTrue("missing lifetimeRequests", json.has("lifetimeRequests"));
        assertTrue("missing lifetimeTokens", json.has("lifetimeTokens"));
        assertTrue("missing lifetimeAvgTokensPerRequest", json.has("lifetimeAvgTokensPerRequest"));
    }

    @Test
    public void test_serialization_preservesValues() throws Exception {
        // When
        final JsonNode json = MAPPER.readTree(MAPPER.writeValueAsString(sample()));

        // Then
        assertEquals("cluster-1", json.get("clusterId").asText());
        assertEquals("server-7", json.get("environmentId").asText());
        assertEquals("2026-05-19T18:48:00Z", json.get("timestamp").asText());
        assertEquals(60, json.get("windowSeconds").asInt());
        assertEquals(1234L, json.get("windowRequests").asLong());
        assertEquals(5678.5d, json.get("windowTokens").asDouble(), 0.0001d);
        assertEquals(4.6d, json.get("windowAvgTokensPerRequest").asDouble(), 0.0001d);
        assertEquals(999_999L, json.get("lifetimeRequests").asLong());
        assertEquals(12_345_678.25d, json.get("lifetimeTokens").asDouble(), 0.0001d);
        assertEquals(12.35d, json.get("lifetimeAvgTokensPerRequest").asDouble(), 0.0001d);
    }

    @Test
    public void test_serialization_emitsExactlyTenFields() throws Exception {
        // When
        final JsonNode json = MAPPER.readTree(MAPPER.writeValueAsString(sample()));

        // Then — guard against accidental leakage of internal fields if someone adds private
        // helpers later without updating the @JsonAutoDetect visibility
        assertEquals("unexpected fields on the wire", 10, json.size());
    }
}
