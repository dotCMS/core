# dotCMS Development Guide

## 🎯 Quick Start Context

### Build Optimization (Choose Right Command)
```bash
# For test-only changes (target specific tests!):
./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dit.test=MyTestClass  # Specific test class (~2-10 min)
./mvnw verify -pl :dotcms-postman -Dpostman.test.skip=false -Dpostman.collections=all  # OR: just test-postman (~1-3 min)

# For simple code changes in dotcms-core only:
./mvnw install -pl :dotcms-core -DskipTests    # OR: just build-quicker (~2-3 min)

# If core changes affect dependencies:  
./mvnw install -pl :dotcms-core --am -DskipTests (~3-5 min)

# For major changes or starting fresh:
./mvnw clean install -DskipTests               # OR: just build (~8-15 min)
```

### Essential Patterns
```java
// Java (ALWAYS use these)
import com.dotmarketing.util.Config;   // Config.getStringProperty("key", "default")
import com.dotmarketing.util.Logger;   // Logger.info(this, "message")
UserAPI userAPI = APILocator.getUserAPI();
```

### Test Development Workflow
```bash
# ⚠️ CRITICAL: Never run full integration suite during development (60+ min)
# Instead, target specific test classes or methods:

# Option 1: Command line with specific test class (2-10 min)
./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dit.test=ContentTypeAPIImplTest

# Option 2: IDE debugging with services (fastest iteration)
just test-integration-ide          # Starts PostgreSQL + Elasticsearch + dotCMS
# → Run/debug individual tests in IDE with breakpoints (10-30 sec per test)
just test-integration-stop         # Clean up when done
```

#### Immutable Objects (Critical Pattern)
```java
@Value.Immutable
@JsonSerialize(as = ImmutableMyEntity.class)
@JsonDeserialize(as = ImmutableMyEntity.class)
public abstract class MyEntity {
    public abstract String name();
    public abstract Optional<String> description();
    
    @Value.Default
    public boolean enabled() { return true; }
    
    public static Builder builder() { return ImmutableMyEntity.builder(); }
    
    // Generated builder methods available at compile time
    // Must run ./mvnw compile after creating @Value.Immutable classes
}

// Usage: MyEntity.builder().name("test").description("desc").build()
```

#### REST Endpoints (JAX-RS Pattern)
```java
@Path("/v1/myresource")
@Tag(name = "Resource Category", description = "Description of this resource group")
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
    
    @Operation(
        summary = "Create new resource",
        description = "Creates a new resource with the provided data"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", 
                    description = "Resource created successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityMyResourceView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Invalid request data",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized access",
                    content = @Content(mediaType = "application/json"))
    })
    @POST @Produces(MediaType.APPLICATION_JSON) @Consumes(MediaType.APPLICATION_JSON) @NoCache
    public Response create(@Context HttpServletRequest request,
                          @RequestBody(description = "Resource data to create", 
                                     required = true,
                                     content = @Content(schema = @Schema(implementation = MyResourceForm.class)))
                          MyResourceForm form) {
        InitDataObject initData = webResource.init(request, response, true);
        User user = initData.getUser();
        
        // Business logic
        return Response.status(201).entity(new ResponseEntityMyResourceView(result)).build();
    }
}
```

#### REST Endpoint Documentation Standards

**REQUIRED Swagger/OpenAPI Annotations:**
```java
// Class level - ALWAYS required
@Tag(name = "Category", description = "Brief description")

// Method level - ALL methods MUST have these
@Operation(summary = "Brief action", description = "Detailed explanation")
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Success description",
                content = @Content(mediaType = "application/json",
                                  schema = @Schema(implementation = ResponseEntitySpecificView.class))),
    @ApiResponse(responseCode = "4xx/5xx", description = "Error description",
                content = @Content(mediaType = "application/json"))
})

// Parameters - ALL path/query parameters MUST be documented
@Parameter(description = "Parameter purpose", required = true/false)

// Request bodies - ALL POST/PUT/PATCH with bodies MUST be documented
@RequestBody(description = "Body purpose", required = true,
           content = @Content(schema = @Schema(implementation = FormClass.class)))
```

**Parameter Annotation Rules:**
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

**🚨 CRITICAL: @Schema Implementation Rules**

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

