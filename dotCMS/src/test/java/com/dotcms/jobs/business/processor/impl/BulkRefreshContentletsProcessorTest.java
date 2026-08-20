package com.dotcms.jobs.business.processor.impl;

import static com.dotcms.jobs.business.processor.impl.BulkRefreshContentletsProcessor.PARAM_CONTENTLET_IDS;
import static com.dotcms.jobs.business.processor.impl.BulkRefreshContentletsProcessor.PARAM_INCLUDE_DEPENDENCIES;
import static com.dotcms.jobs.business.processor.impl.BulkRefreshContentletsProcessor.PARAM_INCLUDE_ITEM_RESULTS;
import static com.dotcms.jobs.business.processor.impl.BulkRefreshContentletsProcessor.PARAM_USER_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.content.elasticsearch.business.ContentletIndexAPI;
import com.dotcms.jobs.business.error.JobValidationException;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.job.JobState;
import com.dotcms.jobs.business.processor.DefaultProgressTracker;
import com.dotcms.rest.api.v1.content.bulkrefresh.BulkRefreshItemResult;
import com.dotcms.rest.api.v1.content.bulkrefresh.BulkRefreshItemStatus;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.ContentletCache;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

/**
 * Unit tests for {@link BulkRefreshContentletsProcessor}.
 * <p>
 * The processor's collaborators are injected so these tests can exercise the parts that carry the
 * real risk — per-identifier de-duplication, failure isolation, the cancel boundary and the delta
 * cursor — without a database or an index.
 */
public class BulkRefreshContentletsProcessorTest {

    private static final String USER_ID = "user-1";

    private ContentletAPI contentletAPI;
    private IdentifierAPI identifierAPI;
    private ContentletIndexAPI contentletIndexAPI;
    private ContentletCache contentletCache;
    private UserAPI userAPI;
    private BulkRefreshContentletsProcessor processor;

    @Before
    public void setUp() throws Exception {
        contentletAPI = mock(ContentletAPI.class);
        identifierAPI = mock(IdentifierAPI.class);
        contentletIndexAPI = mock(ContentletIndexAPI.class);
        contentletCache = mock(ContentletCache.class);
        userAPI = mock(UserAPI.class);

        final User user = mock(User.class);
        when(user.getUserId()).thenReturn(USER_ID);
        when(userAPI.loadUserById(USER_ID)).thenReturn(user);

        processor = new BulkRefreshContentletsProcessor(contentletAPI, identifierAPI,
                contentletIndexAPI, contentletCache, userAPI);
    }

    /**
     * Method to test: {@link BulkRefreshContentletsProcessor#process(Job)}
     * <p>
     * Given scenario: Three inodes are submitted that are three language versions of the same
     * contentlet identifier.
     * <p>
     * Expected result: The identifier is reindexed once, {@code total} is 1, and the single record
     * lists all three submitted inodes. This is what lets a client mark every selected grid row from
     * one result — reporting per inode instead would either reindex the same content three times or
     * leave two rows with no outcome.
     */
    @Test
    public void test_process_collapsesLanguageVersionsOntoOneIdentifier() throws Exception {
        final String identifier = "ident-A";
        stubIdentifier(identifier, List.of("inode-en", "inode-es", "inode-fr"), 3);

        final Map<String, Object> metadata =
                runAndReadResult(job(List.of("inode-en", "inode-es", "inode-fr"), false, true));

        assertEquals("Three language rows are one identifier", 1, metadata.get("total"));
        assertEquals(1, metadata.get("successCount"));
        assertEquals("All three versions were written", 3, metadata.get("versionsIndexed"));

        assertEquals(1, records(metadata).size());
        final BulkRefreshItemResult result = records(metadata).get(0);
        assertEquals(identifier, result.identifier().orElse(null));
        assertEquals("Every submitted inode must be named",
                Set.of("inode-en", "inode-es", "inode-fr"), Set.copyOf(result.inodes()));
        assertEquals(BulkRefreshItemStatus.SUCCESS, result.status());
    }

