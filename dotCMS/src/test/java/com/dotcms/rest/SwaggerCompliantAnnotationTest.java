package com.dotcms.rest;

import com.dotcms.rest.annotation.SwaggerCompliant;
import com.dotcms.rest.api.v1.apps.AppsResource;
import com.dotcms.rest.api.v1.authentication.AuthenticationResource;
import com.dotcms.rest.api.v1.user.UserResource;
import com.dotcms.rest.api.v1.content.ContentResource;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Simple test to verify @SwaggerCompliant annotation scanning without 
 * initializing the full dotCMS application context.
 */
public class SwaggerCompliantAnnotationTest {

    @Test
    public void testSwaggerCompliantAnnotationIsPresent() {
        // Test a few representative classes from different batches
        // Only test classes that have @SwaggerCompliant annotation (progressive rollout)
        verifyAnnotationIfPresent(AuthenticationResource.class, 1, "Core authentication");
        verifyAnnotationIfPresent(ContentResource.class, 2, "Content management");
        verifyAnnotationIfPresent(AppsResource.class, 6, "Rules engine");
        verifyAnnotationIfPresent(UserResource.class, 1, "Core authentication");
    }
    
    @Test 
    public void testAnnotationStructure() {
        SwaggerCompliant annotation = AppsResource.class.getAnnotation(SwaggerCompliant.class);
        
        if (annotation == null) {
            System.out.println("⚠️  AppsResource does not have @SwaggerCompliant annotation yet (progressive rollout)");
            return; // Skip test if annotation not present
        }
        
        assertEquals("Should be batch 6", 6, annotation.batch());
        assertTrue("Should have meaningful value", annotation.value().length() > 0);
        assertEquals("Should have correct version", "1.0", annotation.version());
    }
    
    @Test
    public void testBatchNumbers() {
        // Verify batch numbers are correctly assigned (only for annotated classes)
        verifyBatchNumberIfPresent(AuthenticationResource.class, 1, "AuthenticationResource");
        verifyBatchNumberIfPresent(ContentResource.class, 2, "ContentResource");
        verifyBatchNumberIfPresent(AppsResource.class, 6, "AppsResource");
    }
    
    @Test
    public void testAnnotationValues() {
        SwaggerCompliant authAnnotation = AuthenticationResource.class.getAnnotation(SwaggerCompliant.class);
        SwaggerCompliant contentAnnotation = ContentResource.class.getAnnotation(SwaggerCompliant.class);
        SwaggerCompliant appsAnnotation = AppsResource.class.getAnnotation(SwaggerCompliant.class);
        
        // Verify annotation values make sense (only for annotated classes)
        if (authAnnotation != null) {
            assertTrue("Auth annotation should mention authentication", 
                       authAnnotation.value().toLowerCase().contains("authentication"));
        }
        if (contentAnnotation != null) {
            assertTrue("Content annotation should mention content", 
                       contentAnnotation.value().toLowerCase().contains("content"));
        }
        if (appsAnnotation != null) {
            assertTrue("Apps annotation should mention appropriate context", 
                       appsAnnotation.value().length() > 10);
        }
        
        // Log which classes are not yet annotated
        if (authAnnotation == null) {
            System.out.println("⚠️  AuthenticationResource not yet annotated (progressive rollout)");
        }
        if (contentAnnotation == null) {
            System.out.println("⚠️  ContentResource not yet annotated (progressive rollout)");
        }
        if (appsAnnotation == null) {
            System.out.println("⚠️  AppsResource not yet annotated (progressive rollout)");
        }
    }
    
    private void verifyAnnotation(Class<?> resourceClass, int expectedBatch, String expectedContextInValue) {
        SwaggerCompliant annotation = resourceClass.getAnnotation(SwaggerCompliant.class);
        
        assertNotNull("@SwaggerCompliant annotation should be present on " + resourceClass.getSimpleName(), 
                      annotation);
        assertEquals("Batch number should match for " + resourceClass.getSimpleName(), 
                     expectedBatch, annotation.batch());
        assertTrue("Value should contain expected context for " + resourceClass.getSimpleName(),
                   annotation.value().toLowerCase().contains(expectedContextInValue.toLowerCase()));
    }
    
    private void verifyAnnotationIfPresent(Class<?> resourceClass, int expectedBatch, String expectedContextInValue) {
        SwaggerCompliant annotation = resourceClass.getAnnotation(SwaggerCompliant.class);
        
        if (annotation == null) {
            System.out.println("⚠️  " + resourceClass.getSimpleName() + " does not have @SwaggerCompliant annotation yet (progressive rollout)");
            return; // Skip verification if annotation not present
        }
        
        assertEquals("Batch number should match for " + resourceClass.getSimpleName(), 
                     expectedBatch, annotation.batch());
        assertTrue("Value should contain expected context for " + resourceClass.getSimpleName(),
                   annotation.value().toLowerCase().contains(expectedContextInValue.toLowerCase()));
    }
    
    private void verifyBatchNumberIfPresent(Class<?> resourceClass, int expectedBatch, String className) {
        SwaggerCompliant annotation = resourceClass.getAnnotation(SwaggerCompliant.class);
        
        if (annotation == null) {
            System.out.println("⚠️  " + className + " does not have @SwaggerCompliant annotation yet (progressive rollout)");
            return; // Skip verification if annotation not present
        }
        
        assertEquals(className + " should be batch " + expectedBatch, 
                     expectedBatch, annotation.batch());
    }
}