// ✅ CORRECT @Schema - use Object.class for unwrapped collections
@Schema(implementation = Object.class)  // For direct Map<String, RestLanguage>
@Schema(implementation = Object.class)  // For direct List<ContentType>
```

### ✅ Pattern 2b: Typed Maps Require Specific View Classes
```java
// Method that returns typed Map with specific domain objects:
public Map<String, RestPersona> list() {
    return personaMap;  // Direct Map<String, RestPersona> return
}

// ✅ CORRECT @Schema - create specific view class for typed maps
@Schema(implementation = MapStringRestPersonaView.class)

// The view class must extend the actual map type:
public class MapStringRestPersonaView extends HashMap<String, RestPersona> {
    public MapStringRestPersonaView() { super(); }
    public MapStringRestPersonaView(Map<String, RestPersona> map) { super(map); }
}

// ❌ WRONG - Object.class loses type information for API documentation
@Schema(implementation = Object.class)  // Don't use for typed maps!
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

### ✅ Pattern 4: Complex Dynamic JSON (Rare Cases)
```java
// Method that returns complex dynamic structures:
public Response getComplexData() {
    Map<String, Object> complex = Map.of("nested", nestedMap, "arrays", arrays);
    return Response.ok(complex).build();
}

// ✅ CORRECT @Schema - only for truly dynamic complex JSON
@Schema(implementation = Object.class)  // For JSONObject, complex Map.of(), etc.
```

**❌ NEVER Use These Antipatterns:**
```java
// ❌ WRONG - provides no meaningful API documentation
@Schema(implementation = ResponseEntityView.class)

// ❌ WRONG - missing schema entirely  
content = @Content(mediaType = "application/json")

// ❌ WRONG - using wrapper schema for unwrapped return
// Method returns: Map<String, RestLanguage> (unwrapped)
@Schema(implementation = ResponseEntityMapView.class)  // WRONG - no wrapper used

// ❌ WRONG - using Object.class for wrapped return  
// Method returns: new ResponseEntityUserView(user) (wrapped)
@Schema(implementation = Object.class)  // WRONG - specific wrapper exists
```

**🔍 How to Determine the Correct @Schema:**

1. **Check the method's return statement** - what does it actually return?
2. **Wrapped returns** → Use the specific ResponseEntity*View class
3. **Unwrapped collections/maps** → Use Object.class  
4. **No specific class exists** → Use generic ResponseEntity*View utilities

**Available ResponseEntity*View Classes (80+ available):**
```java
// Core Utility Classes (use when no specific domain class exists)
ResponseEntityBooleanView, ResponseEntityStringView, ResponseEntityListView,
ResponseEntityMapView, ResponseEntityCountView, ResponseEntityJobView

// Authentication & User Management
ResponseEntityUserView, ResponseEntityApiTokenView, ResponseEntityLoginFormView,
ResponseEntityJwtView, ResponseEntityPasswordResetView

// Content Management
ResponseEntityContentletView, ResponseEntityContentTypeListView,
ResponseEntityContentTypeOperationView, ResponseEntityFieldView

// Workflow System
ResponseEntityWorkflowSchemeView, ResponseEntityWorkflowStepView,
ResponseEntityWorkflowActionView, ResponseEntityBulkActionView

// Infrastructure
ResponseEntityContainerView, ResponseEntityTemplateView, ResponseEntityCategoryView,
ResponseEntityTagView, ResponseEntityExperimentView, ResponseEntityHealthStatusView
```

**Media Type Annotation Rules:**
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

**Standard Media Types:**
- Primary: `MediaType.APPLICATION_JSON`
- Multiple response formats: `{MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN}`
- Streaming: `{MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON}`
- File uploads: `MediaType.MULTIPART_FORM_DATA`

**Response Status Codes:**
```java
// Standard success codes
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

**ResponseEntity View Classes (Critical Pattern):**
```java
// ALWAYS use specific typed ResponseEntity view classes instead of generic ResponseEntityView
// This provides better type safety and precise Swagger schema documentation

// ✅ PREFERRED: Use existing specific view classes
@ApiResponse(responseCode = "200", description = "Success",
            content = @Content(mediaType = "application/json",
                              schema = @Schema(implementation = ResponseEntityStringView.class)))
