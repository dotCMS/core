package com.dotmarketing.common.reindex;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.content.index.IndexTag;
import com.dotcms.content.index.domain.IndexBulkItemResult;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Test;

/**
 * Unit tests for how {@link BulkProcessorListener} reports failures of the OpenSearch <em>shadow</em>
 * bulk leg in dual-write phases (issue #36222 follow-up).
 *
 * <h2>What this covers</h2>
 * <p>The listener used to log one WARN per failed item and nothing else. When the OS user loses a
 * permission, every document in every batch is rejected with the same message, which produced
 * hundreds of thousands of identical lines and — worse — no signal at all that the shadow store had
 * stopped receiving writes altogether. QA hit exactly that on TC-056: ~900 identical warnings in one
 * minute, and a reindex that looked successful while OpenSearch received nothing.</p>
 *
 * <p>Both helpers under test are pure functions, so no container, cluster or config is needed. The
 * classification of the rejection text itself lives in the OpenSearch adapter and is exercised by
 * {@code OSIndexAPIImplConnectionClassifyTest}; here we only assert the escalation policy built on
 * top of it.</p>
 *
 * <pre>
 *   ./mvnw test -pl :dotcms-core -Dmaven.build.cache.enabled=false \
 *       -Dtest=BulkProcessorListenerShadowFailureTest
 * </pre>
 *
 * @author Fabrizzio Araya
 */
public class BulkProcessorListenerShadowFailureTest {

    /** Verbatim rejection emitted by the OpenSearch security plugin for a denied bulk write. */
    private static final String SECURITY_EXCEPTION =
            "security_exception: no permissions for [indices:data/write/bulk[s],"
                    + " indices:data/write/index] and User [name=non-admin, backend_roles=[],"
                    + " requestedTenant=null]";

    /** A per-document problem: the batch is healthy, this one contentlet is not. */
    private static final String MAPPER_PARSING_EXCEPTION =
            "mapper_parsing_exception: failed to parse field [myNumber] of type [long]";

    private static IndexBulkItemResult failed(final String id, final String message) {
        return IndexBulkItemResult.builder().id(id).failed(true).failureMessage(message).build();
    }

    private static IndexBulkItemResult succeeded(final String id) {
        return IndexBulkItemResult.builder().id(id).failed(false).build();
    }

    private static List<IndexBulkItemResult> allFailedWith(final int count, final String message) {
        return IntStream.range(0, count)
                .mapToObj(i -> failed("id-" + i, message))
                .collect(Collectors.toList());
    }

    // ---- summarizeFailures ----------------------------------------------------------------------

    @Test
    public void identicalFailures_areCollapsedIntoOneEntryWithItsCount() {
        // The whole point: one log line for 185 rejections, not 185 lines.
        final Map<String, Long> summary =
                BulkProcessorListener.summarizeFailures(allFailedWith(185, SECURITY_EXCEPTION));

        assertEquals("Identical rejections must collapse to a single entry", 1, summary.size());
        assertEquals(Long.valueOf(185L), summary.get(SECURITY_EXCEPTION));
    }

    @Test
    public void successfulItems_areNotCounted() {
        final List<IndexBulkItemResult> results = List.of(
                succeeded("ok-1"),
                failed("bad-1", MAPPER_PARSING_EXCEPTION),
                succeeded("ok-2"));

        final Map<String, Long> summary = BulkProcessorListener.summarizeFailures(results);

        assertEquals(1, summary.size());
        assertEquals(Long.valueOf(1L), summary.get(MAPPER_PARSING_EXCEPTION));
    }

    @Test
    public void distinctFailures_areKeptApart() {
        final List<IndexBulkItemResult> results = List.of(
                failed("bad-1", SECURITY_EXCEPTION),
                failed("bad-2", MAPPER_PARSING_EXCEPTION),
                failed("bad-3", SECURITY_EXCEPTION));

        final Map<String, Long> summary = BulkProcessorListener.summarizeFailures(results);

        assertEquals(2, summary.size());
        assertEquals(Long.valueOf(2L), summary.get(SECURITY_EXCEPTION));
        assertEquals(Long.valueOf(1L), summary.get(MAPPER_PARSING_EXCEPTION));
    }

