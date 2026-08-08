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

> Forms are **inbound request-binding beans**, not immutable value objects. They stay as mutable
> classes (Jackson binds them, then the resource validates and consumes them) — they do **not** use
> records or the Immutables library, so the record migration below does not apply to them.

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

## View Object Pattern (Java Records)

REST view objects are **Java records** — not the Immutables `@Value.Immutable` `Abstract*` interface
style, and not the `Immutable*`-prefix class style from
[JAVA_STANDARDS.md](JAVA_STANDARDS.md#immutable-objects-critical-pattern). The record *is* the public
type: there is no generated `Abstract*`/`Immutable*` indirection, no `@Value.Style`, and no
`passAnnotations`. Records are implicitly `final` and their components are `final`, so immutability
comes for free.

Key points when migrating a view object off Immutables:

- **The record is the type.** Drop `@Value.Immutable`, `@Value.Style`, the `Abstract*` interface, and
  the `@JsonDeserialize(as = …)` redirect. Reference the record directly everywhere.
- **`@Schema` goes straight on the record and its components.** Because the record is a concrete,
  directly-annotated type, the class-level `@Schema(description=…)` is emitted into `openapi.yaml`
  without the `passAnnotations` workaround Immutables required. Component-level `@Schema` carries over
  the same way it did on interface accessors.
- **Jackson uses native record support (Jackson 2.12+).** Deserialization runs through the canonical
  constructor — no `@JsonCreator` needed. **Gotcha:** Jackson needs the constructor parameter names,
  so the module must be compiled with `-parameters` (preferred); otherwise fall back to
  `@JsonProperty` on each component.
- **Builder is hand-written for now.** Records don't generate a builder, and many view objects have
  enough fields that positional construction is error-prone. Provide a small static nested `Builder`.
  *Going forward,* prefer the [`@RecordBuilder`](https://github.com/Randgalt/record-builder)
  annotation processor to generate the builder instead of maintaining it by hand — the call site
  (`FileAssetView.builder()…build()`) stays identical, so the swap is non-breaking.

```java
@Schema(description = "File asset view returned after a save")
public record FileAssetView(
        @Schema(description = "Asset identifier") String identifier,
        @Schema(description = "File size in bytes") long fileSize
) {
    // Hand-written builder for now; replace with @RecordBuilder on the record going forward.
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String identifier;
        private long fileSize;

        public Builder identifier(String identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder fileSize(long fileSize) {
            this.fileSize = fileSize;
            return this;
        }

        public FileAssetView build() {
            return new FileAssetView(identifier, fileSize);
        }
    }
}
// FileAssetView.builder().identifier(id).fileSize(size).build();
```

The same record + hand-written-builder approach applies to **immutable query / parameter objects**
(e.g. `MyEntityQuery` used in pagination below). These are the other place the Immutables builder
was pulling its weight, and records cover them cleanly — set the builder defaults to match the
endpoint's `@DefaultValue`s:

```java
public record MyEntityQuery(int page, int perPage, String filter, String orderBy) {
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private int page = 1;       // matches @DefaultValue("1")
        private int perPage = 20;   // matches @DefaultValue("20")
        private String filter;
        private String orderBy;

        public Builder page(int page) { this.page = page; return this; }
        public Builder perPage(int perPage) { this.perPage = perPage; return this; }
        public Builder filter(String filter) { this.filter = filter; return this; }
        public Builder orderBy(String orderBy) { this.orderBy = orderBy; return this; }

        public MyEntityQuery build() {
            return new MyEntityQuery(page, perPage, filter, orderBy);
        }
    }
}
// MyEntityQuery.builder().page(page).perPage(perPage).filter(filter).orderBy(orderBy).build();
```

After changes, run `./mvnw compile -pl :dotcms-core -DskipTests`, then confirm
`git diff .../openapi/openapi.yaml` is empty (or contains only the intended schema changes).

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
- **Record view objects**: `@Schema` annotations on the record type and its components are picked up
  directly — no `passAnnotations` configuration is needed since records are concrete annotated types.

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
    
    // Build query (MyEntityQuery is now a record with a hand-written builder)
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
- **REST endpoints**: Located in `com.dotcms.rest.*` packages but the ones under `com.dotcms.rest.v*` are considered the new endpoints, the ones under `com.dotcms.rest` are considered legacy endpoints. For example: `com.dotcms.rest.ContentResource` is the legacy one and `com.dotcms.rest.api.v1.ContentResource` is the new one.
- **WebResource**: Found in `com.dotcms.rest.WebResource`
- **ResponseUtil**: Located in `com.dotcms.rest.ResponseUtil`
- **Forms**: Typically in same package as resource or `*.form` subpackage
- **View / query records**: Same package as the resource or a `*.view` / `*.query` subpackage
- **Integration tests**: Located in `dotcms-integration` module
