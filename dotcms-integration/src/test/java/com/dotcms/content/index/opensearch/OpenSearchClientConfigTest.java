package com.dotcms.content.index.opensearch;

import com.dotcms.content.index.opensearch.ImmutableOpenSearchClientConfig.Builder;
import org.junit.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

/**
 * Unit test for OpenSearchClientConfig.
 * Tests the configuration builder pattern and validation logic.
 *
 * @author fabrizio
 */
public class OpenSearchClientConfigTest {

    /**
     * Given Scenario: Creating config with minimal settings (just endpoint)
     * Expected: Config should be created successfully with default values
     */
    @Test
    public void test_createConfig_withMinimalSettings_shouldSucceed() {
        // Act
        OpenSearchClientConfig config = OpenSearchClientConfig.builder()
            .addEndpoints("http://localhost:9200")
            .build();

        // Assert
        assertNotNull("Config should not be null", config);
        assertEquals("Should have one endpoint", 1, config.endpoints().size());
        assertEquals("Endpoint should match", "http://localhost:9200", config.endpoints().get(0));
        assertFalse("Username should be empty", config.username().isPresent());
        assertFalse("Password should be empty", config.password().isPresent());
        assertFalse("JWT token should be empty", config.jwtToken().isPresent());
        assertFalse("TLS should be disabled by default", config.tlsEnabled());
        assertFalse("Trust self-signed should be false by default", config.trustSelfSigned());
    }

    /**
     * Given Scenario: Configuring all three authentication methods simultaneously (basic, JWT, and certificate)
     * Expected: Should throw IllegalArgumentException with specific message about conflicting auth methods
     */
    @Test
    public void test_createConfig_withAllAuthMethods_shouldThrowException() {
        // Act & Assert - Should throw IllegalArgumentException because multiple auth methods are configured
        try {
            OpenSearchClientConfig.builder()
                .endpoints(Arrays.asList("https://node1:9200", "https://node2:9200"))
                .jwtToken("fake.token.here")
                .username("admin")
                .password("password123")
                .clientCertPath("/path/to/cert.pem")
                .clientKeyPath("/path/to/key.pem")
                .caCertPath("/path/to/ca.pem")
                .tlsEnabled(true)
                .trustSelfSigned(false)
                .connectionTimeout(Duration.ofSeconds(15))
                .socketTimeout(Duration.ofSeconds(45))
                .maxConnections(200)
                .maxConnectionsPerRoute(100)
                .build();

            fail("Should have thrown IllegalArgumentException when multiple authentication methods are configured");
        } catch (IllegalArgumentException e) {
            // Expected exception
            assertTrue("Exception message should mention authentication methods",
                e.getMessage().contains("Only one authentication method should be configured"));
        }
    }

    /**
     * Given Scenario: Configuring with basic authentication (username and password)
     * Expected: Config should be created with only basic auth fields populated
     */
    @Test
    public void test_createConfig_withBasicAuth_shouldSucceed() {
        // Act
        OpenSearchClientConfig config = OpenSearchClientConfig.builder()
            .addEndpoints("https://localhost:9200")
            .username("user")
            .password("pass")
            .tlsEnabled(true)
            .build();

        // Assert
        assertTrue("Username should be present", config.username().isPresent());
        assertTrue("Password should be present", config.password().isPresent());
        assertEquals("Username should match", "user", config.username().get());
        assertEquals("Password should match", "pass", config.password().get());
        assertFalse("JWT token should not be present", config.jwtToken().isPresent());
        assertFalse("Client cert should not be present", config.clientCertPath().isPresent());
    }

