package com.dotcms.rest;

import com.dotcms.rest.annotation.SwaggerCompliant;
import org.junit.Test;

import javax.ws.rs.Path;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * Test to verify the cumulative batch functionality works correctly.
 * This test simulates what the actual compliance tests do with -Dtest.batch.max.
 */
public class SwaggerCompliantCumulativeTest {

    @Test
    public void testCumulativeClassSelection() throws Exception {
        // Test that cumulative batch selection works for batches 1-3
        List<Class<?>> batch1Classes = getAnnotatedClassesUpToBatch(1);
        List<Class<?>> batch2Classes = getAnnotatedClassesUpToBatch(2);
        List<Class<?>> batch3Classes = getAnnotatedClassesUpToBatch(3);
        
        // Verify cumulative behavior
        assertTrue("Batch 2 should include all batch 1 classes", batch2Classes.containsAll(batch1Classes));
        assertTrue("Batch 3 should include all batch 1 classes", batch3Classes.containsAll(batch1Classes));
        assertTrue("Batch 3 should include all batch 2 classes", batch3Classes.containsAll(batch2Classes));
        
        // Verify counts increase cumulatively
        assertTrue("Batch 2 should have more classes than batch 1", batch2Classes.size() > batch1Classes.size());
        assertTrue("Batch 3 should have more classes than batch 2", batch3Classes.size() > batch2Classes.size());
        
        // Verify batch 1 has expected minimum count (15 classes)
        assertTrue("Batch 1 should have at least 15 classes", batch1Classes.size() >= 15);
        
        // Verify batch 2 has expected minimum count (15 + 14 = 29 classes)
        assertTrue("Batch 2 should have at least 29 classes", batch2Classes.size() >= 29);
        
        System.out.println("Cumulative batch results:");
        System.out.println("Batch 1: " + batch1Classes.size() + " classes");
        System.out.println("Batch 2: " + batch2Classes.size() + " classes");
        System.out.println("Batch 3: " + batch3Classes.size() + " classes");
    }
    
    @Test
    public void testAllBatchesHaveClasses() throws Exception {
        // Test that all 8 batches have classes assigned
        for (int batchNum = 1; batchNum <= 8; batchNum++) {
            List<Class<?>> batchClasses = getAnnotatedClassesForBatch(batchNum);
            assertTrue("Batch " + batchNum + " should have classes assigned", 
                      batchClasses.size() > 0);
            
            System.out.println("Batch " + batchNum + ": " + batchClasses.size() + " classes");
        }
    }
    
    @Test
    public void testSystemPropertySimulation() throws Exception {
        // Simulate what happens when -Dtest.batch.max=2 is set
        int maxBatch = 2;
        List<Class<?>> selectedClasses = getAnnotatedClassesUpToBatch(maxBatch);
        
        // Verify only batch 1 and 2 classes are selected
        for (Class<?> clazz : selectedClasses) {
            SwaggerCompliant annotation = clazz.getAnnotation(SwaggerCompliant.class);
            assertTrue("Class " + clazz.getSimpleName() + " should be in batch 1 or 2", 
                      annotation.batch() <= maxBatch);
        }
        
        // Verify we have classes from both batches
        boolean hasBatch1 = selectedClasses.stream().anyMatch(c -> 
            c.getAnnotation(SwaggerCompliant.class).batch() == 1);
        boolean hasBatch2 = selectedClasses.stream().anyMatch(c -> 
            c.getAnnotation(SwaggerCompliant.class).batch() == 2);
            
        assertTrue("Should have batch 1 classes", hasBatch1);
        assertTrue("Should have batch 2 classes", hasBatch2);
        
        System.out.println("Selected " + selectedClasses.size() + " classes for batches 1-" + maxBatch);
    }
    
