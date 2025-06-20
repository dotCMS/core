package com.dotcms.metrics;

import com.dotmarketing.util.Config;
import io.micrometer.core.instrument.Tag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test cases for MetricsTaggingService to verify K8s tags configuration
 * works correctly with and without environment variables.
 */
public class MetricsTaggingServiceTest {
    
    private MetricsTaggingService taggingService;
    
    @BeforeEach
    public void setup() {
        taggingService = new MetricsTaggingService();
    }
    
    /**
     * Test that default tags are applied when no DOT_METRICS_TAG_ environment variables are set.
     */
    @Test
    public void testDefaultTagsWithoutEnvironmentVariables() throws Exception {
        try (MockedStatic<Config> configMock = Mockito.mockStatic(Config.class);
             MockedStatic<InetAddress> inetMock = Mockito.mockStatic(InetAddress.class)) {
            
            // Mock Config for MetricsConfig constants
            configMock.when(() -> Config.getBooleanProperty("metrics.dynamic.tagging.enabled", true)).thenReturn(true);
            
            // Mock Config.subsetContainsAsList to return empty list (no METRICS_TAG properties)
            configMock.when(() -> Config.subsetContainsAsList("METRICS_TAG")).thenReturn(Collections.emptyList());
            
            // Mock fallback environment property
            configMock.when(() -> Config.getStringProperty("environment", "local")).thenReturn("local");
            
            // Mock InetAddress to return a test hostname
            InetAddress mockAddress = mock(InetAddress.class);
            inetMock.when(InetAddress::getLocalHost).thenReturn(mockAddress);
            when(mockAddress.getHostName()).thenReturn("test-host");
            
            // Initialize the service
            taggingService.init();
            
            // Get common tags and verify defaults
            List<Tag> tags = taggingService.getCommonTags();
            
            assertNotNull(tags, "Common tags should not be null");
            assertFalse(tags.isEmpty(), "Common tags should not be empty");
            
            // Verify expected default tags
            assertTagExists(tags, "app", "dotcms");
            assertTagExists(tags, "host", "test-host");
            assertTagExists(tags, "env", "local"); // Fallback to environment property
            // Version and commit tags are always present from MetricsConfig
            assertTrue(tags.stream().anyMatch(tag -> "version".equals(tag.getKey())), "Version tag should exist");
            assertTrue(tags.stream().anyMatch(tag -> "commit".equals(tag.getKey())), "Commit tag should exist");
        }
    }
    
    /**
     * Test that DOT_METRICS_TAG_ environment variables are properly used when set.
     */
    @Test
    public void testKubernetesEnvironmentVariables() throws Exception {
        try (MockedStatic<Config> configMock = Mockito.mockStatic(Config.class);
             MockedStatic<InetAddress> inetMock = Mockito.mockStatic(InetAddress.class)) {
            
            // Mock Config for MetricsConfig constants
            configMock.when(() -> Config.getBooleanProperty("metrics.dynamic.tagging.enabled", true)).thenReturn(true);
            
            // Mock Config.subsetContainsAsList to return DOT_METRICS_TAG_ properties
            List<String> metricsTagProperties = Arrays.asList(
                "DOT_METRICS_TAG_APP",
                "DOT_METRICS_TAG_ENV", 
                "DOT_METRICS_TAG_CUSTOMER",
                "DOT_METRICS_TAG_DEPLOYMENT"
            );
            configMock.when(() -> Config.subsetContainsAsList("METRICS_TAG")).thenReturn(metricsTagProperties);
            
            // Mock Config property calls for the discovered properties
            configMock.when(() -> Config.getStringProperty("DOT_METRICS_TAG_APP", null)).thenReturn("my-app");
            configMock.when(() -> Config.getStringProperty("DOT_METRICS_TAG_ENV", null)).thenReturn("production");
            configMock.when(() -> Config.getStringProperty("DOT_METRICS_TAG_CUSTOMER", null)).thenReturn("customer-123");
            configMock.when(() -> Config.getStringProperty("DOT_METRICS_TAG_DEPLOYMENT", null)).thenReturn("my-app-prod-deployment");
            
            // Mock fallback environment property
            configMock.when(() -> Config.getStringProperty("environment", "local")).thenReturn("local");
            
            // Mock InetAddress to return a K8s-style hostname
            InetAddress mockAddress = mock(InetAddress.class);
            inetMock.when(InetAddress::getLocalHost).thenReturn(mockAddress);
            when(mockAddress.getHostName()).thenReturn("my-app-prod-abc123-0");
            
            // Initialize the service
            taggingService.init();
            
            // Get common tags and verify K8s values
            List<Tag> tags = taggingService.getCommonTags();
            
            assertNotNull(tags, "Common tags should not be null");
            assertFalse(tags.isEmpty(), "Common tags should not be empty");
            
            // Verify environment variable values are used (overriding defaults)
            assertTagExists(tags, "app", "my-app");
            assertTagExists(tags, "env", "production");
            assertTagExists(tags, "customer", "customer-123");
            assertTagExists(tags, "deployment", "my-app-prod-deployment");
            assertTagExists(tags, "host", "my-app-prod-abc123-0");
            assertTagExists(tags, "replica", "0"); // Extracted from hostname
            // Version and commit tags are always present from MetricsConfig
            assertTrue(tags.stream().anyMatch(tag -> "version".equals(tag.getKey())), "Version tag should exist");
            assertTrue(tags.stream().anyMatch(tag -> "commit".equals(tag.getKey())), "Commit tag should exist");
        }
    }
    
