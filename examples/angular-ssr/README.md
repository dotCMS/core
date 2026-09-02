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
git clone -n --depth=1 --filter=tree:0 https://github.com/dotCMS/core
cd core
git sparse-checkout set --no-cone examples/angular-ssr
git checkout
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

## Deploy to Vercel

### Environment variables

Set these in your Vercel project for **both Production and Preview**:

| Variable | Description |
| --- | --- |
| `DOTCMS_URL` | Your dotCMS instance URL |
| `DOTCMS_AUTH_TOKEN` | dotCMS API authentication token |
| `DOTCMS_SITE_ID` | Your dotCMS site identifier |

Custom domain? Add it to `NG_ALLOWED_HOSTS` (comma-separated). `localhost` and
`*.vercel.app` are allowed by default.

### How it works

Vercel runs the Angular SSR app as a serverless function, behind a proxy. Three
things differ from a plain Node server, each handled in code:

**1. Routing — `vercel.json` + `api/index.js`**
All requests are rewritten to the `api/index.js` function, which imports the
compiled Express app from `server.ts` and calls it. `app.listen()` never runs:
Vercel invokes the function directly, so there is no listening port.

**2. SSR data fetch runs in-process**
During SSR the app calls its own `/data/page` endpoint via a relative URL.
Angular resolves relative URLs against the request host, so the server would
fetch *itself* over the public network — unreachable on Vercel (no listening
port; the public URL round-trips through the edge). A server-only HTTP
interceptor (`src/app/server-base-url.interceptor.ts`) instead dispatches these
calls through the Express `app` **in-process** (passed in via `requestContext`
in `server.ts`). No network, no port. The dotCMS auth token never leaves the
server — only `/data/page` reads it.

**3. Host validation behind the proxy**
Angular 20.3+ validates the request host to prevent SSRF. Behind Vercel's proxy
the real host is in `x-forwarded-host` while `host` is an internal value.
`api/index.js` promotes `x-forwarded-host` to `host` and strips `x-forwarded-*`,
so Angular validates one clean host against the allowlist. Allowlisting the
deployment host is the app's responsibility by design
([angular/angular-cli#32616](https://github.com/angular/angular-cli/issues/32616)).

> A missing `DOTCMS_AUTH_TOKEN` makes `/data/page` fail; the app logs the cause
> and renders the page shell instead of crashing. If a deploy renders empty,
> check the function logs and the environment variables.

`src/indexFile.html` is the renamed `index.html` — Angular's default name would
be served statically by Vercel and bypass SSR.

## Additional Resources

- [Angular CLI Documentation](https://angular.dev/tools/cli)
- [dotCMS Angular Library](https://www.dotcms.com/docs/latest/angular-integration)
- [Angular SSR Best Practices](https://angular.dev/guide/ssr)
- [dotCMS GraphQL API](https://www.dotcms.com/docs/latest/graphql-api)
- [dotCMS Universal Visual Editor](https://www.dotcms.com/docs/latest/universal-visual-editor)