    /**
     * Given Scenario: Configuring with JWT token authentication
     * Expected: Config should be created with only JWT token populated
     */
    @Test
    public void test_createConfig_withJWTAuth_shouldSucceed() {
        // Act
        OpenSearchClientConfig config = OpenSearchClientConfig.builder()
            .addEndpoints("https://localhost:9200")
            .jwtToken("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token")
            .tlsEnabled(true)
            .build();

        // Assert
        assertTrue("JWT token should be present", config.jwtToken().isPresent());
        assertEquals("JWT token should match", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token", config.jwtToken().get());
        assertFalse("Username should not be present", config.username().isPresent());
        assertFalse("Password should not be present", config.password().isPresent());
        assertFalse("Client cert should not be present", config.clientCertPath().isPresent());
    }

    /**
     * Given Scenario: Configuring with certificate authentication (client cert, key, and CA)
     * Expected: Config should be created with only certificate fields populated
     */
    @Test
    public void test_createConfig_withCertAuth_shouldSucceed() {
        // Act
        OpenSearchClientConfig config = OpenSearchClientConfig.builder()
            .addEndpoints("https://localhost:9200")
            .clientCertPath("/path/to/client.pem")
            .clientKeyPath("/path/to/client.key")
            .caCertPath("/path/to/ca.pem")
            .tlsEnabled(true)
            .build();

        // Assert
        assertTrue("Client cert path should be present", config.clientCertPath().isPresent());
        assertTrue("Client key path should be present", config.clientKeyPath().isPresent());
        assertTrue("CA cert path should be present", config.caCertPath().isPresent());
        assertEquals("Client cert path should match", "/path/to/client.pem", config.clientCertPath().get());
        assertEquals("Client key path should match", "/path/to/client.key", config.clientKeyPath().get());
        assertEquals("CA cert path should match", "/path/to/ca.pem", config.caCertPath().get());
        assertFalse("Username should not be present", config.username().isPresent());
        assertFalse("JWT token should not be present", config.jwtToken().isPresent());
    }

    /**
     * Given Scenario: Creating config with minimal settings to test defaults
     * Expected: All default values should be correctly set (timeouts, connections, TLS settings)
     */
    @Test
    public void test_createConfig_withDefaults_shouldHaveCorrectDefaults() {
        // Act
        OpenSearchClientConfig config = OpenSearchClientConfig.builder()
            .addEndpoints("http://localhost:9200")
            .build();

        // Assert - Test default values
        assertFalse("TLS should be disabled by default", config.tlsEnabled());
        assertFalse("Trust self-signed should be false by default", config.trustSelfSigned());
        assertEquals("Default connection timeout should be 10 seconds",
            Duration.ofSeconds(10), config.connectionTimeout());
        assertEquals("Default socket timeout should be 30 seconds",
            Duration.ofSeconds(30), config.socketTimeout());
        assertEquals("Default max connections should be 100", 100, config.maxConnections());
        assertEquals("Default max connections per route should be 50", 50, config.maxConnectionsPerRoute());
    }

    /**
     * Given Scenario: Creating config with empty endpoints list
     * Expected: Should throw IllegalArgumentException
     */
    @Test(expected = IllegalArgumentException.class)
    public void test_createConfig_withEmptyEndpoints_shouldThrowException() {
        // Act & Assert
        OpenSearchClientConfig.builder()
            .endpoints(Collections.emptyList())
            .build();
    }

    /**
     * Given Scenario: Creating config with one empty endpoint in the list
     * Expected: Should throw IllegalArgumentException
     */
    @Test(expected = IllegalArgumentException.class)
    public void test_createConfig_withEmptyEndpoint_shouldThrowException() {
        // Act & Assert
        OpenSearchClientConfig.builder()
            .endpoints(Arrays.asList("http://localhost:9200", ""))
            .build();
    }

    /**
     * Given Scenario: Configuring both basic auth and JWT token simultaneously
     * Expected: Should throw IllegalArgumentException
     */
    @Test(expected = IllegalArgumentException.class)
    public void test_createConfig_withBasicAndJWTAuth_shouldThrowException() {
        // Act & Assert
        OpenSearchClientConfig.builder()
            .addEndpoints("https://localhost:9200")
            .username("user")
            .password("pass")
            .jwtToken("token")
            .build();
    }

    /**
     * Given Scenario: Configuring both basic auth and certificate auth simultaneously
     * Expected: Should throw IllegalArgumentException
     */
    @Test(expected = IllegalArgumentException.class)
    public void test_createConfig_withBasicAndCertAuth_shouldThrowException() {
        // Act & Assert
        OpenSearchClientConfig.builder()
            .addEndpoints("https://localhost:9200")
            .username("user")
            .password("pass")
            .clientCertPath("/path/to/cert.pem")
            .build();
    }

    /**
     * Given Scenario: Configuring both JWT token and certificate auth simultaneously
     * Expected: Should throw IllegalArgumentException
     */
    @Test(expected = IllegalArgumentException.class)
    public void test_createConfig_withJWTAndCertAuth_shouldThrowException() {
        // Act & Assert
        OpenSearchClientConfig.builder()
            .addEndpoints("https://localhost:9200")
            .jwtToken("token")
            .clientCertPath("/path/to/cert.pem")
            .clientKeyPath("/path/to/key.pem")
            .build();
    }

    /**
     * Given Scenario: Configuring only username without password
     * Expected: Config should be created successfully (password can be empty for some scenarios)
     */
    @Test
    public void test_createConfig_withOnlyUsername_shouldSucceed() {
        // Act
        OpenSearchClientConfig config = OpenSearchClientConfig.builder()
            .addEndpoints("https://localhost:9200")
            .username("user")
            .build();

        // Assert
        assertTrue("Username should be present", config.username().isPresent());
        assertFalse("Password should not be present", config.password().isPresent());
    }

    /**
     * Given Scenario: Configuring only client certificate without key
     * Expected: Config should be created successfully (partial certificate configuration allowed)
     */
    @Test
    public void test_createConfig_withOnlyClientCert_shouldSucceed() {
        // Act
        OpenSearchClientConfig config = OpenSearchClientConfig.builder()
            .addEndpoints("https://localhost:9200")
            .clientCertPath("/path/to/cert.pem")
            .build();

        // Assert
        assertTrue("Client cert should be present", config.clientCertPath().isPresent());
        assertFalse("Client key should not be present", config.clientKeyPath().isPresent());
    }

    /**
     * Given Scenario: Attempting to modify endpoints list after config creation
     * Expected: Should throw UnsupportedOperationException (config is immutable)
     */
    @Test
    public void test_config_shouldBeImmutable() {
        // Act
        OpenSearchClientConfig config = OpenSearchClientConfig.builder()
            .addEndpoints("http://localhost:9200")
            .build();

        // Assert - The endpoints list should be immutable
        try {
            config.endpoints().add("http://another:9200");
            fail("Should not be able to modify endpoints list");
        } catch (UnsupportedOperationException e) {
            // Expected - list should be immutable
        }
    }

    /**
     * Given Scenario: Reusing the same builder to create multiple different configs
     * Expected: Each config should have its own distinct values
     */
    @Test
    public void test_builderReuse_shouldCreateDifferentConfigs() {
        // Act
        Builder builder = OpenSearchClientConfig.builder()
            .addEndpoints("http://localhost:9200");

        OpenSearchClientConfig config1 = builder.username("user1").build();
        OpenSearchClientConfig config2 = builder.username("user2").build();

        // Assert
        assertEquals("First config should have user1", "user1", config1.username().get());
        assertEquals("Second config should have user2", "user2", config2.username().get());
    }

    /**
     * Given Scenario: Adding multiple endpoints using different addEndpoints calls
     * Expected: All endpoints should be collected and available in the final config
     */
    @Test
    public void test_addMultipleEndpoints_shouldAcceptAllEndpoints() {
        // Act
        OpenSearchClientConfig config = OpenSearchClientConfig.builder()
            .addEndpoints("http://node1:9200")
            .addEndpoints("http://node2:9200", "http://node3:9200")
            .addEndpoints("http://node4:9200")
            .build();

        // Assert
        assertEquals("Should have 4 endpoints", 4, config.endpoints().size());
        assertTrue("Should contain node1", config.endpoints().contains("http://node1:9200"));
        assertTrue("Should contain node2", config.endpoints().contains("http://node2:9200"));
        assertTrue("Should contain node3", config.endpoints().contains("http://node3:9200"));
        assertTrue("Should contain node4", config.endpoints().contains("http://node4:9200"));
    }
}