    /**
     * Test that replica extraction from hostname works correctly.
     */
    @Test
    public void testReplicaExtractionFromHostname() throws Exception {
        try (MockedStatic<Config> configMock = Mockito.mockStatic(Config.class);
             MockedStatic<InetAddress> inetMock = Mockito.mockStatic(InetAddress.class)) {
            
            // Mock Config for MetricsConfig constants
            configMock.when(() -> Config.getBooleanProperty("metrics.dynamic.tagging.enabled", true)).thenReturn(true);
            
            // Mock Config.subsetContainsAsList to return DOT_METRICS_TAG_REPLICA
            List<String> replicaProperty = Arrays.asList("DOT_METRICS_TAG_REPLICA");
            configMock.when(() -> Config.subsetContainsAsList("METRICS_TAG")).thenReturn(replicaProperty);
            
            // Mock Config property call for replica
            configMock.when(() -> Config.getStringProperty("DOT_METRICS_TAG_REPLICA", null)).thenReturn("5");
            configMock.when(() -> Config.getStringProperty("environment", "local")).thenReturn("local");
            
            // Mock InetAddress to return various hostname patterns
            InetAddress mockAddress = mock(InetAddress.class);
            inetMock.when(InetAddress::getLocalHost).thenReturn(mockAddress);
            
            // Test with explicit replica environment variable
            when(mockAddress.getHostName()).thenReturn("dotcms-prod-abc123-5");
            taggingService.init();
            List<Tag> tags = taggingService.getCommonTags();
            assertTagExists(tags, "replica", "5");
            
            // Test with replica extraction from hostname (no explicit config)
            configMock.when(() -> Config.subsetContainsAsList("METRICS_TAG")).thenReturn(Collections.emptyList());
            configMock.when(() -> Config.getStringProperty("environment", "local")).thenReturn("local");
            
            when(mockAddress.getHostName()).thenReturn("simple-hostname");
            taggingService = new MetricsTaggingService(); // Reset service
            taggingService.init();
            tags = taggingService.getCommonTags();
            assertTagNotExists(tags, "replica"); // No replica pattern in hostname
        }
    }
    
    /**
     * Test that additional tags can be created properly.
     */
    @Test
    public void testCreateAdditionalTags() throws Exception {
        try (MockedStatic<Config> configMock = Mockito.mockStatic(Config.class);
             MockedStatic<InetAddress> inetMock = Mockito.mockStatic(InetAddress.class)) {
            
            // Mock Config for MetricsConfig constants
            configMock.when(() -> Config.getBooleanProperty("metrics.dynamic.tagging.enabled", true)).thenReturn(true);
            
            // Mock Config.subsetContainsAsList to return one DOT_METRICS_TAG_ property
            List<String> customerProperty = Arrays.asList("DOT_METRICS_TAG_CUSTOMER");
            configMock.when(() -> Config.subsetContainsAsList("METRICS_TAG")).thenReturn(customerProperty);
            
            // Mock Config property calls
            configMock.when(() -> Config.getStringProperty("DOT_METRICS_TAG_CUSTOMER", null)).thenReturn("default");
            configMock.when(() -> Config.getStringProperty("environment", "local")).thenReturn("local");
            
            // Mock InetAddress
            InetAddress mockAddress = mock(InetAddress.class);
            inetMock.when(InetAddress::getLocalHost).thenReturn(mockAddress);
            when(mockAddress.getHostName()).thenReturn("test-host");
            
            // Initialize the service
            taggingService.init();
            
            // Test creating additional tags
            List<Tag> tags = taggingService.createTags("service", "api", "endpoint", "/health");
            
            assertNotNull(tags, "Tags should not be null");
            assertTrue(tags.size() >= 2, "Tags should contain common tags plus additional");
            
            // Verify additional tags are included
            assertTagExists(tags, "service", "api");
            assertTagExists(tags, "endpoint", "/health");
            
            // Verify common tags are still included
            assertTagExists(tags, "app", "dotcms");
            assertTagExists(tags, "host", "test-host");
            assertTagExists(tags, "customer", "default");
        }
    }
    
