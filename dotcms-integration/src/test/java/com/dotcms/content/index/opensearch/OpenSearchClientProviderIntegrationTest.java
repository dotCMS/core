package com.dotcms.content.index.opensearch;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.dotcms.DataProviderWeldRunner;
import com.dotcms.IntegrationTestBase;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import java.io.IOException;
import java.time.Duration;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch._types.Time;
import org.opensearch.client.opensearch.cluster.HealthRequest;
import org.opensearch.client.opensearch.cluster.HealthResponse;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.DeleteIndexRequest;
import org.opensearch.client.opensearch.indices.ExistsRequest;

/**
 * Integration test for ConfigurableOpenSearchProvider.
 * Tests the provider's ability to create and configure OpenSearch clients.
 *
 * @author fabrizio
 */
@ApplicationScoped
@RunWith(DataProviderWeldRunner.class)
public class OpenSearchClientProviderIntegrationTest extends IntegrationTestBase {

    private static final String TEST_INDEX = "test-dotcms-opensearch-" + System.currentTimeMillis();

    @Inject
    OpenSearchDefaultClientProvider singleton;

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();

    }

    /**
     * Test direct provider with custom configuration
     * This demonstrates how tests should use ConfigurableOpenSearchProvider directly for custom configs
     */
    @Test
    public void test_directProvider_withCustomConfiguration_shouldWork() {
        ConfigurableOpenSearchProvider provider = null;
        try {
            // Arrange - Create test configuration
            OpenSearchClientConfig testConfig = OpenSearchClientConfig.builder()
                    .addEndpoints("http://localhost:9201")  // Local OpenSearch port
                    .tlsEnabled(false)
                    .maxConnections(200) // Different configuration for testing
                    .maxConnectionsPerRoute(100)
                    .build();

            // Act - Create provider with custom configuration
            provider = new ConfigurableOpenSearchProvider(testConfig);
            OpenSearchClient client = provider.getClient();

            // Assert
            assertNotNull("Provider should not be null", provider);
            assertNotNull("Client should not be null", client);

            // Verify the singleton still works independently
            OpenSearchClient singletonClient = singleton.getClient();
            assertNotNull("Singleton client should not be null", singletonClient);

            // They should be different instances (custom vs default config)
            assertTrue("Should be different client instances", client != singletonClient);

        } finally {
            closeProvider(provider);
        }
    }

    /**
     * Test direct provider with convenience configurations
     * Shows how to use the convenience configuration methods with ConfigurableOpenSearchProvider
     */
    @Test
    public void test_directProvider_withConvenienceConfigurations_shouldWork() {
        ConfigurableOpenSearchProvider localProvider = null;
        ConfigurableOpenSearchProvider prodProvider = null;
        try {
            // Test 1: Local test configuration
            OpenSearchClientConfig localConfig = OpenSearchDefaultClientProvider.createLocalTestConfig();
            localProvider = new ConfigurableOpenSearchProvider(localConfig);

            assertEquals("Should use local port", "http://localhost:9201", localConfig.endpoints().get(0));
            assertFalse("Should have TLS disabled", localConfig.tlsEnabled());

            // Test client creation
            OpenSearchClient localClient = localProvider.getClient();
            assertNotNull("Local test client should not be null", localClient);

            // Test 2: Production-like test configuration
            OpenSearchClientConfig prodConfig = OpenSearchDefaultClientProvider.createProductionTestConfig();
            prodProvider = new ConfigurableOpenSearchProvider(prodConfig);

            assertEquals("Should use production endpoint", "https://opensearch.prod.com:9200", prodConfig.endpoints().get(0));
            assertTrue("Should have TLS enabled", prodConfig.tlsEnabled());
            assertTrue("Should have username", prodConfig.username().isPresent());

            // Test client creation
            OpenSearchClient prodClient = prodProvider.getClient();
            assertNotNull("Production test client should not be null", prodClient);

            // Verify they are different instances
            assertTrue("Should be different client instances", localClient != prodClient);

        } finally {
            // Cleanup
            closeProvider(localProvider);
            closeProvider(prodProvider);
        }
    }

    /**
     * Test configuration from properties
     * This tests the properties loading mechanism using local OpenSearch
     */
    @Test
    public void test_loadConfiguration_fromProperties_shouldCreateValidConfig() {
        // Arrange - Set some test properties for local OpenSearch
        String originalEndpoints = Config.getStringProperty("OS_ENDPOINTS", null);
        String originalTlsEnabled = Config.getStringProperty("OS_TLS_ENABLED", null);

        try {
            // Set test properties for local OpenSearch (no auth, security disabled)
            Config.setProperty("OS_ENDPOINTS", "http://localhost:9201");
            Config.setProperty("OS_TLS_ENABLED", false);

            // Act
            ConfigurableOpenSearchProvider provider = new ConfigurableOpenSearchProvider();
            OpenSearchClient client = provider.getClient();

            // Assert
            assertNotNull("Provider should not be null", provider);
            assertNotNull("Client should not be null", client);

            // Cleanup
            closeProvider(provider);

        } finally {
            // Reset properties
            resetProperty("OS_ENDPOINTS", originalEndpoints);
            resetProperty("OS_TLS_ENABLED", originalTlsEnabled);
        }
    }

    /**
     * Test client functionality with cluster health check
     * This test connects to the local OpenSearch instance on port 9201
     */
    @Test
    public void test_clientFunctionality_clusterHealth_shouldReturnValidResponse() {
        // This test connects to local OpenSearch (opensearch-3x container on port 9201)
        ConfigurableOpenSearchProvider provider = null;
        try {
            // Arrange - Configure for local OpenSearch container
            OpenSearchClientConfig config = OpenSearchClientConfig.builder()
                    .addEndpoints("http://localhost:9201")  // Local OpenSearch port
                    .tlsEnabled(false)  // Security disabled
                    .trustSelfSigned(true)
                    .connectionTimeout(Duration.ofSeconds(10))  // Increased timeout
                    .socketTimeout(Duration.ofSeconds(10))
                    .build();

            provider = new ConfigurableOpenSearchProvider(config);
            OpenSearchClient client = provider.getClient();

            // Act - Try to get cluster health

            HealthResponse healthResponse = client.cluster().health();

            // Assert
            assertNotNull("Health response should not be null", healthResponse);
            assertNotNull("Cluster name should not be null", healthResponse.clusterName());

            Logger.info(this, "✅ OpenSearch cluster health: " + healthResponse.status());
            Logger.info(this, "✅ Cluster name: " + healthResponse.clusterName());
            Logger.info(this, "✅ Number of nodes: " + healthResponse.numberOfNodes());
            Logger.info(this, "✅ Active shards: " + healthResponse.activeShards());

            // Additional assertions for the specific container configuration
            assertTrue("Should have at least one node", healthResponse.numberOfNodes() >= 1);
            assertEquals("Should be single-node cluster", "opensearch-cluster", healthResponse.clusterName());

            // Verify OpenSearch 3.x version
            var infoResponse = client.info();
            assertNotNull("Info response should not be null", infoResponse);
            String version = infoResponse.version().number();
            assertTrue("Expected OpenSearch 3.x but got version: " + version,
                    version.startsWith("3."));
            Logger.info(this, "✅ Confirmed OpenSearch 3.x version: " + version);

        } catch (OpenSearchException e) {
            Logger.error(this, "❌ OpenSearch API error during cluster health test: " + e.getMessage());
            Logger.error(this, "❌ Status: " + e.status());
            fail("OpenSearch cluster health test failed. Make sure OpenSearch container is running on http://localhost:9201. Error: " + e.getMessage());
        } catch (IOException e) {
            Logger.error(this, "❌ Connection error during cluster health test: " + e.getMessage());
            Logger.error(this, "❌ Make sure OpenSearch container is running with: docker-compose up opensearch");
            fail("Cannot connect to OpenSearch on http://localhost:9201. Error: " + e.getMessage());
        } catch (Exception e) {
            Logger.error(this, "❌ Unexpected error during cluster health test: " + e.getMessage(), e);
            fail("Unexpected error during cluster health test: " + e.getMessage());
        } finally {
            closeProvider(provider);
        }
    }

    /**
     * Test index operations using local OpenSearch
     */
    @Test
    public void test_clientFunctionality_indexOperations_shouldWorkCorrectly() {
        ConfigurableOpenSearchProvider provider = null;
        try {
            // Arrange - Configure for local OpenSearch container
            OpenSearchClientConfig config = OpenSearchClientConfig.builder()
                    .addEndpoints("http://localhost:9201")  // Local OpenSearch port
                    .tlsEnabled(false)  // Security disabled
                    .trustSelfSigned(true)
                    .connectionTimeout(Duration.ofSeconds(10))
                    .socketTimeout(Duration.ofSeconds(10))
                    .build();

            provider = new ConfigurableOpenSearchProvider(config);
            OpenSearchClient client = provider.getClient();

            // Act & Assert - Try basic index operations

            // 1. Check if test index exists (should not)
            ExistsRequest existsRequest = ExistsRequest.of(e -> e.index(TEST_INDEX));
            boolean existsBefore = client.indices().exists(existsRequest).value();
            assertFalse("Test index should not exist initially", existsBefore);
            Logger.info(this, "✅ Verified test index does not exist initially: " + TEST_INDEX);

            // 2. Create test index
            CreateIndexRequest createRequest = CreateIndexRequest.of(c -> c.index(TEST_INDEX));
            client.indices().create(createRequest);
            Logger.info(this, "✅ Created test index: " + TEST_INDEX);

            // 3. Check if test index exists now (should exist)
            boolean existsAfter = client.indices().exists(existsRequest).value();
            assertTrue("Test index should exist after creation", existsAfter);
            Logger.info(this, "✅ Verified test index exists after creation");

            // 4. Delete test index
            DeleteIndexRequest deleteRequest = DeleteIndexRequest.of(d -> d.index(TEST_INDEX));
            client.indices().delete(deleteRequest);
            Logger.info(this, "✅ Deleted test index: " + TEST_INDEX);

            // 5. Verify index is deleted
            boolean existsAfterDelete = client.indices().exists(existsRequest).value();
            assertFalse("Test index should not exist after deletion", existsAfterDelete);
            Logger.info(this, "✅ Verified test index does not exist after deletion");

            // Verify OpenSearch 3.x version
            var infoResponse = client.info();
            assertNotNull("Info response should not be null", infoResponse);
            String version = infoResponse.version().number();
            assertTrue("Expected OpenSearch 3.x but got version: " + version,
                    version.startsWith("3."));
            Logger.info(this, "✅ Confirmed OpenSearch 3.x version: " + version);

            Logger.info(this, "✅ All OpenSearch index operations completed successfully!");

        } catch (OpenSearchException e) {
            Logger.error(this, "❌ OpenSearch API error during index operations test: " + e.getMessage());
            Logger.error(this, "❌ Status: " + e.status());
            fail("OpenSearch index operations test failed. Make sure OpenSearch container is running on http://localhost:9201. Error: " + e.getMessage());
        } catch (IOException e) {
            Logger.error(this, "❌ Connection error during index operations test: " + e.getMessage());
            Logger.error(this, "❌ Make sure OpenSearch container is running with: docker-compose up opensearch");
            fail("Cannot connect to OpenSearch on http://localhost:9201. Error: " + e.getMessage());
        } catch (Exception e) {
            Logger.error(this, "❌ Unexpected error during index operations test: " + e.getMessage(), e);
            fail("Unexpected error during index operations test: " + e.getMessage());
        } finally {
            closeProvider(provider);
        }
    }

    /**
     * Comprehensive connectivity test for local OpenSearch container
     * This test provides detailed information about the OpenSearch cluster
     */
    @Test
    public void test_localOpenSearchConnectivity_shouldProvideDetailedInfo() {
        ConfigurableOpenSearchProvider provider = null;
        try {
            // Arrange - Configure specifically for the opensearch-3x container
            OpenSearchClientConfig config = OpenSearchClientConfig.builder()
                    .addEndpoints("http://localhost:9201")  // Container mapped port
                    .tlsEnabled(false)  // DISABLE_SECURITY_PLUGIN=true
                    .connectionTimeout(Duration.ofSeconds(15))
                    .socketTimeout(Duration.ofSeconds(15))
                    .maxConnections(10)
                    .maxConnectionsPerRoute(5)
                    .build();

            Logger.info(this, "🔍 Testing connectivity to local OpenSearch container...");
            Logger.info(this, "🔍 Endpoint: http://localhost:9201");
            Logger.info(this, "🔍 Expected cluster: opensearch-cluster");

            provider = new ConfigurableOpenSearchProvider(config);
            OpenSearchClient client = provider.getClient();

            // Test 1: Basic cluster health
            Logger.info(this, "\n📊 Getting cluster health...");
            HealthRequest healthRequest = HealthRequest.of(h ->
                    h.timeout(new Time.Builder().time("15s").build())
            );
            HealthResponse healthResponse = client.cluster().health(healthRequest);

            // Detailed assertions and logging
            assertNotNull("Health response should not be null", healthResponse);
            Logger.info(this, "✅ Health Status: " + healthResponse.status());
            Logger.info(this, "✅ Cluster Name: " + healthResponse.clusterName());
            Logger.info(this, "✅ Number of Nodes: " + healthResponse.numberOfNodes());
            Logger.info(this, "✅ Active Shards: " + healthResponse.activeShards());
            Logger.info(this, "✅ Initializing Shards: " + healthResponse.initializingShards());
            Logger.info(this, "✅ Relocating Shards: " + healthResponse.relocatingShards());
            Logger.info(this, "✅ Unassigned Shards: " + healthResponse.unassignedShards());

            // Verify expected container configuration
            assertEquals("Should match container cluster name",
                    "opensearch-cluster", healthResponse.clusterName());
            assertEquals("Should have exactly one node (single-node setup)",
                    1, healthResponse.numberOfNodes());

            // Test 2: Get cluster info and verify OpenSearch 3.x version
            Logger.info(this, "\n🏗️ Getting cluster information and verifying OpenSearch 3.x version...");
            var infoResponse = client.info();
            Logger.info(this, "✅ OpenSearch Version: " + infoResponse.version().number());
            Logger.info(this, "✅ Lucene Version: " + infoResponse.version().luceneVersion());
            Logger.info(this, "✅ Build Date: " + infoResponse.version().buildDate());
            Logger.info(this, "✅ Build Hash: " + infoResponse.version().buildHash().substring(0, 8) + "...");

            // Critical assertions - these MUST pass
            assertNotNull("Info response should not be null", infoResponse);
            assertNotNull("Version should not be null", infoResponse.version());
            assertNotNull("Version number should not be null", infoResponse.version().number());

            String version = infoResponse.version().number();
            assertTrue("Expected OpenSearch 3.x but got version: " + version + ". Make sure opensearch-3x container is running.",
                    version.startsWith("3."));
            Logger.info(this, "✅ Confirmed OpenSearch 3.x version: " + version);

            Logger.info(this, "\n✅ Local OpenSearch connectivity test PASSED!");
            Logger.info(this, "✅ Container opensearch is running and accessible");

        } catch (OpenSearchException e) {
            Logger.error(this, "\n❌ OpenSearch API Error:");
            Logger.error(this, "❌ Error: " + e.getMessage());
            Logger.error(this, "❌ Status: " + e.status());
            Logger.error(this, "\n🔧 Troubleshooting steps:");
            Logger.error(this, "🔧 1. Check if container is running: docker ps | grep opensearch");
            Logger.error(this, "🔧 2. Check container logs: docker logs opensearch");
            Logger.error(this, "🔧 3. Verify port mapping: curl http://localhost:9201");
            fail("OpenSearch API error: " + e.getMessage());

        } catch (IOException e) {
            Logger.error(this, "\n❌ Connection Error:");
            Logger.error(this, "❌ " + e.getMessage());
            Logger.error(this, "\n🔧 Troubleshooting steps:");
            Logger.error(this, "🔧 1. Start OpenSearch container: docker-compose up -d opensearch");
            Logger.error(this, "🔧 2. Wait for startup: docker logs -f opensearch");
            Logger.error(this, "🔧 3. Test direct access: curl http://localhost:9201");
            Logger.error(this, "🔧 4. Check network: docker network ls");
            fail("Cannot connect to OpenSearch on http://localhost:9201. Connection error: " + e.getMessage());

        } catch (Exception e) {
            Logger.error(this, "\n❌ Unexpected error: " + e.getClass().getSimpleName());
            Logger.error(this, "❌ Message: " + e.getMessage(), e);
            fail("Unexpected error during connectivity test: " + e.getMessage());

        } finally {
            closeProvider(provider);
        }
    }

    /**
     * Test OpenSearch 3.x version validation using Maven POM properties
     * This test reads the endpoint from DOT_ES_ENDPOINTS system property and validates the version
     *
     * Given Scenario: Integration tests with DOT_ES_ENDPOINTS property from Maven POM
     * Expected: Should connect to OpenSearch 3.x instance and return version starting with "3."
     */
    @Test
    public void test_opensearch3x_versionValidation_fromMavenProperties() {
        ConfigurableOpenSearchProvider provider = null;
        try {
            // Given Scenario: Read OpenSearch 3.x endpoint from Maven POM system property
            String opensearchUpgradeEndpoint = Config.getStringProperty("DOT_ES_ENDPOINTS_UPGRADE", "");

            Logger.info(this, "🔍 Testing OpenSearch 3.x version validation from Maven properties");
            Logger.info(this, "🔍 DOT_ES_ENDPOINTS_UPGRADE property: " + opensearchUpgradeEndpoint);

            // Verify the property is correctly set from Maven
            assertNotNull("DOT_ES_ENDPOINTS_UPGRADE should not be null", opensearchUpgradeEndpoint);
            assertTrue("DOT_ES_ENDPOINTS_UPGRADE should contain localhost:9201",
                      opensearchUpgradeEndpoint.contains("localhost:9201"));

            // Create configuration using the Maven property
            OpenSearchClientConfig config = OpenSearchClientConfig.builder()
                    .addEndpoints(opensearchUpgradeEndpoint)  // Use Maven property value
                    .tlsEnabled(false)
                    .connectionTimeout(Duration.ofSeconds(15))
                    .socketTimeout(Duration.ofSeconds(15))
                    .build();

            provider = new ConfigurableOpenSearchProvider(config);
            OpenSearchClient client = provider.getClient();

            // Act - Make a request to extract OpenSearch version info
            Logger.info(this, "📡 Making request to OpenSearch 3.x to extract version info...");
            var infoResponse = client.info();

            // Assert - Validate the response and version
            assertNotNull("Info response should not be null", infoResponse);
            assertNotNull("Version should not be null", infoResponse.version());
            assertNotNull("Version number should not be null", infoResponse.version().number());

            String version = infoResponse.version().number();
            String buildHash = infoResponse.version().buildHash();
            String buildDate = infoResponse.version().buildDate();
            String luceneVersion = infoResponse.version().luceneVersion();

            // Expected: Version should be 3.x
            assertTrue("Expected OpenSearch 3.x but got version: " + version +
                      ". Check that opensearch-3x container is running with correct image version.",
                      version.startsWith("3."));

            // Log detailed version information
            Logger.info(this, "✅ OpenSearch Version Validation Results:");
            Logger.info(this, "✅ - Version: " + version);
            Logger.info(this, "✅ - Build Hash: " + buildHash.substring(0, Math.min(12, buildHash.length())) + "...");
            Logger.info(this, "✅ - Build Date: " + buildDate);
            Logger.info(this, "✅ - Lucene Version: " + luceneVersion);
            Logger.info(this, "✅ - Endpoint Used: " + opensearchUpgradeEndpoint);
            Logger.info(this, "✅ - Maven Property: DOT_ES_ENDPOINTS_UPGRADE");

            // Additional cluster verification
            var healthResponse = client.cluster().health();
            assertEquals("Should connect to opensearch-cluster",
                        "opensearch-cluster", healthResponse.clusterName());

            Logger.info(this, "✅ Cluster Name Verified: " + healthResponse.clusterName());
            Logger.info(this, "✅ OpenSearch 3.x version validation PASSED!");

        } catch (OpenSearchException e) {
            Logger.error(this, "❌ OpenSearch API error during version validation: " + e.getMessage());
            Logger.error(this, "❌ Status: " + e.status());
            Logger.error(this, "❌ Make sure opensearch container is running with OpenSearch 3.0.0 image");
            fail("OpenSearch 3.x version validation failed. API error: " + e.getMessage());
        } catch (IOException e) {
            Logger.error(this, "❌ Connection error during version validation: " + e.getMessage());
            Logger.error(this, "❌ Check that opensearch service is running on port 9201");
            fail("Cannot connect to OpenSearch 3.x for version validation. Connection error: " + e.getMessage());
        } catch (Exception e) {
            Logger.error(this, "❌ Unexpected error during version validation: " + e.getMessage(), e);
            fail("Unexpected error during OpenSearch 3.x version validation: " + e.getMessage());
        } finally {
            closeProvider(provider);
        }
    }

    /**
     * Test provider close functionality
     */
    @Test
    public void test_closeProvider_shouldNotThrowException() {
        // Arrange
        ConfigurableOpenSearchProvider provider = new ConfigurableOpenSearchProvider();

        // Act & Assert - Should not throw exception
        try {
            provider.close();
        } catch (Exception e) {
            fail("Close should not throw exception: " + e.getMessage());
        }
    }

    /**
     * Test invalid configuration scenarios
     */
    @Test(expected = DotRuntimeException.class)
    public void test_createProvider_withInvalidEndpoint_shouldThrowException() {
        // Arrange
        OpenSearchClientConfig config = OpenSearchClientConfig.builder()
                .addEndpoints("invalid-url-format")
                .build();

        // Act & Assert - Should throw DotRuntimeException due to invalid URL
        new ConfigurableOpenSearchProvider(config);
    }

    /**
     * Helper method to close provider safely
     */
    private void closeProvider(ConfigurableOpenSearchProvider provider) {
        if (provider != null) {
            try {
                provider.close();
            } catch (IOException e) {
                Logger.warn(this, "Warning: Error closing provider: " + e.getMessage());
            }
        }
    }

    /**
     * Helper method to reset properties
     */
    private void resetProperty(String key, String originalValue) {
         Config.setProperty(key, originalValue);
    }
}
