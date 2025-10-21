# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository Overview

This is the **DotCMS Core-Web** monorepo - the frontend infrastructure for the DotCMS content management system. Built with **Nx workspace** architecture, it contains Angular applications, TypeScript SDKs, shared libraries, and web components.

## Key Development Commands

### Development Server
```bash
# Start main admin UI with backend proxy
nx serve dotcms-ui

# Start block editor development
nx serve dotcms-block-editor

# Start with specific configuration
nx serve dotcms-ui --configuration=development
```

### Building
```bash
# Build main application
nx build dotcms-ui

# Build specific SDK for publishing
nx build sdk-client
nx build sdk-react
nx build sdk-analytics

# Build all affected projects
nx affected:build
```

### Testing
```bash
# Run all tests
yarn run test:dotcms

# Run specific project tests
nx test dotcms-ui
nx test sdk-client
nx test block-editor

# Run E2E tests
nx e2e dotcms-ui-e2e

# Run single test file
nx test dotcms-ui --testPathPattern=dot-edit-content

# Test with coverage
nx test dotcms-ui --coverage
```

### Code Quality
```bash
# Lint all projects
yarn run lint:dotcms

# Lint specific project
nx lint dotcms-ui

# Fix linting issues
nx lint dotcms-ui --fix

# Check affected projects
nx affected:test
nx affected:lint
```

### Monorepo Management
```bash
# Visualize project dependencies
nx dep-graph

# Show project information
nx show project dotcms-ui

# Run tasks in parallel
nx run-many --target=test --projects=sdk-client,sdk-react
```

## Architecture & Structure

### Monorepo Organization
- **apps/** - Main applications (dotcms-ui, dotcms-block-editor, dotcms-binary-field-builder, mcp-server)
- **libs/sdk/** - External-facing SDKs (client, react, angular, analytics, experiments, uve)
- **libs/data-access/** - Angular services for API communication
- **libs/ui/** - Shared UI components and patterns
- **libs/portlets/** - Feature-specific portlets (analytics, experiments, locales, etc.)
- **libs/dotcms-models/** - TypeScript interfaces and types
- **libs/block-editor/** - TipTap-based rich text editor
- **libs/template-builder/** - Template construction utilities

### Technology Stack
- **Angular 19.2.9** with standalone components
- **Nx 20.5.1** for monorepo management
- **PrimeNG 17.18.11** UI components
- **TipTap 2.14.0** for rich text editing
- **NgRx 19.2.1** for state management
- **Jest 29.7.0** for testing
- **Playwright** for E2E testing
- **Node.js >=v22.15.0** requirement

### Component Conventions
- **Prefix**: All Angular components use `dot-` prefix
- **Naming**: Follow Angular style guide with kebab-case
- **Architecture**: Feature modules with lazy loading
- **State**: Component-store pattern with NgRx signals
- **Testing**: Jest unit tests + Playwright E2E

### Backend Integration
- **Development Proxy**: `proxy-dev.conf.mjs` routes `/api/*` to port 8080
- **API Services**: Centralized in `libs/data-access`
- **Authentication**: Bearer token-based with `DotcmsConfigService`
- **Content Management**: Full CRUD through `DotHttpService`

## Development Workflows

### Local Development Setup
1. Ensure Node.js >=v22.15.0
2. Run `yarn install` to install dependencies
3. Run `node prepare.js` to set up Husky git hooks
4. Start backend dotCMS on port 8080
5. Run `nx serve dotcms-ui` for frontend development

### Adding New Features
1. Create feature branch following naming convention
2. Add libraries in `libs/` for reusable code
3. Use existing patterns from similar features
4. Follow component prefix conventions (`dot-`)
5. Add comprehensive tests (Jest + Playwright if needed)
6. Update TypeScript paths in `tsconfig.base.json` if adding new libraries

### SDK Development
- **Client SDK**: Core API client in `libs/sdk/client`
- **React SDK**: React components in `libs/sdk/react`
- **Angular SDK**: Angular services in `libs/sdk/angular`
- **Publishing**: Automated via npm with proper versioning

### Testing Strategy
- **Unit Tests**: Jest with comprehensive mocking utilities
- **E2E Tests**: Playwright for critical user workflows
- **Coverage**: Reports generated to `../../../target/core-web-reports/`
- **Mock Data**: Extensive mock utilities in `libs/utils-testing`

### Build Targets & Configurations
- **Development**: Proxy configuration with source maps
- **Production**: Optimized builds with tree shaking
- **Library**: Rollup/Vite builds for SDK packages
- **Web Components**: Stencil.js compilation for `dotcms-webcomponents`

## Important Notes

### TypeScript Configuration
- **Strict Mode**: Enabled across all projects
- **Path Mapping**: Extensive use of `@dotcms/*` barrel exports
- **Types**: Centralized in `libs/dotcms-models` and `libs/sdk/types`

### State Management
- **NgRx**: Component stores with signals pattern
- **Global Store**: Centralized state in `libs/global-store`
- **Services**: Angular services for data access and business logic

### Web Components
- **Stencil.js**: Framework-agnostic components in `libs/dotcms-webcomponents`
- **Legacy**: `libs/dotcms-field-elements` (deprecated, use Stencil components)
- **Integration**: Used across Angular, React, and vanilla JS contexts

### Performance Considerations
- **Lazy Loading**: Feature modules loaded on demand
- **Tree Shaking**: Proper barrel exports for optimal bundles
- **Caching**: Nx task caching for faster builds
- **Affected**: Only build/test changed projects in CI

## Debugging & Troubleshooting

### Common Issues
- **Proxy Errors**: Ensure backend is running on port 8080
- **Build Failures**: Check TypeScript paths and circular dependencies
- **Test Failures**: Verify mock data and async handling
- **Linting**: Follow component naming conventions with `dot-` prefix

### Development Tools
- **Nx Console**: VS Code extension for Nx commands
- **Angular DevTools**: Browser extension for debugging
- **Coverage Reports**: Check `target/core-web-reports/` for test coverage
- **Dependency Graph**: Use `nx dep-graph` to visualize project relationships

This codebase emphasizes consistency, testability, and maintainability through its monorepo architecture and established patterns.