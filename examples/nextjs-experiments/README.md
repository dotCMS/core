# dotCMS + Next.js: Experiments (A/B Testing) Example

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
2. **Set up `.env.local`** — `cp .env.local.example .env.local` and fill in the host, auth token, site ID, and the experiments key (see [Step 3: Configure the Next.js Application](#step-3-configure-the-nextjs-application)).
3. **Set up the UVE app** in dotCMS — point the Universal Visual Editor at `http://localhost:3000` (see [Configure the Universal Visual Editor](#c-configure-the-universal-visual-editor)).
4. **Install dependencies** — `npm install`.
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
- [Setup Guide](#setup-guide)
    - [Step 1: Create the Next.js Application](#step-1-create-the-nextjs-application)
    - [Step 2: Configure dotCMS Access](#step-2-configure-dotcms-access)
    - [Step 3: Configure the Next.js Application](#step-3-configure-the-nextjs-application)
    - [Step 4: Run the Application](#step-4-run-the-application)
- [Edit your page in the Universal Visual Editor](#edit-your-page-in-the-universal-visual-editor)
- [Advanced: Next.js + dotCMS Architecture](#advanced-nextjs--dotcms-architecture)
    - [Integration Overview](#integration-overview)
    - [File Structure](#file-structure)
    - [Understanding the Structure](#understanding-the-structure)
    - [How to Fetch Content from dotCMS](#how-to-fetch-content-from-dotcms)
    - [How to Render Your Page](#how-to-render-your-page)
- [Conclusion](#conclusion)
- [Learn More](#learn-more)

## Prerequisites

Before you begin, make sure you have:

### System Requirements

- **Node.js**: v18.20.8 (LTS) or later (v22+ recommended)
- **NPM**, **Yarn**, or **pnpm** package manager
- **Git** for version control
- A code editor (VS Code, WebStorm, etc.)

### dotCMS Requirements

Because this example exercises the **Experiments** feature, you need more than a plain dotCMS instance:

- **A running dotCMS instance with the full starter** — the starter site provides the demo pages and content the experiments run against.
- **The experiments (analytics) infrastructure up** — Keycloak, Jitsu, Cube, ClickHouse, and the analytics configurator, all wired to dotCMS.
- **The Experiments app configured** in dotCMS — analytics URLs and client credentials set under **Apps → dotExperiments-config**.
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
| [@dotcms/experiments](https://www.npmjs.com/package/@dotcms/experiments) | A/B Testing | A/B testing capabilities — the core package for this example |

### Installing @dotcms/experiments

`@dotcms/experiments` is already listed in this example's `package.json`, so a normal `npm install` (or `yarn`/`pnpm install`) will pull it in. If you are adding experiments to your own project, or the package is missing, install it explicitly:

```bash
# Using npm
npm install @dotcms/experiments
```

#### Verify the installation

Check that the package was added to your `package.json` under `dependencies`:

```json
{
    "dependencies": {
        "@dotcms/experiments": "^<version>"
    }
}
```

If the entry is present, the package installed correctly and you're ready to continue.

> [!NOTE]
> You can confirm the available SDK versions on the npm registry: [@dotcms/experiments versions](https://www.npmjs.com/package/@dotcms/experiments?activeTab=versions).

## Setup Guide

This guide will walk you through the process of setting up the dotCMS Next.js example from scratch.

This will create a new directory with the example code and install all necessary dependencies.

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
> Make your API Token had read-only permissions for Pages, Folders, Assets, and Content. Using a key with minimal permissions follows security best practices.

This integration requires an API Key with read-only permissions for security best practices:

1. Go to the **dotCMS admin panel**.
2. Click on **System** > **Users**.
3. Select the user you want to create the API Key for.
4. Go to **API Access Key** and generate a new key.

For detailed instructions, please refer to the [dotCMS API Documentation - Read-only token](https://dev.dotcms.com/docs/rest-api-authentication#ReadOnlyToken).

#### C. Configure the Universal Visual Editor

The [Universal Visual Editor (UVE)](https://dev.dotcms.com/docs/universal-visual-editor) is a critical feature that creates a bridge between your dotCMS instance and your Next.js application. This integration **Enables real-time visual editing** and allows content editors to see and modify your actual Next.js pages directly from within dotCMS.

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

### Step 3: Configure the Next.js Application

#### A. Set Environment Variables

Create a `.env.local` file in the root of the project by running the following command:

```bash
# This will create a new file with the correct variables.
cp .env.local.example .env.local
```

Then set each variable in the `.env.local` file:

- `NEXT_PUBLIC_DOTCMS_HOST`: The URL of your dotCMS site (e.g. `http://localhost:8080/`, or `http://localhost:8082/` if you use the Docker stack).
- `NEXT_PUBLIC_DOTCMS_AUTH_TOKEN`: The API Key you created in Step 2B.
- `NEXT_PUBLIC_DOTCMS_SITE_ID`: The site key of the site you want to use.
- `NEXT_PUBLIC_DOTCMS_MODE`: The runtime mode for the app (e.g. `production`).
- `NODE_TLS_REJECT_UNAUTHORIZED`: Set to `0` to allow self-signed certificates when connecting to a local dotCMS over HTTPS. **Local development only** — do not use in production.
- `NEXT_PUBLIC_DOTCMS_EXPERIMENTS_KEY`: The machine-to-machine (M2M) analytics key used to track and evaluate experiments. This is the key found under the **Experiments app** in dotCMS — see Step 2C below.
- `NEXT_PUBLIC_EXPERIMENTS_DEBUG` _(optional)_: Set to `true` to enable verbose experiments/analytics debug logging. Defaults to off. Consumed by the centralized `experimentsConfig` (see [Understanding the Structure](#understanding-the-structure)).

The site ID variable refers to the site that will be used to pull content into your Next.js app. dotCMS is a multi-site CMS, meaning a single instance can manage multiple websites; the site ID specifies which site's content should be pulled into your Next.js app. If left empty or given an incorrect value, content will be pulled from the default site configured in dotCMS.

You can find the values for this variable — site keys or identifiers both work, though keys are simpler and more recommended — under System > Sites. Learn more about [dotCMS Multi-Site management here](https://dev.dotcms.com/docs/multi-site-management#multi-site-management).

#### B. Get the Experiments Key

The `NEXT_PUBLIC_DOTCMS_EXPERIMENTS_KEY` is the analytics key that authorizes the Next.js app to report and evaluate experiment data. You can find it in dotCMS under **Settings → Apps → dotExperiments-config** (the Experiments app). If you used the [Experiments Docker Compose stack](../../docker/docker-compose-examples/experiments/README.md), this app is pre-configured and the key is auto-generated by the analytics configurator — just copy it from the app into your `.env.local`.

### Step 4: Run the Application

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

4. **Config (src/config/)**: The `dotcms.config.ts` module provides typed, centralized access to the dotCMS environment variables, so every other module reads configuration from one place instead of touching `process.env` directly. This module also exposes a dedicated, centralized **`experimentsConfig`** object that groups everything the Experiments feature needs — the dotCMS host (`server`), the analytics `apiKey` (from `NEXT_PUBLIC_DOTCMS_EXPERIMENTS_KEY`), and a `debug` flag — so experiments settings live in one typed place alongside the rest of the configuration. Each value falls back to an empty string / `false`, so both `server` and `apiKey` are always typed as `string` (required by the Experiments SDK) and a missing `apiKey` reads as falsy:

    ```ts
    // src/config/dotcms.config.ts
    export const experimentsConfig = {
        server: process.env.NEXT_PUBLIC_DOTCMS_HOST ?? "",
        apiKey: process.env.NEXT_PUBLIC_DOTCMS_EXPERIMENTS_KEY ?? "",
        debug: process.env.NEXT_PUBLIC_EXPERIMENTS_DEBUG === "true",
    };
    ```

    Because `apiKey` defaults to `""` when the env var is unset, code can guard on it (`experimentsConfig.apiKey ? withExperiments(...) : ...`) to enable experiments only when a key is configured.

5. **Lib (src/lib/)**: Contains the `dotCMSClient.ts`, which initializes the connection to your dotCMS instance.

6. **Types (src/types/)**: Shared TypeScript interfaces (`Blog`, `Destination`, `NavItem`, `ContentTypeProps`, and more) used across the app for type-safe content rendering.

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
    dotCMSSiteId,
} from "@/config/dotcms.config";

export const dotCMSClient = createDotCMSClient({
    dotcmsUrl: dotCMSHost,
    authToken: dotCMSAuthToken,
    siteId: dotCMSSiteId,
    logLevel: process.env.NODE_ENV === "development" ? "verbose" : "default",
    requestOptions: {
        // UVE needs fresh data so in-context edits are reflected immediately.
        cache: "no-cache",
    },
});
```

Learn more about the `@dotcms/client` package [here](https://www.npmjs.com/package/@dotcms/client/).

#### Enabling A/B Experiments with `withExperiments`

This is where this example differs from the plain content/UVE integration: the page's `DotCMSLayoutBody` is wrapped with the `withExperiments` higher-order component from `@dotcms/experiments`, so the page can serve experiment variants and report results.

In `src/views/Page.tsx`, the wrapping is done conditionally based on the centralized `experimentsConfig`. When an `apiKey` is configured, `DotCMSLayoutBody` is wrapped with experiments support; otherwise the plain component is used, so the app still works without experiments configured:

```tsx
"use client";

import { DotCMSLayoutBody, useEditableDotCMSPage } from "@dotcms/react";
import { withExperiments } from "@dotcms/experiments";
import { experimentsConfig } from "@/config/dotcms.config";
import { useRouter } from "next/navigation";

export function Page({ pageContent }: PageProps) {
    const { pageAsset, content = {} } = useEditableDotCMSPage(pageContent);
    const { replace } = useRouter();

    // Conditionally wrap with experiments if an apiKey is configured
    const DotCMSLayoutBodyComponent = experimentsConfig.apiKey
        ? withExperiments(DotCMSLayoutBody, {
              ...experimentsConfig,
              redirectFn: replace,
          })
        : DotCMSLayoutBody;

    return (
        <DotCMSLayoutBodyComponent
            page={pageAsset}
            components={pageComponents}
            mode={dotCMSMode}
        />
    );
}
```

> [!NOTE]
>
> - `withExperiments` takes the base `DotCMSLayoutBody` and the spread `experimentsConfig` (`server`, `apiKey`, `debug`), returning a component that transparently handles variant assignment and tracking.
> - `redirectFn: replace` (from Next.js's `useRouter`) lets experiments perform client-side redirects when a variant requires navigating to a different URL.
> - The `experimentsConfig.apiKey` guard means the wrapping only happens when `NEXT_PUBLIC_DOTCMS_EXPERIMENTS_KEY` is set — with no key, the page renders normally without experiments.
> - Render the resulting `DotCMSLayoutBodyComponent` (not the raw `DotCMSLayoutBody`) so the experiments wrapping actually takes effect.

## Experiments (headless)

This example integrates the dotCMS **Experiments** feature on top of the content/UVE patterns above, using `@dotcms/experiments` for A/B testing in the Next.js App Router. It requires the experiments infrastructure and configured Experiments app described in [dotCMS Requirements](#dotcms-requirements).

For additional references and the safe `withExperiments` pattern, see:

- [Experiments SDK README](../../core-web/libs/sdk/experiments/README.md)
- [Experiments Docker Compose stack](../../docker/docker-compose-examples/experiments/README.md) — one-command setup for dotCMS + analytics infrastructure
