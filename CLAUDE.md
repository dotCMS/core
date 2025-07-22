# CLAUDE.md - dotCMS Development Guide

## 📚 Documentation Structure

### 🤖 Context Strategy for AI Tools
**Claude Integration:**
- This CLAUDE.md file provides **always-available context**
- Detailed `/docs/` files are read **on-demand** via Read tool
- Use `/clear` command between tasks to manage context window

**Cursor Integration:**
- `.cursor/rules/` directory provides **context-aware guidance**
- Critical patterns **always included** in relevant file contexts
- Use `@docs/path/file.md` references for **detailed examples on-demand**

### 🏗️ Core Principles
- **Architecture**: [docs/core/ARCHITECTURE_OVERVIEW.md](docs/core/ARCHITECTURE_OVERVIEW.md)
- **Code Structure**: [docs/core/CODE_STRUCTURE.md](docs/core/CODE_STRUCTURE.md)
- **Security**: [docs/core/SECURITY_PRINCIPLES.md](docs/core/SECURITY_PRINCIPLES.md)
- **Git Workflows**: [docs/core/GIT_WORKFLOWS.md](docs/core/GIT_WORKFLOWS.md)
- **CI/CD Pipeline**: [docs/core/CICD_PIPELINE.md](docs/core/CICD_PIPELINE.md)
- **GitHub Issue Management**: [docs/core/GITHUB_ISSUE_MANAGEMENT.md](docs/core/GITHUB_ISSUE_MANAGEMENT.md)

### 🎯 Backend (Java/Maven)
- **Java Standards**: [docs/backend/JAVA_STANDARDS.md](docs/backend/JAVA_STANDARDS.md)
- **Maven Build**: [docs/backend/MAVEN_BUILD_SYSTEM.md](docs/backend/MAVEN_BUILD_SYSTEM.md)
- **Configuration Patterns**: [docs/backend/CONFIGURATION_PATTERNS.md](docs/backend/CONFIGURATION_PATTERNS.md)
- **REST API Patterns**: [docs/backend/REST_API_PATTERNS.md](docs/backend/REST_API_PATTERNS.md)
- **Database Patterns**: [docs/backend/DATABASE_PATTERNS.md](docs/backend/DATABASE_PATTERNS.md)
- **Security Backend**: [docs/backend/SECURITY_BACKEND.md](docs/backend/SECURITY_BACKEND.md)

### 🎨 Frontend (Angular/TypeScript)
- **Angular Standards**: [docs/frontend/ANGULAR_STANDARDS.md](docs/frontend/ANGULAR_STANDARDS.md)
- **Component Architecture**: [docs/frontend/COMPONENT_ARCHITECTURE.md](docs/frontend/COMPONENT_ARCHITECTURE.md)
- **State Management**: [docs/frontend/STATE_MANAGEMENT.md](docs/frontend/STATE_MANAGEMENT.md)
- **Styling**: [docs/frontend/STYLING_STANDARDS.md](docs/frontend/STYLING_STANDARDS.md)

### 🖥️ CLI (Quarkus/Java 21)
- **CLI Overview**: [docs/cli/CLI_OVERVIEW.md](docs/cli/CLI_OVERVIEW.md)
- **CLI Build System**: [docs/cli/CLI_BUILD_SYSTEM.md](docs/cli/CLI_BUILD_SYSTEM.md)

### 🧪 Testing
- **Backend Unit Tests**: [docs/testing/BACKEND_UNIT_TESTS.md](docs/testing/BACKEND_UNIT_TESTS.md)
- **Frontend Testing**: [docs/frontend/TESTING_FRONTEND.md](docs/frontend/TESTING_FRONTEND.md)
- **Integration Tests**: [docs/testing/INTEGRATION_TESTS.md](docs/testing/INTEGRATION_TESTS.md)
- **API Testing**: [docs/testing/API_TESTING.md](docs/testing/API_TESTING.md)
- **E2E Tests**: [docs/testing/E2E_TESTS.md](docs/testing/E2E_TESTS.md)
- **Performance Tests**: [docs/testing/PERFORMANCE_TESTS.md](docs/testing/PERFORMANCE_TESTS.md)

### 🔗 Integration
- **API Contracts**: [docs/integration/API_CONTRACTS.md](docs/integration/API_CONTRACTS.md)
- **Docker Build**: [docs/infrastructure/DOCKER_BUILD_PROCESS.md](docs/infrastructure/DOCKER_BUILD_PROCESS.md)

### 🤖 AI Guidance
- **Workflow Patterns**: [docs/claude/WORKFLOW_PATTERNS.md](docs/claude/WORKFLOW_PATTERNS.md)
- **Documentation Maintenance**: [docs/claude/DOCUMENTATION_MAINTENANCE.md](docs/claude/DOCUMENTATION_MAINTENANCE.md)
- **GitHub Automation**: [docs/claude/GITHUB_AUTOMATION.md](docs/claude/GITHUB_AUTOMATION.md)

## ⚡ Quick Commands

