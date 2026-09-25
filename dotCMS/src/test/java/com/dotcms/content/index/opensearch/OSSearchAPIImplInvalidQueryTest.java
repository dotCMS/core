package com.dotcms.content.index.opensearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.dotcms.UnitTestBase;
import com.dotcms.content.index.domain.InvalidSearchQueryException;
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

    /** HTTP 400 means OpenSearch parsed the request and rejected it: the caller's error. */
    @Test
    public void httpFailure_400_isAnInvalidQuery() {
        final DotStateException failure = OSSearchAPIImpl.searchFailure(400,
                "{\"error\":{\"type\":\"parsing_exception\"}}");

        assertTrue(failure instanceof InvalidSearchQueryException);
        assertTrue(failure.getMessage(), failure.getMessage().contains("parsing_exception"));
    }

    /**
     * Anything else — a missing index, an auth failure, an overloaded node — is not the caller's
     * error, and must stay eligible for the Phase 2 fallback.
     */
    @Test
    public void httpFailure_otherStatuses_areNotInvalidQueries() {
        for (final int status : new int[]{401, 403, 404, 429, 500, 503}) {
            final DotStateException failure = OSSearchAPIImpl.searchFailure(status, "{}");
            assertFalse("HTTP " + status + " must stay a fallback condition",
                    failure instanceof InvalidSearchQueryException);
        }
    }
}
