# dotCMS REST API Development Guide

This guide provides comprehensive documentation for developing REST endpoints in dotCMS using JAX-RS and Swagger/OpenAPI standards.

## 📋 Table of Contents

- [REST Endpoint Patterns](#rest-endpoint-patterns)
- [Swagger/OpenAPI Documentation Standards](#swaggeropenapi-documentation-standards)
- [Schema Implementation Rules](#schema-implementation-rules)
- [🚨 CRITICAL: @Schema Best Practices for Dynamic JSON](#-critical-schema-best-practices-for-dynamic-json)
- [Response Entity View Classes](#response-entity-view-classes)
- [Media Type Standards](#media-type-standards)
- [Parameter Documentation](#parameter-documentation)
- [Response Status Codes](#response-status-codes)
- [Deprecation Documentation](#deprecation-documentation)
- [Error Handling Patterns](#error-handling-patterns)
- [Examples and Best Practices](#examples-and-best-practices)
- [Summary Checklist](#summary-checklist)

---

## REST Endpoint Patterns

### Basic JAX-RS Resource Structure

```java
@Path("/v1/myresource")
@Tag(name = "Resource Category")
public class MyResource {
    private final WebResource webResource = new WebResource();
    
    @Operation(
        summary = "Get resource by ID",
        description = "Retrieves a specific resource using its identifier"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Resource found successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityMyResourceView.class))),
        @ApiResponse(responseCode = "404", 
                    description = "Resource not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized access",
                    content = @Content(mediaType = "application/json"))
    })
    @GET @Path("/{id}") @Produces(MediaType.APPLICATION_JSON) @NoCache
    public ResponseEntityMyResourceView getById(@Context HttpServletRequest request,
                           @Parameter(description = "Resource identifier", required = true)
                           @PathParam("id") String id) {
        // ALWAYS initialize request context
        InitDataObject initData = webResource.init(request, response, true);
        User user = initData.getUser();
        
        // Business logic
        return new ResponseEntityMyResourceView(result);
    }
}
```

### Request Context Initialization

**ALWAYS initialize WebResource context:**

```java
// For authenticated endpoints
InitDataObject initData = webResource.init(request, response, true);
User user = initData.getUser();

// For public endpoints  
InitDataObject initData = webResource.init(request, response, false);

// Using builder pattern (preferred for complex initialization)
InitDataObject initData = new WebResource.InitBuilder(webResource)
    .requiredBackendUser(true)
    .requiredFrontendUser(false)
    .requestAndResponse(request, response)
    .rejectWhenNoUser(true)
    .init();
```

---

## Swagger/OpenAPI Documentation Standards

### REQUIRED Annotations

#### Class Level - ALWAYS Required
```java
@Tag(name = "Category", description = "Brief description")
```

#### Method Level - ALL Methods MUST Have These
```java
@Operation(summary = "Brief action", description = "Detailed explanation")
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Success description",
                content = @Content(mediaType = "application/json",
                                  schema = @Schema(implementation = AppropriateClass.class))),
    @ApiResponse(responseCode = "4xx/5xx", description = "Error description",
                content = @Content(mediaType = "application/json"))
})
```

#### Parameters - ALL Must Be Documented
```java
// Path parameters
@Parameter(description = "Parameter purpose", required = true)
@PathParam("id") String id

// Query parameters  
@Parameter(description = "Filter criteria", required = false)
@QueryParam("filter") String filter

// Request bodies
@RequestBody(description = "Body purpose", required = true,
           content = @Content(schema = @Schema(implementation = FormClass.class)))
```

---

## 🚨 Schema Implementation Rules

**The @Schema implementation must match what the method actually returns!**

### ✅ Pattern 1: Methods Returning ResponseEntity*View Wrappers
```java
// Method that returns wrapped response:
public ResponseEntityUserView getUser() {
    return new ResponseEntityUserView(user);
}

// OR method that wraps in Response.ok():
public Response getUser() {
    return Response.ok(new ResponseEntityUserView(user)).build();
}

// ✅ CORRECT @Schema - matches the wrapper class
@Schema(implementation = ResponseEntityUserView.class)
@Schema(implementation = ResponseEntityWorkflowStepsView.class)
@Schema(implementation = ResponseEntityContentTypeListView.class)
```

### ✅ Pattern 2: Methods Returning Unwrapped Collections/Maps
```java
// Method that returns unwrapped Map directly:
public Map<String, RestLanguage> list() {
    return languageMap;  // Direct Map return, no wrapper
}

// Method that returns unwrapped List directly:
public List<ContentType> getTypes() {
    return contentTypes;  // Direct List return, no wrapper
}

// ✅ CORRECT @Schema - use specific view classes for typed collections
@Schema(implementation = MapStringRestLanguageView.class)  // For Map<String, RestLanguage>
@Schema(implementation = ContentTypeListView.class)        // For List<ContentType> - create if needed

// Pattern for unwrapped lists:
// Create: public class ContentTypeListView extends ArrayList<ContentType> {
//     public ContentTypeListView() { super(); }
//     public ContentTypeListView(List<ContentType> list) { super(list); }
// }
```

### ✅ Pattern 3: Generic Utility Classes (When No Specific Class Exists)
```java
// Method that returns generic wrapped response:
public Response getSomething() {
    return Response.ok(new ResponseEntityView<>(mapData)).build();
}

// ✅ CORRECT @Schema - use appropriate generic class
@Schema(implementation = ResponseEntityMapView.class)      // For Map<String, Object> wrapped
@Schema(implementation = ResponseEntityListView.class)     // For List<T> wrapped
@Schema(implementation = ResponseEntityBooleanView.class)  // For boolean operations
@Schema(implementation = ResponseEntityStringView.class)   // For string responses
@Schema(implementation = ResponseEntityCountView.class)    // For count operations
```

---

## 🚨 CRITICAL: @Schema Best Practices for Dynamic JSON

### ✅ PREFERRED: Use `type = "object"` for Dynamic JSON
```java
// ✅ BEST PRACTICE - Descriptive schema with example
@Schema(type = "object", 
        description = "User information containing profile data and preferences",
        example = """
        {
          "userId": "12345",
          "name": "John Doe", 
          "email": "john@example.com",
          "preferences": {
            "theme": "dark",
            "notifications": true
          }
        }
        """)

// ✅ GOOD - At minimum provide a description
@Schema(type = "object", description = "Dynamic JSON response with operation results")
```

### ❌ AVOID: `implementation = Object.class`
```java
// ❌ DEPRECATED - Provides no meaningful API documentation
@Schema(implementation = Object.class)

// ❌ PROBLEM - Also avoid Map.class or extensions of Map<String,Object>
@Schema(implementation = Map.class)
@Schema(implementation = HashMap.class)
@Schema(implementation = LinkedHashMap.class)
```

### 📝 Why `type = "object"` is Preferred

1. **Better Documentation**: Descriptions and examples help API consumers
2. **OpenAPI Compatibility**: Works consistently across all OpenAPI tools
3. **No Extra Classes**: Avoids creating unnecessary ResponseEntity wrapper classes
4. **Flexibility**: Can include examples, descriptions, and property hints
5. **Consistency**: Map.class and Object.class are handled identically in OpenAPI

### 🎯 When to Use Each Approach

| Return Type | Recommended @Schema | Example |
|-------------|-------------------|---------|
| `ResponseEntityUserView` | `implementation = ResponseEntityUserView.class` | Wrapped specific response |
| `ResponseEntity<List<T>>` | `implementation = ResponseEntityListView.class` | Wrapped list response |
| `List<MyEntity>` | `implementation = MyEntityListView.class` | Unwrapped typed list |
| `Map<String, MyEntity>` | `implementation = MapStringMyEntityView.class` | Unwrapped typed map |
| `Map<String, Object>` | `type = "object", description = "..."` | Dynamic key-value data |
| `JSONObject` | `type = "object", description = "..."` | Complex nested JSON |
| External API response | `type = "object", description = "...", example = "..."` | AI/third-party APIs |

### 🔍 How to Determine the Correct @Schema

1. **Check the method's return statement** - what does it actually return?
2. **Wrapped returns (ResponseEntity<T>)** → Use the specific ResponseEntity*View class
3. **Unwrapped typed collections (List<MyEntity>)** → Create/use MyEntityListView extends List<MyEntity>
4. **Unwrapped typed maps (Map<String, MyEntity>)** → Create/use MapStringMyEntityView extends Map<String, MyEntity>
5. **Dynamic JSON (Map<String, Object>, JSONObject)** → Use type = "object" with description
6. **No specific class exists** → Use generic ResponseEntity*View utilities

---

## Response Entity View Classes

### Core Utility Classes (Use When No Specific Domain Class Exists)
```java
ResponseEntityBooleanView.class      // For boolean operations
ResponseEntityStringView.class       // For string responses  
ResponseEntityListView.class         // For list/array responses
ResponseEntityMapView.class          // For key-value data
ResponseEntityCountView.class        // For count operations
ResponseEntityJobView.class          // For job status
ResponseEntityEndpointView.class     // For endpoint data
```

### Domain-Specific Classes (80+ Available)
```java
// Authentication & Users
ResponseEntityUserView.class, ResponseEntityApiTokenView.class
ResponseEntityLoginFormView.class, ResponseEntityJwtView.class

// Content Management  
ResponseEntityContentletView.class, ResponseEntityContentTypeListView.class
ResponseEntityContentTypeOperationView.class

// Workflow
ResponseEntityWorkflowSchemeView.class, ResponseEntityWorkflowStepView.class
ResponseEntityWorkflowActionView.class, ResponseEntityBulkActionView.class

// Containers & Templates
ResponseEntityContainerView.class, ResponseEntityTemplateView.class

// Categories & Tags
ResponseEntityCategoryView.class, ResponseEntityTagView.class

// Pages & Navigation
ResponseEntityPageView.class, ResponseEntityPageOperationView.class
ResponseEntityNavigationView.class

// Experiments & Personalization
ResponseEntityExperimentView.class, ResponseEntityPersonalizationView.class

// Apps & Health
ResponseEntityAppView.class, ResponseEntityHealthStatusView.class
```

---

## Media Type Standards

### @Produces Annotation Rules
```java
// @Produces - ALWAYS specify at method level (not class level)
@Produces(MediaType.APPLICATION_JSON)                    // Single type
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})  // Multiple types

// @Consumes - ONLY on endpoints that accept request bodies
@GET                                    // NO @Consumes (no body)
@POST @Consumes(MediaType.APPLICATION_JSON)            // YES @Consumes (has body)
@PUT @Consumes(MediaType.APPLICATION_JSON)             // YES @Consumes (has body)
@DELETE                                 // Usually NO @Consumes (no body)
@DELETE @Consumes(MediaType.APPLICATION_JSON)          // Only if body required

// Special cases - Some GET endpoints accept bodies (non-standard but exists)
@GET @Consumes(MediaType.APPLICATION_JSON)             // Only if GET accepts body
```

### Standard Media Types
```java
// Primary format
MediaType.APPLICATION_JSON

// Multiple response formats
{MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN}

// Streaming responses
{MediaType.APPLICATION_JSON}

// File uploads
MediaType.MULTIPART_FORM_DATA
```

---

## Parameter Documentation

### Path Parameters vs Query Parameters
```java
// Path parameters - for URL path segments
@Path("/resource/{id}")
public Response getById(@PathParam("id") String id) {...}

// Query parameters - for URL query strings (?param=value)
@QueryParam("filter") String filter        // /resource?filter=value
@QueryParam("allUsers") Boolean allUsers   // /resource?allUsers=true

// ❌ WRONG - @PathParam without corresponding @Path placeholder
@Path("/resource")  // No {params} placeholder
public Response get(@PathParam("params") String params) {...}

// ✅ CORRECT - @QueryParam for optional filtering
@Path("/resource")
public Response get(@QueryParam("filter") String filter) {...}
```

### Parameter Annotation Examples
```java
// Required path parameter
@Parameter(description = "Unique identifier of the resource", required = true)
@PathParam("id") String id

// Optional query parameter with default
@Parameter(description = "Number of items per page", required = false)
@QueryParam("perPage") @DefaultValue("20") int perPage

// Request body with schema
@RequestBody(description = "Resource data to create", 
           required = true,
           content = @Content(schema = @Schema(implementation = MyResourceForm.class)))
MyResourceForm form
```

---

## Response Status Codes

### Standard Success Codes
```java
// Common success responses
return Response.ok(entity).build();                    // 200 OK
return Response.status(201).entity(entity).build();    // 201 Created
return Response.noContent().build();                   // 204 No Content

// Standard error responses (document in @ApiResponses)
// 400 Bad Request - Invalid input
// 401 Unauthorized - Authentication required  
// 403 Forbidden - Insufficient permissions
// 404 Not Found - Resource not found
// 500 Internal Server Error - Server error
```

### Complete @ApiResponses Example
```java
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", 
                description = "Operation completed successfully",
                content = @Content(mediaType = "application/json",
                                  schema = @Schema(implementation = ResponseEntityView.class))),
    @ApiResponse(responseCode = "400", 
                description = "Bad request - invalid input parameters",
                content = @Content(mediaType = "application/json")),
    @ApiResponse(responseCode = "401", 
                description = "Unauthorized - authentication required",
                content = @Content(mediaType = "application/json")),
    @ApiResponse(responseCode = "403", 
                description = "Forbidden - insufficient permissions",
                content = @Content(mediaType = "application/json")),
    @ApiResponse(responseCode = "404", 
                description = "Resource not found",
                content = @Content(mediaType = "application/json")),
    @ApiResponse(responseCode = "500", 
                description = "Internal server error",
                content = @Content(mediaType = "application/json"))
})
```

---

## Deprecation Documentation

### Deprecating Resources/Methods
```java
// Class level deprecation (legacy resources)
In DotRestApplication where @Tags are declared

@Tag(name = "Category", description = "Legacy endpoints (deprecated - use v2 instead)")

// Method level deprecation
@Operation(
    summary = "Action name (deprecated)",
    description = "Method description. This endpoint is deprecated - use v2 CategoryResource instead.",
    deprecated = true
)

// Standard deprecation response descriptions
@ApiResponse(responseCode = "200", description = "Success (deprecated endpoint)")
```

---

## Error Handling Patterns

### dotCMS Exception Hierarchy
```java
// Use specific dotCMS exceptions
try {
    riskyOperation();
} catch (SQLException e) {
    Logger.error(this, "Database operation failed: " + e.getMessage(), e);
    throw new DotDataException("Failed to process request", e);
} catch (SecurityException e) {
    throw new DotSecurityException("Access denied", e);
}

// Exception types: DotDataException, DotSecurityException, DotRuntimeException, DotStateException
```

### Response Error Patterns
```java
// Standard error response patterns
return ExceptionMapperUtil.createResponse(e, Response.Status.BAD_REQUEST);
return ExceptionMapperUtil.createResponse(e, Response.Status.NOT_FOUND);
return ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);

// Custom error responses
return Response.status(Response.Status.BAD_REQUEST)
    .entity(Map.of("error", "Invalid request parameters"))
    .build();
```

---

## Examples and Best Practices

### Complete Resource Example
```java
@Path("/v1/example")
@Tag(name = "Example")
public class ExampleResource {
    private final WebResource webResource = new WebResource();
    
    @Operation(
        summary = "Get example by ID",
        description = "Retrieves a specific example resource by its unique identifier"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Example retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityExampleView.class))),
        @ApiResponse(responseCode = "404", 
                    description = "Example not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized access",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public ResponseEntityExampleView getById(
            @Context HttpServletRequest request,
            @Parameter(description = "Example identifier", required = true)
            @PathParam("id") String id) {
        
        // Initialize request context
        InitDataObject initData = webResource.init(request, response, true);
        User user = initData.getUser();
        
        // Business logic
        Example example = APILocator.getExampleAPI().find(id, user);
        
        return new ResponseEntityExampleView(example);
    }
    
    @Operation(
        summary = "Create new example",
        description = "Creates a new example resource with the provided data"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", 
                    description = "Example created successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityExampleView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Invalid request data",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized access",
                    content = @Content(mediaType = "application/json"))
    })
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @NoCache
    public Response create(
            @Context HttpServletRequest request,
            @RequestBody(description = "Example data to create", 
                       required = true,
                       content = @Content(schema = @Schema(implementation = ExampleForm.class)))
            ExampleForm form) {
        
        InitDataObject initData = webResource.init(request, response, true);
        User user = initData.getUser();
        
        // Business logic
        Example example = APILocator.getExampleAPI().create(form, user);
        
        return Response.status(201)
            .entity(new ResponseEntityExampleView(example))
            .build();
    }
}
```

---

## Summary Checklist

### ✅ Required Elements for Every REST Endpoint

- [ ] **Class Level**: `@Tag(name = "...", description = "...")`
- [ ] **Method Level**: `@Operation(summary = "...", description = "...")`
- [ ] **Responses**: Complete `@ApiResponses` with proper `@Schema` implementations
- [ ] **Parameters**: All path/query parameters documented with `@Parameter`
- [ ] **Request Bodies**: All bodies documented with `@RequestBody` and schema
- [ ] **Media Types**: `@Produces` at method level, `@Consumes` only when needed
- [ ] **Context**: WebResource initialization in every method
- [ ] **Error Handling**: Proper exception handling and response codes
- [ ] **Schema**: Correct schema based on actual return type (wrapped vs unwrapped)

### ❌ Common Antipatterns to Avoid

- [ ] Missing `@Schema` implementations
- [ ] Using raw `ResponseEntityView.class` as schema
- [ ] **Using `@Schema(implementation = Object.class)` instead of `type = "object"`**
- [ ] **Using `@Schema(implementation = Map.class)` for dynamic JSON**
- [ ] **Missing descriptions on `type = "object"` schemas**
- [ ] Using Object.class for wrapped responses
- [ ] Using wrapper schemas for unwrapped returns
- [ ] Missing parameter documentation
- [ ] `@Consumes` on GET endpoints without request bodies
- [ ] Missing WebResource initialization
- [ ] Inconsistent error response documentation

This guide should serve as the comprehensive reference for all REST API development in dotCMS!