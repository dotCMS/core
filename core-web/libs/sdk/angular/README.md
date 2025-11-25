# @dotcms/angular

`@dotcms/angular` is the official Angular library designed to work seamlessly with dotCMS. This library simplifies the process of rendering dotCMS pages and integrating with the [Universal Visual Editor](dotcms.com/docs/latest/universal-visual-editor) in your Angular applications.

## Table of Contents

- [Features](#features)
- [Installation](#installation)
- [Configuration](#provider-setup)
  - [Provider Setup](#provider-setup)
  - [Client Usage](#client-usage)
- [Components](#components)
  - [DotcmsLayoutComponent](#dotcmslayoutcomponent)
- [Best Practices](#best-practices)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)
- [Licensing](#licensing)

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

## Configutarion
### Provider Setup
We need to provide the information of our dotCMS instance

```javascript

import { ClientConfig } from '@dotcms/client';

const DOTCMS_CLIENT_CONFIG: ClientConfig = {
    dotcmsUrl: environment.dotcmsUrl,
    authToken: environment.authToken,
    siteId: environment.siteId
};
```
And add this config in the Angular app ApplicationConfig.

`src/app/app.config.ts`
```javascript
import { InjectionToken } from '@angular/core';
import { ClientConfig, DotCmsClient } from '@dotcms/client';

export const DOTCMS_CLIENT_TOKEN = new InjectionToken<DotCmsClient>('DOTCMS_CLIENT');

export const appConfig: ApplicationConfig = {
    providers: [
        provideRouter(routes),
        {
            provide: DOTCMS_CLIENT_TOKEN,
            useValue: DotCmsClient.init(DOTCMS_CLIENT_CONFIG),
        }
    ],
};
```
This way, we will have access to `DOTCMS_CLIENT_TOKEN` from anywhere in our application.

### Client Usage
To interact with the client and obtain information from, for example, our pages

```javascript
private readonly client = inject(DOTCMS_CLIENT_TOKEN);

this.client.page
    .get({ ...pageParams })
    .then((response) => {
        // Use your response
    })
    .catch((e) => {
      const error: PageError = {
        message: e.message,
        status: e.status,
      };
      // Use the error response
    })
```
For more information to how to use DotCms Client, you can visit the [documentation](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/client/README.md)

## DotCMS Page API

The `DotcmsLayoutComponent` requires a `DotCMSPageAsset` object to be passed in to it. This object represents a dotCMS page and can be fetched using the `@dotcms/client` library.

- [DotCMS Official Angular Example](https://github.com/dotCMS/core/tree/main/examples/angular)
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

## Contributing

GitHub pull requests are the preferred method to contribute code to dotCMS. Before any pull requests can be accepted, an automated tool will ask you to agree to the [dotCMS Contributor's Agreement](https://gist.github.com/wezell/85ef45298c48494b90d92755b583acb3).

## Licensing

dotCMS comes in multiple editions and as such is dual licensed. The dotCMS Community Edition is licensed under the GPL 3.0 and is freely available for download, customization and deployment for use within organizations of all stripes. dotCMS Enterprise Editions (EE) adds a number of enterprise features and is available via a supported, indemnified commercial license from dotCMS. For the differences between the editions, see [the feature page](http://dotcms.com/cms-platform/features).
