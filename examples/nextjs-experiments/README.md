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

## Advanced: Next.js + dotCMS Architecture

### Integration Overview

The integration between dotCMS and Next.js works by:

1. **Content Creation**: Content editors create and manage content in dotCMS
2. **Content Delivery**: Next.js fetches content from dotCMS using the API client
3. **Content Rendering**: React components render the fetched content
4. **Visual Editing**: The Universal Visual Editor enables in-context editing

### File Structure

```bash
src/
├── app/                       # App Router pages (Server-Side Rendered, .tsx)
│   ├── [[...slug]]/           # Dynamic catch-all routing
│   │   └── page.tsx           # Rendering rules for the specified route
│   ├── blog/                  # Blog pages
│   │   ├── post/[[...slug]]   # Further dynamic routing
│   │   │        └── page.tsx  # Rendering rules for the specified route
│   │   └── page.tsx           # Rendering for `blog/` root page
│   ├── layout.tsx             # Root layout
│   ├── not-found.tsx          # 404 page
│   └── globals.css            # Global styles
├── components/
│   ├── content-types/         # One component per dotCMS Content Type
│   │   ├── index.ts           # Content Type → React component mapping (pageComponents)
│   │   └── *.tsx              # Individual Content Type components
│   ├── editor/                # UVE editor buttons (EditButton, ReorderMenuButton)
│   ├── header/ footer/ forms/ # Site chrome
│   └── *.tsx                  # error, ErrorLayout, BlogCard, DestinationListing, ...
├── config/
│   └── dotcms.config.ts       # Typed, centralized environment access
├── hooks/                     # Custom React hooks (useIsEditMode, useDebounce)
├── lib/
│   └── dotCMSClient.ts        # dotCMS API client initialization
├── types/
│   └── content.ts             # Shared TypeScript interfaces (Blog, Destination, ...)
├── utils/                     # Utility functions
│   ├── getDotCMSPage.ts       # Cached page fetch (page + GraphQL content)
│   ├── pageResponse.ts        # Typed guards (isPageError, getPageContent, ...)
│   ├── queries.ts             # GraphQL query strings
│   └── imageLoader.ts         # Custom Next.js image loader
└── views/                     # Client-side page templates (can use React hooks)
    ├── Page.tsx
    ├── DetailPage.tsx
    └── BlogListingPage.tsx
```

### Understanding the Structure

This project is written in **TypeScript** (strict mode, with the `@/*` path alias configured in `tsconfig.json`) and uses Next.js with App Router for server-side rendering, with some important architectural decisions:

1. **App Router (src/app/)**: Contains all server-side rendered pages and routes. These components don't use React hooks directly due to Next.js 13+ restrictions. Learn more about the App Router [here](https://nextjs.org/docs/app).

2. **Components (src/components/)**:
    - The `content-types/` folder contains React components that render dotCMS content.
    - In dotCMS, a "Content Type" is like a data model (e.g., "Product", "BlogPost"), while a "Contentlet" is an actual content instance.
    - For each Content Type in dotCMS, you need a corresponding React component to render it.
    - For example, if you have a `MyCustomContent` content type in dotCMS, you would create a matching component in this folder to render it.

3. **Views (src/views/)**: Contains client-side page templates (`Page.tsx`, `DetailPage.tsx`, `BlogListingPage.tsx`) that can use React hooks. Since Next.js App Router components can't directly use hooks, these components handle client-side logic.

4. **Config (src/config/)**: The `dotcms.config.ts` module provides typed, centralized access to the dotCMS environment variables, so every other module reads configuration from one place instead of touching `process.env` directly. This module also exposes a dedicated, centralized **`experimentsConfig`** object that groups everything the Experiments feature needs — the dotCMS host (`server`), the analytics `experimentsKey` (from `NEXT_PUBLIC_DOTCMS_EXPERIMENTS_KEY`), and a `debug` flag — so experiments settings live in one typed place alongside the rest of the configuration:

    ```ts
    // src/config/dotcms.config.ts
    export const experimentsConfig = {
        server: process.env.NEXT_PUBLIC_DOTCMS_HOST,
        experimentsKey: process.env.NEXT_PUBLIC_DOTCMS_EXPERIMENTS_KEY,
        debug: process.env.NEXT_PUBLIC_EXPERIMENTS_DEBUG === "true",
    };
    ```

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

