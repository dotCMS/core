package com.dotcms.content.elasticsearch.business;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.content.elasticsearch.util.MappingHelper;
import com.dotcms.content.index.ContentletIndexOperations;
import com.dotcms.content.index.IndexAPI;
import com.dotcms.content.index.IndexTag;
import com.dotcms.content.index.VersionedIndicesAPI;
import java.io.IOException;
import java.util.List;
import org.junit.Test;

/**
 * Unit tests for the idempotent index-bootstrap logic introduced for orphaned cluster indices
 * (#36237 / PR #36238).
 *
 * <p>Exercises {@link ContentletIndexAPIImpl#createContentIndex(String, int, IndexTag,
 * ContentletIndexOperations, IndexAPI, MappingHelper)} — the collaborator-injected seam of the
 * private {@code createContentIndex(indexName, shards, tag)} — so the orphan-reuse decision can be
 * validated without a running Elasticsearch/OpenSearch cluster or the {@code MappingHelper}
 * singleton. All collaborators are Mockito doubles.</p>
 *
 * <p>The behaviour under test:</p>
 * <ul>
 *   <li>an <b>empty</b> orphan (0 docs) is <b>deleted and recreated</b> from scratch (full settings
 *       + base mapping + custom mapping) — reusing in place cannot repair a bare orphan whose
 *       static custom analyzer can only be set at creation time (#36237);</li>
 *   <li>a <b>populated</b> orphan (&gt; 0 docs) is <b>reused untouched</b> — never deleted,
 *       recreated, or remapped, so its data (and any reindex progress) is preserved;</li>
 *   <li>a failing doc-count probe is treated as "populated" — the orphan is reused, never deleted;</li>
 *   <li>a missing index is created and, on success, mapped;</li>
 *   <li>a failed create does not apply a mapping;</li>
 *   <li>a failing existence probe is treated as "does not exist", so bootstrap falls through to
 *       the create path instead of aborting;</li>
 *   <li>a failed delete of an empty orphan does not abort bootstrap — the create is still attempted.</li>
 * </ul>
 */
public class ContentletIndexAPIImplBootstrapTest {

    private static final String CLUSTER_PREFIX = "cluster_test.";
    private static final String LOGICAL_NAME = "working_T0";
    private static final String PHYSICAL_NAME = CLUSTER_PREFIX + LOGICAL_NAME;
    private static final int SHARDS = 1;

    /**
     * Builds an instance solely so the package-private seam can be invoked. The constructor
     * dependencies are irrelevant to the seam, which operates exclusively on its injected
     * collaborators.
     */
    private static ContentletIndexAPIImpl newApi() {
        return new ContentletIndexAPIImpl(
                mock(ContentletIndexOperations.class),
                mock(ContentletIndexOperations.class),
                mock(IndexAPI.class),
                mock(IndiciesAPI.class),
                mock(VersionedIndicesAPI.class));
    }

    /**
     * Given : an EMPTY orphaned index (0 docs) exists in the cluster but is missing from the store
     *         (left by a previous bootstrap that never committed its store pointer).
     * When  : createContentIndex() runs during bootstrap.
     * Then  : the empty orphan is deleted and recreated from scratch (full settings + base mapping),
     *         the custom mapping is applied to the clean index, and the method returns true.
     */
    @Test
    public void test_emptyOrphan_deletedAndRecreated_withFullMapping() throws IOException {
        final ContentletIndexOperations ops = mock(ContentletIndexOperations.class);
        final IndexAPI providerApi = mock(IndexAPI.class);
        final MappingHelper helper = mock(MappingHelper.class);

        when(ops.toPhysicalName(LOGICAL_NAME)).thenReturn(PHYSICAL_NAME);
        when(providerApi.indexExists(PHYSICAL_NAME)).thenReturn(true);
        when(ops.getIndexDocumentCount(PHYSICAL_NAME)).thenReturn(0L);
        when(providerApi.delete(PHYSICAL_NAME)).thenReturn(true);
        when(ops.createContentIndex(PHYSICAL_NAME, SHARDS)).thenReturn(true);

        final boolean result = newApi()
                .createContentIndex(LOGICAL_NAME, SHARDS, IndexTag.ES, ops, providerApi, helper);

        assertTrue("Empty orphan must be recreated and reported as available", result);
        verify(providerApi).delete(PHYSICAL_NAME);
        verify(ops).createContentIndex(PHYSICAL_NAME, SHARDS);
        verify(helper).addCustomMapping(List.of(LOGICAL_NAME), IndexTag.ES);
    }

