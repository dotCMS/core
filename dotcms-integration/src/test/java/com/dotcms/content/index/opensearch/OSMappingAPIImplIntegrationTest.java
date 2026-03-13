package com.dotcms.content.index.opensearch;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.DataProviderWeldRunner;
import com.dotcms.IntegrationTestBase;
import com.dotcms.content.elasticsearch.business.ESMappingAPIImpl;
import com.dotcms.content.index.IndexMappingRestOperations;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.util.Logger;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Integration tests for {@link ESMappingAPIImpl} routing through the OpenSearch code path.
 *
 * <p>Tests are constructed via {@link ESMappingAPIImpl#usingOS(IndexMappingRestOperations)},
 * which pins both internal delegates to the CDI-injected {@link MappingOperationsOS} bean.
 * The bean in turn receives {@link OSTestClientProvider}
 * ({@code @Alternative @Priority(1)}) via Weld, so all calls target the
 * {@code opensearch-upgrade} Docker container on {@code http://localhost:9201}.</p>
 *
 * <p>Requires the {@code opensearch-upgrade} Docker container.
 * Registered in {@link com.dotcms.OpenSearchUpgradeSuite}.</p>
 *
 * <p>Run with:
 * <pre>
 *   ./mvnw verify -pl :dotcms-integration \
 *       -Dcoreit.test.skip=false \
 *       -Dopensearch.upgrade.test=true
 * </pre>
 * </p>
 *
 * @author Fabrizzio Araya
 * @see ESMappingAPIImpl
 */
@ApplicationScoped
@RunWith(DataProviderWeldRunner.class)
public class OSMappingAPIImplIntegrationTest extends IntegrationTestBase {

    /**
     * Unique suffix appended to every OS index name created by this suite.
     * Prevents cross-run pollution in a shared OpenSearch node.
     */
    private static final String RUN_ID =
            UUID.randomUUID().toString().replace("-", "").substring(0, 8);

    private static final String IDX_MAPPING = "mapping_" + RUN_ID;

    /**
     * Simple but realistic dotCMS-style mapping JSON used across multiple tests.
     * Contains a keyword field and a text field to verify type fidelity after round-trip.
     */
    private static final String SIMPLE_MAPPING_JSON =
            "{\"properties\":{\"title\":{\"type\":\"keyword\"},\"body\":{\"type\":\"text\"}}}";

    // ── CDI-injected beans ────────────────────────────────────────────────────
    // MappingOperationsOS (@ApplicationScoped) receives OSTestClientProvider
    // (@Alternative @Priority(1)) automatically — no manual wiring needed.
    @Inject
    private MappingOperationsOS osOps;

    // OSIndexAPIImpl is used to create/delete the real OS indices needed by mapping tests.
    @Inject
    private OSIndexAPIImpl osIndexAPI;

    // =========================================================================
    // Lifecycle
    // =========================================================================

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    @Before
    public void setUp() throws Exception {
        cleanupTestOsIndices();
        osIndexAPI.createIndex(IDX_MAPPING, 1);
        Logger.info(this, "setUp: created OS index '" + IDX_MAPPING + "'");
    }

    @After
    public void tearDown() {
        cleanupTestOsIndices();
    }

    // =========================================================================
    // Tests – OS mapping REST operations
    // =========================================================================

    /**
     * Given scenario: A simple JSON mapping is applied to a freshly created OS index via
     *                 {@link ESMappingAPIImpl#putMapping(String, String)}.
     * Expected:
     * <ul>
     *   <li>{@code putMapping} returns {@code true} (acknowledged by OS)</li>
     *   <li>A subsequent {@link ESMappingAPIImpl#getMapping(String)} returns a non-empty
     *       JSON string that mentions the mapped field names</li>
     * </ul>
     */
    @Test
    public void test_putAndGetMapping_shouldRoundTripViaOS() throws Exception {
        final ESMappingAPIImpl api = buildOSMappingAPI();

        final boolean ack = api.putMapping(IDX_MAPPING, SIMPLE_MAPPING_JSON);
        assertTrue("putMapping must be acknowledged by OpenSearch", ack);

        final String mapping = api.getMapping(IDX_MAPPING);
        assertNotNull("getMapping must return a non-null string", mapping);
        assertFalse("getMapping must not return empty JSON", mapping.equals("{}"));
        assertTrue("Mapping JSON must mention the 'title' field",
                mapping.contains("title"));
        assertTrue("Mapping JSON must mention the 'body' field",
                mapping.contains("body"));
        Logger.info(this, "✅ test_putAndGetMapping_shouldRoundTripViaOS passed"
                + " – mapping length: " + mapping.length());
    }

    /**
     * Given scenario: {@link ESMappingAPIImpl#putMapping(List, String)} is called with a
     *                 single-element list.
     * Expected: Returns {@code true} (the list overload delegates correctly to the OS client).
     */
    @Test
    public void test_putMapping_listOverload_shouldAcknowledge() throws Exception {
        final ESMappingAPIImpl api = buildOSMappingAPI();

        final boolean ack = api.putMapping(List.of(IDX_MAPPING), SIMPLE_MAPPING_JSON);
        assertTrue("putMapping(List, String) must be acknowledged by OpenSearch", ack);
        Logger.info(this, "✅ test_putMapping_listOverload_shouldAcknowledge passed");
    }

    /**
     * Given scenario: A mapping is applied to an OS index, then
     *                 {@link ESMappingAPIImpl#getFieldMappingAsMap(String, String)} is called
     *                 for the mapped field.
     * Expected: Returns a non-empty map that contains the field name as a key.
     */
    @Test
    public void test_getFieldMappingAsMap_shouldReturnMappingForKnownField() throws Exception {
        final ESMappingAPIImpl api = buildOSMappingAPI();
        api.putMapping(IDX_MAPPING, SIMPLE_MAPPING_JSON);

        final Map<String, Object> fieldMapping =
                api.getFieldMappingAsMap(IDX_MAPPING, "title");

        assertNotNull("getFieldMappingAsMap must never return null", fieldMapping);
        assertFalse("Field mapping must not be empty for a mapped field",
                fieldMapping.isEmpty());
        Logger.info(this, "✅ test_getFieldMappingAsMap_shouldReturnMappingForKnownField passed"
                + " – keys: " + fieldMapping.keySet());
    }

    /**
     * Given scenario: {@link ESMappingAPIImpl#getFieldMappingAsMap(String, String)} is called
     *                 for a field that was never mapped.
     * Expected: Returns an empty map (no exception, graceful response).
     */
    @Test
    public void test_getFieldMappingAsMap_shouldReturnEmptyMapForUnknownField() throws Exception {
        final ESMappingAPIImpl api = buildOSMappingAPI();

        final Map<String, Object> fieldMapping =
                api.getFieldMappingAsMap(IDX_MAPPING, "nonexistent_field_" + RUN_ID);

        assertNotNull("getFieldMappingAsMap must never return null", fieldMapping);
        assertTrue("getFieldMappingAsMap must return an empty map for an unmapped field",
                fieldMapping.isEmpty());
        Logger.info(this, "✅ test_getFieldMappingAsMap_shouldReturnEmptyMapForUnknownField passed");
    }

    /**
     * Given scenario: The same mapping is applied twice to the same index.
     * Expected: Both calls return {@code true} — applying an identical mapping is idempotent.
     */
    @Test
    public void test_putMapping_idempotent_shouldAcknowledgeBothTimes() throws Exception {
        final ESMappingAPIImpl api = buildOSMappingAPI();

        assertTrue("First putMapping must be acknowledged",
                api.putMapping(IDX_MAPPING, SIMPLE_MAPPING_JSON));
        assertTrue("Second putMapping with the same mapping must also be acknowledged",
                api.putMapping(IDX_MAPPING, SIMPLE_MAPPING_JSON));
        Logger.info(this, "✅ test_putMapping_idempotent_shouldAcknowledgeBothTimes passed");
    }

    // =========================================================================
    // Tests – mirrored from ESMappingAPITest
    // =========================================================================

    /**
     * Given scenario: {@link ESMappingAPIImpl#toJsonString(Map)} is called with a plain map
     *                 using an OS-wired instance.
     * Expected:
     * <ul>
     *   <li>Returns a non-null, non-empty JSON string</li>
     *   <li>The JSON contains the expected key and value</li>
     * </ul>
     * NOTE: {@code toJsonString} is pure Java serialization — it does not call OS REST
     * operations. This test verifies that the OS-wired constructor does not break the
     * Java-side serialization path.
     */
    @Test
    public void test_toJsonString_withOSInstance_shouldProduceValidJson() throws Exception {
        final ESMappingAPIImpl api = buildOSMappingAPI();

        final Map<String, Object> map = Map.of("title", "hello-os", "count", 42);
        final String json = api.toJsonString(map);

        assertNotNull("toJsonString must return a non-null string", json);
        assertFalse("toJsonString must return a non-empty string", json.isEmpty());
        assertTrue("JSON must contain the 'title' key", json.contains("title"));
        assertTrue("JSON must contain the expected title value", json.contains("hello-os"));
        assertTrue("JSON must contain the count value", json.contains("42"));
        Logger.info(this, "✅ test_toJsonString_withOSInstance_shouldProduceValidJson passed"
                + " – json: " + json);
    }

    // =========================================================================
    // Tests – provider accessors exposed for testing
    // =========================================================================

    /**
     * Given scenario: {@link ESMappingAPIImpl#usingOS(IndexMappingRestOperations)} is called
     *                 with a concrete {@link MappingOperationsOS} instance.
     * Expected:
     * <ul>
     *   <li>Both {@code esOps()} and {@code osOps()} return the provided instance</li>
     *   <li>This confirms the explicit OS routing is wired correctly</li>
     * </ul>
     */
    @Test
    public void test_usingOS_factory_shouldWireBothDelegatesToOSOps() {
        final ESMappingAPIImpl api = buildOSMappingAPI();

        assertNotNull("esOps() delegate must not be null", api.esOps());
        assertNotNull("osOps() delegate must not be null", api.osOps());
        assertTrue("Both delegates must point to the same MappingOperationsOS instance",
                api.esOps() == api.osOps());
        Logger.info(this, "✅ test_usingOS_factory_shouldWireBothDelegatesToOSOps passed");
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Builds a {@link ESMappingAPIImpl} that routes all REST mapping operations through
     * the CDI-injected {@link MappingOperationsOS} — which targets the test OS container.
     */
    private ESMappingAPIImpl buildOSMappingAPI() {
        return ESMappingAPIImpl.usingOS(osOps);
    }

    /**
     * Deletes every test-scoped OS index that actually exists in OpenSearch.
     * Skipping absent indices avoids noisy error logs between tests.
     */
    private synchronized void cleanupTestOsIndices() {
        for (final String idx : List.of(IDX_MAPPING)) {
            try {
                if (osIndexAPI.indexExists(idx)) {
                    osIndexAPI.delete(idx);
                }
            } catch (Exception e) {
                Logger.warn(this, "Cleanup: error removing OS index '" + idx
                        + "': " + e.getMessage());
            }
        }
    }
}
