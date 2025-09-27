# Angular SSR with dotCMS Integration

This Angular project demonstrates how to implement editable dotCMS pages using Angular Server Side Rendering (SSR). It showcases best practices for integrating dotCMS content management with Angular's hybrid rendering capabilities.

### Content Management Features
- **Dynamic Page Rendering**: Automatic page generation from dotCMS routing
- **Content Type Components**: 10+ pre-built components for common content types
- **Block Editor Integration**: Rich text content with dotCMS Block Editor
- **Image Management**: Optimized image handling with dotCMS image API
- **GraphQL Queries**: Advanced content filtering and search capabilities

### Visual Editing (UVE)
- **Live Content Editing**: Edit content directly in the browser
- **Visual Page Builder**: In-context editing experience
- **Contentlet Editing**: Direct editing of individual content pieces

### Technical Integration
- **Server-Side Rendering**: Full SSR with hydration support
- **Type Safety**: Complete TypeScript interfaces for all content types
- **Performance Optimization**: HTTP transfer cache and image optimization

For the official Angular SSR guide, visit: [Angular SSR Documentation](https://angular.dev/guide/ssr)

## Getting Started

### Setup

```bash
git clone <repository-url>
cd angular-ssr
npm install
```

### Development Server

```bash
ng serve
```

Navigate to `http://localhost:4200/`. The application will automatically reload when source files are modified.

### Building for Production

```bash
ng build
```

Build artifacts are stored in the `dist/` directory with performance optimizations.

### Running Tests

```bash
ng test
ng e2e
```

## Architecture Overview

### Routing Strategy

The application uses a strategic combination of catch-all and specific routing in `app.routes.ts`:

- **Specific routes**: Custom pages like `/blog/post/:slug`, `/activities/:slug`
- **Catch-all route (`**`)**: Handles all dotCMS-generated pages through a single `PageComponent`

This approach eliminates the need to duplicate dotCMS folder/page structure in Angular routing, preventing developer intervention for every new route.

### Page Rendering

Pages are rendered using the `<dotcms-layout-body>` component from `@dotcms/angular` library. This component:
- Renders all page rows, columns, and content
- Accepts a component map via the `components` input
- Maps content type variable names to Angular components
- Automatically passes full content objects to components

Example component mapping:
```typescript
const DYNAMIC_COMPONENTS = {
  Banner: BannerComponent,
  Product: ProductComponent,
  Activity: ActivityComponent
};
```

### Folder Structure

```
src/app/
├── components/           # Standard site-wide components
│   ├── header/
│   ├── footer/
│   └── navigation/
└── dotcms/              # dotCMS-specific components
    ├── pages/           # Page components (see app.routes.ts)
    ├── components/      # Content type components
    └── types/           # TypeScript interfaces
```

### Server-Side Implementation

The `server.ts` file implements a secure API architecture:

#### `/api/page` Endpoint
- Uses `@dotcms/client` to fetch pages from dotCMS
- Hides sensitive information (auth tokens, dotCMS URLs) from client-side requests
- Prevents exposure of credentials in browser network inspector

#### Security Rationale
- **Initial SSR**: Server renders page, credentials stay server-side
- **Client Navigation**: RouterLink navigation exposes API calls in browser
- **Solution**: Proxy endpoint masks dotCMS requests from client inspection

## Development Workflow

1. **New Content Types**: Add component mapping to `DYNAMIC_COMPONENTS` in `page.ts`
2. **Custom Pages**: Create specific routes in `app.routes.ts`
3. **Site Components**: Add to `components/` folder for site-wide usage
4. **dotCMS Components**: Add to `dotcms/components/` for content rendering

## Additional Resources

- [Angular CLI Documentation](https://angular.dev/tools/cli)
- [dotCMS Angular Library](https://www.dotcms.com/docs/latest/angular-integration)
- [Angular SSR Best Practices](https://angular.dev/guide/ssr)
- [dotCMS GraphQL API](https://www.dotcms.com/docs/latest/graphql-api)
- [dotCMS Universal Visual Editor](https://www.dotcms.com/docs/latest/universal-visual-editor)
