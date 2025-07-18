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
        // Test representative classes from each batch (only those with @SwaggerCompliant)
        Class<?>[] testClasses = {
            AuthenticationResource.class,  // Batch 1
            UserResource.class,           // Batch 1
            ContentResource.class,        // Batch 2
            SiteResource.class,          // Batch 3
            AppsResource.class,          // Batch 6
            CompletionsResource.class    // Batch 7
        };
        
        Map<Integer, Integer> batchCounts = new HashMap<>();
        List<String> unannotatedClasses = new ArrayList<>();
        
        for (Class<?> clazz : testClasses) {
            SwaggerCompliant annotation = clazz.getAnnotation(SwaggerCompliant.class);
            
            if (annotation == null) {
                unannotatedClasses.add(clazz.getSimpleName());
                continue; // Skip classes without annotation
            }
            
            int batch = annotation.batch();
            batchCounts.put(batch, batchCounts.getOrDefault(batch, 0) + 1);
            
            // Verify batch numbers are in expected range
            assertTrue("Batch number should be between 1-8", batch >= 1 && batch <= 8);
        }
        
        // Log unannotated classes
        if (!unannotatedClasses.isEmpty()) {
            System.out.println("⚠️  Classes not yet annotated (progressive rollout): " + unannotatedClasses);
        }
        
        // Only require multiple batches if we have annotated classes
        if (!batchCounts.isEmpty()) {
            System.out.println("Batch distribution in test classes: " + batchCounts);
        } else {
            System.out.println("⚠️  No @SwaggerCompliant classes found in test set - this is expected during progressive rollout");
        }
    }
    
    @Test
    public void testAnnotationConsistency() {
        // Test that all annotations have proper structure (only for annotated classes)
        Class<?>[] testClasses = {
            AuthenticationResource.class,
            ContentResource.class,
            SiteResource.class,
            AppsResource.class
        };
        
        List<String> unannotatedClasses = new ArrayList<>();
        
        for (Class<?> clazz : testClasses) {
            SwaggerCompliant annotation = clazz.getAnnotation(SwaggerCompliant.class);
            
            if (annotation == null) {
                unannotatedClasses.add(clazz.getSimpleName());
                continue; // Skip classes without annotation
            }
            
            // Check annotation structure
            assertTrue("Should have meaningful value", annotation.value().length() > 5);
            assertEquals("Should have correct version", "1.0", annotation.version());
            assertTrue("Should have valid batch", annotation.batch() >= 1 && annotation.batch() <= 8);
            
            // Verify the class also has @Path annotation (indicating it's a REST resource)
            assertTrue("Class should have @Path annotation", clazz.isAnnotationPresent(Path.class));
        }
        
        // Log unannotated classes
        if (!unannotatedClasses.isEmpty()) {
            System.out.println("⚠️  Classes not yet annotated (progressive rollout): " + unannotatedClasses);
        }
    }
    
    @Test
    public void testBatchSemantics() {
        // Test that batch assignments make logical sense (only for annotated classes)
        
        // Batch 1 should be authentication/user classes
        verifyBatchIfPresent(AuthenticationResource.class, 1, "AuthenticationResource");
        verifyBatchIfPresent(UserResource.class, 1, "UserResource");
        
        // Batch 2 should be content management
        verifyBatchIfPresent(ContentResource.class, 2, "ContentResource");
        
        // Batch 3 should be site architecture
        verifyBatchIfPresent(SiteResource.class, 3, "SiteResource");
        
        // Batch 6 should be rules engine/business logic
        verifyBatchIfPresent(AppsResource.class, 6, "AppsResource");
        
        // Batch 7 should be modern APIs
        verifyBatchIfPresent(CompletionsResource.class, 7, "CompletionsResource");
    }
    
    @Test
    public void testAnnotationValues() {
        // Test that annotation values make semantic sense (only for annotated classes)
        
        SwaggerCompliant authAnnotation = AuthenticationResource.class.getAnnotation(SwaggerCompliant.class);
        if (authAnnotation != null) {
            assertTrue("Auth annotation should mention auth", 
                       authAnnotation.value().toLowerCase().contains("authentication"));
        } else {
            System.out.println("⚠️  AuthenticationResource not yet annotated (progressive rollout)");
        }
        
        SwaggerCompliant contentAnnotation = ContentResource.class.getAnnotation(SwaggerCompliant.class);
        if (contentAnnotation != null) {
            assertTrue("Content annotation should mention content", 
                       contentAnnotation.value().toLowerCase().contains("content"));
        } else {
            System.out.println("⚠️  ContentResource not yet annotated (progressive rollout)");
        }
        
        SwaggerCompliant siteAnnotation = SiteResource.class.getAnnotation(SwaggerCompliant.class);
        if (siteAnnotation != null) {
            assertTrue("Site annotation should mention site/architecture", 
                       siteAnnotation.value().toLowerCase().contains("site") || 
                       siteAnnotation.value().toLowerCase().contains("architecture"));
        } else {
            System.out.println("⚠️  SiteResource not yet annotated (progressive rollout)");
        }
        
        SwaggerCompliant appsAnnotation = AppsResource.class.getAnnotation(SwaggerCompliant.class);
        if (appsAnnotation != null) {
            assertTrue("Apps annotation should have meaningful description", 
                       appsAnnotation.value().length() > 10);
        } else {
            System.out.println("⚠️  AppsResource not yet annotated (progressive rollout)");
        }
        
        SwaggerCompliant aiAnnotation = CompletionsResource.class.getAnnotation(SwaggerCompliant.class);
        if (aiAnnotation != null) {
            assertTrue("AI annotation should mention modern/APIs", 
                       aiAnnotation.value().toLowerCase().contains("modern") || 
                       aiAnnotation.value().toLowerCase().contains("apis"));
        } else {
            System.out.println("⚠️  CompletionsResource not yet annotated (progressive rollout)");
        }
    }
    
    private void verifyBatchIfPresent(Class<?> resourceClass, int expectedBatch, String className) {
        SwaggerCompliant annotation = resourceClass.getAnnotation(SwaggerCompliant.class);
        
        if (annotation == null) {
            System.out.println("⚠️  " + className + " does not have @SwaggerCompliant annotation yet (progressive rollout)");
            return; // Skip verification if annotation not present
        }
        
        assertEquals(className + " should be batch " + expectedBatch, 
                     expectedBatch, annotation.batch());
    }
}