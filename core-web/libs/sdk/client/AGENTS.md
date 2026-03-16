# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is the **DotCMS Client SDK** (`@dotcms/client`) - a JavaScript/TypeScript library for interacting with DotCMS REST APIs. The SDK is part of a larger DotCMS monorepo and serves as the foundation for framework-specific integrations (React, Angular, etc.).

## Essential Commands

### Development Commands
```bash
# Install dependencies (from monorepo root)
yarn install

# Build library for distribution
nx run sdk-client:build                  # ESM/CJS dual package build
nx run sdk-client:build:js               # Specialized esbuild for editor integration

# Run tests
nx run sdk-client:test                   # Run Jest tests
nx run sdk-client:test:ci                # Run tests with CI configuration and coverage

# Code quality
nx run sdk-client:lint                   # ESLint code linting

# Publishing
nx run sdk-client:publish --args.ver=1.0.0 --args.tag=latest
```

### Testing Commands
```bash
# Run specific test file
nx test sdk-client --testNamePattern="specific test name"

# Run tests in watch mode
nx test sdk-client --watch

# Run tests with coverage
nx test sdk-client --coverage
```

## Architecture Overview

### Project Structure
```
libs/sdk/client/
├── src/
│   ├── index.ts                    # Main export (createDotCMSClient)
│   ├── internal.ts                 # Internal utilities for other SDKs
│   └── lib/
│       ├── client/
│       │   ├── client.ts          # Main DotCMSClient class
│       │   ├── content/           # Content API with query builders
│       │   ├── navigation/        # Navigation API
│       │   ├── page/             # Page API with GraphQL support
│       │   └── models/           # TypeScript interfaces and types
│       └── utils/
│           └── graphql/          # GraphQL utilities
├── project.json                   # Nx project configuration
├── package.json                   # NPM package configuration
├── tsconfig.json                  # TypeScript configuration
└── jest.config.ts                 # Jest testing configuration
```

### Key Components

**Core API Pattern**: The SDK follows a client-builder pattern with three main APIs:
- `client.page.get()` - Fetches complete page content with layout and containers
- `client.content.getCollection()` - Builder pattern for querying content collections
- `client.navigation.get()` - Fetches site navigation structure

**Build Targets**:
- `build` - Standard library build producing ESM/CJS dual packages
- `build:js` - Specialized esbuild for editor integration (outputs to `dotCMS/src/main/webapp/html/js/editor-js`)

## Development Standards

### TypeScript Configuration
- **Strict mode enabled**: Full TypeScript strict checking
- **Target**: ES2020 with lib support for ES2020, DOM, DOM.Iterable
- **Dual package support**: ESM and CJS through Rollup build
- **Type definitions**: `@dotcms/types` for comprehensive type safety

### Code Patterns

#### Client Builder Pattern
```typescript
// Main client initialization
const client = createDotCMSClient({
    dotcmsUrl: 'https://instance.com',
    authToken: 'token',
    siteId: 'site-id'
});

// Page API - single request for complete page
const { pageAsset } = await client.page.get('/about-us');

// Content API - fluent builder pattern
const blogs = await client.content
    .getCollection('Blog')
    .query((qb) => qb.field('title').equals('dotCMS*'))
    .limit(10)
    .sortBy([{ field: 'publishDate', direction: 'desc' }]);
```

#### GraphQL Integration
```typescript
// Extend page requests with GraphQL for additional content
const { pageAsset, content } = await client.page.get('/about-us', {
    graphql: {
        page: `title vanityUrl { url }`,
        content: {
            blogs: `BlogCollection(limit: 3) { title urlTitle }`,
            navigation: `DotNavigation(uri: "/", depth: 2) { href title }`
        }
    }
});
```

#### Query Builder Pattern
```typescript
// Fluent query building for content collections
const query = await client.content
    .getCollection('Product')
    .query((qb) => qb
        .field('category').equals('electronics')
        .and()
        .field('price').raw(':[100 TO 500]')
        .not()
        .field('discontinued').equals('true')
    )
    .limit(10)
    .page(1);
```