return Response.ok(new ResponseEntityView<>("success message")).build();

@ApiResponse(responseCode = "200", description = "Workflow steps retrieved",
            content = @Content(mediaType = "application/json",
                              schema = @Schema(implementation = ResponseEntityWorkflowStepsView.class)))
return Response.ok(new ResponseEntityView<>(steps)).build();

// ✅ CREATE new specific view classes when needed
// Example: ResponseEntityTagsView.java
public class ResponseEntityTagsView extends ResponseEntityView<List<Tag>> {
    public ResponseEntityTagsView(List<Tag> tags) {
        super(tags);
    }
}

// ✅ Common existing view classes to use:
// - ResponseEntityStringView (for String responses)
// - ResponseEntityCountView (for count/number responses)  
// - ResponseEntityJobView (for job status)
// - ResponseEntityWorkflowStepsView (for workflow steps)
// - ResponseEntityWorkflowActionsView (for workflow actions)
// - ResponseEntityEnvironmentView (for single environment)
// - ResponseEntityEnvironmentsView (for environment lists)

// ❌ AVOID: Generic ResponseEntityView.class in @Schema
@ApiResponse(responseCode = "200", description = "Success",
            content = @Content(mediaType = "application/json",
                              schema = @Schema(implementation = ResponseEntityView.class)))  // Too generic!
```

**Deprecation Documentation Standards:**
```java
// Class level deprecation (legacy resources)
@Deprecated
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

#### Exception Handling (dotCMS Hierarchy)
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

#### Utility Methods (Null-Safe Patterns)
```java
import com.dotmarketing.util.UtilMethods;

// ALWAYS use UtilMethods.isSet() for null checking
if (UtilMethods.isSet(myString)) {  // checks null, empty, and "null" string
    processString(myString);
}

// Collections
List<String> list = CollectionsUtils.list("item1", "item2");
Map<String, Object> map = CollectionsUtils.map("key1", "value1", "key2", "value2");

// Safe supplier pattern (avoids NullPointerException)
String value = UtilMethods.isSet(() -> complex.getObject().getValue()) 
    ? complex.getObject().getValue() 
    : "default";
```

#### Database Access Patterns
```java
import com.dotmarketing.common.db.DotConnect;

// Query with parameters
DotConnect dotConnect = new DotConnect();
List<Map<String, Object>> results = dotConnect
    .setSQL("SELECT * FROM my_table WHERE column1 = ? AND column2 = ?")
    .addParam("value1")
    .addParam("value2")
    .loadResults();

// Use with LocalTransaction for atomic operations
LocalTransaction.wrapReturn(() -> {
    return dotConnect.setSQL("UPDATE my_table SET column1 = ?")
        .addParam("newValue")
        .executeUpdate();
});
```

### Angular Development (core-web/)
```typescript
// Angular (REQUIRED modern syntax)
@if (condition()) { <content /> }      // NOT *ngIf
data = input<string>();                // NOT @Input()
spectator.setInput('prop', value);     // Testing CRITICAL
```

```bash
# Test Commands (fastest - no core rebuild needed!)
# ⚠️ IMPORTANT: Target specific test classes, NOT full suite (full suite = 60+ min)
./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dit.test=ContentTypeAPIImplTest  # Specific test class (2-10 min)
just test-postman ai                                  # Specific Postman collection 
./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dit.test=MyTest#testMethod      # Specific test method

# ⚠️ CRITICAL: All test modules need explicit skip=false flags or tests are silently skipped!
# -Dcoreit.test.skip=false (integration) | -Dpostman.test.skip=false | -Dkarate.test.skip=false

# IDE Testing (start services manually, then run tests in IDE)
just test-integration-ide                             # Start DB/ES services for IDE debugging
# → Now run individual tests in your IDE with breakpoints
just test-integration-stop                            # Stop services when done

# Build Commands (choose based on your changes)
./mvnw install -pl :dotcms-core -DskipTests           # Fast: simple core changes (~2-3 min)
just build-quicker                                     # Same as above, shorter command

./mvnw install -pl :dotcms-core --am -DskipTests      # Medium: core + dependencies (~3-5 min)  

./mvnw clean install -DskipTests                      # Full: major changes/clean start (~8-15 min)
just build                                             # Same as above, shorter command

./mvnw install -pl :dotcms-core -DskipTests -Ddocker.skip  # Fastest: no Docker (~1-2 min)
just build-no-docker                                  # Full build without Docker

# Run Commands (use AFTER building)
./mvnw -pl :dotcms-core -Pdocker-start -Dtomcat.port=8080  -Ddocker.glowroot.enabled=true # Run dotCMS in Docker
just dev-run                                          # Start with Glowroot profiler enabled

cd core-web && nx run dotcms-ui:serve                 # Separate Frontend dev server only
```

