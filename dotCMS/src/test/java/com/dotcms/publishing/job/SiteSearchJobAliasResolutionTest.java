package com.dotcms.publishing.job;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.content.elasticsearch.business.IndiciesAPI;
import com.dotcms.content.index.IndexConfigHelper.MigrationPhase;
import com.dotcms.content.index.migration.MirrorStatus;
import com.dotcms.enterprise.publishing.sitesearch.SiteSearchResults;
import com.dotcms.publishing.PublisherAPI;
import com.dotcms.publishing.job.SiteSearchJobImpl.IndexMetaData;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.sitesearch.business.SiteSearchAPI;
import com.dotmarketing.sitesearch.business.SiteSearchAuditAPI;
import com.dotmarketing.util.Config;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;

/**
 * Container-free coverage for how {@link SiteSearchJobImpl} resolves the alias a crawl must end up
 * with (issue #36983, Bug 1).
 *
 * <p>The job detail stores whatever the Site Search scheduler put in its {@code indexAlias} field,
 * and that value is <em>not</em> guaranteed to be an alias: when alias resolution missed on
 * OpenSearch (Phases 2/3) the index selector fell back to the raw internal index name and saved it
 * there. The alias derived here is the one handed to the publisher, which re-applies it to the newly
 * built index after the switch — so carrying a raw index name forward made a dead index's NAME
 * become the new index's alias, wiping the alias the user created. These tests pin the resolution
 * rules so that cannot come back.</p>
 */
public class SiteSearchJobAliasResolutionTest {

    private static final String EXISTING_INDEX = "sitesearch_20260810160529";
    private static final String CUSTOM_ALIAS = "sitesearch-ph-3";

    private SiteSearchAPI siteSearchAPI;
    private SiteSearchJobImpl job;

