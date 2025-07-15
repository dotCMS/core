package com.dotcms.rest;

import com.dotcms.rest.annotation.SwaggerCompliant;
import com.dotcms.rest.api.v1.authentication.AuthenticationResource;
import com.dotcms.rest.api.v1.user.UserResource;
import com.dotcms.rest.api.v1.content.ContentResource;
import com.dotcms.rest.api.v1.site.SiteResource;
import com.dotcms.rest.api.v1.apps.AppsResource;
import com.dotcms.ai.rest.CompletionsResource;
import org.junit.Test;

import javax.ws.rs.Path;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Test to verify batch organization and annotation structure for progressive testing.
 */
public class SwaggerCompliantBatchTest {

    @Test
    public void testBatchDistribution() {
        // Test representative classes from each batch
        Class<?>[] testClasses = {
            AuthenticationResource.class,  // Batch 1
            UserResource.class,           // Batch 1
            ContentResource.class,        // Batch 2
            SiteResource.class,          // Batch 3
            AppsResource.class,          // Batch 6
            CompletionsResource.class    // Batch 7
        };
        
        Map<Integer, Integer> batchCounts = new HashMap<>();
        
        for (Class<?> clazz : testClasses) {
            SwaggerCompliant annotation = clazz.getAnnotation(SwaggerCompliant.class);
            assertNotNull("Class " + clazz.getSimpleName() + " should have @SwaggerCompliant", annotation);
            
            int batch = annotation.batch();
            batchCounts.put(batch, batchCounts.getOrDefault(batch, 0) + 1);
            
            // Verify batch numbers are in expected range
            assertTrue("Batch number should be between 1-8", batch >= 1 && batch <= 8);
        }
        
        // Verify we have classes from multiple batches
        assertTrue("Should have classes from multiple batches", batchCounts.size() >= 4);
        
        System.out.println("Batch distribution in test classes: " + batchCounts);
    }
    
    @Test
    public void testAnnotationConsistency() {
        // Test that all annotations have proper structure
        Class<?>[] testClasses = {
            AuthenticationResource.class,
            ContentResource.class,
            SiteResource.class,
            AppsResource.class
        };
        
        for (Class<?> clazz : testClasses) {
            SwaggerCompliant annotation = clazz.getAnnotation(SwaggerCompliant.class);
            
            // Check annotation structure
            assertNotNull("Should have annotation", annotation);
            assertTrue("Should have meaningful value", annotation.value().length() > 5);
            assertEquals("Should have correct version", "1.0", annotation.version());
            assertTrue("Should have valid batch", annotation.batch() >= 1 && annotation.batch() <= 8);
            
            // Verify the class also has @Path annotation (indicating it's a REST resource)
            assertTrue("Class should have @Path annotation", clazz.isAnnotationPresent(Path.class));
        }
    }
    
    @Test
    public void testBatchSemantics() {
        // Test that batch assignments make logical sense
        
        // Batch 1 should be authentication/user classes
        assertEquals("AuthenticationResource should be batch 1", 
                     1, AuthenticationResource.class.getAnnotation(SwaggerCompliant.class).batch());
        assertEquals("UserResource should be batch 1", 
                     1, UserResource.class.getAnnotation(SwaggerCompliant.class).batch());
        
        // Batch 2 should be content management
        assertEquals("ContentResource should be batch 2", 
                     2, ContentResource.class.getAnnotation(SwaggerCompliant.class).batch());
        
        // Batch 3 should be site architecture
        assertEquals("SiteResource should be batch 3", 
                     3, SiteResource.class.getAnnotation(SwaggerCompliant.class).batch());
        
        // Batch 6 should be rules engine/business logic
        assertEquals("AppsResource should be batch 6", 
                     6, AppsResource.class.getAnnotation(SwaggerCompliant.class).batch());
        
        // Batch 7 should be modern APIs
        assertEquals("CompletionsResource should be batch 7", 
                     7, CompletionsResource.class.getAnnotation(SwaggerCompliant.class).batch());
    }
    
    @Test
    public void testAnnotationValues() {
        // Test that annotation values make semantic sense
        
        SwaggerCompliant authAnnotation = AuthenticationResource.class.getAnnotation(SwaggerCompliant.class);
        assertTrue("Auth annotation should mention auth", 
                   authAnnotation.value().toLowerCase().contains("authentication"));
        
        SwaggerCompliant contentAnnotation = ContentResource.class.getAnnotation(SwaggerCompliant.class);
        assertTrue("Content annotation should mention content", 
                   contentAnnotation.value().toLowerCase().contains("content"));
        
        SwaggerCompliant siteAnnotation = SiteResource.class.getAnnotation(SwaggerCompliant.class);
        assertTrue("Site annotation should mention site/architecture", 
                   siteAnnotation.value().toLowerCase().contains("site") || 
                   siteAnnotation.value().toLowerCase().contains("architecture"));
        
        SwaggerCompliant appsAnnotation = AppsResource.class.getAnnotation(SwaggerCompliant.class);
        assertTrue("Apps annotation should have meaningful description", 
                   appsAnnotation.value().length() > 10);
        
        SwaggerCompliant aiAnnotation = CompletionsResource.class.getAnnotation(SwaggerCompliant.class);
        assertTrue("AI annotation should mention modern/APIs", 
                   aiAnnotation.value().toLowerCase().contains("modern") || 
                   aiAnnotation.value().toLowerCase().contains("apis"));
    }
}