### Tech Stack
- **Backend**: Java 21 runtime, Java 11 syntax (core), Maven, Spring/CDI
- **Frontend**: Angular 18.2.3, PrimeNG 17.18.11, NgRx Signals, Jest + Spectator  
- **Infrastructure**: Docker, PostgreSQL, Elasticsearch, GitHub Actions

### Critical Rules
- **Maven versions**: Add to `bom/application/pom.xml` ONLY, never `dotCMS/pom.xml`
- **Testing**: ALWAYS use `data-testid` and `spectator.setInput()`
- **Security**: No hardcoded secrets, validate input, use Logger not System.out

## 📚 Documentation Navigation (Load On-Demand)

### Core Architecture & Workflows
- [Architecture Overview](docs/core/ARCHITECTURE_OVERVIEW.md) - System design, modules, patterns
- [Git Workflows](docs/core/GIT_WORKFLOWS.md) - Branch naming, PR process, issue management
- [CI/CD Pipeline](docs/core/CICD_PIPELINE.md) - Build process, testing, deployment

### Backend Development (Java/Maven)
- [Java Standards](docs/backend/JAVA_STANDARDS.md) - Coding patterns, frameworks, APIs
- [Maven Build System](docs/backend/MAVEN_BUILD_SYSTEM.md) - **CRITICAL**: Dependency management
- [Configuration Patterns](docs/backend/CONFIGURATION_PATTERNS.md) - Config.getProperty() usage
- [REST API Patterns](docs/backend/REST_API_PATTERNS.md) - JAX-RS, endpoints, security
- [Database Patterns](docs/backend/DATABASE_PATTERNS.md) - DotConnect, transactions

### Frontend Development (Angular/TypeScript)  
- [Angular Standards](docs/frontend/ANGULAR_STANDARDS.md) - Modern syntax, signals, components
- [Testing Frontend](docs/frontend/TESTING_FRONTEND.md) - **CRITICAL**: Spectator patterns
- [Component Architecture](docs/frontend/COMPONENT_ARCHITECTURE.md) - Structure, organization
- [Styling Standards](docs/frontend/STYLING_STANDARDS.md) - SCSS, BEM, variables

### Testing
- [Backend Unit Tests](docs/testing/BACKEND_UNIT_TESTS.md) - JUnit, integration patterns
- [Integration Tests](docs/testing/INTEGRATION_TESTS.md) - API testing, database setup
- [E2E Tests](docs/testing/E2E_TESTS.md) - Playwright, user workflows

### Infrastructure & Build
- [Docker Build Process](docs/infrastructure/DOCKER_BUILD_PROCESS.md) - **BUILD OPTIMIZATION**, container setup
- [GitHub Issue Management](docs/core/GITHUB_ISSUE_MANAGEMENT.md) - Issues, PRs, epics

## 🔄 Context Management Strategy

### For Claude:
- Use this streamlined guide for always-available context
- Load detailed `/docs/` files on-demand with Read tool
- Use `/clear` between different work contexts

### For Cursor:
- This guide provides essential patterns immediately
- Use `@docs/path/file.md` syntax for detailed patterns
- Domain-specific rules trigger additional context when needed

### Progressive Enhancement
When editing ANY code:
- Add missing generics: `List<String>` not `List`
- Replace legacy patterns: `Logger.info()` not `System.out.println()`
- Update to modern syntax: `@if` not `*ngIf`
- Add missing annotations: `@Override`, `@Nullable`

## 📝 Documentation Maintenance

### When You Discover Issues:
1. **Identify location**: Which `/docs/{domain}/` file needs updating?
2. **Update authoritative source**: Make changes in `/docs/` directory
3. **Cross-reference**: Link from other relevant locations
4. **Keep context minimal**: This file should only contain navigation + essential patterns