    /**
     * Given : a POPULATED orphaned index (&gt; 0 docs) exists in the cluster but is missing from
     *         the store.
     * When  : createContentIndex() runs during bootstrap.
     * Then  : the orphan is reused in place, untouched — it is NOT deleted, NOT recreated, and its
     *         mapping is NOT re-applied (a dotCMS-created index already carries the full mapping).
     *         Discarding it would force a costly full reindex. The method returns true.
     */
    @Test
    public void test_populatedOrphan_reusedInPlace_notDeletedNotRemapped() throws IOException {
        final ContentletIndexOperations ops = mock(ContentletIndexOperations.class);
        final IndexAPI providerApi = mock(IndexAPI.class);
        final MappingHelper helper = mock(MappingHelper.class);

        when(ops.toPhysicalName(LOGICAL_NAME)).thenReturn(PHYSICAL_NAME);
        when(providerApi.indexExists(PHYSICAL_NAME)).thenReturn(true);
        when(ops.getIndexDocumentCount(PHYSICAL_NAME)).thenReturn(42L);

        final boolean result = newApi()
                .createContentIndex(LOGICAL_NAME, SHARDS, IndexTag.ES, ops, providerApi, helper);

        assertTrue("Populated orphan must be reused and reported as available", result);
        verify(providerApi, never()).delete(PHYSICAL_NAME);
        verify(ops, never()).createContentIndex(anyString(), anyInt());
        verify(helper, never()).addCustomMapping(List.of(LOGICAL_NAME), IndexTag.ES);
    }

    /**
     * Given : an orphan exists but the document-count probe fails (e.g. transient cluster error).
     * When  : createContentIndex() runs during bootstrap.
     * Then  : the uncertainty is treated as "has data" — the orphan is reused in place and never
     *         deleted, so a possibly-populated index is never discarded on a flaky probe.
     */
    @Test
    public void test_orphanDocCountProbeFails_treatedAsPopulated_reused() throws IOException {
        final ContentletIndexOperations ops = mock(ContentletIndexOperations.class);
        final IndexAPI providerApi = mock(IndexAPI.class);
        final MappingHelper helper = mock(MappingHelper.class);

        when(ops.toPhysicalName(LOGICAL_NAME)).thenReturn(PHYSICAL_NAME);
        when(providerApi.indexExists(PHYSICAL_NAME)).thenReturn(true);
        when(ops.getIndexDocumentCount(PHYSICAL_NAME))
                .thenThrow(new RuntimeException("count unavailable"));

        final boolean result = newApi()
                .createContentIndex(LOGICAL_NAME, SHARDS, IndexTag.ES, ops, providerApi, helper);

        assertTrue("Unknown doc count must be reused (never deleted)", result);
        verify(providerApi, never()).delete(PHYSICAL_NAME);
        verify(ops, never()).createContentIndex(anyString(), anyInt());
    }

    /**
     * Given : an EMPTY orphan exists but its delete is not acknowledged (e.g. transient error).
     * When  : createContentIndex() runs during bootstrap.
     * Then  : the failure does not abort bootstrap — the create is still attempted and, on
     *         success here, the mapping is applied.
     */
    @Test
    public void test_emptyOrphanDeleteFails_stillAttemptsCreate() throws IOException {
        final ContentletIndexOperations ops = mock(ContentletIndexOperations.class);
        final IndexAPI providerApi = mock(IndexAPI.class);
        final MappingHelper helper = mock(MappingHelper.class);

        when(ops.toPhysicalName(LOGICAL_NAME)).thenReturn(PHYSICAL_NAME);
        when(providerApi.indexExists(PHYSICAL_NAME)).thenReturn(true);
        when(ops.getIndexDocumentCount(PHYSICAL_NAME)).thenReturn(0L);
        when(providerApi.delete(PHYSICAL_NAME))
                .thenThrow(new RuntimeException("delete not acknowledged"));
        when(ops.createContentIndex(PHYSICAL_NAME, SHARDS)).thenReturn(true);

        final boolean result = newApi()
                .createContentIndex(LOGICAL_NAME, SHARDS, IndexTag.ES, ops, providerApi, helper);

        assertTrue("A failed orphan delete must fall through to a (successful) create", result);
        verify(ops).createContentIndex(PHYSICAL_NAME, SHARDS);
        verify(helper).addCustomMapping(List.of(LOGICAL_NAME), IndexTag.ES);
    }

