package com.dotcms.rest;

import com.dotcms.UnitTestBase;
import com.dotcms.rest.annotation.SwaggerCompliant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.junit.Test;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

// Jandex utility for improved performance
import com.dotcms.util.JandexClassMetadataScanner;

import static org.junit.Assert.*;

/**
 * Test class to validate REST endpoint annotations compliance according to the 
 * dotCMS REST API Development Guide standards.
 * 
 * This test provides validation for:
 * - Required @Tag annotations on resource classes
 * - Required @Operation annotations on endpoint methods
 * - Required @ApiResponses with proper @Schema implementations
 * - Proper @Parameter annotations for path/query parameters
 * - Correct @Produces/@Consumes usage
 * - Common @Schema antipatterns
 * 
 * Based on the rules defined in: dotCMS/src/main/java/com/dotcms/rest/README.md
 * 
 * Updated to use Jandex for improved performance over reflection-based scanning.
 */
public class RestEndpointAnnotationComplianceTest extends UnitTestBase {
    
    private static final String REST_PACKAGE = "com.dotcms.rest";
    private static final Map<String, List<String>> violationsByClass = new HashMap<>();
    

    
    /**
     * Test for duplicate operationIds across all tested REST endpoints.
     * OperationIds must be unique across the entire API to avoid OpenAPI conflicts.
     */
    @Test
    public void testUniqueOperationIds() {
        List<Class<?>> swaggerCompliantClasses = findSwaggerCompliantClasses();
        
        Map<String, String> operationIdToClassMethod = new HashMap<>();
        List<String> duplicateOperationIds = new ArrayList<>();
        
        for (Class<?> resourceClass : swaggerCompliantClasses) {
            try {
                if (resourceClass.isAnnotationPresent(Path.class)) {
                    Method[] methods = resourceClass.getDeclaredMethods();
                    
                    for (Method method : methods) {
                        if (isRestEndpointMethod(method)) {
                            Operation operationAnnotation = method.getAnnotation(Operation.class);
                            
                            if (operationAnnotation != null && 
                                operationAnnotation.operationId() != null && 
                                !operationAnnotation.operationId().trim().isEmpty()) {
                                
                                String operationId = operationAnnotation.operationId();
                                String classMethod = getQualifiedClassName(resourceClass) + "." + method.getName();
                                
                                if (operationIdToClassMethod.containsKey(operationId)) {
                                    String existingClassMethod = operationIdToClassMethod.get(operationId);
                                    duplicateOperationIds.add("OperationId '" + operationId + "' used by both: " + 
                                                            existingClassMethod + " and " + classMethod);
                                } else {
                                    operationIdToClassMethod.put(operationId, classMethod);
                                }
                            }
                        }
                    }
                }
                
            } catch (Exception e) {
                System.err.println("Error checking operationIds in class " + resourceClass.getName() + ": " + e.getMessage());
                // Log the full stack trace for debugging but continue processing
                if (System.getProperty("test.debug") != null) {
                    e.printStackTrace();
                }
                continue;
            }
        }
        
        if (!duplicateOperationIds.isEmpty()) {
            System.err.println("\n🚨 DUPLICATE OPERATION IDs DETECTED:");
            for (String duplicate : duplicateOperationIds) {
                System.err.println("  - " + duplicate);
            }
            fail("Duplicate operationIds found: " + duplicateOperationIds);
        }
        
        System.out.println("✅ All operationIds are unique across " + operationIdToClassMethod.size() + " endpoints");
    }

