# @dotcms/angular

`@dotcms/angular` is the official set of Angular components and services designed to work seamlessly with dotCMS, making it easy to render dotCMS pages an use the page builder

## Features

-   A collection of  Angular components and services tailored to render dotCMS pages.
-   Streamlined integration with dotCMS page editor.
-   Improved development experience with comprehensive TypeScript typings.

## Installation

Install the package via npm:

```bash
npm install @dotcms/angular
```

Or using Yarn:

```bash
yarn add @dotcms/angular
```

## Provider
In our Angular application, we need to provide the information of our dotCMS instance

```javascript
const DOTCMS_CLIENT_CONFIG: ClientConfig = {
    dotcmsUrl: environment.dotcmsUrl,
    authToken: environment.authToken,
    siteId: environment.siteId
};
```
And add this config in the ApplicationConfig in `src/app/app.config.ts`

```
const client = DotCmsClient.init(DOTCMS_CLIENT_CONFIG);

export const appConfig: ApplicationConfig = {
    providers: [
        provideRouter(routes),
        // Add here
        {
            provide: DOTCMS_CLIENT_TOKEN,
            useValue: client
        }
    ],
};
```
This way, we will have access to `DOTCMS_CLIENT_TOKEN` from anywhere in our application.

## Client
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
For more information to how to use DotCms Client, you can visit the [documentation](https://github.com/dotCMS/core/blob/master/core-web/libs/sdk/client/README.md)

## Components

### `DotcmsLayoutComponent`

A component that renders a layout for a dotCMS page.

#### Inputs

| Name       | Type                        | Description |
|------------|----------------------|---------------|
| `pageAsset`   | `DotCMSPageAsset`             | The object that represents a dotCMS page from PageAPI response.             |
| `components`   | `DotCMSPageComponent`             | An object with the relation of contentlets and the component to render each.             |
| `editor`  | `EditorConfig`    | The configuration custom params for data fetching on Edit Mode..            |


#### Usage

`/pages.component.ts`
```javascript
    DYNAMIC_COMPONENTS: DotCMSPageComponent = {
        Activity: import('../pages/content-types/activity/activity.component').then(
            (c) => c.ActivityComponent,
        ),
        Banner: import('../pages/content-types/banner/banner.component').then(
            (c) => c.BannerComponent,
        ),
        Image: import('../pages/content-types/image/image.component').then(
            (c) => c.ImageComponent,
        ),
        webPageContent: import(
            '../pages/content-types/web-page-content/web-page-content.component'
        ).then((c) => c.WebPageContentComponent),
        Product: import('../pages/content-types/product/product.component').then(
            (c) => c.ProductComponent,
        ),
    };
    
    components = signal(DYNAMIC_COMPONENTS);
    
    editorConfig = signal({ params: { depth: 2 } })

```

`/pages.component.html`
```
    <dotcms-layout 
        [entity]="pageAsset" 
        [components]="components()" 
        [editor]="editorConfig()" />
```

## Contributing

GitHub pull requests are the preferred method to contribute code to dotCMS. Before any pull requests can be accepted, an automated tool will ask you to agree to the [dotCMS Contributor's Agreement](https://gist.github.com/wezell/85ef45298c48494b90d92755b583acb3).

## Licensing

dotCMS comes in multiple editions and as such is dual licensed. The dotCMS Community Edition is licensed under the GPL 3.0 and is freely available for download, customization and deployment for use within organizations of all stripes. dotCMS Enterprise Editions (EE) adds a number of enterprise features and is available via a supported, indemnified commercial license from dotCMS. For the differences between the editions, see [the feature page](http://dotcms.com/cms-platform/features).

## Support

If you need help or have any questions, please [open an issue](https://github.com/dotCMS/core/issues/new/choose) in the GitHub repository.

## Documentation

Always refer to the official [DotCMS documentation](https://www.dotcms.com/docs/latest/) for comprehensive guides and API references.

## Getting Help

| Source          | Location                                                            |
| --------------- | ------------------------------------------------------------------- |
| Installation    | [Installation](https://dotcms.com/docs/latest/installation)         |
| Documentation   | [Documentation](https://dotcms.com/docs/latest/table-of-contents)   |
| Videos          | [Helpful Videos](http://dotcms.com/videos/)                         |
| Forums/Listserv | [via Google Groups](https://groups.google.com/forum/#!forum/dotCMS) |
| Twitter         | [@dotCMS](https://x.com/dotcms)                                     |
| Main Site       | [dotCMS.com](https://dotcms.com/)                                   |
