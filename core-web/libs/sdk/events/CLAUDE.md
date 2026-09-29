# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

`@dotcms/events` is the dotCMS SDK for everything a page reports back to dotCMS: automatic pageviews, conversions, content clicks, content impressions, and experiments (A/B tests). It is greenfield: it sends only what dotCMS accepts today. It replaces `@dotcms/analytics` and `@dotcms/experiments`, which are deprecated.

One object, `events`, configured by one `init` call, runs a single Analytics.js instance. Experiments run inside that instance: an engine asks dotCMS which variant the visitor gets and redirects to it, and a plugin adds `context.experiments` to every event. The event payload is the one `@dotcms/analytics` sends.

The core is framework-free: the same object serves Next.js and plain React, and traditional (VTL) pages get it as `ca.min.js`, an IIFE dotCMS injects (see Traditional Pages). What a page prints so an experiment runs comes from this package too: `experimentMarkup` (`@dotcms/events/markup`) for any framework, and `DotCMSExperiment` (`@dotcms/events/react`) for React. The other SDKs are not involved: `@dotcms/uve` and `@dotcms/react` have no experiment code.

Status: prototype, not released yet.

## Essential Commands

```bash
# From core-web. Nx is not installed globally: always go through pnpm.
pnpm nx build sdk-events          # ESM + CJS package in dist/libs/sdk/events
pnpm nx test sdk-events           # Vitest
pnpm nx lint sdk-events           # ESLint, including the layer boundaries (below)

# One spec file
pnpm nx test sdk-events -- src/lib/impressions/tracker.spec.ts

# Size, publint and declared-dependency checks on the built package (builds every SDK first)
pnpm nx test sdk-bundle-budgets

# ca.min.js, the IIFE for traditional pages, in dist/libs/sdk/events-standalone
pnpm nx run sdk-events:build:standalone

# Current gzip size of every bundle probe, to review libs/sdk/bundle-budgets/budgets.json
./node_modules/.bin/tsx libs/sdk/bundle-budgets/src/measure.ts
```

`npx` fails in this repo with `EBADDEVENGINES` (the root `devEngines` requires pnpm), so run binaries from `node_modules/.bin` or through `pnpm exec`.

## How It Is Used

```ts
// src/instrumentation-client.ts (Next.js 15.3+): runs after the HTML loads, before hydration
import { events } from '@dotcms/events';

events.init({
    dotcmsUrl: process.env.NEXT_PUBLIC_DOTCMS_HOST!,
    siteAuth: process.env.NEXT_PUBLIC_DOTCMS_SITE_AUTH!,
    impressions: true,
    clicks: true
});
```

```ts
// Any other module gets the same object, already configured
import { events } from '@dotcms/events';

events.conversion('signup');
events.pageView({ campaign: 'spring' }); // only needed with autoPageView: false
```

```tsx
// Wrap what the experiment varies. In a server component it costs the client no JavaScript.
import { DotCMSExperiment } from '@dotcms/events/react';

<DotCMSExperiment page={pageAsset}>
    <DotCMSLayoutBody page={pageAsset} components={pageComponents} />
</DotCMSExperiment>
```

- `dotcmsUrl` and `siteAuth` (the Site Auth from the Content Analytics app) are required.
- Experiments and automatic pageviews are on by default. Impressions and clicks are opt-in, as in `@dotcms/analytics`.
- After `init`, the object is also `window.dotEvents`, for traditional pages and plain scripts.
- Calls made before Analytics.js loads, `init` included, are buffered (up to 50) and replayed once it does.
- `init` runs once. A second call with the same config does nothing; one with another config is ignored with a warning.
- On the server and inside the UVE editor every call is a no-op. Inside UVE the experiment's content is revealed, so nothing stays hidden while editing.
- `conversion` takes a name only, and there is no `track`: the SDK sends only the event types dotCMS accepts (see Events It Sends).
- `DotCMSExperiment` takes only the page: its script never calls dotCMS, so it needs no URL.

The full option table is in README.md.

## Architecture Overview

### Project Structure

