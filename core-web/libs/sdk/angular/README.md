# dotCMS Angular SDK

The `@dotcms/angular` SDK is the official Angular integration library for dotCMS, designed to empower Angular developers to build powerful, editable websites and applications with minimal effort.

## Table of Contents

* [Quickstart](#quickstart)
* [Example Project](#example-project)
* [What Is It?](#what-is-it)
* [Installation](#installation)
* [Key Concepts](#key-concepts)
* [API Reference](#api-reference)
  * [Components](#components)
    * [DotCMSLayoutComponent](#dotcmslayoutcomponent)

* [FAQ](#faq)
  * [How do I configure and use the dotCMS Client in my Angular application?](#how-do-i-configure-and-use-the-dotcms-client-in-my-angular-application)
  * [What are the differences between UVE modes?](#what-are-the-differences-between-uve-modes)
  * [What if my components don't render?](#what-if-my-components-dont-render)
* [dotCMS Support](#dotcms-support)
* [How To Contribute](#how-to-contribute)
* [Licensing Information](#licensing-information)

## Quickstart

Install the SDK and required dependencies:

```bash
npm install @dotcms/angular @dotcms/uve @dotcms/client @dotcms/types
```

Set up your dotCMS configuration:

```typescript
import { ClientConfig } from '@dotcms/client';

const DOTCMS_CLIENT_CONFIG: ClientConfig = {
    dotcmsUrl: environment.dotcmsUrl,
    authToken: environment.authToken,
    siteId: environment.siteId
};
```

Render a dotCMS page:

```typescript
@Component({
    selector: 'app-pages',
    template: `
        <dotcms-layout-body
            [pageAsset]="pageAsset"
            [components]="components()"
        />
    `
})
export class PagesComponent {
    DYNAMIC_COMPONENTS: DotCMSPageComponent = {
        Blog: import('./blog.component').then(c => c.BlogComponent),
        Product: import('./product.component').then(c => c.ProductComponent)
    };

    components = signal(this.DYNAMIC_COMPONENTS);
    editorConfig = signal<EditorConfig>({ params: { depth: 2 } });
    pageAsset = signal<DotCMSPageAsset | null>(null);

    ngOnInit() {
        this.client.page
            .get({ url: '/my-page' })
            .then(({ pageAsset }) => {
                this.pageAsset.set(pageAsset);
            });
    }
}
```

▶️ Want a full working app? Check the [Angular example project](https://github.com/dotCMS/core/tree/main/examples/angular).

---

## Example Project

We maintain a complete [Angular starter project](https://github.com/dotCMS/core/tree/main/examples/angular) that demonstrates how to:

* Fetch and render dotCMS pages
* Register components for different content types
* Enable editing via the Universal Visual Editor (UVE)
* Use Angular's dependency injection and signals

You can clone it directly from our repository:

```bash
git clone https://github.com/dotCMS/core.git
cd core/examples/angular
```

This is the fastest way to get up and running with dotCMS + Angular.

---

## What Is It?

The `@dotcms/angular` SDK bridges dotCMS content management with Angular's component-based system. It includes:

* Components for rendering dotCMS pages and content
* Integration with Angular's dependency injection system
* Support for live editing in the Universal Visual Editor (UVE)
* TypeScript types for enhanced development experience

Use it to:

* Render dotCMS content with minimal setup
* Build fully editable Angular pages
* Integrate seamlessly with dotCMS's Universal Visual Editor (UVE)

---

## Installation

```bash
npm install @dotcms/angular
```

### Peer Dependencies

Make sure to also install:

```bash
npm install @dotcms/uve @dotcms/client @dotcms/types
```

---

## Key Concepts

| Term                  | Description                                                          |
|----------------------|----------------------------------------------------------------------|
| `pageAsset`          | The actual page layout data used by the renderer                     |
| `DotCMSPageComponent`| Type for mapping content types to Angular components                 |
| `EditorConfig`       | Configuration for the Universal Visual Editor integration            |
| UVE                  | Universal Visual Editor, dotCMS's editing interface                  |

---

## API Reference

### Components

#### DotCMSLayoutBody

**Overview**: Component used to render the layout for a DotCMS page, supporting both production and development modes.

**Inputs**:

| Input        | Type                    | Required | Default        | Description                                     |
|--------------|-------------------------|----------| ---------------|------------------------------------------------|
| `page`       | `DotCMSPageAsset`      | ✅        | -              | The page asset containing the layout to render  |
| `components` | `DotCMSPageComponent`   | ✅        | `{}`           | Map of content type → Angular component         |
| `mode`       | `DotCMSPageRendererMode`| ❌        | `'production'` | Rendering mode ('production' or 'development')  |

**Usage**:

```typescript
@Component({
    template: `
        <dotcms-layout-body
            [page]="pageAsset()"
            [components]="components()"
            mode="development"
        />
    `
})
export class PageComponent {
    components = signal<DotCMSPageComponent>({
        Blog: import('./blog.component').then(c => c.BlogComponent)
    });
    pageAsset = signal<DotCMSPageAsset | null>(null);

    constructor(private readonly client: DotCmsClient) {}

    ngOnInit() {
        this.client.page
            .get({ url: '/my-page' })
            .then(({ pageAsset }) => {
                this.pageAsset.set(pageAsset);
            });
    }
}
```

**Component Mapping**:

* The key `Blog` is the content type variable in dotCMS
* The value `BlogComponent` is the Angular component that renders the blog post

**Mode**:

* `development` mode is used for development and debugging
* `production` mode is used for production


**Editor Integration**:

* Automatically detects if it's inside dotCMS UVE
* Works with `DotCMSEditablePageService` for real-time updates

**Common Issues**:

* Missing component mappings cause unrendered containers
* Editing layout structure manually may break UVE sync
* Modifying the layout structure directly may break the editor's ability to track changes


#### DotCMSEditableText

**Overview**: Component for inline editing of text fields in dotCMS, supporting plain text, text area, and WYSIWYG fields.

**Inputs**:

| Input        | Type                | Required | Description                                       |
|--------------|---------------------|----------|---------------------------------------------------|
| `contentlet` | `DotCMSContentlet`  | ✅        | The contentlet containing the editable field      |
| `fieldName`  | `string`            | ✅        | Name of the field to edit                         |
| `mode`       | `'plain' \| 'full'` | ❌        | Editor mode (default: 'plain')                    |
| `format`     | `'text' \| 'html'`  | ❌        | Output format (default: 'text')                   |

**Usage**:

```typescript
<dotcms-editable-text
    [contentlet]="contentlet"
    fieldName="title"
    mode="plain"
/>
```

**Editor Integration**:

* Detects UVE edit mode and enables inline TinyMCE editing
* Saves on blur without needing full content dialog

**Common Issues**:

* `mode` and `format` must match the actual field type
* `full` mode is only supported for `WYSIWYG` fields. For `text` and `text_area` fields, use `plain` mode.
* Should not be used on Block Editor fields


#### DotCMSBlockEditorRenderer

**Overview**: Component for rendering Block Editor content from dotCMS with support for custom block renderers.

**Inputs**:

| Input              | Type                  | Required | Description                                      |
|-------------------|----------------------|-----------|--------------------------------------------------|
| `blocks`          | `BlockEditorContent`  | ✅        | The block editor content to render               |
| `customRenderers` | `CustomRenderers`     | ❌        | Custom rendering functions for specific blocks   |
| `className`       | `string`             | ❌        | CSS class to apply to the container              |
| `style`           | `CSSProperties`       | ❌        | Inline styles for the container                  |

**Usage**:

```typescript
<dotcms-block-editor-renderer
    [blocks]="contentlet.blockField"
    [customRenderers]="myCustomRenderers"
/>
```


**Editor Integration**:

* Renders exactly as configured in dotCMS editor
* Supports custom renderers for block types

**Common Issues**:

* Should not be used with DotCMSEditableText
* Inherits styling from parent containers, which may cause conflicts
* `DotCMSBlockEditorRenderer` only works with Block Editor fields. For other fields, use [`DotCMSEditableText`](#dotcmseditabletext).

📘 For advanced examples, customization options, and best practices, refer to the [DotCMSBlockEditorRenderer README](https://github.com/dotCMS/core/tree/master/core-web/libs/sdk/angular/src/lib/components/DotCMSBlockEditorRenderer).

### Directives

#### DotCMSShowWhen

**Overview**: Directive for conditionally showing content based on the current UVE mode.

**Input**:

| Input    | Type       | Required | Description                                    |
|----------|------------|----------|------------------------------------------------|
| `when`   | `UVE_MODE` | ✅        | The UVE mode when content should be displayed  |

**Usage**:

```typescript
<div *dotCMSShowWhen="UVE_MODE.EDIT">
    Only visible in edit mode
</div>
```

**Editor Integration**:

* Useful for mode-based behaviors outside of render logic

**Common Issues**:

* Requires proper mode enum from `@dotcms/types`

### Services

#### DotCMSEditablePageService

**Overview**: Service for managing editable page state and interactions with the Universal Visual Editor.

**Methods**:

| Method                  | Parameters                    | Returns              | Description                                        |
|------------------------|-------------------------------|---------------------|----------------------------------------------------|
| `init`                 | `pageResponse: PageResponse`  | `void`             | Initializes the editable page service              |
| `getPageAsset`         | -                            | `DotCMSPageAsset`   | Returns the current page asset                     |
| `updatePageAsset`      | `asset: DotCMSPageAsset`     | `void`             | Updates the current page asset                     |
| `isEditMode`           | -                            | `boolean`           | Checks if the page is in edit mode                 |

**Usage**:

```typescript
@Component({...})
export class PageComponent {
    constructor(private editablePageService: DotCMSEditablePageService) {
        this.editablePageService.init(pageResponse);
    }
}
```

**Editor Integration**:

* Syncs changes in real time
* Detects edit mode, initializes UVE, listens for updates

**Common Issues**:

* Only works with `pageResponse` from `@dotcms/client`
* Has no effect outside the editor

**How It Works**

1\. Taking the page asset from the `pageResponse` parameter

2\. Initializing the Universal Visual Editor (UVE) with the parameters from the page asset

3\. Setting up event listeners for the UVE to detect content changes

4\. Retrieving and returning the updated page asset whenever changes are made in the editor

### Configuration

#### Provider Setup

Configure the dotCMS client in your application:

```typescript
import { InjectionToken } from '@angular/core';
import { ClientConfig, DotCmsClient } from '@dotcms/client';

export const DOTCMS_CLIENT_TOKEN = new InjectionToken<DotCmsClient>('DOTCMS_CLIENT');

export const appConfig: ApplicationConfig = {
    providers: [
        {
            provide: DOTCMS_CLIENT_TOKEN,
            useValue: DotCmsClient.init(DOTCMS_CLIENT_CONFIG),
        }
    ],
};
```

#### Client Usage

Inject and use the client in your components:

```typescript
export class YourComponent {
    private readonly client = inject(DOTCMS_CLIENT_TOKEN);

    ngOnInit() {
        this.client.page
            .get({ url: '/my-page' })
            .then(response => {
                // Handle page data
            });
    }
}
```

## FAQ

### How do I configure and use the dotCMS Client in my Angular application?

The dotCMS Client needs to be properly configured and provided at the application level for all dotCMS components to work correctly.

1. **Setup the Client Configuration**:

```typescript
import { ClientConfig } from '@dotcms/client';

const DOTCMS_CLIENT_CONFIG: ClientConfig = {
    dotcmsUrl: environment.dotcmsUrl,  // Your dotCMS instance URL
    authToken: environment.authToken,   // Authentication token
    siteId: environment.siteId         // Your site identifier
};
```

2. **Configure the Provider** in your `app.config.ts`:

```typescript
import { InjectionToken } from '@angular/core';
import { ClientConfig, DotCmsClient } from '@dotcms/client';

export const DOTCMS_CLIENT_TOKEN = new InjectionToken<DotCmsClient>('DOTCMS_CLIENT');

export const appConfig: ApplicationConfig = {
    providers: [
        {
            provide: DOTCMS_CLIENT_TOKEN,
            useValue: DotCmsClient.init(DOTCMS_CLIENT_CONFIG),
        }
    ],
};
```

3. **Use in Components**:

```typescript
@Component({...})
export class YourComponent {
    private readonly client = inject(DOTCMS_CLIENT_TOKEN);

    ngOnInit() {
        this.client.page
            .get({ url: '/my-page' })
            .then(response => {
                // Handle page data
            });
    }
}
```

**Common Issues**:
- Ensure environment variables are properly set for `dotcmsUrl`, `authToken`, and `siteId`
- The `DOTCMS_CLIENT_TOKEN` must be provided at the application root level
- Check for CORS configuration if accessing a remote dotCMS instance
- Verify the auth token has the necessary permissions

### What are the differences between UVE modes?

The dotCMS Universal Visual Editor (UVE) supports three modes:

1. **EDIT Mode**
   * Content is visible when editing within dotCMS's editor
   * Used for edit controls and admin tools
   * Only visible to content editors

2. **PREVIEW Mode**
   * Content visible in the preview panel
   * Used for preview-only content and workflow messaging

3. **LIVE Mode**
   * Shows content as it appears on the live site
   * Used for final verification before publishing

### What if my components don't render?

Common solutions for rendering issues:

1. Verify component registration:
```typescript
const DYNAMIC_COMPONENTS: DotCMSPageComponent = {
    Blog: import('./blog.component').then(c => c.BlogComponent)
};
```

2. Check for console errors related to component loading
3. Ensure all required dependencies are properly installed
4. Verify the `pageAsset` structure is correct

## dotCMS Support

We offer multiple channels for support:

* **GitHub Issues**: For bug reports and feature requests, please [open an issue](https://github.com/dotCMS/core/issues/new/choose)
* **Community Forum**: Join our [community discussions](https://community.dotcms.com/)
* **Stack Overflow**: Use the tag `dotcms-angular`

Enterprise customers can access premium support through the [dotCMS Support Portal](https://dev.dotcms.com/docs/help).

---

## How To Contribute

We welcome contributions to the dotCMS Angular SDK! To contribute:

1. Fork the repository [dotCMS/core](https://github.com/dotCMS/core)
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

Before any pull requests can be accepted, you'll need to agree to the [dotCMS Contributor's Agreement](https://gist.github.com/wezell/85ef45298c48494b90d92755b583acb3).

---

## Licensing Information

dotCMS comes in multiple editions and as such is dual-licensed. The dotCMS Community Edition is licensed under the GPL 3.0 and is freely available for download, customization, and deployment for use within organizations of all stripes. dotCMS Enterprise Editions (EE) adds several enterprise features and is available via a supported, indemnified commercial license from dotCMS. For the differences between the editions, see [the feature page](http://www.dotcms.com/cms-platform/features).

[Learn more](https://www.dotcms.com) at [dotcms.com](https://www.dotcms.com).
