# dotCMS Angular SDK

The `@dotcms/angular` SDK is the DotCMS official Angular library. It empowers Angular developers to build powerful, editable websites and applications in no time.

## Table of Contents

-   [Prerequisites & Setup](#prerequisites--setup)
    -   [dotCMS Instance](#dotcms-instance)
    -   [Create a dotCMS API Key](#create-a-dotcms-api-key)
    -   [Configure The Universal Visual Editor App](#configure-the-universal-visual-editor-app)
    -   [Installation](#installation)
    -   [dotCMS Client Configuration](#dotcms-client-configuration)
    -   [Proxy Configuration for Static Assets](#proxy-configuration-for-static-assets)
    -   [Using dotCMS Images with Angular's `NgOptimizedImage` Directive (Recommended)](#using-dotcms-images-with-angulars-ngoptimizedimage-directive-recommended)
-   [Quickstart: Render a Page with dotCMS](#quickstart-render-a-page-with-dotcms)
    -   [Example Project](#example-project-)
-   [SDK Reference](#sdk-reference)
    -   [DotCMSLayoutBody](#dotcmslayoutbody)
    -   [DotCMSEditableText](#dotcmseditabletext)
    -   [DotCMSBlockEditorRenderer](#dotcmsblockeditorrenderer)
    -   [DotCMSShowWhen](#dotcmsshowwhen)
    -   [DotCMSEditablePageService](#dotcmseditablepageservice)
-   [Troubleshooting](#troubleshooting)
    -   [Common Issues & Solutions](#common-issues--solutions)
    -   [Debugging Tips](#debugging-tips)
    -   [Still Having Issues?](#still-having-issues)
-   [dotCMS Support](#dotcms-support)
-   [How To Contribute](#how-to-contribute)
-   [Changelog](#changelog)
    -   [v1.1.0](#110)
-   [Licensing Information](#licensing-information)

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

### Installation

```bash
npm install @dotcms/angular@latest
```

This will automatically install the required dependencies:
- `@dotcms/uve`: Enables interaction with the [Universal Visual Editor](https://dev.dotcms.com/docs/uve-headless-config) for real-time content editing
- `@dotcms/client`: Provides the core client functionality for fetching and managing dotCMS data

## Configuration

### Basic Configuration

The recommended way to configure the DotCMS client in your Angular application is to use the `provideDotCMSClient` function in your `app.config.ts`:

```ts
import { ApplicationConfig } from '@angular/core';
import { provideDotCMSClient } from '@dotcms/angular';
import { environment } from './environments/environment'; // Assuming your environment variables are here

export const appConfig: ApplicationConfig = {
  providers: [
    provideDotCMSClient({
      dotcmsUrl: environment.dotcmsUrl,
      authToken: environment.authToken,
      siteId: environment.siteId,
      // Optional: Custom HTTP client
      httpClient: (http) => new AngularHttpClient(http)
    })
  ]
};
```

### Custom HTTP Client Configuration

For advanced use cases, you can provide a custom HTTP client implementation that leverages Angular's `HttpClient`:

```ts
import { HttpClient } from '@angular/common/http';
import { BaseHttpClient, DotRequestOptions } from '@dotcms/types';

// Custom HTTP client using Angular's HttpClient
class AngularHttpClient extends BaseHttpClient {
  constructor(private http: HttpClient) {
    super();
  }

  async request<T>(url: string, options?: DotRequestOptions): Promise<T> {
    return this.http.get<T>(url).toPromise();
  }
}

// Configure with custom HTTP client
export const appConfig: ApplicationConfig = {
  providers: [
    provideHttpClient(), // Ensure Angular's HttpClient is available
    provideDotCMSClient({
      dotcmsUrl: environment.dotcmsUrl,
      authToken: environment.authToken,
      siteId: environment.siteId,
      httpClient: (http: HttpClient) => new AngularHttpClient(http)
    })
  ]
};
```

### Using the Client

Then, you can inject the `DotCMSClient` into your components or services:

```ts
import { Component, inject } from '@angular/core';
import { DotCMSClient } from '@dotcms/angular';

@Component({
  selector: 'app-my-component',
  template: `<!-- Your component template -->`
})
export class MyComponent {
  dotcmsClient = inject(DotCMSClient);

  ngOnInit() {
    this.dotcmsClient.page
        .get({ url: '/about-us' })
        .then(({ pageAsset }) => {
            console.log(pageAsset);
        });
  }
}
```

### Proxy Configuration for Static Assets

Configure a proxy to leverage the powerful dotCMS image API, allowing you to resize and serve optimized images efficiently. This enhances application performance and improves user experience, making it a strategic enhancement for your project.

#### 1. Create a Proxy Configuration

Create a `proxy.conf.json` file in your project:

```json
// proxy.conf.json
{
    "/dA": {
        "target": "http://localhost:8080", // Your dotCMS instance URL
        "secure": false, // Set to true if using HTTPS
        "changeOrigin": true // Required for hosting scenarios
    }
}
```

#### 2. Update Angular Configuration

Add the proxy configuration to your `angular.json`:

```json
// angular.json
{
    "projects": {
        "my-app": {
            "architect": {
                "serve": {
                    "builder": "@angular-devkit/build-angular:dev-server",
                    "options": {
                        "proxyConfig": "src/proxy.conf.json"
                    }
                }
            }
        }
    }
}
```

#### 3. Usage in Components

Once configured, image URLs in your components will automatically be proxied to your dotCMS instance:

>📚 Learn more about [Image Resizing and Processing in dotCMS with Angular](https://www.dotcms.com/blog/image-resizing-and-processing-in-dotcms-with-angular-and-nextjs).

```typescript
// /components/my-dotcms-image.component.ts
@Component({
    template: `
        <img [src]="'/dA/' + contentlet.inode" alt="Asset from dotCMS" />
    `
})
class MyDotCMSImageComponent {
    @Input() contentlet: DotCMSBasicContentlet;
}
```

### Using dotCMS Images with Angular's `NgOptimizedImage` Directive (Recommended)

To optimize images served from dotCMS in your Angular app, we recommend using the built-in `NgOptimizedImage` directive. This integration supports automatic image preloading, lazy loading, and improved performance.

We provide a helper function `provideDotCMSImageLoader()` to configure image loading with your dotCMS instance.

#### Setup

Add the image loader to your `app.config.ts`:

```ts
// src/app/app.config.ts
import { ApplicationConfig } from '@angular/core';
import { provideDotCMSClient, provideDotCMSImageLoader } from '@dotcms/angular';
import { environment } from './environments/environment';

export const appConfig: ApplicationConfig = {
  providers: [
    provideDotCMSClient({
      dotcmsUrl: environment.dotcmsUrl,
      authToken: environment.authToken,
      siteId: environment.siteId
    }),
    provideDotCMSImageLoader(environment.dotcmsUrl)
  ]
};
```

#### Usage

Once configured, you can use the `NgOptimizedImage` directive to render dotCMS images:

```ts
// src/components/my-dotcms-image.component.ts
@Component({
  selector: 'my-dotcms-image',
  template: `
    <img [ngSrc]="imagePath" alt="Asset from dotCMS" width="400" height="300" />
  `,
  standalone: true
})
export class MyDotCMSImageComponent {
  @Input() contentlet!: DotCMSBasicContentlet;

  get imagePath() {
    return this.contentlet.image.versionPath;
  }
}
```

The image loader automatically handles:
- **Automatic resizing** based on the `width` and `height` attributes
- **Quality optimization** with a default quality of 50 for better performance
- **Language-specific images** using the current language context
- **Responsive images** that adapt to different screen sizes

> 📚 Learn more about [`NgOptimizedImage`](https://angular.dev/guide/image-optimization)

## Quickstart: Render a Page with dotCMS

The following example demonstrates how to quickly set up a basic dotCMS page renderer in your Angular application. This example shows how to:

-   Create a standalone component that renders a dotCMS page
-   Set up dynamic component loading for different content types
-   Handle both regular page viewing and editor mode
-   Subscribe to real-time page updates when in the Universal Visual Editor

```typescript
// /src/app/pages/dotcms-page.component.ts
import { Component, signal } from '@angular/core';

import { DotCMSLayoutBody, DotCMSEditablePageService} from '@dotcms/angular';
import { getUVEState } from '@dotcms/uve';
import { DotCMSPageAsset } from '@dotcms/types';

import { DOTCMS_CLIENT_TOKEN } from './app.config';

const DYNAMIC_COMPONENTS = {
    Blog: import('./blog.component').then(c => c.BlogComponent),
    Product: import('./product.component').then(c => c.ProductComponent)
};

@Component({
    selector: 'app-pages',
    standalone: true,
    imports: [DotCMSLayoutBody],
    providers: [DotCMSEditablePageService, DOTCMS_CLIENT_TOKEN],
    template: `
        @if (pageAsset()) {
            <dotcms-layout-body
                [pageAsset]="pageAsset"
                [components]="components()"
            />
        } @else {
            <div>Loading...</div>
        }
    `
})
export class PagesComponent {
    private readonly dotCMSClient: DotCMSClient = inject(DOTCMS_CLIENT_TOKEN);
    private readonly editablePageService = inject(DotCMSEditablePageService);
    readonly components = signal(DYNAMIC_COMPONENTS);
    readonly pageAsset = signal<DotCMSPageAsset | null>(null);

    ngOnInit() {
        this.dotCMSClient.page
            .get({ url: '/my-page' })
            .then(({ pageAsset }) => {
                if(getUVEState()) {
                   this.#subscribeToPageUpdates(response);
                   return;
               }

                this.pageAsset.set(pageAsset);
            });
    }

    #subscribeToPageUpdates(response: DotCMSPageResponse) {
        this.editablePageService
            .listen(response)
            .subscribe({ pageAsset } => this.pageAsset.set(pageAsset));
    }
}
```

### Example Project 🚀

Looking to get started quickly? We've got you covered! Our [Angular starter project](https://github.com/dotCMS/core/tree/main/examples/angular) is the perfect launchpad for your dotCMS + Angular journey. This production-ready template demonstrates everything you need:

📦 Fetch and render dotCMS pages with best practices
🧩 Register and manage components for different content types
🔍 Listing pages with search functionality
📝 Detail pages for blogs
📈 Image and assets optimization for better performance
✨ Enable seamless editing via the Universal Visual Editor (UVE)
⚡️ Leverage Angular's dependency injection and signals for optimal performance

> [!TIP]
> This starter project is more than just an example, it follows all our best practices. We highly recommend using it as the base for your next dotCMS + Angular project!

## SDK Reference

All components, directives, and services should be imported from `@dotcms/angular`.

### DotCMSLayoutBody

`DotCMSLayoutBody` is a component used to render the layout for a DotCMS page, supporting both production and development modes.

| Input        | Type                     | Required | Default        | Description                                    |
|--------------|--------------------------|----------|----------------|------------------------------------------------|
| `page`       | `DotCMSPageAsset`       | ✅       | -              | The page asset containing the layout to render |
| `components` | `DotCMSPageComponent`    | ✅       | `{}`           | [Map of content type → Angular component](#component-mapping)        |
| `mode`       | `DotCMSPageRendererMode` | ❌       | `'production'` | [Rendering mode ('production' or 'development')](#layout-body-modes) |

#### Usage

```typescript
import { Component, signal } from '@angular/core';
import { DotCMSPageAsset } from '@dotcms/types';
import { DotCMSLayoutBody } from '@dotcms/angular';

import { DOTCMS_CLIENT_TOKEN } from './app.config';

@Component({
    template: `
        <dotcms-layout-body [page]="pageAsset()" [components]="components()" mode="development" />
    `
})
export class MyPageComponent {
    protected readonly components = signal({
        Blog: import('./blog.component').then((c) => c.BlogComponent)
    });
    protected readonly pageAsset = signal<DotCMSPageAsset | null>(null);
    private readonly dotCMSClient = inject(DOTCMS_CLIENT_TOKEN);

    ngOnInit() {
        this.dotCMSClient.page.get({ url: '/my-page' }).then(({ pageAsset }) => {
            this.pageAsset.set(pageAsset);
        });
    }
}
```

#### Layout Body Modes

-   `production`: Performance-optimized mode that only renders content with explicitly mapped components, leaving unmapped content empty.
-   `development`: Debug-friendly mode that renders default components for unmapped content types and provides visual indicators and console logs for empty containers and missing mappings.

#### Component Mapping

The `DotCMSLayoutBody` component uses a `components` input to map content type variable names to Angular components. This allows you to render different components for different content types. Example:

```typescript
const DYNAMIC_COMPONENTS = {
    Blog: import('./blog.component').then((c) => c.BlogComponent),
    Product: import('./product.component').then((c) => c.ProductComponent)
};
```

-   Keys (e.g., `Blog`, `Product`): Match your [content type variable names](https://dev.dotcms.com/docs/content-types#VariableNames) in dotCMS
-   Values: Dynamic imports of your Angular components that render each content type
-   Supports lazy loading through dynamic imports
-   Components must be standalone or declared in a module

> [!TIP]
> Always use the exact content type variable name from dotCMS as the key. You can find this in the Content Types section of your dotCMS admin panel.

### DotCMSEditableText

`DotCMSEditableText` is a component for inline editing of text fields in dotCMS, supporting plain text, text area, and WYSIWYG fields.

| Input        | Type                | Required | Description                                                                                                                                                                                                                 |
|--------------|---------------------|----------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `contentlet` | `T extends DotCMSBasicContentlet`  | ✅       | The contentlet containing the editable field                                                                                                   |
| `fieldName`  | `keyof T`                     | ✅       | Name of the field to edit, which must be a valid key of the contentlet type `T`                                                                |
| `mode`       | `'plain' \| 'full'` | ❌       | `plain` (default): Support text editing. Does not show style controls. <br/> `full`: Enables a bubble menu with style options. This mode only works with [`WYSIWYG` fields](https://dev.dotcms.com/docs/the-wysiwyg-field). |
| `format`     | `'text' \| 'html'`  | ❌       | `text` (default): Renders HTML tags as plain text <br/> `html`: Interprets and renders HTML markup                                                                                                                          |

#### Usage

```typescript
import { Component, Input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { DotCMSBasicContentlet } from '@dotcms/types';
import { DotCMSEditableTextComponent } from '@dotcms/angular';

@Component({
    selector: 'app-your-component',
    imports: [RouterLink, NgOptimizedImage, DotCMSEditableTextComponent],
    template: `
        <div
            class="flex overflow-hidden relative justify-center items-center w-full h-96 bg-gray-200">
            <img
                class="object-cover w-full"
                [src]="'/dA/' + contentlet().inode"
                [alt]="contentlet().title" />
            <div
                class="flex absolute inset-0 flex-col justify-center items-center p-4 text-center text-white">
                <h2 class="mb-2 text-6xl font-bold text-shadow">
                    <dotcms-editable-text fieldName="title" [contentlet]="contentlet()" />
                </h2>
                <a
                    class="p-4 text-xl bg-red-400 rounded-sm transition duration-300 hover:bg-red-500"
                    [routerLink]="contentlet().link">
                    See more
                </a>
            </div>
        </div>
    `
})
export class MyBannerComponent {
    @Input() contentlet: DotCMSBasicContentlet;
}
```

#### Editor Integration

-   Detects UVE edit mode and enables inline TinyMCE editing
-   Triggers a `Save` [workflow action](https://dev.dotcms.com/docs/workflows) on blur without needing full content dialog.

### DotCMSBlockEditorRenderer

`DotCMSBlockEditorRenderer` is a component for rendering [Block Editor](https://dev.dotcms.com/docs/block-editor) content from dotCMS with support for custom block renderers.

| Input             | Type                 | Required | Description                                                                                                |
|-------------------|----------------------|----------|------------------------------------------------------------------------------------------------------------|
| `blocks`          | `BlockEditorContent` | ✅       | The [Block Editor](https://dev.dotcms.com/docs/block-editor) content to render                             |
| `customRenderers` | `CustomRenderer`     | ❌       | Custom rendering functions for specific [block types](https://dev.dotcms.com/docs/block-editor#BlockTypes) |
| `className`       | `string`            | ❌       | CSS class to apply to the container                                                                        |
| `style`           | `CSSProperties`      | ❌       | Inline styles for the container                                                                            |

#### Usage

```typescript
import { DotCMSBasicContentlet } from '@dotcms/types';
import { DotCMSBlockEditorRenderer } from '@dotcms/angular';

const CUSTOM_RENDERERS = {
    customBlock: import('./custom-block.component').then((c) => c.CustomBlockComponent),
    h1: import('./custom-h1.component').then((c) => c.CustomH1Component)
};

@Component({
    selector: 'app-your-component',
    imports: [DotCMSShowWhen],
    template: `
        <dotcms-block-editor-renderer
            [blocks]="contentlet.myBlockEditorField"
            [customRenderers]="customRenderers()" />
    `
})
export class MyBannerComponent {
    @Input() contentlet: DotCMSBasicContentlet;
    readonly customRenderers = signal(CUSTOM_RENDERERS);
}
```

#### Recommendations

-   Should not be used with [`DotCMSEditableText`](#dotcmseditabletext)
-   Take into account the CSS cascade can affect the look and feel of your blocks.
-   `DotCMSBlockEditorRenderer` only works with [Block Editor fields](https://dev.dotcms.com/docs/block-editor). For other fields, use [`DotCMSEditableText`](#dotcmseditabletext).

📘 For advanced examples, customization options, and best practices, refer to the [DotCMSBlockEditorRenderer README](https://github.com/dotCMS/core/tree/master/core-web/libs/sdk/angular/src/lib/components/DotCMSBlockEditorRenderer).


### DotCMSShowWhen

`DotCMSShowWhen` is a `directive` for conditionally showing content based on the current UVE mode. Useful for mode-based behaviors outside of render logic.

| Input  | Type       | Required | Description                                                                                                                                                                                                         |
|--------|------------|----------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `when` | `UVE_MODE` | ✅       | The `UVE` mode when content should be displayed: <br/> `UVE_MODE.EDIT`: Only visible in edit mode <br/> `UVE_MODE.PREVIEW`: Only visible in preview mode <br/> `UVE_MODE.PUBLISHED`: Only visible in published mode |

#### Usage

```typescript
import { UVE_MODE } from '@dotcms/types';
import { DotCMSShowWhen } from '@dotcms/angular';

@Component({
    selector: 'app-your-component',
    imports: [DotCMSShowWhen],
    template: `
        <div *dotCMSShowWhen="UVE_MODE.EDIT">Only visible in edit mode</div>
    `
})
export class YourComponent {}
```

📚 Learn more about the `UVE_MODE` enum in the [dotCMS UVE Package Documentation](https://dev.dotcms.com/docs/uve).

### DotCMSEditablePageService

The `DotCMSEditablePageService` enables real-time page updates when using the Universal Visual Editor. It provides a single method `listen` that returns an Observable of page changes.

| Param          | Type                 | Required | Description                                   |
|----------------|----------------------|----------|-----------------------------------------------|
| `pageResponse` | `DotCMSPageResponse` | ✅       | The page data object from `client.page.get()` |

#### Service Lifecycle & Operations

When you use the `listen` method, the service:

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

```typescript
import { Subscription } from 'rxjs';
import { Component, OnDestroy, OnInit, signal, inject } from '@angular/core';

import { getUVEState } from '@dotcms/uve';
import { DotCMSPageAsset } from '@dotcms/types';
import { DotCMSLayoutBody, DotCMSEditablePageService, DotCMSClient } from '@dotcms/angular';

@Component({
    imports: [DotCMSLayoutBody],
    providers: [DotCMSEditablePageService],
    template: `
        @if (pageAsset()) {
            <dotcms-layout-body [page]="pageAsset()" [components]="components()" />
        } @else {
            <div>Loading...</div>
        }
    `
})
export class PageComponent implements OnInit, OnDestroy {
    private subscription?: Subscription;
    private readonly dotCMSClient = inject(DOTCMS_CLIENT_TOKEN);
    private readonly editablePageService = inject(DotCMSEditablePageService);
    readonly pageAsset = signal<DotCMSPageAsset | null>(null);

    ngOnInit() {
        this.dotCMSClient.page.get({ url: '/about-us' }).then((pageResponse) => {
            // Only subscribe to changes when in the editor
            if (getUVEState()) {
                this.subscription = this.editablePageService
                    .listen(pageResponse)
                    .subscribe(({ pageAsset }) => {
                        this.pageAsset.set(pageAsset);
                    });
            } else {
                const { pageAsset } = pageResponse;
                this.pageAsset.set(pageAsset);
            }
        });
    }

    ngOnDestroy() {
        this.subscription?.unsubscribe();
    }
}
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
        - Verify proxy settings in `angular.json`
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
        - Check all imports are from `@dotcms/angular`
        - Verify all peer dependencies are installed
        - Update to latest compatible versions

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

## dotCMS Support

We offer multiple channels to get help with the dotCMS Angular SDK:

-   **GitHub Issues**: For bug reports and feature requests, please [open an issue](https://github.com/dotCMS/core/issues/new/choose) in the GitHub repository.
-   **Community Forum**: Join our [community discussions](https://community.dotcms.com/) to ask questions and share solutions.
-   **Stack Overflow**: Use the tag `dotcms-angular` when posting questions.
-   **Enterprise Support**: Enterprise customers can access premium support through the [dotCMS Support Portal](https://helpdesk.dotcms.com/support/).

When reporting issues, please include:

-   SDK version you're using
-   Angular version
-   Minimal reproduction steps
-   Expected vs. actual behavior

## How To Contribute

GitHub pull requests are the preferred method to contribute code to dotCMS. We welcome contributions to the DotCMS UVE SDK! If you'd like to contribute, please follow these steps:

1. Fork the repository [dotCMS/core](https://github.com/dotCMS/core)
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

Please ensure your code follows the existing style and includes appropriate tests.

## Changelog

### [1.1.0] - 2024-XX-XX

#### ✨ Added - Enhanced Client Architecture

**New Features:**
- Added `DotCMSAngularProviderConfig` for Angular-specific configuration
- Custom HTTP client support with Angular's `HttpClient` integration
- Enhanced image optimization with automatic quality control (default: 50%)
- Improved provider system with better error handling and type safety

**No Breaking Changes:**
- `DotCMSClient` remains the same - no migration required
- All existing code continues to work without changes
- New features are additive and optional

#### 🔄 Enhanced - Provider System

**Improvements:**
- Enhanced `provideDotCMSClient` with optional custom HTTP client factory
- Added `DotCMSAngularProviderConfig` interface for better type safety
- Improved error handling and HTTP client architecture

**New Configuration Options:**
```typescript
provideDotCMSClient({
  dotcmsUrl: environment.dotcmsUrl,
  authToken: environment.authToken,
  siteId: environment.siteId,
  httpClient: (http: HttpClient) => new AngularHttpClient(http) // New!
})
```

#### 🎨 Enhanced - Image Loading

**Improvements:**
- Automatic quality optimization for better performance
- Enhanced responsive image support
- Improved language-specific image handling
- Better integration with Angular's `NgOptimizedImage` directive

### Licensing Information

dotCMS comes in multiple editions and as such is dual-licensed. The dotCMS Community Edition is licensed under the GPL 3.0 and is freely available for download, customization, and deployment for use within organizations of all stripes. dotCMS Enterprise Editions (EE) adds several enterprise features and is available via a supported, indemnified commercial license from dotCMS. For the differences between the editions, see [the feature page](http://www.dotcms.com/cms-platform/features).

This SDK is part of dotCMS's dual-licensed platform (GPL 3.0 for Community, commercial license for Enterprise).

[Learn more ](https://www.dotcms.com)at [dotcms.com](https://www.dotcms.com).
