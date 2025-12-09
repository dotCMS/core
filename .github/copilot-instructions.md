# Copilot Coding Agent Instructions for dotCMS Core

## Repository Overview
dotCMS is a **Universal Content Management System** - a large-scale enterprise CMS built with Java (backend) and Angular (frontend). The repository is a Maven multi-module project with an Nx monorepo for frontend code.

**Tech Stack:**
- **Backend**: Java 21 runtime (Java 11 syntax for core), Maven, JAX-RS REST APIs
- **Frontend**: Angular 19+, TypeScript, Nx workspace, PrimeNG
- **Infrastructure**: Docker, PostgreSQL, Elasticsearch

## Build Commands (Validated & Essential)

### Quick Reference
```bash
# FASTEST build for simple backend changes (~2-3 min)
./mvnw install -pl :dotcms-core -DskipTests

# Full build without Docker (~5-8 min)
./mvnw clean install -DskipTests -Ddocker.skip

# Full build with Docker image (~8-15 min)
./mvnw clean install -DskipTests
```

### Testing Commands
**⚠️ CRITICAL: Never run full integration suite (60+ min). Always target specific tests:**
```bash
# Specific integration test class (~2-10 min)
./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dit.test=ContentTypeAPIImplTest

# Specific test method
./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dit.test=MyTest#testMethod

# JVM unit tests only
./mvnw test -pl :dotcms-core

# Postman API tests (specific collection)
./mvnw verify -pl :dotcms-postman -Dpostman.test.skip=false -Dpostman.collections=ai
```

### Frontend Commands
```bash
cd core-web
yarn install                    # Install dependencies
nx run dotcms-ui:serve          # Development server
nx run dotcms-ui:test           # Run tests
nx run dotcms-ui:lint           # Lint code
nx affected -t test             # Test affected projects
```

## Project Structure

```
core/
├── dotCMS/                    # Main backend Java code
│   └── src/main/java/com/    # Java source files
├── core-web/                  # Frontend (Angular/Nx monorepo)
│   ├── apps/dotcms-ui/       # Main admin UI
│   └── libs/                 # Shared libraries and SDKs
├── dotcms-integration/        # Integration tests
├── dotcms-postman/            # Postman API tests
├── bom/application/pom.xml   # Dependency versions (ADD versions here)
├── parent/pom.xml            # Plugin management
└── .github/workflows/        # CI/CD pipelines
```

## Critical Patterns (Always Follow)

### Maven Dependency Management
**ALWAYS add dependency versions to `bom/application/pom.xml`, NEVER to module POMs:**
```xml
<!-- In bom/application/pom.xml -->
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
```

### Java Coding Patterns
```java
// Configuration - ALWAYS use Config class
import com.dotmarketing.util.Config;
String value = Config.getStringProperty("key", "default");

// Logging - ALWAYS use Logger class
import com.dotmarketing.util.Logger;
Logger.info(this, "message");

// Services - ALWAYS use APILocator
import com.dotcms.api.system.APILocator;
ContentletAPI contentletAPI = APILocator.getContentletAPI();

// Null checking - ALWAYS use UtilMethods
import com.dotmarketing.util.UtilMethods;
if (UtilMethods.isSet(myString)) { }
```

### REST API Patterns
```java
@Path("/v1/resource")
@Tag(name = "Resource", description = "Resource operations")
public class ResourceEndpoint {
    private final WebResource webResource = new WebResource();
    
    @GET @Path("/{id}")
    @Operation(summary = "Get by ID")
    @ApiResponse(responseCode = "200", content = @Content(
        schema = @Schema(implementation = ResponseEntityResourceView.class)))
    @Produces(MediaType.APPLICATION_JSON)
    public Response getById(@Context HttpServletRequest request, 
            @Context HttpServletResponse response, @PathParam("id") String id) {
        InitDataObject initData = webResource.init(request, response, true);
        // Business logic
    }
}
```

### Angular/Frontend Patterns
```typescript
// Modern control flow (REQUIRED)
@if (condition()) { <content /> }
@for (item of items(); track item.id) { }

// Modern inputs/outputs (REQUIRED)
data = input<string>();
onChange = output<string>();

// Testing - use data-testid
<button data-testid="submit-button">Submit</button>
spectator.setInput('prop', value);  // ALWAYS use setInput
```

## CI/CD and Validation

### What Triggers CI
Changes to these paths trigger builds (from `.github/filters.yaml`):
- **Backend**: `dotCMS/**`, `bom/**`, `parent/**`, `pom.xml`, `dotcms-integration/**`
- **Frontend**: `core-web/**`
- **CLI**: `tools/dotcms-cli/**`

### Required Test Flags
Tests are skipped by default. Enable with explicit flags:
```bash
-Dcoreit.test.skip=false   # Integration tests
-Dpostman.test.skip=false  # Postman tests
-Dkarate.test.skip=false   # Karate tests
```

### Validation Checklist
Before committing:
1. Run relevant tests for changed code
2. Check no hardcoded secrets or sensitive data
3. Verify dependency versions are in `bom/application/pom.xml`
4. For REST endpoints: include Swagger/OpenAPI annotations

## Key Files Reference

| Purpose | Location |
|---------|----------|
| Backend source | `dotCMS/src/main/java/com/dotcms/` |
| Frontend source | `core-web/apps/dotcms-ui/`, `core-web/libs/` |
| Dependency versions | `bom/application/pom.xml` |
| Plugin versions | `parent/pom.xml` |
| Integration tests | `dotcms-integration/src/test/java/` |
| CI workflows | `.github/workflows/cicd_*.yml` |
| Change detection | `.github/filters.yaml` |

## Common Issues and Solutions

| Issue | Solution |
|-------|----------|
| Build fails with Java version | Requires Java 21. Set with SDKMAN: `sdk env install` |
| Tests skipped silently | Add `-D<type>.test.skip=false` flag |
| Frontend build fails | Run `yarn install` first, requires Node 22.15+ |
| Dependency version conflict | Check `bom/application/pom.xml`, run `./mvnw dependency:tree` |
| Docker build fails | Use `-Ddocker.skip` for non-Docker builds |

## Environment Requirements
- **Java**: 21.0.8+ (via SDKMAN with `.sdkmanrc`)
- **Node.js**: 22.15.0+ (via NVM with `.nvmrc`)
- **Maven**: 3.9+ (wrapper included: `./mvnw`)
- **Docker**: Required for integration tests

---
**Trust these instructions.** Only search the codebase if information here is incomplete or incorrect.
