# Backend Security Patterns

## Input Validation (Critical)

### Parameter Validation Pattern
```java
import com.dotmarketing.util.UtilMethods;
import com.dotcms.rest.ResponseUtil;

public class MyResource {
    
    public Response processInput(String userInput) {
        // Null and empty validation
        if (!UtilMethods.isSet(userInput)) {
            return ResponseUtil.mapExceptionResponse(
                new DotDataException("Input cannot be empty")
            );
        }
        
        // Length validation
        if (userInput.length() > 255) {
            return ResponseUtil.mapExceptionResponse(
                new DotDataException("Input exceeds maximum length of 255 characters")
            );
        }
        
        // Format validation (whitelist approach)
        if (!userInput.matches("^[a-zA-Z0-9\\s\\-_\\.]+$")) {
            Logger.warn(this, "Invalid input format attempted: " + sanitizeForLogging(userInput));
            return ResponseUtil.mapExceptionResponse(
                new DotSecurityException("Invalid input format")
            );
        }
        
        // Business logic validation
        if (isBlacklisted(userInput)) {
            Logger.warn(this, "Blacklisted input attempted");
            return ResponseUtil.mapExceptionResponse(
                new DotSecurityException("Input not allowed")
            );
        }
        
        // Process validated input
        return processValidatedInput(userInput);
    }
    
    private String sanitizeForLogging(String input) {
        // Remove potentially dangerous characters for logging
        return input.replaceAll("[<>\"'&]", "_");
    }
}
```

### Form Validation Pattern
```java
@JsonIgnoreProperties(ignoreUnknown = true)
public class MySecureForm {
    private String name;
    private String email;
    private String description;
    
    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        
        // Name validation
        if (!UtilMethods.isSet(name)) {
            errors.add("Name is required");
        } else if (name.length() > 100) {
            errors.add("Name must be 100 characters or less");
        } else if (!name.matches("^[a-zA-Z0-9\\s\\-_]+$")) {
            errors.add("Name contains invalid characters");
        }
        
        // Email validation
        if (UtilMethods.isSet(email) && !EmailValidator.getInstance().isValid(email)) {
            errors.add("Invalid email format");
        }
        
        // Description validation
        if (UtilMethods.isSet(description) && description.length() > 1000) {
            errors.add("Description must be 1000 characters or less");
        }
        
        return errors;
    }
    
    public boolean isValid() {
        return validate().isEmpty();
    }
}
```

## Authentication and Authorization

### Authentication Check Pattern
```java
public Response secureEndpoint(HttpServletRequest request, HttpServletResponse response) {
    InitDataObject initData = webResource.init(request, response, true);
    User user = initData.getUser();
    
    // Verify user is authenticated
    if (user == null || !user.isLoggedIn()) {
        return ResponseUtil.mapExceptionResponse(
            new DotSecurityException("Authentication required")
        );
    }
    
    // Verify user account is active
    if (!user.isActive()) {
        return ResponseUtil.mapExceptionResponse(
            new DotSecurityException("Account is inactive")
        );
    }
    
    // Process authenticated request
    return processAuthenticatedRequest(user);
}
```

### Permission Validation Pattern
```java
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionLevel;

public class MySecureService {
    
    public MyEntity getEntity(String entityId, User user) throws DotDataException, DotSecurityException {
        // Load entity
        MyEntity entity = findById(entityId);
        
        if (entity == null) {
            throw new DotDataException("Entity not found");
        }
        
        // Check read permission
        PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        if (!permissionAPI.hasPermission(user, entity, PermissionLevel.READ)) {
            throw new DotSecurityException("Read permission denied for entity: " + entityId);
        }
        
        return entity;
    }
    
    public MyEntity updateEntity(String entityId, MyEntityForm form, User user) 
            throws DotDataException, DotSecurityException {
        
        // Load and check permissions
        MyEntity entity = findById(entityId);
        PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        
        if (!permissionAPI.hasPermission(user, entity, PermissionLevel.EDIT)) {
            throw new DotSecurityException("Edit permission denied for entity: " + entityId);
        }
        
        // Validate form data
        if (!form.isValid()) {
            throw new DotDataException("Invalid form data: " + form.getValidationErrors());
        }
        
        // Update entity
        return performUpdate(entity, form, user);
    }
    
    public void deleteEntity(String entityId, User user) 
            throws DotDataException, DotSecurityException {
        
        MyEntity entity = findById(entityId);
        PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        
        if (!permissionAPI.hasPermission(user, entity, PermissionLevel.EDIT_PERMISSIONS)) {
            throw new DotSecurityException("Delete permission denied for entity: " + entityId);
        }
        
        performDelete(entity, user);
    }
}
```