    /**
     * Method to test: {@link BulkRefreshContentletsProcessor#process(Job)}
     * <p>
     * Given scenario: A batch of two inodes, one of which no longer resolves — a grid row that went
     * stale between the click and the submit.
     * <p>
     * Expected result: The good identifier is reindexed, the stale inode becomes a FAILED record with
     * no identifier, and the job completes rather than aborting. A stale row must not cost the caller
     * the rest of their selection.
     */
    @Test
    public void test_process_unresolvableInodeFailsThatItemOnly() throws Exception {
        stubIdentifier("ident-A", List.of("inode-good"), 1);
        when(contentletAPI.find(eq("inode-gone"), any(User.class), anyBoolean())).thenReturn(null);

        final Map<String, Object> metadata =
                runAndReadResult(job(List.of("inode-good", "inode-gone"), false, true));

        assertEquals(2, metadata.get("total"));
        assertEquals(1, metadata.get("successCount"));
        assertEquals(1, metadata.get("failedCount"));
        assertEquals(0, metadata.get("skippedCount"));

        final BulkRefreshItemResult failed = records(metadata).stream()
                .filter(r -> r.status() == BulkRefreshItemStatus.FAILED)
                .findFirst().orElseThrow();
        assertTrue("An unresolved inode has no identifier", failed.identifier().isEmpty());
        assertEquals(List.of("inode-gone"), failed.inodes());
        assertTrue("The failure must say what went wrong",
                failed.errorMessage().orElse("").contains("inode-gone"));
    }

    /**
     * Method to test: {@link BulkRefreshContentletsProcessor#process(Job)}
     * <p>
     * Given scenario: Reading the versions of one identifier throws {@link DotSecurityException}.
     * <p>
     * Expected result: That identifier is a FAILED item and the run continues. A permission problem on
     * one identifier is a per-item outcome — treating it as a job failure would discard the results of
     * every item already reindexed.
     */
    @Test
    public void test_process_securityExceptionIsPerItemNotPerJob() throws Exception {
        stubIdentifier("ident-A", List.of("inode-ok"), 1);

        final Contentlet denied = contentlet("inode-denied", "ident-B");
        when(contentletAPI.find(eq("inode-denied"), any(User.class), anyBoolean()))
                .thenReturn(denied);
        final Identifier identB = mock(Identifier.class);
        when(identifierAPI.find("ident-B")).thenReturn(identB);
        when(contentletAPI.findAllVersions(eq(identB), anyBoolean(), any(User.class), anyBoolean()))
                .thenThrow(new DotSecurityException("no access"));

        final Map<String, Object> metadata =
                runAndReadResult(job(List.of("inode-ok", "inode-denied"), false, true));

        assertEquals(1, metadata.get("successCount"));
        assertEquals(1, metadata.get("failedCount"));
        assertEquals("Every item must be accounted for",
                (int) metadata.get("total"), processedSum(metadata));
    }

    /**
     * Method to test: {@link BulkRefreshContentletsProcessor#process(Job)}
     * <p>
     * Given scenario: A contentlet is reindexed.
     * <p>
     * Expected result: Its index policy is WAIT_FOR before the index write, and the contentlet cache
     * entry is cleared first. The default DEFER policy only enqueues into {@code dist_reindex_journal}
     * and returns — that is precisely why the single-item endpoint can report success with nothing
     * reindexed, and the whole point of this endpoint is that "done" means done.
     */
    @Test
    public void test_process_indexesSynchronouslyAndClearsCache() throws Exception {
        stubIdentifier("ident-A", List.of("inode-en"), 1);

        processor.process(job(List.of("inode-en"), false, false));

        verify(contentletCache).remove("ident-A");

        final ArgumentCaptor<Contentlet> captor = ArgumentCaptor.forClass(Contentlet.class);
        verify(contentletIndexAPI).addContentToIndex(captor.capture(), anyBoolean());
        assertEquals("Indexing must be synchronous, not deferred",
                IndexPolicy.WAIT_FOR, captor.getValue().getIndexPolicy());
    }

    /**
     * Method to test: {@link BulkRefreshContentletsProcessor#process(Job)}
     * <p>
     * Given scenario: The job is submitted with {@code includeDependencies} false, then true.
     * <p>
     * Expected result: The flag reaches the index call unchanged. This is a deliberate divergence from
     * the single-item {@code _refresh}, which always includes dependencies; at batch size a
     * {@code loadDeps()} fan-out per item is a different cost profile, so it must be opt-in.
     */
    @Test
    public void test_process_passesIncludeDependenciesThrough() throws Exception {
        stubIdentifier("ident-A", List.of("inode-en"), 1);
        processor.process(job(List.of("inode-en"), false, false));
        verify(contentletIndexAPI).addContentToIndex(any(Contentlet.class), eq(false));

        setUp();
        stubIdentifier("ident-A", List.of("inode-en"), 1);
        processor.process(job(List.of("inode-en"), true, false));
        verify(contentletIndexAPI).addContentToIndex(any(Contentlet.class), eq(true));
    }