### Quality Standards:
- Concrete examples with ✅ correct and ❌ incorrect patterns
- Specific file paths and line references
- Cross-references instead of duplication
- Context-window optimized content

**Remember**: Both Claude and Cursor use the same documentation system. Keep information DRY and in the right places.
Key endpoints:
- `/livez` - Kubernetes liveness probe (minimal text response)
- `/readyz` - Kubernetes readiness probe (minimal text response)  
- `/api/v1/health` - Detailed monitoring (JSON response, requires authentication)

### Changing Log Levels on Running Server
You can dynamically change log levels without restarting the server using the Logger REST API:

```bash
# Change log level for a specific class (requires admin credentials)
curl -X PUT \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic $(echo -n 'admin@dotcms.com:admin' | base64)" \
  -d '{"name": "com.dotcms.health.servlet.HealthProbeServlet", "level": "DEBUG"}' \
  "http://localhost:8080/api/v1/logger"

# Change multiple loggers at once (comma-separated)
curl -X PUT \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic $(echo -n 'admin@dotcms.com:admin' | base64)" \
  -d '{"name": "com.dotcms.health,com.dotmarketing.util", "level": "INFO"}' \
  "http://localhost:8080/api/v1/logger"

# Get current logger levels
curl -H "Authorization: Basic $(echo -n 'admin@dotcms.com:admin' | base64)" \
  "http://localhost:8080/api/v1/logger/com.dotcms.health.servlet.HealthProbeServlet"

# List all current loggers
curl -H "Authorization: Basic $(echo -n 'admin@dotcms.com:admin' | base64)" \
  "http://localhost:8080/api/v1/logger"
```

Valid log levels: `TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR`, `FATAL`, `OFF`

## Git and Development Workflow

### Git Workflow Standards
- **Branch from Main**: Always create feature branches from the main branch
- **Branch Naming Convention**: All branches for PRs must use `issue-{issue number}-` prefix for automatic issue linking:
  ```bash
  # Required format for issue linking
  git checkout -b issue-123-add-new-feature
  git checkout -b issue-456-fix-login-bug
  git checkout -b issue-789-update-documentation
  ```
- **For Human Developers**: Use dotCMS utilities for streamlined workflow:
  ```bash
  # Create GitHub issue with proper labeling
  git issue-create
  
  # Create branch from assigned issue (automatically uses correct naming)
  git issue-branch
  
  # List your assigned issues
  git issue-branch --list
  ```
- **For AI Agents**: Use standard GitHub tools:
  ```bash
  # Create issues using gh CLI
  gh issue create --title "Issue title" --body "Description" --label "bug,enhancement"
  
  # Create branches with proper naming for issue linking
  git checkout -b issue-123-descriptive-name
  gh issue develop 123 --checkout
  
  # List issues
  gh issue list --assignee @me
  ```
- **Issue Templates**: Available templates in `.github/ISSUE_TEMPLATE/`:
  - `task.yaml` - Technical tasks or improvements
  - `defect.yaml` - Bug reports and defects
  - `feature.yaml` - New features and enhancements
  - `spike.yaml` - Research and exploration tasks
  - `epic.yml` - Large initiatives spanning multiple issues
  - `pillar.yml` - Strategic themes
  - `ux.yaml` - UX improvements and design tasks
- **Conventional Commits**: Use conventional commit format for all changes:
  ```
  feat: add new workflow component
  fix: resolve artifact dependency issue
  docs: update workflow documentation
  test: add integration test for build phase
  refactor: improve change detection logic
  ```

### Pull Request Standards
- **Draft PRs**: Create pull requests in draft status initially for review
- **Documentation Updates**: Update relevant documentation files when making changes to:
  - Application behavior or architecture
  - Security procedures or guidelines
  - Testing strategies or new test types
  - Troubleshooting procedures or known issues

## Security Guidelines

### Critical Security Rules

**🚨 NEVER do these in any code:**
```java
// ❌ NEVER: Direct input injection without validation
System.out.println("User input: " + userInput);  // INJECTION RISK

// ❌ NEVER: Hardcoded secrets or keys
String apiKey = "sk-1234567890abcdef";  // SECURITY VIOLATION

// ❌ NEVER: Exposing sensitive information in logs
Logger.info(this, "Password: " + password);  // SECURITY VIOLATION
```

