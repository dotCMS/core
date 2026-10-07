# dotCMS + Next.js: Experiments (A/B Testing) Example

> [!NOTE]
> This example's `@dotcms/*` dependencies are pinned to `latest`, matching a dotCMS Evergreen
> instance (always the current release). If your dotCMS instance is **not** on Evergreen — an
> older self-hosted release, or an LTS server — installing as-is may fail with GraphQL
> `FieldUndefined` errors. Check your server's version and replace `latest` with that exact
> version for every `@dotcms/*` entry in `package.json` before installing.

## Introduction & Overview

This project demonstrates how to run **A/B Experiments** on a [Next.js](https://nextjs.org/) front end powered by [dotCMS](https://dotcms.com/) as a headless CMS. It builds on the standard fully-editable-page integration and adds the dotCMS **Experiments** feature, so you can:

- **Run A/B tests** on your pages and measure variant performance
- **Create content in dotCMS** and deliver it headlessly to your Next.js application
- **Edit content visually** using dotCMS's Universal Visual Editor (UVE) directly on your Next.js front end
- **Build high-performance pages** leveraging Next.js's server-side rendering capabilities
- **Maintain content separation** between your CMS and presentation layer

> [!IMPORTANT]
> Unlike the plain content/UVE example, the Experiments feature requires a running dotCMS **with the full starter** plus the **experiments (analytics) infrastructure** up and running, and the **Experiments app configured** in dotCMS. The easiest way to get all of this running is the ready-made Docker Compose stack — see [dotCMS Requirements](#dotcms-requirements) below.

## TL;DR

If you just want to see experiments working end to end, follow these steps (details for each are further down):

1. **Start dotCMS + experiments** via the [Docker Compose stack](../../docker/docker-compose-examples/experiments/README.md) — `./start-experiments.sh` from that directory. This brings up dotCMS with the full starter, the analytics infrastructure, and **configures the Experiments app by default**.
2. **Set up `.env.local`** — `cp .env.local.example .env.local` and fill in the host, auth token, site ID, and the Site Auth from the Content Analytics app (see [Step 2: Configure the Next.js Application](#step-2-configure-the-nextjs-application)).
3. **Set up the UVE app** in dotCMS — point the Universal Visual Editor at `http://localhost:3000` (see [Configure the Universal Visual Editor](#c-configure-the-universal-visual-editor)).
4. **Install dependencies** — `npm install`. Until `@dotcms/events` is on npm, install it from its local build instead (see [Installing @dotcms/events](#installing-dotcmsevents)).
5. **Run the dev server** — `npm run dev`.
6. **Open the Home page in the UVE** in dotCMS.
7. **Create a new experiment** on that page with one or more variants and start it.
8. **Open `http://localhost:3000` in a different browser** (or an incognito/private window) — you should see the experiment serving the variants you created.

### How It Works

```
┌───────────────┐      ┌───────────────┐      ┌───────────────┐
│               │      │               │      │               │
│    dotCMS     │──────▶   Next.js     │──────▶   Browser     │
│  (Content)    │      │  (Front end)  │      │  (Viewing)    │
│               │      │               │      │               │
└───────────────┘      └───────────────┘      └───────────────┘
        ▲                      │                      │
        │                      │                      │
        └──────────────────────┴──────────────────────┘
                Universal Visual Editor (UVE)
                   (Content Editing)
```

The integration uses dotCMS APIs to fetch content and the Universal Visual Editor to enable in-context editing directly on your Next.js pages.

## Table of Contents

- [Prerequisites](#prerequisites)
    - [System Requirements](#system-requirements)
    - [dotCMS Requirements](#dotcms-requirements)
    - [Knowledge Prerequisites](#knowledge-prerequisites)
- [dotCMS SDK Dependencies](#dotcms-sdk-dependencies)
    - [Installing @dotcms/events](#installing-dotcmsevents)
- [Setup Guide](#setup-guide)
    - [Step 1: Configure dotCMS Access](#step-1-configure-dotcms-access)
    - [Step 2: Configure the Next.js Application](#step-2-configure-the-nextjs-application)
    - [Step 3: Run the Application](#step-3-run-the-application)
- [Edit your page in the Universal Visual Editor](#edit-your-page-in-the-universal-visual-editor)
    - [Understanding the Structure](#understanding-the-structure)
    - [How the Content is Fetched from dotCMS](#how-the-content-is-fetched-from-dotcms)
        - [The getDotCMSPage utility and serving a specific variant](#the-getdotcmspage-utility-and-serving-a-specific-variant)
        - [Running experiments with DotCMSExperiment](#running-experiments-with-dotcmsexperiment)
- [Experiments (headless)](#experiments-headless)

## Prerequisites

Before you begin, make sure you have:

### System Requirements

- **Node.js**: v20.9 or later, which Next.js 16 requires (v22+ recommended)
- **NPM**, **Yarn**, or **pnpm** package manager
- **Git** for version control
- A code editor (VS Code, WebStorm, etc.)

### dotCMS Requirements

Because this example exercises the **Experiments** feature, you need more than a plain dotCMS instance:

- **A running dotCMS instance with the full starter** — the starter site provides the demo pages and content the experiments run against.
- **The experiments (analytics) infrastructure up** — Keycloak, Jitsu, Cube, ClickHouse, and the analytics configurator, all wired to dotCMS.
- **The Experiments app configured** in dotCMS — analytics URLs and client credentials set under **Apps → dotExperiments-config**.
- **The Content Analytics app configured** for the site — its **Site Auth** goes with every event the SDK sends (see [Step 2B](#b-get-the-site-auth)).
- **Administrator access** to create API tokens and configure the Universal Visual Editor.
- **API token** with appropriate read permissions for your Next.js app.

> [!TIP]
> Standing all of this up by hand is tedious. Use the ready-made Docker Compose stack at [`docker/docker-compose-examples/experiments`](../../docker/docker-compose-examples/experiments) — its [README](../../docker/docker-compose-examples/experiments/README.md) spins up dotCMS (with the full starter), the entire analytics stack, and pre-configures the Experiments app for you.
>
> Quick start (from that directory):
>
> ```bash
> # Full stack: dotCMS + analytics infrastructure, pre-configured
> ./start-experiments.sh
> ```
>
> Once it's up, dotCMS is available at `http://localhost:8082` (admin@dotcms.com / admin) with experiments already configured.

### Knowledge Prerequisites

- Basic understanding of React and Next.js concepts
- Familiarity with content management systems (prior dotCMS experience helpful but not required)

## dotCMS SDK Dependencies

> [!NOTE]
> These packages are already included in the example project's dependencies, so you don't need to install them separately.

This example uses the following npm packages from dotCMS:

| Package                                                         | Purpose           | Description                                             |
| --------------------------------------------------------------- | ----------------- | ------------------------------------------------------- |
| [@dotcms/client](https://www.npmjs.com/package/@dotcms/client/) | API Communication | Core API client for fetching content from dotCMS        |
| [@dotcms/react](https://www.npmjs.com/package/@dotcms/react/)   | UI Components     | React components and hooks for rendering dotCMS content |
| [@dotcms/uve](https://www.npmjs.com/package/@dotcms/uve)        | Visual Editing    | Universal Visual Editor integration                     |
| [@dotcms/types](https://www.npmjs.com/package/@dotcms/types)    | Type Safety       | TypeScript type definitions for dotCMS                  |
| [@dotcms/events](../../core-web/libs/sdk/events/README.md)    | Events and A/B Testing | Pageviews, conversions, content impressions and clicks, and experiments — the core package for this example |

### Installing @dotcms/events

`@dotcms/events` is listed in this example's `package.json`, so `npm install` pulls it in with the other SDKs. To add it to your own project:

```bash
npm install @dotcms/events
```

> [!NOTE]
> `@dotcms/events` is not on npm yet, so `npm install` fails on it. Until it is published, build it from `core-web` and install the local build, which leaves `package.json` and `package-lock.json` untouched:
>
> ```bash
> # From core-web
> pnpm nx build sdk-events
>
> # Then, from this example
> npm install --no-save --install-links --legacy-peer-deps ../../core-web/dist/libs/sdk/events
> ```
>
> `--install-links` copies the build into `node_modules`, so it resolves React from this app, and `--legacy-peer-deps` skips the check of its peer versions, which the release sets: the local build still says `0.0.0`. Run the same command again after rebuilding.

## Setup Guide

This guide walks you through configuring dotCMS access, wiring up the Next.js application, and running it against your experiments-enabled dotCMS instance.

### Step 1: Configure dotCMS Access

#### A. Get a dotCMS Site

First, get a [dotCMS Site](https://www.dotcms.com/pricing). If you want to test this example, you can also use our [demo site](https://demo.dotcms.com/dotAdmin/).

If using the demo site, you can log in with these credentials:

| User Name        | Password |
| ---------------- | -------- |
| admin@dotcms.com | admin    |

Once you have a site, you can log in with the credentials and start creating content.

#### B. Create a dotCMS API Key

> [!TIP]
> Make sure your API Token has read-only permissions for Pages, Folders, Assets, and Content. Using a key with minimal permissions follows security best practices.

This integration requires an API Key with read-only permissions for security best practices:

1. Go to the **dotCMS admin panel**.
2. Click on **System** > **Users**.
3. Select the user you want to create the API Key for.
4. Go to **API Access Key** and generate a new key.

For detailed instructions, please refer to the [dotCMS API Documentation - Read-only token](https://dev.dotcms.com/docs/rest-api-authentication#ReadOnlyToken).

#### C. Configure the Universal Visual Editor

The [Universal Visual Editor (UVE)](https://dev.dotcms.com/docs/universal-visual-editor) is a critical feature that creates a bridge between your dotCMS instance and your Next.js application. It **enables real-time visual editing** and allows content editors to see and modify your actual Next.js pages directly from within dotCMS.

To set up the Universal Visual Editor:

1. Browse to **Settings** > **Apps**.
2. Select the built-in integration for UVE - Universal Visual Editor.
3. Select the site that will be feeding the destination pages.
4. Add the following configuration:

```json
{
    "config": [
        {
            "pattern": "(.*)",
            "url": "http://localhost:3000"
        }
    ]
}
```

For detailed instructions, see the [dotCMS UVE Headless Configuration](https://dev.dotcms.com/docs/uve-headless-config).

This configuration tells dotCMS that when editors are working on content in the admin panel, they should see your Next.js application running at `http://localhost:3000`. The pattern `(.*)` means this applies to all pages in your site.

### Step 2: Configure the Next.js Application

#### A. Set Environment Variables

Create a `.env.local` file in the root of the project by running the following command:

```bash
# This will create a new file with the correct variables.
cp .env.local.example .env.local
```

Then set each variable in the `.env.local` file:

- `NEXT_PUBLIC_DOTCMS_HOST`: The URL of your dotCMS site (e.g. `http://localhost:8080/`, or `http://localhost:8082/` if you use the Docker stack).
- `NEXT_PUBLIC_DOTCMS_AUTH_TOKEN`: The API Key you created in Step 1B.
- `NEXT_PUBLIC_DOTCMS_SITE_ID`: The site key of the site you want to use.
- `NEXT_PUBLIC_DOTCMS_MODE`: The runtime mode for the app (e.g. `production`).
- `NODE_TLS_REJECT_UNAUTHORIZED`: Set to `0` to allow self-signed certificates when connecting to a local dotCMS over HTTPS. **Local development only** — do not use in production.
- `NEXT_PUBLIC_DOTCMS_SITE_AUTH`: The Site Auth from the **Content Analytics app**, which the SDK sends with every event — see Step 2B below.
- `NEXT_PUBLIC_EVENTS_DEBUG` _(optional)_: Set to `true` to log what the SDK does: the events it sends and how it decides each experiment. `.env.local.example` turns it on. Read by `eventsConfig` (see [Understanding the Structure](#understanding-the-structure)).

The site ID variable refers to the site that will be used to pull content into your Next.js app. dotCMS is a multi-site CMS, meaning a single instance can manage multiple websites; the site ID specifies which site's content should be pulled into your Next.js app. If left empty or given an incorrect value, content will be pulled from the default site configured in dotCMS.

You can find the values for this variable — site keys or identifiers both work, though keys are simpler and more recommended — under System > Sites. Learn more about [dotCMS Multi-Site management here](https://dev.dotcms.com/docs/multi-site-management#multi-site-management).

#### B. Get the Site Auth

`NEXT_PUBLIC_DOTCMS_SITE_AUTH` identifies the site in every event the SDK sends to dotCMS. In dotCMS, go to **Settings → Apps → Content Analytics**, open the configuration of the site set in `NEXT_PUBLIC_DOTCMS_SITE_ID`, and copy its **Site Auth** into your `.env.local`; if it is empty, **Auto Generate Site Auth** creates one. Asking dotCMS which variant a visitor gets needs no key.

### Step 3: Run the Application

Run the development server with one of the following commands:

```bash
# Using npm
npm run dev
```

You should see a message in your terminal indicating that the Next.js app is running at `http://localhost:3000`. Open this URL in your browser to see your dotCMS-powered Next.js site.

## Edit your page in the Universal Visual Editor

After setting up the Universal Visual Editor and running your Next.js application, you can edit your page in the Universal Visual Editor:

1. Log in to the dotCMS admin panel
2. Browse to Site > Pages
3. Open the page you want to edit
4. The page will be rendered in the editor with your Next.js front end
5. Make changes directly on the page

Learn more about the Universal Visual Editor [here](https://dev.dotcms.com/docs/universal-visual-editor).

### Understanding the Structure

This project is written in **TypeScript** (strict mode, with the `@/*` path alias configured in `tsconfig.json`) and uses Next.js with App Router for server-side rendering, with some important architectural decisions:

1. **App Router (src/app/)**: Contains all server-side rendered pages and routes. These components don't use React hooks directly due to Next.js 13+ restrictions. Learn more about the App Router [here](https://nextjs.org/docs/app).

2. **Components (src/components/)**:
    - The `content-types/` folder contains React components that render dotCMS content.
    - In dotCMS, a "Content Type" is like a data model (e.g., "Product", "BlogPost"), while a "Contentlet" is an actual content instance.
    - For each Content Type in dotCMS, you need a corresponding React component to render it.
    - For example, if you have a `MyCustomContent` content type in dotCMS, you would create a matching component in this folder to render it.

3. **Views (src/views/)**: Contains client-side page templates (`Page.tsx`, `DetailPage.tsx`, `BlogListingPage.tsx`) that can use React hooks. Since Next.js App Router components can't directly use hooks, these components handle client-side logic.

4. **Config (src/config/)**: The `dotcms.config.ts` module provides typed, centralized access to the dotCMS environment variables, so every other module reads configuration from one place instead of touching `process.env` directly. It also exports **`eventsConfig`**, the `DotCMSEventsConfig` that `dotEvents.init()` receives: the dotCMS host (`dotcmsUrl`), the Site Auth (`siteAuth`, from `NEXT_PUBLIC_DOTCMS_SITE_AUTH`), content impressions and clicks, which are opt-in and this example turns on, and a `debug` flag. Experiments and automatic pageviews are on by default, so they need no setting:

    ```ts
    // src/config/dotcms.config.ts
    export const eventsConfig: DotCMSEventsConfig = {
        dotcmsUrl: process.env.NEXT_PUBLIC_DOTCMS_HOST ?? "",
        siteAuth: process.env.NEXT_PUBLIC_DOTCMS_SITE_AUTH ?? "",
        impressions: true,
        clicks: true,
        debug: process.env.NEXT_PUBLIC_EVENTS_DEBUG === "true",
    };
    ```

5. **Instrumentation (src/instrumentation-client.ts)**: Next.js runs this file in the browser after the HTML loads and before React hydrates, the earliest place to start the SDK. It calls `dotEvents.init(eventsConfig)` once, which covers pageviews, impressions, clicks, conversions and experiments.

6. **Lib (src/lib/)**: Contains the `dotCMSClient.ts`, which initializes the connection to your dotCMS instance.

7. **Types (src/types/)**: Shared TypeScript interfaces (`Blog`, `Destination`, `NavItem`, `ContentTypeProps`, and more) used across the app for type-safe content rendering.

### How the Content is Fetched from dotCMS

Content in this integration is fetched using the `@dotcms/client` package, which provides a streamlined way to communicate with the dotCMS API. This client handles authentication, request formatting, and response parsing automatically.

The process works as follows:

1. First, we create a configured client instance in `src/lib/dotCMSClient.ts`
2. This client reads its configuration from `src/config/dotcms.config.ts`, which centralizes typed access to the environment variables
3. When a page is requested, we use this client to fetch the page data along with its content
4. All API requests are managed through this central client for consistency

Here's how the client is configured:

```ts
import { createDotCMSClient } from "@dotcms/client";

import {
    dotCMSAuthToken,
    dotCMSHost,
    dotCMSLogLevel,
    dotCMSSiteId,
} from "@/config/dotcms.config";

export const dotCMSClient = createDotCMSClient({
    dotcmsUrl: dotCMSHost,
    authToken: dotCMSAuthToken,
    siteId: dotCMSSiteId,
    logLevel: dotCMSLogLevel,
    requestOptions: {
        // UVE needs fresh data so in-context edits are reflected immediately.
        cache: "no-cache",
    },
});
```

Learn more about the `@dotcms/client` package [here](https://www.npmjs.com/package/@dotcms/client/).

#### The `getDotCMSPage` utility and serving a specific variant

Page fetching is centralized in `src/utils/getDotCMSPage.ts`. The utility is wrapped in React's `cache()` so that multiple callers within a single request — for example `generateMetadata` and the page body in `src/app/[[...slug]]/page.tsx` — share a single network round-trip. On failure it returns `{ error }` so callers can branch with the guards in `@/utils/pageResponse` instead of using `try/catch`.

It accepts an optional `variantName` that is forwarded to the dotCMS page API, so the page can be rendered as a specific experiment variant:

```ts
// src/utils/getDotCMSPage.ts
export const getDotCMSPage = cache(async (path: string, variantName?: string) => {
    try {
        return await dotCMSClient.page.get<{ content: PageExtraContent }>(path, {
            variantName,
            graphql: {
                /* blogs, destinations, navigation … */
            },
        });
    } catch (error) {
        return { error };
    }
});
```

The catch-all route reads `variantName` from the request's query string and passes it through. This means you can force a given variant by appending `?variantName=<variant>` to any URL (for example `http://localhost:3000/?variantName=my-variant`), which is useful for previewing and debugging individual experiment variants:

```tsx
// src/app/[[...slug]]/page.tsx
interface SlugPageProps {
    params: Promise<{ slug?: string[] }>;
    searchParams: Promise<{ variantName?: string }>;
}

export default async function Home({ params, searchParams }: SlugPageProps) {
    const { slug } = await params;
    const { variantName } = await searchParams;
    const pageContent = await getDotCMSPage(getPath(slug), variantName);
    // …
}
```

> [!NOTE]
> `generateMetadata` reads the same `searchParams.variantName` and forwards it to `getDotCMSPage` too, so the page's metadata (e.g. its title) matches the variant being rendered. Because both calls share the cached fetch, passing the same `variantName` costs only one request.

#### Running experiments with `DotCMSExperiment`

This is where this example differs from the plain content/UVE integration. Experiments take two pieces from `@dotcms/events`: `dotEvents.init()` in `src/instrumentation-client.ts` (see [Understanding the Structure](#understanding-the-structure)), and `DotCMSExperiment` around what an experiment varies. The catch-all route wraps the whole page in it, in the server component:

```tsx
// src/app/[[...slug]]/page.tsx
import { DotCMSExperiment } from "@dotcms/events/react";

export default async function Home({ params, searchParams }: SlugPageProps) {
    // … reads variantName and fetches the page, as shown above

    // Rendered here, in a server component, the experiment's markup costs the client no JavaScript
    return (
        <DotCMSExperiment page={pageContent.pageAsset}>
            <Page pageContent={pageContent} />
        </DotCMSExperiment>
    );
}
```

> [!NOTE]
>
> - `DotCMSExperiment` takes only the page. On a page that runs an experiment, it wraps its children in an element marked with the experiment and the variant the server rendered, keeps that element hidden, and prints a small script that sends a returning visitor to their variant while the HTML is parsed, before the page paints. On any other page it renders its children as they are, so there is nothing to turn off when no experiment runs.
> - The SDK decides the variant. A new visitor's content stays hidden until dotCMS answers which variant they get, 3 seconds at most. A visitor assigned another variant is sent to the same URL with `?variantName=<variant>`, which the route passes to `getDotCMSPage` (see the previous section): a route that drops it renders the original, and the visit is not counted.
> - There is no redirect function to pass: the SDK leaves the page for the variant with `location.replace`.
> - Inside the UVE editor the content shows as rendered, and the SDK sends nothing.
> - `src/components/forms/ContactUs.tsx` sends a conversion, `dotEvents.conversion("contact-form")`, when the form is submitted. Every event, conversions included, carries the session's experiments in `context.experiments`.

## Experiments (headless)

This example integrates the dotCMS **Experiments** feature on top of the content/UVE patterns above, using `@dotcms/events`: `dotEvents.init` in `src/instrumentation-client.ts`, and `DotCMSExperiment` around what an experiment varies. It requires the experiments infrastructure and configured Experiments app described in [dotCMS Requirements](#dotcms-requirements).

The routes render in two ways, so an experiment on either shows how each behaves:

- **The catch-all route (`[[...slug]]`) renders in one piece.** The server answers once dotCMS has, so the experiment's markup arrives with the first HTML, and a returning visitor is sent to their variant while the HTML is parsed, before the page paints.
- **The blog (`/blog`) streams.** Its `loading.tsx`, the same as wrapping the page in `<Suspense>` yourself, makes the page answer at once with a fallback, and the blog, with its experiment's markup, arrives when dotCMS answers. A returning visitor is sent to their variant when that part arrives, after the fallback has painted, and the SDK decides the marks as they stream in. A page that reads `searchParams` with `cacheComponents` on streams this way.

For additional references, see:

- [Events SDK README](../../core-web/libs/sdk/events/README.md)
- [Experiments Docker Compose stack](../../docker/docker-compose-examples/experiments/README.md) — one-command setup for dotCMS + analytics infrastructure
