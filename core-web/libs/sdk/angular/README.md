# @dotcms/angular

`@dotcms/angular` is the official Angular library designed to work seamlessly with dotCMS. This library simplifies the process of rendering dotCMS pages and integrating with the [Universal Visual Editor](https://www.dotcms.com/docs/latest/universal-visual-editor) in your Angular applications.

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

- A set of Angular components developed for dotCMS page rendering and editor integration.
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

## Configuration
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

Add this configuration to `ApplicationConfig` in your Angular app.

`src/app/app.config.ts`
```typescript
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

```typescript
export class YourComponent {
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
}
```

For more information on how to use the dotCMS Client, you can visit the [documentation](https://www.github.com/dotCMS/core/blob/main/core-web/libs/sdk/client/README.md)

## DotCMS Page API

The `DotcmsLayoutComponent` requires a `DotCMSPageAsset` object to be passed in to it. This object represents a dotCMS page and can be fetched using the `@dotcms/client` library.

- [DotCMS Official Angular Example](https://www.github.com/dotCMS/core/tree/main/examples/angular)
- [`@dotcms/client` documentation](https://www.npmjs.com/package/@dotcms/client)
- [Page API documentation](https://www.dotcms.com/docs/latest/page-api)

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

If you encounter issues while using `@dotcms/angular`, here are some common problems and their solutions:

1. **Dependency Issues**:
   - Ensure that all dependencies, such as `@dotcms/client`, `@angular/core`, and `rxjs`, are correctly installed and up-to-date. You can verify installed versions by running:
     ```bash
     npm list @dotcms/client @angular/core rxjs
     ```
   - If there are any missing or incompatible versions, reinstall dependencies by running:
     ```bash
     npm install
     ```
     or
     ```bash
     npm install --force
     ```

2. **Configuration Errors**:
   - **DotCMS Configuration**: Double-check that your `DOTCMS_CLIENT_CONFIG` settings (URL, auth token, site ID) are correct and aligned with the environment variables. For example:
     ```typescript
     const DOTCMS_CLIENT_CONFIG: ClientConfig = {
         dotcmsUrl: environment.dotcmsUrl,  // Ensure this is a valid URL
         authToken: environment.authToken,  // Ensure auth token has the correct permissions
         siteId: environment.siteId         // Ensure site ID is valid and accessible
     };
     ```
   - **Injection Issues**: Ensure that `DOTCMS_CLIENT_TOKEN` is provided globally. Errors like `NullInjectorError` usually mean the token hasn’t been properly added to the `ApplicationConfig`. Verify by checking `src/app/app.config.ts`.

3. **Network and API Errors**:
   - **dotCMS API Connectivity**: If API calls are failing, check your browser’s Network tab to ensure requests to `dotcmsUrl` are successful. For CORS-related issues, ensure that your dotCMS server allows requests from your application’s domain.
   - **Auth Token Permissions**: If you’re seeing `401 Unauthorized` errors, make sure the auth token used in `DOTCMS_CLIENT_CONFIG` has appropriate permissions in dotCMS for accessing pages and content.

4. **Page and Component Rendering**:
   - **Dynamic Imports**: If you’re encountering issues with lazy-loaded components, make sure dynamic imports are correctly set up, as in:
     ```typescript
      const DYNAMIC_COMPONENTS: DotCMSPageComponent = {
        Activity: import('../pages/content-types/activity/activity.component').then(
            (c) => c.ActivityComponent
        )
      };
     ```
   - **Invalid Page Assets**: Ensure that `pageAsset` objects are correctly formatted. Missing fields in `pageAsset` can cause errors in `DotcmsLayoutComponent`. Validate the structure by logging `pageAsset` before passing it in.

5. **Common Angular Errors**:
   - **Change Detection**: Angular sometimes fails to detect changes with dynamic content. If `DotcmsLayoutComponent` isn’t updating as expected, you may need to call `ChangeDetectorRef.detectChanges()` manually.
   - **TypeScript Type Errors**: Ensure all types (e.g., `DotCMSPageAsset`, `DotCMSPageComponent`) are imported correctly from `@dotcms/angular`. Type mismatches can often be resolved by verifying imports.

6. **Consult Documentation**: Refer to the official [dotCMS documentation](https://dotcms.com/docs/) and the [@dotcms/angular GitHub repository](https://github.com/dotCMS/core). These sources often provide updates and additional usage examples.

## Contributing

GitHub pull requests are the preferred method to contribute code to dotCMS. Before any pull requests can be accepted, an automated tool will ask you to agree to the [dotCMS Contributor's Agreement](https://www.gist.github.com/wezell/85ef45298c48494b90d92755b583acb3).

## Licensing

dotCMS comes in multiple editions and as such is dual licensed. The dotCMS Community Edition is licensed under the GPL 3.0 and is freely available for download, customization and deployment for use within organizations of all stripes. dotCMS Enterprise Editions (EE) adds a number of enterprise features and is available via a supported, indemnified commercial license from dotCMS. For the differences between the editions, see [the feature page](http://www.dotcms.com/cms-platform/features).
