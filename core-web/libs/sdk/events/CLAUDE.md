# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

`@dotcms/events` is the dotCMS SDK for everything a page reports back to dotCMS: automatic pageviews, conversions, content clicks, content impressions, and experiments (A/B tests). It replaces `@dotcms/analytics` and `@dotcms/experiments`, and is greenfield: it sends only what dotCMS accepts today.

One object, `dotEvents`, configured by one `init` call, runs a single Analytics.js instance. The core is framework-free; `DotCMSExperiment` (`./react`) and `experimentMarkup` (`./markup`) print what a page needs to run its experiment, and traditional (VTL) pages get the SDK as `ca.min.js`. Status: prototype, not released yet.

How apps use it, every option and what it stores are in [README.md](README.md). This file covers what you need to change the code safely.

## Essential Commands

```bash
# From core-web. Nx is not installed globally: always go through pnpm.
pnpm nx build sdk-events          # ESM + CJS package in dist/libs/sdk/events
pnpm nx test sdk-events           # Vitest
pnpm nx lint sdk-events           # ESLint, including the layer boundaries
pnpm nx test sdk-events -- src/lib/impressions/tracker.spec.ts   # one spec file

# Vitest does not type-check: this must report no errors
./node_modules/.bin/tsc -p libs/sdk/events/tsconfig.spec.json --noEmit

# Size, publint and declared-dependency checks on the built package (builds every SDK first)
pnpm nx test sdk-bundle-budgets
./node_modules/.bin/tsx libs/sdk/bundle-budgets/src/measure.ts   # current gzip size of each probe

# ca.min.js, the IIFE for traditional pages, in dist/libs/sdk/events-standalone
pnpm nx run sdk-events:build:standalone
```

`npx` fails in this repo with `EBADDEVENGINES` (the root `devEngines` requires pnpm): run binaries from `node_modules/.bin` or through `pnpm exec`.

## Architecture Overview

### Project Structure

```
src/
├── index.ts        # Public entry: dotEvents and the types its methods, options and errors use
├── markup.ts       # Public entry ./markup: experimentMarkup
├── react.ts        # Public entry ./react: DotCMSExperiment
├── standalone.ts   # The IIFE entry (ca.min.js), not an export
└── lib/
    ├── events.ts       # dotEvents: init, conversion, pageView, automatic pageviews
    ├── models.ts       # Public types (DotCMSEvents*)
    ├── pipeline/       # What every event goes through: identity, enricher, sender (queue, http), navigation
    ├── contentlets/    # What the two content trackers share
    ├── impressions/    # IntersectionObserver + dwell time
    ├── clicks/         # One capture listener on the document
    ├── experiments/    # The engine, the boot script, decideVariant, the store
    ├── react/          # DotCMSExperiment: the only place React is imported
    └── standalone/     # Reads the config from the script tag dotCMS prints
```

### What `init` Does

1. Claims the config, so a second call with the same config returns at once (another config is ignored with a warning).
2. Creates the experiments engine and starts its `isUserIncluded` check, or reveals the marked content when `experiments: false`.
3. Sets `window.__dotAnalyticsActive__` and sends `dotcms:analytics:ready`, so the React, Angular and Vue renderers print the contentlet attributes; sets `window.dotEvents`.
4. Loads Analytics.js with `import('analytics')`, kept out of the app's startup task. Calls made meanwhile wait (up to 50) and run once it loads.
5. Builds the instance, replays those calls and starts the automatic pageviews.

On the server and inside the UVE editor every call is a no-op.

### Plugin Order

The Analytics.js plugins run in this order, and each reads what the earlier ones wrote:

1. `dot-events-identity`: creates `context` (site, session, user, device) and tracks session activity.
2. `dot-events-experiments`: adds `context.experiments`; left out with `experiments: false`.
3. `dot-events-impressions` and `dot-events-clicks`: only when turned on.
4. `dot-events-enricher`: page, UTM and custom data. Its hooks are keyed with the sender's name, so both read it from `SENDER_PLUGIN_NAME`: renaming one alone leaves events without page data, and the sender drops them.
5. `dot-events-sender`: builds the dotCMS events and sends them through the queue.

Analytics.js gets `createMemoryStorage()`: its default storage falls back to a cookie, and nothing of Analytics.js's may reach storage or cookies.

### Queue and Page Lifecycle

