# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Essential Commands

### Build Commands
```bash
# Basic builds
./mvnw clean install                    # Full build with Docker
./mvnw clean install -DskipTests       # Fast build without tests
./mvnw install -pl :dotcms-core -DskipTests  # Core module only

# Development with Docker
./mvnw -pl :dotcms-core -Pdocker-start -Dtomcat.port=8080        # Start
./mvnw -pl :dotcms-core -Pdocker-start,debug -Dtomcat.port=8080  # Debug
./mvnw -pl :dotcms-core -Pdocker-stop                            # Stop

# Testing
./mvnw clean install -Dcoreit.test.skip=false    # With integration tests
./mvnw -pl :dotcms-integration verify -Dcoreit.test.skip=false   # Integration only
./mvnw -pl :dotcms-postman verify -Dpostman.test.skip=false -Dpostman.collections=ai  # Postman tests

# Alternative: Use 'just' commands (brew install just)
just build          # ./mvnw clean install -DskipTests
just dev-start-on-port 8080  # Start Docker
just test-integration        # Run integration tests
```

### Frontend Commands (in core-web/)
```bash
yarn install                    # Install dependencies
nx run dotcms-ui:serve         # Serve at http://localhost:4200/dotAdmin
nx run dotcms-ui:test          # Run tests
```

### Development Utilities
Install additional tools: `bash <(curl -fsSL https://raw.githubusercontent.com/dotcms/dotcms-utilities/main/install-dev-scripts.sh)`

## Architecture Overview

**Monorepo Structure:**
- `dotCMS/`: Core Java backend (Java 21 runtime, Java 11 bytecode compatibility)
- `core-web/`: Angular 18.2.3 frontend with Nx, standalone components, signals
- `tools/dotcms-cli/`: CLI tool (full Java 21 features allowed)
- `docker/`, `e2e/`: Docker configs and testing

**Key Technologies:** Spring/CDI, OSGi plugins, immutable models (`@Value.Immutable`), PostgreSQL, Elasticsearch

## Java Version & Coding Standards

**Environment:** Java 21 runtime, **Java 11 syntax required** for core modules

### ✅ Use Java 11 Syntax in Core Modules
```java
// Java 11 compatible syntax only in core modules
var users = userAPI.findActiveUsers();
var contentTypes = contentTypeAPI.findAll();

// Java 11 compatible Optional and Stream operations
Optional<String> value = getValue();
String result = value.orElse("default");

List<String> names = users.stream()
    .map(User::getName)
    .filter(Objects::nonNull)
    .collect(Collectors.toList());

// Traditional string concatenation or String.format()
String query = "SELECT c.identifier, c.title FROM contentlet c " +
               "WHERE c.structure_inode = ?";

// Traditional switch statements
String status;
switch (contentlet.getBaseType()) {
    case CONTENT:
        status = "Content";
        break;
    case HTMLPAGE:
        status = "Page";
        break;
    default:
        status = "Unknown";
}
```

### ✅ Java 21 Syntax Allowed in CLI/Tools Modules Only
```java
// ONLY in tools/dotcms-cli and test modules
var query = """
    SELECT c.identifier, c.title FROM contentlet c 
    WHERE c.structure_inode = ?
    """;

var status = switch (contentlet.getBaseType()) {
    case CONTENT -> "Content";
    case HTMLPAGE -> "Page";
    default -> "Unknown";
};

public record UserInfo(String id, String email, String name) {}
```

### ❌ Avoid Java 21 Runtime Features Everywhere
```java
// DON'T USE anywhere (require Java 21 runtime)
Thread.ofVirtual().start(() -> doWork());  // Virtual threads
SequencedSet<String> set = new LinkedHashSet<>();  // Sequenced collections
```

## dotCMS Development Standards

### Configuration Management
**ALWAYS use `com.dotmarketing.util.Config`:**
```java
import com.dotmarketing.util.Config;

// Hierarchical naming for new properties
boolean enabled = Config.getBooleanProperty("experiments.enabled", false);
String url = Config.getStringProperty("experiments.auto-js-injection.url", "");

// Environment variables automatically get DOT_ prefix
// experiments.enabled → DOT_EXPERIMENTS_ENABLED
```

**Property Resolution Order:** Environment vars (DOT_*) → System table → Properties files

#### Critical Config Pattern Details
**Automatic Environment Variable Transformation:**
```java
// Config.envKey() automatically transforms property names:
"experiments.enabled" → "DOT_EXPERIMENTS_ENABLED"
"health.checks.database.timeout-seconds" → "DOT_HEALTH_CHECKS_DATABASE_TIMEOUT_SECONDS"
"cache.provider" → "DOT_CACHE_PROVIDER"
```

