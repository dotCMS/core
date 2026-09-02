package com.dotcms.content.index.opensearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.DataProviderWeldRunner;
import com.dotcms.IntegrationTestBase;
import com.dotcms.content.index.domain.IndexBulkRequest;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.util.Logger;
import java.util.Map;
import java.util.UUID;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.indices.GetIndexResponse;
import org.opensearch.client.opensearch.indices.RefreshRequest;

/**
 * Integration tests that verify the full index-creation-with-mapping pipeline of
 * {@link ContentletIndexOperationsOS#createContentIndex}.
 *
 * <p>A single {@link ContentletIndexOperationsOS#createContentIndex} call must:</p>
 * <ol>
 *   <li>Create the physical OS index (verified by {@code indexExists}).</li>
 *   <li>Apply the dotCMS dynamic-template mapping from {@code os-content-mapping.json}
 *       (verified by reading the mapping back via {@link MappingOperationsOS#getMapping}).</li>
 *   <li>Configure custom analysers from {@code os-content-settings.json}
 *       (verified by inspecting the index settings).</li>
 * </ol>
 *
 * <p>Registered in {@link com.dotcms.OpenSearchUpgradeSuite}. Run with:</p>
 * <pre>
 *   ./mvnw verify -pl :dotcms-integration \
 *       -Dcoreit.test.skip=false \
 *       -Dopensearch.upgrade.test=true
 * </pre>
 *
 * @author Fabrizio Araya
 * @see ContentletIndexOperationsOS#createContentIndex
 */
@ApplicationScoped
@RunWith(DataProviderWeldRunner.class)
public class OSCreateContentIndexIntegrationTest extends IntegrationTestBase {

    private static final String RUN_ID =
            UUID.randomUUID().toString().replace("-", "").substring(0, 8);

    /** Bare logical name — {@link OSIndexAPIImpl} adds the cluster prefix internally. */
    private static final String IDX = "content_" + RUN_ID;

    @Inject
    private ContentletIndexOperationsOS opsOS;

    @Inject
    private OSIndexAPIImpl osIndexAPI;

    @Inject
    private MappingOperationsOS mappingOps;

    @Inject
    private OSClientProvider clientProvider;

    /**
     * Canonical OS physical name (cluster prefix + {@code .os} tag, e.g.
     * {@code cluster_xxx.content_xxxxxxxx.os}). Resolved once per test in {@link #setUp()} via
     * {@link ContentletIndexOperationsOS#toPhysicalName}. Safe to pass to any dotCMS API surface
     * (idempotent on the cluster prefix and the tag) and to the raw OS client (matches the
     * actual cluster index name).
     */
    private String physicalName;

    // =========================================================================
    // Lifecycle
    // =========================================================================

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    @Before
    public void setUp() {
        physicalName = opsOS.toPhysicalName(IDX);
        cleanupTestIndex();
    }

    @After
    public void tearDown() {
        cleanupTestIndex();
    }

    // =========================================================================
    // Tests
    // =========================================================================

    /**
     * Given scenario: {@link ContentletIndexOperationsOS#createContentIndex} is called
     *                 for the physical name of a non-existent index.
     * Expected: The index exists in OpenSearch immediately after the call.
     */
    @Test
    public void test_createContentIndex_shouldCreateIndexInOpenSearch() throws Exception {
        assertFalse("Pre-condition: index must not exist before creation",
                osIndexAPI.indexExists(physicalName));

        opsOS.createContentIndex(physicalName, 1);

        assertTrue("Index must exist in OpenSearch after createContentIndex",
                osIndexAPI.indexExists(physicalName));
        Logger.info(this, "✅ test_createContentIndex_shouldCreateIndexInOpenSearch passed");
    }