    /**
     * Method to test: {@link BulkRefreshContentletsProcessor#cancel(Job)}
     * <p>
     * Given scenario: Cancellation is requested before the run starts, then the run executes.
     * <p>
     * Expected result: Nothing is indexed, every identifier is counted skipped, and the counters still
     * sum to {@code total}. A cancelled run must leave no item reported as pending — a client showing
     * per-row state has to be able to settle every row it was told about.
     */
    @Test
    public void test_cancel_marksRemainingItemsSkippedAndKeepsCountersWhole() throws Exception {
        stubIdentifier("ident-A", List.of("inode-a"), 1);
        stubIdentifier("ident-B", List.of("inode-b"), 1);

        final Job job = job(List.of("inode-a", "inode-b"), false, true);
        processor.cancel(job);
        final Map<String, Object> metadata = runAndReadResult(job);

        verify(contentletIndexAPI, never()).addContentToIndex(any(Contentlet.class), anyBoolean());
        assertEquals(2, metadata.get("skippedCount"));
        assertEquals((int) metadata.get("total"), processedSum(metadata));
        assertNotNull("A skip must explain itself", metadata.get("skipReason"));
        assertTrue("Skipped items are recorded, not omitted", records(metadata).stream()
                .allMatch(r -> r.status() == BulkRefreshItemStatus.SKIPPED));
    }

    /**
     * Method to test: {@link BulkRefreshContentletsProcessor#getResultMetadata(Job)}
     * <p>
     * Given scenario: The job ran with {@code includeItemResults} false.
     * <p>
     * Expected result: Counters are present, the per-item array is absent. Counters are what a
     * progress bar and a tally need; the breakdown is only for a drill-down, and it is persisted with
     * the job, so recording it unasked would store a 500-entry array nobody reads.
     */
    @Test
    public void test_getResultMetadata_omitsItemResultsWhenNotRequested() throws Exception {
        stubIdentifier("ident-A", List.of("inode-a"), 2);

        final Map<String, Object> metadata =
                runAndReadResult(job(List.of("inode-a"), false, false));

        assertNotNull(metadata);
        assertEquals(1, metadata.get("total"));
        assertEquals(1, metadata.get("successCount"));
        assertEquals(0, metadata.get("failedCount"));
        assertEquals(0, metadata.get("skippedCount"));
        assertEquals(2, metadata.get("versionsIndexed"));
        assertEquals(false, metadata.get("includeDependencies"));
        assertFalse("results must be absent when not requested", metadata.containsKey("results"));
    }

    /**
     * Method to test: {@link BulkRefreshContentletsProcessor#getResultMetadata(Job)}
     * <p>
     * Given scenario: The job ran with {@code includeItemResults} true.
     * <p>
     * Expected result: The persisted metadata carries one record per identifier. The terminal SSE event
     * is built from this, not from the live processor, so it has to be complete here.
     */
    @Test
    public void test_getResultMetadata_carriesEveryRecordWhenRequested() throws Exception {
        stubIdentifier("ident-A", List.of("inode-a"), 1);
        stubIdentifier("ident-B", List.of("inode-b"), 1);

        final Map<String, Object> metadata =
                runAndReadResult(job(List.of("inode-a", "inode-b"), false, true));

        final List<BulkRefreshItemResult> results = records(metadata);
        assertNotNull("results must be present when requested", results);
        assertEquals("One record per identifier", 2, results.size());
        assertEquals("Records follow submission order, so a client can settle rows as it sent them",
                List.of("ident-A", "ident-B"), results.stream()
                        .map(r -> r.identifier().orElseThrow()).collect(Collectors.toList()));
    }

    /**
     * Method to test: {@link BulkRefreshContentletsProcessor#process(Job)}
     * <p>
     * Given scenario: Two identifiers are reindexed.
     * <p>
     * Expected result: Progress is reported as work completes and reaches 1.0. Without this the client
     * has a job id and no way to show anything moving.
     */
    @Test
    public void test_process_reportsProgressToCompletion() throws Exception {
        stubIdentifier("ident-A", List.of("inode-a"), 1);
        stubIdentifier("ident-B", List.of("inode-b"), 1);

        final DefaultProgressTracker tracker = new DefaultProgressTracker();
        processor.process(job(List.of("inode-a", "inode-b"), false, false, tracker));

        assertEquals(1.0f, tracker.progress(), 0.001f);
    }

