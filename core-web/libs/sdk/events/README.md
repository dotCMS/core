# dotCMS Events SDK

The `@dotcms/events` SDK is the official dotCMS library for everything your pages report back to dotCMS: pageviews, conversions, content impressions, content clicks, and A/B testing experiments. You configure it once, and a single object, `dotEvents`, takes care of the rest.

## Overview

### When to Use It

- Measuring how visitors use your dotCMS-powered pages: which pages they visit, which content they see, and what they click
- Recording conversions when a visitor completes a goal, such as a sign-up or a download
- Running the A/B testing experiments you create in dotCMS on a headless site
- Replacing `@dotcms/analytics` and `@dotcms/experiments` with a single package

### Key Features

- **Automatic pageviews**: on the first load, on every client-side navigation, and when the visitor comes back to a page with the browser's Back and Forward buttons
- **Content impressions**: records which contentlets the visitor actually saw, and for how long they had to be on screen
- **Content clicks**: records clicks on the links and buttons inside your contentlets
- **Conversions**: one call when a visitor completes a goal
- **Experiments without flicker**: sends each visitor to their variant, and keeps the content hidden for a moment so nobody sees the wrong one
- **One setup**: one `init` call configures everything, and every module of your app uses the same object
- **No cookies**: the SDK stores anonymous ids in the browser's storage, never in cookies
- **Safe in the editor**: inside the dotCMS Universal Visual Editor (UVE), nothing is sent

## Table of Contents

