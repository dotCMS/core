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
        verifyAnnotation(AuthenticationResource.class, 1, "Core authentication");
        verifyAnnotation(ContentResource.class, 2, "Content management");
        verifyAnnotation(AppsResource.class, 6, "Rules engine");
        verifyAnnotation(UserResource.class, 1, "Core authentication");
    }
    
    @Test 
    public void testAnnotationStructure() {
        SwaggerCompliant annotation = AppsResource.class.getAnnotation(SwaggerCompliant.class);
        
        assertNotNull("@SwaggerCompliant annotation should be present", annotation);
        assertEquals("Should be batch 6", 6, annotation.batch());
        assertTrue("Should have meaningful value", annotation.value().length() > 0);
        assertEquals("Should have correct version", "1.0", annotation.version());
    }
    
    @Test
    public void testBatchNumbers() {
        // Verify batch numbers are correctly assigned
        assertEquals("AuthenticationResource should be batch 1", 
                     1, AuthenticationResource.class.getAnnotation(SwaggerCompliant.class).batch());
        assertEquals("ContentResource should be batch 2", 
                     2, ContentResource.class.getAnnotation(SwaggerCompliant.class).batch());
        assertEquals("AppsResource should be batch 6", 
                     6, AppsResource.class.getAnnotation(SwaggerCompliant.class).batch());
    }
    
    @Test
    public void testAnnotationValues() {
        SwaggerCompliant authAnnotation = AuthenticationResource.class.getAnnotation(SwaggerCompliant.class);
        SwaggerCompliant contentAnnotation = ContentResource.class.getAnnotation(SwaggerCompliant.class);
        SwaggerCompliant appsAnnotation = AppsResource.class.getAnnotation(SwaggerCompliant.class);
        
        // Verify annotation values make sense
        assertTrue("Auth annotation should mention authentication", 
                   authAnnotation.value().toLowerCase().contains("authentication"));
        assertTrue("Content annotation should mention content", 
                   contentAnnotation.value().toLowerCase().contains("content"));
        assertTrue("Apps annotation should mention appropriate context", 
                   appsAnnotation.value().length() > 10);
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
}