- Batches of up to 15 events, every 5 s. Each event keeps the context it was created with, and a batch goes out as one request per context (site, session, visitor, experiments).
- On a hidden page or `pagehide`, the queue sends everything with `keepalive`. On an SPA navigation it persists the queue to sessionStorage instead.
- A page kept in the back/forward cache comes back as it was, so the trackers clean up only on the `pagehide` that discards the page (`onPageDiscard`). `beforeunload` is not used: it fires before the page goes into that cache too.
- When the browser restores such a page (`onPageRestore`), it counts as a new view: a pageview, and the impressions count again. A restore is not a navigation: `onNavigation` reports nothing and the engine's navigation count stays.

### Experiments

- The contract (attributes, hiding rule, timeouts, storage keys, `ExperimentBootState`) lives in `experiments/constants.ts` and `experiments/models.ts`, and the decision in `decideVariant` (`decision.ts`). The boot script and the engine both run it, so change the decision there only.
- The boot script decides returning visitors while the HTML is parsed, from what is stored; the engine decides new visitors once it loads, waiting for `isUserIncluded` up to `experiments.timeout`. That wait is capped at 3 s, the time the hiding rule keeps the content hidden on its own.
- `sendPageView` asks the engine to `decide()` before `instance.page()`, and drops the pageview when the page redirects to a variant or the visitor leaves during the wait.
- Content is revealed through a style rule keyed by the rendered variant, never by touching DOM nodes React owns.
- `context.experiments` goes only on the events of pages that run an experiment the visitor is in, with every experiment the visitor joined in the session. This is a product decision: the analytics service filters events by `experiments` before grouping them by session, so Reach Page and URL Parameter goals reached on other pages don't count until it attributes them by session.
- Traditional pages run in `contentlets` mode (`initEvents(config, 'contentlets')`): the experiment comes from the stored `isExperimentPage` rule and the variant from the contentlet wrappers' `data-dot-variant`.
- With `debug: true`, the engine warns about setup mistakes through `warn` (silent otherwise): an experiment page with no marks, a route that ignores `variantName`, a timeout above 3 s.

### Traditional Pages

- `ca.min.js` reads its config from the script tag dotCMS prints (`readScriptConfig` in `standalone/config.ts`, with the attribute names of `dotCMS/src/main/resources/ca/html/analytics_head.html`); `dotcmsUrl` is the page's origin.
- dotCMS still serves `@dotcms/analytics`'s build at `/ext/analytics/ca.min.js`: `core-web/pom.xml` copies `dist/libs/sdk/analytics-standalone` there. Pointing it at `dist/libs/sdk/events-standalone` switches dotCMS to this SDK (#37798, the backend changes that make it the only script). VTL code that calls `window.dotAnalytics` then needs `window.dotEvents`.

### Content Trackers

- The impression tracker observes contentlets that have a `data-dot-identifier`, are visible and have a size. It scans 100 ms after `init`, on DOM changes, when an identifier arrives, and on `dotcms:events:rescan`, and saves each contentlet's position in `data-dot-dom-index`. It forgets the page's impressions on a navigation and on a restore.
- The click tracker listens once, on the document, in the capture phase, and finds the clicked link or button and its contentlet with `closest`. It writes nothing to the contentlets.

## Rules That Break Silently

### Public API

- Three entries: `.`, `./markup` and `./react`. No `./internal`, ever. Adding an export is an API decision.
- Each entry exports every type its public signatures use. Public types live in `src/lib/models.ts` with the `DotCMSEvents` prefix; never export the core's internal types.
- The root entry does not re-export `experimentMarkup`: a bundler that keeps the re-export ships the boot script builder to every page.
- The event payload and the config stay compatible with `@dotcms/analytics`. Conversions carry a name only, on purpose: dotCMS rejects their custom data.
- The SDK sends the four event types dotCMS accepts (`pageview`, `conversion`, `content_impression`, `content_click`) and nothing else; the sender throws on any other type. No custom-event API until the backend accepts one.

### Names Other Code Reads

- `__dotAnalyticsActive__` and `dotcms:analytics:ready` keep their names because `@dotcms/uve` and the React, Angular and Vue SDKs read them: rename them only together with `@dotcms/uve`.
- New storage keys use the `dot_events_` prefix, new globals the `dotEvents` name, console messages the `[dotCMS events]` prefix. No cookies.

### Printed Functions

`dotcmsExperimentBoot` (`boot.ts`) and `decideVariant` are printed into the page with `toString()`, so neither may reference anything outside itself: no module constants, no helpers the compiler adds (the `es2020` target needs none for what they use). `decision.spec.ts` and `boot.spec.ts` run them from their printed source and fail on an outside reference.

