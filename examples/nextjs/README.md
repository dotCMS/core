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

## dotCMS SDK Components

This example uses the following npm packages from dotCMS:

| Package | Purpose | Description |
|---------|---------|-------------|
| [@dotcms/client](https://www.npmjs.com/package/@dotcms/client/next) | API Communication | Core API client for fetching content from dotCMS |
| [@dotcms/react](https://www.npmjs.com/package/@dotcms/react/next) | UI Components | React components and hooks for rendering dotCMS content |
| [@dotcms/uve](https://www.npmjs.com/package/@dotcms/uve) | Visual Editing | Universal Visual Editor integration |
| [@dotcms/types](https://www.npmjs.com/package/@dotcms/types) | Type Safety | TypeScript type definitions for dotCMS |

> [!NOTE]
> These packages are already included in the example project's dependencies, so you don't need to install them separately.

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

First, get a [dotCMS Site](https://www.dotcms.com/pricing). If you want to test this example, you can also use our [demo site](https://dev.dotcms.com/docs/demo-site).

If using the demo site, you can log in with these credentials:

| User Name | Password |
|-----------|----------|
| admin@dotcms.com | admin |

Once you have a site, you can log in with the credentials and start creating content.

#### B. Create a dotCMS API Key

This integration requires an API Key with read-only permissions for security best practices:

1. Go to the **dotCMS admin panel**.
2. Click on **System** > **Users**.
3. Select the user you want to create the API Key for.
4. Go to **API Access Key** and generate a new key.

> [!WARNING]
> **Security Note**: Read-only permissions for Pages, Folders, Assets, and Content are sufficient for this integration. Using a key with minimal permissions follows security best practices.

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

Create a `.env.local` file in the root of the project by running the following command:

```bash
# This will create a new file with the correct variables.
cp .env.local.example .env.local
```

Then set each variable in the `.env.local` file:

- `NEXT_PUBLIC_DOTCMS_HOST`: The URL of your dotCMS site.
- `NEXT_PUBLIC_DOTCMS_API_KEY`: The API Key you created in Step 2B.
- `NEXT_PUBLIC_DOTCMS_SITE_ID`: The ID of the site you want to use. 

The Site ID refers to the site that will be used to pull content into your Next.js app. dotCMS is a multi-site CMS, meaning a single instance can manage multiple websites, the site ID specifies which specific site's content should be pulled into your Next.js app. If incorrect, content requests will fail. If left empty, content will be pulled from the default site configured in dotCMS.

You can find your site ID in the dotCMS admin panel under System > Sites. Learn more about [dotCMS Multi-Site management here](https://dev.dotcms.com/docs/multi-site-management#multi-site-management).

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

## How It Works

The integration between dotCMS and Next.js works by:

1. **Content Creation**: Content editors create and manage content in dotCMS
2. **Content Delivery**: Next.js fetches content from dotCMS using the API client
3. **Content Rendering**: React components render the fetched content
4. **Visual Editing**: The Universal Visual Editor enables in-context editing

For developers who want to understand the technical implementation details, see the [Advanced: Next.js + dotCMS Architecture](#advanced-nextjs--dotcms-architecture) section.

## Edit your page in the Universal Visual Editor

After setting up the Universal Visual Editor and running your Next.js application, you can edit your page in the Universal Visual Editor:

1. Log in to the dotCMS admin panel
2. Go to the page you want to edit
3. Click on the "Edit" button
4. The page will be rendered in the editor with your Next.js frontend
5. Make changes directly on the page

Learn more about the Universal Visual Editor [here](https://dev.dotcms.com/docs/universal-visual-editor).

## Advanced: Next.js + dotCMS Architecture

If you want to understand the technical details of how this project is structured and how it integrates with dotCMS, this section provides an in-depth look at the architecture.

### File Structure

```bash
src/
├── app/              # App Router components (Server-Side Rendered pages)
│   ├── [[...slug]]/  # Dynamic routing
│   ├── blog/         # Blog pages
│   ├── layout.js     # Root layout
│   └── ...
├── components/ 
│   └── contenttype/  # Components for rendering dotCMS contenttypes
├── hooks/            # Custom React hooks
├── pages/            # Client-side page templates (can use React hooks)
└── utils/            # Utility functions
    ├── dotCMSClient.js  # dotCMS API client initialization
    └── ...
```

### Understanding the Structure

This project uses Next.js with App Router for server-side rendering, but with some important architectural decisions:

1. **App Router (src/app/)**: Contains all server-side rendered pages and routes. These components don't use React hooks directly due to Next.js 13+ restrictions. Learn more about the App Router [here](https://nextjs.org/docs/app).

2. **Components (src/components/)**: 
   - The `contenttype/` folder contains React components that render dotCMS content.
   - In dotCMS, a "Content Type" is like a data model (e.g., "Product", "BlogPost"), while a "Contentlet" is an actual content instance.
   - For each Content Type in dotCMS, you need a corresponding React component to render it.
   - For example, if you have a `MyCustomContent` content type in dotCMS, you would create a matching component in this folder to render it.

3. **Pages (src/pages/)**: Contains client-side page templates that can use React hooks. Since Next.js App Router components can't directly use hooks, these components handle client-side logic.

4. **Utils (src/utils/)**: Contains utility functions, most importantly the `dotCMSClient.js` which initializes the connection to your dotCMS instance.

### How the Content is Fetched from dotCMS

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

As an example, inside the `src/utils/getDotCMSPage.js` file, we fetch a page from dotCMS:

```js
import { dotCMSClient } from "./dotCMSClient";

export const getDotCMSPage = async (path, searchParams) => {
    try {
        const pageData = await dotCMSClient.page.get(path, searchParams);
        return pageData;
    } catch (e) {
        console.error("ERROR FETCHING PAGE: ", e.message);

        return null;
    }
};
```

Learn more about the `@dotcms/client` package [here](https://www.npmjs.com/package/@dotcms/client/next).

### How the Page is Rendered

This project uses `@dotcms/react` to render dotCMS content in React components:

```js
"use client";

import { DotCMSBodyLayout, useEditableDotCMSPage } from "@dotcms/react/next";

// Define custom components for specific contenttypes
const pageComponents = {
    dotCMSProductContent: MyCustomDotCMSProductComponent,
}

export function MyPage({ page }) {
    // Make the page editable in the UVE
    const { pageAsset, content } = useEditableDotCMSPage(page);

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
1. Takes the `page` from the dotCMS Client SDK using the `getDotCMSPage` method.
2. Uses `useEditableDotCMSPage` to make the page editable in the Universal Visual Editor
3. Renders the page content with `DotCMSBodyLayout`

> [!HINT]
> - The `useEditableDotCMSPage` hook will not modify the `page` object outside the editor
> - The `DotCMSBodyLayout` component renders both the page structure and content
> - Custom components defined in `pageComponents` will be used to render specific contenttypes

Learn more about the `@dotcms/react` package [here](https://www.npmjs.com/package/@dotcms/react/v/next).

#### ContentType to React Component Mapping

One of the key concepts in this integration is mapping dotCMS contenttypes to React components:

```js
// Example of mapping dotCMS contenttypes to React components
const pageComponents = {
  // If you have a "Product" content type in dotCMS, this maps it to your ProductComponent
  DotCMSProduct: ProductComponent,
  // If you have a "BlogPost" content type, this maps it to your BlogPostComponent
  DotCMSBlogPost: BlogPostComponent
}
```

This mapping tells the dotCMS integration which React component should render which type of content from dotCMS. It should be passed to the `DotCMSBodyLayout` component. Learn more about the `DotCMSBodyLayout` component [here](https://www.npmjs.com/package/@dotcms/react/v/next).

## Conclusion

This example demonstrates the powerful integration between dotCMS and Next.js, enabling fully editable and dynamic web pages. By leveraging dotCMS as a Universal CMS and Next.js for frontend rendering, you can create high-performance websites that offer both developer flexibility and content editor ease-of-use.

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