- [Overview](#overview)
- [Installation](#installation)
- [Which SDK Version Should I Use?](#which-sdk-version-should-i-use)
- [Quick Start (Next.js)](#quick-start-nextjs)
    - [Example Project](#example-project)
- [How It Works](#how-it-works)
- [Configuration](#configuration)
- [Usage](#usage)
- [API Reference](#api-reference)
- [Migrating from @dotcms/analytics and @dotcms/experiments](#migrating-from-dotcmsanalytics-and-dotcmsexperiments)
- [Under the Hood](#under-the-hood)
- [Troubleshooting](#troubleshooting)
- [Support](#support)
- [Contributing](#contributing)
- [Licensing](#licensing)

## Installation

Install the package via npm:

```bash
npm install @dotcms/events
```

Or using Yarn:

```bash
yarn add @dotcms/events
```

Requirements:

- A dotCMS instance with the **Content Analytics** app configured for your site. You need its **Site Auth**.
- **Next.js 16 or later**, if you use Next.js.
- **React 18 or later**, for `DotCMSExperiment` (`@dotcms/events/react`).

The package has three entry points:

| Import                  | What it gives you                                                         | Use it in                                  |
| ----------------------- | ------------------------------------------------------------------------- | ------------------------------------------ |
| `@dotcms/events`        | `dotEvents`: starts the SDK, records conversions and sends pageviews      | Every app                                  |
| `@dotcms/events/react`  | `DotCMSExperiment`: runs a page's experiment on the content it wraps      | React and Next.js pages that run experiments |
| `@dotcms/events/markup` | `experimentMarkup`: what a server-rendered page prints to run its experiment | Other frameworks, rendered on the server |

## Which SDK Version Should I Use?

dotCMS SDKs are published in lockstep with dotCMS itself: every `@dotcms/*` package ships
at the **exact same version number** as the dotCMS release it was built for (e.g. dotCMS
`26.7.14-1` → `@dotcms/client@26.7.14-1`, `@dotcms/react@26.7.14-1`, and so on).

**Simple rule of thumb: use the SDK version that matches your dotCMS instance's version.**

You don't have to upgrade the SDK every time dotCMS releases a new version (or vice versa).
Most releases don't change anything the SDKs rely on, so an older SDK usually keeps working
fine against a newer dotCMS instance. Occasionally, though, a release does include a real
breaking change — and if your SDK is older than that point, it will stop working correctly.

You don't need to track this yourself: your dotCMS instance always knows the oldest SDK
version it still supports, and the SDK checks itself against it automatically. If you're
using an SDK that's too old, you'll see a clear warning in your console telling you to
upgrade.

**Recommendation:** pin your SDKs to the same version as your dotCMS instance, and only bump
them when you upgrade dotCMS — or when the console tells you to.

> **On an LTS release?** LTS releases don't currently get their own matching SDK version.
> Until that's addressed, use the SDK version published for the closest regular release at
> or before your LTS version.
>
> Want more background on how dotCMS releases and support windows work? See
> [Release & Support Lifecycle](https://dev.dotcms.com/docs/release-support-lifecycle).

## Quick Start (Next.js)

### 1. Add the environment variables

Add these to your `.env.local` file:

```bash
NEXT_PUBLIC_DOTCMS_HOST=https://your-dotcms-instance.com
NEXT_PUBLIC_DOTCMS_SITE_AUTH=your-site-auth
```

| Variable                       | Description                                                      |
| ------------------------------ | ---------------------------------------------------------------- |
| `NEXT_PUBLIC_DOTCMS_HOST`      | The URL of your dotCMS instance                                  |
| `NEXT_PUBLIC_DOTCMS_SITE_AUTH` | The Site Auth from the Content Analytics app in dotCMS           |

### 2. Start the SDK once

Create `src/instrumentation-client.ts`. Next.js runs this file once in the browser, before your app becomes interactive:

```ts
// src/instrumentation-client.ts
import { dotEvents } from '@dotcms/events';

dotEvents.init({
    dotcmsUrl: process.env.NEXT_PUBLIC_DOTCMS_HOST!,
    siteAuth: process.env.NEXT_PUBLIC_DOTCMS_SITE_AUTH!,
    impressions: true,
    clicks: true
});
```

That's all pageviews, impressions, clicks and experiments need. Impressions and clicks are optional; pageviews and experiments are on by default.

### 3. Record conversions

Import the same object wherever a visitor completes a goal, and record the conversion once it succeeds:

```tsx
'use client';

import { dotEvents } from '@dotcms/events';

export function SignupForm() {
    const onSubmit = async (event: React.FormEvent) => {
        event.preventDefault();
        await createAccount(); // your own code

        // Only after the goal is reached, never on the attempt
        dotEvents.conversion('signup');
    };

    return <form onSubmit={onSubmit}>{/* fields */}</form>;
}
```

### 4. Run experiments on your pages

Wrap the content your experiments change in `DotCMSExperiment`, and pass the URL's `variantName` to the page request:

```tsx
// src/app/[[...slug]]/page.tsx
import { DotCMSExperiment } from '@dotcms/events/react';

import { dotCMSClient } from '@/lib/dotCMSClient'; // your @dotcms/client instance

interface PageProps {
    params: Promise<{ slug?: string[] }>;
    searchParams: Promise<{ variantName?: string }>;
}

export default async function Page({ params, searchParams }: PageProps) {
    const { slug } = await params;
    const { variantName } = await searchParams;
    const path = slug?.length ? `/${slug.join('/')}` : '/';

    // variantName makes dotCMS render the visitor's variant
    const pageContent = await dotCMSClient.page.get(path, { variantName });

    return (
        <DotCMSExperiment page={pageContent.pageAsset}>
            <MyPage pageContent={pageContent} />
        </DotCMSExperiment>
    );
}
```

`MyPage` is your view of the page, for example one that renders it with `DotCMSLayoutBody` from `@dotcms/react`. On a page that runs no experiment, `DotCMSExperiment` renders its children as they are, so you can wrap every page the same way.

### Example Project

The [Next.js experiments example](https://github.com/dotCMS/core/tree/main/examples/nextjs-experiments) runs on this package. It shows:

- The `init` call in `instrumentation-client.ts`, with the configuration in one file
- Pageviews, impressions and clicks on every page
- Experiments on the home page and on a blog listing, including content that streams in
- A conversion from a contact form

## How It Works

### Pageviews

A pageview is sent when the page first loads, on every client-side navigation, and when the visitor returns to a page with the Back or Forward button. Each one carries the page's URL, title and referrer, the campaign (UTM) parameters, and the visitor's device and screen.

To send them yourself instead, set `autoPageView: false` and call [`dotEvents.pageView()`](#doteventspageviewdata).

### Content Impressions

With `impressions` on, the SDK records each contentlet the visitor actually sees: at least half of it in view, for at least 750 ms. Each contentlet counts once per page view, and nothing counts while the tab is hidden or the window is behind others.

Impressions and clicks need the contentlets to carry dotCMS's attributes (the `dotcms-contentlet` class and its `data-dot-*` attributes). The dotCMS React, Angular and Vue SDKs add them on their own while this SDK is active, so pages rendered with `DotCMSLayoutBody` need nothing more.

### Content Clicks

With `clicks` on, the SDK records clicks on the links and buttons inside your contentlets: which contentlet, the element's text and link, and where the contentlet sits on the page. Repeated clicks on the same contentlet within 300 ms count once.

### Conversions

A conversion records that a visitor reached a goal. dotCMS stores its name and the page it happened on. Record it only once the goal is reached: a completed purchase, a finished download, a created account, not the click that started it.

### Experiments

Experiments are on by default. When a visitor opens a page that runs an experiment:

1. The content the experiment changes stays hidden for a moment, so the visitor never sees the wrong variant.
2. The SDK asks dotCMS which variant the visitor gets, and remembers the answer in the browser.
3. If the page shows another variant, the SDK sends the visitor to theirs: the same URL with `?variantName=<variant>`.
4. Otherwise it shows the content.

A returning visitor is sent to their variant right away, before the page finishes loading. If dotCMS takes more than 3 seconds to answer a new visitor, the original content shows, and the assignment applies from the next page on.

Events sent from a page that runs an experiment the visitor is in carry the experiment and the visitor's variant, together with the other experiments the visitor joined during the same session. Events from other pages carry none.

### How Events Are Sent

Events are sent in batches: every 5 seconds, or as soon as 15 are waiting. When the visitor switches tabs, closes the tab or leaves the page, whatever is waiting goes out right away. Nothing is retried, and nothing the SDK does can throw an error into your page: failures go to [`onError`](#errors).

### Visitors and Sessions

Each visitor gets an anonymous id, kept in the browser's localStorage, and each visit a session id. A session ends after 30 minutes without activity, or at midnight (UTC). The SDK sets no cookies.

### In the Universal Visual Editor

Inside the dotCMS Universal Visual Editor, the SDK sends nothing, and experiment content shows as it is, so editors can work on the page. On the server, every call does nothing.

## Configuration

### Config Options

| Option         | Type                                     | Required | Default | Description                                                                 |
| -------------- | ---------------------------------------- | -------- | ------- | --------------------------------------------------------------------------- |
| `dotcmsUrl`    | `string`                                 | Yes      | —       | The URL of your dotCMS instance, the same value `createDotCMSClient` takes |
| `siteAuth`     | `string`                                 | Yes      | —       | The Site Auth from the Content Analytics app                                |
| `autoPageView` | `boolean`                                | No       | `true`  | Send pageviews automatically                                                |
| `experiments`  | `boolean \| DotCMSEventsExperimentsConfig` | No       | `true`  | Run experiments; `false` turns them off                                     |
| `impressions`  | `boolean \| DotCMSEventsImpressionsConfig` | No       | `false` | Record content impressions                                                  |
| `clicks`       | `boolean`                                | No       | `false` | Record content clicks                                                       |
| `queue`        | `boolean \| DotCMSEventsQueueConfig`       | No       | `true`  | Send events in batches; `false` sends each one at once                      |
| `debug`        | `boolean`                                | No       | `false` | Log what the SDK does to the browser console                                |
| `logLevel`     | `'debug' \| 'info' \| 'warn' \| 'error'`   | No       | —       | The least important level the console shows                                 |
| `onError`      | `(error: DotCMSEventsError) => void`     | No       | —       | Called when something fails that the page can't see                         |

### Impressions

`true` turns impressions on with the defaults. An object changes them:

| Option                | Default | Description                                                        |
| --------------------- | ------- | ------------------------------------------------------------------ |
| `visibilityThreshold` | `0.5`   | How much of the contentlet must be in view, from `0` to `1`        |
| `dwellMs`             | `750`   | How long, in milliseconds, it must stay in view                    |
| `maxNodes`            | `100`   | The most contentlets tracked on a page                             |

```ts
dotEvents.init({
    dotcmsUrl: process.env.NEXT_PUBLIC_DOTCMS_HOST!,
    siteAuth: process.env.NEXT_PUBLIC_DOTCMS_SITE_AUTH!,
    impressions: { visibilityThreshold: 0.7, dwellMs: 1000 }
});
```

### Experiments

| Option    | Default | Description                                                                                                                       |
| --------- | ------- | --------------------------------------------------------------------------------------------------------------------------------- |
| `timeout` | `3000`  | How long, in milliseconds, a new visitor's page waits for their variant before showing the original. 3000 is also the most: past it the content shows on its own |

### Queue

| Option           | Default | Description                                  |
| ---------------- | ------- | -------------------------------------------- |
| `eventBatchSize` | `15`    | The most events in one batch                 |
| `flushInterval`  | `5000`  | How often, in milliseconds, a batch is sent  |

### Errors

`onError` receives what failed, with a `code`:

| Code          | When                                                                                                         |
| ------------- | ------------------------------------------------------------------------------------------------------------ |
| `REJECTED`    | dotCMS refused an events request, or some of its events. `status` and `detail` say why                       |
| `NETWORK`     | An events request got no answer                                                                              |
| `EXPERIMENTS` | Asking dotCMS for the visitor's variant failed. A 403 means experiments are off for the site; the SDK stops asking for a day |

```ts
dotEvents.init({
    dotcmsUrl: process.env.NEXT_PUBLIC_DOTCMS_HOST!,
    siteAuth: process.env.NEXT_PUBLIC_DOTCMS_SITE_AUTH!,
    onError: (error) => console.warn(`dotCMS events: ${error.code}`, error.message)
});
```

## Usage

### Next.js App Router

Follow the [Quick Start](#quick-start-nextjs). An experiment page also needs to:

- **Pass `variantName` to the page request.** Without it, the variant's URL shows the original content.
- **Render on every request.** Reading `searchParams` does that. A page built ahead of time can't run an experiment.
- **Use `DotCMSExperiment` in a server component when it can.** It then adds no JavaScript to the browser; in a client component it adds about 1.5 KB.

Content that streams in, behind `<Suspense>` or `loading.tsx`, works too: the SDK decides it when it arrives. On a site with a nonce-based Content-Security-Policy, pass the nonce: `<DotCMSExperiment page={pageAsset} nonce={nonce}>`.

### Other React Apps

Call `init` once, where your app starts, before it renders. `DotCMSExperiment` works the same way.

### Other Frameworks

`dotEvents` works with any framework. To run experiments, print what `experimentMarkup` returns in the HTML your server sends:

```ts
import { experimentMarkup } from '@dotcms/events/markup';

const markup = experimentMarkup(pageAsset); // null when the page runs no experiment

// <style>{markup.style}</style>
// <script>{markup.script}</script>
// <div {...markup.attributes}>the content the experiment changes</div>
```

### Traditional Pages

Pages dotCMS renders itself (VTL) don't need this package: dotCMS adds the tracking to them on its own. You turn it on and configure it in the Content Analytics app.

### Plain Scripts

After `init`, any script on the page can reach the same object as `window.dotEvents`:

```html
<script>
    window.dotEvents?.conversion('newsletter');
</script>
```

### Sending Pageviews Yourself

With `autoPageView: false`, call `dotEvents.pageView()` on each page, optionally with your own data. Call it as well when the browser brings back a page the visitor returns to with the Back button, which it can do without loading it again:

```ts
dotEvents.pageView({ campaign: 'spring' });

window.addEventListener('pageshow', (event) => {
    if (event.persisted) {
        dotEvents.pageView();
    }
});
```

## API Reference

### `dotEvents.init(config)`

Starts the SDK with your [configuration](#config-options). Call it once, as early as possible. A second call with the same configuration does nothing; one with another configuration is ignored with a warning. Calls made before the SDK is ready wait and run once it is.

### `dotEvents.conversion(name)`

Records a conversion with its `name`. dotCMS records the name and the page; it accepts no other data.

### `dotEvents.pageView(data?)`

Sends a pageview, with optional custom data. Only needed with `autoPageView: false`.

### `DotCMSExperiment`

```tsx
import { DotCMSExperiment } from '@dotcms/events/react';
```

| Prop        | Type        | Required | Description                                                          |
| ----------- | ----------- | -------- | -------------------------------------------------------------------- |
| `page`      | page asset  | Yes      | The page asset, requested with the URL's `variantName`               |
| `children`  | `ReactNode` | Yes      | What the experiment changes: the page's layout, or part of it        |
| `nonce`     | `string`    | No       | The nonce of a nonce-based Content-Security-Policy                   |
| `className` | `string`    | No       | A class for the element that wraps the content                       |

### `experimentMarkup(page)`

```ts
import { experimentMarkup } from '@dotcms/events/markup';
```

Returns what a server-rendered page prints to run its experiment, or `null` when the page runs none: `style` and `script`, which go before the content, and `attributes`, for the element that wraps it.

### Types

Every type the options, methods and errors use comes from the main entry:

```ts
import type { DotCMSEventsConfig, DotCMSEventsError } from '@dotcms/events';
```

## Migrating from @dotcms/analytics and @dotcms/experiments

| Before                                                        | With `@dotcms/events`                                                           |
| ------------------------------------------------------------- | ------------------------------------------------------------------------------- |
| `<DotContentAnalytics config={...} />` in the root layout     | `dotEvents.init({...})` in `src/instrumentation-client.ts`                      |
| `server` in the analytics config                              | `dotcmsUrl`                                                                     |
| `useContentAnalytics(config).conversion(name, data)`          | `dotEvents.conversion(name)`: dotCMS records the name only                     |
| `useContentAnalytics(config).pageView(data)`                  | `dotEvents.pageView(data)`, with `autoPageView: false`                          |
| `useContentAnalytics(config).track(name, data)`               | Not available: dotCMS accepts pageviews, conversions, impressions and clicks only |
| `withExperiments(DotCMSLayoutBody, config)`                   | `<DotCMSExperiment page={pageAsset}>` around the content                        |
| A separate experiments config, with its own key               | The same `init` call: experiments are on by default                             |

Visitor and session ids are stored under new names (`dot_events_*`), so visitors start with new ids after the switch.

## Under the Hood

### What It Stores

No cookies. Everything carries the `dot_events_` prefix:

| Key                                | Storage        | Contents                                                     |
| ---------------------------------- | -------------- | ------------------------------------------------------------ |
| `dot_events_user_id`               | localStorage   | The visitor's anonymous id                                   |
| `dot_events_experiments`           | localStorage   | The visitor's experiment assignments                         |
| `dot_events_experiments_off_until` | localStorage   | Set when experiments are off for the site, for a day         |
| `dot_events_session_id`            | sessionStorage | The session's id                                             |
| `dot_events_tab_id`                | sessionStorage | The tab's id                                                 |
| `dot_events_queue_<tab id>`        | sessionStorage | Events not sent yet, kept until the next page sends them     |
| `dot_events_experiments_checked`   | sessionStorage | This tab already asked for the visitor's assignments         |
| `dot_events_session_experiments`   | sessionStorage | The experiments the visitor joined during the session        |

### Endpoints

| Request                                    | What for                                  |
| ------------------------------------------ | ----------------------------------------- |
| `POST {dotcmsUrl}/api/v1/analytics/content/event` | Every event                        |
| `POST {dotcmsUrl}/api/v1/experiments/isUserIncluded` | The visitor's experiment assignments |

## Troubleshooting

### Common Issues & Solutions

| Symptom                                           | Likely cause                                                                   | What to do                                                                                     |
| ------------------------------------------------- | ------------------------------------------------------------------------------ | ---------------------------------------------------------------------------------------------- |
| No events in the Network tab                      | Missing `dotcmsUrl` or `siteAuth`, or you're inside the Universal Visual Editor | Check both values, test outside the editor, and turn on `debug: true`                          |
| Environment variables are empty                   | Next.js only exposes variables that start with `NEXT_PUBLIC_` to the browser    | Rename them, and restart the dev server after changing `.env.local`                            |
| `onError` reports `REJECTED`                      | dotCMS refused the events                                                      | Check that `siteAuth` is the Site Auth of the same site, and read `detail`                     |
| No impressions                                    | `impressions` is off, the contentlets lack dotCMS's attributes, or the tab is hidden | Turn it on, render with a dotCMS SDK, and keep the page in view while testing             |
| No clicks                                         | `clicks` is off, or the element isn't a link or button inside a contentlet     | Turn it on, and click a link or button inside a contentlet                                     |
| The variant never shows                           | The route doesn't pass `variantName` to the page request, or the page is built ahead of time | Pass `variantName`, and render the page on every request                          |
| The page reloads once on a first visit            | Expected: the visitor is being sent to their variant                           | Nothing to do                                                                                  |
| The experiment content appears after 3 seconds    | dotCMS took longer than 3 seconds to answer                                    | The original shows; the visitor gets their variant from the next page on                       |
| Nothing about experiments in the Network tab      | The experiment isn't running in dotCMS, or experiments are off                 | Check the experiment's status in dotCMS, and look for `EXPERIMENTS` in `onError`               |

### Debugging Tips

1. **Turn on debug mode.** `debug: true` logs what the SDK does to the browser console, and warns about setup mistakes, such as a route that ignores `variantName`.
2. **Watch the Network tab.** Filter by `/api/v1/analytics/content/event` to see each batch of events, and by `isUserIncluded` to see the experiments check.
3. **Check the storage.** In DevTools > Application, look for the `dot_events_*` keys in localStorage and sessionStorage. Clearing them makes you a new visitor, with a new experiment assignment.

### Still Having Issues?

If you're still experiencing problems after trying these solutions:

1. Search existing [GitHub issues](https://github.com/dotCMS/core/issues)
2. Ask questions on the [community forum](https://community.dotcms.com/) to engage with other users.
3. Create a new issue with:
    - Detailed reproduction steps
    - Environment information
    - Error messages
    - Code samples

## Support

We offer multiple channels to get help with the dotCMS Events SDK:

- **GitHub Issues**: For bug reports and feature requests, please [open an issue](https://github.com/dotCMS/core/issues/new/choose) in the GitHub repository.
- **Community Forum**: Join our [community discussions](https://community.dotcms.com/) to ask questions and share solutions.
- **Stack Overflow**: Use the tag `dotcms-events` when posting questions.
- **Enterprise Support**: Enterprise customers can access premium support through the [dotCMS Support Portal](https://www.dotcms.com/support).

When reporting issues, please include:

- SDK version you're using
- Framework/library version (if applicable)
- Minimal reproduction steps
- Expected vs. actual behavior

## Contributing

GitHub pull requests are the preferred method to contribute code to dotCMS. We welcome contributions to the dotCMS Events SDK! If you'd like to contribute, please follow these steps:

1. Fork the repository [dotCMS/core](https://github.com/dotCMS/core)
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

Please ensure your code follows the existing style and includes appropriate tests.

## Licensing

dotCMS is available under either the [Business Source License 1.1 (BSL)](https://www.dotcms.com/bsl) or a commercial license.

Under the BSL, dotCMS can be used at no cost by individual developers, small businesses or agencies under $5M in total finances, and by larger organizations in non-production environments. Every BSL release automatically converts to GPL v3 four years after its release date. For full terms and FAQs, visit [dotcms.com/bsl](https://www.dotcms.com/bsl) and [dotcms.com/bsl-faq](https://www.dotcms.com/bsl-faq).

Production use in larger organizations, along with access to managed cloud, SLAs, support, and enterprise capabilities, is available under a commercial license from dotCMS. For details on commercial plans, features, and support options, see [dotcms.com/pricing](https://www.dotcms.com/pricing).
