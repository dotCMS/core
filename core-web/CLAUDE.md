# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository Overview

This is the **DotCMS Core-Web** monorepo (v23.4.0-next.1) - the frontend infrastructure for the DotCMS content management system. Built with **Nx workspace** architecture, it contains Angular applications, TypeScript SDKs, shared libraries, and web components.

**Package Manager**: Uses Yarn with npm resolutions/overrides for dependency management

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
# Lint all projects (with auto-fix, excludes skip:lint tagged projects)
yarn run lint:dotcms

# Lint specific project
nx lint dotcms-ui

# Fix linting issues
nx lint dotcms-ui --fix

# Check affected projects
nx affected:test
nx affected:lint

# Format code with Prettier 3.7.4
yarn run format
yarn run format:check
```

**Linting Stack:**
-   **ESLint 8.57.0** with TypeScript 8.38.0 parser
-   **@angular-eslint 21.0.0** for Angular-specific rules
-   **@stylistic/eslint-plugin 5.2.2** for code style
-   **eslint-plugin-react 7.34.2** for React projects
-   **eslint-config-prettier 10.1.8** for Prettier integration

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

-   **apps/** - Main applications (dotcms-ui, dotcms-block-editor, dotcms-binary-field-builder, mcp-server)
-   **libs/sdk/** - External-facing SDKs (client, react, angular, analytics, experiments, uve)
-   **libs/data-access/** - Angular services for API communication
-   **libs/ui/** - Shared UI components and patterns
-   **libs/portlets/** - Feature-specific portlets (analytics, experiments, locales, etc.)
-   **libs/dotcms-models/** - TypeScript interfaces and types
-   **libs/block-editor/** - TipTap-based rich text editor (using ngx-tiptap 12.0.0)
-   **libs/template-builder/** - Template construction utilities
-   **libs/dotcms-webcomponents/** - Stencil.js 4.39.0 web components

### Technology Stack

-   **Angular 21.0.2** with standalone components
-   **Nx 21.6.9** for monorepo management
-   **PrimeNG 21.0.2** UI components
-   **TipTap 2.14.0** for rich text editing
-   **NgRx 19.2.1** for state management
-   **Jest 29.7.0** for testing
-   **Playwright 1.36.0** for E2E testing
-   **Node.js >=v22.15.0** requirement
-   **Storybook 9.1.9** for component documentation

### Component Conventions

-   **Prefix**: All Angular components use `dot-` prefix
-   **Naming**: Follow Angular style guide with kebab-case
-   **Architecture**: Feature modules with lazy loading
-   **State**: Component-store pattern with NgRx signals
-   **Testing**: Jest unit tests + Playwright E2E
-   **Styling**: Tailwind CSS 4.1.17 with PrimeFlex 3.3.1 utilities
-   **Icons**: PrimeIcons 7.0.0 and Font Awesome 4.7.0

### Modern Angular Syntax (REQUIRED)

```typescript
// ✅ CORRECT: Modern control flow syntax
@if (condition()) { <content /> }      // NOT *ngIf
@for (item of items(); track item.id) { }  // NOT *ngFor

// ✅ CORRECT: Modern input/output syntax
data = input<string>();                // NOT @Input()
onChange = output<string>();           // NOT @Output()

// ✅ CRITICAL: Testing with Spectator
spectator.setInput('prop', value);     // ALWAYS use setInput for inputs
spectator.detectChanges();             // Trigger change detection

