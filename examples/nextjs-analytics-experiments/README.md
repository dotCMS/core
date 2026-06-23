# dotCMS Analytics + Experiments — Next.js Example

Minimal headless Next.js (App Router) example showing **separated** integration of:

- **Content Analytics** (`@dotcms/analytics`) — automatic page views on every route via root layout
- **A/B Experiments** (`@dotcms/experiments`) — isolated to the `/blog` route only

Use this as a copy-paste starting point for customer headless implementations.

## Architecture

```
app/layout.tsx          → DotContentAnalytics (page views on ALL routes)
app/[[...slug]]/page    → views/Page.tsx        (normal render, no experiments)
app/blog/page.tsx       → views/BlogPage.tsx    (withExperiments wrapper)
```

| Route | Analytics | Experiments |
| --- | --- | --- |
| `/`, `/about`, etc. | Auto page views | — |
| `/blog` | Auto page views | `POST /api/v1/experiments/isUserIncluded` |

## Prerequisites

- Node.js 22+
- A running dotCMS instance with published pages at `/` and `/blog`
- dotCMS Analytics / Experiments stack configured (see [docker/docker-compose-examples/experiments](../../docker/docker-compose-examples/experiments/README.md))

## Quick Start

```bash
cd examples/nextjs-analytics-experiments
cp .env.local.example .env.local
# Edit .env.local with your dotCMS credentials and jsKey
npm install
npm run dev
```

Open [http://localhost:3000](http://localhost:3000).

## Environment Variables

Copy `.env.local.example` to `.env.local`:

| Variable | Description |
| --- | --- |
| `NEXT_PUBLIC_DOTCMS_HOST` | dotCMS origin URL (e.g. `http://localhost:8080`). **Not** a CDN. |
| `NEXT_PUBLIC_DOTCMS_AUTH_TOKEN` | API token from dotCMS Admin |
| `NEXT_PUBLIC_DOTCMS_SITE_ID` | Site identifier from dotCMS Admin |
| `NEXT_PUBLIC_DOTCMS_ANALYTICS_KEY` | **jsKey** from Analytics app (frontend key, not m2m) |
| `NEXT_PUBLIC_DOTCMS_MODE` | `production` or `edit` (UVE) |
| `NEXT_PUBLIC_DOTCMS_DEBUG` | `true` for verbose SDK console logs |

### Getting the jsKey

For a local experiments stack, fetch keys from the configurator:

```bash
curl http://localhost:8088/c/customer1/cluster1/keys
```

Use the **`jsKey`** value for `NEXT_PUBLIC_DOTCMS_ANALYTICS_KEY`. This single key feeds both analytics (`siteAuth`) and experiments (`apiKey`).

> **Do not** use the `m2mKey` in frontend apps — that key is for server-to-server dotCMS configuration.

## SDK Versions

All `@dotcms/*` packages should be aligned:

```json
"@dotcms/client": "^1.5.6",
"@dotcms/react": "^1.5.6",
"@dotcms/experiments": "^1.5.6",
"@dotcms/analytics": "^1.5.6"
```

> `@dotcms/analytics` is published on npm alongside other `@dotcms/*` SDK packages — keep versions aligned.

### dotCMS Platform Compatibility

- Minimum dotCMS: **v25.05**
- Recommended: latest Evergreen release
- SDK versions are published independently on npm but should be kept in sync across `@dotcms/*` packages

## Key Files

| File | Purpose |
| --- | --- |
| `src/config/dotcms.config.ts` | Unified env mapping for client, analytics, experiments |
| `src/lib/dotCMSClient.ts` | `@dotcms/client` instance |
| `src/app/layout.tsx` | `<DotContentAnalytics />` for site-wide page views |
| `src/views/Page.tsx` | Standard page render (analytics only) |
| `src/views/BlogPage.tsx` | Blog render with `withExperiments(DotCMSLayoutBody)` — always called on this route |
| `src/components/AnalyticsDemoCta.tsx` | Example `useContentAnalytics().track()` call |

## Why experiments only on `/blog`

This example intentionally separates analytics and experiments across routes:

- **Analytics** (`@dotcms/analytics`) runs site-wide from `layout.tsx` — automatic page views on every navigation.
- **Experiments** (`@dotcms/experiments`) run only on `/blog` via a dedicated `BlogPage.tsx` view.

`withExperiments` uses React hooks internally and **must not** be called conditionally (for example `apiKey ? withExperiments(...) : DotCMSLayoutBody`) in the same component. That pattern violates the rules of hooks and can crash Next.js App Router pages and the UVE editor.

This architecture is the recommended pattern documented in the [Experiments SDK README](../../core-web/libs/sdk/experiments/README.md) and addresses [GitHub issue #36225](https://github.com/dotCMS/core/issues/36225).

For pages without A/B testing, use `views/Page.tsx` (standard `DotCMSLayoutBody` only). For experiment-enabled pages, use a dedicated view like `BlogPage.tsx` on its own route.

## UVE (Universal Visual Editor) Setup

In dotCMS, configure the headless app URL pattern:

```
(.*)  →  http://localhost:3000
```

Set `NEXT_PUBLIC_DOTCMS_MODE=edit` when editing in UVE.

When opening `/blog` in UVE, the page should render without a "client-side exception" overlay. If it crashes, verify that `BlogPage.tsx` does not conditionally skip `withExperiments`.

## Verification Checklist

After `npm run dev`, open DevTools → Network:

1. **Any route** — `POST {HOST}/api/v1/analytics/content/event` (page view)
2. **`/blog` only** — `POST {HOST}/api/v1/experiments/isUserIncluded`
3. With `NEXT_PUBLIC_DOTCMS_DEBUG=true` — `[dotCMS ...]` logs in console
4. Open `/` and `/blog` in UVE — no client-side exception

Build check:

```bash
npm run build
npm start
```

## Troubleshooting

| Symptom | Likely cause |
| --- | --- |
| 500 / page not found | No published page in dotCMS at that path |
| No analytics events | Wrong `jsKey`, or testing inside UVE editor (analytics auto-disabled) |
| No experiment assignment | Missing `NEXT_PUBLIC_DOTCMS_ANALYTICS_KEY`, or no active experiment on `/blog` |
| UVE crash on `/blog` | `withExperiments` called conditionally — use dedicated view pattern (see above) |
| `GET /experiments/DEFAULT` 404 | Known backoffice bug (variant name used as experiment ID) — harmless in headless apps |
| Image 404 | Check `NEXT_PUBLIC_DOTCMS_HOST` matches dotCMS origin |

## Related Examples

- [examples/nextjs](../nextjs) — Full-featured Next.js demo with more content types
- [core-web/libs/sdk/analytics/README.md](../../core-web/libs/sdk/analytics/README.md) — Analytics SDK docs
- [core-web/libs/sdk/experiments/README.md](../../core-web/libs/sdk/experiments/README.md) — Experiments SDK docs
