# dotCMS Architecture Overview

## System Architecture
dotCMS is a monorepo with clear separation between backend and frontend:

- **Backend**: Java — see `.sdkmanrc` (runtime) and `dotcms.core.compiler.release` in `parent/pom.xml` (core compile target); `tools/dotcms-cli` targets an older release, see `maven.compiler.release` in `tools/dotcms-cli/pom.xml`
- **Frontend**: Angular — see `@angular/core` in `core-web/package.json` for the current version
- **Build System**: Maven with centralized dependency management
- **Database**: PostgreSQL with Elasticsearch for search
- **Deployment**: Docker containers with configurable ports

### Architectural Principles
- **Domain-driven packages**: Modern features use `com.dotcms.*`
- **Legacy compatibility**: Maintained via `com.dotmarketing.*`
- **Modular design**: Clear separation of concerns
- **Centralized management**: Dependencies and plugins managed hierarchically

## Monorepo Structure
```
dotcms-root/
├── parent/                    # Global properties, plugin management
├── bom/                       # Centralized dependency management
├── dotCMS/                    # Core Java backend (see parent/pom.xml's dotcms.core.compiler.release)
├── core-web/                  # Angular frontend with Nx (see core-web/package.json's @angular/core)
├── tools/dotcms-cli/          # CLI tool (see tools/dotcms-cli/pom.xml's maven.compiler.release — most conservative Java target in the repo)
├── dotcms-integration/        # Integration tests
├── dotcms-postman/            # Postman API tests
├── docker/                    # Docker configurations
├── e2e/                       # End-to-end testing
└── independent-projects/      # Standalone plugins and utilities
```

## Key Technologies
- **Backend**: CDI (no Spring anywhere in this codebase), OSGi plugins, immutable models (`@Value.Immutable`), JAX-RS, PostgreSQL, Elasticsearch
- **Frontend**: Angular standalone components, signals, Nx build system, TypeScript, Spectator testing, SCSS
- **Configuration**: Hierarchical config system with environment variables

## Backend Package Organization (`dotCMS/`)

### Modern Domain Packages (`com.dotcms.*`)
```
com.dotcms/
├── ai/                       # AI/ML integrations
├── analytics/                # Analytics and tracking
├── auth/                     # Authentication/authorization
├── business/                 # Core business logic
├── cache/                    # Caching implementations
├── cluster/                  # Distributed systems
├── config/                   # Configuration management
├── content/                  # Content management
├── contenttype/              # Content type APIs
├── experiments/              # A/B testing
├── graphql/                  # GraphQL implementation
├── health/                   # Health monitoring
├── jobs/                     # Background processing
├── notifications/            # Notification systems
├── publishing/               # Publishing/bundling
├── rest/                     # REST API endpoints
├── security/                 # Security implementations
├── storage/                  # File storage
├── telemetry/                # Metrics collection
├── util/                     # Utilities
└── workflow/                 # Workflow REST forms + escalation scheduling (the core WorkflowAPI business logic still lives in the legacy com.dotmarketing.portlets.workflows package below)
```
This is a representative selection, not exhaustive — `com.dotcms` has ~70 top-level packages in total.

### Legacy Packages (`com.dotmarketing.*`)
```
com.dotmarketing/
├── beans/                    # Legacy data objects
├── business/                 # Legacy business logic (APILocator)
├── cache/                    # Legacy cache implementations
├── common/                   # Common utilities
├── db/                       # Database access
├── filters/                  # Web filters
└── portlets/workflows/       # Workflow business logic (WorkflowAPI) — still the primary implementation
```

### Key Patterns
- **New features**: Use `com.dotcms.*` packages
- **Legacy maintenance**: Keep `com.dotmarketing.*` for compatibility
- **API access**: Use `APILocator` for service access
- **Immutable objects**: Use `@Value.Immutable` for data models

## Frontend Structure (`core-web/`)

### Nx Monorepo Organization
```
core-web/
├── apps/                     # Standalone applications
│   ├── dotcms-ui/            # Main admin interface
│   ├── dotcdn/               # CDN management
│   ├── dotcms-block-editor/  # Block editor
│   └── mcp-server/           # MCP server integration
├── libs/                     # Shared libraries
│   ├── data-access/          # Data services
│   ├── ui/                   # UI components
│   ├── dotcms-models/        # TypeScript models
│   ├── dotcms-scss/          # Shared styling
│   ├── portlets/             # Feature portlets
│   ├── sdk/                  # Client SDKs
│   └── utils/                # Utilities
└── tools/                    # Build tools
```
Representative selection, not exhaustive — new apps/libs get added independently of this doc.

