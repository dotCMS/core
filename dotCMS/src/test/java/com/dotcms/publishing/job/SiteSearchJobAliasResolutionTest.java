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
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Before;
import org.quartz.JobDataMap;
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
    // The repair must outlive the run that made it (issue #36983)
    // =======================================================================

    /**
     * The heart of it: a job saved with a raw index name has its real alias recovered, and that
     * recovery is written back to the job detail.
     *
     * <p>Without this the crawl still succeeds — but it deletes the index whose name is stored, so the
     * NEXT run resolves that string to nothing, abandons the index it just built (which keeps the real
     * alias and quietly stops being crawled) and starts a fresh one. The repair has to be persisted or
     * it is only a per-run workaround.</p>
     */
    @Test
    public void test_recoveredAlias_isWrittenBackToTheJobDetail() {
        final JobDataMap dataMap = new JobDataMap();
        dataMap.put(SiteSearchJobImpl.INDEX_ALIAS, EXISTING_INDEX);

        SiteSearchJobImpl.persistResolvedAlias(dataMap, EXISTING_INDEX, CUSTOM_ALIAS);

        assertEquals("The job must remember the alias, not the name of the index it just replaced",
                CUSTOM_ALIAS, dataMap.getString(SiteSearchJobImpl.INDEX_ALIAS));
    }

    /** A job already holding a real alias is left untouched — nothing to repair, nothing to write. */
    @Test
    public void test_aliasAlreadyCorrect_isNotRewritten() {
        final JobDataMap dataMap = new JobDataMap();
        dataMap.put(SiteSearchJobImpl.INDEX_ALIAS, CUSTOM_ALIAS);

        SiteSearchJobImpl.persistResolvedAlias(dataMap, CUSTOM_ALIAS, CUSTOM_ALIAS);

        assertEquals(CUSTOM_ALIAS, dataMap.getString(SiteSearchJobImpl.INDEX_ALIAS));
    }

    /**
     * An index with no alias leaves nothing stable to store — its name changes on every full crawl —
     * so the stored value is left as it is rather than blanked.
     */
    @Test
    public void test_noAliasToRecover_leavesTheJobDetailAlone() {
        final JobDataMap dataMap = new JobDataMap();
        dataMap.put(SiteSearchJobImpl.INDEX_ALIAS, EXISTING_INDEX);

        SiteSearchJobImpl.persistResolvedAlias(dataMap, EXISTING_INDEX, null);

        assertEquals(EXISTING_INDEX, dataMap.getString(SiteSearchJobImpl.INDEX_ALIAS));
    }

    // =======================================================================
    // A dead index's name must never become a live index's alias (issue #36983)
    // =======================================================================

    /**
     * The second half of the defect. A job pointing at a `sitesearch_<timestamp>` that no longer
     * exists — deleted by an earlier crawl, or by hand — must NOT have that name stamped onto the new
     * index as its alias, which is precisely the damage this issue is about.
     */
    @Test
    public void test_staleIndexNameIsNotAdoptedAsANewIndexAlias() {
        final IndexMetaData brandNew = new IndexMetaData(null, false, EXISTING_INDEX, true);

        assertNull(SiteSearchJobImpl.aliasForNewIndex(brandNew, EXISTING_INDEX));
    }

    /** A real alias someone chose is still carried onto the new index. */
    @Test
    public void test_realAliasIsStillAdoptedAsANewIndexAlias() {
        final IndexMetaData brandNew = new IndexMetaData(null, false, CUSTOM_ALIAS, true);

        assertEquals(CUSTOM_ALIAS, SiteSearchJobImpl.aliasForNewIndex(brandNew, CUSTOM_ALIAS));
    }

    /** An existing index keeps its alias through the switch, so nothing is applied at creation. */
    @Test
    public void test_existingIndexGetsNoAliasAtCreationTime() {
        final IndexMetaData existing = new IndexMetaData(EXISTING_INDEX, false, CUSTOM_ALIAS, false);

        assertNull(SiteSearchJobImpl.aliasForNewIndex(existing, CUSTOM_ALIAS));
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
     * A job whose mirror supplier records whether it was consulted, so a test can assert that no
     * measurement happened at all — not merely that it produced no warning.
     */
    private SiteSearchJobImpl jobRecordingWhetherItMeasured(final AtomicBoolean measured) {
        return new SiteSearchJobImpl(mock(IndiciesAPI.class), siteSearchAPI, mock(HostAPI.class),
                mock(UserAPI.class), mock(SiteSearchAuditAPI.class), mock(PublisherAPI.class),
                () -> {
                    measured.set(true);
                    return List.of(contentRow(686L, 686, 21));
                });
    }

    /** Opts the check in (it is off by default) and puts the install in the given phase. */
    private static void optIn(final String phase) {
        Config.setProperty(SiteSearchJobImpl.MIN_CONTENT_INDEXED_KEY, "95");
        Config.setProperty(MigrationPhase.FLAG_KEY, phase);
    }

    /** Back to the shipped defaults: check off, migration not started. */
    private static void restoreDefaults() {
        Config.setProperty(SiteSearchJobImpl.MIN_CONTENT_INDEXED_KEY, null);
        Config.setProperty(MigrationPhase.FLAG_KEY, null);
    }

    /**
     * The case this exists for: a Phase-3 crawl reading an OpenSearch content index that was never
     * rebuilt. The crawl queries that index to build its corpus, so it can only produce a partial Site
     * Search index — and reindexing the content afterwards does not repair it.
     */
    @Test
    public void test_incompleteContentIndexOnTheReadEngine_isWarnedAbout() {
        optIn("3"); // reads = OpenSearch
        try {
            final Optional<String> warning = jobSeeing(contentRow(686L, 686, 21))
                    .incompleteContentIndexWarning();

            assertTrue(warning.isPresent());
            assertTrue(warning.get().contains("INCOMPLETE content index"));
            assertTrue(warning.get().contains("OpenSearch"));
            assertTrue(warning.get().contains("3.06%"));
        } finally {
            restoreDefaults();
        }
    }

    /**
     * The same incomplete OpenSearch copy is NOT warned about in a phase that reads Elasticsearch: the
     * crawl will query the complete ES index, so its corpus is fine. Warning there would train
     * operators to ignore the message.
     */
    @Test
    public void test_incompleteCopyOnTheEngineNotBeingRead_isNotWarnedAbout() {
        optIn("1"); // reads = Elasticsearch
        try {
            assertFalse(jobSeeing(contentRow(686L, 686, 21))
                    .incompleteContentIndexWarning().isPresent());
        } finally {
            restoreDefaults();
        }
    }

    /** A complete index says nothing. */
    @Test
    public void test_completeContentIndex_isNotWarnedAbout() {
        optIn("3");
        try {
            assertFalse(jobSeeing(contentRow(686L, 686, 686))
                    .incompleteContentIndexWarning().isPresent());
        } finally {
            restoreDefaults();
        }
    }

    /** Without a database denominator there is no coverage to judge — silence, not a false alarm. */
    @Test
    public void test_noDatabaseDenominator_isNotWarnedAbout() {
        optIn("3");
        try {
            assertFalse(jobSeeing(contentRow(null, 686, 21))
                    .incompleteContentIndexWarning().isPresent());
        } finally {
            restoreDefaults();
        }
    }

    /** The check is advisory: if it cannot be computed, the crawl proceeds silently. */
    @Test
    public void test_failureToMeasure_isSwallowed() {
        optIn("3");
        try {
            final SiteSearchJobImpl failing = new SiteSearchJobImpl(mock(IndiciesAPI.class),
                    siteSearchAPI, mock(HostAPI.class), mock(UserAPI.class),
                    mock(SiteSearchAuditAPI.class), mock(PublisherAPI.class),
                    () -> { throw new IllegalStateException("cluster down"); });

            assertFalse(failing.incompleteContentIndexWarning().isPresent());
        } finally {
            restoreDefaults();
        }
    }

    /**
     * The default: nobody asked for the check, so nothing is measured — not even in the phase where the
     * warning would have something to say. Measuring costs a sequential scan of
     * {@code contentlet_version_info} plus a stats call and a count query per engine, on every crawl,
     * and it changes nothing about how the crawl runs; an install must not pay that on a schedule for a
     * diagnostic.
     *
     * <p>Asserted by recording whether the supplier was consulted, NOT by having it throw: the
     * measurement runs inside a vavr {@code Try}, which swallows {@link Throwable}, so a thrown
     * assertion would be absorbed and the test would pass against the very regression it guards. For
     * the same reason a plain {@code assertFalse} on the result is not enough on its own — it also
     * passes when the work ran and merely found nothing to report.</p>
     */
    @Test
    public void test_notOptedIn_doesNotMeasureAtAll() {
        Config.setProperty(MigrationPhase.FLAG_KEY, "3"); // the phase that WOULD warn
        try {
            final AtomicBoolean measured = new AtomicBoolean(false);

            assertFalse(jobRecordingWhetherItMeasured(measured)
                    .incompleteContentIndexWarning().isPresent());
            assertFalse("The content mirror must not be measured unless the check is switched on",
                    measured.get());
        } finally {
            restoreDefaults();
        }
    }

    /**
     * Even switched on, the check does not run before the migration starts: there is no second engine
     * whose copy could have been left behind, which is the failure it exists to catch, so it could only
     * ever report the one Elasticsearch index the whole product already depends on.
     */
    @Test
    public void test_migrationNotStarted_doesNotMeasureAtAll() {
        optIn("0");
        try {
            final AtomicBoolean measured = new AtomicBoolean(false);

            assertFalse(jobRecordingWhetherItMeasured(measured)
                    .incompleteContentIndexWarning().isPresent());
            assertFalse("The content mirror must not be measured before the migration starts",
                    measured.get());
        } finally {
            restoreDefaults();
        }
    }

    /** Explicitly setting the threshold to 0 switches the check back off. */
    @Test
    public void test_thresholdZero_disablesTheCheck() {
        optIn("3");
        Config.setProperty(SiteSearchJobImpl.MIN_CONTENT_INDEXED_KEY, "0");
        try {
            assertFalse(jobSeeing(contentRow(686L, 686, 21))
                    .incompleteContentIndexWarning().isPresent());
        } finally {
            restoreDefaults();
        }
    }
}