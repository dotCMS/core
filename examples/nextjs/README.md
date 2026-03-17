# Fully Editable Page Using dotCMS + Next.js

## Introduction & Overview

This project demonstrates how to build dynamic, fully editable pages using [dotCMS](https://dotcms.com/) as a headless CMS with a [Next.js](https://nextjs.org/) front end. By combining these technologies, you can:

- **Create content in dotCMS** and deliver it headlessly to your Next.js application
- **Edit content visually** using dotCMS's Universal Visual Editor (UVE) directly on your Next.js front end
- **Build high-performance pages** leveraging Next.js's server-side rendering capabilities
- **Maintain content separation** between your CMS and presentation layer

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

### Demo

See a live example at **[https://nextjs-example-sigma-five.vercel.app/](https://nextjs-example-sigma-five.vercel.app/)**.

The example above is a Next.js front end for the [dotCMS demo site](https://demo.dotcms.com/), and changes to pages and content on the latter will be reflected, there. For more information on the demo site, see [the relevant section below](#a-get-a-dotcms-site).

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
    - [Style Editor](#style-editor)
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

- **dotCMS instance**: Access to a dotCMS instance (v25.05 or Evergreen recommended)
    - For testing: You can use [the dotCMS demo site](https://dev.dotcms.com/docs/demo-site)
    - For production: [Sign up for a dotCMS instance](https://www.dotcms.com/pricing)
- **Administrator access**: To create API tokens and configure the Universal Visual Editor
- **API token**: With appropriate read permissions for your Next.js app

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

- `NEXT_PUBLIC_DOTCMS_HOST`: The URL of your dotCMS site.
- `NEXT_PUBLIC_DOTCMS_AUTH_TOKEN`: The API Key you created in Step 2B.
- `NEXT_PUBLIC_DOTCMS_SITE_ID`: The site key of the site you want to use.

The site ID variable refers to the site that will be used to pull content into your Next.js app. dotCMS is a multi-site CMS, meaning a single instance can manage multiple websites; the site ID specifies which site's content should be pulled into your Next.js app. If left empty or given an incorrect value, content will be pulled from the default site configured in dotCMS.

You can find the values for this variable — site keys or identifiers both work, though keys are simpler and more recommended — under System > Sites. Learn more about [dotCMS Multi-Site management here](https://dev.dotcms.com/docs/multi-site-management#multi-site-management).

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
├── app/                       # App Router components (Server-Side Rendered pages)
│   ├── [[...slug]]/           # Dynamic routing
│   │   └── page.js            # Rendering rules for the specified route
│   ├── blog/                  # Blog pages
│   │   ├── post/[[...slug]]   # Further dynamic routing
│   │   │        └── page.js   # Rendering rules for the specified route
│   │   └── page.js            # Rendering for `blog/` root page
│   ├── layout.js              # Root layout
│   └── ...
├── components/
│   └── content-type/          # Components for rendering dotCMS Content Types
├── hooks/                     # Custom React hooks
├── pages/                     # Client-side page templates (can use React hooks)
└── utils/                     # Utility functions
    ├── dotCMSClient.js        # dotCMS API client initialization
    └── ...
```

### Understanding the Structure

This project uses Next.js with App Router for server-side rendering, but with some important architectural decisions:

1. **App Router (src/app/)**: Contains all server-side rendered pages and routes. These components don't use React hooks directly due to Next.js 13+ restrictions. Learn more about the App Router [here](https://nextjs.org/docs/app).

2. **Components (src/components/)**:
    - The `content-type/` folder contains React components that render dotCMS content.
    - In dotCMS, a "Content Type" is like a data model (e.g., "Product", "BlogPost"), while a "Contentlet" is an actual content instance.
    - For each Content Type in dotCMS, you need a corresponding React component to render it.
    - For example, if you have a `MyCustomContent` content type in dotCMS, you would create a matching component in this folder to render it.

3. **Pages (src/pages/)**: Contains client-side page templates that can use React hooks. Since Next.js App Router components can't directly use hooks, these components handle client-side logic.

4. **Utils (src/utils/)**: Contains utility functions, most importantly the `dotCMSClient.js` which initializes the connection to your dotCMS instance.

### How the Content is Fetched from dotCMS

Content in this integration is fetched using the `@dotcms/client` package, which provides a streamlined way to communicate with the dotCMS API. This client handles authentication, request formatting, and response parsing automatically.

The process works as follows:

1. First, we create a configured client instance in `src/utils/dotCMSClient.js`
2. This client uses the environment variables to connect to your dotCMS instance
3. When a page is requested, we use this client to fetch the page data along with its content
4. All API requests are managed through this central client for consistency

Here's how the client is configured:

```js
import { createDotCMSClient } from "@dotcms/client";

export const dotCMSClient = createDotCMSClient({
    dotcmsUrl: process.env.NEXT_PUBLIC_DOTCMS_HOST,
    authToken: process.env.NEXT_PUBLIC_DOTCMS_AUTH_TOKEN,
    siteId: process.env.NEXT_PUBLIC_DOTCMS_SITE_ID,
    requestOptions: {
        cache: "no-cache",
    },
});
```

And here's a typical page fetching function:

```js
export const getDotCMSPage = async (path, searchParams) => {
    try {
        return await dotCMSClient.page.get(path, searchParams);
    } catch (e) {
        console.error("ERROR FETCHING PAGE: ", e.message);
        return null;
    }
};
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

```js
"use client";

import { DotCMSLayoutBody, useEditableDotCMSPage } from "@dotcms/react";

// Define custom components for specific Content Types
// The key is the Content Type variable name in dotCMS
const pageComponents = {
    dotCMSProductContent: MyCustomDotCMSProductComponent,
    dotCMSBlogPost: BlogPostComponent,
};

export function MyPage({ page }) {
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

```js
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

```jsx
// The props passed to this component will be the contentlet data from dotCMS
function ProductComponent(props) {
    // Access fields defined in the DotCMSProduct Content Type
    const { title, price, description, image } = props;

    return (
        <div className="product">
            <h2>{title}</h2>
            <img src={image.url} alt={title} />
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

### Style Editor

The Style Editor enables content editors to customize component appearance (typography, colors, layouts, etc.) directly in the Universal Visual Editor without code changes. Style properties are defined by developers and made editable through style editor schemas.

#### Defining a Style Editor Schema

Create a schema file (e.g., `src/utils/styleEditorSchemas.js`) that defines editable style properties for your content types:

```js
import { defineStyleEditorSchema, styleEditorField } from "@dotcms/uve";

export const BANNER_SCHEMA = defineStyleEditorSchema({
    contentType: 'Banner', // Must match your dotCMS Content Type
    sections: [
        {
            title: 'Typography',
            fields: [
                styleEditorField.dropdown({
                    id: 'title-size',
                    label: 'Title Size',
                    options: [
                        { label: 'Small', value: 'text-4xl' },
                        { label: 'Medium', value: 'text-5xl' },
                        { label: 'Large', value: 'text-6xl' },
                    ]
                }),
                styleEditorField.checkboxGroup({
                    id: 'title-style',
                    label: 'Title Style',
                    options: [
                        { label: 'Bold', key: 'bold' },
                        { label: 'Italic', key: 'italic' },
                    ]
                }),
            ]
        },
        {
            title: 'Layout',
            fields: [
                styleEditorField.radio({
                    id: 'text-alignment',
                    label: 'Text Alignment',
                    options: [
                        { label: 'Left', value: 'left' },
                        { label: 'Center', value: 'center' },
                        { label: 'Right', value: 'right' },
                    ]
                }),
            ]
        },
    ]
});
```

#### Field Types

The Style Editor supports four field types:

- **`styleEditorField.input()`** - Text or number input for custom values
- **`styleEditorField.dropdown()`** - Dropdown with predefined options
- **`styleEditorField.radio()`** - Radio buttons for single selection (supports images)
- **`styleEditorField.checkboxGroup()`** - Multiple checkboxes (returns object with boolean values)

#### Registering Schemas

Register your schemas in your page component using the `useStyleEditorSchemas` hook:

```js
"use client";

import { useStyleEditorSchemas } from "@dotcms/react";
import { BANNER_SCHEMA, ACTIVITY_SCHEMA } from "@/utils/styleEditorSchemas";

export function Page({ pageContent }) {
    // Register schemas - makes them available in UVE edit mode
    useStyleEditorSchemas([BANNER_SCHEMA, ACTIVITY_SCHEMA]);

    return (
        // Your page content
    );
}
```

#### Using Style Properties in Components

Style properties are automatically passed to your contentlet components via the `dotStyleProperties` prop. Access them with defaults:

```js
function Banner({ title, caption, dotStyleProperties }) {
    // Extract style properties with defaults
    const titleSize = dotStyleProperties?.["title-size"] || "text-6xl";
    const titleStyle = dotStyleProperties?.["title-style"] || {};
    const textAlignment = dotStyleProperties?.["text-alignment"] || "center";

    // Build dynamic classes
    const titleClasses = [
        titleSize,
        titleStyle.bold ? "font-bold" : "font-normal",
        titleStyle.italic ? "italic" : "",
    ].filter(Boolean).join(" ");

    return (
        <div className={`text-${textAlignment}`}>
            <h2 className={titleClasses}>{title}</h2>
            <p>{caption}</p>
        </div>
    );
}
```

**Value Types:**
- **Input/Dropdown/Radio**: Returns a string (the selected value)
- **Checkbox Group**: Returns an object with boolean values (e.g., `{ bold: true, italic: false }`)

**Best Practices:**
- Always provide default values when accessing style properties
- Use meaningful field IDs that match your styling logic
- Group related fields into logical sections
- The `contentType` in your schema must exactly match your dotCMS Content Type variable name

For complete Style Editor documentation, see the [@dotcms/uve Style Editor guide](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/uve/README.md#style-editor).

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