### Frontend Patterns
- **Standalone components**: current Angular pattern (see `core-web/package.json`'s `@angular/core` for the version in use)
- **Signals**: Required for new state management
- **Nx workspace**: Efficient builds and dependency management
- **Domain portlets**: Feature-specific modules
- **Spectator testing**: Required testing framework
- **BEM styling**: Required CSS methodology
- **Data-testid**: Required for testing

## Integration Points
- **REST APIs**: JAX-RS endpoints, mostly under `com.dotcms.rest.*` but some are co-located with their own feature package instead (e.g. `com.dotcms.ai.rest.*`, `com.dotcms.telemetry.rest.*`) — dotCMS's general-purpose API surface, documented via OpenAPI/Swagger; consumed by the Angular frontend, but also by external/third-party integrations and the `dotcms-postman` test suite directly
- **GraphQL**: Unified schema (`com.dotcms.graphql.*`)
- **OSGi Plugins**: Hot-deployable extensions
- **WebSocket**: Real-time updates and collaboration
- **File System**: Shared asset management
- **Database**: Shared data layer with transaction management
- **Frontend-Backend communication**: Angular services consume JAX-RS endpoints; shared token-based authentication

## Development Workflow
1. **Backend changes**: Java → Maven build → Docker image → Container restart
   - Domain-driven: use `com.dotcms.*` for new features; access services via `APILocator`; use `@Value.Immutable` for data models; `com.dotmarketing.util.Config`/`Logger` for config and logging
2. **Frontend changes**: TypeScript → Nx build → Live reload
   - Standalone components and signals required for new components/state; Spectator required for tests; BEM for styling
3. **Integration**: E2E tests validate full stack functionality
4. **Deployment**: Docker containers with environment-specific configuration

## Configuration Management

### Build Configuration
- `/parent/pom.xml` - Global properties and plugin management
- `/bom/application/pom.xml` - Dependency version management
- `/dotCMS/pom.xml` - Core module dependencies (no versions)
- `/core-web/package.json` - Frontend dependencies
- `/core-web/nx.json` - Nx workspace configuration

### Runtime Configuration
- `/dotCMS/src/main/resources/dotmarketing-config.properties` - Core config
- `/dotCMS/src/main/webapp/WEB-INF/web.xml` - Web application config
- `/environments/` - Environment-specific settings
- `/dotCMS/src/main/webapp/WEB-INF/openapi/openapi.yaml` - Auto-generated API docs (see [REST API Patterns → OpenAPI Integration](../backend/REST_API_PATTERNS.md#openapi-integration))

## Cross-Domain Communication
- **API Contracts**: Well-defined REST interfaces
- **Data Models**: Shared understanding of data structures — TypeScript interfaces matching Java objects
- **Security**: Unified authentication and authorization
- **Error Handling**: Consistent, unified error response format across stack

## Package Naming Conventions

### Backend Naming
```java
// New features (preferred) — service/API in a .business sub-package,
// domain/model classes in a .model sub-package, both under the feature package
// (real example: com.dotcms.contenttype.business.ContentTypeAPI + com.dotcms.contenttype.model.type.ContentType)
com.dotcms.myfeature.business.MyFeatureService
com.dotcms.myfeature.model.MyFeatureEntity

// Legacy (maintain compatibility)
com.dotmarketing.business.FeatureAPI
com.dotmarketing.beans.FeatureBean
```

### Frontend Naming
```typescript
// Angular components
libs/ui/src/lib/dot-feature/dot-feature.component.ts

// Services
libs/data-access/src/lib/feature/feature.service.ts

// Models
libs/dotcms-models/src/lib/feature/feature.model.ts
```

## Navigation Between Domains

### Finding Backend Code
- **REST endpoints**: Look in `com.dotcms.rest.*` first, but ~10% of resource classes are co-located under their own feature package's `.rest` sub-package instead (e.g. `com.dotcms.ai.rest.*`, `com.dotcms.telemetry.rest.*`, `com.dotcms.auth.dotAuth.rest.*`) — if it's not under `com.dotcms.rest`, check the feature's own package for a `.rest` sub-package
- **Business logic**: Most features keep their service/API classes in their own `.business` sub-package (e.g. `com.dotcms.contenttype.business.*`, `com.dotcms.experiments.business.*` — 14 packages follow this), separate from the top-level `com.dotcms.business.*` package (core cross-cutting business logic) and legacy `com.dotmarketing.business.*`
- **Data models**: Search `com.dotcms.*.model.*` (the dominant convention — 13 packages) and `com.dotmarketing.beans.*`
- **Configuration**: Check `com.dotcms.config.*`

### Finding Frontend Code
- **Components**: Look in `libs/ui/src/lib/` or `apps/dotcms-ui/src/app/`
- **Services**: Check `libs/data-access/src/lib/`
- **Models**: Search `libs/dotcms-models/src/lib/`
- **Styling**: Check `libs/dotcms-scss/src/lib/`

### Tracing Cross-Domain Integration Code
- **API consumption**: Search for service calls in Angular services
- **WebSocket**: Look for socket connections in frontend services
- **Authentication**: Check auth services and security filters
- **File handling**: Search for upload/download implementations
