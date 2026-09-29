# @dotcms/events

One SDK for dotCMS events: automatic pageviews, conversions, content clicks and content impressions, and experiments. Everything runs in a single Analytics.js instance. For experiments, the SDK asks dotCMS for the visitor's variant, redirects to it, and adds `context.experiments` to every event.

It replaces `@dotcms/analytics` and `@dotcms/experiments`.

Status: prototype. Nothing here is released yet.

## Entries

| Import                  | What it has                                                               | Who imports it                                      | Framework          |
| ----------------------- | ------------------------------------------------------------------------- | --------------------------------------------------- | ------------------ |
| `@dotcms/events`        | `events`, and the types of its methods, options and errors                | Every app: the `init` call and any module that sends events | None           |
| `@dotcms/events/markup` | `experimentMarkup`: what a page prints so its experiment runs             | Server code of a framework without an adapter       | None               |
| `@dotcms/events/react`  | `DotCMSExperiment`, which prints that markup around its children          | React and Next.js pages that run experiments        | React 18 or later  |

Only these three are public. Anything under `src/` in the package is internal, even when an editor resolves it (with TypeScript's old `moduleResolution: "node"`, for instance): it changes without notice, and bundlers and Node refuse to load it. `ca.min.js`, the script for traditional pages, is a separate build, not an import (see [Traditional Pages](#traditional-pages)).

## Next.js

Call `init` once, in `src/instrumentation-client.ts`:

```ts
import { events } from '@dotcms/events';

events.init({
    dotcmsUrl: process.env.NEXT_PUBLIC_DOTCMS_HOST!,
    siteAuth: process.env.NEXT_PUBLIC_DOTCMS_SITE_AUTH!,
    impressions: true,
    clicks: true
});
```

Every other module imports the same object, already configured:

```tsx
'use client';

import { events } from '@dotcms/events';

const onSubmit = () => events.conversion('contact-form');
```

- `conversion` takes a name only: dotCMS records a conversion's name and the page it happened on.
- The SDK sends only the event types dotCMS accepts: pageviews, conversions, content impressions and content clicks. There is no custom-event API, because dotCMS rejects any other type.
- `pageView(data)` sends a pageview with custom data, for apps that set `autoPageView: false`.
- Types come from the same entry: `import type { DotCMSEventsConfig, DotCMSEventsJsonObject } from '@dotcms/events'`.

After `init`, plain scripts reach the same object as `window.dotEvents`.

## Traditional Pages

dotCMS injects the SDK into traditional (VTL) pages as a script, `ca.min.js`, built from this package by `build:standalone`. The script reads its settings from its own tag, in the attributes dotCMS's template prints:

| Attribute                       | Sets                                                                                          |
| ------------------------------- | --------------------------------------------------------------------------------------------- |
| `data-analytics-auth`           | `siteAuth`. Without it the script does not start                                              |
| `data-analytics-debug`          | `debug`, with `"true"`                                                                        |
| `data-analytics-auto-page-view` | `autoPageView`: on unless `"false"`                                                            |
| `data-analytics-impressions`    | `impressions`, with `"true"`                                                                  |
| `data-analytics-clicks`         | `clicks`, with `"true"`                                                                       |
| `data-analytics-config`         | JSON with `queue`, `impressions`, `clicks`, `autoPageView`, `debug` or `logLevel`. An attribute with a value wins over it |

`dotcmsUrl` is the page's origin, since dotCMS serves both. Plain scripts on the page reach the SDK as `window.dotEvents`.

The script runs the page's experiments too, in place of dotCMS's own experiments script:

- The experiment comes from the `isExperimentPage` rule `isUserIncluded` returns, and the variant dotCMS rendered from the `data-dot-variant` it prints on each contentlet's wrapper.
- A returning visitor is decided while the head is parsed, so a visitor with another variant is sent to it before the page paints.
- On a new visitor's first page, the contentlets stay hidden until `isUserIncluded` answers, 3 s at most.

dotCMS prints those wrappers only when content impressions or clicks are on, until [#37798](https://github.com/dotCMS/core/issues/37798) (the backend changes that make this the only script) ships.

## Experiments

Wrap what the experiment varies in `DotCMSExperiment`:

```tsx
import { DotCMSExperiment } from '@dotcms/events/react';

<DotCMSExperiment page={pageAsset}>
    <DotCMSLayoutBody page={pageAsset} components={pageComponents} />
</DotCMSExperiment>
```

On a page that runs an experiment, the content stays hidden until the visitor's variant is decided. A visitor who already has an assignment gets it from a small inline script while the HTML loads, before the app's scripts: it shows the content, or sends the visitor to the assigned variant before the page has hydrated or requested its images. A new visitor's variant is decided by the SDK once it loads. On any other page, `DotCMSExperiment` renders its children alone.

- The content can be the page's layout or content a view renders itself: whatever the experiment's variants change.
- Rendered in a server component, it adds no JavaScript to the client. In a client component, it adds about 1.4 KB gzip.

Without React, print what `experimentMarkup` returns, in the HTML the server sends:

```ts
import { experimentMarkup } from '@dotcms/events/markup';

const markup = experimentMarkup(pageAsset); // null when no experiment runs
// <style>{markup.style}</style>
// <script>{markup.script}</script>
// <div {...markup.attributes}>…the content the experiment varies…</div>
```

Either way, an experiment page needs two more things:

- its route passes the URL's `variantName` to the page request, or the variant's URL shows the original;
- it renders on every request: in Next.js, reading `searchParams` does that. A page prerendered at build time cannot run an experiment.

With `debug: true` the SDK warns when an experiment runs on a page that prints neither, and when a route ignores `variantName`.

## Configuration

| Option         | Default     | What it does                                                                                                   |
| -------------- | ----------- | -------------------------------------------------------------------------------------------------------------- |
| `dotcmsUrl`    | required    | The dotCMS origin, the same value `createDotCMSClient` takes                                                   |
| `siteAuth`     | required    | The Site Auth from the Content Analytics app                                                                   |
| `autoPageView` | `true`      | A pageview on load and on every History change                                                                 |
| `experiments`  | `true`      | `isUserIncluded`, the pageview hold, the redirect and `context.experiments`; `{ timeout }` sets how long a new visitor's pageview waits for the assignment (3000 ms) |
| `impressions`  | `false`     | Content impressions; an object sets the threshold, dwell time and limits                                       |
| `clicks`       | `false`     | Content clicks                                                                                                 |
| `queue`        | batching on | `false` sends each event at once; an object sets the batch size and interval                                   |
| `debug`        | `false`     | Logs what the SDK does to the console                                                                          |
| `logLevel`     |             | Minimum level for the analytics core's logs                                                                    |
| `onError`      |             | Called when something fails where the page cannot see it (below)                                               |

## Errors

Nothing the SDK sends is retried, and nothing it fails at throws into the page. `onError` receives what failed, with a `code`:

| Code          | When                                                                                                 |
| ------------- | ---------------------------------------------------------------------------------------------------- |
| `REJECTED`    | dotCMS answered an events request with an error, or accepted it but failed some of its events: `status` and `detail` say why |
| `NETWORK`     | An events request got no answer                                                                      |
| `EXPERIMENTS` | `isUserIncluded` failed; a 403 means experiments are off for the site, and the SDK stops asking for a day |

An error that `onError` throws is ignored.

## What It Stores

No cookies. Everything carries the `dot_events_` prefix:

| Key                                | Storage        | Contents                                                        |
| ---------------------------------- | -------------- | --------------------------------------------------------------- |
| `dot_events_user_id`               | localStorage   | The visitor's id                                                |
| `dot_events_experiments`           | localStorage   | The visitor's experiment assignments                            |
| `dot_events_experiments_off_until` | localStorage   | Set when dotCMS answers 403: experiments stay off for a day     |
| `dot_events_session_id`            | sessionStorage | The session's id                                                |
| `dot_events_tab_id`                | sessionStorage | The tab's id                                                    |
| `dot_events_queue_<tab id>`        | sessionStorage | Events not sent yet, kept across a page navigation              |
| `dot_events_experiments_checked`   | sessionStorage | This tab already asked for the visitor's assignments            |
| `dot_events_session_experiments`   | sessionStorage | The experiments the session's events carry                      |