### Backend Development
```bash
# Fast build
./mvnw install -pl :dotcms-core -DskipTests

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

## GitHub Issue Management

**📋 IMPORTANT: For comprehensive guidance on Issues, PRs, Epics, and Subtasks, see [GitHub Issue Management](docs/core/GITHUB_ISSUE_MANAGEMENT.md)**

### Quick Reference
- **Issue Creation**: Always use `issue-{number}-{description}` branch naming
- **Epic Management**: Use GitHub sub-issues API for proper linking
- **PR Standards**: Link to both Epic and specific Issue
- **Project Integration**: Issues auto-added to "dotCMS - Product Planning V2"

### Common Commands
```bash
# Create issue with dotCMS utilities (preferred)
git issue-create "Task description" --team Platform --type Task --repo dotCMS/core --dry-run
git issue-create "Task description" --team Platform --type Task --repo dotCMS/core --yes

# Create branch matching issue number (creates automatically if doesn't exist)
git smart-switch issue-{issue_number}-{descriptive-name}

# Link subtask to epic (fallback when utilities don't support sub-issues)
gh api -X POST /repos/:owner/:repo/issues/{epic_id}/sub_issues --field sub_issue_id={task_id}

# Check epic progress
gh api /repos/:owner/:repo/issues/{epic_id} --jq '.sub_issues_summary'
```

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
public class MyResource {
    private final WebResource webResource = new WebResource();
    
    @GET @Path("/{id}") @Produces(MediaType.APPLICATION_JSON) @NoCache
    public Response getById(@Context HttpServletRequest request, @PathParam("id") String id) {
        // ALWAYS initialize request context
        InitDataObject initData = webResource.init(request, response, true);
        User user = initData.getUser();
        
        // Business logic
        return Response.ok(new ResponseEntityView<>(result)).build();
    }
}
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

# Integration tests
./mvnw -pl :dotcms-integration verify -Dcoreit.test.skip=false
```

### Frontend Development
```bash
# Development server
cd core-web && nx run dotcms-ui:serve

# Run tests
cd core-web && nx run dotcms-ui:test

# Install dependencies
cd core-web && yarn install
```

### CLI Development
```bash
# Development mode
cd tools/dotcms-cli && ./mvnw quarkus:dev -pl cli

# Build CLI
cd tools/dotcms-cli && ./mvnw clean install

# Run CLI
java -jar tools/dotcms-cli/cli/target/quarkus-app/quarkus-run.jar --help
```

### Git & CI/CD Operations
```bash
# Create feature branch
git checkout -b issue-{number}-{description}

# Commit and push
git add . && git commit -m "Implement feature"
git push -u origin issue-{number}-{description}

# Check dependency conflicts
./mvnw dependency:tree -Dverbose

# Debug CI/CD issues
yamllint .github/workflows/workflow-name.yml
```

## 🎯 Task Context Detection

### Backend Tasks → Read backend docs
- `/dotCMS/` paths, `.java` files, `pom.xml` changes, Docker containers, database operations, REST APIs

### Frontend Tasks → Read frontend docs
- `/core-web/` paths, `.ts/.html/.scss` files, `package.json` changes, Angular components

### CLI Tasks → Read CLI docs
- `/tools/dotcms-cli/` paths, Quarkus applications, PicocLI commands, native image builds

### Git & CI/CD Tasks → Read git/cicd docs
- `.github/workflows/` paths, `.yml` files, GitHub Actions modifications, CI/CD pipeline issues

## 🔄 Development Workflow

1. **Read Relevant Documentation** (use task context detection above)
2. **Use TodoWrite for Multi-Step Tasks** (break complex tasks into manageable steps)
3. **Follow Progressive Enhancement** - Always improve: add generics, use Logger, use Config
4. **Update Documentation** - When you discover wrong assumptions, missing info, or new patterns

### 🤝 Proactive Workflow Assistance

**When user mentions different work context:**
- Suggest branch switching with `git smart-switch issue-{number}-{description}`
- Help organize work by switching to appropriate branches

**When discovering unrelated issues during development:**
- Suggest creating quick issues with `git issue-create` to track problems
- Offer to create issues for bugs/improvements found during current work
- Help maintain clean separation between different problems

**When work is complete:**
- Suggest creating PR with `git issue-pr` for review
- Help link work back to original issues and epics

See: [Documentation Maintenance System](docs/claude/DOCUMENTATION_MAINTENANCE.md)

## 🚨 Critical Reminders

### Security (NEVER violate)
- No hardcoded secrets or credentials
- Validate all user input
- Never log sensitive information
- See: [Security Principles](docs/core/SECURITY_PRINCIPLES.md)

### Build System (NEVER violate)
- Add dependency versions to `bom/application/pom.xml`
- Never add versions to `dotCMS/pom.xml`
- Run `./mvnw compile` after `@Value.Immutable` changes
- See: [Maven Build System](docs/backend/MAVEN_BUILD_SYSTEM.md)

### Testing (ALWAYS required)
- **Backend**: Integration tests for REST endpoints
- **Frontend**: Spectator tests with `data-testid`
- Run tests before completing tasks
- **See**: [Testing Documentation](docs/testing/) for comprehensive test guides

## 📝 Documentation Maintenance

**Core Principle: Right Information in Right Place**
- **Core docs**: Roadmap to locate information, not implementation details
- **Domain docs**: Technology patterns, not class-specific details
- **Code docs**: Class-specific details belong in the code itself
- **Single source of truth**: Each piece of information exists in one place

**When you discover incorrect or missing information:**
1. Identify the right document and location (core/domain/code)
2. Update with specific, actionable information
3. Use cross-references instead of duplicating content
4. Only include information relevant to current task context

See: [Documentation Maintenance System](docs/claude/DOCUMENTATION_MAINTENANCE.md)