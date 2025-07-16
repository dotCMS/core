# dotCMS Architecture Overview

## System Architecture
dotCMS is a monorepo with clear separation between backend and frontend:

- **Backend**: Java 21 runtime with Java 11 bytecode compatibility
- **Frontend**: Angular 18.2.3 with Nx workspace
- **Build System**: Maven with centralized dependency management
- **Database**: PostgreSQL with Elasticsearch for search
- **Deployment**: Docker containers with configurable ports

## Monorepo Structure
```
├── dotCMS/                 # Core Java backend
├── core-web/               # Angular frontend with Nx
├── tools/dotcms-cli/       # CLI tool (full Java 21 features)
├── docker/                 # Docker configurations
├── e2e/                    # End-to-end testing
├── bom/                    # Bill of Materials for dependencies
└── parent/                 # Parent POM with plugin management
```

## Key Technologies
- **Backend**: Spring/CDI, OSGi plugins, immutable models (`@Value.Immutable`)
- **Frontend**: Angular standalone components, signals, Spectator testing
- **Database**: PostgreSQL with DotConnect abstraction
- **Search**: Elasticsearch integration
- **Configuration**: Hierarchical config system with environment variables

## Integration Points
- **REST APIs**: JAX-RS endpoints for frontend communication
- **WebSocket**: Real-time updates and collaboration
- **File System**: Shared asset management
- **Database**: Shared data layer with transaction management

## Development Workflow
1. **Backend changes**: Java → Maven build → Docker image → Container restart
2. **Frontend changes**: TypeScript → Nx build → Live reload
3. **Integration**: E2E tests validate full stack functionality
4. **Deployment**: Docker containers with environment-specific configuration

## Cross-Domain Communication
- **API Contracts**: Well-defined REST interfaces
- **Data Models**: Shared understanding of data structures
- **Security**: Unified authentication and authorization
- **Error Handling**: Consistent error responses across stack