```
libs/sdk/events/
├── src/
│   ├── index.ts                  # Public entry: `events` and the types its methods, options and errors use
│   ├── markup.ts                 # Public entry ./markup: `experimentMarkup` and its types
│   ├── react.ts                  # Public entry ./react: `DotCMSExperiment` and its props
│   ├── standalone.ts             # The IIFE entry, not an export: ca.min.js for traditional pages
│   └── lib/
│       ├── events.ts             # The `events` object: init, conversion, pageView, automatic pageviews
│       ├── models.ts             # Public types (DotCMSEvents*)
│       ├── pipeline/             # What every event goes through, in Analytics.js plugins
│       │   ├── identity/         # plugin.ts: context (site, session, user, device); activity.ts: session activity
│       │   ├── enricher/         # plugin.ts: page, UTM and custom data
│       │   ├── sender/           # plugin.ts: builds the dotCMS event; queue.ts; http.ts, and onError
│       │   ├── constants.ts      # Event types, endpoint, storage keys, SENDER_PLUGIN_NAME
│       │   ├── models.ts         # The pipeline's config and payloads, and the shapes dotCMS receives
│       │   ├── logger.ts
│       │   └── utils.ts          # Context, session, user id, page data
│       ├── contentlets/          # What the content trackers share
│       │   ├── constants.ts      # Contentlet class and attribute, observer debounce, the rescan event
│       │   ├── utils.ts          # Finding and reading contentlets, their DOM observer, the tracker plugins' helpers
│       │   └── viewport.ts       # Where a contentlet sits in the viewport
│       ├── impressions/          # plugin.ts, tracker.ts (IntersectionObserver + dwell), utils.ts, constants.ts
│       ├── clicks/               # plugin.ts, tracker.ts, utils.ts, constants.ts
│       ├── experiments/          # The experiments engine
│       │   ├── api.ts            # POST /api/v1/experiments/isUserIncluded
│       │   ├── boot.ts           # The boot script: applies the decision while the HTML is parsed
│       │   ├── constants.ts      # The experiment contract: attributes, hiding rule, timeouts, storage keys
│       │   ├── decision.ts       # decideVariant: the decision the boot script and the engine share
│       │   ├── dom.ts            # Reads the marks, reveals the marked content, leaves the page for a variant
│       │   ├── engine.ts         # Assignment check, pageview hold, redirect or reveal
│       │   ├── markup.ts         # experimentMarkup: attributes, hiding rule and boot script for a page
│       │   ├── models.ts         # isUserIncluded response, stored assignments, ExperimentBootState
│       │   ├── plugin.ts         # Analytics.js plugin: adds context.experiments
│       │   └── store.ts          # localStorage and sessionStorage state
│       ├── react/                # DotCMSExperiment, the React adapter (the only place React is imported)
│       └── standalone/           # config.ts: the events config, read from the script tag dotCMS injects
├── rollup.config.cjs             # Build, the same setup as @dotcms/client
├── vite.config.mts               # Vitest config
├── vite.standalone.config.mts    # The IIFE build: ca.min.js
├── project.json, package.json, tsconfig*.json, eslint.config.mjs
└── README.md
```

### What `init` Does

Everything a renderer or the experiment needs happens before `init` returns; Analytics.js loads afterwards.

1. Claims the config (`activeConfigKey`), so a second call returns at once.
2. Creates the experiments engine and starts its `isUserIncluded` check when one is due, or reveals the marked content when `experiments: false`.
3. Sets `window.__dotAnalyticsActive__` and sends `dotcms:analytics:ready`, so the page renderers print the contentlet attributes before they hydrate, and sets `window.dotEvents`.
4. Loads Analytics.js with `import('analytics')`. The module reads the time zone with `Intl.DateTimeFormat` when it is evaluated, so a static import put that work in the task that starts the app. Calls wait in `pendingCalls` meanwhile.
5. Once it loads, builds the instance, replays the waiting calls and starts the automatic pageviews.

### The Analytics.js Instance

`Analytics({ app: 'dotEvents', storage })` runs these plugins, in this order. The order matters: each plugin reads what the ones before it wrote.

1. **dot-events-identity** (`pipeline/identity/plugin.ts`): creates `context` with `site_auth`, `session_id`, `user_id` and `device`, and tracks session activity.
2. **dot-events-experiments** (`experiments/plugin.ts`): adds the session's `context.experiments` in `pageStart` and `trackStart`. Left out with `experiments: false`.
3. **dot-events-impressions** and **dot-events-clicks** (`impressions/plugin.ts`, `clicks/plugin.ts`): only added with `impressions` or `clicks`.
4. **dot-events-enricher** (`pipeline/enricher/plugin.ts`): adds page, UTM and custom data. Its hooks are keyed with the sender's name (`page:<name>`, `track:<name>`), so both plugins read it from `SENDER_PLUGIN_NAME`: renaming one alone would leave events without page data, and the sender would drop them.
5. **dot-events-sender** (`pipeline/sender/plugin.ts`): builds the events and sends them through the queue to `/api/v1/analytics/content/event`.

`storage` is `createMemoryStorage()`. Analytics.js keeps an anonymous id (`__anon_id`) that nothing here reads, and its default storage (`@analytics/storage-utils`) falls back to a cookie when localStorage is unavailable. In memory, nothing of Analytics.js's reaches localStorage, sessionStorage or a cookie; checked in Chrome with localStorage blocked. Its types leave `storage` out of the config, which the code works around with an intersection type.

The queue sends batches of up to 15 events, every 5 s, and flushes everything with `keepalive` when the page is hidden (not on SPA navigation, where it persists the queue to sessionStorage instead). A batch carries one `context`: the one of the last event queued.

### Pageview Flow on an Experiment Page

A returning visitor's variant is decided by the boot script the page prints, while the HTML is parsed, from what the engine stored on an earlier page. A new visitor's is decided by the engine, once it loads. Both run the same function, `decideVariant` (`experiments/decision.ts`): the engine imports it, and the boot script receives it printed next to itself. It is pure and reads only its input: the experiment, the rendered variant, the URL, the stored assignments and whether experiments are off. It returns one of:

- `unknown`: nothing is stored about the experiment; dotCMS has to be asked;
- `excluded`: experiments are off, or the visitor was evaluated and is not in this one;
- `assigned`: the server rendered the visitor's variant;
- `ignored`: the visitor has another variant and the URL already asks for it, so the route does not pass `variantName` to its page request; loading the URL again would loop;
- `redirect`: the visitor has another variant, at `url` (the same URL with `?variantName=<variant>`, or without the parameter for `DEFAULT`).

The rest of the contract between the two (the attributes, the hiding rule, the timeouts, the storage keys and `ExperimentBootState`) lives in `experiments/constants.ts` and `experiments/models.ts`.

1. On a page that runs an experiment, `DotCMSExperiment`, or a page that prints `experimentMarkup`, wraps what the experiment varies in an element marked `data-dot-experiment="<experiment id>"` and `data-dot-variant="<variant the server rendered>"`. It prints before that element the rule that hides it (`HIDING_RULE`, which shows it on its own after `DECISION_TIMEOUT_MS`, 3 s) and the boot script (`buildExperimentBootScript`). On any other page it prints nothing.
2. The boot script (`dotcmsExperimentBoot`) runs before the app's scripts and never calls dotCMS. Inside UVE it shows the content. Otherwise it runs `decideVariant` on what is stored: on `unknown` it does nothing, so the content stays hidden and the engine decides once it loads; on `redirect` it calls `window.stop()` and `location.replace` to the variant's URL; on anything else it shows the content.
3. When it replaces the page, it leaves `ExperimentBootState` (`redirectedTo`, the variant's URL) on `window.__dotEventsExperimentBoot`. The page being left may still run its app until the variant's document arrives, so for 5 s it holds every new `fetch` of that page except `keepalive` ones.
4. On DOMContentLoaded, `sendPageView` asks the engine to `decide()` before it calls `instance.page()`. The engine answers `redirected` at once when the boot script is replacing the page, so it neither redirects again nor sends that page's pageview.
5. Otherwise `decide()` reads the marks. With no marks the pageview goes out at once. With marks, it runs `decideVariant` on its stored assignments; on a page the script already showed, it reaches the same decision from the same storage. On `unknown` it holds the pageview and waits for `isUserIncluded`, up to `experiments.timeout` (3000 ms by default): when the wait times out it reveals the content and sends the pageview, and when dotCMS answers it decides again. Then:
   - `redirect`: it leaves the page with `leavePageFor` (`window.stop()`, `location.replace`, and the same 5 s hold of the page's new requests) and drops the pageview; the variant's page sends its own. On a first visit the page has hydrated by then, and Next would otherwise prefetch every visible link while the variant renders;
   - `assigned`: it adds the experiment to the session's `context.experiments`;
   - `ignored`: it warns that the route ignores `variantName`;
   - `excluded`: nothing more.

   In every case but `redirect`, it reveals the content and sends the pageview.
6. The content is revealed by appending `:root [data-dot-experiment="<id>"]{visibility:visible !important;animation:none !important}` to `<style id="dotcms-experiment-reveal">`, so neither side modifies DOM nodes that React owns. A style rule changes nothing the trackers observe, so `revealRows` then sends `CONTENTLET_RESCAN_EVENT` (`dotcms:events:rescan`) and the impression tracker scans again for the contentlets it skipped while they were hidden.

History changes (SPA navigation) run step 5 through `onRouteChange` from `@analytics/router-utils`, two animation frames later so the new route's content is in the DOM, and deduplicated by path plus query string. React never runs an inline script it renders on the client, so the boot script only acts on the page the server sent; the hiding rule, a plain `<style>`, still keeps the new route's experiment content hidden until the engine decides.

### What an Experiment Page Needs

- It wraps the content the experiment varies in `DotCMSExperiment`, or prints `experimentMarkup`, in the HTML the server sends. A dotCMS variant changes the content in the page's containers, so a view that does not render that content has nothing to vary.
- Its route passes the URL's `variantName` to the page request. Otherwise the variant's URL renders the original, and the visit is not counted.
- It renders on every request. In Next.js, reading `searchParams` does that; a page prerendered at build time cannot serve `variantName`, and does not see an experiment that started after the build.

### Traditional Pages (ca.min.js)

dotCMS injects the SDK into traditional pages as an IIFE: `src/standalone.ts`, built by `build:standalone` (`vite.standalone.config.mts`) into `dist/libs/sdk/events-standalone/ca.min.js`, one file with Analytics.js inside. It is not a package export and not published to npm.

- `readScriptConfig` (`lib/standalone/config.ts`) reads the tag dotCMS prints (`dotCMS/src/main/resources/ca/html/analytics_head.html`), in that template's attribute names: `data-analytics-auth` (required), `data-analytics-debug`, `data-analytics-auto-page-view` (opt-out), `data-analytics-impressions`, `data-analytics-clicks` and `data-analytics-config` (the app's advanced JSON, single quotes accepted; only `queue`, `impressions`, `clicks`, `autoPageView`, `debug` and `logLevel` are read). The template prints every placeholder, empty when unset, so an empty attribute leaves its default, and an attribute with a value wins over the JSON.
- `dotcmsUrl` is the page's origin: dotCMS serves the page and the script.
- It runs experiments, in place of dotCMS's own experiments script (`experiment/js/init_script.js`, which #37798 removes). `standalone.ts` calls `initEvents(config, 'contentlets')`: those pages carry no experiment markup, so the engine reads the contentlet wrappers dotCMS prints (`ContainerLoader`, since #34400 and #34401, content clicks and impressions on traditional pages). The experiment comes from the stored `isExperimentPage` rule, tested as dotCMS's script tests it (the part of the URL before the query in lowercase); the rendered variant from the wrappers' `data-dot-variant`, or from `variantName` when the variant changes none of them. `prepare()` runs while the head is parsed: it replaces a returning visitor's page with the assigned variant before it paints, or, on a first visit, hides `.dotcms-contentlet` until `isUserIncluded` answers (3 s at most). `decide()` then reveals them, or redirects, as on marked pages. A page that carries the experiment markup is still decided by it.
- dotCMS prints the wrappers only with content impressions or clicks on (`ContentAnalyticsUtil.isContentTrackingEnabled`); #37798 prints them whenever the site runs an experiment.
- The script finds its tag through `document.currentScript`, or `script[data-analytics-auth]`. Without a site auth it warns and does not start.
- The build fails if anything it imports reaches React (the `noReact` plugin in the Vite config).
- It is named `ca.min.js` because dotCMS serves and injects `/ext/analytics/ca.min.js`. `core-web/pom.xml` still copies `@dotcms/analytics`'s `dist/libs/sdk/analytics-standalone` there; pointing that resource at `dist/libs/sdk/events-standalone` switches dotCMS to this SDK. That switch is not made. VTL code that calls `window.dotAnalytics` would need `window.dotEvents`.

### Setup Mistakes It Reports

With `debug: true`, the engine warns through `console.warn` (`warn` in `EngineOptions`, silent otherwise) when:

- an experiment the visitor is in runs on the current URL, by the `regexs.isExperimentPage` rule `isUserIncluded` returns, but nothing on the page carries experiment marks: the page is not rendered as above, and the experiment never shows there. Once per URL;
- the URL already asks for the assigned variant but the server rendered another one: the route does not pass `variantName`.

The URL rules are used for these warnings only. Which experiment a page runs, and which variant the server rendered, always come from the marks.

### Errors

Nothing is retried, and no failure throws into the page. `onError` in the config receives a `DotCMSEventsError` with one of these codes:

- `REJECTED`: dotCMS answered an events request with an error, or with a 202 whose `entity.failed` is above 0. It carries the first error's message, the HTTP `status`, `detail` (dotCMS's error list) and the number of `events` the request carried. Raised by `reportResponse` in `pipeline/sender/http.ts`, on the `keepalive` path too.
- `NETWORK`: an events request got no answer.
- `EXPERIMENTS`: `isUserIncluded` failed, raised by the engine's `check`. A 403 (`status: 403`) means experiments are off for the site, and the engine stops asking for a day; other failures keep the stored assignments and carry the HTTP status when there is one.

