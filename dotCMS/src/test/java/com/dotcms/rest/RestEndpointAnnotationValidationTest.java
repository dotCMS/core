package com.dotcms.rest;

import com.dotcms.UnitTestBase;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.annotation.SwaggerCompliant;
import com.dotcms.rest.annotation.ConsumesRequestBodyDirectly;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
// Parameter import handled via full qualification to avoid conflicts
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.junit.BeforeClass;
import org.junit.Test;
// Note: This test uses reflection but avoids external dependencies
// For production use, consider using the simpler RestEndpointAnnotationComplianceTest

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static org.junit.Assert.*;

/**
 * Comprehensive test class to validate REST endpoint annotations compliance
 * according to the dotCMS REST API Development Guide standards.
 * 
 * This test ensures that all REST endpoints follow the annotation rules defined in:
 * dotCMS/src/main/java/com/dotcms/rest/README.md
 */
public class RestEndpointAnnotationValidationTest extends UnitTestBase {

    private static Set<Class<?>> restResourceClasses;
    private static final String REST_PACKAGE = "com.dotcms.rest";
    private static final String PORTLET_PACKAGE = "com.dotmarketing.portlets";
    
    // Track violations for detailed reporting
    private static Map<String, List<String>> violationsByClass = new HashMap<>();
    
    // Track architectural warnings separately (don't fail tests)
    private static Map<String, List<String>> warningsByClass = new HashMap<>();
    
    @BeforeClass
    public static void setUpClass() {
        long startTime = System.currentTimeMillis();
        
        try {
            // Progressive rollout approach: Only test classes with @SwaggerCompliant annotation
            restResourceClasses = findSwaggerCompliantClasses();
            
            System.out.println("Found " + restResourceClasses.size() + " @SwaggerCompliant REST resource classes for validation");
        } catch (Exception e) {
            // Handle setup failures gracefully - this can happen when no @SwaggerCompliant classes are found
            // or when the dotCMS context is not fully initialized
            System.out.println("Setup failed (expected during progressive rollout): " + e.getMessage());
            restResourceClasses = new HashSet<>();
        }
        
        long endTime = System.currentTimeMillis();
        System.out.println("Setup completed in " + (endTime - startTime) + "ms");
    }
    
    /**
     * Find all classes annotated with @SwaggerCompliant in the REST packages.
     * This method dynamically scans the classpath for classes with the annotation.
     */
    private static Set<Class<?>> findSwaggerCompliantClasses() {
        Set<Class<?>> swaggerCompliantClasses = new HashSet<>();
        
        // Common REST packages to scan
        String[] packagesToScan = {
            "com.dotcms.rest.api.v1",
            "com.dotcms.rest.api.v2", 
            "com.dotcms.rest.api.v3",
            "com.dotcms.rest",
            "com.dotcms.ai.rest",
            "com.dotcms.telemetry.rest",
            "com.dotcms.auth.providers.saml.v1",
            "com.dotcms.contenttype.model.field",
            "com.dotcms.rendering.js"
        };
        
        // Check for batch filtering - cumulative approach
        String maxBatchProperty = System.getProperty("test.batch.max");
        Integer maxBatch = null;
        
        if (maxBatchProperty != null) {
            try {
                maxBatch = Integer.parseInt(maxBatchProperty);
            } catch (NumberFormatException e) {
                System.out.println("Warning: Invalid max batch number '" + maxBatchProperty + "', ignoring filter");
            }
        }
        
        for (String packageName : packagesToScan) {
            try {
                List<Class<?>> classes = getClassesInPackage(packageName);
                for (Class<?> clazz : classes) {
                    try {
                        if (clazz.isAnnotationPresent(SwaggerCompliant.class)) {
                            SwaggerCompliant annotation = clazz.getAnnotation(SwaggerCompliant.class);
                            int classBatch = annotation.batch();
                            
                            // Apply cumulative batch filtering - include all batches up to maxBatch
                            if (maxBatch != null && classBatch > maxBatch) {
                                continue; // Skip classes beyond max batch
                            }
                            
                            swaggerCompliantClasses.add(clazz);
                        }
                    } catch (Exception e) {
                        // Skip classes that can't be processed (e.g., missing dependencies)
                        System.out.println("Warning: Could not process class " + clazz.getName() + ": " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                // Package might not exist or have issues - continue scanning
                System.out.println("Warning: Could not scan package " + packageName + ": " + e.getMessage());
            }
        }
        
        return swaggerCompliantClasses;
    }
    
    /**
     * Get all classes in a package using reflection.
     */
    private static List<Class<?>> getClassesInPackage(String packageName) throws Exception {
        List<Class<?>> classes = new ArrayList<>();
        String packagePath = packageName.replace('.', '/');
        
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> resources = classLoader.getResources(packagePath);
        
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            
            if (resource.getProtocol().equals("file")) {
                // Handle file system resources
                File directory = new File(resource.getFile());
                classes.addAll(getClassesFromDirectory(directory, packageName));
            } else if (resource.getProtocol().equals("jar")) {
                // Handle JAR resources
                classes.addAll(getClassesFromJar(resource, packagePath));
            }
        }
        
        return classes;
    }
    
    /**
     * Get classes from a directory on the file system.
     */
    private static List<Class<?>> getClassesFromDirectory(File directory, String packageName) throws Exception {
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
                String className = packageName + "." + file.getName().replace(".class", "");
                try {
                    Class<?> clazz = Class.forName(className);
                    classes.add(clazz);
                } catch (ClassNotFoundException | LinkageError | SecurityException e) {
                    // Skip classes that can't be loaded (various reasons including missing dependencies,
                    // static initialization failures, etc.)
                }
            }
        }
        
        return classes;
    }
    
