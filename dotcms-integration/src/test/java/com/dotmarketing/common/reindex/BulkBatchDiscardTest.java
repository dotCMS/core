package com.dotmarketing.common.reindex;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.content.elasticsearch.business.ContentletIndexAPI;
import com.dotcms.content.index.domain.IndexBulkProcessor;
import com.dotcms.contenttype.model.field.TextAreaField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
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
        final List<String> healthy = new ArrayList<>();
        for (int i = 0; i < HEALTHY_COUNT; i++) {
            healthy.add(newContentlet("healthy body " + i).getIdentifier());
        }
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
        final BulkProcessorListener listener = new BulkProcessorListener();
        listener.workingRecords.putAll(entries);
        try (final IndexBulkProcessor processor = indexAPI.createBulkProcessor(listener)) {
            indexAPI.appendToBulkProcessor(processor, entries.values());
        } // close() flushes and waits for afterBulk
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
        final String query = "+identifier_dotraw:" + identifier;
        long count = 0;
        for (int i = 0; i < 20 && count == 0; i++) {
            // indexCount results are cached per query: a 0 read before the index refreshed would
            // otherwise be served for every later attempt.
            CacheLocator.getESQueryCache().clearCache();
            CacheLocator.getOSQueryCache().clearCache();
            count = APILocator.getContentletAPI().indexCount(query, systemUser, false);
            if (count == 0) {
                Thread.sleep(500);
            }
        }
        assertTrue("healthy contentlet must be indexed: " + identifier, count > 0);
    }
}