    @Before
    public void setup() {
        siteSearchAPI = mock(SiteSearchAPI.class);
        when(siteSearchAPI.listIndices()).thenReturn(Collections.singletonList(EXISTING_INDEX));
        when(siteSearchAPI.search(anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(new SiteSearchResults());

        job = new SiteSearchJobImpl(mock(IndiciesAPI.class), siteSearchAPI, mock(HostAPI.class),
                mock(UserAPI.class), mock(SiteSearchAuditAPI.class), mock(PublisherAPI.class));
    }

    /**
     * A job saved with the index's real alias keeps behaving exactly as before: the alias travels
     * through untouched and resolves to the index it points at.
     */
    @Test
    public void test_aliasStoredInJobDetail_isCarriedThrough() throws Exception {
        when(siteSearchAPI.getAliasToIndexMapAllEngines()).thenReturn(Map.of(CUSTOM_ALIAS, EXISTING_INDEX));

        final IndexMetaData metaData = job.getIndexMetaData(CUSTOM_ALIAS);

        assertEquals(CUSTOM_ALIAS, metaData.getAlias());
        assertEquals(EXISTING_INDEX, metaData.getIndexName());
        assertFalse(metaData.isNewIndex());
    }

    /**
     * A job saved with a RAW INDEX NAME (the Phase 2/3 scheduler fallback) must resolve that index's
     * real alias — not hand the raw name over as if it were one. Handing it over is what replaced the
     * user's alias with a timestamped index name after a crawl (issue #36983).
     */
    @Test
    public void test_rawIndexNameStoredInJobDetail_resolvesTheIndexRealAlias() throws Exception {
        when(siteSearchAPI.getAliasToIndexMapAllEngines()).thenReturn(Map.of(CUSTOM_ALIAS, EXISTING_INDEX));

        final IndexMetaData metaData = job.getIndexMetaData(EXISTING_INDEX);

        assertEquals(CUSTOM_ALIAS, metaData.getAlias());
        assertNotEquals("The raw index name must never be re-applied as an alias", EXISTING_INDEX,
                metaData.getAlias());
        assertEquals(EXISTING_INDEX, metaData.getIndexName());
        assertFalse(metaData.isNewIndex());
    }

    /**
     * Same fallback, but the index genuinely has no alias: the crawl must end up with NO alias rather
     * than one invented from the old index's name.
     */
    @Test
    public void test_rawIndexNameWithoutAlias_resolvesToNoAlias() throws Exception {
        when(siteSearchAPI.getAliasToIndexMapAllEngines()).thenReturn(Collections.emptyMap());

        final IndexMetaData metaData = job.getIndexMetaData(EXISTING_INDEX);

        assertNull(metaData.getAlias());
        assertEquals(EXISTING_INDEX, metaData.getIndexName());
    }

    /**
     * A name that matches neither an alias nor an existing index describes a brand-new index: it is
     * kept as the alias to apply at creation time.
     */
    @Test
    public void test_unknownName_isKeptAsTheAliasOfANewIndex() throws Exception {
        when(siteSearchAPI.getAliasToIndexMapAllEngines()).thenReturn(Collections.emptyMap());
        when(siteSearchAPI.listIndices()).thenReturn(Collections.emptyList());

        final IndexMetaData metaData = job.getIndexMetaData("brand-new-alias");

        assertEquals("brand-new-alias", metaData.getAlias());
        assertTrue(metaData.isNewIndex());
    }

    // =======================================================================
    // Incomplete-content-index warning (issue #36983)
    // =======================================================================

    /** A content row with the given coverage on each engine. */
    private static MirrorStatus contentRow(final Long expected, final long esCount,
            final long osCount) {
        return new MirrorStatus("working_1", MirrorStatus.IndexKind.CONTENT_WORKING,
                new MirrorStatus.EngineCopy(true, esCount, "cluster_x.working_1"),
                new MirrorStatus.EngineCopy(true, osCount, "cluster_x.working_1.os"),
                MirrorStatus.Verdict.IN_SYNC, "", expected);
    }

    private SiteSearchJobImpl jobSeeing(final MirrorStatus... rows) {
        return new SiteSearchJobImpl(mock(IndiciesAPI.class), siteSearchAPI, mock(HostAPI.class),
                mock(UserAPI.class), mock(SiteSearchAuditAPI.class), mock(PublisherAPI.class),
                () -> List.of(rows));
    }

    /**
     * The case this exists for: a Phase-3 crawl reading an OpenSearch content index that was never
     * rebuilt. The crawl queries that index to build its corpus, so it can only produce a partial Site
     * Search index — and reindexing the content afterwards does not repair it.
     */
    @Test
    public void test_incompleteContentIndexOnTheReadEngine_isWarnedAbout() {
        Config.setProperty(MigrationPhase.FLAG_KEY, "3"); // reads = OpenSearch
        try {
            final Optional<String> warning = jobSeeing(contentRow(686L, 686, 21))
                    .incompleteContentIndexWarning();

            assertTrue(warning.isPresent());
            assertTrue(warning.get().contains("INCOMPLETE content index"));
            assertTrue(warning.get().contains("OpenSearch"));
            assertTrue(warning.get().contains("3.06%"));
        } finally {
            Config.setProperty(MigrationPhase.FLAG_KEY, null);
        }
    }

    /**
     * The same incomplete OpenSearch copy is NOT warned about in a phase that reads Elasticsearch: the
     * crawl will query the complete ES index, so its corpus is fine. Warning there would train
     * operators to ignore the message.
     */
    @Test
    public void test_incompleteCopyOnTheEngineNotBeingRead_isNotWarnedAbout() {
        Config.setProperty(MigrationPhase.FLAG_KEY, "1"); // reads = Elasticsearch
        try {
            assertFalse(jobSeeing(contentRow(686L, 686, 21))
                    .incompleteContentIndexWarning().isPresent());
        } finally {
            Config.setProperty(MigrationPhase.FLAG_KEY, null);
        }
    }

    /** A complete index says nothing. */
    @Test
    public void test_completeContentIndex_isNotWarnedAbout() {
        Config.setProperty(MigrationPhase.FLAG_KEY, "3");
        try {
            assertFalse(jobSeeing(contentRow(686L, 686, 686))
                    .incompleteContentIndexWarning().isPresent());
        } finally {
            Config.setProperty(MigrationPhase.FLAG_KEY, null);
        }
    }

    /** Without a database denominator there is no coverage to judge — silence, not a false alarm. */
    @Test
    public void test_noDatabaseDenominator_isNotWarnedAbout() {
        Config.setProperty(MigrationPhase.FLAG_KEY, "3");
        try {
            assertFalse(jobSeeing(contentRow(null, 686, 21))
                    .incompleteContentIndexWarning().isPresent());
        } finally {
            Config.setProperty(MigrationPhase.FLAG_KEY, null);
        }
    }

    /** The check is advisory: if it cannot be computed, the crawl proceeds silently. */
    @Test
    public void test_failureToMeasure_isSwallowed() {
        Config.setProperty(MigrationPhase.FLAG_KEY, "3");
        try {
            final SiteSearchJobImpl failing = new SiteSearchJobImpl(mock(IndiciesAPI.class),
                    siteSearchAPI, mock(HostAPI.class), mock(UserAPI.class),
                    mock(SiteSearchAuditAPI.class), mock(PublisherAPI.class),
                    () -> { throw new IllegalStateException("cluster down"); });

            assertFalse(failing.incompleteContentIndexWarning().isPresent());
        } finally {
            Config.setProperty(MigrationPhase.FLAG_KEY, null);
        }
    }

    /** The threshold is configurable, and 0 disables the check outright. */
    @Test
    public void test_thresholdZero_disablesTheCheck() {
        Config.setProperty(MigrationPhase.FLAG_KEY, "3");
        Config.setProperty(SiteSearchJobImpl.MIN_CONTENT_COVERAGE_KEY, "0");
        try {
            assertFalse(jobSeeing(contentRow(686L, 686, 21))
                    .incompleteContentIndexWarning().isPresent());
        } finally {
            Config.setProperty(SiteSearchJobImpl.MIN_CONTENT_COVERAGE_KEY, null);
            Config.setProperty(MigrationPhase.FLAG_KEY, null);
        }
    }
}