    /**
     * Given scenario: {@link ContentletIndexOperationsOS#createContentIndex} is called.
     * Expected: The mapping returned by OpenSearch contains the dotCMS dynamic templates
     *           defined in {@code os-content-mapping.json}:
     *           {@code template_1} ({@code *_dotraw → keyword}),
     *           {@code textmapping} ({@code *_text → text}),
     *           {@code geomapping} ({@code *latlon → geo_point}), and
     *           {@code keywordmapping} (keyword fields by path).
     */
    @Test
    public void test_createContentIndex_shouldApplyDotCMSDynamicTemplates() throws Exception {
        opsOS.createContentIndex(physicalName, 1);

        final String mapping = mappingOps.getMapping(IDX);
        assertNotNull("getMapping must return a non-null string", mapping);
        assertFalse("getMapping must not return empty JSON", mapping.equals("{}"));

        // Dynamic-template names declared in os-content-mapping.json
        assertTrue("Mapping must contain 'template_1' (*_dotraw → keyword)",
                mapping.contains("template_1"));
        assertTrue("Mapping must contain 'textmapping' (*_text → text)",
                mapping.contains("textmapping"));
        assertTrue("Mapping must contain 'geomapping' (*latlon → geo_point)",
                mapping.contains("geomapping"));
        assertTrue("Mapping must contain 'keywordmapping'",
                mapping.contains("keywordmapping"));

        // Field types that confirm the template bodies were stored correctly
        assertTrue("Mapping must declare geo_point type for geo templates",
                mapping.contains("geo_point"));
        assertTrue("Mapping must declare keyword type",
                mapping.contains("keyword"));

        Logger.info(this, "✅ test_createContentIndex_shouldApplyDotCMSDynamicTemplates passed"
                + " – mapping length: " + mapping.length());
    }

    /**
     * Given scenario: An index is created via {@link ContentletIndexOperationsOS#createContentIndex}
     *                 and then a document is indexed with fields that match three dynamic templates
     *                 from {@code os-content-mapping.json}:
     *                 <ul>
     *                   <li>{@code title_dotraw} — matches {@code *_dotraw → keyword}</li>
     *                   <li>{@code body_text}    — matches {@code *_text    → text}</li>
     *                   <li>{@code loc_latlon}   — matches {@code *latlon   → geo_point}</li>
     *                 </ul>
     * Expected: After the index refresh, {@link MappingOperationsOS#getFieldMappingAsMap} reports
     *           the correct type for each field, proving the dynamic templates actually fire.
     */
    @Test
    public void test_dynamicTemplates_shouldResolveCorrectFieldTypes() throws Exception {
        opsOS.createContentIndex(physicalName, 1);

        // Index a document with one field per dynamic template under test
        final String docJson = "{"
                + "\"title_dotraw\":\"hello\","     // *_dotraw → keyword
                + "\"body_text\":\"hello world\","  // *_text   → text
                + "\"loc_latlon\":\"0,0\""          // *latlon  → geo_point (ignore_malformed=true)
                + "}";
        final IndexBulkRequest req = opsOS.createBulkRequest();
        opsOS.addIndexOp(req, physicalName, "dyn-template-doc-" + RUN_ID, docJson);
        opsOS.putToIndex(req);

        // Force a refresh so the dynamic mapping is resolved and stored
        final OpenSearchClient client = clientProvider.getClient();
        client.indices().refresh(RefreshRequest.of(r -> r.index(physicalName)));

        // *_dotraw → keyword
        final Map<String, Object> dotrawMapping = mappingOps.getFieldMappingAsMap(IDX, "title_dotraw");
        assertFalse("title_dotraw field mapping must not be empty", dotrawMapping.isEmpty());
        assertTrue("title_dotraw must be mapped as keyword (*_dotraw template)",
                dotrawMapping.toString().contains("keyword"));

        // *_text → text
        final Map<String, Object> textMapping = mappingOps.getFieldMappingAsMap(IDX, "body_text");
        assertFalse("body_text field mapping must not be empty", textMapping.isEmpty());
        assertTrue("body_text must be mapped as text (*_text template)",
                textMapping.toString().contains("\"type\":\"text\"")
                || textMapping.toString().contains("type=text"));

        // *latlon → geo_point
        final Map<String, Object> geoMapping = mappingOps.getFieldMappingAsMap(IDX, "loc_latlon");
        assertFalse("loc_latlon field mapping must not be empty", geoMapping.isEmpty());
        assertTrue("loc_latlon must be mapped as geo_point (*latlon template)",
                geoMapping.toString().contains("geo_point"));

        Logger.info(this, "✅ test_dynamicTemplates_shouldResolveCorrectFieldTypes passed");
    }

