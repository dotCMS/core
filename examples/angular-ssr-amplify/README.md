# Angular SSR with dotCMS on AWS Amplify Hosting

> [!NOTE]
> This example's `@dotcms/*` dependencies are pinned to `latest`, matching a dotCMS Evergreen instance (always the current release). If your dotCMS instance is **not** on Evergreen — an older self-hosted release, or an LTS server — installing as-is may fail with GraphQL `FieldUndefined` errors. Check your server's version and replace `latest` with that exact version for every `@dotcms/*` entry in `package.json` before installing.

> [!IMPORTANT]
> **Maintainers:** this example, [`examples/angular`](../angular) and [`examples/angular-ssr`](../angular-ssr) must stay on the same Angular version. `@dotcms/angular` is built against the Angular version in `core-web`, so an example left a major behind cannot build against the SDK it exists to demonstrate ([#37681](https://github.com/dotCMS/core/issues/37681)). Upgrade all three in the same PR, one major at a time with `ng update`.

This Angular project demonstrates how to implement editable dotCMS pages using Angular Server Side Rendering (SSR). It showcases best practices for integrating dotCMS content management with Angular's hybrid rendering capabilities.

It is the same application as [`examples/angular-ssr`](../angular-ssr), packaged for **AWS Amplify Hosting** instead of Vercel. Everything specific to Amplify is described in [Deploy to AWS Amplify Hosting](#deploy-to-aws-amplify-hosting).

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

Requires Node.js `^22.22.3`, `^24.15.0` or `>=26` (required by Angular 22) and npm.

### Setup

```bash
git clone -n --depth=1 --filter=tree:0 https://github.com/dotCMS/core
cd core
git sparse-checkout set --no-cone examples/angular-ssr-amplify
git checkout
cd examples/angular-ssr-amplify
npm install
cp .env.example .env
```

Edit `.env` with your dotCMS URL, an API token and your site identifier. The file is git-ignored.

### Development Server

```bash
npm start
```

Navigate to `http://localhost:4200/`. The application will automatically reload when source files are modified.

### Building for Production

```bash
npm run build
```

Build artifacts are stored in the `dist/` directory with performance optimizations.

### Running Tests

```bash
npm test
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

#### `/data/page` Endpoint
- Uses `@dotcms/client` to fetch pages from dotCMS
- Hides sensitive information (auth tokens, dotCMS URLs) from client-side requests
- Prevents exposure of credentials in browser network inspector
- Accepts only a site-relative page path and a `graphql` params object, and rejects cross-site browser requests

#### Security Rationale
- **Initial SSR**: Server renders page, credentials stay server-side
- **Client Navigation**: RouterLink navigation exposes API calls in browser
- **Solution**: Proxy endpoint masks dotCMS requests from client inspection

## Development Workflow

1. **New Content Types**: Add component mapping to `DYNAMIC_COMPONENTS` in `page.ts`
2. **Custom Pages**: Create specific routes in `app.routes.ts`
3. **Site Components**: Add to `components/` folder for site-wide usage
4. **dotCMS Components**: Add to `dotcms/components/` for content rendering

## Deploy to AWS Amplify Hosting

> [!NOTE]
> **Verification status.** The deployment bundle has been built and run locally with
> `npm run build:amplify && npm run start:amplify`: the compute entry serves
> server-rendered HTML on port 3000, a forwarded `*.amplifyapp.com` host is accepted and an
> unlisted host gets a `400`, and `/data/page` rejects unsafe paths and cross-site calls.
> It has not yet been verified on a live Amplify app, so treat the console steps below as a
> starting point and check the [Troubleshooting](#troubleshooting) table if a deploy
> misbehaves.

Amplify Hosting has built-in support for Next.js and Nuxt. Any other SSR framework, Angular
included, deploys by producing a bundle that follows the
[Amplify Hosting deployment specification](https://docs.aws.amazon.com/amplify/latest/userguide/ssr-deployment-specification.html).
This example builds that bundle for you.

### How it works

`npm run build:amplify` runs `ng build` and then assembles `.amplify-hosting/`:

```
.amplify-hosting/
├── deploy-manifest.json        # routing rules and the compute runtime
├── static/                     # dist/angular-ssr-amplify/browser (served by the CDN)
└── compute/default/
    ├── compute-entry.cjs       # starts the HTTP server on port 3000
    ├── server/                 # dist/angular-ssr-amplify/server
    └── .env                    # runtime variables (see below)
```

Four files do the work:

| File | Role |
| --- | --- |
| `amplify.yml` | Build settings: installs Node 24, runs `npm ci` and `npm run build:amplify`, and publishes `.amplify-hosting` |
| `scripts/amplify-postbuild.mjs` | Assembles `.amplify-hosting/` from the Angular output and stamps the built Angular version into the manifest |
| `amplify/deploy-manifest.json` | Routes `/*.js`, `/*.css` and other files with an extension to static storage (falling back to compute), and everything else to compute. Sets the `nodejs24.x` runtime |
| `amplify/compute-entry.cjs` | Imports the Express app exported by `src/server.ts` and serves it on port 3000 |

Three things differ from running `server.mjs` on a plain Node server, each handled in code:

**1. The compute must listen on port 3000**
Angular's `server.mjs` only calls `listen()` when it is the main module, and on its own
port. `amplify/compute-entry.cjs` imports the Express app and starts the HTTP server
itself. The server bundle is self-contained, so the compute ships without `node_modules`.

**2. Host validation behind CloudFront**
Angular 20.3+ validates the request host to prevent SSRF. Behind a proxy the public host
arrives in `x-forwarded-host`. The compute entry promotes it to `host` and strips the
`x-forwarded-*` headers, so Angular validates one clean host against the allowlist.
`localhost` and `*.amplifyapp.com` are allowed by default (`src/server.ts`). Using a custom
domain? Add it to `NG_ALLOWED_HOSTS` (comma-separated), otherwise requests to it get a
`400`. Allowlisting the deployment host is the app's responsibility by design
([angular/angular-cli#32616](https://github.com/angular/angular-cli/issues/32616)).

**3. SSR data fetch runs in-process**
During SSR the app calls its own `/data/page` endpoint with a relative URL. A server-only
HTTP interceptor (`src/app/server-base-url.interceptor.ts`) dispatches those calls through
the Express app in-process instead of over the public network. The dotCMS auth token never
leaves the server: only `/data/page` reads it.

Unlike [`examples/angular-ssr`](../angular-ssr), this example keeps Angular's default
`src/index.html`. With that name Angular emits `index.csr.html`, which lets you render
individual routes on the client (`RenderMode.Client`) in `src/app/app.routes.server.ts`.

### Environment variables

| Variable | Description |
| --- | --- |
| `DOTCMS_URL` | Your dotCMS instance URL |
| `DOTCMS_AUTH_TOKEN` | dotCMS API authentication token |
| `DOTCMS_SITE_ID` | Your dotCMS site identifier |
| `NG_ALLOWED_HOSTS` | Optional. Extra hosts allowed to reach the app, such as a custom domain |

Variables set in the Amplify console are available to the build, not to the compute
runtime. `amplify.yml` therefore writes the `DOTCMS_*` and `NG_ALLOWED_HOSTS` variables to
a `.env` file during the build, the post-build script copies it next to the compute entry,
and the entry loads it at startup.

> [!WARNING]
> With this approach the dotCMS token is stored inside the deploy artifact. Use a
> **read-only** token with the narrowest permissions the site needs. For production,
> keep the token in AWS Secrets Manager or SSM Parameter Store and load it when the
> compute starts, instead of bundling it.

### Deploy steps

1. Push this example to its own Git repository, with `amplify.yml` at the repository
   root. To deploy it from a subfolder of a larger repository, set it up as an Amplify
   [monorepo app](https://docs.aws.amazon.com/amplify/latest/userguide/monorepo-configuration.html)
   and add the matching `appRoot` to `amplify.yml`.
2. In the Amplify console choose **Create new app**, connect the repository and pick the
   branch.
3. In the build settings, confirm that Amplify picked up `amplify.yml` and that the build
   output directory (`baseDirectory`) is `.amplify-hosting`.
4. Under **Advanced settings**, add the environment variables from the table above.
5. Enable **SSR app logs** so the compute's output reaches CloudWatch.
6. Save and deploy. The app is served at `https://<branch>.<app-id>.amplifyapp.com`.

### Checking the bundle locally

```bash
npm run build:amplify
npm run start:amplify      # http://localhost:3000
```

This runs the same compute entry Amplify runs, with the `.env` from the project root
bundled at build time. It is the fastest way to reproduce a server-side deployment problem
with `curl`.

The compute only serves what Amplify sends to it. JavaScript, CSS and other static files
live in `.amplify-hosting/static` and are served by Amplify's CDN, so a browser pointed at
port 3000 gets the server-rendered HTML without styles or hydration. For a complete local
production run use `npm run build && npm run serve:ssr:angular-ssr-amplify` (port 4000).

### Troubleshooting

| Symptom | Likely cause |
| --- | --- |
| Build fails with an unsupported Node engine error | The build image is on an older Node. `amplify.yml` installs Node 24 in `preBuild`; keep that step |
| Deploy fails validating `deploy-manifest.json` | The manifest no longer follows the deployment specification, or `baseDirectory` is not `.amplify-hosting` |
| Every page returns `400` | The host is not allowlisted. Add your domain to `NG_ALLOWED_HOSTS` and redeploy |
| Pages render the shell without content | `DOTCMS_AUTH_TOKEN`, `DOTCMS_URL` or `DOTCMS_SITE_ID` is missing or wrong. `/data/page` logs the cause; the variables are read at build time, so redeploy after changing them |
| No logs in CloudWatch | **SSR app logs** is not enabled for the app |

## Additional Resources

- [Angular CLI Documentation](https://angular.dev/tools/cli)
- [dotCMS Angular Library](https://www.dotcms.com/docs/latest/angular-integration)
- [Angular SSR Best Practices](https://angular.dev/guide/ssr)
- [dotCMS GraphQL API](https://www.dotcms.com/docs/latest/graphql-api)
- [dotCMS Universal Visual Editor](https://www.dotcms.com/docs/latest/universal-visual-editor)
