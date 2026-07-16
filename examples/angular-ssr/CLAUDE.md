# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an Angular SSR (Server-Side Rendering) example project that demonstrates integration with dotCMS as a headless CMS. The project showcases:

- Angular 20.3.0 with server-side rendering capabilities
- dotCMS SDK integration (@dotcms/angular, @dotcms/client, @dotcms/types, @dotcms/uve)
- Tailwind CSS 4.x for styling
- Component-based architecture with dotCMS content rendering

## Development Commands

```bash
# Development server
npm start                    # Start dev server on http://localhost:4200
ng serve                     # Alternative command

# Building
npm run build                # Production build with SSR
npm run watch                # Development build with watch mode

# Testing
npm test                     # Run unit tests with Karma
ng test                      # Alternative command

# SSR Server (after building)
npm run serve:ssr:angular-ssr  # Serve the built SSR application
```

## Architecture

### Core Components Structure
```
src/app/
├── components/              # Shared Angular components
├── dotcms/                 # dotCMS-specific integration
│   ├── components/         # dotCMS content rendering components
│   │   ├── activity/       # Activity component
│   │   ├── category-filter/  # Category filter component
│   │   └── vtl-include/    # VTL include with variations
│   │       └── components/
│   │           └── destination-listing/  # Destination listing component
│   └── types/              # TypeScript models for dotCMS content
│       └── contentlet.model.ts  # Content type definitions
├── app.config.ts           # Application configuration with dotCMS setup
├── app.routes.ts           # Client-side routes
└── app.routes.server.ts    # Server-side routes for SSR
```

### dotCMS Integration Patterns

**Configuration (app.config.ts)**
- dotCMS client is configured with local instance (localhost:8080)
- Uses JWT authentication token for API access
- Custom HTTP client implementation (`AngularHttpClient`)
- Image loader provider for dotCMS assets

**Content Rendering Pattern**
- `VtlIncludeComponent` handles dynamic content type rendering using `@switch`
- Uses `input.required<T>()` for component inputs (modern Angular syntax)
- Implements `DotCMSShowWhenDirective` for UVE (Universal Visual Editor) mode detection
- Type-safe content models with `VTLIncludeWithVariations`

**Modern Angular Patterns**
```typescript
// Use input() instead of @Input()
contentlet = input.required<VTLIncludeWithVariations>();

// Use @switch/@case instead of *ngSwitchCase
@switch (contentlet().componentType) {
  @case ('destinationListing') {
    <app-destination-listing />
  }
}

// Use standalone components with imports array
@Component({
  imports: [DotCMSShowWhenDirective, DestinationListingComponent],
  // ...
})
```

## Development Guidelines

### dotCMS SDK Dependencies
The project uses local file dependencies for dotCMS SDKs:
```json
"@dotcms/angular": "file:../../core-web/dist/libs/sdk/angular/dotcms-angular-1.0.1.tgz"
"@dotcms/client": "file:../../core-web/dist/libs/sdk/client/dotcms-client-1.1.0.tgz"
"@dotcms/types": "file:../../core-web/dist/libs/sdk/types/dotcms-types-1.1.0.tgz"
"@dotcms/uve": "file:../../core-web/dist/libs/sdk/uve/dotcms-uve-1.0.1.tgz"
```

These are built from the dotCMS core project. If SDK changes are needed, they must be built in the parent `core-web` directory first.

### Styling with Tailwind CSS
- Uses Tailwind CSS v4.x with PostCSS integration
- Main stylesheet: `src/styles.css` imports `@import "tailwindcss"`
- PostCSS configuration: `.postcssrc.json` with `@tailwindcss/postcss` plugin

### Testing
- Karma + Jasmine setup for unit testing
- Only 2 test files exist currently (minimal test coverage)
- Test files use `.spec.ts` extension

### TypeScript Configuration
- `tsconfig.json` - Base TypeScript configuration
- `tsconfig.app.json` - Application-specific config
- `tsconfig.spec.json` - Test-specific config

## Angular SSR Configuration

The project is configured for server-side rendering:
- **Entry points**: `src/main.ts` (browser), `src/main.server.ts` (server)
- **SSR server**: `src/server.ts`
- **Output mode**: Server rendering enabled
- **Hydration**: Client hydration with event replay and HTTP transfer cache

## Prerequisites

- Node.js and npm
- dotCMS instance running on localhost:8080 (for API integration)
- Angular CLI for scaffolding new components