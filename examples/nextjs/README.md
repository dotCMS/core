# Fully Editable Page Using dotCMS + Next.js

## Introduction & Overview

This project demonstrates how to build dynamic, fully editable pages using [dotCMS](https://dotcms.com/) as a Universal CMS with a [Next.js](https://nextjs.org/) frontend. By combining these technologies, you can:

- **Create content in dotCMS** and deliver it headlessly to your Next.js application
- **Edit content visually** using dotCMS's Universal Visual Editor (UVE) directly on your Next.js frontend
- **Build high-performance pages** leveraging Next.js's server-side rendering capabilities
- **Maintain content separation** between your CMS and presentation layer

### How It Works

```
┌───────────────┐      ┌───────────────┐      ┌───────────────┐
│               │      │               │      │               │
│    dotCMS     │──────▶   Next.js     │──────▶   Browser     │
│  (Content)    │      │  (Frontend)   │      │  (Viewing)    │
│               │      │               │      │               │
└───────────────┘      └───────────────┘      └───────────────┘
        ▲                      │                      │
        │                      │                      │
        └──────────────────────┴──────────────────────┘
                Universal Visual Editor (UVE)
                   (Content Editing)
```

The integration uses dotCMS APIs to fetch content and the Universal Visual Editor to enable in-context editing directly on your Next.js pages.

### Demo

See a live example at [https://dotcms-nextjs-demo.vercel.app/](https://dotcms-nextjs-demo.vercel.app/)

## Prerequisites

Before you begin, make sure you have:

### System Requirements
- **Node.js**: v18.8.0 or later (v22+ recommended)
- **NPM**, **Yarn**, or **pnpm** package manager
- **Git** for version control
- A code editor (VS Code, WebStorm, etc.)

### dotCMS Requirements
- **dotCMS instance**: Access to a dotCMS instance (v25.05 or Evergreen recommended)
  - For testing: You can use [the dotCMS demo site](https://dev.dotcms.com/docs/demo-site)
  - For production: [Sign up for a dotCMS instance](https://www.dotcms.com/pricing)
- **Administrator access**: To create API tokens and configure the Universal Visual Editor
- **API token**: With appropriate read permissions for your Next.js app

### Knowledge Prerequisites
- Basic understanding of React and Next.js concepts
- Familiarity with content management systems (prior dotCMS experience helpful but not required)

## Setup Guide

This guide will walk you through the process of setting up the dotCMS Next.js example from scratch.

### Step 1: Create the Next.js Application

Use one of the following commands to create a new Next.js app with the dotCMS example:

```bash
# Using npm
npx create-next-app dotcms-nextjs-demo --example https://github.com/dotCMS/core/tree/main/examples/nextjs

# Using Yarn
yarn create next-app dotcms-nextjs-demo --example https://github.com/dotCMS/core/tree/main/examples/nextjs

# Using pnpm
pnpm create next-app dotcms-nextjs-demo --example https://github.com/dotCMS/core/tree/main/examples/nextjs
```

This will create a new directory with the example code and install all necessary dependencies.

### Step 2: Configure dotCMS Access

#### A. Get a dotCMS Site

First, [get a dotCMS Site](https://www.dotcms.com/pricing). If you want to test this example, you can also use our [demo site](https://dev.dotcms.com/docs/demo-site).

If using the demo site, you can log in with these credentials:

| User Name | Password |
|-----------|----------|
| admin@dotcms.com | admin |

After creating an account, create a new empty site from the dashboard and assign to it any name of your liking.

#### B. Create a dotCMS API Key

This integration requires an API Key with read-only permissions for security best practices:

1. Go to the dotCMS admin panel.
2. Click on System > Users.
3. Select the user you want to create the API Key for.
4. Go to API Access Key and generate a new key.

> **Security Note**: Read-only permissions for Pages, Folders, Assets, and Content are sufficient for this integration. Using a key with minimal permissions follows security best practices.

For detailed instructions, please refer to the [dotCMS API Documentation - Read-only token](https://dev.dotcms.com/docs/rest-api-authentication#ReadOnlyToken).

#### C. Configure the Universal Visual Editor

The Universal Visual Editor (UVE) is a critical feature that creates a bridge between your dotCMS instance and your Next.js application. This integration **Enables real-time visual editing** and allows content editors to see and modify your actual Next.js pages directly from within dotCMS.

To set up the Universal Visual Editor:

1. Browse to Settings -> Apps.
2. Select the built-in integration for UVE - Universal Visual Editor.
3. Select the site that will be feeding the destination pages.
4. Add the following configuration:

```json
{ 
    "config":[ 
        { 
            "pattern":"(.*)", 
            "url":"http://localhost:3000"
        }
    ] 
}
```

This configuration tells dotCMS that when editors are working on content in the admin panel, they should see your Next.js application running at `http://localhost:3000`. The pattern `(.*)` means this applies to all pages in your site.

You can learn more about configuring the Universal Visual Editor in the [dotCMS UVE Documentation](https://dev.dotcms.com/docs/uve-headless-config).

### Step 3: Configure the Next.js Application

#### A. Set Environment Variables

Create a `.env.local` file in the root of the project by copying the example file:

```bash
cp .env.local.example .env.local
```

Then set each variable in the `.env.local` file:

- `NEXT_PUBLIC_DOTCMS_HOST`: The URL of your dotCMS site.
- `NEXT_PUBLIC_DOTCMS_API_KEY`: The API Key you created in Step 2B.
- `NEXT_PUBLIC_DOTCMS_SITE_ID`: The ID of the site you want to use. 
  - dotCMS is a multi-site CMS, meaning a single instance can manage multiple websites.
  - The site ID specifies which specific site's content should be pulled into your Next.js app.
  - If incorrect, content requests will fail.
  - If left empty, content will be pulled from the default site configured in dotCMS.
  - You can find your site ID in the dotCMS admin panel under System > Sites. 
  - Learn more about [dotCMS Multi-Site management here](https://dev.dotcms.com/docs/multi-site-management#multi-site-management).

### Step 4: Run the Application

Run the development server with one of the following commands:

```bash
# Using npm
npm run dev

# Using Yarn
yarn dev

# Using pnpm
pnpm dev
```

You should see a message in your terminal indicating that the Next.js app is running at `http://localhost:3000`. Open this URL in your browser to see your dotCMS-powered Next.js site.

## Project Structure and Components

### File Structure

```bash
.
├── src/
│   ├── app/              # App router components
│   │   ├── [[...slug]]/  # Dynamic routing
│   │   ├── blog/         # Blog pages
│   │   ├── layout.js     # Root layout
│   │   └── ...
│   ├── components/       # Reusable UI components
│   ├── hooks/            # Custom React hooks
│   ├── pages/            # Page templates
│   └── utils/            # Utility functions
│       ├── dotCMSClient.js  # dotCMS API client
│       └── ...
├── .env.local.example    # Template for environment variables
├── next.config.js        # Next.js configuration
├── package.json          # Dependencies and scripts
└── jsconfig.json         # JavaScript configuration
```

### @dotcms SDK Components Used

This project uses the following @dotcms packages:

- [@dotcms/client](https://www.npmjs.com/package/@dotcms/client/next) - Core API client for fetching content from dotCMS
- [@dotcms/react](https://www.npmjs.com/package/@dotcms/react/next) - React components and hooks for rendering dotCMS content
- [@dotcms/uve](https://www.npmjs.com/package/@dotcms/uve) - Universal Visual Editor integration
- [@dotcms/types](https://www.npmjs.com/package/@dotcms/types) - TypeScript type definitions for dotCMS

## How It Works

### Fetching Content from dotCMS

This project uses `@dotcms/client` to pull content from dotCMS.
Inside the `src/utils/dotCMSClient.js` file, we configure the client to use the dotCMS API:

```js
import { createDotCMSClient } from "@dotcms/client/next";

export const dotCMSClient = createDotCMSClient({
    dotcmsUrl: process.env.NEXT_PUBLIC_DOTCMS_HOST,
    authToken: process.env.NEXT_PUBLIC_DOTCMS_AUTH_TOKEN,
    siteId: process.env.NEXT_PUBLIC_DOTCMS_SITE_ID,
    requestOptions: {
        cache: "no-cache",
    }
});
```

The `dotCMSClient` can then be used to fetch various types of content from dotCMS, including pages, assets, and contentlets.

Learn more about the `@dotcms/client` package [here](https://www.npmjs.com/package/@dotcms/client/next).

### Rendering Content on the Page

This project uses `@dotcms/react` to render dotCMS content in React components:

```js
import { DotCMSBodyLayout, useEditableDotCMSPage } from "@dotcms/react/next";

// Define custom components for specific content types
const pageComponents = {
    dotCMSProductContent: MyCustomDotCMSProductComponent,
}

export function MyPage({ pageResponse }) {
    // Make the page editable in the UVE
    const { pageAsset, content } = useEditableDotCMSPage(pageResponse);

    return (
        <div>
            <DotCMSBodyLayout
                page={pageAsset}
                components={pageComponents}
            />
        </div>
    );
}
```

This code:
1. Takes the `pageResponse` from the dotCMS API
2. Uses `useEditableDotCMSPage` to make the page editable in the Universal Visual Editor
3. Renders the page content with `DotCMSBodyLayout`

> [!NOTE]
> - The `useEditableDotCMSPage` hook will not modify the `pageResponse` object outside the editor
> - The `DotCMSBodyLayout` component renders both the page structure and content
> - Custom components defined in `pageComponents` will be used to render specific content types

Learn more about the `@dotcms/react` package [here](https://www.npmjs.com/package/@dotcms/react/next).

### Using the Universal Visual Editor

After setting up the Universal Visual Editor and running your Next.js application:

1. Log in to the dotCMS admin panel
2. Go to the page you want to edit
3. Click on the "Edit" button
4. The page will be rendered in the editor with your Next.js frontend
5. Make changes directly on the page

Learn more about the Universal Visual Editor [here](https://dev.dotcms.com/docs/universal-visual-editor).
