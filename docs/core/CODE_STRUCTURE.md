# dotCMS Code Structure

## Monorepo Architecture

### Root Module Organization
```
dotcms-root/
├── parent/                    # Global properties, plugin management
├── bom/                      # Centralized dependency management
├── dotCMS/                   # Core Java backend (Java 21 runtime, Java 11 compatibility)
├── core-web/                 # Angular 18.2.3 frontend with Nx
├── tools/dotcms-cli/         # CLI tools (full Java 21 features)
├── dotcms-integration/       # Integration tests
├── dotcms-postman/          # API testing
├── e2e/                     # End-to-end testing
└── independent-projects/     # Standalone plugins and utilities
```

### Architectural Principles
- **Domain-driven packages**: Modern features use `com.dotcms.*`
- **Legacy compatibility**: Maintained via `com.dotmarketing.*`
- **Modular design**: Clear separation of concerns
- **Centralized management**: Dependencies and plugins managed hierarchically

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
├── telemetry/               # Metrics collection
├── util/                     # Utilities
└── workflow/                 # Workflow management
```

### Legacy Packages (`com.dotmarketing.*`)
```
com.dotmarketing/
├── beans/                    # Legacy data objects
├── business/                 # Legacy business logic (APILocator)
├── cache/                    # Legacy cache implementations
├── common/                   # Common utilities
├── db/                       # Database access
└── filters/                  # Web filters
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
│   ├── dotcms-ui/           # Main admin interface
│   ├── dotcdn/              # CDN management
│   ├── dotcms-block-editor/ # Block editor
│   └── mcp-server/          # MCP server integration
├── libs/                     # Shared libraries
│   ├── data-access/         # Data services
│   ├── ui/                  # UI components
│   ├── dotcms-models/       # TypeScript models
│   ├── dotcms-scss/         # Shared styling
│   ├── portlets/            # Feature portlets
│   ├── sdk/                 # Client SDKs
│   └── utils/               # Utilities
└── tools/                    # Build tools
```

### Frontend Patterns
- **Standalone components**: Angular 18+ pattern
- **Signals**: Required for new state management
- **Nx workspace**: Efficient builds and dependency management
- **Domain portlets**: Feature-specific modules

## Integration Architecture

### Build System Integration
```
parent/pom.xml              # Global properties, plugin management
├── bom/application/pom.xml # Dependency versions
└── dotCMS/pom.xml         # Module dependencies (no versions)
```

### Runtime Integration Points
- **REST APIs**: JAX-RS endpoints (`com.dotcms.rest.*`)
- **GraphQL**: Unified schema (`com.dotcms.graphql.*`)
- **OSGi Plugins**: Hot-deployable extensions
- **WebSocket**: Real-time communication
- **CDI/Spring**: Dependency injection bridge

### Frontend-Backend Communication
- **REST consumption**: Angular services → JAX-RS endpoints
- **Authentication**: Shared token system
- **WebSocket**: Real-time updates
- **Asset handling**: File uploads and management

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
- `/dotCMS/src/main/webapp/WEB-INF/openapi/openapi.yaml` - Auto-generated API docs

## Module Dependencies

### Backend Dependencies
- **Spring/CDI**: Dependency injection
- **OSGi**: Plugin system
- **JAX-RS**: REST API framework
- **PostgreSQL**: Database
- **Elasticsearch**: Search engine
- **Docker**: Containerization

### Frontend Dependencies
- **Angular 18.2.3**: Framework
- **Nx**: Build system
- **TypeScript**: Language
- **Spectator**: Testing framework
- **SCSS**: Styling

## Development Patterns

### Backend Development
- **Domain-driven**: Use `com.dotcms.*` for new features
- **API Locator**: Access services via `APILocator`
- **Immutable models**: Use `@Value.Immutable`
- **Config management**: Use `com.dotmarketing.util.Config`
- **Logging**: Use `com.dotmarketing.util.Logger`

### Frontend Development
- **Standalone components**: Required for new components
- **Signals**: Required for state management
- **Spectator testing**: Required testing framework
- **BEM styling**: Required CSS methodology
- **Data-testid**: Required for testing

### Cross-Domain Patterns
- **API contracts**: Well-defined REST interfaces
- **Shared models**: TypeScript interfaces matching Java objects
- **Security**: Consistent authentication/authorization
- **Error handling**: Unified error response format

## Package Naming Conventions

### Backend Naming
```java
// New features (preferred)
com.dotcms.domain.feature.FeatureService
com.dotcms.domain.feature.model.FeatureEntity

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
- **REST endpoints**: Look in `com.dotcms.rest.*`
- **Business logic**: Check `com.dotcms.business.*` then `com.dotmarketing.business.*`
- **Data models**: Search `com.dotcms.*.model.*` and `com.dotmarketing.beans.*`
- **Configuration**: Check `com.dotcms.config.*`

### Finding Frontend Code
- **Components**: Look in `libs/ui/src/lib/` or `apps/dotcms-ui/src/app/`
- **Services**: Check `libs/data-access/src/lib/`
- **Models**: Search `libs/dotcms-models/src/lib/`
- **Styling**: Check `libs/dotcms-scss/src/lib/`

### Integration Points
- **API consumption**: Search for service calls in Angular services
- **WebSocket**: Look for socket connections in frontend services
- **Authentication**: Check auth services and security filters
- **File handling**: Search for upload/download implementations