    /**
     * Test that tag validation works correctly.
     */
    @Test
    public void testTagValidation() throws Exception {
        try (MockedStatic<Config> configMock = Mockito.mockStatic(Config.class);
             MockedStatic<InetAddress> inetMock = Mockito.mockStatic(InetAddress.class)) {
            
            // Mock Config for MetricsConfig constants
            configMock.when(() -> Config.getBooleanProperty("metrics.dynamic.tagging.enabled", true)).thenReturn(true);
            
            // Mock Config.subsetContainsAsList with valid configuration
            List<String> validProperties = Arrays.asList("DOT_METRICS_TAG_APP", "DOT_METRICS_TAG_ENV");
            configMock.when(() -> Config.subsetContainsAsList("METRICS_TAG")).thenReturn(validProperties);
            
            // Mock Config property calls
            configMock.when(() -> Config.getStringProperty("DOT_METRICS_TAG_APP", null)).thenReturn("test-app");
            configMock.when(() -> Config.getStringProperty("DOT_METRICS_TAG_ENV", null)).thenReturn("test-env");
            configMock.when(() -> Config.getStringProperty("environment", "local")).thenReturn("local");
            
            // Mock InetAddress
            InetAddress mockAddress = mock(InetAddress.class);
            inetMock.when(InetAddress::getLocalHost).thenReturn(mockAddress);
            when(mockAddress.getHostName()).thenReturn("test-host");
            
            // Initialize the service
            taggingService.init();
            
            // Test validation
            boolean isValid = taggingService.validateTagConfiguration();
            assertTrue(isValid, "Tag configuration should be valid");
        }
    }
    
    /**
     * Test fallback behavior when initialization fails.
     */
    @Test
    public void testFallbackBehavior() throws Exception {
        try (MockedStatic<Config> configMock = Mockito.mockStatic(Config.class);
             MockedStatic<InetAddress> inetMock = Mockito.mockStatic(InetAddress.class)) {
            
            // Mock Config to throw exception for some calls but not MetricsConfig constants
            configMock.when(() -> Config.getBooleanProperty("metrics.dynamic.tagging.enabled", true)).thenReturn(true);
            configMock.when(() -> Config.subsetContainsAsList("METRICS_TAG")).thenThrow(new RuntimeException("Config error"));
            configMock.when(() -> Config.getStringProperty("environment", "local")).thenReturn("local");
            
            // Mock InetAddress to throw exception  
            inetMock.when(InetAddress::getLocalHost).thenThrow(new RuntimeException("Network error"));
            
            // Initialize the service (should handle errors gracefully)
            taggingService.init();
            
            // Should still provide some tags even in error case
            List<Tag> tags = taggingService.getCommonTags();
            assertNotNull(tags, "Tags should not be null even in error case");
        }
    }
    
    /**
     * Helper method to assert that a tag with specific key-value exists.
     */
    private void assertTagExists(List<Tag> tags, String key, String value) {
        boolean found = tags.stream()
                .anyMatch(tag -> key.equals(tag.getKey()) && value.equals(tag.getValue()));
        assertTrue(found, "Tag with key '" + key + "' and value '" + value + "' should exist");
    }
    
    /**
     * Helper method to assert that a tag with specific key does not exist.
     */
    private void assertTagNotExists(List<Tag> tags, String key) {
        boolean found = tags.stream()
                .anyMatch(tag -> key.equals(tag.getKey()));
        assertFalse(found, "Tag with key '" + key + "' should not exist");
    }
}