    /**
     * Method to test: {@link BulkRefreshContentletsProcessor#validate(Map)}
     * <p>
     * Given scenario: The parameters carry an empty inode list.
     * <p>
     * Expected result: Rejected before the job is created, so no job id is handed out for work that
     * can never happen.
     */
    @Test(expected = JobValidationException.class)
    public void test_validate_rejectsEmptySelection() {
        processor.validate(Map.of(PARAM_CONTENTLET_IDS, List.of(), PARAM_USER_ID, USER_ID));
    }

    /**
     * Method to test: {@link BulkRefreshContentletsProcessor#validate(Map)}
     * <p>
     * Given scenario: The parameters carry no inode list at all.
     * <p>
     * Expected result: Rejected, same as an empty list.
     */
    @Test(expected = JobValidationException.class)
    public void test_validate_rejectsMissingSelection() {
        processor.validate(Map.of(PARAM_USER_ID, USER_ID));
    }

    /**
     * Method to test: {@link BulkRefreshContentletsProcessor#validate(Map)}
     * <p>
     * Given scenario: A well-formed set of parameters.
     * <p>
     * Expected result: Accepted without throwing.
     */
    @Test
    public void test_validate_acceptsAWellFormedSelection() {
        processor.validate(Map.of(
                PARAM_CONTENTLET_IDS, List.of("inode-a"),
                PARAM_USER_ID, USER_ID));
    }

    /**
     * Runs the job and returns what the framework will persist as its result.
     * <p>
     * The counters and records are read back the same way production reads them — through
     * {@link BulkRefreshContentletsProcessor#getResultMetadata(Job)} — rather than through a
     * test-only accessor, so these assertions cover the path a client actually sees.
     */
    private Map<String, Object> runAndReadResult(final Job job) {
        processor.process(job);
        return processor.getResultMetadata(job);
    }

    @SuppressWarnings("unchecked")
    private static List<BulkRefreshItemResult> records(final Map<String, Object> metadata) {
        return (List<BulkRefreshItemResult>) metadata.get("results");
    }

    private static int processedSum(final Map<String, Object> metadata) {
        return (int) metadata.get("successCount")
                + (int) metadata.get("failedCount")
                + (int) metadata.get("skippedCount");
    }

    /**
     * Stubs the inode → contentlet → identifier → versions chain for one identifier.
     *
     * @param identifier   the identifier every given inode resolves to
     * @param inodes       the submitted inodes that resolve to it
     * @param versionCount how many versions {@code findAllVersions} returns
     */
    private void stubIdentifier(final String identifier, final List<String> inodes,
            final int versionCount) throws Exception {
        for (final String inode : inodes) {
            when(contentletAPI.find(eq(inode), any(User.class), anyBoolean()))
                    .thenReturn(contentlet(inode, identifier));
        }
        final Identifier id = mock(Identifier.class);
        when(id.getId()).thenReturn(identifier);
        when(identifierAPI.find(identifier)).thenReturn(id);

        final List<Contentlet> versions = new ArrayList<>();
        for (int i = 0; i < versionCount; i++) {
            versions.add(contentlet(identifier + "-v" + i, identifier));
        }
        when(contentletAPI.findAllVersions(eq(id), anyBoolean(), any(User.class), anyBoolean()))
                .thenReturn(versions);
    }

    private static Contentlet contentlet(final String inode, final String identifier) {
        final Contentlet contentlet = new Contentlet();
        contentlet.setInode(inode);
        contentlet.setIdentifier(identifier);
        return contentlet;
    }

    private static Job job(final List<String> inodes, final boolean includeDependencies,
            final boolean includeItemResults) {
        return job(inodes, includeDependencies, includeItemResults, new DefaultProgressTracker());
    }

    private static Job job(final List<String> inodes, final boolean includeDependencies,
            final boolean includeItemResults, final DefaultProgressTracker tracker) {
        final Map<String, Object> parameters = new HashMap<>();
        parameters.put(PARAM_CONTENTLET_IDS, inodes);
        parameters.put(PARAM_INCLUDE_DEPENDENCIES, includeDependencies);
        parameters.put(PARAM_INCLUDE_ITEM_RESULTS, includeItemResults);
        parameters.put(PARAM_USER_ID, USER_ID);

        return Job.builder()
                .id("job-1")
                .queueName("bulkRefreshContentlets")
                .state(JobState.RUNNING)
                .parameters(parameters)
                .progressTracker(tracker)
                .build();
    }
}
