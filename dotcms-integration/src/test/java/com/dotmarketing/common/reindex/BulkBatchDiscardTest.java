package com.dotmarketing.common.reindex;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.cdi.CDIUtils;
import com.dotcms.content.elasticsearch.business.ContentletIndexAPI;
import com.dotcms.content.elasticsearch.business.OrderedMappingIndexAPI;
import com.dotcms.content.index.IndexConfigHelper;
import com.dotcms.content.index.VersionedIndices;
import com.dotcms.content.index.domain.IndexBulkProcessor;
import com.dotcms.content.index.opensearch.OSClientProvider;
import com.dotcms.contenttype.model.field.TextAreaField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for #37269: one document that cannot be serialized must not fail its whole
 * reindex batch.
 *
 * <h2>What is at stake</h2>
 * <p>The reindex thread claims a group of journal entries, maps each one to index documents and
 * sends them to the engine in bulk requests. Today, when one document is larger than Jackson's
 * read limit (20,000,000 characters per string), the Elasticsearch client fails while building
 * the request, nothing is sent, and {@link BulkProcessorListener#afterBulk(long, Throwable)}
 * marks <b>every</b> entry of the group failed with that one document's message. On a real
 * install, 10 oversized documents left 380 contentlets out of the index.</p>
 *
 * <p>These tests drive one reindex iteration exactly the way {@code ReindexThread} does — a fresh
 * {@link BulkProcessorListener} holding the claimed entries, a processor from
 * {@link ContentletIndexAPI#createBulkProcessor}, and {@link ContentletIndexAPI#appendToBulkProcessor}
 * — and assert on {@code dist_reindex_journal} and on the index the ambient phase reads from.</p>
 *
 * <p>The tests never change the migration phase: the weekly Phase Sweep runs this class at
 * Phases 0, 1 and 3 (forcing a phase mid-JVM breaks Phase 3 environments, see #37432).</p>
 */
public class BulkBatchDiscardTest {

    /** One character over Jackson's default maximum string length. */
    private static final int OVERSIZED_LENGTH = 20_000_001;

    /** The parser buffer size the old message reported instead of the document's length. */
    private static final String PARSER_BUFFER_SIZE = "20054016";

    private static final int HEALTHY_COUNT = 20;

    private static final Pattern FIELD_AND_LENGTH =
            Pattern.compile("field '[^']+' is [0-9,]+ chars");

    private static User systemUser;
    private static ContentType contentType;
    private static ReindexQueueAPI reindexQueueAPI;
    private static ContentletIndexAPI indexAPI;

    /**
     * Initialises the environment and stops the background reindex thread from claiming the
     * journal entries these tests create (and {@code markAsFailed} from unpausing it).
     */
    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        systemUser = APILocator.systemUser();
        reindexQueueAPI = APILocator.getReindexQueueAPI();
        indexAPI = APILocator.getContentletIndexAPI();
        Config.setProperty("ALLOW_MANUAL_REINDEX_UNPAUSE", true);
        ReindexThread.pause();
        contentType = new ContentTypeDataGen()
                .field(new FieldDataGen().type(TextAreaField.class).velocityVarName("body").next())
                .nextPersisted();
    }

    /** Restores the reindex thread and clears anything left in the journal. */
    @AfterClass
    public static void cleanUp() throws Exception {
        reindexQueueAPI.deleteReindexAndFailedRecords();
        Config.setProperty("ALLOW_MANUAL_REINDEX_UNPAUSE", false);
        ReindexThread.unpause();
    }

    /**
     * Given Scenario: One contentlet whose {@code body} exceeds Jackson's string limit is claimed
     * in the same reindex iteration as {@value #HEALTHY_COUNT} healthy contentlets.
     * Expected Result: Every healthy contentlet is in the index the ambient phase reads from; the
     * journal keeps exactly one row, for the oversized contentlet; its reason names the inode,
     * the field and the real length, and not the parser buffer size (AC-001, AC-002, AC-003).
     */
    @Test
    public void test_oversizedDocument_failsAlone_restOfGroupIndexed() throws Exception {
        final Contentlet oversized = newContentlet("x".repeat(OVERSIZED_LENGTH));
        final List<Contentlet> healthyContent = new ArrayList<>();
        for (int i = 0; i < HEALTHY_COUNT; i++) {
            healthyContent.add(newContentlet("healthy body " + i));
        }
        final List<String> healthy = healthyContent.stream().map(Contentlet::getIdentifier)
                .collect(Collectors.toList());
        final List<String> all = new ArrayList<>(healthy);
        all.add(oversized.getIdentifier());

        runOneReindexIteration(claimEntries(all));

        final Map<String, String> remaining = journalReasons(all);
        assertEquals("only the oversized contentlet may stay in the journal: " + remaining.keySet(),
                Set.of(oversized.getIdentifier()), remaining.keySet());

        final String reason = remaining.get(oversized.getIdentifier());
        assertTrue("reason must name the oversized inode: " + reason,
                reason.contains(oversized.getInode()));
        assertTrue("reason must name the field and its real length: " + reason,
                FIELD_AND_LENGTH.matcher(reason).find());
        assertFalse("reason must not report the parser buffer size: " + reason,
                reason.contains(PARSER_BUFFER_SIZE));

        for (final String identifier : healthy) {
            assertIndexed(identifier);
        }
        assertInShadowWhenDualWrite(healthyContent);
    }

    /**
     * Given Scenario: An identifier whose live version is healthy and whose working version
     * exceeds Jackson's string limit is reindexed, with the rejected document handed to the
     * processor <b>before</b> the healthy one.
     * Expected Result: The live version is in the live index, the oversized working version is in
     * no index, and the journal row survives with the violation reason (AC-005).
     */
    @Test
    public void test_healthyLive_oversizedWorking_liveIndexed_failureKept_rejectedFirst()
            throws Exception {
        healthyLiveOversizedWorking(true);
    }

    /**
     * Same as {@link #test_healthyLive_oversizedWorking_liveIndexed_failureKept_rejectedFirst()}
     * with the rejected document handed to the processor <b>after</b> the healthy one.
     */
    @Test
    public void test_healthyLive_oversizedWorking_liveIndexed_failureKept_rejectedLast()
            throws Exception {
        healthyLiveOversizedWorking(false);
    }

    /**
     * Given Scenario: An identifier with two languages, the second one exceeding Jackson's string
     * limit, is reindexed with the rejected document handed to the processor <b>before</b> the
     * healthy one.
     * Expected Result: The default-language document is indexed, the oversized translation is in
     * no index, and the journal row survives with the violation reason (AC-005).
     */
    @Test
    public void test_twoLanguages_oneOversized_otherIndexed_failureKept_rejectedFirst()
            throws Exception {
        twoLanguagesOneOversized(true);
    }

    /**
     * Same as {@link #test_twoLanguages_oneOversized_otherIndexed_failureKept_rejectedFirst()}
     * with the rejected document handed to the processor <b>after</b> the healthy one.
     */
    @Test
    public void test_twoLanguages_oneOversized_otherIndexed_failureKept_rejectedLast()
            throws Exception {
        twoLanguagesOneOversized(false);
    }

    /**
     * Publishes a healthy version, saves an oversized working version on top of it, runs one
     * iteration in the given order and checks the outcome.
     */
    private static void healthyLiveOversizedWorking(final boolean rejectedFirst) throws Exception {
        final Contentlet live = ContentletDataGen.publish(newContentlet("healthy live body"));
        final Contentlet working = ContentletDataGen.checkout(live);
        working.setProperty("body", "x".repeat(OVERSIZED_LENGTH));
        final Contentlet oversized = ContentletDataGen.checkin(working, IndexPolicy.DEFER);
        final String identifier = live.getIdentifier();
        final String liveQuery =
                "+identifier_dotraw:" + identifier + " +live:true +inode:" + live.getInode();
        // Publishing indexed the healthy version already; take it out so that finding it after
        // the iteration proves the iteration put it back.
        indexAPI.removeContentFromLiveIndex(live);
        awaitAbsent(liveQuery, "the healthy live version must be out of the index before the run");

        runOneReindexIteration(new OrderedMappingIndexAPI(rejectedFirst),
                claimEntries(List.of(identifier)));

        assertFailureKept(identifier, oversized);
        assertFound(liveQuery,
                "the healthy live version must be in the live index: " + identifier);
        assertAbsent("+inode:" + oversized.getInode(),
                "the oversized working version must not be indexed: " + oversized.getInode());
    }

    /**
     * Saves a healthy default-language version and an oversized translation of the same
     * identifier, runs one iteration in the given order and checks the outcome.
     */
    private static void twoLanguagesOneOversized(final boolean rejectedFirst) throws Exception {
        final Language secondLanguage = new LanguageDataGen().nextPersisted();
        final Contentlet healthy = newContentlet("healthy default-language body");
        final Contentlet translation = ContentletDataGen.checkout(healthy);
        translation.setLanguageId(secondLanguage.getId());
        translation.setProperty("body", "x".repeat(OVERSIZED_LENGTH));
        final Contentlet oversized = ContentletDataGen.checkin(translation, IndexPolicy.DEFER);
        final String identifier = healthy.getIdentifier();
        assertEquals("the translation must share the identifier", identifier,
                oversized.getIdentifier());

        runOneReindexIteration(new OrderedMappingIndexAPI(rejectedFirst),
                claimEntries(List.of(identifier)));

        assertFailureKept(identifier, oversized);
        assertFound("+identifier_dotraw:" + identifier + " +languageId:" + healthy.getLanguageId(),
                "the healthy default-language document must be indexed: " + identifier);
        assertAbsent("+inode:" + oversized.getInode(),
                "the oversized translation must not be indexed: " + oversized.getInode());
    }

    // -------------------------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------------------------

    /** Creates a contentlet of the test type without indexing it on save. */
    private static Contentlet newContentlet(final String body) {
        return new ContentletDataGen(contentType)
                .setProperty("body", body)
                .setPolicy(IndexPolicy.DEFER)
                .nextPersisted();
    }

    /**
     * Queues the identifiers and claims their journal entries. The queue serves claims from an
     * in-memory buffer that may hold stale entries from earlier tests, so this polls until the
     * fresh rows come back.
     */
    private static Map<String, ReindexEntry> claimEntries(final List<String> identifiers)
            throws Exception {
        reindexQueueAPI.deleteReindexAndFailedRecords();
        for (final String identifier : identifiers) {
            reindexQueueAPI.addIdentifierReindex(identifier);
        }
        final Set<String> wanted = new HashSet<>(identifiers);
        final Map<String, ReindexEntry> entries = new HashMap<>();
        for (int i = 0; i < 20 && !entries.keySet().containsAll(wanted); i++) {
            reindexQueueAPI.findContentToReindex(identifiers.size() + 50).forEach((id, entry) -> {
                if (wanted.contains(id)) {
                    entries.put(id, entry);
                }
            });
        }
        assertEquals("every entry must be claimed from the queue", wanted, entries.keySet());
        return entries;
    }

    /** Runs one iteration the way {@code ReindexThread.runReindexLoop} does. */
    private static void runOneReindexIteration(final Map<String, ReindexEntry> entries)
            throws Exception {
        runOneReindexIteration(indexAPI, entries);
    }

    /** Runs one iteration through the given index API, e.g. one that pins the document order. */
    private static void runOneReindexIteration(final ContentletIndexAPI api,
            final Map<String, ReindexEntry> entries) throws Exception {
        final BulkProcessorListener listener = new BulkProcessorListener();
        listener.workingRecords.putAll(entries);
        try (final IndexBulkProcessor processor = api.createBulkProcessor(listener)) {
            api.appendToBulkProcessor(processor, entries.values());
        } // close() flushes and waits for afterBulk
    }

    /**
     * Asserts the identifier's journal row is still there and names the rejected version: its
     * inode, the field and the real length.
     */
    private static void assertFailureKept(final String identifier, final Contentlet rejected)
            throws Exception {
        final Map<String, String> remaining = journalReasons(List.of(identifier));
        assertEquals("the journal row must survive a sibling's success",
                Set.of(identifier), remaining.keySet());
        final String reason = remaining.get(identifier);
        assertTrue("reason must name the rejected inode: " + reason,
                reason.contains(rejected.getInode()));
        assertTrue("reason must name the field and its real length: " + reason,
                FIELD_AND_LENGTH.matcher(reason).find());
    }

    /** Journal rows left for the given identifiers, keyed by identifier, valued by reason. */
    private static Map<String, String> journalReasons(final List<String> identifiers)
            throws Exception {
        final String placeholders = identifiers.stream().map(id -> "?")
                .collect(Collectors.joining(","));
        final DotConnect dc = new DotConnect().setSQL(
                "select ident_to_index, index_val from dist_reindex_journal where ident_to_index in ("
                        + placeholders + ")");
        identifiers.forEach(dc::addParam);
        final Map<String, String> reasons = new HashMap<>();
        for (final Map<String, Object> row : dc.loadObjectResults()) {
            reasons.put((String) row.get("ident_to_index"), String.valueOf(row.get("index_val")));
        }
        return reasons;
    }

    /**
     * Waits for the index to refresh and asserts the identifier is searchable, querying the
     * keyword field {@code identifier_dotraw} for an exact match.
     */
    private static void assertIndexed(final String identifier) throws Exception {
        assertFound("+identifier_dotraw:" + identifier,
                "healthy contentlet must be indexed: " + identifier);
    }

    /**
     * In a dual-write phase, asserts each contentlet's working document also reached the
     * OpenSearch working index (AC-004). Phase-aware searches read only one engine, so this asks
     * OpenSearch directly by document id, which is real-time and needs no refresh. Outside
     * dual-write phases it does nothing.
     */
    private static void assertInShadowWhenDualWrite(final List<Contentlet> contentlets)
            throws Exception {
        if (!IndexConfigHelper.isDualWrite()) {
            return;
        }
        final String osWorking = APILocator.getVersionedIndicesAPI().loadDefaultVersionedIndices()
                .flatMap(VersionedIndices::working)
                .orElseThrow(() -> new AssertionError("no OpenSearch working index in a dual-write phase"));
        final OpenSearchClient client = CDIUtils.getBeanThrows(OSClientProvider.class).getClient();
        for (final Contentlet contentlet : contentlets) {
            final String documentId = contentlet.getIdentifier() + "_" + contentlet.getLanguageId()
                    + "_" + contentlet.getVariantId();
            assertTrue("healthy contentlet must also be in OpenSearch: " + documentId,
                    client.exists(e -> e.index(osWorking).id(documentId)).value());
        }
    }

    /** Waits for the index to refresh and asserts the query matches at least one document. */
    private static void assertFound(final String query, final String message) throws Exception {
        long count = 0;
        for (int i = 0; i < 20 && count == 0; i++) {
            count = indexCount(query);
            if (count == 0) {
                Thread.sleep(500);
            }
        }
        assertTrue(message, count > 0);
    }

    /** Waits for the index to refresh and asserts the query no longer matches anything. */
    private static void awaitAbsent(final String query, final String message) throws Exception {
        long count = indexCount(query);
        for (int i = 0; i < 20 && count > 0; i++) {
            Thread.sleep(500);
            count = indexCount(query);
        }
        assertEquals(message, 0L, count);
    }

    /**
     * Asserts the query matches nothing. Call it after {@link #assertFound} on the same
     * iteration, so the index has already refreshed and a zero is not just a late refresh.
     */
    private static void assertAbsent(final String query, final String message) throws Exception {
        assertEquals(message, 0L, indexCount(query));
    }

    /** Counts matches with the query caches cleared, so a stale zero is never served. */
    private static long indexCount(final String query) throws Exception {
        // indexCount results are cached per query: a 0 read before the index refreshed would
        // otherwise be served for every later attempt.
        CacheLocator.getESQueryCache().clearCache();
        CacheLocator.getOSQueryCache().clearCache();
        return APILocator.getContentletAPI().indexCount(query, systemUser, false);
    }
}