### Admin Permission Check
```java
public Response adminOnlyEndpoint(HttpServletRequest request, HttpServletResponse response) {
    InitDataObject initData = webResource.init(request, response, true);
    User user = initData.getUser();
    
    // Check if user is admin
    if (!user.isAdmin()) {
        Logger.warn(this, "Non-admin user attempted admin operation: " + user.getUserId());
        return ResponseUtil.mapExceptionResponse(
            new DotSecurityException("Administrator privileges required")
        );
    }
    
    return processAdminRequest(user);
}
```

## SQL Injection Prevention

### Parameterized Query Pattern (Required)
```java
// ✅ ALWAYS use parameterized queries
public List<MyEntity> findByName(String name) throws DotDataException {
    DotConnect dotConnect = new DotConnect();
    
    List<Map<String, Object>> results = dotConnect
        .setSQL("SELECT * FROM my_table WHERE name = ?")
        .addParam(name)
        .loadResults();
        
    return results.stream()
        .map(this::mapToEntity)
        .collect(Collectors.toList());
}

// ✅ Safe dynamic query building
public List<MyEntity> findByFilters(String name, String status, Date createdAfter) throws DotDataException {
    StringBuilder sql = new StringBuilder("SELECT * FROM my_table WHERE 1=1");
    DotConnect dotConnect = new DotConnect();
    
    if (UtilMethods.isSet(name)) {
        sql.append(" AND name ILIKE ?");
        dotConnect.addParam("%" + name + "%");
    }
    
    if (UtilMethods.isSet(status)) {
        sql.append(" AND status = ?");
        dotConnect.addParam(status);
    }
    
    if (createdAfter != null) {
        sql.append(" AND created_date >= ?");
        dotConnect.addParam(createdAfter);
    }
    
    return dotConnect
        .setSQL(sql.toString())
        .loadResults()
        .stream()
        .map(this::mapToEntity)
        .collect(Collectors.toList());
}

// ❌ NEVER use string concatenation
public List<MyEntity> unsafeFind(String name) throws DotDataException {
    String sql = "SELECT * FROM my_table WHERE name = '" + name + "'"; // SQL INJECTION RISK
    // This pattern is FORBIDDEN
}
```

## XSS Prevention

### Output Encoding Pattern
```java
import org.springframework.web.util.HtmlUtils;

public class MyResponseBuilder {
    
    public Map<String, Object> buildResponse(MyEntity entity) {
        Map<String, Object> response = new HashMap<>();
        
        // Encode HTML content for safe display
        response.put("name", HtmlUtils.htmlEscape(entity.getName()));
        response.put("description", HtmlUtils.htmlEscape(entity.getDescription()));
        
        // Safe content (already validated)
        response.put("id", entity.getId());
        response.put("createdDate", entity.getCreatedDate());
        
        return response;
    }
}
```

### Input Sanitization Pattern
```java
public class InputSanitizer {
    
    public String sanitizeHtml(String input) {
        if (!UtilMethods.isSet(input)) {
            return null;
        }
        
        // Remove script tags and dangerous attributes
        return input
            .replaceAll("<script[^>]*>.*?</script>", "")
            .replaceAll("javascript:", "")
            .replaceAll("on\\w+\\s*=", "")
            .replaceAll("<iframe[^>]*>.*?</iframe>", "");
    }
    
    public String sanitizeForDatabase(String input) {
        if (!UtilMethods.isSet(input)) {
            return null;
        }
        
        // Basic sanitization for database storage
        return input.trim()
            .replaceAll("[\r\n\t]", " ")
            .replaceAll("\\s+", " ");
    }
}
```

## Logging Security

### Safe Logging Pattern
```java
public class SecureLogger {
    
    public void logUserAction(User user, String action, String resourceId) {
        // Safe to log - no sensitive data
        Logger.info(this, String.format(
            "User action - User: %s, Action: %s, Resource: %s",
            user.getUserId(), action, resourceId
        ));
    }
    
    public void logSecurityEvent(String event, String details) {
        // Sanitize details before logging
        String sanitizedDetails = sanitizeForLogging(details);
        Logger.warn(this, String.format("Security event: %s - %s", event, sanitizedDetails));
    }
    
    public void logError(String operation, Exception e) {
        // Log error without sensitive data
        Logger.error(this, String.format(
            "Operation failed: %s - Error: %s",
            operation, e.getMessage()
        ), e);
    }
    
    private String sanitizeForLogging(String input) {
        if (!UtilMethods.isSet(input)) {
            return "null";
        }
        
        // Remove potentially sensitive information
        return input
            .replaceAll("password\\s*=\\s*[^\\s]+", "password=***")
            .replaceAll("token\\s*=\\s*[^\\s]+", "token=***")
            .replaceAll("[<>\"'&]", "_")
            .substring(0, Math.min(input.length(), 200)); // Limit length
    }
}

// ❌ NEVER log sensitive information
public void badLogging(User user, String password) {
    Logger.info(this, "User logged in: " + user.getEmailAddress() + " with password: " + password);
    // This is a SECURITY VIOLATION
}
```

