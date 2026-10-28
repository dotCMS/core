# Backend Security Patterns

## Input Validation (Critical)

### Parameter Validation Pattern
```java
import com.dotmarketing.util.UtilMethods;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;

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

### Programmatic Basic authentication

`ALLOW_BASIC_AUTH` defaults to `JWT_TOKEN` and is read through `Config` for each authentication
attempt, not cached at startup. The default validates the same persisted API token a
`Bearer` header would — no password verification and no session — so a Basic wrapper is
credential-equivalent to Bearer, and a Basic-only client such as WebDAV works out of the box
with username + API JWT as its password. Administrators can change it at runtime through
`POST /api/v1/system-table`, for example with `{"key":"ALLOW_BASIC_AUTH","value":"NONE"}`.
System-table changes require no restart and apply to subsequent authentication attempts;
existing sessions are not revoked.

Normal configuration precedence still applies: an explicit `DOT_ALLOW_BASIC_AUTH` environment
override wins over the system table. Changing the process environment requires restarting the
instance; do not pin that override if administrators need to change this setting at runtime.
Mode names ignore surrounding whitespace and case. Explicit `FALSE`, `NONE`, empty and
unrecognized values disable the credential transports entirely rather than enabling a legacy
mode; an absent property is the `JWT_TOKEN` default.

| Mode | Username/account password | Username/API JWT |
| --- | --- | --- |
| `NONE` | Disabled | Disabled |
| `JWT_TOKEN` (default) | Disabled; no password verification | Accepted only for a valid API token belonging to the named user |
| `ALL` | Legacy compatibility | Accepted after normal token validation; legacy password fallback remains enabled |

API-token authentication validates signature, configured issuer rules, dates, persistence,
revocation, the active owner and client-IP restrictions. It does not create a browser login
session or cookie. `JWT_TOKEN` rejects login/remember-me tokens and never falls back to password
verification. `ALL` preserves existing WebDAV login-token compatibility; it does not add that
credential type to REST or weaken endpoints that already require Bearer authentication.

The restriction covers HTTP Basic and equivalent programmatic credential paths (`DOTAUTH`,
accepted REST user/password parameters and legacy Ajax password fallbacks). Existing supported
session/Bearer access and designated JSON/form login remain available, including the deprecated
JSON token-login endpoint used by deployed CLI clients. An independently valid session retains
precedence; disabling Basic does not revoke sessions already established. Foreign Basic headers
on public assets retain the existing anonymous fallback without granting private access.

**Migration:** use an API JWT with `Authorization: Bearer <token>` for ordinary API clients.
Obtain it through normal JSON session login (`POST /api/v1/authentication`) followed by
session-authenticated `POST /api/v1/apitoken` (`entity.jwt`), or provision a token separately.
Do not forward the bootstrap session into token-authenticated test clients, log credentials,
or substitute a login token (`entity.token`) where an API token is required.

WebDAV remains a Basic-transport client: opt into `JWT_TOKEN` and supply the matching username
and API JWT as its password. `NONE` disables WebDAV credential access, including protected
operations routed through library shortcuts. `ALL` re-enables account-password exposure and
should be used only as an explicit legacy compatibility choice, never to keep ordinary test
suites green. Separate Postman compatibility runs are documented in
[`dotcms-postman/README.md`](../../dotcms-postman/README.md).

Remote token creation uses the designated JSON-login/session-token flow without a Basic
exemption. Upgrade older senders that still send password Basic before switching a receiver
to `NONE`, or provision tokens through a supported path. Docker environment imports should
use `DOT_IMPORT_API_TOKEN`; the legacy `DOT_IMPORT_USERNAME_PASSWORD` option requires the
**remote** server to opt into `ALL`. Unrelated outbound Basic used for OAuth/search providers
is not controlled by this setting.

### Authentication Check Pattern
```java
public Response secureEndpoint(HttpServletRequest request, HttpServletResponse response) {
    InitDataObject initData = new WebResource.InitBuilder()
            .requiredBackendUser(true)
            .requestAndResponse(request, response)
            .rejectWhenNoUser(true)
            .init();
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

### Anonymous Access to REST APIs

Two config keys decide whether a request with no credentials (the anonymous user) may reach a REST endpoint.

- `CONTENT_APIS_ALLOW_ANONYMOUS` (`NONE | READ | WRITE`, default `NONE`) is the global setting. Endpoints that opt in to anonymous callers through `WebResource.InitBuilder.requiredAnonAccess(...)` or the deprecated `init(..., rejectWhenNoUser=false, ...)` overloads follow it: the content API (legacy and v1), GraphQL, the Elasticsearch search API, tags, languages, content types and fields, widgets (`/api/widget`), and content relationships (`/api/v1/contentrelationships`). Anonymous writes (workflow fire, temp-file upload, legacy content save) also call `requiredAnonAccess(AnonymousAccess.WRITE)`, so they need `WRITE`; `READ` is not enough.
- `SCRIPTING_APIS_ALLOW_ANONYMOUS` (`true | false`, default `false`) covers only the file-based scripting endpoints, `/api/vtl/{folder}` (`VTLResource`) and `/api/js/{folder}` (`JsResource`). When `true`, anonymous callers can reach them even if the global key is `NONE`; when the global key already allows anonymous reads, they are reachable as before. Anonymous requests get `401` only when the global key is `NONE` and this key is `false`. The script runs as the anonymous user, so permission-checked tools such as `$dotcontent` see only what that user can read; tools that do not check permissions behave as they would for any caller, so review what an endpoint outputs before opening it. This lets a site keep the built-in APIs closed while serving its own published endpoints. The key applies to every HTTP method (GET, POST, PUT, PATCH and DELETE), not only reads. To keep an endpoint read-only, publish only its `get.vtl` / `get.js`, or remove anonymous read permission from the other method files.

The `/api/vtl/dynamic` and `/api/js/dynamic` endpoints run code the caller sends, so the scripting key does not cover them; they follow the global key like every other endpoint. Authenticated users are unaffected by either key.

There is no `REST_API_REJECT_WITH_NO_USER` setting. An endpoint that must never accept anonymous callers says so in code with `.rejectWhenNoUser(true)` on the `InitBuilder` (as in the pattern above); `InitBuilder` already defaults to rejecting anonymous callers unless `requiredAnonAccess(...)` is set.

**Velocity Secrets in scripting endpoints:** Velocity Secrets (`$dotsecrets`) can be read from the file-based `/api/vtl` and `/api/js` endpoints, including by anonymous callers, when the user who last edited the endpoint's script file (for example `get.vtl` or `get.js`) has the Scripting Developer role. This matches how secrets work in page content. If the file's last editor doesn't have that role, the requesting user must have it instead, so anonymous calls can't read secrets. Code posted to the `/dynamic` endpoints comes from the caller, so there only the requesting user's role counts, and setting `secrets.scripting.enabled=false` still turns secrets off everywhere.

For `/api/js`, the editor of the entry file (`get.js`, `post.js`, and so on) is what counts, including for any modules it imports.

**Warning:** never evaluate request input (Velocity `#evaluate`, JavaScript `eval`) in an endpoint whose file was last edited by a Scripting Developer. Code evaluated on the fly has no file of its own, so it runs with the editor's access to secrets, and an anonymous caller could send `$dotsecrets.get('anyKey')` and get the value back.

### Permission Validation Pattern
```java
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;

public class MySecureService {
    
    public MyEntity getEntity(String entityId, User user) throws DotDataException, DotSecurityException {
        // Load entity
        MyEntity entity = findById(entityId);
        
        if (entity == null) {
            throw new DotDataException("Entity not found");
        }
        
        // Check read permission
        PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        if (!permissionAPI.doesUserHavePermission(entity, PermissionAPI.PERMISSION_READ, user)) {
            throw new DotSecurityException("Read permission denied for entity: " + entityId);
        }
        
        return entity;
    }
    
    public MyEntity updateEntity(String entityId, MyEntityForm form, User user) 
            throws DotDataException, DotSecurityException {
        
        // Load and check permissions
        MyEntity entity = findById(entityId);
        PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        
        if (!permissionAPI.doesUserHavePermission(entity, PermissionAPI.PERMISSION_EDIT, user)) {
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
        
        if (!permissionAPI.doesUserHavePermission(entity, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, user)) {
            throw new DotSecurityException("Delete permission denied for entity: " + entityId);
        }
        
        performDelete(entity, user);
    }
}
```

### Admin Permission Check
```java
public Response adminOnlyEndpoint(HttpServletRequest request, HttpServletResponse response) {
    InitDataObject initData = new WebResource.InitBuilder()
            .requiredBackendUser(true)
            .requestAndResponse(request, response)
            .rejectWhenNoUser(true)
            .init();
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
import com.liferay.util.Xss;

public class MyResponseBuilder {
    
    public Map<String, Object> buildResponse(MyEntity entity) {
        Map<String, Object> response = new HashMap<>();
        
        // Encode HTML content for safe display
        response.put("name", Xss.encodeForHTML(entity.getName()));
        response.put("description", Xss.encodeForHTML(entity.getDescription()));
        
        // Safe content (already validated)
        response.put("id", entity.getId());
        response.put("createdDate", entity.getCreatedDate());
        
        return response;
    }
}
```

### Input Sanitization Pattern

> **Don't hand-roll HTML sanitization with regex** — stripping `<script>`/`javascript:`/`on*=`
> patterns is well-known to be bypassable (case variation, malformed tags, encodings, event
> handlers this list doesn't cover). Use the real `Xss.encodeForHTML(...)` pattern from
> **XSS Prevention** above instead — that's the correct, maintained answer.

```java
public class InputSanitizer {
    
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