### Testing Standards

#### Jest Configuration
- **Framework**: Jest with TypeScript support
- **Coverage**: Outputs to `../../../coverage/libs/sdk/client`
- **File patterns**: `**/*.spec.ts` and `**/*.test.ts`
- **CI mode**: Supports CI configuration with coverage reporting

#### Test Patterns
```typescript
// Use descriptive test names and group related tests
describe('DotCMSClient', () => {
    describe('page.get()', () => {
        it('should fetch page content with default options', async () => {
            // Test implementation
        });
        
        it('should apply GraphQL extensions correctly', async () => {
            // Test implementation
        });
    });
});

// Mock external dependencies
jest.mock('../utils/fetch', () => ({
    dotFetch: jest.fn()
}));
```

### Build System Integration

#### Nx Monorepo Integration
- **Executor**: `@nx/rollup:rollup` for main build, `@nx/esbuild:esbuild` for editor build
- **Outputs**: Dual package (ESM/CJS) with proper export maps
- **Dependencies**: Automatically managed through Nx dependency graph

#### Export Configuration
```typescript
// Package exports support both named and default imports
export { createDotCMSClient } from './lib/client/client';
export type { DotCMSClient } from './lib/client/client';

// Internal exports for framework SDKs
export { DotCMSClientImpl } from './lib/client/client-impl';
```

## Key Configuration Files

- **project.json**: Nx project configuration with build targets
- **package.json**: NPM package metadata with proper exports field
- **tsconfig.json**: TypeScript configuration with strict mode
- **tsconfig.lib.json**: Library-specific TypeScript settings
- **jest.config.ts**: Jest testing configuration

## Development Workflow

### Standard Development Flow
1. **Make changes** → Test locally with `nx test sdk-client`
2. **Lint code** → `nx lint sdk-client`
3. **Build library** → `nx build sdk-client`
4. **Integration testing** → Test with dependent SDKs or examples

### Editor Integration Build
For changes affecting the editor integration:
1. **Build editor version** → `nx run sdk-client:build:js`
2. **Test in dotCMS editor** → Verify functionality in dotCMS admin
3. **Clean build artifacts** → Script automatically removes temporary files

### Release Process
1. **Version bump** → Update version in `package.json`
2. **Build all targets** → `nx build sdk-client`
3. **Publish** → `nx run sdk-client:publish --args.ver=X.X.X --args.tag=latest`

## Integration Context

### Framework SDKs
This client SDK serves as the foundation for:
- `@dotcms/react` - React integration with UVE support
- `@dotcms/angular` - Angular integration with UVE support
- `@dotcms/uve` - Universal Visual Editor low-level integration

### Dependencies
- **Runtime**: `consola` for logging
- **Development**: `@dotcms/types` for TypeScript definitions
- **Peer Dependencies**: Framework-specific SDKs extend this client

## Common Development Tasks

### Adding New API Methods
1. **Define interfaces** in `lib/client/models/`
2. **Implement method** in appropriate API class (`page/`, `content/`, `navigation/`)
3. **Export** from main `index.ts`
4. **Add tests** following existing patterns
5. **Update TypeScript types** if needed

### Extending Query Builders
1. **Add method** to appropriate builder class
2. **Update builder interface** with new method signature
3. **Add tests** for new query capabilities
4. **Document** in README with examples

### GraphQL Integration
1. **Define GraphQL fragments** in `utils/graphql/`
2. **Add to page API** GraphQL parameter types
3. **Test** with various GraphQL queries
4. **Update documentation** with new capabilities

## Summary Checklist
- ✅ Use Nx commands for all build/test operations
- ✅ Follow client-builder pattern for new APIs
- ✅ Maintain TypeScript strict mode compliance
- ✅ Write comprehensive Jest tests for new features
- ✅ Use proper export patterns for dual package support
- ✅ Test both ESM and CJS builds
- ✅ Verify editor integration when making core changes
- ❌ Avoid breaking changes to public API without migration plan
- ❌ Don't add runtime dependencies without careful consideration