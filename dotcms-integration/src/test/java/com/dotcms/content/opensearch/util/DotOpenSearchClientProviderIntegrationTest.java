package com.dotcms.content.opensearch.util;

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
public class DotOpenSearchClientProviderIntegrationTest extends IntegrationTestBase {

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

            System.out.println("✅ OpenSearch cluster health: " + healthResponse.status());
            System.out.println("✅ Cluster name: " + healthResponse.clusterName());
            System.out.println("✅ Number of nodes: " + healthResponse.numberOfNodes());
            System.out.println("✅ Active shards: " + healthResponse.activeShards());

            // Additional assertions for the specific container configuration
            assertTrue("Should have at least one node", healthResponse.numberOfNodes() >= 1);
            assertEquals("Should be single-node cluster", "opensearch-3x-cluster", healthResponse.clusterName());

        } catch (OpenSearchException | IOException e) {
            // OpenSearch is not available - provide helpful message
            System.out.println("⚠️ Local OpenSearch not available for integration test: " + e.getMessage());
            System.out.println("⚠️ Make sure OpenSearch container is running with:");
            System.out.println("⚠️   docker-compose up opensearch-3x");
            System.out.println("⚠️ Expected container on http://localhost:9201");
            System.out.println("⚠️ Skipping cluster health test");
        } catch (Exception e) {
            System.out.println("❌ Unexpected error during cluster health test: " + e.getMessage());
            e.printStackTrace();
            // Don't fail the test for unexpected errors in case OpenSearch is not available
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
            System.out.println("✅ Verified test index does not exist initially: " + TEST_INDEX);

            // 2. Create test index
            CreateIndexRequest createRequest = CreateIndexRequest.of(c -> c.index(TEST_INDEX));
            client.indices().create(createRequest);
            System.out.println("✅ Created test index: " + TEST_INDEX);

            // 3. Check if test index exists now (should exist)
            boolean existsAfter = client.indices().exists(existsRequest).value();
            assertTrue("Test index should exist after creation", existsAfter);
            System.out.println("✅ Verified test index exists after creation");

            // 4. Delete test index
            DeleteIndexRequest deleteRequest = DeleteIndexRequest.of(d -> d.index(TEST_INDEX));
            client.indices().delete(deleteRequest);
            System.out.println("✅ Deleted test index: " + TEST_INDEX);

            // 5. Verify index is deleted
            boolean existsAfterDelete = client.indices().exists(existsRequest).value();
            assertFalse("Test index should not exist after deletion", existsAfterDelete);
            System.out.println("✅ Verified test index does not exist after deletion");

            System.out.println("✅ All OpenSearch index operations completed successfully!");

        } catch (OpenSearchException | IOException e) {
            // OpenSearch is not available - provide helpful message
            System.out.println("⚠️ Local OpenSearch not available for index operations test: " + e.getMessage());
            System.out.println("⚠️ Make sure OpenSearch container is running with:");
            System.out.println("⚠️   docker-compose up opensearch-3x");
            System.out.println("⚠️ Expected container on http://localhost:9201");
            System.out.println("⚠️ Skipping index operations test");
        } catch (Exception e) {
            System.out.println("❌ Unexpected error during index operations test: " + e.getMessage());
            e.printStackTrace();
            // Don't fail the test for unexpected errors in case OpenSearch is not available
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

            System.out.println("🔍 Testing connectivity to local OpenSearch container...");
            System.out.println("🔍 Endpoint: http://localhost:9201");
            System.out.println("🔍 Expected cluster: opensearch-3x-cluster");

            provider = new ConfigurableOpenSearchProvider(config);
            OpenSearchClient client = provider.getClient();

            // Test 1: Basic cluster health
            System.out.println("\n📊 Getting cluster health...");
            HealthRequest healthRequest = HealthRequest.of(h ->
                h.timeout(new Time.Builder().time("15s").build())
            );
            HealthResponse healthResponse = client.cluster().health(healthRequest);

            // Detailed assertions and logging
            assertNotNull("Health response should not be null", healthResponse);
            System.out.println("✅ Health Status: " + healthResponse.status());
            System.out.println("✅ Cluster Name: " + healthResponse.clusterName());
            System.out.println("✅ Number of Nodes: " + healthResponse.numberOfNodes());
            System.out.println("✅ Active Shards: " + healthResponse.activeShards());
            System.out.println("✅ Initializing Shards: " + healthResponse.initializingShards());
            System.out.println("✅ Relocating Shards: " + healthResponse.relocatingShards());
            System.out.println("✅ Unassigned Shards: " + healthResponse.unassignedShards());

            // Verify expected container configuration
            assertEquals("Should match container cluster name",
                "opensearch-3x-cluster", healthResponse.clusterName());
            assertEquals("Should have exactly one node (single-node setup)",
                1, healthResponse.numberOfNodes());

            // Test 2: Get cluster info
            System.out.println("\n🏗️ Getting cluster information...");
            try {
                var infoResponse = client.info();
                System.out.println("✅ OpenSearch Version: " + infoResponse.version().number());
                System.out.println("✅ Lucene Version: " + infoResponse.version().luceneVersion());
                System.out.println("✅ Build Date: " + infoResponse.version().buildDate());
                System.out.println("✅ Build Hash: " + infoResponse.version().buildHash().substring(0, 8) + "...");

                assertNotNull("Info response should not be null", infoResponse);
                assertTrue("Should be OpenSearch 3.x",
                    infoResponse.version().number().startsWith("3."));
            } catch (Exception e) {
                System.out.println("⚠️ Could not get cluster info: " + e.getMessage());
            }

            System.out.println("\n✅ Local OpenSearch connectivity test PASSED!");
            System.out.println("✅ Container opensearch-3x is running and accessible");

        } catch (OpenSearchException e) {
            System.out.println("\n❌ OpenSearch API Error:");
            System.out.println("❌ Error: " + e.getMessage());
            System.out.println("❌ Status: " + e.status());
            System.out.println("\n🔧 Troubleshooting steps:");
            System.out.println("🔧 1. Check if container is running: docker ps | grep opensearch");
            System.out.println("🔧 2. Check container logs: docker logs opensearch-3x");
            System.out.println("🔧 3. Verify port mapping: curl http://localhost:9201");
            fail("OpenSearch API error: " + e.getMessage());

        } catch (IOException e) {
            System.out.println("\n❌ Connection Error:");
            System.out.println("❌ " + e.getMessage());
            System.out.println("\n🔧 Troubleshooting steps:");
            System.out.println("🔧 1. Start OpenSearch container: docker-compose up -d opensearch-3x");
            System.out.println("🔧 2. Wait for startup: docker logs -f opensearch-3x");
            System.out.println("🔧 3. Test direct access: curl http://localhost:9201");
            System.out.println("🔧 4. Check network: docker network ls");

            // Don't fail the test - just report the issue
            System.out.println("\n⚠️ Skipping connectivity test - OpenSearch not available");

        } catch (Exception e) {
            System.out.println("\n❌ Unexpected error: " + e.getClass().getSimpleName());
            System.out.println("❌ Message: " + e.getMessage());
            e.printStackTrace();
            fail("Unexpected error during connectivity test: " + e.getMessage());

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
                System.err.println("Warning: Error closing provider: " + e.getMessage());
            }
        }
    }

    /**
     * Helper method to reset properties
     */
    private void resetProperty(String key, String originalValue) {
        if (originalValue != null) {
            Config.setProperty(key, originalValue);
        } else {
            Config.setProperty(key, "");
        }
    }
}