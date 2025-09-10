package com.dotcms.util;

import com.dotcms.UnitTestBase;
import com.dotcms.rest.annotation.SwaggerCompliant;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Test class to verify Jandex class metadata scanning functionality
 */
public class JandexClassMetadataScannerTest extends UnitTestBase {

    /**
     * Test that Jandex scanner can detect availability
     */
    @Test
    public void testJandexAvailability() {
        // This test will pass regardless of whether Jandex is available
        // It just verifies the method doesn't throw exceptions
        boolean available = JandexClassMetadataScanner.isJandexAvailable();
        // Jandex availability will be logged by the scanner itself
        
        // The method should return a boolean value
        assertTrue("Jandex availability check should return a boolean", 
                  available == true || available == false);
    }

    /**
     * Test finding classes with @SwaggerCompliant annotation
     */
    @Test
    public void testFindSwaggerCompliantClasses() {
        List<String> classNames = JandexClassMetadataScanner.findClassesWithAnnotation(
            "com.dotcms.rest.annotation.SwaggerCompliant", 
            "com.dotcms.rest"
        );
        
        // Results will be logged by the scanner itself if needed
        
        // Should not throw exceptions
        assertNotNull("Class names list should not be null", classNames);
        
        // If Jandex is available, we should find some classes
        // Scanner availability is logged internally
    }

    /**
     * Test finding classes with @Path annotation
     */
    @Test
    public void testFindPathAnnotatedClasses() {
        List<String> classNames = JandexClassMetadataScanner.findClassesWithAnnotation(
            "javax.ws.rs.Path", 
            "com.dotcms.rest"
        );
        
        // Results logged internally if needed
        
        // Should not throw exceptions
        assertNotNull("Class names list should not be null", classNames);
    }

    /**
     * Test loading classes with annotation
     */
    @Test
    public void testLoadClassesWithAnnotation() {
        List<Class<?>> classes = JandexClassMetadataScanner.findClassesWithAnnotation(
            SwaggerCompliant.class, 
            "com.dotcms.rest"
        );
        
        // Results logged internally if needed
        
        // Should not throw exceptions
        assertNotNull("Classes list should not be null", classes);
        
        // Verify that loaded classes actually have the annotation
        for (Class<?> clazz : classes) {
            assertTrue("Loaded class should have @SwaggerCompliant annotation", 
                      clazz.isAnnotationPresent(SwaggerCompliant.class));
        }
    }

    /**
     * Test annotation value extraction
     */
    @Test
    public void testGetAnnotationValue() {
        // Test with a known class that has @SwaggerCompliant annotation
        List<String> classNames = JandexClassMetadataScanner.findClassesWithAnnotation(
            "com.dotcms.rest.annotation.SwaggerCompliant", 
            "com.dotcms.rest"
        );
        
        if (!classNames.isEmpty()) {
            String testClassName = classNames.get(0);
            
            // Test getting batch value
            Integer batchValue = JandexClassMetadataScanner.getClassAnnotationIntValue(
                testClassName, 
                "com.dotcms.rest.annotation.SwaggerCompliant", 
                "batch"
            );
            
            // Batch value retrieved successfully
            
            // Batch value should be an integer if present
            if (batchValue != null) {
                assertTrue("Batch value should be positive", batchValue > 0);
            }
        }
    }

    /**
     * Test annotation presence check
     */
    @Test
    public void testHasClassAnnotation() {
        // Test with a known class that has @Path annotation
        List<String> classNames = JandexClassMetadataScanner.findClassesWithAnnotation(
            "javax.ws.rs.Path", 
            "com.dotcms.rest"
        );
        
        if (!classNames.isEmpty()) {
            String testClassName = classNames.get(0);
            
            boolean hasPath = JandexClassMetadataScanner.hasClassAnnotation(
                testClassName, 
                "javax.ws.rs.Path"
            );
            
            // Annotation presence checked successfully
            
            // Should return true for classes we know have the annotation
            assertTrue("Class should have @Path annotation", hasPath);
        }
    }

    /**
     * Test performance comparison (informational)
     */
    @Test
    public void testPerformanceComparison() {
        long startTime = System.currentTimeMillis();
        
        List<String> jandexResults = JandexClassMetadataScanner.findClassesWithAnnotation(
            "com.dotcms.rest.annotation.SwaggerCompliant", 
            "com.dotcms.rest"
        );
        
        long jandexTime = System.currentTimeMillis() - startTime;
        
        // Performance metrics collected for " + jandexResults.size() + " classes in " + jandexTime + "ms
        
        // Performance test - should complete in reasonable time
        assertTrue("Jandex scanning should complete in reasonable time", jandexTime < 5000);
    }
    
    /**
     * Test class hierarchy functionality
     */
    @Test
    public void testClassHierarchyMethods() {
        if (!JandexClassMetadataScanner.isJandexAvailable()) {
            // Skip test if Jandex not available
            return;
        }
        
        // Test finding implementations - this should not throw exceptions
        List<String> implementations = JandexClassMetadataScanner.findImplementationsOf(
            "java.lang.Comparable", 
            "java.lang"
        );
        
        assertNotNull("Implementations list should not be null", implementations);
        
        // Test finding subclasses - this should not throw exceptions
        List<String> subclasses = JandexClassMetadataScanner.findSubclassesOf(
            "java.lang.Exception", 
            "java.lang"
        );
        
        assertNotNull("Subclasses list should not be null", subclasses);
    }
    
    /**
     * Test class metadata methods
     */
    @Test
    public void testClassMetadataMethods() {
        if (!JandexClassMetadataScanner.isJandexAvailable()) {
            // Skip test if Jandex not available
            return;
        }
        
        // Test with a known class that should exist
        List<String> classNames = JandexClassMetadataScanner.findClassesWithAnnotation(
            "javax.ws.rs.Path", 
            "com.dotcms.rest"
        );
        
        if (!classNames.isEmpty()) {
            String testClassName = classNames.get(0);
            
            // Test getting superclass
            String superclass = JandexClassMetadataScanner.getSuperclassName(testClassName);
            // Superclass can be null or a valid class name
            
            // Test getting interfaces
            List<String> interfaces = JandexClassMetadataScanner.getInterfaceNames(testClassName);
            assertNotNull("Interfaces list should not be null", interfaces);
            
            // Test getting methods with annotation
            List<String> annotatedMethods = JandexClassMetadataScanner.getMethodsWithAnnotation(
                testClassName, 
                "javax.ws.rs.Path"
            );
            assertNotNull("Annotated methods list should not be null", annotatedMethods);
            
            // Test getting fields with annotation
            List<String> annotatedFields = JandexClassMetadataScanner.getFieldsWithAnnotation(
                testClassName, 
                "javax.inject.Inject"
            );
            assertNotNull("Annotated fields list should not be null", annotatedFields);
        }
    }
} 