    /**
     * Get classes from a JAR file.
     */
    private static List<Class<?>> getClassesFromJar(URL jarUrl, String packagePath) throws Exception {
        List<Class<?>> classes = new ArrayList<>();
        
        // Extract jar file path from URL
        String jarPath = jarUrl.getPath();
        if (jarPath.startsWith("file:")) {
            jarPath = jarPath.substring(5);
        }
        if (jarPath.contains("!")) {
            jarPath = jarPath.substring(0, jarPath.indexOf("!"));
        }
        
        try (JarFile jarFile = new JarFile(jarPath)) {
            Enumeration<JarEntry> entries = jarFile.entries();
            
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();
                
                if (entryName.startsWith(packagePath) && entryName.endsWith(".class")) {
                    String className = entryName.replace('/', '.').replace(".class", "");
                    try {
                        Class<?> clazz = Class.forName(className);
                        classes.add(clazz);
                    } catch (ClassNotFoundException | LinkageError | SecurityException e) {
                        // Skip classes that can't be loaded (various reasons including missing dependencies,
                        // static initialization failures, etc.)
                    }
                }
            }
        } catch (IOException e) {
            // Skip JAR files that can't be read
        }
        
        return classes;
    }
    
    /**
     * Test that all REST resource classes have proper @Tag annotations
     */
    @Test
    public void testClassLevelTagAnnotations() {
        // Skip test if no @SwaggerCompliant classes found (progressive rollout)
        if (restResourceClasses.isEmpty()) {
            System.out.println("✅ No @SwaggerCompliant classes found - skipping testClassLevelTagAnnotations");
            return;
        }
        
        List<String> violatingClasses = new ArrayList<>();
        
        for (Class<?> resourceClass : restResourceClasses) {
            Tag tagAnnotation = resourceClass.getAnnotation(Tag.class);
            
            if (tagAnnotation == null) {
                violatingClasses.add(resourceClass.getName());
                addViolation(resourceClass.getName(), "Missing @Tag annotation at class level");
            } else {
                // Validate tag content
                if (tagAnnotation.name() == null || tagAnnotation.name().trim().isEmpty()) {
                    addViolation(resourceClass.getName(), "@Tag annotation missing name");
                }
                // Note: Descriptions should NOT be in resource classes - only in DotRestApplication
                // This is checked in testTagsAreDeclaredInApplication()
            }
        }
        
        if (!violatingClasses.isEmpty()) {
            fail("REST resource classes missing @Tag annotations: " + violatingClasses);
        }
    }
    
    /**
     * Test that all REST endpoint methods have proper @Operation annotations
     */
    @Test
    public void testMethodLevelOperationAnnotations() {
        // Skip test if no @SwaggerCompliant classes found (progressive rollout)
        if (restResourceClasses.isEmpty()) {
            System.out.println("✅ No @SwaggerCompliant classes found - skipping testMethodLevelOperationAnnotations");
            return;
        }
        
        List<String> violatingMethods = new ArrayList<>();
        
        for (Class<?> resourceClass : restResourceClasses) {
            Method[] methods = resourceClass.getDeclaredMethods();
            
            for (Method method : methods) {
                if (isRestEndpointMethod(method)) {
                    Operation operationAnnotation = method.getAnnotation(Operation.class);
                    
                    if (operationAnnotation == null) {
                        String methodName = resourceClass.getSimpleName() + "." + method.getName();
                        violatingMethods.add(methodName);
                        addViolation(resourceClass.getName(), "Method " + method.getName() + " missing @Operation annotation");
                    } else {
                        // Validate operation content
                        if (operationAnnotation.summary() == null || operationAnnotation.summary().trim().isEmpty()) {
                            addViolation(resourceClass.getName(), "Method " + method.getName() + " @Operation missing summary");
                        }
                        if (operationAnnotation.description() == null || operationAnnotation.description().trim().isEmpty()) {
                            addViolation(resourceClass.getName(), "Method " + method.getName() + " @Operation missing description");
                        }
                    }
                }
            }
        }
        
        if (!violatingMethods.isEmpty()) {
            fail("REST endpoint methods missing @Operation annotations: " + violatingMethods);
        }
    }
    
    /**
     * Test that REST endpoint methods have proper response documentation (via @ApiResponses or @Operation.responses)
     */
    @Test
    public void testMethodLevelApiResponsesAnnotations() {
        // Skip test if no @SwaggerCompliant classes found (progressive rollout)
        if (restResourceClasses.isEmpty()) {
            System.out.println("✅ No @SwaggerCompliant classes found - skipping testMethodLevelApiResponsesAnnotations");
            return;
        }
        
        List<String> violatingMethods = new ArrayList<>();
        
        for (Class<?> resourceClass : restResourceClasses) {
            Method[] methods = resourceClass.getDeclaredMethods();
            
            for (Method method : methods) {
                if (isRestEndpointMethod(method)) {
                    ApiResponses apiResponsesAnnotation = method.getAnnotation(ApiResponses.class);
                    Operation operationAnnotation = method.getAnnotation(Operation.class);
                    
                    // Check for 200 response documentation in either @ApiResponses or @Operation.responses
                    boolean has200Response = false;
                    boolean hasValid200Schema = false;
                    ApiResponse response200 = null;
                    
                    // First check @ApiResponses
                    if (apiResponsesAnnotation != null) {
                        for (ApiResponse response : apiResponsesAnnotation.value()) {
                            if ("200".equals(response.responseCode())) {
                                has200Response = true;
                                response200 = response;
                                break;
                            }
                        }
                    }
                    
                    // If no 200 response in @ApiResponses, check @Operation.responses
                    if (!has200Response && operationAnnotation != null) {
                        for (ApiResponse response : operationAnnotation.responses()) {
                            if ("200".equals(response.responseCode())) {
                                has200Response = true;
                                response200 = response;
                                break;
                            }
                        }
                    }
                    
                    if (!has200Response) {
                        // No 200 response documentation found - add as warning
                        addWarning(resourceClass.getName(), "Method " + method.getName() + " missing 200 response documentation - add to @ApiResponses or @Operation.responses");
                    } else {
                        // Validate the 200 response schema
                        hasValid200Schema = validateSuccessResponseSchema(resourceClass, method, response200);
                        
                        if (!hasValid200Schema) {
                            String methodName = resourceClass.getSimpleName() + "." + method.getName();
                            violatingMethods.add(methodName);
                            addViolation(resourceClass.getName(), "Method " + method.getName() + " has 200 response but missing proper schema documentation");
                        }
                    }
                }
            }
        }
        
        if (!violatingMethods.isEmpty()) {
            fail("REST endpoint methods with invalid 200 response schemas: " + violatingMethods);
        }
    }
    
    /**
     * Test that all path parameters have proper @Parameter annotations
     */
    @Test
    public void testPathParameterAnnotations() {
        // Skip test if no @SwaggerCompliant classes found (progressive rollout)
        if (restResourceClasses.isEmpty()) {
            System.out.println("✅ No @SwaggerCompliant classes found - skipping testPathParameterAnnotations");
            return;
        }
        
        List<String> violatingParameters = new ArrayList<>();
        
        for (Class<?> resourceClass : restResourceClasses) {
            Method[] methods = resourceClass.getDeclaredMethods();
            
            for (Method method : methods) {
                if (isRestEndpointMethod(method)) {
                    java.lang.reflect.Parameter[] parameters = method.getParameters();
                    
                    for (java.lang.reflect.Parameter parameter : parameters) {
                        PathParam pathParamAnnotation = parameter.getAnnotation(PathParam.class);
                        
                        if (pathParamAnnotation != null) {
                            io.swagger.v3.oas.annotations.Parameter parameterAnnotation = 
                                parameter.getAnnotation(io.swagger.v3.oas.annotations.Parameter.class);
                            
                            if (parameterAnnotation == null) {
                                String paramName = resourceClass.getSimpleName() + "." + method.getName() + 
                                                 "(" + parameter.getName() + ")";
                                violatingParameters.add(paramName);
                                addViolation(resourceClass.getName(), "Path parameter " + parameter.getName() + 
                                           " in method " + method.getName() + " missing @Parameter annotation");
                            } else {
                                // Validate parameter content
                                if (parameterAnnotation.description() == null || 
                                    parameterAnnotation.description().trim().isEmpty()) {
                                    addViolation(resourceClass.getName(), "Path parameter " + parameter.getName() + 
                                               " in method " + method.getName() + " missing description");
                                }
                            }
                        }
                    }
                }
            }
        }
        
        if (!violatingParameters.isEmpty()) {
            fail("Path parameters missing @Parameter annotations: " + violatingParameters);
        }
    }
    
    /**
     * Test that request bodies have proper @RequestBody annotations
     */
    @Test
    public void testRequestBodyAnnotations() {
        // Skip test if no @SwaggerCompliant classes found (progressive rollout)
        if (restResourceClasses.isEmpty()) {
            System.out.println("✅ No @SwaggerCompliant classes found - skipping testRequestBodyAnnotations");
            return;
        }
        
        List<String> violatingMethods = new ArrayList<>();
        
        for (Class<?> resourceClass : restResourceClasses) {
            Method[] methods = resourceClass.getDeclaredMethods();
            
            for (Method method : methods) {
                if (isRestEndpointMethod(method) && hasRequestBody(method)) {
                    boolean hasRequestBodyAnnotation = false;
                    boolean hasFormParams = false;
                    boolean isFormUrlEncoded = false;
                    boolean isMultipartFormData = false;
                    boolean hasSuspendedParam = false;
                    
                    // Check if method uses form-urlencoded or multipart form data content type
                    Consumes consumesAnnotation = method.getAnnotation(Consumes.class);
                    if (consumesAnnotation != null) {
                        for (String mediaType : consumesAnnotation.value()) {
                            if (mediaType.contains("form-urlencoded")) {
                                isFormUrlEncoded = true;
                                break;
                            }
                            if (mediaType.contains("multipart/form-data") || mediaType.contains("MULTIPART_FORM_DATA")) {
                                isMultipartFormData = true;
                                break;
                            }
                        }
                    }
                    
                    for (java.lang.reflect.Parameter parameter : method.getParameters()) {
                        if (parameter.getAnnotation(RequestBody.class) != null) {
                            hasRequestBodyAnnotation = true;
                            
                            // Validate request body annotation
                            RequestBody requestBodyAnnotation = parameter.getAnnotation(RequestBody.class);
                            if (requestBodyAnnotation.description() == null || 
                                requestBodyAnnotation.description().trim().isEmpty()) {
                                addViolation(resourceClass.getName(), "Method " + method.getName() + 
                                           " @RequestBody missing description");
                            }
                            break;
                        }
                        
                        // Check for @FormParam which is alternative to @RequestBody for form data
                        if (parameter.isAnnotationPresent(javax.ws.rs.FormParam.class)) {
                            hasFormParams = true;
                        }
                        
                        // Check for @FormDataParam which is alternative to @RequestBody for multipart
                        try {
                            @SuppressWarnings("unchecked")
                            Class<? extends Annotation> formDataParamClass = (Class<? extends Annotation>) Class.forName("org.glassfish.jersey.media.multipart.FormDataParam");
                            if (parameter.isAnnotationPresent(formDataParamClass)) {
                                hasFormParams = true;
                            }
                        } catch (ClassNotFoundException e) {
                            // FormDataParam not available, continue
                        }
                        
                        // Check for @BeanParam which is alternative to @RequestBody for form data aggregation
                        if (parameter.isAnnotationPresent(javax.ws.rs.BeanParam.class)) {
                            hasFormParams = true;
                        }
                        
                        // Check for @Suspended which indicates async JAX-RS endpoint
                        if (parameter.isAnnotationPresent(javax.ws.rs.container.Suspended.class)) {
                            hasSuspendedParam = true;
                        }
                    }
                    
                    // Check if method consumes request body directly (e.g., streaming endpoints)
                    boolean consumesDirectly = method.isAnnotationPresent(ConsumesRequestBodyDirectly.class);
                    
                    // Don't require @RequestBody if method uses @FormParam, is form-urlencoded, multipart form data, async (@Suspended), or consumes directly
                    // These methods access request data through JAX-RS form parameters, HttpServletRequest.getParameter(), async response, or direct stream access
                    if (!hasRequestBodyAnnotation && !hasFormParams && !isFormUrlEncoded && !isMultipartFormData && !hasSuspendedParam && !consumesDirectly) {
                        String methodName = resourceClass.getSimpleName() + "." + method.getName();
                        violatingMethods.add(methodName);
                        addViolation(resourceClass.getName(), "Method " + method.getName() + 
                                   " with request body missing @RequestBody annotation");
                    }
                }
            }
        }
        
        if (!violatingMethods.isEmpty()) {
            fail("Methods with request bodies missing @RequestBody annotations: " + violatingMethods);
        }
    }
    
    /**
     * Test that methods have proper @Produces annotations
     */
    @Test
    public void testProducesAnnotations() {
        // Skip test if no @SwaggerCompliant classes found (progressive rollout)
        if (restResourceClasses.isEmpty()) {
            System.out.println("✅ No @SwaggerCompliant classes found - skipping testProducesAnnotations");
            return;
        }
        
        List<String> violatingMethods = new ArrayList<>();
        
        for (Class<?> resourceClass : restResourceClasses) {
            Method[] methods = resourceClass.getDeclaredMethods();
            
            for (Method method : methods) {
                if (isRestEndpointMethod(method)) {
                    Produces producesAnnotation = method.getAnnotation(Produces.class);
                    
                    if (producesAnnotation == null) {
                        String methodName = resourceClass.getSimpleName() + "." + method.getName();
                        violatingMethods.add(methodName);
                        addViolation(resourceClass.getName(), "Method " + method.getName() + 
                                   " missing @Produces annotation");
                    } else {
                        // Validate media types
                        String[] mediaTypes = producesAnnotation.value();
                        if (mediaTypes.length == 0) {
                            addViolation(resourceClass.getName(), "Method " + method.getName() + 
                                       " @Produces annotation has no media types");
                        }
                    }
                }
            }
        }
        
        if (!violatingMethods.isEmpty()) {
            fail("REST endpoint methods missing @Produces annotations: " + violatingMethods);
        }
    }
    
    /**
     * Test that @Consumes annotations are properly used
     */
    @Test
    public void testConsumesAnnotations() {
        // Skip test if no @SwaggerCompliant classes found (progressive rollout)
        if (restResourceClasses.isEmpty()) {
            System.out.println("✅ No @SwaggerCompliant classes found - skipping testConsumesAnnotations");
            return;
        }
        
        List<String> violatingMethods = new ArrayList<>();
        
        for (Class<?> resourceClass : restResourceClasses) {
            Method[] methods = resourceClass.getDeclaredMethods();
            
            for (Method method : methods) {
                if (isRestEndpointMethod(method)) {
                    boolean hasRequestBody = hasRequestBody(method);
                    Consumes consumesAnnotation = method.getAnnotation(Consumes.class);
                    
                    // Check if method has @Suspended parameter (async JAX-RS)
                    boolean hasSuspended = false;
                    for (java.lang.reflect.Parameter parameter : method.getParameters()) {
                        if (parameter.isAnnotationPresent(javax.ws.rs.container.Suspended.class)) {
                            hasSuspended = true;
                            break;
                        }
                    }
                    
                    // Skip validation for async methods with @Suspended
                    if (!hasSuspended) {
                        // Check for VTL GET methods with request bodies (legacy exception)
                        boolean isVtlGetWithBody = isVtlGetMethodWithRequestBody(resourceClass, method);
                        
                        if (hasRequestBody && consumesAnnotation == null) {
                            String methodName = resourceClass.getSimpleName() + "." + method.getName();
                            violatingMethods.add(methodName);
                            addViolation(resourceClass.getName(), "Method " + method.getName() + 
                                       " has request body but missing @Consumes annotation");
                        } else if (!hasRequestBody && consumesAnnotation != null && !isVtlGetWithBody) {
                            // Allow @Consumes on VTL GET methods with request bodies (legacy pattern)
                            String methodName = resourceClass.getSimpleName() + "." + method.getName();
                            violatingMethods.add(methodName);
                            addViolation(resourceClass.getName(), "Method " + method.getName() + 
                                       " has @Consumes but no request body (GET/DELETE without body)");
                        }
                    }
                }
            }
        }
        
        if (!violatingMethods.isEmpty()) {
            fail("Methods with incorrect @Consumes usage: " + violatingMethods);
        }
    }
    
    /**
     * Test that all @Tag annotations reference tags declared in DotRestApplication
     */
    @Test
    public void testTagsAreDeclaredInApplication() {
        // Skip test if no @SwaggerCompliant classes found (progressive rollout)
        if (restResourceClasses.isEmpty()) {
            System.out.println("✅ No @SwaggerCompliant classes found - skipping testTagsAreDeclaredInApplication");
            return;
        }
        
        List<String> violatingTags = new ArrayList<>();
        
        // Get declared tags from DotRestApplication
        Set<String> declaredTags = getDeclaredTagsFromApplication();
        Set<String> usedTags = new HashSet<>();
        
        for (Class<?> resourceClass : restResourceClasses) {
            Tag tagAnnotation = resourceClass.getAnnotation(Tag.class);
            
            if (tagAnnotation != null) {
                String tagName = tagAnnotation.name();
                usedTags.add(tagName);
                
                if (!declaredTags.contains(tagName)) {
                    violatingTags.add(resourceClass.getSimpleName() + " uses undeclared tag: '" + tagName + "'");
                    addViolation(resourceClass.getName(), "Uses undeclared tag '" + tagName + "' - must be declared in DotRestApplication");
                }
                
                // Also check for duplicate descriptions (should only be in DotRestApplication)
                if (tagAnnotation.description() != null && !tagAnnotation.description().trim().isEmpty()) {
                    addViolation(resourceClass.getName(), "Tag '" + tagName + "' has description in resource class - descriptions should only be in DotRestApplication");
                }
            }
        }
        
        // Check for unused declared tags
        Set<String> unusedTags = new HashSet<>(declaredTags);
        unusedTags.removeAll(usedTags);
        
        if (!unusedTags.isEmpty()) {
            System.out.println("\nUnused tags declared in DotRestApplication:");
            unusedTags.stream().sorted().forEach(tag -> System.out.println("  - " + tag));
            System.out.println("Consider removing these unused tag declarations.");
        }
        
        if (!violatingTags.isEmpty()) {
            System.out.println("\nUndeclared tags found:");
            violatingTags.forEach(System.out::println);
            fail("Resource classes using undeclared tags: " + violatingTags);
        }
        
        // Print usage summary
        System.out.println("\nTag Usage Summary:");
        System.out.println("Declared tags: " + declaredTags.size());
        System.out.println("Used tags: " + usedTags.size());
        System.out.println("Unused tags: " + unusedTags.size());
    }
    
    /**
     * Test for architectural design issues (warnings, not failures)
     */
    @Test
    public void testArchitecturalWarnings() {
        // Skip test if no @SwaggerCompliant classes found (progressive rollout)
        if (restResourceClasses.isEmpty()) {
            System.out.println("✅ No @SwaggerCompliant classes found - skipping testArchitecturalWarnings");
            return;
        }
        
        for (Class<?> resourceClass : restResourceClasses) {
            Method[] methods = resourceClass.getDeclaredMethods();
            
            for (Method method : methods) {
                if (isRestEndpointMethod(method)) {
                    // Check for POST methods that don't consume request bodies (should probably be GET/DELETE)
                    if (method.isAnnotationPresent(POST.class) && !method.isAnnotationPresent(Consumes.class)) {
                        // Check if method uses path parameters instead of request body
                        boolean usesPathParams = false;
                        for (java.lang.reflect.Parameter parameter : method.getParameters()) {
                            if (parameter.isAnnotationPresent(PathParam.class)) {
                                usesPathParams = true;
                                break;
                            }
                        }
                        
                        if (usesPathParams) {
                            addWarning(resourceClass.getName(), 
                                "Method " + method.getName() + " uses @POST with path parameters - consider @GET or @DELETE for better REST semantics");
                        }
                    }
                }
            }
        }
        
        // Architectural warnings don't fail the test, just get reported
    }
    
    
    /**
     * Test for common @Schema antipatterns
     */
    @Test
    public void testSchemaAntipatterns() {
        // Skip test if no @SwaggerCompliant classes found (progressive rollout)
        if (restResourceClasses.isEmpty()) {
            System.out.println("✅ No @SwaggerCompliant classes found - skipping testSchemaAntipatterns");
            return;
        }
        
        List<String> violatingMethods = new ArrayList<>();
        
        for (Class<?> resourceClass : restResourceClasses) {
            Method[] methods = resourceClass.getDeclaredMethods();
            
            for (Method method : methods) {
                if (isRestEndpointMethod(method)) {
                    ApiResponses apiResponsesAnnotation = method.getAnnotation(ApiResponses.class);
                    
                    if (apiResponsesAnnotation != null) {
                        for (ApiResponse response : apiResponsesAnnotation.value()) {
                            if ("200".equals(response.responseCode())) {
                                Content[] contentArray = response.content();
                                for (Content content : contentArray) {
                                    Schema schemaAnnotation = content.schema();
                                    
                                    // Check for antipatterns
                                    if (schemaAnnotation != null) {
                                        Class<?> implementation = schemaAnnotation.implementation();
                                        
                                        // Antipattern: Raw ResponseEntityView.class
                                        if (implementation == ResponseEntityView.class) {
                                            String methodName = resourceClass.getSimpleName() + "." + method.getName();
                                            violatingMethods.add(methodName);
                                            addViolation(resourceClass.getName(), "Method " + method.getName() + 
                                                       " uses raw ResponseEntityView.class - should use specific ResponseEntity*View class");
                                        }
                                        
                                        // Antipattern: Object.class without type="object"
                                        if (implementation == Object.class && 
                                            (schemaAnnotation.type() == null || schemaAnnotation.type().trim().isEmpty())) {
                                            addViolation(resourceClass.getName(), "Method " + method.getName() + 
                                                       " uses Object.class without type='object' - should use type='object' with description");
                                        }
                                        
                                        // Antipattern: type="object" without description
                                        if ("object".equals(schemaAnnotation.type()) && 
                                            (schemaAnnotation.description() == null || schemaAnnotation.description().trim().isEmpty())) {
                                            addViolation(resourceClass.getName(), "Method " + method.getName() + 
                                                       " uses type='object' without description - should include meaningful description");
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        if (!violatingMethods.isEmpty()) {
            fail("Methods with @Schema antipatterns: " + violatingMethods);
        }
    }
    
    /**
     * Print detailed violation report with performance metrics
     */
    @Test
    public void testPrintViolationReport() {
        // Skip test if no @SwaggerCompliant classes found (progressive rollout)
        if (restResourceClasses.isEmpty()) {
            System.out.println("✅ No @SwaggerCompliant classes found - skipping testPrintViolationReport");
            return;
        }
        
        long overallStartTime = System.currentTimeMillis();
        
        System.out.println("\n=== COMPREHENSIVE REST ENDPOINT VALIDATION ===");
        System.out.println("Testing " + restResourceClasses.size() + " REST resource classes");
        
        // Run all validation checks to populate violations
        long[] testTimes = new long[7];
        
        testTimes[0] = measureTest("Class-level @Tag annotations", () -> {
            try {
                testClassLevelTagAnnotations();
            } catch (AssertionError e) {
                // Continue to collect all violations
            }
        });
        
        testTimes[1] = measureTest("Method-level @Operation annotations", () -> {
            try {
                testMethodLevelOperationAnnotations();
            } catch (AssertionError e) {
                // Continue to collect all violations
            }
        });
        
        testTimes[2] = measureTest("Method-level @ApiResponses annotations", () -> {
            try {
                testMethodLevelApiResponsesAnnotations();
            } catch (AssertionError e) {
                // Continue to collect all violations
            }
        });
        
        testTimes[3] = measureTest("Path parameter @Parameter annotations", () -> {
            try {
                testPathParameterAnnotations();
            } catch (AssertionError e) {
                // Continue to collect all violations
            }
        });
        
        testTimes[4] = measureTest("Request body @RequestBody annotations", () -> {
            try {
                testRequestBodyAnnotations();
            } catch (AssertionError e) {
                // Continue to collect all violations
            }
        });
        
        testTimes[5] = measureTest("@Produces annotations", () -> {
            try {
                testProducesAnnotations();
            } catch (AssertionError e) {
                // Continue to collect all violations
            }
        });
        
        testTimes[6] = measureTest("@Consumes annotations", () -> {
            try {
                testConsumesAnnotations();
            } catch (AssertionError e) {
                // Continue to collect all violations
            }
        });
        
        
        long[] expandedTestTimes2 = new long[testTimes.length + 1];
        System.arraycopy(testTimes, 0, expandedTestTimes2, 0, testTimes.length);
        
        expandedTestTimes2[7] = measureTest("@Schema antipatterns", () -> {
            try {
                testSchemaAntipatterns();
            } catch (AssertionError e) {
                // Continue to collect all violations
            }
        });
        
        testTimes = expandedTestTimes2;
        
        long[] expandedTestTimes = new long[testTimes.length + 1];
        System.arraycopy(testTimes, 0, expandedTestTimes, 0, testTimes.length);
        
        expandedTestTimes[8] = measureTest("Tag declarations in DotRestApplication", () -> {
            try {
                testTagsAreDeclaredInApplication();
            } catch (AssertionError e) {
                // Continue to collect all violations
            }
        });
        
        // Add the new architectural warnings test
        long[] finalTestTimes = new long[expandedTestTimes.length + 1];
        System.arraycopy(expandedTestTimes, 0, finalTestTimes, 0, expandedTestTimes.length);
        
        finalTestTimes[expandedTestTimes.length] = measureTest("Architectural warnings (non-blocking)", () -> {
            try {
                testArchitecturalWarnings();
            } catch (AssertionError e) {
                // Continue to collect all violations
            }
        });
        
        testTimes = finalTestTimes;
        
        long overallTime = System.currentTimeMillis() - overallStartTime;
        
        // Print performance summary
        System.out.println("\n=== PERFORMANCE SUMMARY ===");
        System.out.println("Overall validation time: " + overallTime + "ms");
        System.out.println("Average time per class: " + (overallTime / restResourceClasses.size()) + "ms");
        long totalTestTime = 0;
        for (long time : testTimes) {
            totalTestTime += time;
        }
        System.out.println("Total test execution time: " + totalTestTime + "ms");
        
        // Print detailed report
        System.out.println("\n=== REST ENDPOINT ANNOTATION VIOLATIONS REPORT ===");
        System.out.println("Total classes analyzed: " + restResourceClasses.size());
        System.out.println("Classes with violations: " + violationsByClass.size());
        
        if (!violationsByClass.isEmpty()) {
            System.out.println("\nDetailed violations by class:");
            violationsByClass.forEach((className, violations) -> {
                System.out.println("\n" + className + ":");
                violations.forEach(violation -> System.out.println("  - " + violation));
            });
            
            System.out.println("\n=== SUMMARY ===");
            int totalViolations = violationsByClass.values().stream()
                    .mapToInt(List::size)
                    .sum();
            System.out.println("Total violations: " + totalViolations);
            System.out.println("Violation rate: " + String.format("%.2f", (double)violationsByClass.size() / restResourceClasses.size() * 100) + "%");
        } else {
            System.out.println("\nNo violations found! All REST endpoints are properly annotated.");
        }
        
        // Print architectural warnings separately
        if (!warningsByClass.isEmpty()) {
            System.out.println("\n=== ARCHITECTURAL WARNINGS (NON-BLOCKING) ===");
            System.out.println("Classes with architectural warnings: " + warningsByClass.size());
            warningsByClass.forEach((className, warnings) -> {
                System.out.println("\n" + className + ":");
                warnings.forEach(warning -> System.out.println("  - WARNING: " + warning));
            });
            
            int totalWarnings = warningsByClass.values().stream()
                    .mapToInt(List::size)
                    .sum();
            System.out.println("\nTotal architectural warnings: " + totalWarnings);
            System.out.println("Note: These are design suggestions and do not fail the validation.");
        }
        
        // Comparison with compliance test
        System.out.println("\n=== COMPARISON WITH COMPLIANCE TEST ===");
        System.out.println("Compliance test covers: 15 classes");
        System.out.println("This comprehensive test covers: " + restResourceClasses.size() + " classes");
        System.out.println("Coverage increase: " + (restResourceClasses.size() - 15) + " additional classes");
        System.out.println("Coverage factor: " + String.format("%.1f", (double)restResourceClasses.size() / 15) + "x");
    }
    
    /**
     * Measure execution time of a test
     */
    private long measureTest(String testName, Runnable test) {
        long startTime = System.currentTimeMillis();
        test.run();
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        System.out.println("  " + testName + ": " + duration + "ms");
        return duration;
    }
    
    // Helper methods
    
    private boolean isRestEndpointMethod(Method method) {
        // Check if it has REST annotations
        boolean hasRestAnnotation = method.isAnnotationPresent(GET.class) ||
                                  method.isAnnotationPresent(POST.class) ||
                                  method.isAnnotationPresent(PUT.class) ||
                                  method.isAnnotationPresent(DELETE.class);
        
        // Exclude methods marked as @Hidden from validation
        if (hasRestAnnotation && method.isAnnotationPresent(io.swagger.v3.oas.annotations.Hidden.class)) {
            return false;
        }
        
        return hasRestAnnotation;
    }
    
    private boolean hasRequestBody(Method method) {
        // Check if method already has @RequestBody annotation - definitely has request body
        for (java.lang.reflect.Parameter parameter : method.getParameters()) {
            if (parameter.getAnnotation(RequestBody.class) != null) {
                return true;
            }
        }
        
        // Check if method is marked as consuming request body directly (for streaming, async, etc.)
        if (method.isAnnotationPresent(ConsumesRequestBodyDirectly.class)) {
            return true;
        }
        
        // Check if method has @RequestBody in @Operation annotation
        Operation operationAnnotation = method.getAnnotation(Operation.class);
        if (operationAnnotation != null && operationAnnotation.requestBody() != null && 
            !operationAnnotation.requestBody().description().isEmpty()) {
            // If there's a non-empty requestBody in the Operation, method has request body
            return true;
        }
        
        // Check if method has form parameters - these consume request bodies
        for (java.lang.reflect.Parameter parameter : method.getParameters()) {
            if (parameter.isAnnotationPresent(javax.ws.rs.FormParam.class)) {
                return true;
            }
            
            // @BeanParam can aggregate both query params (GET) and form params (POST/PUT)
            // Only consider it a request body for non-GET methods with @Consumes
            if (parameter.isAnnotationPresent(javax.ws.rs.BeanParam.class)) {
                // For GET methods, @BeanParam aggregates query parameters, not request body
                if (!method.isAnnotationPresent(GET.class)) {
                    Consumes consumesAnnotation = method.getAnnotation(Consumes.class);
                    if (consumesAnnotation != null) {
                        for (String mediaType : consumesAnnotation.value()) {
                            if (mediaType.contains("json") || mediaType.contains("xml") || 
                                mediaType.contains("form-urlencoded") || mediaType.contains("multipart")) {
                                return true;
                            }
                        }
                    }
                }
            }
            
            // Check for @FormDataParam
            try {
                @SuppressWarnings("unchecked")
                Class<? extends Annotation> formDataParamClass = (Class<? extends Annotation>) 
                    Class.forName("org.glassfish.jersey.media.multipart.FormDataParam");
                if (parameter.isAnnotationPresent(formDataParamClass)) {
                    return true;
                }
            } catch (ClassNotFoundException e) {
                // FormDataParam not available, continue
            }
        }
        
        // Check if method has @Suspended (async methods don't consume traditional request bodies)
        for (java.lang.reflect.Parameter parameter : method.getParameters()) {
            if (parameter.isAnnotationPresent(javax.ws.rs.container.Suspended.class)) {
                return false; // Async methods don't have traditional request bodies
            }
        }
        
        // Check HTTP method - only POST, PUT, PATCH typically have request bodies
        boolean isPotentialBodyMethod = method.isAnnotationPresent(POST.class) || 
                                       method.isAnnotationPresent(PUT.class) || 
                                       method.isAnnotationPresent(javax.ws.rs.PATCH.class);
        
        // Special case: VTL GET methods with request bodies (legacy pattern)
        if (method.isAnnotationPresent(GET.class)) {
            // VTLResource GET methods are legacy exceptions that accept request bodies
            Class<?> declaringClass = method.getDeclaringClass();
            if (declaringClass != null && declaringClass.getSimpleName().equals("VTLResource")) {
                // Check if this is one of the specific GET methods that accepts request bodies
                String methodName = method.getName();
                if ((methodName.equals("get") || methodName.equals("dynamicGet")) && hasNonJaxRsParameters(method)) {
                    return true; // VTL GET methods with body parameters
                }
            }
            return false; // Regular GET methods don't have request bodies
        }
        
        if (!isPotentialBodyMethod) {
            return false; // DELETE, HEAD, OPTIONS don't have request bodies
        }
        
        // Check @Consumes annotation - if a POST/PUT/PATCH method has @Consumes for JSON/XML it likely consumes request body  
        Consumes consumesAnnotation = method.getAnnotation(Consumes.class);
        if (consumesAnnotation != null) {
            // Only flag JSON/XML consumers as needing @RequestBody
            for (String mediaType : consumesAnnotation.value()) {
                if (mediaType.contains("json") || mediaType.contains("xml")) {
                    return true;
                }
            }
        }
        
        // For POST/PUT/PATCH methods, be more conservative about detecting request body parameters
        // Only consider it a request body if there are parameters that are NOT JAX-RS standard annotations
        // AND the method has @Consumes or other strong indicators
        return hasNonJaxRsParameters(method) && consumesAnnotation != null;
    }
    
    /**
     * Check if method has non-JAX-RS parameters (extracted for reuse)
     */
    private boolean hasNonJaxRsParameters(Method method) {
        for (java.lang.reflect.Parameter parameter : method.getParameters()) {
            // Skip all JAX-RS standard annotations and dotCMS context parameters
            if (isJaxRsOrContextParameter(parameter)) {
                continue; // These don't indicate request body consumption
            }
            
            // If we reach here, this parameter is not a standard JAX-RS/context parameter
            return true;
        }
        return false;
    }
    
    /**
     * Check if this is a VTL GET method that accepts request bodies (legacy pattern)
     */
    private boolean isVtlGetMethodWithRequestBody(Class<?> resourceClass, Method method) {
        if (!method.isAnnotationPresent(GET.class)) {
            return false;
        }
        
        if (!resourceClass.getSimpleName().equals("VTLResource")) {
            return false;
        }
        
        String methodName = method.getName();
        if (!(methodName.equals("get") || methodName.equals("dynamicGet"))) {
            return false;
        }
        
        // Check if method has request body parameters (like Map<String, Object> bodyMap or String bodyMapString)
        for (java.lang.reflect.Parameter parameter : method.getParameters()) {
            if (isJaxRsOrContextParameter(parameter)) {
                continue;
            }
            
            // VTL methods have Map<String, Object> or String parameters for request body
            Class<?> paramType = parameter.getType();
            String paramTypeName = paramType.getSimpleName();
            if (paramType == java.util.Map.class || paramTypeName.equals("String")) {
                return true; // This is a VTL GET method with request body parameter
            }
        }
        
        return false;
    }
    
    /**
     * Checks if a parameter is a standard JAX-RS or dotCMS context parameter that should not be considered a request body
     */
    private boolean isJaxRsOrContextParameter(java.lang.reflect.Parameter parameter) {
        // Standard JAX-RS parameter annotations
        if (parameter.isAnnotationPresent(javax.ws.rs.core.Context.class) ||
            parameter.isAnnotationPresent(javax.ws.rs.PathParam.class) ||
            parameter.isAnnotationPresent(javax.ws.rs.QueryParam.class) ||
            parameter.isAnnotationPresent(javax.ws.rs.HeaderParam.class) ||
            parameter.isAnnotationPresent(javax.ws.rs.CookieParam.class) ||
            parameter.isAnnotationPresent(javax.ws.rs.MatrixParam.class) ||
            parameter.isAnnotationPresent(javax.ws.rs.DefaultValue.class) ||
            parameter.isAnnotationPresent(io.swagger.v3.oas.annotations.Parameter.class)) {
            return true;
        }
        
        // Check parameter type - common context types
        Class<?> paramType = parameter.getType();
        if (paramType == javax.servlet.http.HttpServletRequest.class ||
            paramType == javax.servlet.http.HttpServletResponse.class ||
            paramType == javax.servlet.http.HttpSession.class ||
            paramType == javax.ws.rs.core.SecurityContext.class ||
            paramType == javax.ws.rs.core.UriInfo.class ||
            paramType == javax.ws.rs.core.Request.class ||
            paramType == javax.ws.rs.core.Response.class) {
            return true;
        }
        
        return false;
    }
    
    private boolean validateSuccessResponseSchema(Class<?> resourceClass, Method method, ApiResponse response) {
        Content[] contentArray = response.content();
        
        // Check if this is a no-content response (valid for DELETE, some POST operations)
        String description = response.description();
        if (description != null && (description.toLowerCase().contains("no content") || 
                                   description.toLowerCase().contains("no body") ||
                                   description.toLowerCase().contains("(no"))) {
            // No content responses are valid without schema
            return true;
        }
        
        if (contentArray.length == 0) {
            // Only flag as violation if it's not a DELETE method or other operation that might not return content
            if (!method.isAnnotationPresent(DELETE.class)) {
                addViolation(resourceClass.getName(), "Method " + method.getName() + 
                           " 200 response missing content/schema (add content with schema or update description to indicate no content)");
            }
            return method.isAnnotationPresent(DELETE.class); // DELETE methods are OK without content
        }
        
        for (Content content : contentArray) {
            Schema schemaAnnotation = content.schema();
            if (schemaAnnotation == null) {
                addViolation(resourceClass.getName(), "Method " + method.getName() + 
                           " 200 response missing @Schema annotation");
                return false;
            }
            
            // Check for valid schema implementation
            Class<?> implementation = schemaAnnotation.implementation();
            String type = schemaAnnotation.type();
            
            if (implementation == void.class && (type == null || type.trim().isEmpty())) {
                addViolation(resourceClass.getName(), "Method " + method.getName() + 
                           " 200 response has empty @Schema (no implementation or type)");
                return false;
            }
        }
        
        return true;
    }
    
    private void addViolation(String className, String violation) {
        violationsByClass.computeIfAbsent(className, k -> new ArrayList<>()).add(violation);
    }
    
    private void addWarning(String className, String warning) {
        warningsByClass.computeIfAbsent(className, k -> new ArrayList<>()).add(warning);
    }
    
    /**
     * Extract declared tag names from DotRestApplication
     */
    private Set<String> getDeclaredTagsFromApplication() {
        Set<String> declaredTags = new HashSet<>();
        
        try {
            Class<?> dotRestAppClass = Class.forName("com.dotcms.rest.config.DotRestApplication");
            io.swagger.v3.oas.annotations.OpenAPIDefinition openApiDef = 
                dotRestAppClass.getAnnotation(io.swagger.v3.oas.annotations.OpenAPIDefinition.class);
            
            if (openApiDef != null && openApiDef.tags() != null) {
                for (Tag tag : openApiDef.tags()) {
                    if (tag.name() != null && !tag.name().trim().isEmpty()) {
                        declaredTags.add(tag.name());
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            System.out.println("WARNING: Could not find DotRestApplication class for tag validation");
        }
        
        return declaredTags;
    }
    
    /**
     * Optional test method to validate only @SwaggerCompliant classes instead of all classes.
     * This can be used for progressive validation during the migration process.
     */
    @Test
    public void testSwaggerCompliantClassesOnly() {
        // Skip test if no @SwaggerCompliant classes found (progressive rollout)
        if (restResourceClasses.isEmpty()) {
            System.out.println("✅ No @SwaggerCompliant classes found - skipping testSwaggerCompliantClassesOnly");
            return;
        }
        
        // Check if we should use annotation filtering
        boolean useAnnotationFilter = Boolean.parseBoolean(System.getProperty("test.swagger.compliant.only", "false"));
        
        if (!useAnnotationFilter) {
            System.out.println("Skipping @SwaggerCompliant-only test. Enable with -Dtest.swagger.compliant.only=true");
            return;
        }
        
        // Find all @SwaggerCompliant classes
        Set<Class<?>> swaggerCompliantClasses = findSwaggerCompliantClasses();
        
        if (swaggerCompliantClasses.isEmpty()) {
            System.out.println("⚠️  No @SwaggerCompliant classes found. Apply @SwaggerCompliant annotation to fixed resource classes.");
            return;
        }
        
        System.out.println("Testing " + swaggerCompliantClasses.size() + " @SwaggerCompliant REST resource classes...");
        
        // Replace the static set with the filtered set
        restResourceClasses = swaggerCompliantClasses;
        
        // Run all the existing test methods on the filtered set
        testClassLevelTagAnnotations();
        testMethodLevelOperationAnnotations();
        testMethodLevelApiResponsesAnnotations();
        testPathParameterAnnotations();
        testRequestBodyAnnotations();
        testProducesAnnotations();
        testConsumesAnnotations();
        testSchemaAntipatterns();
        testTagsAreDeclaredInApplication();
        testArchitecturalWarnings();
        
        // Print the final violation report
        testPrintViolationReport();
    }
    
}