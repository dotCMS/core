package com.dotcms.content.index.opensearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.dotcms.UnitTestBase;
import com.dotcms.content.index.domain.InvalidSearchQueryException;
import com.dotcms.content.index.domain.QueryRejectedByOpenSearchException;
import com.dotmarketing.business.DotStateException;
import org.junit.Test;

/**
 * Unit tests for how {@link OSSearchAPIImpl} classifies a search the caller got wrong: it must be
 * raised as {@link InvalidSearchQueryException}, so the Phase 2 router propagates it instead of
 * retrying it against Elasticsearch and logging it as an index outage (issue #37637).
 */
public class OSSearchAPIImplInvalidQueryTest extends UnitTestBase {

    private final OSSearchAPIImpl api = new OSSearchAPIImpl(mock(OSClientProvider.class));

    /** A query that is not JSON never reaches the cluster and is reported as the caller's error. */
    @Test
    public void searchRaw_notJson_isAnInvalidQuery() {
        final InvalidSearchQueryException thrown = assertThrows(InvalidSearchQueryException.class,
                () -> api.searchRaw("this is not json", false, null, true));
        assertEquals("Unable to parse the given query.", thrown.getMessage());
    }

    /** A missing query is the caller's error too. */
    @Test
    public void searchRaw_nullQuery_isAnInvalidQuery() {
        assertThrows(InvalidSearchQueryException.class,
                () -> api.searchRaw(null, false, null, true));
    }

    /**
     * HTTP 400 with a parse failure: OpenSearch refused the request. Not the caller's error in the
     * no-fallback sense — Elasticsearch 7 may accept syntax OpenSearch 3 dropped — so it is raised
     * as a rejection, which Phase 2 still retries on Elasticsearch.
     */
    @Test
    public void httpFailure_400_parsingException_isARejection() {
        final DotStateException failure = OSSearchAPIImpl.searchFailure(400,
                "{\"error\":{\"root_cause\":[{\"type\":\"parsing_exception\","
                        + "\"reason\":\"unknown query [bogus_clause]\"}],"
                        + "\"type\":\"parsing_exception\"},\"status\":400}");

        assertTrue(failure instanceof QueryRejectedByOpenSearchException);
        assertFalse(failure instanceof InvalidSearchQueryException);
        assertTrue(failure.getMessage(), failure.getMessage().contains("parsing_exception"));
    }

    /** A body OpenSearch could not read as query content is a rejection too. */
    @Test
    public void httpFailure_400_contentParseException_isARejection() {
        assertTrue(OSSearchAPIImpl.searchFailure(400,
                "{\"error\":{\"root_cause\":[{\"type\":\"x_content_parse_exception\"}],"
                        + "\"type\":\"x_content_parse_exception\"},\"status\":400}")
                instanceof QueryRejectedByOpenSearchException);
    }

    /**
     * A well-formed query can still draw a 400 from a shard whose mapping differs — during Phase 2
     * an OpenSearch index still catching up with Elasticsearch. That is the stale-index case the
     * fallback exists for, so it must stay eligible for it.
     */
    @Test
    public void httpFailure_400_queryShardException_isNotARejection() {
        assertFalse(OSSearchAPIImpl.searchFailure(400,
                "{\"error\":{\"root_cause\":[{\"type\":\"query_shard_exception\","
                        + "\"reason\":\"failed to create query\"}],"
                        + "\"type\":\"search_phase_execution_exception\"},\"status\":400}")
                instanceof QueryRejectedByOpenSearchException);
    }

    /** A shard-level illegal argument (e.g. sorting on a text field) is a mapping question too. */
    @Test
    public void httpFailure_400_shardIllegalArgument_isNotARejection() {
        assertFalse(OSSearchAPIImpl.searchFailure(400,
                "{\"error\":{\"root_cause\":[{\"type\":\"illegal_argument_exception\"}],"
                        + "\"type\":\"search_phase_execution_exception\"},\"status\":400}")
                instanceof QueryRejectedByOpenSearchException);
    }

    /** A 400 whose body cannot be read gives no reason to skip the fallback. */
    @Test
    public void httpFailure_400_unreadableBody_isNotARejection() {
        assertFalse(OSSearchAPIImpl.searchFailure(400, "<html>Bad Request</html>")
                instanceof QueryRejectedByOpenSearchException);
    }

    /**
     * Anything else — a missing index, an auth failure, an overloaded node — is not the caller's
     * error, and must stay eligible for the Phase 2 fallback.
     */
    @Test
    public void httpFailure_otherStatuses_areNotInvalidQueries() {
        for (final int status : new int[]{401, 403, 404, 429, 500, 503}) {
            final DotStateException failure = OSSearchAPIImpl.searchFailure(status, "{}");
            assertFalse("HTTP " + status + " must stay a plain fallback condition",
                    failure instanceof InvalidSearchQueryException
                            || failure instanceof QueryRejectedByOpenSearchException);
        }
    }
}