// ✅ CORRECT: Use data-testid for selectors
<button data-testid="submit-button">Submit</button>
const button = spectator.query('[data-testid="submit-button"]');
```

### Backend Integration

-   **Development Proxy**: `proxy-dev.conf.mjs` routes `/api/*` to port 8080
-   **API Services**: Centralized in `libs/data-access`
-   **Authentication**: Bearer token-based with `DotcmsConfigService`
-   **Content Management**: Full CRUD through `DotHttpService`
-   **HTTP Client**: Cross-fetch 3.1.4 for universal fetch API support

### Additional Key Libraries

-   **Rich Text Editing**: TinyMCE 6.8.3 with Angular/React wrappers, Marked 12.0.2 for markdown
-   **Date Handling**: date-fns 4.0.0 with @date-fns/tz 1.4.0 for timezone support
-   **Drag & Drop**: dragula 3.7.3 with ng2-dragula 5.0.1, dom-autoscroller 2.3.4
-   **Charts**: chart.js 4.3.0 for data visualization
-   **Layout**: gridstack 8.1.1 for grid-based layouts
-   **Utilities**: uuid 9.0.0, md5 2.3.0, turndown 7.2.0 (HTML to Markdown)
-   **Validation**: zod 4.1.9, superstruct 1.0.3 for runtime type checking
-   **Analytics**: @jitsu/sdk-js 3.1.5, analytics 0.8.14
-   **Model Context Protocol**: @modelcontextprotocol/sdk 1.13.1

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

-   **Client SDK**: Core API client in `libs/sdk/client`
-   **React SDK**: React 18.3.1 components in `libs/sdk/react`
-   **Angular SDK**: Angular services in `libs/sdk/angular`
-   **Next.js Support**: Next.js 14.0.4 integration patterns
-   **Publishing**: Automated via npm with proper versioning
-   **Build Tools**: Vite 7.2.7, Rollup 4.14.0, esbuild 0.19.2 for optimized library builds

### Testing Strategy

-   **Unit Tests**: Jest 29.7.0 with jest-preset-angular 14.6.2 and @ngneat/spectator 19.6.2
-   **E2E Tests**: Playwright 1.36.0 for critical user workflows
-   **Test Environment**: @happy-dom/jest-environment 15.7.4 (modern, fast alternative to jsdom)
-   **Coverage**: Reports generated to `../../../target/core-web-reports/` using jest-html-reporters
-   **Mock Data**: @faker-js/faker 8.4.1 for realistic test data, extensive mock utilities in `libs/utils-testing`
-   **Component Testing**: ng-mocks 14.12.1 for Angular component mocking

### Build Targets & Configurations

-   **Development**: Proxy configuration with source maps (webpack-dev-middleware 6.1.2)
-   **Production**: Optimized builds with tree shaking (Terser 5.28.1 minification)
-   **Library**: Rollup 4.14.0 / Vite 7.2.7 / esbuild 0.19.2 builds for SDK packages
-   **Web Components**: Stencil.js 4.39.0 compilation for `dotcms-webcomponents`
-   **Angular Builder**: @angular-devkit/build-angular 21.0.2
-   **Bundle Analysis**: webpack-bundle-analyzer 4.5.0 for size optimization

## Important Notes

### TypeScript Configuration

-   **TypeScript 5.9.3**: Latest stable version with strict mode enabled
-   **Strict Mode**: Enabled across all projects
-   **Path Mapping**: Extensive use of `@dotcms/*` barrel exports
-   **Types**: Centralized in `libs/dotcms-models` and `libs/sdk/types`
-   **Build Tools**: ng-packagr 19.2.2 for library builds

### State Management

-   **NgRx 19.2.1**: Component stores with signals pattern
-   **@ngrx/component-store**: For local component state management
-   **@ngrx/signals**: Modern reactive state with signals
-   **@ngrx/operators**: Reactive operators for state transformations
-   **Global Store**: Centralized state in `libs/global-store`
-   **Services**: Angular services for data access and business logic

### Web Components

-   **Stencil.js 4.39.0**: Framework-agnostic components in `libs/dotcms-webcomponents`
-   **@nxext/stencil 21.0.0**: Nx integration for Stencil projects
-   **@stencil/sass 3.2.3**: SASS support for component styling
-   **Integration**: Used across Angular, React, and vanilla JS contexts
-   **Material Web Components**: @material/mwc-* components (v0.20.0) for specific UI needs

### Performance Considerations

-   **Lazy Loading**: Feature modules loaded on demand
-   **Tree Shaking**: Proper barrel exports for optimal bundles
-   **Caching**: Nx task caching for faster builds
-   **Affected**: Only build/test changed projects in CI

## Debugging & Troubleshooting

### Common Issues

-   **Proxy Errors**: Ensure backend is running on port 8080
-   **Build Failures**: Check TypeScript paths and circular dependencies
-   **Test Failures**: Verify mock data and async handling
-   **Linting**: Follow component naming conventions with `dot-` prefix

### Development Tools

-   **Nx Console**: VS Code extension for Nx commands
-   **Angular DevTools**: Browser extension for debugging
-   **Coverage Reports**: Check `target/core-web-reports/` for test coverage
-   **Dependency Graph**: Use `nx dep-graph` to visualize project relationships

This codebase emphasizes consistency, testability, and maintainability through its monorepo architecture and established patterns.

## Summary Checklist

### Angular/TypeScript Development

-   ✅ Use modern control flow: `@if`, `@for` (NOT `*ngIf`, `*ngFor`)
-   ✅ Use modern inputs/outputs: `input<T>()`, `output<T>()` (NOT `@Input()`, `@Output()`)
-   ✅ Use `data-testid` attributes for all testable elements
-   ✅ Use `spectator.setInput()` for testing component inputs
-   ✅ Follow `dot-` prefix convention for all components
-   ✅ Use standalone components with lazy loading
-   ✅ Use NgRx signals for state management
-   ❌ Avoid legacy Angular syntax (`*ngIf`, `@Input()`, etc.)
-   ❌ Avoid direct DOM queries without `data-testid`
-   ❌ Never skip unit tests for new components

### For Backend/Java Development

-   See **[../CLAUDE.md](../CLAUDE.md)** for Java, Maven, REST API, and Git workflow standards

<!-- nx configuration start-->
<!-- Leave the start & end comments to automatically receive updates. -->

# General Guidelines for working with Nx

-   When running tasks (for example build, lint, test, e2e, etc.), always prefer running the task through `nx` (i.e. `nx run`, `nx run-many`, `nx affected`) instead of using the underlying tooling directly
-   You have access to the Nx MCP server and its tools, use them to help the user
-   When answering questions about the repository, use the `nx_workspace` tool first to gain an understanding of the workspace architecture where applicable.
-   When working in individual projects, use the `nx_project_details` mcp tool to analyze and understand the specific project structure and dependencies
-   For questions around nx configuration, best practices or if you're unsure, use the `nx_docs` tool to get relevant, up-to-date docs. Always use this instead of assuming things about nx configuration
-   If the user needs help with an Nx configuration or project graph error, use the `nx_workspace` tool to get any errors

<!-- nx configuration end-->