The handler runs inside a `try` (`reportError` in `events.ts`, `notify` in `pipeline/sender/http.ts`): an error it throws is ignored.

### Events It Sends

The SDK sends the four event types dotCMS accepts, and no others (`DotCMSPredefinedEventType`):

- `pageview`: automatic, or `pageView(data)`, whose custom data dotCMS accepts;
- `conversion`: `conversion(name)` sends `{ name }`, and the sender builds `data: { conversion: { name }, page }`. dotCMS's schema for conversions (`dotCMS/src/main/resources/analytics/validators/conversion.json`) accepts `conversion.name`, `page.url` and `page.title` only, and answers 400 `UNKNOWN_FIELD` to `data.custom`;
- `content_impression` and `content_click`: from their trackers only, when `impressions` and `clicks` turn them on.

There is no custom-event API. dotCMS validates each event with the schema of its type (`validators/`), has one only for those four, and the fallback `AnalyticsValidatorUtil.validateEvents` uses for any other type matches none of the event's fields: every custom event comes back `UNKNOWN_FIELD` in a 202 `PARTIAL_SUCCESS`. `@dotcms/analytics`'s `track` sends that payload and fails the same way. An event API comes once the backend accepts it; adding one breaks no caller. The sender throws on any other type, which only a bug in the SDK can produce.

### State

What the SDK stores, all with the `dot_events_` prefix, and no cookies:

| Key                                | Storage        | Contents                                                                                                   |
| ---------------------------------- | -------------- | ---------------------------------------------------------------------------------------------------------- |
| `dot_events_user_id`               | localStorage   | The visitor's id, `context.user_id`                                                                         |
| `dot_events_experiments`           | localStorage   | Assignments (each expires with its `lookBackWindow`) and the experiment ids already evaluated, sent back as `exclude` |
| `dot_events_experiments_off_until` | localStorage   | Set when `isUserIncluded` answers 403: experiments stay off on the site for a day                           |
| `dot_events_session_id`            | sessionStorage | `context.session_id`, with its start and last activity: it ends after 30 minutes idle or at midnight        |
| `dot_events_tab_id`                | sessionStorage | The tab's id, which names its queue                                                                         |
| `dot_events_queue_<tab id>`        | sessionStorage | Events not sent yet, kept for the tab's next page                                                           |
| `dot_events_experiments_checked`   | sessionStorage | This tab already asked `isUserIncluded`                                                                     |
| `dot_events_session_experiments`   | sessionStorage | The session's cumulative `context.experiments`, keyed by the analytics session id                           |

