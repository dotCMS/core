# dotCMS Vue.js Example — TravelLux

An editorial travel front end ("TravelLux") powered by [dotCMS](https://www.dotcms.com/) as a headless CMS and **Vue 3 + Vite + TypeScript + Tailwind CSS v4**, using the [`@dotcms/vue`](../../core-web/libs/sdk/vue) SDK. Content is managed in dotCMS and rendered here, fully editable in-context through the **Universal Visual Editor (UVE)**.

This mirrors the [Next.js example](../nextjs) feature-for-feature (minus AI search), showing the same content-type components, block-editor rendering, inline editing, and layout system.

## Requirements

1. A dotCMS instance (v25.05 / Evergreen) — or [demo.dotcms.com](https://demo.dotcms.com).
2. A **read-only** API token for the instance ([docs](https://auth.dotcms.com/docs/latest/rest-api-authentication#creating-an-api-token-in-the-ui)).
3. Node.js **22.18+** or **24.12+** (required by the Vite 8 / TypeScript 6 toolchain) and npm.

## Setup

```bash
# 1. Install dependencies
npm install

# 2. Configure the environment
cp .env.example .env.local
# then edit .env.local:
#   VITE_DOTCMS_HOST       — your dotCMS URL (e.g. http://localhost:8080)
#   VITE_DOTCMS_AUTH_TOKEN — a read-only API token
#   VITE_DOTCMS_SITE_ID    — your site identifier
#   VITE_DOTCMS_MODE       — 'production'

# 3. Run the dev server (http://localhost:5173)
npm run dev
```

### Configure the UVE in dotCMS

To edit pages in-context, point the Universal Visual Editor at this app. In dotCMS go to **Settings → Apps → UVE** and set:

```json
{ "config": [{ "pattern": "(.*)", "url": "http://localhost:5173" }] }
```

## Scripts

```bash
npm run dev          # Vite dev server
npm run build        # type-check (vue-tsc) + production build
npm run preview      # preview the production build
npm run type-check   # vue-tsc --noEmit
```

## How it works

- **Client** — `src/lib/dotCMSClient.ts` builds the dotCMS Vue plugin (`createDotCMSVue`) from the env config; `main.ts` installs it with `app.use()`, so components read the client with `useDotCMSClient()`. It also re-exports `.client` for code outside a component `setup` (e.g. the page loader).
- **Fetching** — `src/utils/getDotCMSPage.ts` fetches a page plus extra GraphQL content (blogs, destinations, navigation) in one request.
- **Images** — `src/utils/imageLoader.ts` wraps the SDK's `createDotCMSImageLoader` to build optimized `/dA/` image URLs.
- **Routing** — `vue-router` with a catch-all route for dotCMS pages, plus `/blog` and `/blog/post/:slug`.
- **Rendering** — `useEditableDotCMSPage()` prepares the page for editing and `DotCMSLayoutBody` renders the layout, dispatching each contentlet to a component from the map in `src/components/content-types/index.ts`.
- **Content types** — one Vue component per dotCMS Content Type in `src/components/content-types/`. **Keys in the map must match the Content Type variable name exactly.**
- **Editing** — `DotCMSEditableText` (inline text), `DotCMSBlockEditorRenderer` (rich text with custom renderers), and the Edit/Reorder buttons appear only inside the UVE in edit mode.

### Adding a Content Type

1. Create `src/components/content-types/MyType.vue` reading the contentlet fields it needs as props.
2. Register it in `src/components/content-types/index.ts` under its Content Type variable name.
