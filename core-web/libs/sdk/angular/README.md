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
    * [DotCMSEditableText](#dotcmseditabletext)
    * [DotCMSBlockEditorRenderer](#dotcmsblockeeditorrenderer)
    * [DotCMSShowWhen](#dotcmsshowwhen)
  * [Directives](#directives)
    * [DotCMSShowWhen](#dotcmsshowwhen
  * [Services](#services)
    * [DotCMSEditablePageService](#dotcmseditablepageservice))

* [FAQ](#faq)
  * [How do I configure and use the dotCMS Client in my Angular application?](#how-do-i-configure-and-use-the-dotcms-client-in-my-angular-application)
  * [What are the differences between UVE modes?](#what-are-the-differences-between-uve-modes)
  * [What if my components don't render?](#what-if-my-components-dont-render)
  * [How do I properly handle images in dotCMS components?](#how-do-i-properly-handle-images-in-dotcms-components)z
* [dotCMS Support](#dotcms-support)
* [How To Contribute](#how-to-contribute)
* [Licensing Information](#licensing-information)

## Quickstart

Install the SDK and required dependencies:

```bash
npm install @dotcms/angular@next @dotcms/uve@next @dotcms/client@next @dotcms/types@next
```

Set up your dotCMS configuration:

```typescript
import { DotCMSClientConfig } from '@dotcms/types';

const DOTCMS_CLIENT_CONFIG: DotCMSClientConfig = {
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
npm install @dotcms/angular@next
```

### Peer Dependencies

Make sure to also install:

```bash
npm install @dotcms/uve@next @dotcms/client@next @dotcms/types@next
```

---

## Key Concepts

| Term                  | Description                                                          |
|----------------------|----------------------------------------------------------------------|
| `pageAsset`          | The actual page layout data used by the renderer                     |
| `DotCMSPageComponent`| Type for mapping content types to Angular components                 |
| UVE                  | Universal Visual Editor, dotCMS's editing interface                  |
| Editable Page        | A page enhanced via `DotCMSEditablePageService` for real-time updates|

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
import { DotCMSPageAsset } from '@dotcms/types';
import { DotCMSLayoutBody } from '@dotcms/angular/next';

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
import { DotCMSBasicContentlet } from '@dotcms/types';
import { DotCMSEditableText } from '@dotcms/angular/next';

@Component({
    selector: 'app-your-component',
    imports: [DotCMSShowWhen],
    template: `
        <dotcms-editable-text
            [contentlet]="contentlet"
            fieldName="title"
            mode="plain"
        />
    `
})
export class YourComponent {
    @Input() contentlet: DotCMSBasicContentlet;
}
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
import { DotCMSBasicContentlet } from '@dotcms/types';
import { DotCMSBlockEditorRenderer } from '@dotcms/angular/next';

const myCustomRenderers: CustomRenderers = {
    customBlock: import('./custom-block.component').then(c => c.CustomBlockComponent)
};

@Component({
    selector: 'app-your-component',
    imports: [DotCMSShowWhen],
    template: `
        <dotcms-block-editor-renderer
            [blocks]="contentlet.myBlockEditorField"
            [customRenderers]="myCustomRenderers"
        />
    `
})
export class YourComponent {
    @Input() contentlet: DotCMSBasicContentlet;
}
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
import { UVE_MODE } from '@dotcms/types';
import { DotCMSShowWhen } from '@dotcms/angular/next';

@Component({
    selector: 'app-your-component',
    imports: [DotCMSShowWhen],
    template: `
        <div *dotCMSShowWhen="UVE_MODE.EDIT">
            Only visible in edit mode
        </div>
    `
})
export class YourComponent {
}
```

**Editor Integration**:

* Useful for mode-based behaviors outside of render logic

**Common Issues**:

* Requires proper mode enum from `@dotcms/types`

### Services

#### DotCMSEditablePageService

**Overview**: Service for managing editable page state and interactions with the Universal Visual Editor.

**Methods**:

| Method      | Parameters                    | Returns                                        | Description                                        |
|------------|-------------------------------|------------------------------------------------|----------------------------------------------------|
| `listen`    | `response?: DotCMSPageResponse` | `Observable<DotCMSPageResponse \| null>`      | Listens for changes to an editable page            |

**Usage**:

```typescript
import { DotCMSEditablePageService } from '@dotcms/angular/next';

@Component({...})
export class PageComponent implements OnInit, OnDestroy {
    private subscription: Subscription;

    constructor(private editablePageService: DotCMSEditablePageService) {}

    ngOnInit() {
        this.client.page
            .get({ url: '/my-page' })
            .then(response => {
                if(getUVEState()) {
                    this.#listenEditorChanges(response);
                    return;
                }

                // Handle page data
            });
    }

    #listenEditorChanges(response: DotCMSPageResponse) {
        this.subscription = this.editablePageService
            .listen(response)
            .subscribe(updatedPage => {
                // Handle updated page data
            });
    }

    ngOnDestroy() {
        this.subscription?.unsubscribe();
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

## FAQ

### What are the differences between UVE modes?

The dotCMS Universal Visual Editor (UVE) supports three modes that affect component behavior:

1. **DRAFT Mode (**`UVE_MODE.EDIT`**)**

   * Content is visible only when editing a page within dotCMS's editor draft mode
   * Ideal for edit controls, admin tools, or inline helpers
   * Visible only to content editors with proper permissions

2. **PREVIEW MODE (**`UVE_MODE.PREVIEW`**)**

   * Content is visible in dotCMS editor's preview panel
   * Useful for preview-only banners, staged content, or workflow messaging

3. **PUBLISHED Mode (**`UVE_MODE.LIVE`**)**

   * Content is shown in the "Published View" mode within the editor
   * Simulates the live site appearance, but is still within the editor
   * ⚠️ Do not confuse with the actual live production environment

Use these modes with:

* `<dotcms-show-when when={UVE_MODE.EDIT}>...</dotcms-show-when>`

### How do I configure and use the dotCMS Client in my Angular application?

The dotCMS Client needs to be properly configured and provided at the application level for all dotCMS components to work correctly.

1. **Setup the Client Configuration**:

```typescript
import { createDotCMSClient } from '@dotcms/client';
import { DotCMSClientConfig } from '@dotcms/types';

const DOTCMS_CLIENT_CONFIG: DotCMSClientConfig = {
    dotcmsUrl: environment.dotcmsUrl,  // Your dotCMS instance URL
    authToken: environment.authToken,   // Authentication token
    siteId: environment.siteId         // Your site identifier
};
```

2. **Configure the Provider** in your `app.config.ts`:

```typescript
import { InjectionToken } from '@angular/core';
import { createDotCMSClient } from '@dotcms/client';

export type DotCMSClient = ReturnType<typeof createDotCMSClient>;

export const DOTCMS_CLIENT_TOKEN = new InjectionToken<DotCMSClient>('DOTCMS_CLIENT');

export const appConfig: ApplicationConfig = {
    providers: [
        {
            provide: DOTCMS_CLIENT_TOKEN,
            useValue: createDotCMSClient(DOTCMS_CLIENT_CONFIG),
        }
    ],
};
```

3. **Use in Components**:

```typescript
export class YourComponent {
    private readonly client: DotCMSClient = inject(DOTCMS_CLIENT_TOKEN);

    ngOnInit() {
        this.client.page
            .get({ url: '/my-page' })
            .then(response => {
                // Handle page data
            });
    }
}
```

### How do I properly handle images in dotCMS components?

In dotCMS, assets are stored in the `dA` directory. You can use the `inode` property to construct the image URL. Example:

```typescript
@Component({
    selector: 'app-your-component',
    template: `
        <img *ngIf="contentlet.image" [src]="'dA/' + contentlet.inode" />
    `
})
export class YourComponent {
    @Input() contentlet: DotCMSBasicContentlet;


}
```

Remember to set up a proxy in your Angular configuration to redirect image requests to the dotCMS server:

```json
// proxy.conf.json
{
    "/dA": {
        "target": "http://localhost:8080"
    }
}
```

Then, in your `angular.json`, add the proxy configuration:

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

## dotCMS Support

We offer multiple channels to get help with the dotCMS React SDK:

* **GitHub Issues**: For bug reports and feature requests, please [open an issue](https://github.com/dotCMS/core/issues/new/choose) in the GitHub repository.
* **Community Forum**: Join our [community discussions](https://community.dotcms.com/) to ask questions and share solutions.
* **Stack Overflow**: Use the tag `dotcms-angular` when posting questions.

When reporting issues, please include:

* SDK version you're using
* React version
* Minimal reproduction steps
* Expected vs. actual behavior

Enterprise customers can access premium support through the [dotCMS Support Portal](https://dev.dotcms.com/docs/help).

---

## How To Contribute

GitHub pull requests are the preferred method to contribute code to dotCMS. We welcome contributions to the DotCMS UVE SDK! If you'd like to contribute, please follow these steps:

1. Fork the repository [dotCMS/core](https://github.com/dotCMS/core)
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

Please ensure your code follows the existing style and includes appropriate tests.

---

## Licensing Information

dotCMS comes in multiple editions and as such is dual-licensed. The dotCMS Community Edition is licensed under the GPL 3.0 and is freely available for download, customization, and deployment for use within organizations of all stripes. dotCMS Enterprise Editions (EE) adds several enterprise features and is available via a supported, indemnified commercial license from dotCMS. For the differences between the editions, see [the feature page](http://www.dotcms.com/cms-platform/features).

This SDK is part of dotCMS’s dual-licensed platform (GPL 3.0 for Community, commercial license for Enterprise).

[Learn more ](https://www.dotcms.com)at [dotcms.com](https://www.dotcms.com).
