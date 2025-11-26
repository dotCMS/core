# REST API Development Patterns

## JAX-RS Endpoint Pattern (Required)

### Standard Resource Structure
```java
@Path("/v1/myresource")
@ApplicationScoped
public class MyResource {
    private final WebResource webResource = new WebResource();
    
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public Response getById(
        @Context HttpServletRequest request,
        @Context HttpServletResponse response,
        @PathParam("id") String id
    ) {
        // ALWAYS initialize request context
        InitDataObject initData = webResource.init(request, response, true);
        User user = initData.getUser();
        
        try {
            // Input validation
            if (!UtilMethods.isSet(id)) {
                return ResponseUtil.mapExceptionResponse(
                    new DotDataException("ID is required")
                );
            }
            
            // Business logic
            MyService service = APILocator.getMyService();
            MyEntity entity = service.findById(id, user);
            
            // Response
            return Response.ok(new ResponseEntityView<>(entity)).build();
            
        } catch (Exception e) {
            Logger.error(this, "Error retrieving entity: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }
    
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public Response create(
        @Context HttpServletRequest request,
        @Context HttpServletResponse response,
        MyEntityForm form
    ) {
        InitDataObject initData = webResource.init(request, response, true);
        User user = initData.getUser();
        
        try {
            // Form validation
            if (!form.isValid()) {
                return ResponseUtil.mapExceptionResponse(
                    new DotDataException("Invalid form data")
                );
            }
            
            // Business logic
            MyService service = APILocator.getMyService();
            MyEntity entity = service.create(form, user);
            
            return Response.ok(new ResponseEntityView<>(entity)).build();
            
        } catch (Exception e) {
            Logger.error(this, "Error creating entity: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }
}
```

### Required Patterns
- **WebResource.init()**: ALWAYS initialize request context
- **User context**: Extract user from InitDataObject
- **Input validation**: Validate all parameters
- **APILocator**: Access services via APILocator
- **Exception handling**: Use ResponseUtil.mapExceptionResponse()
- **Logging**: Use Logger with proper context

## Request/Response Patterns

### Request Context Initialization
```java
// For authenticated endpoints
InitDataObject initData = webResource.init(request, response, true);
User user = initData.getUser();

// For public endpoints
InitDataObject initData = webResource.init(request, response, false);
```

### Input Validation Pattern
```java
// Parameter validation
if (!UtilMethods.isSet(id) || !id.matches("^[a-zA-Z0-9\\-_]+$")) {
    return ResponseUtil.mapExceptionResponse(
        new DotDataException("Invalid ID format")
    );
}

// Form validation
if (!form.isValid()) {
    return ResponseUtil.mapExceptionResponse(
        new DotDataException("Invalid form data: " + form.getValidationErrors())
    );
}

// Business rule validation
if (!securityAPI.hasPermission(user, entity, PermissionLevel.READ)) {
    return ResponseUtil.mapExceptionResponse(
        new DotSecurityException("Access denied")
    );
}
```

### Response Patterns
```java
// Success response
return Response.ok(new ResponseEntityView<>(entity)).build();

// Success with pagination
PaginationResult<MyEntity> result = service.findPaginated(query, user);
return Response.ok(new ResponseEntityView<>(result)).build();

// Error response
return ResponseUtil.mapExceptionResponse(exception);

// Created response
return Response.status(Response.Status.CREATED)
    .entity(new ResponseEntityView<>(entity))
    .build();
```

## Form Object Pattern

### Form Validation
```java
@JsonIgnoreProperties(ignoreUnknown = true)
public class MyEntityForm {
    private String name;
    private String description;
    private boolean enabled;
    
    // Getters and setters
    
    public boolean isValid() {
        return UtilMethods.isSet(name) && 
               name.length() <= 255 &&
               (description == null || description.length() <= 1000);
    }
    
    public List<String> getValidationErrors() {
        List<String> errors = new ArrayList<>();
        
        if (!UtilMethods.isSet(name)) {
            errors.add("Name is required");
        }
        
        if (name != null && name.length() > 255) {
            errors.add("Name must be 255 characters or less");
        }
        
        if (description != null && description.length() > 1000) {
            errors.add("Description must be 1000 characters or less");
        }
        
        return errors;
    }
}
```

## Security Patterns

### Authentication Check
```java
// Ensure user is authenticated
if (user == null || !user.isLoggedIn()) {
    return ResponseUtil.mapExceptionResponse(
        new DotSecurityException("Authentication required")
    );
}
```

### Permission Validation
```java
// Check specific permissions
if (!securityAPI.hasPermission(user, entity, PermissionLevel.READ)) {
    return ResponseUtil.mapExceptionResponse(
        new DotSecurityException("Read permission required")
    );
}

// Check admin permissions
if (!user.isAdmin()) {
    return ResponseUtil.mapExceptionResponse(
        new DotSecurityException("Admin permission required")
    );
}
```

### Input Sanitization
```java
// Sanitize user input
String sanitizedInput = HTMLUtils.htmlEscape(userInput);

// Validate against patterns
if (!userInput.matches("^[a-zA-Z0-9\\s\\-_\\.]+$")) {
    return ResponseUtil.mapExceptionResponse(
        new DotSecurityException("Invalid input format")
    );
}
```

