package com.dotcms.content.opensearch.util;

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
     * Test basic configuration creation with minimal settings
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
     * Test configuration creation with all settings
     */
    @Test
    public void test_createConfig_withAllSettings_shouldSucceed() {
        // Act
        OpenSearchClientConfig config = OpenSearchClientConfig.builder()
            .endpoints(Arrays.asList("https://node1:9200", "https://node2:9200"))
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

        // Assert
        assertNotNull("Config should not be null", config);
        assertEquals("Should have two endpoints", 2, config.endpoints().size());
        assertTrue("Should have first endpoint", config.endpoints().contains("https://node1:9200"));
        assertTrue("Should have second endpoint", config.endpoints().contains("https://node2:9200"));
        assertTrue("Username should be present", config.username().isPresent());
        assertEquals("Username should match", "admin", config.username().get());
        assertTrue("Password should be present", config.password().isPresent());
        assertEquals("Password should match", "password123", config.password().get());
        assertTrue("Client cert path should be present", config.clientCertPath().isPresent());
        assertEquals("Client cert path should match", "/path/to/cert.pem", config.clientCertPath().get());
        assertTrue("TLS should be enabled", config.tlsEnabled());
        assertFalse("Trust self-signed should be false", config.trustSelfSigned());
        assertEquals("Connection timeout should match", Duration.ofSeconds(15), config.connectionTimeout());
        assertEquals("Socket timeout should match", Duration.ofSeconds(45), config.socketTimeout());
        assertEquals("Max connections should match", 200, config.maxConnections());
        assertEquals("Max connections per route should match", 100, config.maxConnectionsPerRoute());
    }

    /**
     * Test configuration with basic authentication
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
     * Test configuration with JWT authentication
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
     * Test configuration with certificate authentication
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
     * Test default values
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
     * Test validation - empty endpoints list should fail
     */
    @Test(expected = IllegalArgumentException.class)
    public void test_createConfig_withEmptyEndpoints_shouldThrowException() {
        // Act & Assert
        OpenSearchClientConfig.builder()
            .endpoints(Collections.emptyList())
            .build();
    }

    /**
     * Test validation - null endpoint should fail
     */
    @Test(expected = IllegalArgumentException.class)
    public void test_createConfig_withNullEndpoint_shouldThrowException() {
        // Act & Assert
        OpenSearchClientConfig.builder()
            .endpoints(Arrays.asList("http://localhost:9200", null))
            .build();
    }

    /**
     * Test validation - empty endpoint should fail
     */
    @Test(expected = IllegalArgumentException.class)
    public void test_createConfig_withEmptyEndpoint_shouldThrowException() {
        // Act & Assert
        OpenSearchClientConfig.builder()
            .endpoints(Arrays.asList("http://localhost:9200", ""))
            .build();
    }

    /**
     * Test validation - conflicting authentication methods (basic + JWT) should fail
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
     * Test validation - conflicting authentication methods (basic + cert) should fail
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
     * Test validation - conflicting authentication methods (JWT + cert) should fail
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
     * Test validation - incomplete basic auth (only username) should pass
     * Note: This is allowed as password could be empty for some auth scenarios
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
     * Test validation - incomplete cert auth (only client cert) should pass
     * Note: This allows partial certificate configuration
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
     * Test immutability - config should be immutable after creation
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
     * Test builder reuse - builder should be reusable
     */
    @Test
    public void test_builderReuse_shouldCreateDifferentConfigs() {
        // Act
        ImmutableOpenSearchClientConfig.Builder builder = OpenSearchClientConfig.builder()
            .addEndpoints("http://localhost:9200");

        OpenSearchClientConfig config1 = builder.username("user1").build();
        OpenSearchClientConfig config2 = builder.username("user2").build();

        // Assert
        assertEquals("First config should have user1", "user1", config1.username().get());
        assertEquals("Second config should have user2", "user2", config2.username().get());
    }

    /**
     * Test multiple endpoints addition
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