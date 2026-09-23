package com.dotcms.content.index.migration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.UnitTestBase;
import com.dotcms.content.index.migration.MirrorStatus.Verdict;
import com.dotcms.content.index.migration.SiteSearchMirrorReconciler.SiteSearchMirrors;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.sitesearch.business.SiteSearchAPI;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for the alias half of {@link SiteSearchMirrorReconciler} — how the migration-readiness
 * report identifies a site-search index (issue #36983). Both engine leaves are mocked, so no live
 * cluster is needed.
 *
 * <p>Operators know a site-search index by its alias, never by its {@code sitesearch_<timestamp>_<uuid>}
 * name, so the report carries the alias each engine has attached — per engine, because during the
 * migration the two sides can legitimately disagree.</p>
 */
public class SiteSearchMirrorReconcilerTest extends UnitTestBase {

    private static final String PREFIX = "cluster_x.";
    private static final String INDEX = "sitesearch_20260810160529";

    private SiteSearchAPI es;
    private SiteSearchAPI os;

    @Before
    public void setUp() {
        es = mock(SiteSearchAPI.class);
        os = mock(SiteSearchAPI.class);
        when(es.listIndices()).thenReturn(List.of(INDEX));
        when(os.listIndices()).thenReturn(List.of(INDEX));
        when(es.existsOnAllWriteEngines(anyString())).thenReturn(true);
        when(os.existsOnAllWriteEngines(anyString())).thenReturn(true);
        when(es.documentCount(anyString())).thenReturn(10L);
        when(os.documentCount(anyString())).thenReturn(10L);
        when(es.getAliasToIndexMap()).thenReturn(Map.of());
        when(os.getAliasToIndexMap()).thenReturn(Map.of());
    }

    private SiteSearchMirrorReconciler reconciler() {
        return new SiteSearchMirrorReconciler(es, os, () -> PREFIX);
    }

    private MirrorStatus onlyStatus() {
        final List<MirrorStatus> statuses = reconciler().statuses();
        assertEquals(1, statuses.size());
        return statuses.get(0);
    }

    /** The alias each engine holds is reported on that engine's copy. */
    @Test
    public void alias_isReportedPerEngine() {
        when(es.getAliasToIndexMap()).thenReturn(Map.of("sitesearch-ph-3", INDEX));
        when(os.getAliasToIndexMap()).thenReturn(Map.of("sitesearch-ph-3", INDEX));

        final MirrorStatus status = onlyStatus();

        assertEquals("sitesearch-ph-3", status.es().alias());
        assertEquals("sitesearch-ph-3", status.os().alias());
        assertEquals(Verdict.IN_SYNC, status.verdict());
    }

    /**
     * An alias present on one engine and absent on the other is exactly the asymmetry an operator
     * needs to see before promoting a phase, so it must survive as two distinct values.
     */
    @Test
    public void alias_missingOnOneEngine_staysVisibleOnTheOther() {
        when(es.getAliasToIndexMap()).thenReturn(Map.of("sitesearch-ph-3", INDEX));
        when(os.getAliasToIndexMap()).thenReturn(Map.of());

        final MirrorStatus status = onlyStatus();

        assertEquals("sitesearch-ph-3", status.es().alias());
        assertNull(status.os().alias());
    }

    /** An index with no alias anywhere reports none — the field is simply absent from the payload. */
    @Test
    public void alias_absentOnBothEngines_isNull() {
        final MirrorStatus status = onlyStatus();

        assertNull(status.es().alias());
        assertNull(status.os().alias());
        assertFalse(status.recommendation().contains("NOTE"));
    }

    /**
     * An alias that is really an index name is the fingerprint of the overwrite fixed in issue #36983.
     * The fix cannot restore an alias already lost, so the report must call it out — while leaving the
     * sync verdict alone, since no data is at risk.
     */
    @Test
    public void aliasShapedLikeAnIndexName_isFlaggedWithoutChangingTheVerdict() {
        final String corrupted = "sitesearch_20260806203309";
        when(es.getAliasToIndexMap()).thenReturn(Map.of(corrupted, INDEX));
        when(os.getAliasToIndexMap()).thenReturn(Map.of(corrupted, INDEX));

        final MirrorStatus status = onlyStatus();

        assertEquals(corrupted, status.es().alias());
        assertTrue(status.recommendation().contains("is an index name, not a real alias"));
        assertTrue(status.recommendation().contains(corrupted));
        assertEquals("A damaged alias must not affect the data-integrity verdict", Verdict.IN_SYNC,
                status.verdict());
        assertFalse(status.needsAttention());
    }

    /** A real alias that merely starts with the site-search prefix is NOT mistaken for an index name. */
    @Test
    public void aliasStartingWithThePrefix_isNotFlagged() {
        when(es.getAliasToIndexMap()).thenReturn(Map.of("sitesearch-ph-3", INDEX));
        when(os.getAliasToIndexMap()).thenReturn(Map.of("sitesearch_prod", INDEX));

        assertFalse(onlyStatus().recommendation().contains("NOTE"));
    }

    /** The alias lookup is one call per engine for the whole set, not one per index. */
    @Test
    public void aliasLookup_runsOncePerEngine() {
        when(es.listIndices()).thenReturn(List.of(INDEX, "sitesearch_20260811090000"));
        when(os.listIndices()).thenReturn(List.of(INDEX, "sitesearch_20260811090000"));

        assertEquals(2, reconciler().statuses().size());

        verify(es, times(1)).getAliasToIndexMap();
        verify(os, times(1)).getAliasToIndexMap();
    }

    /**
     * Elasticsearch cannot be reached: the index list comes from OpenSearch alone, and the
     * Elasticsearch side of each row is marked unavailable rather than missing — "missing" would tell
     * the operator to re-crawl an index that may be perfectly fine (issue #37636). Nothing else is
     * asked of the engine already known to be down.
     */
    @Test
    public void elasticsearchUnreachable_reportsOpenSearchIndices_andMarksElasticsearchUnavailable() {
        when(es.listIndices()).thenThrow(new DotRuntimeException("elasticsearch: Name or service not known"));

        final SiteSearchMirrors mirrors = reconciler().mirrors();

        assertEquals(List.of(MirrorStatus.ELASTICSEARCH),
                List.copyOf(mirrors.unreachableEngines().keySet()));
        assertEquals(1, mirrors.statuses().size());
        final MirrorStatus status = mirrors.statuses().get(0);
        assertEquals(INDEX, status.indexName());
        assertEquals(Verdict.UNMEASURED, status.verdict());
        assertFalse(status.es().wasRead());
        assertEquals(-1, status.es().docCount());
        assertEquals(10, status.os().docCount());
        assertFalse(status.recommendation().contains("is missing"));
        verify(es, never()).documentCount(anyString());
        verify(es, never()).getAliasToIndexMap();
    }

    /** The same, the other way round. */
    @Test
    public void openSearchUnreachable_reportsElasticsearchIndices_andMarksOpenSearchUnavailable() {
        when(os.listIndices()).thenThrow(new DotRuntimeException("Connection refused"));

        final SiteSearchMirrors mirrors = reconciler().mirrors();

        assertEquals(List.of(MirrorStatus.OPENSEARCH),
                List.copyOf(mirrors.unreachableEngines().keySet()));
        final MirrorStatus status = mirrors.statuses().get(0);
        assertEquals(Verdict.UNMEASURED, status.verdict());
        assertEquals(10, status.es().docCount());
        assertFalse(status.os().wasRead());
        verify(os, never()).documentCount(anyString());
    }
}
