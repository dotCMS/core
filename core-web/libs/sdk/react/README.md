# dotCMS React SDK

The `@dotcms/react` SDK is the DotCMS official React library. It empowers React developers to build powerful, editable websites and applications in no time.

## Table of Contents

-   [Prerequisites & Setup](#prerequisites--setup)
    -   [Get a dotCMS Environment](#get-a-dotcms-environment)
    -   [Create a dotCMS API Key](#create-a-dotcms-api-key)
    -   [Configure The Universal Visual Editor App](#configure-the-universal-visual-editor-app)
    -   [Installation](#installation)
    -   [dotCMS Client Configuration](#dotcms-client-configuration)
    -   [Proxy Configuration for Static Assets](#proxy-configuration-for-static-assets)
-   [Quickstart: Render a Page with dotCMS](#quickstart-render-a-page-with-dotcms)
    -   [Example Project](#example-project-)
-   [SDK Reference](#sdk-reference)
    -   [DotCMSLayoutBody](#dotcmslayoutbody)
    -   [DotCMSShow](#dotcmsshow)
    -   [DotCMSBlockEditorRenderer](#dotcmsblockeditorrenderer)
    -   [DotCMSEditableText](#dotcmseditabletext)
    -   [useEditableDotCMSPage](#useeditabledotcmspage)
    -   [useDotCMSShowWhen](#usedotcmsshowwhen)
-   [Troubleshooting](#troubleshooting)
    -   [Common Issues & Solutions](#common-issues--solutions)
    -   [Debugging Tips](#debugging-tips)
    -   [Version Compatibility](#version-compatibility)
    -   [Still Having Issues?](#still-having-issues)
-   [Migration from Alpha to 1.0.X](./MIGRATION.md)
-   [Support](#support)
-   [Contributing](#contributing)
-   [Licensing](#licensing)

## Prerequisites & Setup

### Get a dotCMS Environment

#### Version Compatibility

-   **Recommended**: dotCMS Evergreen
-   **Minimum**: dotCMS v25.05
-   **Best Experience**: Latest Evergreen release

#### Environment Setup

**For Production Use:**

-   ☁️ [Cloud hosting options](https://www.dotcms.com/pricing) - managed solutions with SLA
-   🛠️ [Self-hosted options](https://dev.dotcms.com/docs/current-releases) - deploy on your infrastructure

**For Testing & Development:**

-   🧑🏻‍💻 [dotCMS demo site](https://demo.dotcms.com/dotAdmin/#/public/login) - perfect for trying out the SDK
-   📘 [Learn how to use the demo site](https://dev.dotcms.com/docs/demo-site)
-   📝 Read-only access, ideal for building proof-of-concepts

**For Local Development:**

-   🐳 [Docker setup guide](https://github.com/dotCMS/core/tree/main/docker/docker-compose-examples/single-node-demo-site)
-   💻 [Local installation guide](https://dev.dotcms.com/docs/quick-start-guide)

### Configure The Universal Visual Editor App

For a step-by-step guide on setting up the Universal Visual Editor, check out our [easy-to-follow instructions](https://dev.dotcms.com/docs/uve-headless-config) and get started in no time!

### Create a dotCMS API Key

> [!TIP]
> Make sure your API Token has read-only permissions for Pages, Folders, Assets, and Content. Using a key with minimal permissions follows security best practices.

This integration requires an API Key with read-only permissions for security best practices:

1. Go to the **dotCMS admin panel**.
2. Click on **System** > **Users**.
3. Select the user you want to create the API Key for.
4. Go to **API Access Key** and generate a new key.

For detailed instructions, please refer to the [dotCMS API Documentation - Read-only token](https://dev.dotcms.com/docs/rest-api-authentication#ReadOnlyToken).

### Install Dependencies

```bash
npm install @dotcms/react@latest
```

This will automatically install the required dependencies:
- `@dotcms/uve`: Enables interaction with the [Universal Visual Editor](https://dev.dotcms.com/docs/uve-headless-config) for real-time content editing
- `@dotcms/client`: Provides the core client functionality for fetching and managing dotCMS data

### dotCMS Client Configuration

```typescript
import { createDotCMSClient } from '@dotcms/client';

type DotCMSClient = ReturnType<typeof createDotCMSClient>;

export const dotCMSClient: DotCMSClient = createDotCMSClient({
    dotcmsUrl: 'https://your-dotcms-instance.com',
    authToken: 'your-auth-token', // Optional for public content
    siteId: 'your-site-id' // Optional site identifier/name
});
```

### Proxy Configuration for Static Assets

Configure a proxy to leverage the powerful dotCMS image API, allowing you to resize and serve optimized images efficiently. This enhances application performance and improves user experience, making it a strategic enhancement for your project.

#### 1. Configure Vite

```ts
// vite.config.ts
import { defineConfig } from 'vite';
import dns from 'node:dns';

dns.setDefaultResultOrder('verbatim');

export default defineConfig({
    server: {
        proxy: {
            '/dA': {
                target: 'your-dotcms-instance.com',
                changeOrigin: true
            }
        }
    }
});
```

Learn more about Vite configuration [here](https://vitejs.dev/config/).

#### 2. Usage in Components

Once configured, image URLs in your components will automatically be proxied to your dotCMS instance:

>📚 Learn more about [Image Resizing and Processing in dotCMS with React](https://www.dotcms.com/blog/image-resizing-and-processing-in-dotcms-with-angular-and-nextjs).

```typescript
// /components/my-dotcms-image.tsx
import type { DotCMSBasicContentlet } from '@dotcms/types';

export const MyDotCMSImageComponent = ({ inode, title }: DotCMSBasicContentlet) => {
    return <img src={`/dA/${inode}`} alt={title} />;
}
```

## Quickstart: Render a Page with dotCMS

The following example demonstrates how to quickly set up a basic dotCMS page renderer in your React application. This example shows how to:

-   Create a standalone component that renders a dotCMS page
-   Set up dynamic component loading for different content types
-   Handle both regular page viewing and editor mode
-   Subscribe to real-time page updates when in the Universal Visual Editor

```tsx
// /src/app/pages/dotcms-page.tsx
import { useState, useEffect } from 'react';
import { DotCMSLayoutBody, useEditableDotCMSPage } from '@dotcms/react';
import { DotCMSPageResponse } from '@dotcms/types';

import { dotCMSClient } from './dotCMSClient';
import { BlogComponent } from './BlogComponent';
import { ProductComponent } from './ProductComponent';

const COMPONENTS_MAP = {
    Blog: BlogComponent,
    Product: ProductComponent
};

const MyPage = () => {
    const [response, setResponse] = useState<DotCMSPageResponse | null>(null);
    const { pageAsset } = useEditableDotCMSPage(response);

    useEffect(() => {
        dotCMSClient.page.get('/').then((response) => {
            setResponse(response);
        });
    }, []);

    return <DotCMSLayoutBody page={pageAsset} components={COMPONENTS_MAP} mode="development" />;
};

export default MyPage;
```

### Example Project 🚀

Looking to get started quickly? We've got you covered! Our [Next.js starter project](https://github.com/dotCMS/core/tree/main/examples/nextjs) is the perfect launchpad for your dotCMS + Next.js journey. This production-ready template demonstrates everything you need:

📦 Fetch and render dotCMS pages with best practices
🧩 Register and manage components for different content types
🔍 Listing pages with search functionality
📝 Detail pages for blogs
📈 Image and assets optimization for better performance
✨ Enable seamless editing via the Universal Visual Editor (UVE)
⚡️ Leverage React's hooks and state management for optimal performance

> [!TIP]
> This starter project is more than just an example, it follows all our best practices. We highly recommend using it as the base for your next dotCMS + Next.js project!

## SDK Reference

All components and hooks should be imported from `@dotcms/react`:

### DotCMSLayoutBody

`DotCMSLayoutBody` is a component used to render the layout for a DotCMS page, supporting both production and development modes.

| Input        | Type                     | Required | Default        | Description                                    |
| ------------ | ------------------------ | -------- | -------------- | ---------------------------------------------- |
| `page`       | `DotCMSPageAsset`        | ✅       | -              | The page asset containing the layout to render |
| `components` | `DotCMSPageComponent`    | ✅       | `{}`           | [Map of content type → React component](#component-mapping)          |
| `mode`       | `DotCMSPageRendererMode` | ❌       | `'production'` | [Rendering mode ('production' or 'development')](#layout-body-modes) |

#### Client-Side Only Component

> ⚠️ **Important: This is a client-side React component.**
> `DotCMSLayoutBody` uses React features like `useContext`, `useEffect`, and `useState`.
> If you're using a framework that supports Server-Side Rendering (like **Next.js**, **Gatsby**, or **Astro**), you **must** mark the parent component with `"use client"` or follow your framework’s guidelines for using client-side components.
>
> 👉 [Learn more: Next.js – Client Components](https://nextjs.org/docs/getting-started/react-essentials#client-components)

#### Usage

```tsx
import type { DotCMSPageAsset } from '@dotcms/types';
import { DotCMSLayoutBody } from '@dotcms/react';

import { MyBlogCard } from './MyBlogCard';
import { DotCMSProductComponent } from './DotCMSProductComponent';

const COMPONENTS_MAP = {
    Blog: MyBlogCard,
    Product: DotCMSProductComponent
};

const MyPage = ({ pageAsset }: DotCMSPageResponse) => {
    return <DotCMSLayoutBody page={pageAsset} components={COMPONENTS_MAP} />;
};
```

#### Layout Body Modes

-   `production`: Performance-optimized mode that only renders content with explicitly mapped components, leaving unmapped content empty.
-   `development`: Debug-friendly mode that renders default components for unmapped content types and provides visual indicators and console logs for empty containers and missing mappings.

#### Component Mapping

The `DotCMSLayoutBody` component uses a `components` prop to map content type variable names to React components. This allows you to render different components for different content types. Example:

```typescript
const DYNAMIC_COMPONENTS = {
    Blog: MyBlogCard,
    Product: DotCMSProductComponent
};
```

-   Keys (e.g., `Blog`, `Product`): Match your [content type variable names](https://dev.dotcms.com/docs/content-types#VariableNames) in dotCMS
-   Values: Dynamic imports of your React components that render each content type
-   Supports lazy loading through dynamic imports
-   Components must be standalone or declared in a module

> [!TIP]
> Always use the exact content type variable name from dotCMS as the key. You can find this in the Content Types section of your dotCMS admin panel.


### DotCMSEditableText

`DotCMSEditableText` is a component for inline editing of text fields in dotCMS, supporting plain text, text area, and WYSIWYG fields.

| Input        | Type                | Required | Description                                                                                                                                                                                                                 |
| ------------ | ------------------- | -------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `contentlet` | `T extends DotCMSBasicContentlet`  | ✅       | The contentlet containing the editable field                                                                                                   |
| `fieldName`  | `keyof T`                     | ✅       | Name of the field to edit, which must be a valid key of the contentlet type `T`                                                                |
| `mode`       | `'plain' \| 'full'` | ❌       | `plain` (default): Support text editing. Does not show style controls. <br/> `full`: Enables a bubble menu with style options. This mode only works with [`WYSIWYG` fields](https://dev.dotcms.com/docs/the-wysiwyg-field). |
| `format`     | `'text' \| 'html'`  | ❌       | `text` (default): Renders HTML tags as plain text <br/> `html`: Interprets and renders HTML markup                                                                                                                          |

#### Usage

```tsx
import type { DotCMSBasicContentlet } from '@dotcms/types';
import { DotCMSEditableText } from '@dotcms/react';

const MyBannerComponent = ({ contentlet }: { contentlet: DotCMSBasicContentlet }) => {
    const { inode, title, link } = contentlet;

    return (
        <div className="flex overflow-hidden relative justify-center items-center w-full h-96 bg-gray-200">
            <img className="object-cover w-full" src={`/dA/${inode}`} alt={title} />
            <div className="flex absolute inset-0 flex-col justify-center items-center p-4 text-center text-white">
                <h2 className="mb-2 text-6xl font-bold text-shadow">
                    <DotCMSEditableText fieldName="title" contentlet={contentlet} />
                </h2>
                <a
                    href={link}
                    className="p-4 text-xl bg-red-400 rounded-sm transition duration-300 hover:bg-red-500">
                    See more
                </a>
            </div>
        </div>
    );
};

export default MyBannerComponent;
```

#### Editor Integration

-   Detects UVE edit mode and enables inline TinyMCE editing
-   Triggers a `Save` [workflow action](https://dev.dotcms.com/docs/workflows) on blur without needing full content dialog.

#### DotCMSBlockEditorRenderer

`DotCMSBlockEditorRenderer` is a component for rendering [Block Editor](https://dev.dotcms.com/docs/block-editor) content from dotCMS with support for custom block renderers.

| Input             | Type                 | Required | Description                                                                                                |
| ----------------- | -------------------- | -------- | ---------------------------------------------------------------------------------------------------------- |
| `blocks`          | `BlockEditorContent` | ✅       | The [Block Editor](https://dev.dotcms.com/docs/block-editor) content to render                             |
| `customRenderers` | `CustomRenderers`    | ❌       | Custom rendering functions for specific [block types](https://dev.dotcms.com/docs/block-editor#BlockTypes) |
| `className`       | `string`             | ❌       | CSS class to apply to the container                                                                        |
| `style`           | `CSSProperties`      | ❌       | Inline styles for the container                                                                            |

#### Usage

```tsx
import type { DotCMSBasicContentlet } from '@dotcms/types';
import { DotCMSBlockEditorRenderer } from '@dotcms/react';

import { MyCustomBannerBlock } from './MyCustomBannerBlock';
import { MyCustomH1 } from './MyCustomH1';

const CUSTOM_RENDERERS = {
    customBannerBlock: MyCustomBannerBlock,
    h1: MyCustomH1
};

const DetailPage = ({ contentlet }: { contentlet: DotCMSBasicContentlet }) => {
    return (
        <DotCMSBlockEditorRenderer
            blocks={contentlet['YOUR_BLOCK_EDITOR_FIELD']}
            customRenderers={CUSTOM_RENDERERS}
        />
    );
};
```

#### Recommendations

-   Should not be used with [`DotCMSEditableText`](#dotcmseditabletext)
-   Take into account the CSS cascade can affect the look and feel of your blocks.
-   `DotCMSBlockEditorRenderer` only works with [Block Editor fields](https://dev.dotcms.com/docs/block-editor). For other fields, use [`DotCMSEditableText`](#dotcmseditabletext).

📘 For advanced examples, customization options, and best practices, refer to the [DotCMSBlockEditorRenderer README](https://github.com/dotCMS/core/tree/master/core-web/libs/sdk/react/src/lib/components/DotCMSBlockEditorRenderer).

#### DotCMSShow

`DotCMSShow` is a component for conditionally rendering content based on the current UVE mode. Useful for mode-based behaviors outside of render logic.

| Input      | Type        | Required | Description                                                                                                                                                                                                         |
| ---------- | ----------- | -------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `children` | `ReactNode` | ✅       | Content to be conditionally rendered                                                                                                                                                                                |
| `when`     | `UVE_MODE`  | ✅       | The `UVE` mode when content should be displayed: <br/> `UVE_MODE.EDIT`: Only visible in edit mode <br/> `UVE_MODE.PREVIEW`: Only visible in preview mode <br/> `UVE_MODE.PUBLISHED`: Only visible in published mode |

#### Usage

```tsx
import { UVE_MODE } from '@dotcms/types';
import { DotCMSShow } from '@dotcms/react';

const MyComponent = () => {
    return (
        <DotCMSShow when={UVE_MODE.EDIT}>
            <div>This will only render in UVE EDIT mode</div>
        </DotCMSShow>
    );
};
```

📚 Learn more about the `UVE_MODE` enum in the [dotCMS UVE Package Documentation](https://dev.dotcms.com/docs/uve).

### useEditableDotCMSPage

`useEditableDotCMSPage` is a hook that enables real-time page updates when using the Universal Visual Editor.

| Param          | Type                 | Required | Description                                   |
| -------------- | -------------------- | -------- | --------------------------------------------- |
| `pageResponse` | `DotCMSPageResponse` | ✅       | The page data object from `client.page.get()` |

#### Service Lifecycle & Operations

When you use the hook, it:

1. Initializes the UVE with your page data
2. Sets up communication channels with the editor
3. Tracks content changes in real-time
4. Updates your page automatically when:
    - Content is edited inline
    - Blocks are added or removed
    - Layout changes are made
    - Components are moved
5. Cleans up all listeners and connections on destroy

#### Usage

```tsx
'use client';

import { useEditableDotCMSPage, DotCMSLayoutBody } from '@dotcms/react';
import type { DotCMSPageResponse } from '@dotcms/types';

const COMPONENTS_MAP = {
    Blog: BlogComponent,
    Product: ProductComponent
};

export function DotCMSPage({ pageResponse }: { pageResponse: DotCMSPageResponse }) {
    const { pageAsset } = useEditableDotCMSPage(pageResponse);
    return <DotCMSLayoutBody pageAsset={pageAsset} components={COMPONENTS_MAP} />;
}
```

#### useDotCMSShowWhen

`useDotCMSShowWhen` is a hook for conditionally showing content based on the current UVE mode. Useful for mode-based behaviors outside of render logic.

| Param  | Type       | Required | Description                                                                                                                                                                                                         |
| ------ | ---------- | -------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `when` | `UVE_MODE` | ✅       | The `UVE` mode when content should be displayed: <br/> `UVE_MODE.EDIT`: Only visible in edit mode <br/> `UVE_MODE.PREVIEW`: Only visible in preview mode <br/> `UVE_MODE.PUBLISHED`: Only visible in published mode |

#### Usage

```tsx
import { UVE_MODE } from '@dotcms/types';
import { useDotCMSShowWhen } from '@dotcms/react';

const MyEditButton = () => {
    const isEditMode = useDotCMSShowWhen(UVE_MODE.EDIT); // returns a boolean

    if (isEditMode) {
        return <button>Edit</button>;
    }

    return null;
};
```

## Troubleshooting

### Common Issues & Solutions

#### Universal Visual Editor (UVE)

1. **UVE Not Loading**: Page loads but UVE controls are not visible
    - **Possible Causes**:
        - Incorrect UVE configuration
        - Missing API token permissions
        - Missing the `DotCMSEditablePageService` call to enable UVE.
    - **Solutions**:
        - Verify UVE app configuration in dotCMS admin
        - Check API token has edit permissions
        - Ensure `dotcmsUrl` matches your instance URL exactly

#### Missing Content

1. **Components Not Rendering**: Empty spaces where content should appear

    - **Possible Causes**:
        - Missing component mappings
        - Incorrect content type variable names
    - **Solutions**:
        - Check component registration in `components` prop
        - Verify content type variable names match exactly
        - Enable `development` mode for detailed logging

2. **Asset Loading Issues**: Images or files not loading
    - **Possible Causes**:
        - Proxy configuration issues
        - CORS restrictions
    - **Solutions**:
        - Verify proxy settings in `vite.config.ts`
        - Check network tab for CORS errors
        - Ensure `/dA` path is properly configured

#### Development Setup

1. **Build Errors**: `npm install` fails

    - **Solutions**:
        - Clear npm cache: `npm cache clean --force`
        - Delete `node_modules` and reinstall
        - Verify Node.js version compatibility

2. **Runtime Errors**: Console errors about missing imports or components not rendering
    - **Solutions**:
        - Check all imports are from `@dotcms/react`
        - Verify all peer dependencies are installed
        - Update to latest compatible versions

#### Next.js App Router Integration

1. **Server Component Errors**: Errors about React class components in Server Components

    - **Possible Causes**:
        - Using dotCMS components directly in Server Components
        - Missing 'use client' directive
        - Incorrect data fetching pattern
    - **Solutions**:

        1. Split your code into Server and Client Components:

            ```tsx
            // app/page.tsx (Server Component)
            import { DotCMSPage } from '@/components/DotCMSPage';
            import { dotCMSClient } from '@/lib/dotCMSClient';

            export default async function Page() {
                const pageResponse = await dotCMSClient.page.get('/index');
                return <DotCMSPage pageResponse={pageResponse} />;
            }
            ```

            ```tsx
            // components/DotCMSPage.tsx (Client Component)
            'use client';

            import { useEditableDotCMSPage, DotCMSLayoutBody } from '@dotcms/react';
            import type { DotCMSPageResponse } from '@dotcms/types';

            const COMPONENTS_MAP = {
                Blog: BlogComponent,
                Product: ProductComponent
            };

            export function DotCMSPage({ pageResponse }: { pageResponse: DotCMSPageResponse }) {
                const { pageAsset } = useEditableDotCMSPage(pageResponse);
                return <DotCMSLayoutBody pageAsset={pageAsset} components={COMPONENTS_MAP} />;
            }
            ```

        2. Always fetch data in Server Components for better performance
        3. Use Client Components only for rendering dotCMS interactive components

### Debugging Tips

1. **Enable Development Mode**

    ```typescript
    <dotcms-layout-body
        [page]="pageAsset()"
        [components]="components()"
        mode="development"
    />
    ```

    This will:

    - Show detailed error messages
    - Highlight unmapped components
    - Log component lifecycle events

2. **Check Browser Console**

    - Check for errors in the browser console
    - Check for errors in the browser network tab

3. **Network Monitoring**
    - Use browser dev tools to monitor API calls
    - Check for 401/403 errors (auth issues)
    - Verify asset loading paths

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

We offer multiple channels to get help with the dotCMS React SDK:

-   **GitHub Issues**: For bug reports and feature requests, please [open an issue](https://github.com/dotCMS/core/issues/new/choose) in the GitHub repository.
-   **Community Forum**: Join our [community discussions](https://community.dotcms.com/) to ask questions and share solutions.
-   **Stack Overflow**: Use the tag `dotcms-react` when posting questions.
-   **Enterprise Support**: Enterprise customers can access premium support through the [dotCMS Support Portal](https://helpdesk.dotcms.com/support/).

When reporting issues, please include:

-   SDK version you're using
-   React version
-   Minimal reproduction steps
-   Expected vs. actual behavior

## Contributing

GitHub pull requests are the preferred method to contribute code to dotCMS. We welcome contributions to the dotCMS React SDK! If you'd like to contribute, please follow these steps:

1. Fork the repository [dotCMS/core](https://github.com/dotCMS/core)
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

Please ensure your code follows the existing style and includes appropriate tests.

## Licensing

dotCMS comes in multiple editions and as such is dual-licensed. The dotCMS Community Edition is licensed under the GPL 3.0 and is freely available for download, customization, and deployment for use within organizations of all stripes. dotCMS Enterprise Editions (EE) adds several enterprise features and is available via a supported, indemnified commercial license from dotCMS. For the differences between the editions, see [the feature page](http://www.dotcms.com/cms-platform/features).

This SDK is part of dotCMS's dual-licensed platform (GPL 3.0 for Community, commercial license for Enterprise).

[Learn more ](https://www.dotcms.com)at [dotcms.com](https://www.dotcms.com).
