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

## Maven Build Structure (CRITICAL)

dotCMS follows a structured Maven build hierarchy with centralized dependency and plugin management:

### Dependency Management Pattern
**⚠️ CRITICAL: All dependencies must follow this pattern:**

1. **Define versions in BOM**: Add new dependency versions to `bom/application/pom.xml`
2. **Reference without version in modules**: Use dependencies in `dotCMS/pom.xml` WITHOUT version numbers
3. **Never override BOM versions**: Let the BOM control all dependency versions

#### Example: Adding a New Dependency
```xml
<!-- 1. Add to bom/application/pom.xml -->
<properties>
    <new-library.version>1.2.3</new-library.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.example</groupId>
            <artifactId>new-library</artifactId>
            <version>${new-library.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>

<!-- 2. Use in dotCMS/pom.xml (NO version) -->
<dependency>
    <groupId>com.example</groupId>
    <artifactId>new-library</artifactId>
</dependency>
```

#### Existing Swagger/OpenAPI Dependencies
```xml
<!-- In bom/application/pom.xml -->
<swagger.version>2.2.0</swagger.version>

<dependency>
    <groupId>io.swagger.core.v3</groupId>
    <artifactId>swagger-jaxrs2</artifactId>
    <version>${swagger.version}</version>
</dependency>
<dependency>
    <groupId>io.swagger.core.v3</groupId>
    <artifactId>swagger-jaxrs2-servlet-initializer</artifactId>
    <version>${swagger.version}</version>
</dependency>

<!-- In dotCMS/pom.xml (versions inherited from BOM) -->
<dependency>
    <groupId>io.swagger.core.v3</groupId>
    <artifactId>swagger-jaxrs2</artifactId>
</dependency>
<dependency>
    <groupId>io.swagger.core.v3</groupId>
    <artifactId>swagger-jaxrs2-servlet-initializer</artifactId>
</dependency>
```

### Plugin Management Pattern
**⚠️ CRITICAL: All plugins must follow this pattern:**

1. **Define plugins in parent POM**: Add plugin versions to `parent/pom.xml` in `<pluginManagement>`
2. **Reference without version in modules**: Use plugins in module POMs WITHOUT version numbers
3. **Global properties**: All global properties are defined in `parent/pom.xml`

#### Example: Adding a New Plugin
```xml
<!-- 1. Add to parent/pom.xml -->
<pluginManagement>
    <plugins>
        <plugin>
            <groupId>com.example</groupId>
            <artifactId>example-maven-plugin</artifactId>
            <version>1.0.0</version>
            <configuration>
                <!-- default configuration -->
            </configuration>
        </plugin>
    </plugins>
</pluginManagement>

<!-- 2. Use in any module POM (NO version) -->
<plugin>
    <groupId>com.example</groupId>
    <artifactId>example-maven-plugin</artifactId>
    <executions>
        <execution>
            <goals>
                <goal>generate</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### Build Hierarchy Summary
```
parent/pom.xml              # Global properties, plugin management
├── bom/application/pom.xml # Dependency management (versions)
└── dotCMS/pom.xml         # Module dependencies (no versions)
```

**Key Rules:**
- **NEVER** add version numbers to dependencies in `dotCMS/pom.xml`
- **NEVER** add version numbers to plugins in module POMs
- **ALWAYS** add new dependency versions to `bom/application/pom.xml`
- **ALWAYS** add new plugin versions to `parent/pom.xml`
- **ALWAYS** define global properties in `parent/pom.xml`

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

### OpenAPI Specification Management
The `dotCMS/src/main/webapp/WEB-INF/openapi/openapi.yaml` file is **automatically generated** during compilation. 

**Handling Merge Conflicts:**
- The file uses Git's "ours" merge strategy - always keeps your current branch version
- **Never manually edit** the OpenAPI YAML file - changes will be overwritten
- **Pre-commit hook** automatically regenerates the file when REST API changes are detected
- **After merge**: The next commit with REST changes will update the OpenAPI spec correctly

**Configuration for stable diffs:**
```xml
<prettyPrint>true</prettyPrint>
<sortOutput>true</sortOutput>
```

**Workflow:**
1. Merge branches normally - OpenAPI conflicts resolve automatically using "ours"
2. Make REST API changes and commit - pre-commit hook regenerates OpenAPI spec
3. OpenAPI file is always consistent with current branch's REST implementation

### Immutable Classes Compilation
When creating new models with `@Value.Immutable`, the concrete classes are generated at compile time. **Always run `./mvnw compile`** after creating abstract immutable interfaces to generate the implementation classes.

### Git Hooks Setup
The project uses husky for pre-commit hooks. On first setup or after pulling changes, you may see:
```
core-web/.husky/pre-commit: line 24: core-web/.husky/_/husky.sh: No such file or directory
```

**Fix**: Run `just build` or `./mvnw clean install` to properly install husky and create the missing `_/husky.sh` file.

### ENOBUFS Error Fix (macOS)
If you encounter `spawnSync /bin/sh ENOBUFS` errors during pre-commit hooks:

**🚀 Automatic Fix**: The pre-commit hook will automatically detect and fix ENOBUFS errors by resetting nx cache and reinstalling dependencies. No manual intervention required!

**Manual Fix** (if auto-fix fails):
```bash
cd core-web
yarn nx reset
yarn install

# For persistent issues, full cleanup:
rm -rf node_modules
yarn install
yarn nx reset
```

**Why this happens**: Large codebases (127k+ files) can cause nx cache corruption and macOS buffer limits to be exceeded.

**Prevention**: Run `yarn nx reset` periodically if builds feel slow or after major dependency updates.

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