What it puts on the page:

| Name                        | Kind         | What it is                                                                                              |
| --------------------------- | ------------ | ------------------------------------------------------------------------------------------------------- |
| `dotEvents`                 | window       | The `events` object, for traditional pages and plain scripts                                             |
| `__dotAnalyticsActive__`    | window       | The SDK is active: the React, Vue and Angular renderers then print the contentlet attributes            |
| `__dotEventsExperimentBoot` | window       | The boot script's `ExperimentBootState`, set when it replaces the page                                   |
| `dotcms:analytics:ready`    | window event | Sent once the SDK is active, so those renderers render again                                             |
| `dotcms:events:rescan`      | window event | Asks the impression tracker to scan again                                                                |
| `dotcms:events:cleanup`     | window event | Sent by the activity tracker on `pagehide` and `beforeunload`; nothing listens to it yet                |

New names use `dotEvents` or the `dot_events_` prefix, and console messages the `[dotCMS events]` prefix. `__dotAnalyticsActive__` and `dotcms:analytics:ready` keep their names because other SDKs read them: `@dotcms/uve/internal` declares them again (`ANALYTICS_ACTIVE_WINDOW_KEY`, `ANALYTICS_READY_EVENT`), and the React, Vue and Angular SDKs use them. Renaming them means changing `@dotcms/uve` in the same release.

The boot script only reads the two experiment keys in localStorage. `isUserIncluded` is asked once per tab session, or when the stored answer is more than a day old. A returning visitor with a stored assignment is redirected by the boot script without waiting for the network, before the page it leaves loads its app. Experiments that dotCMS reports as ended drop out of the stored assignments and of `context.experiments`. There are no cookies, by design: nothing on the server knows the visitor's variant, so a variant is always reached through the client-side redirect.

### How Impressions Find Contentlets

The impression tracker observes a contentlet only when it has a `data-dot-identifier`, is not hidden and has a size. None of that holds for long when the page loads, so it scans on four signals:

1. 100 ms after `init`.
2. A contentlet added to or removed from the DOM.
3. A contentlet receiving `data-dot-identifier`. The renderers print it after hydration, within milliseconds of the contentlet, so this observer (`createContentletObserver` in `contentlets/utils.ts`, with `identifiers: true`) does not drop the calls inside its 250 ms throttle window: the last one runs when the window ends.
4. `CONTENTLET_RESCAN_EVENT`, sent when hidden rows are shown.

A scan only adds contentlets it has not seen, so the signals can overlap freely. The click tracker attaches its listener regardless of the attributes and reads them on click, so it watches added contentlets only.

### Layout and Names

One grammar for all of `src/lib`, so the pipeline that sends every event reads like the capabilities built on it:

- **A folder per capability.** `impressions/`, `clicks/` and `experiments/` match the `init` options; `pipeline/` is what every event goes through; `contentlets/` is what the two content trackers share.
- **Files named by their role**, never by the package: `plugin.ts` is how a capability hooks into Analytics.js, next to `tracker.ts`, `engine.ts`, `store.ts`, `api.ts`, `constants.ts`, `models.ts`, `utils.ts`. A pipeline plugin with parts of its own gets a folder (`pipeline/sender/`: `plugin.ts`, `queue.ts`, `http.ts`).
- **No barrels inside `src/lib`.** The three entries are the only files that re-export.
- **Type prefixes say who a type is for.** `DotCMSEvents*` is public, in `src/lib/models.ts`. `DotCMS*` without `Events` is the shape dotCMS's API receives (`DotCMSPageViewEvent`, `DotCMSContentClickPayload`). Everything else is internal and unprefixed (`PipelineConfig`, `EventContext`, `EventRequestBody`, `PipelineError`, `StoredAssignments`).
- **Plugins** are `<capability>Plugin` factories (`identityPlugin`, `senderPlugin`, `impressionsPlugin`…) whose Analytics.js name is `dot-events-<capability>`.

`pipeline/` and the content trackers hold the code of `@dotcms/analytics`, copied here when that package was deprecated and reorganized into this layout; this package is now its home, and fixes go here. Porting a fix from `@dotcms/analytics` means finding the equivalent file, since the names no longer match.

Dependencies go one way. `pipeline/` imports nothing built on it; the content trackers build on `pipeline/` and `contentlets/`, never on each other; `experiments/` builds on `pipeline/` and takes one constant from `contentlets/`, the rescan event. `eslint.config.mjs` enforces it with `no-restricted-imports`, one block per folder, specs excepted. Flat config lets the last block that sets a rule replace the earlier ones, so each file matches one block:

| Files                       | May not import                                                                          |
| --------------------------- | --------------------------------------------------------------------------------------- |
| `src/lib/pipeline/**`       | `contentlets/`, `impressions/`, `clicks/`, `experiments/`, `react/`, `events.ts`, `models.ts`, React |
| `src/lib/contentlets/**`    | `impressions/`, `clicks/`, `experiments/`, `react/`, `events.ts`, `models.ts`, React     |
| `src/lib/impressions/**`    | `clicks/`, `experiments/`, `react/`, `events.ts`, `models.ts`, React                     |
| `src/lib/clicks/**`         | `impressions/`, `experiments/`, `react/`, `events.ts`, `models.ts`, React                |
| `src/lib/experiments/**`    | `contentlets/` except its constants, `impressions/`, `clicks/`, `react/`, `events.ts`, React |
| `src/lib/react/**`          | `pipeline/`, `contentlets/`, `impressions/`, `clicks/`, the engine, its API, store and plugin, `events.ts` |
| `src/lib/*.ts`              | `react/`, React                                                                         |
| `src/index.ts`              | the markup, the boot script, `react/`, React                                            |
| `src/markup.ts`             | `events.ts`, the engine, `pipeline/`, `contentlets/`, `impressions/`, `clicks/`, `react/`, React |
| `src/standalone.ts`         | anything but `lib/events.ts` and `lib/standalone/`                                       |
| `src/lib/standalone/**`     | anything but `lib/models.ts`                                                             |

`pipeline/` has two blocks, because its files sit one or two folders below `src/lib` and `../models` means the public types from the first and the pipeline's own from the second. The `experiments/` block uses a `regex`, because a gitignore-style negation cannot re-include a file whose folder an earlier pattern already excluded.

## Public API Rules

- Expose only what a consumer uses. The `exports` field has three entries: `.` (`events`, and the types its methods, options and errors use), `./markup` (`experimentMarkup` and its types) and `./react` (`DotCMSExperiment`, `DotCMSExperimentProps`). There is no `./internal`, and there must not be one. Adding an export is an API decision, not a refactor.
- An entry exports every type its public signatures use, as types (API Extractor calls a missing one a forgotten export). A type that a signature uses is already part of the contract, since changing it breaks consumers; exporting it only lets them name it.
- The root entry does not re-export `experimentMarkup`. Every page loads the engine, and a bundler that keeps a re-exported module (Turbopack did, measured) would ship the boot script builder with it.
- Public types are declared in `src/lib/models.ts` with the `DotCMSEvents` prefix; the React adapter's props stay with the component. Never export or re-export the core's types: they are internal and change freely.
- The internal `.d.ts` files ship, because `withNx` emits one per source file. With TypeScript's `moduleResolution: "node"` an editor resolves `@dotcms/events/src/...`; with `bundler` or `node16` it does not. No bundler or Node loads such a path, since the package has no JavaScript per source file, and TypeScript 6 already rejects that setting (TS5107) and TypeScript 7 removes it. Bundling the declarations would take a new dependency and set this package apart from the other SDKs, so it is left as is; README.md says only the three entries are public.
- The config and the payload stay compatible with `@dotcms/analytics`: the same `siteAuth`, the same event shapes, the same endpoint. Conversions differ on purpose: they carry no custom data, which dotCMS rejects.

## TypeScript

- `tsconfig.json` turns on every strict option on top of `strict`: `noPropertyAccessFromIndexSignature`, `noUncheckedIndexedAccess`, `exactOptionalPropertyTypes`, `verbatimModuleSyntax`, `noImplicitOverride`, `noImplicitReturns`, `noFallthroughCasesInSwitch`, `noUnusedLocals`, `noUnusedParameters`, `forceConsistentCasingInFileNames`, and unreachable code and unused labels as errors. Specs are held to the same options.
- The package compiles against the published declarations of `@dotcms/uve` and `@dotcms/types` (`paths` to their `dist`), as the Rollup build and every consumer do. Through the base aliases their sources would join the program and be checked with these options, which they were not written for. So `nx typecheck sdk-events` builds them first (`dependsOn: ["^build"]`).
- Type-only imports use `import type`, in a separate statement (`@typescript-eslint/consistent-type-imports`). A module that provides both values and types is imported twice, which `no-duplicate-imports` allows through `allowSeparateTypeImports`.
- In library code, an index that may miss gets a real check, not `!`. Specs may use `!`.
- Optional settings stay without `| undefined` in the public types: nested options are merged over the core's defaults, where an explicit `undefined` would replace a default.
- Public functions declare their return type, so a consumer's editor shows a type it can read: `DotCMSExperiment` returns `ReactElement`, and wraps its children in a fragment when no experiment runs.

## Build and Distribution