## Configuration Security

### Secure Configuration Pattern
```java
public class SecureConfigService {
    
    public String getSecureProperty(String propertyName) {
        // Use Config for secure property access
        String value = Config.getStringProperty(propertyName);
        
        if (!UtilMethods.isSet(value)) {
            Logger.warn(this, "Secure property not configured: " + propertyName);
            throw new DotDataException("Configuration property not found: " + propertyName);
        }
        
        return value;
    }
    
    public String getApiKey() {
        // Environment variable: DOT_EXTERNAL_API_KEY
        String apiKey = Config.getStringProperty("external.api.key");
        
        if (!UtilMethods.isSet(apiKey)) {
            throw new DotDataException("API key not configured");
        }
        
        return apiKey;
    }
    
    public boolean isFeatureEnabled(String featureName) {
        // Environment variable: DOT_FEATURE_ENABLED
        return Config.getBooleanProperty("feature.enabled", false);
    }
}

// ❌ NEVER hardcode sensitive values
public class BadConfigService {
    private static final String API_KEY = "sk-1234567890abcdef"; // SECURITY VIOLATION
    private static final String DATABASE_PASSWORD = "mypassword"; // SECURITY VIOLATION
}
```

## Error Handling Security

### Secure Error Response Pattern
```java
public class SecureErrorHandler {
    
    public Response handleException(Exception e) {
        if (e instanceof DotSecurityException) {
            // Log security exception with context
            Logger.warn(this, "Security exception: " + e.getMessage());
            
            // Return generic error message (don't expose internal details)
            return Response.status(Response.Status.FORBIDDEN)
                .entity(new ResponseEntityView<>(
                    Collections.singletonMap("error", "Access denied")
                ))
                .build();
        }
        
        if (e instanceof DotDataException) {
            // Log data exception
            Logger.error(this, "Data exception: " + e.getMessage(), e);
            
            // Return safe error message
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ResponseEntityView<>(
                    Collections.singletonMap("error", "Invalid request")
                ))
                .build();
        }
        
        // Log unexpected exceptions
        Logger.error(this, "Unexpected exception: " + e.getMessage(), e);
        
        // Return generic error (don't expose stack traces)
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity(new ResponseEntityView<>(
                Collections.singletonMap("error", "Internal server error")
            ))
            .build();
    }
}
```

## Security Testing Patterns

### Security Integration Tests
```java
@Test
public void testUnauthorizedAccess() {
    // Test without authentication
    given()
        .when()
        .get("/api/v1/secure-endpoint")
        .then()
        .statusCode(HttpStatus.SC_UNAUTHORIZED);
}

@Test
public void testInsufficientPermissions() {
    // Test with limited user
    User limitedUser = new UserDataGen().nextPersisted();
    
    given()
        .auth().basic(limitedUser.getEmailAddress(), "admin")
        .when()
        .get("/api/v1/admin-endpoint")
        .then()
        .statusCode(HttpStatus.SC_FORBIDDEN);
}

@Test
public void testInputValidation() {
    // Test with malicious input
    String maliciousInput = "<script>alert('xss')</script>";
    
    given()
        .auth().basic("admin@dotcms.com", "admin")
        .contentType(ContentType.JSON)
        .body(Collections.singletonMap("name", maliciousInput))
        .when()
        .post("/api/v1/myresource")
        .then()
        .statusCode(HttpStatus.SC_BAD_REQUEST);
}

@Test
public void testSqlInjection() {
    // Test with SQL injection attempt
    String sqlInjection = "'; DROP TABLE users; --";
    
    given()
        .auth().basic("admin@dotcms.com", "admin")
        .queryParam("filter", sqlInjection)
        .when()
        .get("/api/v1/myresource")
        .then()
        .statusCode(HttpStatus.SC_BAD_REQUEST);
}
```

## Location Information
- **Security classes**: Located in `com.dotcms.security.*` packages
- **Permission API**: Found in `com.dotmarketing.business.PermissionAPI`
- **WebResource**: Located in `com.dotcms.rest.WebResource`
- **Config**: Found in `com.dotmarketing.util.Config`
- **Validation utilities**: Located in `com.dotmarketing.util.UtilMethods`