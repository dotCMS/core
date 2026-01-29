# Copilot Coding Agent Instructions for dotCMS Core

> **Purpose**: This file provides essential information for AI coding agents working with the dotCMS repository. It contains critical patterns, common errors, and workflows to work efficiently.

## Repository Overview

dotCMS is a **Universal Content Management System** - a large-scale enterprise Java/Angular CMS with:
- **Maven multi-module project** (~30+ modules) for backend
- **Nx monorepo** for Angular frontend applications and libraries
- **Mature codebase** with 15+ years of history (mix of modern and legacy patterns)

**Tech Stack:**
- **Backend**: Java 21 runtime (Java 11 syntax for core), Maven 3.9+, JAX-RS REST APIs
- **Frontend**: Angular 19+, TypeScript 5.6+, Nx 20.5+, PrimeNG 17.18+
- **Infrastructure**: Docker, PostgreSQL, Elasticsearch, Tomcat 9
- **Testing**: JUnit 5, Jest, Spectator, Playwright, Postman

**Key Characteristics:**
- Monolithic architecture with modular design
- Heavy use of dependency injection (CDI/Guice)
- Immutable objects pattern (Immutables library)
- RESTful APIs with OpenAPI/Swagger documentation
- Reactive patterns with signals in frontend

## Environment Requirements

**CRITICAL: Build will fail without correct versions**

