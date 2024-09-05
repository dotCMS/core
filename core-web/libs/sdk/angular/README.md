+# @dotcms/angular

`@dotcms/angular` is the official Angular library designed to work seamlessly with dotCMS. This library simplifies the process of rendering dotCMS pages and integrating with the [Universal Visual Editor](dotcms.com/docs/latest/universal-visual-editor) in your Angular applications.

## Table of Contents

- [Features](#features)
- [Installation](#installation)
- [Configuration](#configuration)
  - [Provider Setup](#provider-setup)
  - [Client Usage](#client-usage)
- [Components](#components)
  - [DotcmsLayoutComponent](#dotcmslayoutcomponent)
- [Best Practices](#best-practices)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)
- [License](#license)

## Features

- A set of Angular components developer for dotCMS page rendering and editor integration.
- Enhanced development workflow with full TypeScript support.
- Optimized performance for efficient rendering of dotCMS pages in Angular applications.
- Flexible customization options to adapt to various project requirements.

## Installation

Install the package using npm:

```bash
npm install @dotcms/angular
```

Or using Yarn:

```bash
yarn add @dotcms/angular
```

## DotCMS Page API

The `DotcmsLayoutComponent` requires a `DotCMSPageAsset` object to be passed in to it. This object represents a dotCMS page and can be fetched using the `@dotcms/client` library.

- [DotCMS Official Angular Example](https://github.com/dotCMS/core/tree/master/examples/angular)
- [`@dotcms/client` documentation](https://www.npmjs.com/package/@dotcms/client)
- [Page API documentation](https://dotcms.com/docs/latest/page-api)

## Components

### DotcmsLayoutComponent

The `DotcmsLayoutComponent` is a crucial component for rendering dotCMS page layouts in your Angular application.

#### Inputs

| Name         | Type                 | Description                                                           |
|--------------|----------------------|-----------------------------------------------------------------------|
| `pageAsset`  | `DotCMSPageAsset`    | The object representing a dotCMS page from PageAPI response.          |
| `components` | `DotCMSPageComponent`| An object mapping contentlets to their respective render components.  |
| `editor`     | `EditorConfig`       | Configuration for data fetching in Edit Mode.                         |

#### Usage Example

In your component file (e.g., `pages.component.ts`):

```typescript
import { Component, signal } from '@angular/core';
import { DotCMSPageComponent, EditorConfig } from '@dotcms/angular';

@Component({
    selector: 'app-pages',
    templateUrl: './pages.component.html',
})
export class PagesComponent {
    DYNAMIC_COMPONENTS: DotCMSPageComponent = {
        Activity: import('../pages/content-types/activity/activity.component').then(
            (c) => c.ActivityComponent
        ),
        Banner: import('../pages/content-types/banner/banner.component').then(
            (c) => c.BannerComponent
        ),
        // Add other components as needed
    };
    
    components = signal(this.DYNAMIC_COMPONENTS);
    editorConfig = signal<EditorConfig>({ params: { depth: 2 } });
    
    // Assume pageAsset is fetched or provided somehow
    pageAsset: DotCMSPageAsset;
}
```

In your template file (e.g., `pages.component.html`):

```html
<dotcms-layout 
    [pageAsset]="pageAsset" 
    [components]="components()" 
    [editor]="editorConfig()" 
/>
```

This setup allows for dynamic rendering of different content types on your dotCMS pages.

## Best Practices

1. **Lazy Loading**: Use dynamic imports for components to improve initial load times.
2. **Error Handling**: Implement robust error handling for API calls and component rendering.
3. **Type Safety**: Leverage TypeScript's type system to ensure proper usage of dotCMS structures.
4. **Performance Optimization**: Monitor and optimize the performance of rendered components.

## Troubleshooting

If you encounter issues:

1. Ensure all dependencies are correctly installed and up to date.
2. Verify that your dotCMS configuration (URL, auth token, site ID) is correct.
3. Check the browser console for any error messages.
4. Refer to the [dotCMS documentation](https://dotcms.com/docs/) for additional guidance.