    /**
     * Given : the physical index does not exist in the target cluster.
     * When  : createContentIndex() runs and the create succeeds.
     * Then  : the index is created with the resolved physical name and shard count, the custom
     *         mapping is applied, and the method returns true.
     */
    @Test
    public void test_indexMissing_createSucceeds_appliesMapping() throws IOException {
        final ContentletIndexOperations ops = mock(ContentletIndexOperations.class);
        final IndexAPI providerApi = mock(IndexAPI.class);
        final MappingHelper helper = mock(MappingHelper.class);

        when(ops.toPhysicalName(LOGICAL_NAME)).thenReturn(PHYSICAL_NAME);
        when(providerApi.indexExists(PHYSICAL_NAME)).thenReturn(false);
        when(ops.createContentIndex(PHYSICAL_NAME, SHARDS)).thenReturn(true);

        final boolean result = newApi()
                .createContentIndex(LOGICAL_NAME, SHARDS, IndexTag.ES, ops, providerApi, helper);

        assertTrue("A successful create must be reported as available", result);
        verify(ops).createContentIndex(PHYSICAL_NAME, SHARDS);
        verify(helper).addCustomMapping(List.of(LOGICAL_NAME), IndexTag.ES);
    }

    /**
     * Given : the physical index does not exist.
     * When  : createContentIndex() runs and the create fails (returns false).
     * Then  : no mapping is applied and the method returns false — the failure is not masked.
     */
    @Test
    public void test_indexMissing_createFails_noMapping() throws IOException {
        final ContentletIndexOperations ops = mock(ContentletIndexOperations.class);
        final IndexAPI providerApi = mock(IndexAPI.class);
        final MappingHelper helper = mock(MappingHelper.class);

        when(ops.toPhysicalName(LOGICAL_NAME)).thenReturn(PHYSICAL_NAME);
        when(providerApi.indexExists(PHYSICAL_NAME)).thenReturn(false);
        when(ops.createContentIndex(PHYSICAL_NAME, SHARDS)).thenReturn(false);

        final boolean result = newApi()
                .createContentIndex(LOGICAL_NAME, SHARDS, IndexTag.ES, ops, providerApi, helper);

        assertFalse("A failed create must not be reported as available", result);
        verify(helper, never()).addCustomMapping(List.of(LOGICAL_NAME), IndexTag.ES);
    }

    /**
     * Given : the existence probe itself fails (e.g. a transient cluster error).
     * When  : createContentIndex() runs.
     * Then  : the probe failure is swallowed and treated as "does not exist", so bootstrap falls
     *         through to the create path rather than aborting. Here the subsequent create
     *         succeeds, so the index is created and mapped.
     */
    @Test
    public void test_existenceProbeThrows_treatedAsMissing_proceedsToCreate() throws IOException {
        final ContentletIndexOperations ops = mock(ContentletIndexOperations.class);
        final IndexAPI providerApi = mock(IndexAPI.class);
        final MappingHelper helper = mock(MappingHelper.class);

        when(ops.toPhysicalName(LOGICAL_NAME)).thenReturn(PHYSICAL_NAME);
        when(providerApi.indexExists(PHYSICAL_NAME))
                .thenThrow(new RuntimeException("cluster unreachable"));
        when(ops.createContentIndex(PHYSICAL_NAME, SHARDS)).thenReturn(true);

        final boolean result = newApi()
                .createContentIndex(LOGICAL_NAME, SHARDS, IndexTag.ES, ops, providerApi, helper);

        assertTrue("A failed existence probe must fall through to a (successful) create", result);
        verify(ops).createContentIndex(PHYSICAL_NAME, SHARDS);
        verify(helper).addCustomMapping(List.of(LOGICAL_NAME), IndexTag.ES);
    }

    /**
     * Given : an OS-tagged bootstrap of an already-existing EMPTY (orphaned) index.
     * When  : createContentIndex() runs with {@link IndexTag#OS}.
     * Then  : the empty orphan is deleted and recreated against the OS provider — the fully-tagged
     *         physical name is used for the delete, create and doc-count probe, and the OS tag is
     *         propagated unchanged to the mapping helper so the correct vendor is targeted.
     */
    @Test
    public void test_osTag_emptyOrphanDeletedRecreated_andTagPropagated() throws IOException {
        final ContentletIndexOperations ops = mock(ContentletIndexOperations.class);
        final IndexAPI providerApi = mock(IndexAPI.class);
        final MappingHelper helper = mock(MappingHelper.class);

        final String osPhysical = PHYSICAL_NAME + ".os";
        when(ops.toPhysicalName(LOGICAL_NAME)).thenReturn(osPhysical);
        when(providerApi.indexExists(osPhysical)).thenReturn(true);
        when(ops.getIndexDocumentCount(osPhysical)).thenReturn(0L);
        when(providerApi.delete(osPhysical)).thenReturn(true);
        when(ops.createContentIndex(osPhysical, SHARDS)).thenReturn(true);

        final boolean result = newApi()
                .createContentIndex(LOGICAL_NAME, SHARDS, IndexTag.OS, ops, providerApi, helper);

        assertTrue(result);
        verify(providerApi).delete(osPhysical);
        verify(ops).createContentIndex(osPhysical, SHARDS);
        verify(helper).addCustomMapping(List.of(LOGICAL_NAME), IndexTag.OS);
    }
}