    /**
     * Get all @SwaggerCompliant annotated classes up to and including the specified batch.
     * This simulates the cumulative behavior used in the actual compliance tests.
     */
    private List<Class<?>> getAnnotatedClassesUpToBatch(int maxBatch) throws Exception {
        List<Class<?>> result = new ArrayList<>();
        
        // Find all classes in the com.dotcms.rest package and subpackages
        List<Class<?>> allClasses = getRestResourceClasses();
        
        for (Class<?> clazz : allClasses) {
            SwaggerCompliant annotation = clazz.getAnnotation(SwaggerCompliant.class);
            if (annotation != null && annotation.batch() <= maxBatch) {
                result.add(clazz);
            }
        }
        
        return result;
    }
    
    /**
     * Get all @SwaggerCompliant annotated classes for a specific batch.
     */
    private List<Class<?>> getAnnotatedClassesForBatch(int batchNum) throws Exception {
        List<Class<?>> result = new ArrayList<>();
        
        // Find all classes in the com.dotcms.rest package and subpackages
        List<Class<?>> allClasses = getRestResourceClasses();
        
        for (Class<?> clazz : allClasses) {
            SwaggerCompliant annotation = clazz.getAnnotation(SwaggerCompliant.class);
            if (annotation != null && annotation.batch() == batchNum) {
                result.add(clazz);
            }
        }
        
        return result;
    }
    
    /**
     * Get all potential REST resource classes (classes with @Path annotation).
     */
    private List<Class<?>> getRestResourceClasses() throws Exception {
        List<Class<?>> result = new ArrayList<>();
        
        // Get classes from common REST packages
        String[] packages = {
            "com.dotcms.rest",
            "com.dotcms.rest.api.v1",
            "com.dotcms.rest.api.v2", 
            "com.dotcms.rest.api.v3",
            "com.dotcms.ai.rest",
            "com.dotcms.auth.providers.saml.v1",
            "com.dotcms.contenttype.model.field",
            "com.dotcms.rendering.js",
            "com.dotcms.telemetry.rest",
            "com.dotcms.rest.elasticsearch",
            "com.dotcms.rest.personas"
        };
        
        for (String packageName : packages) {
            try {
                List<Class<?>> classes = getClassesInPackage(packageName);
                for (Class<?> clazz : classes) {
                    if (clazz.isAnnotationPresent(Path.class) && 
                        !Modifier.isAbstract(clazz.getModifiers()) &&
                        !clazz.isInterface()) {
                        result.add(clazz);
                    }
                }
            } catch (Exception e) {
                // Package may not exist, continue
            }
        }
        
        return result;
    }
    
    /**
     * Get all classes in a package.
     */
    private List<Class<?>> getClassesInPackage(String packageName) throws Exception {
        List<Class<?>> classes = new ArrayList<>();
        String packagePath = packageName.replace('.', '/');
        
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> resources = classLoader.getResources(packagePath);
            
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                if (resource.getProtocol().equals("file")) {
                    File directory = new File(resource.getFile());
                    if (directory.exists()) {
                        classes.addAll(getClassesFromDirectory(directory, packageName));
                    }
                }
            }
        } catch (Exception e) {
            // Ignore errors for missing packages
        }
        
        return classes;
    }
    
    /**
     * Get all classes from a directory.
     */
    private List<Class<?>> getClassesFromDirectory(File directory, String packageName) throws Exception {
        List<Class<?>> classes = new ArrayList<>();
        
        if (!directory.exists()) {
            return classes;
        }
        
        File[] files = directory.listFiles();
        if (files == null) {
            return classes;
        }
        
        for (File file : files) {
            if (file.isDirectory()) {
                classes.addAll(getClassesFromDirectory(file, packageName + "." + file.getName()));
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + "." + file.getName().substring(0, file.getName().length() - 6);
                try {
                    Class<?> clazz = Class.forName(className);
                    classes.add(clazz);
                } catch (Exception e) {
                    // Ignore classes that can't be loaded
                }
            }
        }
        
        return classes;
    }
}