    @Test
    public void missingFailureMessage_doesNotProduceANullKey() {
        final Map<String, Long> summary =
                BulkProcessorListener.summarizeFailures(List.of(failed("bad-1", null)));

        assertEquals(Long.valueOf(1L),
                summary.get(BulkProcessorListener.NO_FAILURE_MESSAGE));
    }

    @Test
    public void healthyBatch_producesNothing() {
        assertTrue(BulkProcessorListener.summarizeFailures(
                List.of(succeeded("ok-1"), succeeded("ok-2"))).isEmpty());
    }

    // ---- systemicFailureEscalation -------------------------------------------------------------

    @Test
    public void wholeBatchRejectedByPermissions_escalates() {
        final List<IndexBulkItemResult> results = allFailedWith(185, SECURITY_EXCEPTION);

        final Optional<String> escalation = BulkProcessorListener.systemicFailureEscalation(
                IndexTag.OS, results.size(), BulkProcessorListener.summarizeFailures(results));

        assertTrue("A batch rejected in full for a permission problem must escalate",
                escalation.isPresent());
        final String message = escalation.get();
        assertTrue("The cause must be named so the operator knows what to fix: " + message,
                message.contains("AUTH_FORBIDDEN"));
        assertTrue("The batch size must be reported: " + message, message.contains("185"));
        assertTrue("The consequence — do not promote a diverging store — must be stated: " + message,
                message.contains("must not be promoted"));
        assertTrue("The verbatim rejection must be kept for support: " + message,
                message.contains(SECURITY_EXCEPTION));
    }

    @Test
    public void partiallyRejectedBatch_doesNotEscalate() {
        // One rejected document out of many is a content problem, not a migration blocker — even
        // when its message would classify as systemic on its own.
        final List<IndexBulkItemResult> results = List.of(
                succeeded("ok-1"),
                succeeded("ok-2"),
                failed("bad-1", SECURITY_EXCEPTION));

        assertFalse(BulkProcessorListener.systemicFailureEscalation(IndexTag.OS, results.size(),
                BulkProcessorListener.summarizeFailures(results)).isPresent());
    }

    @Test
    public void wholeBatchRejectedByAMappingProblem_doesNotEscalate() {
        // Every item failing is not enough: a batch of documents that share a broken field is a
        // content problem, and halting the operator on it would be noise.
        final List<IndexBulkItemResult> results = allFailedWith(10, MAPPER_PARSING_EXCEPTION);

        assertFalse("An unclassifiable cause must not be reported as a systemic failure",
                BulkProcessorListener.systemicFailureEscalation(IndexTag.OS, results.size(),
                        BulkProcessorListener.summarizeFailures(results)).isPresent());
    }

    @Test
    public void mixedCauses_escalateOnTheDominantOne() {
        // Realistic shape of a denied batch: the permission rejection dominates, one document also
        // happens to be malformed. The escalation must name the permission problem.
        final List<IndexBulkItemResult> results = List.of(
                failed("bad-1", SECURITY_EXCEPTION),
                failed("bad-2", SECURITY_EXCEPTION),
                failed("bad-3", MAPPER_PARSING_EXCEPTION));

        final Optional<String> escalation = BulkProcessorListener.systemicFailureEscalation(
                IndexTag.OS, results.size(), BulkProcessorListener.summarizeFailures(results));

        assertTrue(escalation.isPresent());
        assertTrue(escalation.get().contains("AUTH_FORBIDDEN"));
    }

    @Test
    public void emptyBatch_doesNotEscalate() {
        assertFalse(BulkProcessorListener.systemicFailureEscalation(
                IndexTag.OS, 0, Map.of()).isPresent());
    }
}
