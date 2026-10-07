package com.dotcms.rest.api.v1.index;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.content.index.IndexDocumentConstraints;
import com.dotcms.content.index.IndexDocumentViolation;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotmarketing.common.reindex.ReindexEntry;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.Test;

/**
 * Unit tests for the response of {@code GET /api/v1/index/failed} (#37269, AC-007):
 * {@link FailedReindexRecordView} for each record and {@link FailedReindexRecordsView} for the
 * whole listing.
 *
 * <h2>What is at stake</h2>
 * <p>The old {@code GET /api/v1/esindex/failed} embeds each failed record's full contentlet. Once
 * the failed records are the oversized documents #37269 withholds, a dozen of them make a 168 MB
 * response and the Maintenance "Download Failed Records" button crashes the browser tab. The new
 * response identifies the content, says why it failed — including, for a document-limit
 * violation, which limit, which field and by how much — and never carries field values.</p>
 *
 * <pre>
 *   ./mvnw test -pl :dotcms-core -Dmaven.build.cache.enabled=false \
 *       -Dtest=FailedReindexRecordViewTest
 * </pre>
 */
public class FailedReindexRecordViewTest {

    private static final Set<String> RECORD_KEYS = new TreeSet<>(Set.of("identifier", "inode",
            "title", "contentTypeVariable", "languageId", "pendingOperation", "failedAttempts",
            "priority", "lastFailureReason", "violation"));

    private static final Set<String> VIOLATION_KEYS = new TreeSet<>(Set.of("limit", "fieldPath",
            "actual", "allowed", "unit"));

    private static final ObjectMapper MAPPER = DotObjectMapperProvider.createDefaultMapper();

    private static final String VIOLATION_REASON = new IndexDocumentViolation("id-1", "inode-1", 1L,
            new IndexDocumentConstraints.Violation(IndexDocumentConstraints.Kind.STRING_LENGTH,
                    "catchall", 42_000_792L, 20_000_000L)).toFailureReason();

    /**
     * Given Scenario: A failed row for a contentlet whose body is 1,000,000 characters, failed
     * because its {@code catchall} exceeded the string limit, after five failed attempts.
     * Expected Result: The record carries the identity keys, the attempts derived from the
     * priority, the reason, and a {@code violation} naming the limit, field, actual length,
     * allowed length and unit; no field value is serialized.
     */
    @Test
    public void test_violationRow_serializesIdentityAndViolation_noFieldValues() throws Exception {
        final String body = "x".repeat(1_000_000);
        final Contentlet contentlet = contentlet("Oversized 0", body);
        final ReindexEntry row = ReindexEntry.builder().id(1L).identToIndex("id-1").priority(705)
                .lastResult(VIOLATION_REASON).build();

        final String json = MAPPER.writeValueAsString(FailedReindexRecordView.from(row, contentlet));

        final JsonNode node = MAPPER.readTree(json);
        assertEquals(RECORD_KEYS, keys(node));
        assertEquals("id-1", node.get("identifier").asText());
        assertEquals("inode-1", node.get("inode").asText());
        assertEquals("Oversized 0", node.get("title").asText());
        assertEquals("BulkDiscardTest", node.get("contentTypeVariable").asText());
        assertEquals(1L, node.get("languageId").asLong());
        assertEquals("reindex", node.get("pendingOperation").asText());
        assertEquals(5, node.get("failedAttempts").asInt());
        assertEquals(705, node.get("priority").asInt());
        assertEquals(VIOLATION_REASON, node.get("lastFailureReason").asText());

        final JsonNode violation = node.get("violation");
        assertEquals(VIOLATION_KEYS, keys(violation));
        assertEquals("maxStringLength", violation.get("limit").asText());
        assertEquals("catchall", violation.get("fieldPath").asText());
        assertEquals(42_000_792L, violation.get("actual").asLong());
        assertEquals(20_000_000L, violation.get("allowed").asLong());
        assertEquals("chars", violation.get("unit").asText());

        assertFalse("no field value may be serialized", json.contains("xxxxxxxxxx"));
        assertTrue("size must not depend on the content: " + json.length(), json.length() < 2_000);
    }

    /**
     * Given Scenario: A row that failed for a reason that is not a document-limit violation (the
     * whole OpenSearch request rejected with HTTP 413).
     * Expected Result: The reason is reported as recorded and {@code violation} is an explicit
     * {@code null}.
     */
    @Test
    public void test_transportFailure_hasReason_andNullViolation() throws Exception {
        final String reason = "method [POST], host [http://opensearch:9200], URI [/_bulk],"
                + " status line [HTTP/1.1 413 Request Entity Too Large]";
        final ReindexEntry row = ReindexEntry.builder().id(3L).identToIndex("id-3").priority(505)
                .lastResult(reason).build();

        final JsonNode node = MAPPER.readTree(MAPPER.writeValueAsString(
                FailedReindexRecordView.from(row, contentlet("Large 0", "ok"))));

        assertEquals(RECORD_KEYS, keys(node));
        assertEquals(reason, node.get("lastFailureReason").asText());
        assertTrue("violation must be an explicit null", node.get("violation").isNull());
    }