- `rollup.config.cjs` uses `withNx` like `@dotcms/client`: ESM and CJS output, `compiler: 'tsc'`, `generateExportsField`, and `tools/rollup/types-first.cjs` to put `types` first in the generated exports. The build type-checks, so a type error fails `nx build`.
- Three entries (`index`, `markup`, `react`). Rollup puts the modules they share in chunks: `decision.esm.js` (the experiment constants and `decideVariant`) is all the root entry shares with the other two, so the engine never loads the builder, which lives in `markup.esm.js`.
- `dotcmsExperimentBoot` (`boot.ts`) and `decideVariant` (`decision.ts`) are printed with `Function.prototype.toString`, so neither compiled function may reference module-level helpers. The `es2020` target needs none for what they use (no `async`, classes or decorators), even with `importHelpers` on. `decision.spec.ts` runs `decideVariant` rebuilt from its own source to catch a helper that sneaks in.
- The project's `build` target exists because `nx.json` lists `libs/sdk/events` in the `@nx/rollup/plugin` include. The SDK release builds `sdk-*` with that target and publishes every folder under `libs/sdk` that has a `package.json`, so this package ships with the others as soon as it is on main.
- `project.json` declares `implicitDependencies: ["sdk-types"]`. The declarations of `@dotcms/uve` import `@dotcms/types`, and `withNx` only points direct dependencies at their `dist`; without the implicit dependency the build compiles types' sources and fails with TS6059.
- `*.md` files are copied into the package, this one included.

## Dependencies

- **Runtime**: `analytics`, `@analytics/core`, `@analytics/queue-utils`, `@analytics/router-utils`, `@analytics/storage-utils`.
- **Peers**: `@dotcms/uve`, declared with the `"0.0.0"` sentinel that the SDK release workflow replaces with the exact release version; the built `.js` imports only `getUVEState` from it. `react` (`>=18`), optional, for `./react` only. The `declared-deps` spec in `sdk-bundle-budgets` fails when a shipped `@dotcms/*` import is not declared.
- The core stays framework-free: React is imported only in `src/lib/react`, behind `./react`, and Next.js nowhere.

## Testing