**✅ ALWAYS do these security practices:**
```java
// ✅ Validate and sanitize all user input
if (UtilMethods.isSet(userInput) && userInput.matches("^[a-zA-Z0-9\\s\\-_]+$")) {
    Logger.info(this, "Valid input received");
    processInput(userInput);
} else {
    Logger.warn(this, "Invalid input rejected");
    throw new DotSecurityException("Invalid input format");
}

// ✅ Use Config for sensitive properties
String apiKey = Config.getStringProperty("external.api.key", "");
if (!UtilMethods.isSet(apiKey)) {
    throw new DotDataException("API key not configured");
}

// ✅ Never log sensitive information
Logger.info(this, "Authentication successful for user: " + user.getUserId());
```

### Security Checklist

**Before committing any code:**
- [ ] No hardcoded secrets, passwords, or API keys
- [ ] All user input is validated and sanitized
- [ ] Sensitive information is never logged
- [ ] Proper error handling without information leakage
- [ ] Security boundaries are maintained

## Development Patterns

### Error Handling Pattern
```java
// Standard error handling with proper logging
try {
    performOperation();
    Logger.info(this, "Operation completed successfully");
} catch (DotDataException e) {
    Logger.error(this, "Data operation failed: " + e.getMessage(), e);
    throw new DotRuntimeException("Unable to complete operation", e);
} catch (Exception e) {
    Logger.error(this, "Unexpected error: " + e.getMessage(), e);
    throw new DotRuntimeException("System error occurred", e);
}
```

### Input Validation Pattern
```java
// Comprehensive input validation
public void processUserInput(String input) {
    // Null and empty validation
    if (!UtilMethods.isSet(input)) {
        throw new DotDataException("Input cannot be empty");
    }
    
    // Format validation
    if (!input.matches("^[a-zA-Z0-9\\s\\-_\\.]+$")) {
        Logger.warn(this, "Invalid input format attempted");
        throw new DotSecurityException("Invalid input format");
    }
    
    // Length validation
    if (input.length() > 255) {
        throw new DotDataException("Input exceeds maximum length");
    }
    
    // Business logic validation
    if (isBlacklisted(input)) {
        Logger.warn(this, "Blacklisted input attempted");
        throw new DotSecurityException("Input not allowed");
    }
    
    // Process validated input
    processValidatedInput(input);
}
```

### Debugging Pattern
```java
// Structured debugging information
Logger.debug(this, () -> {
    return String.format("Processing request - User: %s, Action: %s, Parameters: %s",
        user.getUserId(), action, sanitizeForLogging(parameters));
});

// Performance monitoring
long startTime = System.currentTimeMillis();
try {
    performOperation();
} finally {
    long duration = System.currentTimeMillis() - startTime;
    Logger.info(this, "Operation completed in " + duration + "ms");
}
```

## Summary Checklist
- ✅ Use `Config.getProperty()` and `Logger.info(this, ...)`
- ✅ Use `APILocator.getXXXAPI()` for services
- ✅ Use `@Value.Immutable` for data objects
- ✅ Use JAX-RS `@Path` for REST endpoints - **See [REST API Guide](dotCMS/src/main/java/com/dotcms/rest/CLAUDE.md)**
- ✅ Use `data-testid` for Angular testing
- ✅ Use modern Java 21 syntax (Java 11 compatible)
- ✅ Follow domain-driven package organization for new features
- ✅ **@Schema Rules**: Match schema to actual return type (wrapped vs unwrapped) - **See [REST Guide](dotCMS/src/main/java/com/dotcms/rest/CLAUDE.md)**
- ❌ Avoid DWR, Struts, portlets, console logging, direct system properties
- ❌ Avoid Java 21 runtime features in core modules
- ❌ **Never use raw `ResponseEntityView.class` as @Schema implementation**
- ❌ **NEVER use `ResponseEntityView.class`** in `@Schema` - provides no meaningful API documentation
- ❌ **NEVER omit `@Schema`** from @ApiResponse(200) - incomplete Swagger documentation
- ❌ **NEVER use `@PathParam`** without corresponding @Path placeholder - use @QueryParam instead
