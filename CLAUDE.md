# dotCMS Development Guide

## 🎯 Quick Start Context

### Build Optimization (Choose Right Command)
```bash
# For test-only changes (fastest!):
./mvnw verify -pl :dotcms-integration          # OR: just test-integration (~30 sec)
./mvnw verify -pl :dotcms-postman              # OR: just test-postman

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
# For test debugging: Start services, then use IDE
just test-integration-ide          # Starts PostgreSQL + Elasticsearch + dotCMS
# → Run/debug individual tests in IDE with breakpoints
just test-integration-stop         # Clean up when done
```

```typescript
// Angular (REQUIRED modern syntax)
@if (condition()) { <content /> }      // NOT *ngIf
data = input<string>();                // NOT @Input()
spectator.setInput('prop', value);     // Testing CRITICAL
```

```bash
# Test Commands (fastest - no core rebuild needed!)
just test-integration                                  # Auto-starts DB/ES + runs tests (~30 sec - 2 min)
just test-postman ai                                  # Specific Postman collection 
./mvnw verify -pl :dotcms-integration -Dit.test=MyTest # Specific integration test

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