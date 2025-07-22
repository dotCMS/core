# dotCMS Development Guide

## 🎯 Quick Start Context

### Essential Patterns
```java
// Java (ALWAYS use these)
import com.dotmarketing.util.Config;   // Config.getStringProperty("key", "default")
import com.dotmarketing.util.Logger;   // Logger.info(this, "message")
UserAPI userAPI = APILocator.getUserAPI();
```

```typescript
// Angular (REQUIRED modern syntax)
@if (condition()) { <content /> }      // NOT *ngIf
data = input<string>();                // NOT @Input()
spectator.setInput('prop', value);     // Testing CRITICAL
```

```bash
# Build Commands
./mvnw clean install -DskipTests                      # Full clean build
./mvnw install -pl :dotcms-core -DskipTests           # Fast build
./mvnw -pl :dotcms-core -Pdocker-start -Dtomcat.port=8080  # Docker dev
cd core-web && nx run dotcms-ui:serve                 # Frontend dev
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
- [Docker Build Process](docs/infrastructure/DOCKER_BUILD_PROCESS.md) - Container setup
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