## Error Handling Patterns

### Exception Mapping
```java
try {
    // Business logic
} catch (DotDataException e) {
    Logger.error(this, "Data error: " + e.getMessage(), e);
    return ResponseUtil.mapExceptionResponse(e);
} catch (DotSecurityException e) {
    Logger.warn(this, "Security error: " + e.getMessage(), e);
    return ResponseUtil.mapExceptionResponse(e);
} catch (Exception e) {
    Logger.error(this, "Unexpected error: " + e.getMessage(), e);
    return ResponseUtil.mapExceptionResponse(
        new DotRuntimeException("Internal server error", e)
    );
}
```

### Custom Error Responses
```java
// Custom validation error
return Response.status(Response.Status.BAD_REQUEST)
    .entity(new ResponseEntityView<>(
        Collections.singletonMap("error", "Invalid request data")
    ))
    .build();

// Not found error
return Response.status(Response.Status.NOT_FOUND)
    .entity(new ResponseEntityView<>(
        Collections.singletonMap("error", "Entity not found")
    ))
    .build();
```

## OpenAPI Integration

### Automatic Documentation
- **OpenAPI spec**: Auto-generated at `/WEB-INF/openapi/openapi.yaml`
- **Pre-commit hook**: Regenerates spec on REST API changes
- **Merge strategy**: Uses "ours" strategy for merge conflicts

### Annotation Examples
```java
@Operation(summary = "Get entity by ID", description = "Retrieves a specific entity")
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Success"),
    @ApiResponse(responseCode = "404", description = "Entity not found"),
    @ApiResponse(responseCode = "403", description = "Access denied")
})
@GET
@Path("/{id}")
public Response getById(@Parameter(description = "Entity ID") @PathParam("id") String id) {
    // Implementation
}
```

## Testing Patterns

### Integration Test Structure
```java
@RunWith(DataProviderRunner.class)
public class MyResourceIntegrationTest {
    
    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }
    
    @Test
    public void testGetById_Success() throws Exception {
        // Arrange
        User user = new UserDataGen().nextPersisted();
        MyEntity entity = new MyEntityDataGen().nextPersisted();
        
        // Act
        Response response = given()
            .auth().basic(user.getEmailAddress(), "admin")
            .when()
            .get("/api/v1/myresource/" + entity.getId())
            .then()
            .statusCode(HttpStatus.SC_OK)
            .extract().response();
        
        // Assert
        MyEntity result = response.jsonPath().getObject("entity", MyEntity.class);
        assertEquals(entity.getId(), result.getId());
    }
    
    @Test
    public void testGetById_NotFound() throws Exception {
        // Test not found scenario
        given()
            .auth().basic("admin@dotcms.com", "admin")
            .when()
            .get("/api/v1/myresource/nonexistent")
            .then()
            .statusCode(HttpStatus.SC_NOT_FOUND);
    }
}
```

## Common REST Patterns

### Pagination
```java
@GET
@Path("/")
public Response list(
    @QueryParam("page") @DefaultValue("1") int page,
    @QueryParam("per_page") @DefaultValue("20") int perPage,
    @QueryParam("filter") String filter,
    @QueryParam("orderBy") String orderBy,
    @Context HttpServletRequest request
) {
    // Validate pagination parameters
    if (page < 1 || perPage < 1 || perPage > 100) {
        return ResponseUtil.mapExceptionResponse(
            new DotDataException("Invalid pagination parameters")
        );
    }
    
    // Build query
    MyEntityQuery query = MyEntityQuery.builder()
        .page(page)
        .perPage(perPage)
        .filter(filter)
        .orderBy(orderBy)
        .build();
    
    // Execute query
    PaginationResult<MyEntity> result = service.findPaginated(query, user);
    
    return Response.ok(new ResponseEntityView<>(result)).build();
}
```

### Bulk Operations
```java
@POST
@Path("/bulk")
public Response bulkOperation(
    @Context HttpServletRequest request,
    BulkOperationForm form
) {
    InitDataObject initData = webResource.init(request, response, true);
    User user = initData.getUser();
    
    // Validate bulk operation
    if (form.getIds().size() > 100) {
        return ResponseUtil.mapExceptionResponse(
            new DotDataException("Bulk operation limited to 100 items")
        );
    }
    
    // Process in transaction
    return LocalTransaction.wrapReturn(() -> {
        List<MyEntity> results = new ArrayList<>();
        
        for (String id : form.getIds()) {
            MyEntity entity = service.processEntity(id, form.getOperation(), user);
            results.add(entity);
        }
        
        return Response.ok(new ResponseEntityView<>(results)).build();
    });
}
```

## Location Information
- **REST endpoints**: Located in `com.dotcms.rest.*` packages
- **WebResource**: Found in `com.dotcms.rest.WebResource`
- **ResponseUtil**: Located in `com.dotcms.rest.ResponseUtil`
- **Forms**: Typically in same package as resource or `*.form` subpackage
- **Integration tests**: Located in `dotcms-integration` module