And here's a typical page fetching function. It is wrapped in React's `cache()` so multiple callers within a single request (e.g. `generateMetadata` and the page body) share one network round-trip, and it requests extra GraphQL content alongside the page. On failure it returns `{ error }` so callers can branch without `try/catch` — use the guards in `@/utils/pageResponse` to narrow the result:

```ts
import { cache } from "react";

import { dotCMSClient } from "@/lib/dotCMSClient";
import type { PageExtraContent } from "@/types/content";
import { blogQuery, destinationQuery, navigationQuery } from "@/utils/queries";

export const getDotCMSPage = cache(async (path: string) => {
    try {
        return await dotCMSClient.page.get<{ content: PageExtraContent }>(path, {
            graphql: {
                content: {
                    blogs: blogQuery,
                    destinations: destinationQuery,
                    navigation: navigationQuery,
                },
            },
        });
    } catch (error) {
        return { error };
    }
});
```

Learn more about the `@dotcms/client` package [here](https://www.npmjs.com/package/@dotcms/client/).

### How dotCMS Routes Pages

dotCMS allows a single page to be accessed via multiple URL paths (e.g., / and /index for the same "Home" page). This flexibility means your Next.js application needs to handle these variations.

To ensure all paths to the same content are properly managed and to prevent 404/500 errors, we recommend using a catch-all route strategy in Next.js.

How to Implement in Next.js:

Implement a dynamic route like `[[...slug]]` in your Next.js app. This route will capture all URL segments, allowing your application to correctly process any path dotCMS uses for your content.

You can learn more about Next.js routing strategies [here](https://nextjs.org/docs/app/api-reference/file-conventions/dynamic-routes#typescript)

### How to Render Your Page

The rendering process for dotCMS content in Next.js involves several key components working together:

1. **Page Templates**: Define the overall layout and structure
2. **DotCMSLayoutBody**: A component that renders the page content structure
3. **Content Type Components**: Custom React components that render specific Content Types from dotCMS
4. **useEditableDotCMSPage**: A hook that makes the page editable in the UVE

When a page is rendered:

- The page data is fetched from dotCMS
- The `useEditableDotCMSPage` hook prepares it for potential editing
- The `DotCMSLayoutBody` component renders the page structure
- Each content item is rendered by its corresponding React component

Here's how this looks in code:

```tsx
"use client";

import { DotCMSLayoutBody, useEditableDotCMSPage } from "@dotcms/react";

// Define custom components for specific Content Types
// The key is the Content Type variable name in dotCMS
const pageComponents = {
    dotCMSProductContent: MyCustomDotCMSProductComponent,
    dotCMSBlogPost: BlogPostComponent,
};

interface MyPageProps {
    page: Parameters<typeof useEditableDotCMSPage>[0];
}

export function MyPage({ page }: MyPageProps) {
    const { pageAsset, content } = useEditableDotCMSPage(page);

    return (
        <div>
            <DotCMSLayoutBody page={pageAsset} components={pageComponents} />
        </div>
    );
}
```

> [!IMPORTANT]
>
> - The `useEditableDotCMSPage` hook will not modify the `page` object outside the editor
> - The `DotCMSLayoutBody` component renders both the page structure and content
> - Custom components defined in `pageComponents` will be used to render Content Types

Learn more about the `@dotcms/react` package [here](https://www.npmjs.com/package/@dotcms/react/v/next).

#### Content Type to React Component Mapping

One of the key concepts in this integration is mapping dotCMS Content Types to React components. This mapping tells the framework which React component should render which type of content from dotCMS.

**How the mapping works:**

1. Each key in the mapping object must match exactly with a Content Type variable name in dotCMS
2. Each value is a React component that will be used to render that specific Content Type
3. When content is rendered, the contentlet data from dotCMS is passed as props to your component

```ts
// Example of mapping dotCMS Content Types to React components
const pageComponents = {
    // The key "DotCMSProduct" must match a Content Type variable name in dotCMS
    DotCMSProduct: ProductComponent,
    // The key "DotCMSBlogPost" must match a Content Type variable name in dotCMS
    DotCMSBlogPost: BlogPostComponent,
};
```

**What happens at runtime:**

1. When dotCMS content of type "DotCMSProduct" is encountered on a page:
    - The `ProductComponent` is rendered
    - The contentlet data is passed as props to `ProductComponent`
2. Your component then has access to all fields defined in that Content Type

Example of a component receiving contentlet data:

```tsx
// The props passed to this component are the contentlet data from dotCMS.
// Declare an interface so each field is typed.
interface ProductProps {
    title?: string;
    price?: number;
    description?: string;
    image?: { url: string };
}

function ProductComponent({ title, price, description, image }: ProductProps) {
    // Access fields defined in the DotCMSProduct Content Type
    return (
        <div className="product">
            <h2>{title}</h2>
            {image && <img src={image.url} alt={title} />}
            <p className="price">${price}</p>
            <p>{description}</p>
        </div>
    );
}
```

This pattern allows you to create custom rendering for each type of content in your dotCMS instance, while maintaining a clean separation between content and presentation.

This mapping should be passed to the `DotCMSLayoutBody` component as shown in the previous section.

**Learn more about dotCMS Content and Components:**

- [Understanding Content Types in dotCMS](https://dev.dotcms.com/docs/content-types) - In-depth explanation of content types and their structure
- [Contentlets in dotCMS](https://dev.dotcms.com/docs/content#Contentlets) - Learn how individual content items (contentlets) work
- [@dotcms/react Documentation](https://www.npmjs.com/package/@dotcms/react/) - Complete reference for the React components library

## Experiments (headless)

This example integrates the dotCMS **Experiments** feature on top of the content/UVE patterns above, using `@dotcms/experiments` for A/B testing in the Next.js App Router. It requires the experiments infrastructure and configured Experiments app described in [dotCMS Requirements](#dotcms-requirements).

For additional references and the safe `withExperiments` pattern, see:

- [Experiments SDK README](../../core-web/libs/sdk/experiments/README.md)
- [Experiments Docker Compose stack](../../docker/docker-compose-examples/experiments/README.md) — one-command setup for dotCMS + analytics infrastructure

## Conclusion

This example demonstrates the powerful integration between dotCMS and Next.js, enabling fully editable and dynamic web pages. By leveraging dotCMS as a headless CMS and Next.js for front-end rendering, you can create high-performance websites that offer both developer flexibility and content editor ease-of-use.

Key benefits of this approach include:

- **Separation of concerns**: Content management in dotCMS, presentation in Next.js
- **Visual editing**: In-context editing with the Universal Visual Editor
- **Performance**: Next.js server-side rendering and optimization
- **Flexibility**: Custom React components for different content types
- **Developer experience**: Modern JavaScript tooling and frameworks

## Learn More

To deepen your understanding of this integration, explore these official dotCMS resources:

- [JavaScript SDK: React Library](https://dev.dotcms.com/docs/javascript-sdk-react-library) - Documentation for the @dotcms/react package, including components and hooks
- [Universal Visual Editor](https://dev.dotcms.com/docs/universal-visual-editor) - Learn more about the visual editing capabilities
- [Content in dotCMS](https://dev.dotcms.com/docs/content) - Understanding content types and content management in dotCMS

Additional resources:

- [dotCMS Developer Documentation](https://dev.dotcms.com/)
- [Next.js Documentation](https://nextjs.org/docs)
