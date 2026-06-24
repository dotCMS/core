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
 *   <li>an index already present in the cluster is <b>reused</b> (no create) and its mapping is
 *       re-asserted — the orphaned-index repair path;</li>
 *   <li>a missing index is created and, on success, mapped;</li>
 *   <li>a failed create does not apply a mapping;</li>
 *   <li>a failing existence probe is treated as "does not exist", so bootstrap falls through to
 *       the create path instead of aborting.</li>
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
     * Given : the physical index already exists in the target cluster (an orphaned cluster index
     *         left behind by a previous bootstrap that never committed its store pointer).
     * When  : createContentIndex() runs during bootstrap.
     * Then  : the index is reused (no create is issued), the custom mapping is re-asserted to
     *         repair a possibly-unmapped orphan, and the method returns true.
     */
    @Test
    public void test_orphanIndexExists_reusesAndReassertsMapping_skipsCreate() throws IOException {
        final ContentletIndexOperations ops = mock(ContentletIndexOperations.class);
        final IndexAPI providerApi = mock(IndexAPI.class);
        final MappingHelper helper = mock(MappingHelper.class);

        when(ops.toPhysicalName(LOGICAL_NAME)).thenReturn(PHYSICAL_NAME);
        when(providerApi.indexExists(PHYSICAL_NAME)).thenReturn(true);

        final boolean result = newApi()
                .createContentIndex(LOGICAL_NAME, SHARDS, IndexTag.ES, ops, providerApi, helper);

        assertTrue("Existing (orphaned) index must be reused and reported as available", result);
        verify(ops, never()).createContentIndex(anyString(), anyInt());
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
     * Given : an OS-tagged bootstrap of an already-existing index.
     * When  : createContentIndex() runs with {@link IndexTag#OS}.
     * Then  : the mapping is re-asserted against the OS provider — the tag is propagated unchanged
     *         to the mapping helper so the correct vendor is targeted.
     */
    @Test
    public void test_osTag_isPropagatedToMappingHelper() throws IOException {
        final ContentletIndexOperations ops = mock(ContentletIndexOperations.class);
        final IndexAPI providerApi = mock(IndexAPI.class);
        final MappingHelper helper = mock(MappingHelper.class);

        final String osPhysical = PHYSICAL_NAME + ".os";
        when(ops.toPhysicalName(LOGICAL_NAME)).thenReturn(osPhysical);
        when(providerApi.indexExists(osPhysical)).thenReturn(true);

        final boolean result = newApi()
                .createContentIndex(LOGICAL_NAME, SHARDS, IndexTag.OS, ops, providerApi, helper);

        assertTrue(result);
        verify(helper).addCustomMapping(List.of(LOGICAL_NAME), IndexTag.OS);
    }
}