    /**
     * Given Scenario: A parked removal whose content no longer exists and whose failure was
     * recorded without any message.
     * Expected Result: Every key is still present; the content-derived values, the reason and the
     * violation are explicit {@code null}s; the pending operation is {@code delete}.
     */
    @Test
    public void test_deleteRow_withoutContentletOrReason_reportsExplicitNulls() throws Exception {
        final ReindexEntry row = ReindexEntry.builder().id(2L).identToIndex("id-2").priority(505)
                .isDelete(true).build();

        final JsonNode node = MAPPER.readTree(MAPPER.writeValueAsString(
                FailedReindexRecordView.from(row, null)));

        assertEquals(RECORD_KEYS, keys(node));
        assertEquals("id-2", node.get("identifier").asText());
        assertEquals("delete", node.get("pendingOperation").asText());
        assertTrue(node.get("inode").isNull());
        assertTrue(node.get("title").isNull());
        assertTrue(node.get("contentTypeVariable").isNull());
        assertTrue(node.get("languageId").isNull());
        assertTrue(node.get("lastFailureReason").isNull());
        assertTrue(node.get("violation").isNull());
    }

    /**
     * Given Scenario: A failure recorded without a message: the journal stores an empty string,
     * because {@code markAsFailed} turns a {@code null} message into {@code ""}.
     * Expected Result: The reason is reported as an explicit {@code null}, the same as no reason.
     */
    @Test
    public void test_emptyReason_isReportedAsNull() throws Exception {
        final ReindexEntry row = ReindexEntry.builder().id(4L).identToIndex("id-4").priority(505)
                .lastResult("").build();

        final JsonNode node = MAPPER.readTree(MAPPER.writeValueAsString(
                FailedReindexRecordView.from(row, null)));

        assertTrue(node.get("lastFailureReason").isNull());
        assertTrue(node.get("violation").isNull());
    }

    /**
     * Given Scenario: The listing of two failed records, with a string limit of 10, a nesting
     * limit of 3 and a retry limit of 5.
     * Expected Result: The root carries the document limits in force, the retry policy, the
     * record count and the records.
     */
    @Test
    public void test_listing_carriesLimitsRetryPolicyAndCount() throws Exception {
        final FailedReindexRecordView first = FailedReindexRecordView.from(
                ReindexEntry.builder().id(1L).identToIndex("id-1").priority(505).build(), null);
        final FailedReindexRecordView second = FailedReindexRecordView.from(
                ReindexEntry.builder().id(2L).identToIndex("id-2").priority(505).build(), null);

        final JsonNode node = MAPPER.readTree(MAPPER.writeValueAsString(
                FailedReindexRecordsView.of(List.of(first, second), 10, 3, 5)));

        assertEquals(new TreeSet<>(Set.of("documentLimits", "retryPolicy", "failedRecordCount",
                "failedRecords")), keys(node));
        assertEquals(10, node.get("documentLimits").get("maxStringLength").asInt());
        assertEquals(3, node.get("documentLimits").get("maxNestingDepth").asInt());
        assertEquals(5, node.get("retryPolicy").get("maxFailedAttempts").asInt());
        assertEquals(2, node.get("failedRecordCount").asInt());
        assertEquals(2, node.get("failedRecords").size());
    }

    private static Contentlet contentlet(final String title, final String body) {
        final ContentType type = mock(ContentType.class);
        when(type.variable()).thenReturn("BulkDiscardTest");
        final Map<String, Object> fields = new HashMap<>();
        fields.put("body", body);
        final Contentlet contentlet = mock(Contentlet.class);
        when(contentlet.getTitle()).thenReturn(title);
        when(contentlet.getInode()).thenReturn("inode-1");
        when(contentlet.getContentType()).thenReturn(type);
        when(contentlet.getLanguageId()).thenReturn(1L);
        when(contentlet.getMap()).thenReturn(fields);
        when(contentlet.get("body")).thenReturn(body);
        return contentlet;
    }

    private static Set<String> keys(final JsonNode node) {
        final Set<String> keys = new TreeSet<>();
        for (final Iterator<String> names = node.fieldNames(); names.hasNext(); ) {
            keys.add(names.next());
        }
        return keys;
    }
}