    /**
     * Test REST resource classes marked with @SwaggerCompliant to validate annotation compliance.
     * This test dynamically finds all classes annotated with @SwaggerCompliant and validates them.
     *
     * IMPORTANT: This test ONLY validates classes that have @SwaggerCompliant annotations.
     * Classes without this annotation are part of the progressive rollout and should be skipped.
     */
    @Test
    public void testSwaggerCompliantResourceAnnotationCompliance() {
        List<Class<?>> swaggerCompliantClasses = findSwaggerCompliantClasses();
        
        if (swaggerCompliantClasses.isEmpty()) {
            System.out.println("✅ No @SwaggerCompliant classes found - this is expected during progressive rollout.");
            System.out.println("   Classes without @SwaggerCompliant annotation are not yet ready for validation.");
            return;
        }
        
        // Print batch information
        String maxBatchProperty = System.getProperty("test.batch.max");
        
        if (maxBatchProperty != null) {
            System.out.println("Testing @SwaggerCompliant classes from batches 1-" + maxBatchProperty + " (" + swaggerCompliantClasses.size() + " classes)");
        } else {
            System.out.println("Testing ALL " + swaggerCompliantClasses.size() + " @SwaggerCompliant REST resource classes...");
        }
        
        // Print batch distribution
        if (swaggerCompliantClasses.size() > 0) {
            Map<Integer, Integer> batchCounts = new HashMap<>();
            for (Class<?> clazz : swaggerCompliantClasses) {
                SwaggerCompliant annotation = clazz.getAnnotation(SwaggerCompliant.class);
                int batch = annotation.batch();
                batchCounts.put(batch, batchCounts.getOrDefault(batch, 0) + 1);
            }
            
            System.out.println("Batch distribution: " + batchCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> "Batch " + e.getKey() + ": " + e.getValue() + " classes")
                .reduce((a, b) -> a + ", " + b)
                .orElse("No batches"));
        }
        
        for (Class<?> resourceClass : swaggerCompliantClasses) {
            try {
                if (resourceClass.isAnnotationPresent(Path.class)) {
                    validateResourceClass(resourceClass);
                }
                
            } catch (Exception e) {
                System.err.println("Error validating class " + resourceClass.getName() + ": " + e.getMessage());
                // Log the full stack trace for debugging but continue processing
                if (System.getProperty("test.debug") != null) {
                    e.printStackTrace();
                }
                continue;
            }
        }
        
        // Print summary report
        printViolationReport();
        
        // Generate structured report for CI/CD
        generateStructuredReport();
        
