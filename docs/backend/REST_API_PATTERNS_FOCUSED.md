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
}
```

## dotCMS-Specific Patterns

### Request Initialization (Required)
```java
// ALWAYS initialize request context for security
InitDataObject initData = webResource.init(request, response, true);
User user = initData.getUser();
```

### Input Validation (Required)
```java
// Use UtilMethods.isSet() for validation
if (!UtilMethods.isSet(id)) {
    return ResponseUtil.mapExceptionResponse(
        new DotDataException("ID is required")
    );
}
```

### Service Access (Required)
```java
// Use APILocator for service access
MyService service = APILocator.getMyService();
```

### Exception Handling (Required)
```java
// Use ResponseUtil.mapExceptionResponse for consistent error handling
catch (Exception e) {
    Logger.error(this, "Error message", e);
    return ResponseUtil.mapExceptionResponse(e);
}
```

### Response Wrapping (Required)
```java
// Use ResponseEntityView for consistent response format
return Response.ok(new ResponseEntityView<>(entity)).build();
```

## Security Patterns

### Permission Checking
```java
// Check permissions before processing
if (!permissionAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_READ, user)) {
    throw new DotSecurityException("User lacks permission");
}
```

### Input Sanitization
```java
// Sanitize user input to prevent injection attacks
String sanitizedInput = SecurityUtils.sanitizeInput(userInput);
```

## URL Versioning
- All new endpoints must use `/v1/` prefix
- Existing endpoints maintain current paths for backwards compatibility
- Version bumps only for breaking changes

## Required Annotations
- `@NoCache` - Prevents caching of responses
- `@ApplicationScoped` - CDI scope for resource classes
- `@Produces(MediaType.APPLICATION_JSON)` - JSON response format
- `@Consumes(MediaType.APPLICATION_JSON)` - JSON request format (for POST/PUT)

## Integration Testing
```java
// REST endpoints require integration tests
@ExtendWith(MockitoExtension.class)
class MyResourceIntegrationTest extends APITestCase {
    
    @Test
    void testGetById() {
        // Test implementation
    }
}
```

## Location Information
- **REST Resources**: `dotCMS/src/main/java/com/dotcms/rest/`
- **WebResource**: `com.dotcms.rest.WebResource`
- **ResponseUtil**: `com.dotcms.rest.ResponseUtil`
- **Response Views**: `com.dotcms.rest.ResponseEntityView`
- **Testing**: Extend `APITestCase` for integration tests