**Property Lookup Process:**
1. Check environment variable with `DOT_` prefix (e.g., `DOT_EXPERIMENTS_ENABLED`)
2. Check system table for both transformed and original names
3. Check properties files for both transformed and original names

**New Property Naming Convention:**
```properties
# Use hierarchical domain-driven naming (RECOMMENDED for new config)
experiments.enabled=true
experiments.auto-js-injection.enabled=true
experiments.auto-js-injection.url=https://example.com/script.js
experiments.auto-js-injection.max-retries=3

health.checks.database.timeout-seconds=30
health.monitoring.include-system-details=true
```

**⚠️ CRITICAL: Never Change Existing Properties Without Migration Strategy**
- Properties used in Docker, Kubernetes, CI/CD are **HIGH RISK** to change
- Even though `Config.envKey()` transformation should work, infrastructure disruption risks are too high
- Examples of HIGH RISK properties: `DATABASE_CONNECTION_TIMEOUT`, `ELASTICSEARCH_URLS`, `CACHE_PROVIDER`

### Logging Standards
**ALWAYS use `com.dotmarketing.util.Logger`:**
```java
import com.dotmarketing.util.Logger;

Logger.info(this, "Operation completed successfully");
Logger.error(this, "Operation failed: " + error.getMessage(), error);

// NEVER use: System.out.println(), printStackTrace(), System.err.println()
```

### Core Patterns

#### API Locator Pattern
```java
// Service access
UserAPI userAPI = APILocator.getUserAPI();
ContentletAPI contentletAPI = APILocator.getContentletAPI();

// Web services
UserWebAPI userWebAPI = WebAPILocator.getUserWebAPI();

// NEVER instantiate services directly
// ❌ UserAPIImpl userAPI = new UserAPIImpl();
```

#### CDI Patterns (For New Components)
```java
@ApplicationScoped
public class MyService {
    private final JobQueueManagerAPI jobQueueManagerAPI;
    
    // Default constructor required for CDI proxy
    public MyService() {
        this.jobQueueManagerAPI = null;
    }
    
    @Inject
    public MyService(JobQueueManagerAPI jobQueueManagerAPI) {
        this.jobQueueManagerAPI = jobQueueManagerAPI;
    }
}

// Safe CDI bean access
Optional<MyService> service = CDIUtils.getBean(MyService.class);
MyService service = CDIUtils.getBeanThrows(MyService.class);
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
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Resource not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized access",
                    content = @Content(mediaType = "application/json"))
    })
    @GET @Path("/{id}") @Produces(MediaType.APPLICATION_JSON) @NoCache
    public Response getById(@Context HttpServletRequest request,
                           @Parameter(description = "Resource identifier", required = true)
                           @PathParam("id") String id) {
        // ALWAYS initialize request context
        InitDataObject initData = webResource.init(request, response, true);
        User user = initData.getUser();
        
        // Business logic
        return Response.ok(new ResponseEntityView<>(result)).build();
    }
    
    @Operation(
        summary = "Create new resource",
        description = "Creates a new resource with the provided data"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", 
                    description = "Resource created successfully",
                    content = @Content(mediaType = "application/json")),
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
        return Response.status(201).entity(new ResponseEntityView<>(result)).build();
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
                content = @Content(mediaType = "application/json")),
    @ApiResponse(responseCode = "4xx/5xx", description = "Error description",
                content = @Content(mediaType = "application/json"))
})

// Parameters - ALL path/query parameters MUST be documented
@Parameter(description = "Parameter purpose", required = true/false)

// Request bodies - ALL POST/PUT/PATCH with bodies MUST be documented
@RequestBody(description = "Body purpose", required = true,
           content = @Content(schema = @Schema(implementation = FormClass.class)))
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
// Modern standalone components with signals
@Component({
    selector: 'dot-my-component',
    standalone: true,
    template: `
        @if (condition()) {
            <div>{{data()}}</div>
        }
        @for (item of items(); track item.id) {
            <div [data-testid]="'item-' + item.id">{{item.name}}</div>
        }
    `
})
export class MyComponent {
    data = input<string>();
    condition = input<boolean>();
    items = input<Item[]>();
    change = output<string>();
}

// Testing with Spectator (REQUIRED pattern)
const createComponent = createComponentFactory({
    component: MyComponent,
    imports: [CommonModule, DotTestingModule],
    providers: [mockProvider(RequiredService)]
});

// ALWAYS use data-testid for element selection
const button = spectator.query(byTestId('submit-button'));

// ALWAYS use setInput for component inputs (NEVER set directly)
spectator.setInput('inputProperty', 'value');
// ❌ spectator.component.inputProperty = 'value';

// CSS class verification - use separate string arguments
expect(icon).toHaveClass('pi', 'pi-update');
// ❌ expect(icon).toHaveClass({ pi: true, 'pi-update': true });

// Test user interactions, not implementation details
spectator.click(byTestId('refresh-button'));
expect(spectator.query(byTestId('success-message'))).toBeVisible();
```

