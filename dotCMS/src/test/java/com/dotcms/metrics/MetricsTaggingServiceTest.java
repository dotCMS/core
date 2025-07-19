package com.dotcms.metrics;

import com.dotmarketing.util.Config;
import io.micrometer.core.instrument.Tag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.net.InetAddress;
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
     * Test that default tags are applied when no environment variables are set.
     */
    @Test
    public void testDefaultTagsWithoutEnvironmentVariables() throws Exception {
        try (MockedStatic<Config> configMock = Mockito.mockStatic(Config.class);
             MockedStatic<InetAddress> inetMock = Mockito.mockStatic(InetAddress.class)) {
            
            // Mock Config to return default values
            configMock.when(() -> Config.getStringProperty("k8s.tags.app", "dotcms")).thenReturn("dotcms");
            configMock.when(() -> Config.getStringProperty("k8s.tags.env", null)).thenReturn(null);
            configMock.when(() -> Config.getStringProperty("environment", "local")).thenReturn("local");
            configMock.when(() -> Config.getStringProperty("k8s.tags.version", "unknown")).thenReturn("unknown");
            configMock.when(() -> Config.getStringProperty("k8s.tags.customer", "default")).thenReturn("default");
            configMock.when(() -> Config.getStringProperty(eq("k8s.tags.deployment"), anyString())).thenReturn("test-host");
            
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
            assertTagExists(tags, "env", "local");
            assertTagExists(tags, "version", "unknown");
            assertTagExists(tags, "customer", "default");
            assertTagExists(tags, "deployment", "test-host");
            assertTagExists(tags, "host", "test-host");
        }
    }
    
    /**
     * Test that K8s environment variables are properly used when set.
     */
    @Test
    public void testKubernetesEnvironmentVariables() throws Exception {
        try (MockedStatic<Config> configMock = Mockito.mockStatic(Config.class);
             MockedStatic<InetAddress> inetMock = Mockito.mockStatic(InetAddress.class)) {
            
            // Mock Config to return K8s environment variable values
            configMock.when(() -> Config.getStringProperty("k8s.tags.app", "dotcms")).thenReturn("my-app");
            configMock.when(() -> Config.getStringProperty("k8s.tags.env", null)).thenReturn("production");
            configMock.when(() -> Config.getStringProperty("environment", "local")).thenReturn("local");
            configMock.when(() -> Config.getStringProperty("k8s.tags.version", "unknown")).thenReturn("1.2.3");
            configMock.when(() -> Config.getStringProperty("k8s.tags.customer", "default")).thenReturn("customer-123");
            configMock.when(() -> Config.getStringProperty(eq("k8s.tags.deployment"), anyString())).thenReturn("my-app-prod-deployment");
            
            // Mock InetAddress to return a K8s-style hostname with replica
            InetAddress mockAddress = mock(InetAddress.class);
            inetMock.when(InetAddress::getLocalHost).thenReturn(mockAddress);
            when(mockAddress.getHostName()).thenReturn("my-app-prod-abc123-0");
            
            // Initialize the service
            taggingService.init();
            
            // Get common tags and verify K8s values
            List<Tag> tags = taggingService.getCommonTags();
            
            assertNotNull(tags, "Common tags should not be null");
            assertFalse(tags.isEmpty(), "Common tags should not be empty");
            
            // Verify K8s environment variable values are used
            assertTagExists(tags, "app", "my-app");
            assertTagExists(tags, "env", "production");
            assertTagExists(tags, "version", "1.2.3");
            assertTagExists(tags, "customer", "customer-123");
            assertTagExists(tags, "deployment", "my-app-prod-deployment");
            assertTagExists(tags, "host", "my-app-prod-abc123-0");
            assertTagExists(tags, "replica", "0");
        }
    }
    
    /**
     * Test that replica extraction from hostname works correctly.
     */
    @Test
    public void testReplicaExtractionFromHostname() throws Exception {
        try (MockedStatic<Config> configMock = Mockito.mockStatic(Config.class);
             MockedStatic<InetAddress> inetMock = Mockito.mockStatic(InetAddress.class)) {
            
            // Mock Config with default values
            configMock.when(() -> Config.getStringProperty(anyString(), anyString())).thenReturn("default");
            configMock.when(() -> Config.getStringProperty(eq("k8s.tags.deployment"), anyString())).thenReturn("test-deployment");
            
            // Mock InetAddress to return various hostname patterns
            InetAddress mockAddress = mock(InetAddress.class);
            inetMock.when(InetAddress::getLocalHost).thenReturn(mockAddress);
            
            // Test with replica number in hostname
            when(mockAddress.getHostName()).thenReturn("dotcms-prod-abc123-5");
            taggingService.init();
            List<Tag> tags = taggingService.getCommonTags();
            assertTagExists(tags, "replica", "5");
            
            // Test without replica number in hostname
            when(mockAddress.getHostName()).thenReturn("simple-hostname");
            taggingService = new MetricsTaggingService(); // Reset service
            taggingService.init();
            tags = taggingService.getCommonTags();
            assertTagNotExists(tags, "replica");
        }
    }
    
    /**
     * Test that additional tags can be created properly.
     */
    @Test
    public void testCreateAdditionalTags() throws Exception {
        try (MockedStatic<Config> configMock = Mockito.mockStatic(Config.class);
             MockedStatic<InetAddress> inetMock = Mockito.mockStatic(InetAddress.class)) {
            
            // Mock Config with minimal setup
            configMock.when(() -> Config.getStringProperty(anyString(), anyString())).thenReturn("default");
            
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
            assertTagExists(tags, "app", "default");
            assertTagExists(tags, "host", "test-host");
        }
    }
    
    /**
     * Test that tag validation works correctly.
     */
    @Test
    public void testTagValidation() throws Exception {
        try (MockedStatic<Config> configMock = Mockito.mockStatic(Config.class);
             MockedStatic<InetAddress> inetMock = Mockito.mockStatic(InetAddress.class)) {
            
            // Mock Config with valid configuration
            configMock.when(() -> Config.getStringProperty("k8s.tags.app", "dotcms")).thenReturn("test-app");
            configMock.when(() -> Config.getStringProperty("k8s.tags.env", null)).thenReturn("test-env");
            configMock.when(() -> Config.getStringProperty("environment", "local")).thenReturn("local");
            configMock.when(() -> Config.getStringProperty("k8s.tags.version", "unknown")).thenReturn("1.0.0");
            configMock.when(() -> Config.getStringProperty("k8s.tags.customer", "default")).thenReturn("test-customer");
            configMock.when(() -> Config.getStringProperty(eq("k8s.tags.deployment"), anyString())).thenReturn("test-deployment");
            
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
            
            // Mock Config to throw exception
            configMock.when(() -> Config.getStringProperty(anyString(), anyString())).thenThrow(new RuntimeException("Config error"));
            
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