### Layers

One folder per capability, files named by their role (`plugin.ts`, `tracker.ts`, `engine.ts`, `store.ts`...), no barrels inside `src/lib`. Dependencies go one way: `pipeline/` imports nothing built on it, the content trackers never import each other, and React stays in `src/lib/react`. `eslint.config.mjs` enforces it with one `no-restricted-imports` block per folder; read it before moving code between folders.

## TypeScript

- Every strict option is on, on top of `strict` (including `exactOptionalPropertyTypes` and `noUncheckedIndexedAccess`), specs included.
- The package compiles against the built declarations of `@dotcms/uve` and `@dotcms/types`, so `nx typecheck sdk-events` builds them first.
- Private fields use `#` (they cost bytes at this target; see Performance); private methods keep the `private` keyword. Specs test behavior, not private state.
- Type-only imports use `import type`, in a separate statement. In library code, an index that may miss gets a real check, not `!`.
- Optional settings stay without `| undefined` in the public types: they are merged over defaults, where an explicit `undefined` would replace a default.

## Build and Distribution

- `rollup.config.cjs` uses `withNx`, like `@dotcms/client`: ESM and CJS, `generateExportsField`, `types` first in the exports. The build type-checks.
- The three entries share one chunk, `decision.esm.js`, so the engine never loads the markup builder.
- The `build` target exists because `nx.json` lists `libs/sdk/events` in the `@nx/rollup/plugin` include. `project.json` declares `implicitDependencies: ["sdk-types"]`, without which the build fails with TS6059.
- Only `README.md` is copied into the package. This file is for working on the repo and does not ship.

## Dependencies

- Runtime: `analytics`, `@analytics/queue-utils` and `@analytics/router-utils`, the packages the built code imports. `@analytics/core` and `@analytics/storage-utils` come with `analytics`.
- Peers: `@dotcms/uve`, with the `"0.0.0"` sentinel the SDK release replaces with the release version, and `react` (`>=18`), optional, for `./react` only.
- Next.js 16 or later is stated in the README but not declared as a peer: nothing imports `next`, and a peer would only make npm refuse to install on an older version.

## Testing

- Vitest, configured by hand in `vite.config.mts` to match what `tools/generate-vite-configs.mjs` emits, with `pool: 'forks'`: the core specs set `global.window = undefined` for SSR cases, which throws on `vmForks`.
- Specs sit next to their sources as `*.spec.ts`. `events.spec.ts` and `events.pageviews.spec.ts` replace Analytics.js with a fake instance.
- Run the `tsc` command above as well: Vitest does not type-check.

## Performance

- `events-init` (`import { dotEvents } from '@dotcms/events'` with its dependencies): 29,208 B gzip, against a 30,000 B ceiling; the `#` fields are 977 B of it. `events-react`: 1,496 B against 2,200, and it fails if the engine or Analytics.js comes along. Raise a ceiling only with a measurement and a reason.
- `ca.min.js`: 73,098 B raw, 25,653 B gzip, experiments included.
- `sideEffects: false`: importing the package does nothing until `init`.

## Summary Checklist

Do:

- Run builds, tests and lint through `pnpm nx`, and the spec `tsc` too.
- Keep the public surface in the three entries, with public types in `src/lib/models.ts`.
- Change the experiment contract in `experiments/constants.ts` and `experiments/models.ts`, and the decision in `decideVariant`.
- Name storage keys `dot_events_*` and globals after `dotEvents`.
- Put new code in its capability's folder, and give a new capability a `plugin.ts` if it hooks into Analytics.js.
- Check the bundle budgets after adding code or a dependency.

Don't:

- Add an `./internal` export, re-export the core's types, or re-export `experimentMarkup` from the root.
- Import React outside `src/lib/react`, or anything that reaches React from `src/standalone.ts` (its build fails on purpose).
- Add an event type dotCMS does not accept, or let Analytics.js write to storage or cookies.
- Reference anything outside `dotcmsExperimentBoot` or `decideVariant`.
- Rename the sender plugin without `SENDER_PLUGIN_NAME`, or `__dotAnalyticsActive__` and `dotcms:analytics:ready` without `@dotcms/uve`.
- Modify DOM nodes React owns: reveal through the style rule.
- Put experiment code in `@dotcms/uve`, `@dotcms/react` or another SDK.