    /**
     * Given scenario: {@link ContentletIndexOperationsOS#createContentIndex} is called.
     * Expected: The index settings contain {@code auto_expand_replicas=0-1} as configured
     *           by {@link OSIndexAPIImpl#createIndex}.
     */
    @Test
    public void test_createContentIndex_shouldHaveAutoExpandReplicasSetting() throws Exception {
        opsOS.createContentIndex(physicalName, 1);

        final OpenSearchClient client = clientProvider.getClient();
        final GetIndexResponse response = client.indices().get(b -> b.index(physicalName));

        final var indexState = response.result().get(physicalName);
        assertNotNull("Index state must be present in GET response", indexState);
        assertNotNull("Index settings must not be null", indexState.settings());
        assertNotNull("Index-level settings block must not be null", indexState.settings().index());
        assertEquals("auto_expand_replicas must be 0-1",
                "0-1",
                indexState.settings().index().autoExpandReplicas());

        Logger.info(this, "✅ test_createContentIndex_shouldHaveAutoExpandReplicasSetting passed");
    }

    /**
     * Given scenario: {@link ContentletIndexOperationsOS#createContentIndex} is called.
     * Expected: The index analysis settings contain the custom analysers
     *           ({@code my_analyzer}, {@code dot_comma_analyzer}) and the custom char filter
     *           ({@code my_char_filter}) declared in {@code os-content-settings.json}.
     */
    @Test
    public void test_createContentIndex_shouldConfigureCustomAnalysers() throws Exception {
        opsOS.createContentIndex(physicalName, 1);

        final OpenSearchClient client = clientProvider.getClient();
        final GetIndexResponse response = client.indices().get(b -> b.index(physicalName));
        final var indexState = response.result().get(physicalName);

        assertNotNull("Index state must be present in GET response", indexState);
        assertNotNull("Index settings must not be null", indexState.settings());
        assertNotNull("Index-level settings block must not be null", indexState.settings().index());
        assertNotNull("Analysis settings must be present on the created index",
                indexState.settings().index().analysis());

        final var analyzers = indexState.settings().index().analysis().analyzer();
        assertFalse("Analyser map must not be empty", analyzers.isEmpty());
        assertTrue("my_analyzer must be defined in index analysis settings",
                analyzers.containsKey("my_analyzer"));
        assertTrue("dot_comma_analyzer must be defined in index analysis settings",
                analyzers.containsKey("dot_comma_analyzer"));

        Logger.info(this, "✅ test_createContentIndex_shouldConfigureCustomAnalysers passed"
                + " – analysers: " + analyzers.keySet());
    }

    /**
     * Tests the idempotence of the {@link ContentletIndexOperationsOS#toPhysicalName} method.
     *
     * Given scenario:
     * - {@link ContentletIndexOperationsOS#toPhysicalName} is called with a logical index name.
     *
     * Expected:
     * - The generated physical name should remain consistent when passed back into the same method.
     * - If the input name has already been converted to a physical name, the method should return it unchanged.
     *
     * This ensures that {@link ContentletIndexOperationsOS#toPhysicalName} behaves in a consistent and idempotent manner.
     *
     * @throws Exception if an unexpected error occurs during the test execution.
     */
    @Test
    public void test_physicalName_Idempotent() throws Exception {
        final String physicalName = opsOS.toPhysicalName(IDX);
        assertEquals("Physical name must be consistent",
                physicalName,
                opsOS.toPhysicalName(physicalName));
    }

    // =========================================================================
    // Helper
    // =========================================================================

    private void cleanupTestIndex() {
        try {
            if (osIndexAPI.indexExists(physicalName)) {
                osIndexAPI.delete(physicalName);
            }
        } catch (Exception e) {
            Logger.warn(this, "Cleanup: error removing OS index '" + physicalName
                    + "': " + e.getMessage());
        }
    }
}
