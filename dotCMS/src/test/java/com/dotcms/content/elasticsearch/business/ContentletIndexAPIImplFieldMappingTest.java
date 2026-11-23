package com.dotcms.content.elasticsearch.business;

import static com.dotcms.content.index.IndexConfigHelper.MigrationPhase.FLAG_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.dotcms.content.elasticsearch.business.ContentletIndexAPIImplPhaseTest.FakeContentletIndexOperations;
import com.dotcms.content.elasticsearch.business.ContentletIndexAPIImplPhaseTest.FakeIndexAPI;
import com.dotcms.content.elasticsearch.business.ContentletIndexAPIImplPhaseTest.FakeIndiciesAPI;
import com.dotcms.content.elasticsearch.business.ContentletIndexAPIImplPhaseTest.FakeVersionedIndicesAPI;
import com.dotcms.content.elasticsearch.util.MappingHelper;
import com.dotcms.content.index.IndexTag;
import com.dotcms.content.index.VersionedIndicesImpl;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.dotmarketing.util.Config;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Test;

/**
 * Unit tests for the per-provider resolution of the mapping applied when a new field is created.
 *
 * <p>Covers <a href="https://github.com/dotCMS/core/issues/36577">#36577</a>. The mapping for a
 * newly created field used to be pushed to the index names held in the Elasticsearch pointer
 * store, using the untagged (phase-dispatched) overload — so in a dual-write phase the write was
 * mirrored onto OpenSearch using an Elasticsearch name, and in Phase 3, where the Elasticsearch
 * store is no longer maintained, it was not pushed anywhere at all.</p>
 *
 * <p>Each provider must receive the mapping on the index names that it actually owns, which is
 * what the {@link IndexTag}-aware overload of {@link MappingHelper} expresses.</p>
 */
public class ContentletIndexAPIImplFieldMappingTest {

    private static final String ES_WORKING = "working_esTs";
    private static final String ES_LIVE    = "live_esTs";
    private static final String OS_WORKING = "working_osTs.os";
    private static final String OS_LIVE    = "live_osTs.os";

    @After
    public void clearPhase() {
        Config.setProperty(FLAG_KEY, null);
    }

    private static void setPhase(final int ordinal) {
        Config.setProperty(FLAG_KEY, String.valueOf(ordinal));
    }

    /**
     * Given Scenario: Phase 2 (dual-write) where the two providers hold indices created at
     *                 different times, so their names differ in timestamp as well as in tag —
     *                 the migration-catchup shape described in the migration guide.
     * When : the mapping for a newly created field is applied.
     * Then : Elasticsearch receives it on its own names and OpenSearch on its own names. The
     *        Elasticsearch names must never reach the OpenSearch provider.
     */
    @Test
    public void testAddCustomMappingForNewField_WhenDualWriteWithDivergentNames_ShouldTargetEachProvidersOwnIndices() {
        setPhase(2);
        final RecordingMappingHelper helper = new RecordingMappingHelper();
        final ContentletIndexAPIImpl api = buildApi();

        api.addCustomMappingForNewField(newField(), helper);

        assertEquals("Each provider must be called exactly once", 2, helper.calls.size());
        assertEquals(List.of(ES_WORKING, ES_LIVE), helper.indexesFor(IndexTag.ES));
        assertEquals(List.of(OS_WORKING, OS_LIVE), helper.indexesFor(IndexTag.OS));
        assertTrue("An Elasticsearch name must never be pushed to the OpenSearch provider",
                helper.indexesFor(IndexTag.OS).stream().noneMatch(name -> name.contains("esTs")));
    }

    /**
     * Given Scenario: Phase 3, where OpenSearch is the only store and the Elasticsearch pointer
     *                 store is deliberately left empty.
     * When : the mapping for a newly created field is applied.
     * Then : OpenSearch still receives it. Previously nothing was written, leaving the field on
     *        dynamic mapping until the next full reindex.
     */
    @Test
    public void testAddCustomMappingForNewField_WhenOpenSearchOnly_ShouldStillMapOnOpenSearch() {
        setPhase(3);
        final RecordingMappingHelper helper = new RecordingMappingHelper();
        final ContentletIndexAPIImpl api = buildApi();

        api.addCustomMappingForNewField(newField(), helper);

        assertEquals("Only the OpenSearch provider is active in Phase 3", 1, helper.calls.size());
        assertEquals(List.of(OS_WORKING, OS_LIVE), helper.indexesFor(IndexTag.OS));
    }

    /**
     * Builds the API with both pointer stores populated: Elasticsearch names in the legacy store,
     * OpenSearch names in the versioned store.
     */
    private static ContentletIndexAPIImpl buildApi() {
        final FakeIndiciesAPI legacy = new FakeIndiciesAPI();
        legacy.setWorking(ES_WORKING);
        legacy.setLive(ES_LIVE);

        final FakeVersionedIndicesAPI versioned = new FakeVersionedIndicesAPI();
        versioned.stored = VersionedIndicesImpl.builder()
                .working(OS_WORKING)
                .live(OS_LIVE)
                .build();

        return new ContentletIndexAPIImpl(
                new FakeContentletIndexOperations(),
                new FakeContentletIndexOperations(),
                new FakeIndexAPI(List.of()),
                legacy,
                versioned);
    }

    private static Field newField() {
        return ImmutableTextField.builder()
                .name("myNewField")
                .variable("myNewField")
                .contentTypeId("someContentTypeId")
                .build();
    }

    /** {@link MappingHelper} fake that records which indexes were mapped for which provider. */
    static final class RecordingMappingHelper implements MappingHelper {

        record Call(List<String> indexes, IndexTag tag) { }

        final List<Call> calls = new ArrayList<>();

        List<String> indexesFor(final IndexTag tag) {
            return calls.stream()
                    .filter(call -> call.tag() == tag)
                    .findFirst()
                    .map(Call::indexes)
                    .orElse(List.of());
        }

        @Override
        public void addCustomMapping(final Field field, final List<String> indexes,
                final IndexTag tag) {
            calls.add(new Call(List.copyOf(indexes), tag));
        }

        @Override
        public void addCustomMapping(final String... indexes) {
            throw new UnsupportedOperationException("untagged fan-out must not be used");
        }

        @Override
        public void addCustomMapping(final Field field, final String... indexes) {
            throw new UnsupportedOperationException("untagged fan-out must not be used");
        }

        @Override
        public void addCustomMapping(final List<String> indexes, final IndexTag tag) {
            throw new UnsupportedOperationException("not exercised by these tests");
        }
    }
}