#### CSS Standards (BEM Methodology)
```scss
// Always import variables
@use "variables" as *;

// Use global variables, never hardcoded values
.component {
  padding: $spacing-3;
  color: $color-palette-primary;
  background: $color-palette-gray-100;
  box-shadow: $shadow-m;
}

// BEM with flat structure (no nesting)
.feature-list { }
.feature-list__header { }
.feature-list__item { }
.feature-list__item--active { }
```

## ⚠️ Antipatterns to Avoid

### Legacy Patterns (Maintain consistency in existing code, avoid in new code)
```java
// ❌ AVOID in new development
@Deprecated public class MyPortletAction extends PortletAction {}  // Legacy portlets
@Deprecated public class MyAjax extends WfBaseAction {}            // DWR classes
System.out.println("message");                                     // Console logging
System.getProperty("property");                                    // Direct system props
StructureAPI structureAPI = APILocator.getStructureAPI();         // Legacy Structure API

// ✅ USE modern alternatives
@Path("/v1/resource") public class MyResource {}                   // REST endpoints
Logger.info(this, "message");                                     // dotCMS Logger
Config.getStringProperty("property", "default");                  // dotCMS Config
ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI();   // Modern Content Type API
```

### Package Organization
```java
// ❌ Legacy: com.dotmarketing.portlets.myfeature.*
// ✅ Modern: com.dotcms.myfeature.* (domain-driven)
```

## Key Configuration Files
- **pom.xml**: Maven configuration
- **core-web/package.json**: Node.js dependencies  
- **environments/**: Environment-specific settings
- **~/.dotcms/license/license.dat**: License file (required for full functionality)

## Development Workflow
1. Make changes → `./mvnw install -pl :dotcms-core -DskipTests`
2. Test in Docker → `./mvnw -pl :dotcms-core -Pdocker-start -Dtomcat.port=8080`
3. Integration tests → `./mvnw -pl :dotcms-integration pre-integration-test -Dcoreit.test.skip=false`

### Critical Docker Build Workflow
⚠️ **Important**: Docker image updates only happen when building WITHOUT `-Ddocker.skip`:

```bash
# Fast development cycle (no Docker image update)
./mvnw install -pl :dotcms-core -DskipTests -Ddocker.skip

# When ready to test in Docker (REQUIRED for new servlets/endpoints):
./mvnw -DskipTests clean install  # Updates Docker image
./mvnw -pl :dotcms-core -Pdocker-start -Dtomcat.port=8080
```

**Key Point**: The Docker container runs the image, not your local compiled classes.

### Immutable Classes Compilation
When creating new models with `@Value.Immutable`, the concrete classes are generated at compile time. **Always run `./mvnw compile`** after creating abstract immutable interfaces to generate the implementation classes.

### Health Check System
For comprehensive health check documentation: **[Health Check System Documentation](dotCMS/src/main/java/com/dotcms/health/README.md)**

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

## Summary Checklist
- ✅ Use `Config.getProperty()` and `Logger.info(this, ...)`
- ✅ Use `APILocator.getXXXAPI()` for services
- ✅ Use `@Value.Immutable` for data objects
- ✅ Use JAX-RS `@Path` for REST endpoints with complete Swagger documentation
- ✅ Use `data-testid` for Angular testing
- ✅ Use modern Java 21 syntax (Java 11 compatible)
- ✅ Follow domain-driven package organization for new features
- ✅ **REST Documentation**: All endpoints MUST have `@Tag`, `@Operation`, `@ApiResponses`, `@Parameter`/`@RequestBody`
- ✅ **Media Types**: `@Produces` at method level, `@Consumes` only on endpoints with request bodies
- ✅ **ResponseEntity Views**: Use specific typed view classes (e.g., `ResponseEntityStringView`, `ResponseEntityWorkflowStepsView`) instead of generic `ResponseEntityView`
- ❌ Avoid DWR, Struts, portlets, console logging, direct system properties
- ❌ Avoid Java 21 runtime features in core modules
- ❌ Avoid `@Consumes` on GET endpoints unless they accept request bodies (non-standard)
- ❌ Avoid generic `ResponseEntityView.class` in `@Schema` - use specific view classes