| Tool | Version | Installation | Verification |
|------|---------|--------------|--------------|
| Java | 21.0.8+ | `sdk env install` (SDKMAN) | `java -version` |
| Node.js | 22.15.0+ | `nvm use` (from `.nvmrc`) | `node --version` |
| Maven | 3.9+ | Wrapper included (`./mvnw`) | `./mvnw --version` |
| Docker | Latest | [Docker Desktop](https://www.docker.com/products/docker-desktop) | `docker --version` |

**Common Setup Error #1 - Wrong Java Version:**
```bash
# Error: "Building this project requires JDK version 21 or higher"
# Solution: Install Java 21 with SDKMAN
sdk env install  # Uses .sdkmanrc file
sdk use java 21.0.8-ms
```

## Build Commands (Choose the Right One!)

**⚠️ CRITICAL**: Build times vary significantly (2-15 min). Choose based on your changes.

### Quick Reference
```bash
# ❌ WRONG - Missing dependencies
./mvnw install -pl :dotcms-core -DskipTests
# Error: "Cannot resolve in-project dependency: com.dotcms:dotcms-core-web"

# ✅ CORRECT - For simple backend changes (~2-3 min)
./mvnw install -pl :dotcms-core --am -DskipTests
# The --am flag builds required dependencies

# ✅ Full build without Docker (~5-8 min)
./mvnw clean install -DskipTests -Ddocker.skip

# ✅ Full build with Docker image (~8-15 min)
./mvnw clean install -DskipTests
```

### Using Just Commands (Optional)
```bash
# Just provides shorter aliases for common tasks
brew install just  # Install once

just build-quicker              # Same as: ./mvnw install -pl :dotcms-core --am -DskipTests
just build                      # Same as: ./mvnw clean install -DskipTests
just build-no-docker            # Same as: ./mvnw clean install -DskipTests -Ddocker.skip
```

## Testing Commands

**⚠️ CRITICAL: Never run full integration suite (60+ min). Always target specific tests.**

### Backend Testing Strategy

**Common Test Error - Tests Silently Skipped:**
```bash
# ❌ WRONG - Tests are skipped by default!
./mvnw verify -pl :dotcms-integration
# No error, but tests don't run

# ✅ CORRECT - Explicit flag required
./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dit.test=MyTest
```

**Recommended Testing Workflow:**

```bash
# 1. Specific integration test class (~2-10 min) - RECOMMENDED
./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dit.test=ContentTypeAPIImplTest

# 2. Specific test method (~30 sec - 2 min)
./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dit.test=MyTest#testMethod

# 3. IDE debugging workflow (FASTEST iteration)
just test-integration-ide       # Start services (PostgreSQL + Elasticsearch + dotCMS)
# → Run/debug individual tests in your IDE (10-30 sec per test)
just test-integration-stop      # Clean up when done

# 4. JVM unit tests only (~30 sec)
./mvnw test -pl :dotcms-core

# 5. Postman API tests (specific collection)
./mvnw verify -pl :dotcms-postman -Dpostman.test.skip=false -Dpostman.collections=ai
just test-postman ai            # Shorter command

# ⚠️ NEVER DO THIS during development (60+ min):
./mvnw verify -Dcoreit.test.skip=false  # Runs ALL integration tests
```

### Frontend Testing Commands
```bash
cd core-web

# Install dependencies first
yarn install                    # Required before first test

# Run specific component tests (RECOMMENDED)
nx run dotcms-ui:test --testNamePattern="ContentTypeComponent"

# Run all tests in a file
nx run dotcms-ui:test --testPathPattern="dot-edit-content"

# Run all unit tests
nx run dotcms-ui:test

# Test only affected by your changes
nx affected -t test --exclude='tag:skip:test'

# Lint code
nx run dotcms-ui:lint --fix

# Development server (separate from backend)
nx run dotcms-ui:serve          # Available at http://localhost:4200
```

## Project Structure

```
core/
├── dotCMS/                       # Main backend Java code
│   ├── src/main/java/com/       # Java source files
│   │   ├── dotcms/              # Modern domain-driven packages
│   │   └── dotmarketing/        # Legacy packages
│   └── src/main/webapp/         # JSP views, static assets
├── core-web/                     # Frontend (Angular/Nx monorepo)
│   ├── apps/                    # Applications
│   │   ├── dotcms-ui/          # Main admin UI
│   │   ├── dotcms-block-editor/ # Block editor app
│   │   └── dotcms-binary-field-builder/
│   └── libs/                    # Shared libraries
│       ├── sdk/                # External SDKs (client, react, angular)
│       ├── data-access/        # API services
│       ├── ui/                 # Shared UI components
│       ├── portlets/           # Feature modules
│       └── dotcms-models/      # TypeScript interfaces
├── dotcms-integration/           # Integration tests
├── dotcms-postman/               # Postman API tests
├── test-karate/                  # Karate API tests
├── e2e/                          # E2E tests (Playwright)
├── bom/                          # Bill of Materials
│   └── application/pom.xml      # ⚠️ DEPENDENCY VERSIONS GO HERE
├── parent/pom.xml                # Plugin management
├── pom.xml                       # Root aggregator POM
├── justfile                      # Task runner commands
└── .github/workflows/            # CI/CD pipelines
    ├── cicd_1-pr.yml            # PR builds
    ├── cicd_2-merge-queue.yml   # Merge queue
    ├── cicd_3-trunk.yml         # Trunk builds
    └── cicd_4-nightly.yml       # Nightly builds
```

## Critical Patterns (Always Follow)

### Maven Dependency Management

**⚠️ CRITICAL RULE: Add dependency versions ONLY to `bom/application/pom.xml`**

```xml
<!-- ❌ WRONG - Adding version to module POM -->
<!-- In dotCMS/pom.xml -->
<dependency>
    <groupId>com.example</groupId>
    <artifactId>my-library</artifactId>
    <version>1.2.3</version>  <!-- NO! This will cause conflicts -->
</dependency>

<!-- ✅ CORRECT - Version in BOM, no version in module -->
<!-- Step 1: In bom/application/pom.xml -->
<properties>
    <my-library.version>1.2.3</my-library.version>
</properties>
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.example</groupId>
            <artifactId>my-library</artifactId>
            <version>${my-library.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>

<!-- Step 2: In dotCMS/pom.xml or other module -->
<dependency>
    <groupId>com.example</groupId>
    <artifactId>my-library</artifactId>
    <!-- NO version here - inherited from BOM -->
</dependency>
```

**Why**: Centralized version management prevents conflicts across 30+ modules.

### Java Coding Patterns

**ALWAYS use these dotCMS utility classes (not standard Java equivalents):**

```java
// ✅ Configuration - Use Config class (NOT System.getProperty)
import com.dotmarketing.util.Config;
String value = Config.getStringProperty("key", "default");
boolean enabled = Config.getBooleanProperty("feature.enabled", false);
int timeout = Config.getIntProperty("timeout.seconds", 30);

// ✅ Logging - Use Logger class (NOT System.out or Log4j directly)
import com.dotmarketing.util.Logger;
Logger.info(this, "Operation completed successfully");
Logger.error(this, "Error occurred: " + e.getMessage(), e);
Logger.debug(this, () -> "Expensive string: " + computeExpensiveString());

// ✅ Services - Use APILocator (NOT direct instantiation)
import com.dotcms.api.system.APILocator;
ContentletAPI contentletAPI = APILocator.getContentletAPI();
UserAPI userAPI = APILocator.getUserAPI();
PermissionAPI permissionAPI = APILocator.getPermissionAPI();

// ✅ Null checking - Use UtilMethods (NOT manual null checks)
import com.dotmarketing.util.UtilMethods;
if (UtilMethods.isSet(myString)) {  // Checks null, empty, "null" string
    processString(myString);
}

// Safe supplier pattern for nested null checks
String value = UtilMethods.isSet(() -> complex.getObject().getValue())
    ? complex.getObject().getValue()
    : "default";

// ✅ Collections - Use CollectionsUtils
import com.dotcms.util.CollectionsUtils;
List<String> list = CollectionsUtils.list("item1", "item2");
Map<String, Object> map = CollectionsUtils.map("key1", "value1", "key2", "value2");
```

### Immutable Objects Pattern

**Use Immutables library for data objects:**

```java
import org.immutables.value.Value;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableMyEntity.class)
@JsonDeserialize(as = ImmutableMyEntity.class)
public abstract class MyEntity {
    public abstract String name();
    public abstract Optional<String> description();
    
    @Value.Default
    public boolean enabled() { return true; }
    
    // Builder convenience method
    public static Builder builder() {
        return ImmutableMyEntity.builder();
    }
}

// ⚠️ IMPORTANT: Run ./mvnw compile after creating @Value.Immutable classes
// The annotation processor generates ImmutableMyEntity at compile time

// Usage:
MyEntity entity = MyEntity.builder()
    .name("test")
    .description("optional description")
    .enabled(false)
    .build();
```

### REST API Patterns (JAX-RS)

**Complete REST endpoint pattern with OpenAPI documentation:**

```java
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.parameters.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;

@Path("/v1/resource")
@Tag(name = "Resource", description = "Resource operations")
public class ResourceEndpoint {
    private final WebResource webResource = new WebResource();
    
    @GET
    @Path("/{id}")
    @Operation(
        summary = "Get by ID",
        description = "Retrieves a resource by its identifier"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Resource found",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = ResponseEntityResourceView.class)
        )
    )
    @ApiResponse(responseCode = "404", description = "Resource not found")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public Response getById(
            @Context HttpServletRequest request,
            @Context HttpServletResponse response,
            @Parameter(description = "Resource ID", required = true)
            @PathParam("id") String id) {
        
        // ALWAYS initialize request context for authentication/permissions
        InitDataObject initData = webResource.init(request, response, true);
        User user = initData.getUser();
        
        // Business logic here
        Resource resource = resourceAPI.findById(id, user);
        
        return Response.ok(new ResponseEntityResourceView(resource)).build();
    }
}
```

**⚠️ CRITICAL: OpenAPI Documentation Rules**
- ALWAYS add `@Tag` at class level
- ALWAYS add `@Operation` to every endpoint
- ALWAYS add `@ApiResponse` for 200 and error codes
- ALWAYS specify `@Schema(implementation = SpecificView.class)` - NEVER use generic `ResponseEntityView.class`
- For path parameters: Use `@PathParam` with matching `@Path` placeholder
- For query parameters: Use `@QueryParam` (e.g., `?filter=value`)
- ALWAYS add `@NoCache` for REST endpoints that return dynamic data

### Angular/Frontend Patterns

**Modern Angular syntax (REQUIRED - no legacy patterns):**

```typescript
// ✅ CORRECT: Modern control flow (Angular 19+)
@if (condition()) {
    <div>Content</div>
}
@for (item of items(); track item.id) {
    <div>{{ item.name }}</div>
}

// ❌ WRONG: Legacy structural directives
<div *ngIf="condition">Content</div>
<div *ngFor="let item of items">{{ item.name }}</div>

// ✅ CORRECT: Modern inputs/outputs (signals)
export class MyComponent {
    data = input<string>();                    // NOT @Input()
    onChange = output<string>();               // NOT @Output()
    
    // Computed values
    displayValue = computed(() => this.data().toUpperCase());
}

// ✅ CORRECT: Testing with Spectator
describe('MyComponent', () => {
    let spectator: Spectator<MyComponent>;
    
    beforeEach(() => {
        spectator = createComponentFactory({
            component: MyComponent
        })();
    });
    
    it('should update on input change', () => {
        // ✅ ALWAYS use spectator.setInput()
        spectator.setInput('data', 'test value');
        spectator.detectChanges();
        
        // ✅ ALWAYS use data-testid for selectors
        const button = spectator.query('[data-testid="submit-button"]');
        expect(button).toBeVisible();
        
        // ✅ Test user interactions
        spectator.click('[data-testid="submit-button"]');
        expect(spectator.query('[data-testid="success-message"]')).toExist();
    });
});

// ❌ WRONG: Direct property access in tests
spectator.component.data = 'value';  // NO! Use setInput()
```

## Security Guidelines

**⚠️ CRITICAL: Security violations are unacceptable and will fail code review.**

### Never Do These (Security Violations)

```java
// ❌ NEVER: Hardcoded secrets or credentials
String apiKey = "sk-1234567890";  // SECURITY VIOLATION
String password = "admin123";      // SECURITY VIOLATION

// ❌ NEVER: Direct input injection without validation
String sql = "SELECT * FROM users WHERE name = '" + userInput + "'";  // SQL INJECTION

// ❌ NEVER: Exposing sensitive data in logs
Logger.info(this, "Password: " + password);  // SECURITY VIOLATION
Logger.info(this, "API Key: " + apiKey);     // SECURITY VIOLATION

// ❌ NEVER: Using System.out/err for any output
System.out.println("Debug info");  // Use Logger instead
```

### Always Do These (Security Best Practices)

```java
// ✅ ALWAYS: Validate and sanitize user input
import com.dotmarketing.util.UtilMethods;

public void processInput(String userInput) {
    // Null/empty check
    if (!UtilMethods.isSet(userInput)) {
        throw new DotDataException("Input cannot be empty");
    }
    
    // Format validation (whitelist approach)
    if (!userInput.matches("^[a-zA-Z0-9\\s\\-_\\.]+$")) {
        Logger.warn(this, "Invalid input format attempted");
        throw new DotSecurityException("Invalid input format");
    }
    
    // Length validation
    if (userInput.length() > 255) {
        throw new DotDataException("Input exceeds maximum length");
    }
    
    // Process validated input
    processValidatedInput(userInput);
}

// ✅ ALWAYS: Use Config for sensitive properties
String apiKey = Config.getStringProperty("external.api.key", "");
if (!UtilMethods.isSet(apiKey)) {
    throw new DotDataException("API key not configured");
}

// ✅ ALWAYS: Use proper exception handling
try {
    riskyOperation();
    Logger.info(this, "Operation completed successfully");
} catch (SQLException e) {
    Logger.error(this, "Database operation failed", e);
    throw new DotDataException("Failed to process request", e);
} catch (Exception e) {
    Logger.error(this, "Unexpected error", e);
    throw new DotRuntimeException("System error occurred", e);
}

// ✅ ALWAYS: Log safely (no sensitive data)
Logger.info(this, "User authenticated: " + user.getUserId());  // Log ID, not password
Logger.debug(this, () -> "Processing " + items.size() + " items");  // Lazy evaluation
```

## Common Issues and Solutions

### Issue #1: Build Fails - Wrong Java Version

**Error:**
```
[ERROR] Rule 1: org.apache.maven.enforcer.rules.version.RequireJavaVersion failed
[ERROR] Building this project requires JDK version 21 or higher
```

**Solution:**
```bash
# Check current version
java -version

# Install Java 21 with SDKMAN
sdk env install          # Uses .sdkmanrc file
sdk use java 21.0.8-ms

# Verify
java -version  # Should show 21.0.8
```

### Issue #2: Build Fails - Missing Dependencies

**Error:**
```
[ERROR] Failed to calculate checksums for dotcms-core: 
Cannot resolve in-project dependency: com.dotcms:dotcms-core-web:war:1.0.0-SNAPSHOT
```

**Solution:**
```bash
# ❌ WRONG - Missing --am flag
./mvnw install -pl :dotcms-core -DskipTests

# ✅ CORRECT - Include dependencies with --am flag
./mvnw install -pl :dotcms-core --am -DskipTests
```

### Issue #3: Tests Are Silently Skipped

**Symptom:** Test command succeeds but no tests run.

**Solution:**
```bash
# Tests are skipped by default. Add explicit flags:
-Dcoreit.test.skip=false   # For integration tests
-Dpostman.test.skip=false  # For Postman tests
-Dkarate.test.skip=false   # For Karate tests

# Example:
./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false
```

### Issue #4: Frontend Build Fails - Node Version

**Error:**
```
error @angular/compiler-cli@19.2.15: The engine "node" is incompatible
```

**Solution:**
```bash
# Check required version
cat .nvmrc  # Shows: v22.15.0

# Install and use correct version
nvm install 22.15.0
nvm use 22.15.0

# Verify
node --version  # Should show v22.15.0
```

### Issue #5: Frontend Build Fails - Puppeteer ARM64

**Error (on Apple M1/M2 Macs):**
```
[ERROR] The chromium binary is not available for arm64
```

**Solution:**
```bash
# Install Chromium for ARM64
brew install chromium

# Set environment variables (add to .zshrc or .bashrc)
export PUPPETEER_EXECUTABLE_PATH=$(which chromium)
export PUPPETEER_SKIP_CHROMIUM_DOWNLOAD=true

# Reload shell
source ~/.zshrc

# Reinstall dependencies
cd core-web && yarn install
```

### Issue #6: Dependency Version Conflict

**Error:**
```
[ERROR] Dependency convergence error for com.example:library:jar
```

**Solution:**
```bash
# Check dependency tree
./mvnw dependency:tree -pl :dotcms-core | grep "library"

# Add version to BOM (NOT module POM!)
# Edit bom/application/pom.xml:
<properties>
    <library.version>1.2.3</library.version>
</properties>
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.example</groupId>
            <artifactId>library</artifactId>
            <version>${library.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### Issue #7: Docker Build Fails

**Error:** Docker image build fails or times out.

**Solution:**
```bash
# Skip Docker build during development
./mvnw clean install -DskipTests -Ddocker.skip

# Or use Just command
just build-no-docker
```

### Issue #8: "Cannot Find Symbol" After Adding @Value.Immutable

**Error:**
```
[ERROR] cannot find symbol: class ImmutableMyEntity
```

**Solution:**
```bash
# Immutables are generated at compile time
# Run compile phase to generate classes
./mvnw compile -pl :dotcms-core

# Then continue with your build
./mvnw install -pl :dotcms-core --am -DskipTests
```

## CI/CD and Validation

### What Triggers CI Builds

**File patterns that trigger builds** (from `.github/filters.yaml`):

- **Backend Changes**: `dotCMS/**`, `bom/**`, `parent/**`, `pom.xml`, `dotcms-integration/**`
- **Frontend Changes**: `core-web/**`, `dotCMS/src/main/webapp/html/**/*.{css,js}`
- **CLI Changes**: `tools/dotcms-cli/**`
- **Full Build Trigger**: `.sdkmanrc`, `.nvmrc`, workflow files

### CI Workflows

| Workflow | Trigger | Purpose |
|----------|---------|---------|
| `cicd_1-pr.yml` | Pull request | PR validation |
| `cicd_2-merge-queue.yml` | Merge queue | Pre-merge validation |
| `cicd_3-trunk.yml` | Push to main | Trunk integration |
| `cicd_4-nightly.yml` | Schedule (nightly) | Full test suite |

### Pre-Commit Checklist

Before committing code, ensure:

1. ✅ **Build succeeds**: `./mvnw install -pl :dotcms-core --am -DskipTests`
2. ✅ **Tests pass**: Run relevant tests for your changes (specific test classes)
3. ✅ **No hardcoded secrets**: Check for API keys, passwords, credentials
4. ✅ **Dependency versions in BOM**: Check `bom/application/pom.xml`
5. ✅ **OpenAPI annotations**: REST endpoints have complete `@Operation` and `@ApiResponse`
6. ✅ **Security validation**: Input validation, no SQL injection risks
7. ✅ **Logging uses Logger**: No `System.out.println()`

## Development Workflows

### Workflow #1: Simple Backend Code Change

```bash
# 1. Make code changes in dotCMS/src/main/java/

# 2. Build with dependencies (~2-3 min)
./mvnw install -pl :dotcms-core --am -DskipTests

# 3. Run specific test (~2-10 min)
./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dit.test=MyTest

# 4. Commit changes
git add .
git commit -m "feat: add new feature"
```

### Workflow #2: Adding New REST Endpoint

```bash
# 1. Create endpoint class in dotCMS/src/main/java/com/dotcms/rest/api/v1/

# 2. Add complete OpenAPI documentation
#    - @Tag at class level
#    - @Operation for each method
#    - @ApiResponse with specific @Schema

# 3. Build
./mvnw install -pl :dotcms-core --am -DskipTests

# 4. Test endpoint with Postman or curl
curl -X GET http://localhost:8080/api/v1/myresource/123

# 5. Run Postman API tests
./mvnw verify -pl :dotcms-postman -Dpostman.test.skip=false -Dpostman.collections=all
```

### Workflow #3: Frontend Component Development

```bash
# 1. Navigate to frontend
cd core-web

# 2. Install dependencies (first time only)
yarn install

# 3. Start development server
nx serve dotcms-ui
# Available at http://localhost:4200

# 4. Make changes in apps/dotcms-ui/src/ or libs/

# 5. Run component tests
nx run dotcms-ui:test --testNamePattern="MyComponent"

# 6. Lint and fix
nx run dotcms-ui:lint --fix

# 7. Build for production
nx build dotcms-ui
```

### Workflow #4: IDE Integration Test Debugging

```bash
# 1. Build the project once
./mvnw clean install -DskipTests

# 2. Start integration test services
just test-integration-ide
# This starts PostgreSQL, Elasticsearch, and dotCMS

# 3. In your IDE (IntelliJ/Eclipse):
#    - Navigate to test class in dotcms-integration/
#    - Set breakpoints
#    - Right-click test method → Debug
#    - Tests run in ~10-30 seconds

# 4. Make code changes, rebuild
./mvnw install -pl :dotcms-core --am -DskipTests

# 5. Re-run test in IDE (no need to restart services)

# 6. Stop services when done
just test-integration-stop
```

### Workflow #5: Adding New Maven Dependency

```bash
# 1. Add version to bom/application/pom.xml
<properties>
    <my-library.version>1.2.3</my-library.version>
</properties>
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.example</groupId>
            <artifactId>my-library</artifactId>
            <version>${my-library.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>

# 2. Add dependency to module (NO version)
# In dotCMS/pom.xml or other module
<dependency>
    <groupId>com.example</groupId>
    <artifactId>my-library</artifactId>
</dependency>

# 3. Build to verify
./mvnw install -pl :dotcms-core --am -DskipTests

# 4. Check for conflicts
./mvnw dependency:tree -pl :dotcms-core | grep "my-library"
```

## Key Files Reference

### Essential Files for Development

| File | Purpose | When to Edit |
|------|---------|--------------|
| `bom/application/pom.xml` | Dependency versions | Adding/updating dependencies |
| `parent/pom.xml` | Plugin configuration | Changing build plugins |
| `pom.xml` | Root aggregator | Adding new modules |
| `justfile` | Task shortcuts | Creating new commands |
| `.sdkmanrc` | Java version | Never (managed by team) |
| `.nvmrc` | Node version | Never (managed by team) |
| `.github/filters.yaml` | CI triggers | Rarely (CI team) |
| `.github/workflows/` | CI/CD pipelines | Rarely (CI team) |

### Configuration Files

| File | Purpose |
|------|---------|
| `dotCMS/src/main/resources/dotcms-config-default.properties` | Default configuration |
| `dotCMS/src/main/webapp/WEB-INF/web.xml` | Web application descriptor |
| `core-web/nx.json` | Nx workspace configuration |
| `core-web/tsconfig.base.json` | TypeScript compiler options |
| `core-web/package.json` | Frontend dependencies |

### Where to Find Code

| Feature | Location |
|---------|----------|
| REST APIs | `dotCMS/src/main/java/com/dotcms/rest/api/` |
| Contentlet API | `dotCMS/src/main/java/com/dotcms/contenttype/` |
| Workflow | `dotCMS/src/main/java/com/dotcms/workflow/` |
| Storage | `dotCMS/src/main/java/com/dotcms/storage/` |
| Integrations | `dotCMS/src/main/java/com/dotcms/integrations/` |
| Util classes | `dotCMS/src/main/java/com/dotmarketing/util/` |
| Frontend UI | `core-web/apps/dotcms-ui/` |
| Shared components | `core-web/libs/ui/` |
| SDKs | `core-web/libs/sdk/` |
| Data services | `core-web/libs/data-access/` |

## Additional Resources

### Documentation
- **Full Development Guide**: [`CLAUDE.md`](/CLAUDE.md) - Comprehensive patterns and examples
- **Backend Onboarding**: [`dotBackendOnboarding.md`](/dotBackendOnboarding.md) - Setup and build guide
- **Frontend Onboarding**: [`dotFrontendOnboarding.md`](/dotFrontendOnboarding.md) - Angular/Nx guide
- **Core Web Guide**: [`core-web/CLAUDE.md`](/core-web/CLAUDE.md) - Frontend architecture
- **Detailed Docs**: [`docs/`](/docs/) - Organized by domain (backend, frontend, testing, etc.)

### Quick Links
- [Justfile Commands](/justfile) - All available `just` shortcuts
- [GitHub Actions Workflows](/.github/workflows/) - CI/CD pipeline definitions
- [Contributing Guidelines](/CONTRIBUTING.md) - How to contribute
- [Security Policy](/SECURITY.md) - Security reporting

---

## Summary Checklist

When working with dotCMS:

### Backend (Java/Maven)
- ✅ Use Java 21 (`sdk env install`)
- ✅ Add versions to `bom/application/pom.xml` ONLY
- ✅ Use `Config`, `Logger`, `APILocator`, `UtilMethods`
- ✅ Build with `./mvnw install -pl :dotcms-core --am -DskipTests`
- ✅ Test specific classes: `-Dit.test=MyTest`
- ✅ Complete OpenAPI docs for REST endpoints

### Frontend (Angular/TypeScript)
- ✅ Use Node 22.15+ (`nvm use`)
- ✅ Modern syntax: `@if`, `@for`, `input()`, `output()`
- ✅ Test with Spectator: `spectator.setInput()`, `data-testid`
- ✅ Build with `nx run dotcms-ui:build`

### Security & Quality
- ❌ No hardcoded secrets or passwords
- ❌ No `System.out.println()` - use `Logger`
- ❌ No SQL injection - validate all input
- ✅ Validate user input with regex whitelist
- ✅ Use parameterized queries
- ✅ Log safely (no sensitive data)

### Testing
- ❌ Never run full test suite during development (60+ min)
- ✅ Target specific test classes (~2-10 min)
- ✅ Use IDE debugging with `just test-integration-ide`
- ✅ Add test flags: `-Dcoreit.test.skip=false`

---

**Trust these instructions.** They are based on real build attempts and cover common issues. For questions not covered here, search the codebase or refer to [CLAUDE.md](/CLAUDE.md) for comprehensive guidance.