- Vitest, configured in `vite.config.mts`. The file is written by hand to match what `tools/generate-vite-configs.mjs` emits for a framework-free SDK, because the generator only handles projects migrated from Jest. One deviation: `pool: 'forks'`, because the core specs set `global.window = undefined` for SSR cases, which throws on `vmForks`.
- Specs sit next to their sources as `*.spec.ts`. The core's specs came with the core. Of the events layer:
  - `events.spec.ts` replaces Analytics.js with a fake instance and covers `window.dotEvents`, the public methods, the conversion payload and the memory storage;
  - `standalone.spec.ts` and `standalone/config.spec.ts` cover the IIFE: the attributes dotCMS prints, their defaults and overrides, the advanced JSON, and a tag without a site auth;
  - `experiments/` has specs for `decision.ts`, `boot.ts` (with a fake window), `engine.ts` (how it follows the boot script, `onError`, and pages marked by dotCMS's contentlet wrappers), `markup.ts` and `dom.ts`;
  - `react/DotCMSExperiment.spec.tsx` uses Testing Library without the jest-dom matchers, which this project does not set up.
- Vitest does not type-check, so run `tsc -p libs/sdk/events/tsconfig.spec.json` as well: it must report no errors. When the core specs moved here they carried 8 errors from `@dotcms/analytics` (an import of `ANALYTICS_CONTENTLET_CLASS`, which no longer exists, gave their contentlets the class `undefined`, and several fixtures no longer matched the models); those are fixed.

## Performance

- The `events-init` probe in `libs/sdk/bundle-budgets` bundles `import { events } from '@dotcms/events'` with its dependencies: 27,433 B gzip, against a 30,000 B ceiling. `events-react` bundles `DotCMSExperiment`: 1,354 B gzip against 2,200, and it fails if the engine or Analytics.js comes along. Raise a ceiling only with a measurement and a reason.
- `sideEffects: false`, and importing the package does nothing until `init`.
- `isUserIncluded` is asked once per tab session, by the engine only. The pageview hold is bounded by the timeout, and the CSS rule shows the content after 3 s even when no script runs.
- The boot script is about 1 KB gzip in the HTML of each page that runs an experiment (961 B in the example), minified by the app's build. From a server component it costs the client no JavaScript, but Next.js repeats the markup in the RSC payload of the HTML. From a client component, the builder (the `events-react` probe) is in that route's JavaScript.
- `ca.min.js` is 72,575 B raw and 24,923 B gzip, experiments included; `@dotcms/analytics`'s is 22,782 B gzip. The contentlets mode is in `events-init` too (412 B gzip), since the engine holds both.
- The impression and click plugins are only added when enabled, but their code is always in the bundle, because `getEnhancedTrackingPlugins` references both.

### Measured Cost

Chrome traces of the `nextjs-experiments` example in production mode (`next build && next start`, debug off), with dotCMS running locally:

- **Main thread, fast laptop:** about 15 ms per page load for this package together with the analytics libraries, with no long tasks and at most a few forced reflows (under 3 ms). Scrolling with impressions costs 11 ms over 13 s.
- **Main thread, CPU slowed down 4x:** with Analytics.js imported statically, about 55 ms of the first page's startup long task (155 ms) was this package and the analytics libraries, and roughly half of that was Analytics.js reading the time zone when its module was evaluated. Loading it with `import('analytics')` in `init` took that work out of the app's startup: measured before and after the change, the longest startup task went from 133 to 80 ms and the total blocking time from 83 to 41 ms.
- **Redirect to a variant, with and without the boot script.** Chrome 153 headless driven by Playwright, on the same laptop. Every visit starts after 16 s without traffic, so dotCMS's GraphQL cache (`cache.graphqlquerycache.seconds`, 15 s) is cold, as for a page nobody requested recently. "SDK alone" is the example with the hiding rule printed by hand and no boot script. "Boot script" was measured when `DotCMSLayoutBody` printed the script before the rows; `DotCMSExperiment` prints the same script before its content, so the numbers carry over, except that a wrapper around the whole page also keeps its header hidden while deciding. Medians in ms after the first byte of the first page, because the cold answer itself varies by up to 200 ms between visits; 6 visits per case at CPU 1x, 3 at 4x:

  | Case                                            | SDK alone | Boot script |
  | ----------------------------------------------- | --------- | ----------- |
  | First visit, variant, 1x: page replaced         | 74        | 85          |
  | First visit, variant, 1x: variant's rows shown  | 445       | 352         |
  | Returning, variant, 1x: page replaced           | 38        | 10          |
  | Returning, variant, 1x: variant's rows shown    | 446       | 366         |
  | First visit, `DEFAULT`, 1x: rows shown          | 152       | 105         |
  | Returning, `DEFAULT`, 1x: rows shown            | 104       | 102         |
  | First visit, variant, 4x: variant's rows shown  | 818       | 840         |
  | Returning, variant, 4x: page replaced           | 178       | 42          |
  | Returning, variant, 4x: variant's rows shown    | 667       | 358         |

  Without the boot script, the page being left sends 10 to 18 RSC prefetches and 4 to 8 image requests while the variant loads, and the variant's server render waits behind them (310 ms at 1x, against 212 ms with the script). With the script, a returning visitor leaves before the app's scripts run, so that page sends nothing. These first-visit numbers were taken when the boot script still asked `isUserIncluded` itself: it asked about 50 ms earlier, but its answer was only handled once the app's startup freed the main thread, so the page was replaced at about the same time as by the SDK alone. First visits are now the engine's, which holds the page's requests as the script did: in two first visits after the change, the page being left sent no RSC prefetch and the redirect came 95 to 116 ms after the first byte.

## Known Gaps (Prototype)

- Links to an experiment page are not rewritten to the assigned variant, so navigating to it goes through the redirect.
- One `context` per batch can attach `context.experiments` to events queued before the visitor reached the experiment.
- dotCMS still ships `@dotcms/analytics`'s `ca.min.js` and its own experiments script: this package's script is built but not wired in until #37798 (see Traditional Pages).
- A new visitor's first experiment page is decided by the engine, after the app's scripts load: that page hydrates and may request images before the redirect, and its content shows only once `isUserIncluded` answers. Only a decision that blocks parsing until dotCMS answers would avoid that, at the cost of delaying every visitor's first paint.
- The boot script and `leavePageFor` hold the `fetch` calls of the page being left by replacing `window.fetch` on it for 5 s. That page is going away, but the override is global while it lasts: keep it only if that trade is accepted.
- Only React has an adapter. Angular and Vue apps can print `experimentMarkup` themselves until they get one; traditional (VTL) pages run them from `ca.min.js`, on dotCMS's contentlet wrappers.
- With localStorage blocked, the core prints `[dotCMS events] Could not save dot_events_user_id to localStorage` on every page load, debug or not. Events still go out, without a lasting user id.
- `experiments/plugin.ts`, `experiments/store.ts` and `experiments/api.ts` have no specs, and `events.spec.ts` does not cover the pageview flow.

## Summary Checklist

Do:

- Run every build, test and lint through `pnpm nx`.
- Keep the public surface in the three entries (`index.ts`, `markup.ts`, `react.ts`), declare public types in `src/lib/models.ts`, and export every type a public signature uses.
- Keep the payload compatible with `@dotcms/analytics`.
- Change the experiment contract in `experiments/constants.ts` and `experiments/models.ts`, and the decision in `decideVariant`: the boot script, the engine and the adapters all read them from there.
- Name new storage keys `dot_events_*` and new globals after `dotEvents`.
- Put new code in the folder of its capability, name files by their role, and give a new capability a `plugin.ts` if it hooks into Analytics.js.
- Check the `events-init` and `events-react` budgets after adding code or a dependency.

Don't:

- Add an `./internal` export or re-export the core's types.
- Import React outside `src/lib/react`, or any other framework anywhere.
- Re-export `experimentMarkup` from the root entry.
- Add an event API dotCMS does not accept yet, or let Analytics.js write to storage or cookies.
- Import React in anything `src/standalone.ts` reaches: its build fails, on purpose.
- Add barrels inside `src/lib`, or give internal types a `DotCMS` prefix.
- Rename the sender plugin without `SENDER_PLUGIN_NAME`: the enricher's hooks are keyed with it.
- Rename `__dotAnalyticsActive__` or `dotcms:analytics:ready` without changing `@dotcms/uve` in the same release.
- Modify DOM nodes React owns; reveal through the style element.
- Put experiment code in `@dotcms/uve`, `@dotcms/react` or another SDK.
- Add a runtime dependency without measuring its size.