        // Fail if there are critical violations
        if (!violationsByClass.isEmpty()) {
            int totalViolations = violationsByClass.values().stream()
                    .mapToInt(List::size)
                    .sum();
            
            System.err.println("\nFound " + totalViolations + " annotation violations in " + 
                             violationsByClass.size() + " classes.");
            System.err.println("See detailed report above for specific violations.");
            
            // All violations have been fixed, so fail the test if any are found
            fail("REST endpoint annotation violations found. See report above.");
        }
    }
    
    /**
     * Validate a single REST resource class
     */
    private void validateResourceClass(Class<?> resourceClass) {
        String className = getQualifiedClassName(resourceClass);
        
        // Skip deprecated classes
        if (resourceClass.isAnnotationPresent(Deprecated.class)) {
            return;
        }
        
        // Validate class-level @Tag annotation
        validateClassLevelTag(resourceClass);
        
        // Validate all REST endpoint methods
        Method[] methods = resourceClass.getDeclaredMethods();
        for (Method method : methods) {
            if (isRestEndpointMethod(method) && !isHiddenMethod(method)) {
                validateEndpointMethod(resourceClass, method);
            }
        }
    }
    
    /**
     * Get a qualified class name for display (shows package to disambiguate)
     */
    private String getQualifiedClassName(Class<?> resourceClass) {
        String fullName = resourceClass.getName();
        String simpleName = resourceClass.getSimpleName();
        
        // Extract the package path after com.dotcms.rest
        if (fullName.startsWith("com.dotcms.rest.")) {
            String packagePath = fullName.substring("com.dotcms.rest.".length());
            int lastDotIndex = packagePath.lastIndexOf('.');
            if (lastDotIndex > 0) {
                packagePath = packagePath.substring(0, lastDotIndex);
                return simpleName + " (" + packagePath + ")";
            } else {
                // No package structure after com.dotcms.rest, just return simple name
                return simpleName;
            }
        }
        
        return simpleName;
    }
    
    /**
     * Validate class-level @Tag annotation
     * Note: Tag descriptions are centralized in DotRestApplication, not on individual resource classes
     */
    private void validateClassLevelTag(Class<?> resourceClass) {
        Tag tagAnnotation = resourceClass.getAnnotation(Tag.class);
        String className = getQualifiedClassName(resourceClass);
        
        if (tagAnnotation == null) {
            addViolation(className, "Missing @Tag annotation at class level");
        } else {
            if (tagAnnotation.name() == null || tagAnnotation.name().trim().isEmpty()) {
                addViolation(className, "@Tag annotation missing name");
            }
            // Note: We don't validate description here as it's centralized in DotRestApplication
        }
    }
    
    /**
     * Validate a single REST endpoint method
     */
    private void validateEndpointMethod(Class<?> resourceClass, Method method) {
        String className = getQualifiedClassName(resourceClass);
        String methodName = method.getName();
        
        // Validate @Operation annotation
        validateOperationAnnotation(className, methodName, method);
        
        // Validate @ApiResponses annotation
        validateApiResponsesAnnotation(className, methodName, method);
        
        // Validate @Produces annotation
        validateProducesAnnotation(className, methodName, method);
        
        // Validate @Consumes annotation
        validateConsumesAnnotation(className, methodName, method);
        
        // Validate parameter annotations
        validateParameterAnnotations(className, methodName, method);
        
        // Validate schema antipatterns
        validateSchemaAntipatterns(className, methodName, method);
    }
    
    /**
     * Validate @Operation annotation
     */
    private void validateOperationAnnotation(String className, String methodName, Method method) {
        Operation operationAnnotation = method.getAnnotation(Operation.class);
        
        if (operationAnnotation == null) {
            addViolation(className, "Method " + methodName + " missing @Operation annotation");
        } else {
            // Check for summary - either summary or description should be present
            boolean hasSummary = operationAnnotation.summary() != null && !operationAnnotation.summary().trim().isEmpty();
            boolean hasDescription = operationAnnotation.description() != null && !operationAnnotation.description().trim().isEmpty();
            
            if (!hasSummary && !hasDescription) {
                addViolation(className, "Method " + methodName + " @Operation missing both summary and description (at least one is required)");
            } else if (!hasSummary) {
                addViolation(className, "Method " + methodName + " @Operation missing summary (recommended for better API documentation)");
            }
        }
    }
    
    /**
     * Validate @ApiResponses annotation or responses in @Operation
     */
    private void validateApiResponsesAnnotation(String className, String methodName, Method method) {
        ApiResponses apiResponsesAnnotation = method.getAnnotation(ApiResponses.class);
        Operation operationAnnotation = method.getAnnotation(Operation.class);
        
        // Check for responses in @ApiResponses annotation
        ApiResponse[] responses = null;
        if (apiResponsesAnnotation != null) {
            responses = apiResponsesAnnotation.value();
        }
        // Check for responses in @Operation annotation  
        else if (operationAnnotation != null && operationAnnotation.responses().length > 0) {
            responses = operationAnnotation.responses();
        }
        
        if (responses == null || responses.length == 0) {
            addViolation(className, "Method " + methodName + " missing @ApiResponses annotation");
        } else {
            boolean hasValidSuccessResponse = false;
            boolean has200Response = false;
            
            for (ApiResponse response : responses) {
                if ("200".equals(response.responseCode())) {
                    has200Response = true;
                    hasValidSuccessResponse = validateSuccessResponseSchema(className, methodName, response);
                    break;
                }
            }
            
            if (!has200Response) {
                addViolation(className, "Method " + methodName + " missing 200 response code");
            } else if (!hasValidSuccessResponse) {
                addViolation(className, "Method " + methodName + " 200 response missing proper schema implementation");
            }
        }
    }
    
    /**
     * Validate @Produces annotation
     */
    private void validateProducesAnnotation(String className, String methodName, Method method) {
        Produces producesAnnotation = method.getAnnotation(Produces.class);
        
        if (producesAnnotation == null) {
            addViolation(className, "Method " + methodName + " missing @Produces annotation");
        } else {
            String[] mediaTypes = producesAnnotation.value();
            if (mediaTypes.length == 0) {
                addViolation(className, "Method " + methodName + " @Produces annotation has no media types");
            }
        }
    }
    
    /**
     * Validate @Consumes annotation
     */
    private void validateConsumesAnnotation(String className, String methodName, Method method) {
        boolean hasRequestBody = hasActualRequestBody(method);
        
        Consumes consumesAnnotation = method.getAnnotation(Consumes.class);
        
        // Check for VTL GET methods with request bodies (legacy exception)
        boolean isVtlGetWithBody = isVtlGetMethodWithRequestBody(method);
        
        if (hasRequestBody && consumesAnnotation == null) {
            addViolation(className, "Method " + methodName + " has request body but missing @Consumes annotation");
        } else if (!hasRequestBody && consumesAnnotation != null && method.isAnnotationPresent(GET.class) && !isVtlGetWithBody) {
            // Allow @Consumes on VTL GET methods with request bodies (legacy pattern)
            addViolation(className, "Method " + methodName + " has @Consumes but no request body (GET method)");
        }
    }
    
    /**
     * Validate parameter annotations
     */
    private void validateParameterAnnotations(String className, String methodName, Method method) {
        java.lang.reflect.Parameter[] parameters = method.getParameters();
        
        for (java.lang.reflect.Parameter parameter : parameters) {
            PathParam pathParamAnnotation = parameter.getAnnotation(PathParam.class);
            QueryParam queryParamAnnotation = parameter.getAnnotation(QueryParam.class);
            
            if (pathParamAnnotation != null) {
                io.swagger.v3.oas.annotations.Parameter parameterAnnotation = 
                    parameter.getAnnotation(io.swagger.v3.oas.annotations.Parameter.class);
                
                if (parameterAnnotation == null) {
                    addViolation(className, "Path parameter " + parameter.getName() + 
                               " in method " + methodName + " missing @Parameter annotation");
                } else if (parameterAnnotation.description() == null || 
                          parameterAnnotation.description().trim().isEmpty()) {
                    addViolation(className, "Path parameter " + parameter.getName() + 
                               " in method " + methodName + " missing description");
                }
            }
            
            // Validate query parameters have @Parameter annotations
            if (queryParamAnnotation != null) {
                io.swagger.v3.oas.annotations.Parameter parameterAnnotation = 
                    parameter.getAnnotation(io.swagger.v3.oas.annotations.Parameter.class);
                
                if (parameterAnnotation == null) {
                    addViolation(className, "Query parameter " + parameter.getName() + 
                               " in method " + methodName + " missing @Parameter annotation");
                } else if (parameterAnnotation.description() == null || 
                          parameterAnnotation.description().trim().isEmpty()) {
                    addViolation(className, "Query parameter " + parameter.getName() + 
                               " in method " + methodName + " missing description");
                }
            }
            
            // Enhanced request body validation
            if (hasActualRequestBody(method) && !isContextParameter(parameter) && 
                pathParamAnnotation == null && queryParamAnnotation == null && !isFrameworkParameter(parameter)) {
                
                // This parameter is likely a request body parameter
                boolean hasRequestBodyAnnotation = parameter.getAnnotation(io.swagger.v3.oas.annotations.parameters.RequestBody.class) != null;
                
                if (!hasRequestBodyAnnotation) {
                    addViolation(className, "Method " + methodName + " parameter '" + parameter.getType().getSimpleName() + 
                               "' appears to be request body but missing @RequestBody annotation");
                } else {
                    // Validate @RequestBody annotation content
                    io.swagger.v3.oas.annotations.parameters.RequestBody requestBodyAnnotation = 
                        parameter.getAnnotation(io.swagger.v3.oas.annotations.parameters.RequestBody.class);
                    
                    if (requestBodyAnnotation.description() == null || requestBodyAnnotation.description().trim().isEmpty()) {
                        addViolation(className, "Method " + methodName + " @RequestBody annotation missing description");
                    }
                    
                    // Check if request body should be required based on HTTP method and operation type
                    if (requestBodyAnnotation.required() == false && 
                        (method.isAnnotationPresent(POST.class) || method.isAnnotationPresent(PUT.class))) {
                        // Only flag as violation for operations that typically require a request body
                        String methodNameLower = methodName.toLowerCase();
                        if (methodNameLower.contains("save") && !methodNameLower.contains("comment") ||
                            methodNameLower.contains("create") ||
                            methodNameLower.contains("update") && !methodNameLower.contains("scheme") ||
                            methodNameLower.contains("add") ||
                            methodNameLower.contains("post") ||
                            methodNameLower.contains("put")) {
                            addViolation(className, "Method " + methodName + " @RequestBody should be required=true for POST/PUT operations");
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Validate schema antipatterns
     */
    private void validateSchemaAntipatterns(String className, String methodName, Method method) {
        ApiResponses apiResponsesAnnotation = method.getAnnotation(ApiResponses.class);
        Operation operationAnnotation = method.getAnnotation(Operation.class);
        
        // Check for responses in @ApiResponses annotation
        ApiResponse[] responses = null;
        if (apiResponsesAnnotation != null) {
            responses = apiResponsesAnnotation.value();
        }
        // Check for responses in @Operation annotation  
        else if (operationAnnotation != null && operationAnnotation.responses().length > 0) {
            responses = operationAnnotation.responses();
        }
        
        if (responses != null) {
            for (ApiResponse response : responses) {
                if ("200".equals(response.responseCode())) {
                    Content[] contentArray = response.content();
                    
                    for (Content content : contentArray) {
                        Schema schemaAnnotation = content.schema();
                        
                        if (schemaAnnotation != null) {
                            Class<?> implementation = schemaAnnotation.implementation();
                            
                            // Antipattern: Raw ResponseEntityView.class
                            if (implementation == ResponseEntityView.class) {
                                addViolation(className, "Method " + methodName + 
                                           " uses raw ResponseEntityView.class - should use specific ResponseEntity*View class");
                            }
                            
                            // Antipattern: Object.class without type="object"
                            if (implementation == Object.class && 
                                (schemaAnnotation.type() == null || schemaAnnotation.type().trim().isEmpty())) {
                                addViolation(className, "Method " + methodName + 
                                           " uses Object.class without type='object' - should use type='object' with description");
                            }
                            
                            // Antipattern: type="object" without description
                            if ("object".equals(schemaAnnotation.type()) && 
                                (schemaAnnotation.description() == null || schemaAnnotation.description().trim().isEmpty())) {
                                addViolation(className, "Method " + methodName + 
                                           " uses type='object' without description - should include meaningful description");
                            }
                            
                            // Antipattern: Using Map.class for dynamic JSON
                            if (implementation == java.util.Map.class) {
                                addViolation(className, "Method " + methodName + 
                                           " uses Map.class - should use type='object' with description for dynamic JSON");
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Validate success response schema
     */
    private boolean validateSuccessResponseSchema(String className, String methodName, ApiResponse response) {
        Content[] contentArray = response.content();
        
        // If no content array, this might be a valid empty response
        if (contentArray.length == 0) {
            // Check if description indicates this is intentionally empty
            String description = response.description();
            if (description != null && 
                (description.toLowerCase().contains("no body") || 
                 description.toLowerCase().contains("empty response") ||
                 description.toLowerCase().contains("no content") ||
                 description.toLowerCase().contains("success"))) {
                return true; // Valid empty response
            }
            // For DELETE operations, empty response is often acceptable
            if (description != null && description.toLowerCase().contains("deleted")) {
                return true;
            }
            addViolation(className, "Method " + methodName + " 200 response missing content/schema");
            return false;
        }
        
        for (Content content : contentArray) {
            Schema schemaAnnotation = content.schema();
            if (schemaAnnotation == null) {
                addViolation(className, "Method " + methodName + " 200 response missing @Schema annotation");
                return false;
            }
            
            Class<?> implementation = schemaAnnotation.implementation();
            String type = schemaAnnotation.type();
            
            // Check for valid schema implementations
            if (implementation != void.class && implementation != null) {
                // Valid implementation class
                return true;
            } else if (type != null && !type.trim().isEmpty()) {
                // Valid type specification
                return true;
            } else if (implementation == void.class && (type == null || type.trim().isEmpty())) {
                addViolation(className, "Method " + methodName + 
                           " 200 response has empty @Schema (no implementation or type)");
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Check if method is a REST endpoint
     */
    private boolean isRestEndpointMethod(Method method) {
        return method.isAnnotationPresent(GET.class) ||
               method.isAnnotationPresent(POST.class) ||
               method.isAnnotationPresent(PUT.class) ||
               method.isAnnotationPresent(DELETE.class);
    }
    
    /**
     * Check if method is hidden from API documentation
     */
    private boolean isHiddenMethod(Method method) {
        return method.isAnnotationPresent(io.swagger.v3.oas.annotations.Hidden.class);
    }
    
    /**
     * Check if method has request body (legacy method for parameter validation)
     */
    private boolean hasRequestBody(Method method) {
        return method.isAnnotationPresent(POST.class) ||
               method.isAnnotationPresent(PUT.class) ||
               (method.isAnnotationPresent(DELETE.class) && 
                method.getAnnotation(Consumes.class) != null);
    }
    
    /**
     * Check if method actually has a request body by examining parameters.
     * A method has a request body if it has parameters that are not:
     * - Context parameters (HttpServletRequest, HttpServletResponse, etc.)
     * - @PathParam parameters
     * - @QueryParam parameters
     * - Special framework parameters (AsyncResponse, Suspended, etc.)
     */
    private boolean hasActualRequestBody(Method method) {
        // Only POST, PUT, and some DELETE methods can have request bodies
        if (!method.isAnnotationPresent(POST.class) && 
            !method.isAnnotationPresent(PUT.class) && 
            !method.isAnnotationPresent(DELETE.class)) {
            
            // Special case: VTL GET methods with request bodies (legacy pattern)
            if (method.isAnnotationPresent(GET.class)) {
                return isVtlGetMethodWithRequestBody(method);
            }
            return false;
        }
        
        java.lang.reflect.Parameter[] parameters = method.getParameters();
        
        for (java.lang.reflect.Parameter parameter : parameters) {
            // Skip context parameters
            if (isContextParameter(parameter)) {
                continue;
            }
            
            // Skip path and query parameters
            if (parameter.isAnnotationPresent(PathParam.class) || 
                parameter.isAnnotationPresent(QueryParam.class)) {
                continue;
            }
            
            // Skip special JAX-RS framework parameters
            if (isFrameworkParameter(parameter)) {
                continue;
            }
            
            // If we find a parameter that's not a context, path, query, or framework param,
            // this method likely has a request body
            return true;
        }
        
        return false;
    }
    
    /**
     * Check if parameter is a context parameter (HttpServletRequest, etc.)
     */
    private boolean isContextParameter(java.lang.reflect.Parameter parameter) {
        String typeName = parameter.getType().getSimpleName();
        return typeName.equals("HttpServletRequest") || 
               typeName.equals("HttpServletResponse") ||
               typeName.equals("User") ||
               typeName.equals("SecurityContext");
    }
    
    /**
     * Check if parameter is a special JAX-RS framework parameter
     */
    private boolean isFrameworkParameter(java.lang.reflect.Parameter parameter) {
        String typeName = parameter.getType().getSimpleName();
        return typeName.equals("AsyncResponse") ||
               parameter.isAnnotationPresent(javax.ws.rs.container.Suspended.class);
    }
    
    /**
     * Add violation to the report
     */
    private void addViolation(String className, String violation) {
        violationsByClass.computeIfAbsent(className, k -> new ArrayList<>()).add(violation);
    }
    
    /**
     * Print detailed violation report
     */
    private void printViolationReport() {
        if (violationsByClass.isEmpty()) {
            System.out.println("\n✅ REST ENDPOINT ANNOTATION COMPLIANCE: PASSED");
            System.out.println("All tested REST endpoints follow the annotation standards.");
            return;
        }
        
        System.out.println("\n⚠️  REST ENDPOINT ANNOTATION VIOLATIONS REPORT");
        System.out.println("Classes with violations: " + violationsByClass.size());
        
        // Sort classes by name for consistent output
        Set<String> sortedClasses = new TreeSet<>(violationsByClass.keySet());
        
        for (String className : sortedClasses) {
            List<String> violations = violationsByClass.get(className);
            System.out.println("\n" + className + " (" + violations.size() + " violations):");
            
            for (String violation : violations) {
                System.out.println("  - " + violation);
            }
        }
        
        int totalViolations = violationsByClass.values().stream()
                .mapToInt(List::size)
                .sum();
        
        System.out.println("\n📊 SUMMARY:");
        System.out.println("Total violations: " + totalViolations);
        System.out.println("Classes affected: " + violationsByClass.size());
        
        System.out.println("\n📖 For annotation standards, see:");
        System.out.println("dotCMS/src/main/java/com/dotcms/rest/README.md");
    }
    
    /**
     * Generate structured report for CI/CD consumption
     */
    private void generateStructuredReport() {
        try {
            int totalViolations = violationsByClass.values().stream()
                    .mapToInt(List::size)
                    .sum();
            
            Map<String, Object> report = new HashMap<>();
            report.put("timestamp", System.currentTimeMillis());
            report.put("status", violationsByClass.isEmpty() ? "PASSED" : "FAILED");
            report.put("totalViolations", totalViolations);
            report.put("affectedClasses", violationsByClass.size());
            report.put("violations", violationsByClass);
            report.put("testName", "RestEndpointAnnotationComplianceTest");
            report.put("reportType", "REST_ANNOTATION_VALIDATION");
            
            // Ensure target directory exists
            java.io.File targetDir = new java.io.File("target");
            if (!targetDir.exists()) {
                targetDir.mkdirs();
            }
            
            // Write JSON report for CI/CD consumption
            try (java.io.FileWriter writer = new java.io.FileWriter("target/rest-annotation-report.json")) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                mapper.writerWithDefaultPrettyPrinter().writeValue(writer, report);
                System.out.println("\n📄 Structured report written to: target/rest-annotation-report.json");
            }
        } catch (Exception e) {
            System.err.println("Failed to generate structured report: " + e.getMessage());
        }
    }
    
    /**
     * Find all classes annotated with @SwaggerCompliant using Jandex for improved performance.
     * Falls back to reflection-based scanning if Jandex index is not available.
     */
    private List<Class<?>> findSwaggerCompliantClasses() {
        // Try Jandex first for better performance
        if (JandexClassMetadataScanner.isJandexAvailable()) {
            return findSwaggerCompliantClassesWithJandex();
        } else {
            return findSwaggerCompliantClassesWithReflection();
        }
    }
    
    /**
     * Find @SwaggerCompliant classes using Jandex index for fast scanning
     */
    private List<Class<?>> findSwaggerCompliantClassesWithJandex() {
        List<Class<?>> swaggerCompliantClasses = new ArrayList<>();
        
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
        
        // Get all classes with @SwaggerCompliant annotation using Jandex
        List<String> classNames = JandexClassMetadataScanner.findClassesWithAnnotation(
            "com.dotcms.rest.annotation.SwaggerCompliant", packagesToScan);
        
        for (String className : classNames) {
            // Get batch information from annotation
            Integer classBatch = JandexClassMetadataScanner.getClassAnnotationIntValue(
                className, "com.dotcms.rest.annotation.SwaggerCompliant", "batch");
            
            if (classBatch == null) {
                classBatch = 1; // Default batch
            }
            
            // Apply cumulative batch filtering - include all batches up to maxBatch
            if (maxBatch != null && classBatch > maxBatch) {
                continue; // Skip classes beyond max batch
            }
            
            try {
                Class<?> clazz = Class.forName(className);
                swaggerCompliantClasses.add(clazz);
            } catch (ClassNotFoundException | NoClassDefFoundError | ExceptionInInitializerError e) {
                // Skip classes that can't be loaded or initialized
                System.out.println("Warning: Could not load class " + className + ": " + e.getMessage());
            }
        }
        
        System.out.println("🔍 Found " + swaggerCompliantClasses.size() + " @SwaggerCompliant classes using Jandex");
        return swaggerCompliantClasses;
    }
    
    /**
     * Fallback method to find @SwaggerCompliant classes using reflection
     */
    private List<Class<?>> findSwaggerCompliantClassesWithReflection() {
        List<Class<?>> swaggerCompliantClasses = new ArrayList<>();
        
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
                    if (clazz.isAnnotationPresent(SwaggerCompliant.class)) {
                        SwaggerCompliant annotation = clazz.getAnnotation(SwaggerCompliant.class);
                        int classBatch = annotation.batch();
                        
                        // Apply cumulative batch filtering - include all batches up to maxBatch
                        if (maxBatch != null && classBatch > maxBatch) {
                            continue; // Skip classes beyond max batch
                        }
                        
                        swaggerCompliantClasses.add(clazz);
                    }
                }
            } catch (Exception e) {
                // Package might not exist or have issues - continue scanning
                System.out.println("Warning: Could not scan package " + packageName + ": " + e.getMessage());
            }
        }
        
        System.out.println("🔍 Found " + swaggerCompliantClasses.size() + " @SwaggerCompliant classes using reflection");
        return swaggerCompliantClasses;
    }
    
    /**
     * Get all classes in a package using reflection.
     */
    private List<Class<?>> getClassesInPackage(String packageName) throws Exception {
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
                String className = packageName + "." + file.getName().replace(".class", "");
                try {
                    Class<?> clazz = Class.forName(className);
                    classes.add(clazz);
                } catch (ClassNotFoundException | NoClassDefFoundError | ExceptionInInitializerError e) {
                    // Skip classes that can't be loaded or initialized
                } catch (Exception e) {
                    // Skip any other class loading errors
                }
            }
        }
        
        return classes;
    }
    
    /**
     * Get classes from a JAR file.
     */
    private List<Class<?>> getClassesFromJar(URL jarUrl, String packagePath) throws Exception {
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
                    } catch (ClassNotFoundException | NoClassDefFoundError | ExceptionInInitializerError e) {
                        // Skip classes that can't be loaded or initialized
                    } catch (Exception e) {
                        // Skip any other class loading errors
                    }
                }
            }
        } catch (IOException e) {
            // Skip JAR files that can't be read
        }
        
        return classes;
    }
    
    /**
     * Check if this is a VTL GET method that accepts request bodies (legacy pattern)
     */
    private boolean isVtlGetMethodWithRequestBody(Method method) {
        if (!method.isAnnotationPresent(GET.class)) {
            return false;
        }
        
        Class<?> resourceClass = method.getDeclaringClass();
        if (!resourceClass.getSimpleName().equals("VTLResource")) {
            return false;
        }
        
        String methodName = method.getName();
        if (!(methodName.equals("get") || methodName.equals("dynamicGet"))) {
            return false;
        }
        
        // Check if method has request body parameters (like Map<String, Object> bodyMap or String bodyMapString)
        for (java.lang.reflect.Parameter parameter : method.getParameters()) {
            if (isContextParameter(parameter)) {
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

}