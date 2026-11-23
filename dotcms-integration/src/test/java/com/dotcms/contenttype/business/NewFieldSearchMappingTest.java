package com.dotcms.contenttype.business;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.content.elasticsearch.business.IndiciesInfo;
import com.dotcms.content.index.IndexConfigHelper.MigrationPhase;
import com.dotcms.content.index.VersionedIndices;
import com.dotcms.contenttype.model.field.DateField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;
import com.liferay.util.StringPool;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Verifies against a live search cluster that the mapping of a newly created field actually lands
 * on the indices the current phase reads from.
 *
 * <p>Covers <a href="https://github.com/dotCMS/core/issues/36577">#36577</a>. The mapping used to
 * be pushed to the index names held in the Elasticsearch pointer store, so once Elasticsearch was
 * decommissioned and that store stopped being maintained, nothing was pushed at all and the field
 * stayed on dynamic mapping until the next full reindex.</p>
 *
 * <p>The test deliberately indexes no content: with an empty index, dynamic mapping cannot invent
 * the field, so finding it in the mapping proves the explicit push happened.</p>
 *
 * <p>It adapts to whichever phase it is run under: the index names are taken from the store that
 * owns the current <em>read</em> provider, deliberately rather than from
 * {@code getActiveIndexName}, which by design keeps reporting the Elasticsearch store through
 * Phase 2. That is what makes this complementary to
 * {@code FieldAPITest#test_SaveNewIndexedField_ShouldAddESMapping}: in a dual-write phase that
 * test checks the Elasticsearch leg and this one checks the OpenSearch leg, which nothing else
 * covers.</p>
 */
public class NewFieldSearchMappingTest {

    private static ContentType contentType;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        contentType = new ContentTypeDataGen().nextPersisted();
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        if (contentType != null) {
            APILocator.getContentTypeAPI(APILocator.systemUser()).delete(contentType);
        }
    }

    /**
     * Given Scenario: a content type exists on an instance whose indices are already in place, and
     *                 a new indexed field is added to it.
     * When : the active indices of the current read provider are asked for that field's mapping.
     * Then : the field is present in all of them. An empty mapping means the push never reached
     *        the provider the instance reads from.
     */
    @Test
    public void testNewIndexedField_ShouldBeMappedOnTheActiveIndices() throws Exception {
        // Built with FieldBuilder rather than FieldDataGen: the data generator always assigns a
        // text default value, which a date field rejects outright (ImmutableDateTimeField invalid
        // default Value). FieldAPITest builds its date field the same way for the same reason.
        final String fieldName = "field" + System.currentTimeMillis();
        final Field field = APILocator.getContentTypeFieldAPI().save(
                FieldBuilder.builder(DateField.class)
                        .name(fieldName)
                        .variable(fieldName)
                        .contentTypeId(contentType.id())
                        .indexed(true)
                        .build(),
                APILocator.systemUser());

        final List<String> activeIndices = activeIndicesOfReadProvider();
        assertFalse("The instance must have active indices for this test to mean anything",
                activeIndices.isEmpty());

        // dotCMS keys the mapping by "<contentTypeVar>.<fieldVar>", lowercased — a bare leaf name
        // does not resolve and would come back empty whatever the state of the index.
        final String mappingKey =
                (contentType.variable() + StringPool.PERIOD + field.variable()).toLowerCase();

        for (final String index : activeIndices) {
            final Map<String, Object> mapping = APILocator.getContentMappingAPI()
                    .getFieldMappingAsMap(index, mappingKey);
            assertNotNull("getFieldMappingAsMap must never return null", mapping);
            assertFalse(String.format(
                    "The mapping of field '%s' never reached index '%s' under phase %s",
                    field.variable(), index, MigrationPhase.current().name()),
                    mapping.isEmpty());
            Logger.info(NewFieldSearchMappingTest.class, String.format(
                    "Field '%s' is mapped on '%s': %s", field.variable(), index, mapping));
        }
    }

    /**
     * Returns the working and live index names of the provider the current phase reads from —
     * Elasticsearch's pointer store before the read switches over, OpenSearch's versioned store
     * from Phase 2 on. The two stores hold different names, so the provider decides which to ask.
     */
    private static List<String> activeIndicesOfReadProvider() throws Exception {
        final List<String> indices = new ArrayList<>(2);
        final MigrationPhase phase = MigrationPhase.current();

        // isReadEnabled() already covers Phase 3, where OpenSearch is the only store.
        if (phase.isReadEnabled()) {
            final Optional<VersionedIndices> osIndices =
                    APILocator.getVersionedIndicesAPI().loadDefaultVersionedIndices();
            assertTrue("Phase " + phase.name() + " reads from OpenSearch, so its index store"
                    + " must hold a record", osIndices.isPresent());
            osIndices.get().working().ifPresent(indices::add);
            osIndices.get().live().ifPresent(indices::add);
            return indices;
        }

        final IndiciesInfo esIndices = APILocator.getIndiciesAPI().loadIndicies();
        assertNotNull("Phase " + phase.name() + " reads from Elasticsearch, so its index store"
                + " must hold a record", esIndices);
        indices.add(esIndices.getWorking());
        indices.add(esIndices.getLive());
        return indices;
    }
}
