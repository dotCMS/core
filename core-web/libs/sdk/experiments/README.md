# dotCMS Experiments SDK

The `@dotcms/experiments` SDK is the official dotCMS JavaScript library that helps add A/B testing to your web applications. It handles user assignments to different variants of a page and tracks their interactions.

## Overview

### When to Use It

-   Adding A/B testing capabilities to your web application
-   Running experiments to optimize user experience
-   Testing different page variants with real users
-   Tracking experiment performance with DotCMS Analytics

### Key Features

-   **User Assignment to Experiments**: Automatically assigns users to different experimental variants, ensuring diverse user experiences and reliable test data
-   **Link Verification for Redirection**: Checks links to ensure users are redirected to their assigned experiment variant, maintaining the integrity of the testing process
-   **Automatic PageView Event Sending**: Automatically sends PageView events to DotCMS Analytics, enabling real-time tracking of user engagement and experiment effectiveness

## Table of Contents

-   [Overview](#overview)
-   [Installation](#installation)
-   [Getting Started](#getting-started)
    -   [Public API](#public-api)
    -   [withExperiments HOC](#withexperiments-higher-order-component)
    -   [Configuration Object](#configuration-object)
-   [Usage](#usage)
    -   [With @dotcms/react (Recommended)](#with-dotcmsreact-recommended)
    -   [Next.js App Router](#nextjs-app-router)
    -   [Configuration Best Practices](#configuration-best-practices)
    -   [Anti-patterns](#anti-patterns)
    -   [How It Works](#how-it-works)
-   [Troubleshooting](#troubleshooting)
-   [Support](#support)
-   [Contributing](#contributing)
-   [Licensing](#licensing)

## Installation

Install the package via npm:

```bash
npm install @dotcms/experiments
```

Or using Yarn:

```bash
yarn add @dotcms/experiments
```

Align `@dotcms/experiments` with the same version line as your other `@dotcms/*` packages (for example `^1.5.6`).

## Getting Started

### Public API

The package exports **one** public symbol:

| Export | Description |
| --- | --- |
| `withExperiments` | HOC that wraps a layout component (typically `DotCMSLayoutBody`) with experiment handling |

`DotExperimentsProvider` and other internals are **not** part of the public API. Do not import them — `withExperiments` wires the provider internally.

### `withExperiments` Higher-Order Component

`withExperiments` wraps your page component with experiment functionality:

-   User assignment to experiment variants
-   Automatic redirection to the correct variant
-   Prevention of content flickering during variant loading
-   Automatic page view tracking to dotCMS Analytics
-   Click event handling to maintain variant consistency across navigation

> **Important:** `withExperiments` uses React hooks internally. It must be called **unconditionally** on every render of the component that uses it. See [Anti-patterns](#anti-patterns).

### Configuration Object

The `config` object passed to `withExperiments` accepts the following properties:

| Property | Required | Default | Description |
| --- | --- | --- | --- |
| `apiKey` | Yes | — | jsKey from the dotCMS Analytics / Experiments app (frontend key, not m2m) |
| `server` | Yes | — | dotCMS **origin** URL (e.g. `https://your-dotcms-instance.com`) — not a CDN |
| `redirectFn` | No | `window.location.replace` | SPA redirect function (e.g. `router.replace` in Next.js) |
| `trackPageView` | No | `true` | Enable/disable automatic page view tracking |
| `debug` | No | `false` | Verbose logging in the browser console |

## Usage

### With @dotcms/react (Recommended)

Use **separate views or routes** for pages with and without experiments. This avoids calling `withExperiments` conditionally in the same component.

```tsx
// views/StandardPage.tsx — no experiments
"use client";

import { DotCMSLayoutBody, useEditableDotCMSPage } from "@dotcms/react";

import { pageComponents } from "@/components/content-types";

export function StandardPage({ pageContent }) {
    const { pageAsset } = useEditableDotCMSPage(pageContent);

    return (
        <main>
            <DotCMSLayoutBody page={pageAsset} components={pageComponents} />
        </main>
    );
}
```

```tsx
// views/ExperimentsPage.tsx — experiments always enabled on this view
"use client";

import { withExperiments } from "@dotcms/experiments";
import { DotCMSLayoutBody, useEditableDotCMSPage } from "@dotcms/react";
import { useRouter } from "next/navigation";

import { pageComponents } from "@/components/content-types";

export function ExperimentsPage({ pageContent }) {
    const { pageAsset } = useEditableDotCMSPage(pageContent);
    const { replace } = useRouter();

    const LayoutBody = withExperiments(DotCMSLayoutBody, {
        apiKey: process.env.NEXT_PUBLIC_DOTCMS_ANALYTICS_KEY!,
        server: process.env.NEXT_PUBLIC_DOTCMS_HOST!,
        redirectFn: replace,
        debug: process.env.NEXT_PUBLIC_DOTCMS_DEBUG === "true",
    });

    return (
        <main>
            <LayoutBody page={pageAsset} components={pageComponents} />
        </main>
    );
}
```

Wire each view to its own route in the App Router (for example `/blog` for experiments, catch-all for standard pages).

> **Learn more about `DotCMSLayoutBody`**: See the [@dotcms/react SDK documentation](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/react/README.md#dotcmslayoutbody).

### Next.js App Router

Canonical working example: [`examples/nextjs-analytics-experiments`](../../../../examples/nextjs-analytics-experiments)

That project demonstrates:

-   Analytics on all routes via `@dotcms/analytics` in `layout.tsx`
-   Experiments isolated to `/blog` via `withExperiments` in a dedicated view
-   Safe separation that avoids rules-of-hooks violations in UVE

**Environment variables** (see the example's `.env.local.example`):

```bash
NEXT_PUBLIC_DOTCMS_HOST=http://localhost:8080      # dotCMS origin — NOT a CDN
NEXT_PUBLIC_DOTCMS_ANALYTICS_KEY=                  # jsKey (frontend key)
NEXT_PUBLIC_DOTCMS_DEBUG=true
```

**Verification** (DevTools → Network):

1. On experiment routes: `POST {HOST}/api/v1/experiments/isUserIncluded`
2. With `debug: true`: `[dotCMS ...]` logs in the console
3. Open the page in UVE — no "client-side exception" overlay

### Configuration Best Practices

-   **Use environment variables** for `apiKey` and `server`
-   **Dedicated routes/views** for experiment pages — do not toggle `withExperiments` with a ternary on `apiKey` in the same component
-   **Framework router**: pass `router.replace` (Next.js) or equivalent to `redirectFn` for SPA navigation
-   **Origin URL**: `server` must point to the dotCMS instance origin. Do not use Azure Front Door, CloudFront, or other CDNs — experiment API responses are session-specific and must not be cached at the edge
-   **Aligned SDK versions**: keep `@dotcms/client`, `@dotcms/react`, `@dotcms/uve`, `@dotcms/types`, and `@dotcms/experiments` on the same version line
-   **Debug mode**: enable during development to troubleshoot assignment issues

### Anti-patterns

Do **not** use these patterns — they can crash Next.js App Router pages and the UVE editor:

```tsx
// ❌ Conditional withExperiments — violates rules of hooks
const Layout = experimentConfig.apiKey
    ? withExperiments(DotCMSLayoutBody, config)
    : DotCMSLayoutBody;

// ❌ Early return before withExperiments in the same component
if (!apiKey) return <DotCMSLayoutBody {...props} />;
const Layout = withExperiments(DotCMSLayoutBody, config);

// ❌ Importing internal provider
import { DotExperimentsProvider } from "@dotcms/experiments";

// ❌ CDN as server
server: "https://cdn.example.com"
```

Use separate components/routes instead (see [With @dotcms/react](#with-dotcmsreact-recommended)).

### How It Works

Once you wrap your component with `withExperiments`, the SDK automatically handles:

1. **User Assignment**: Assigns users to experiment variants when they visit a page with an active experiment
2. **Automatic Redirection**: Redirects users to their assigned variant URL (prevents seeing the wrong variant)
3. **Flicker Prevention**: Hides content during redirection to avoid showing the wrong variant momentarily
4. **Navigation Handling**: Maintains variant consistency when users click links
5. **Analytics Tracking**: Sends pageview events to DotCMS Analytics automatically

**Learn More**: [DotCMS A/B Testing Experiments](https://www.dotcms.com/product/ab-testing-experiments)

## Troubleshooting

| Symptom | Likely cause | What to do |
| --- | --- | --- |
| UVE crash: "A client-side exception has occurred" | `withExperiments` called conditionally, or experiments initializing before UVE state is ready | Use a dedicated experiments view; never use `apiKey ? withExperiments(...) : DotCMSLayoutBody` |
| `GET /api/v1/experiments/DEFAULT` 404 | Known backoffice issue — variant name `"DEFAULT"` passed as experiment ID | Harmless in headless apps; backoffice fix tracked separately |
| "No experiments assigned to the client" | Experiment not in `Running` status, or no analytics session yet | Confirm experiment status in dotCMS; verify `apiKey` and `server` |
| No `isUserIncluded` request | Missing `apiKey`, wrong route, or experiments view not mounted | Check env vars; confirm the page uses the experiments view |
| Events go to wrong host | `server` points to CDN instead of dotCMS origin | Set `server` to the dotCMS instance URL |

## Support

-   **GitHub Issues**: [open an issue](https://github.com/dotCMS/core/issues/new/choose)
-   **Community Forum**: [community.dotcms.com](https://community.dotcms.com/)
-   **Stack Overflow**: tag `dotcms-experiments`
-   **Enterprise Support**: [dotCMS Support Portal](https://helpdesk.dotcms.com/support/)

When reporting issues, include SDK version, framework version, minimal reproduction steps, and expected vs. actual behavior.

## Contributing

1. Fork [dotCMS/core](https://github.com/dotCMS/core)
2. Create a feature branch
3. Commit your changes
4. Open a Pull Request

Please ensure your code follows the existing style and includes appropriate tests.

Before any pull requests can be accepted, an automated tool will ask you to agree to the [dotCMS Contributor's Agreement](https://gist.github.com/wezell/85ef45298c48494b90d92755b583acb3).

## Licensing

dotCMS is available under either the [Business Source License 1.1 (BSL)](https://www.dotcms.com/bsl) or a commercial license.

Under the BSL, dotCMS can be used at no cost by individual developers, small businesses or agencies under $5M in total finances, and by larger organizations in non-production environments. Every BSL release automatically converts to GPL v3 four years after its release date. For full terms and FAQs, visit [dotcms.com/bsl](https://www.dotcms.com/bsl) and [dotcms.com/bsl-faq](https://www.dotcms.com/bsl-faq).

Production use in larger organizations, along with access to managed cloud, SLAs, support, and enterprise capabilities, is available under a commercial license from dotCMS. For details on commercial plans, features, and support options, see [dotcms.com/pricing](https://www.dotcms.com/pricing).
