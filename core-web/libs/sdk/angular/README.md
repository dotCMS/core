# @dotcms/angular

`@dotcms/angular` is the official set of Angular components, services and resolver designed to work seamlessly with dotCMS, making it easy to render dotCMS pages an use the page builder

## Features

-   A collection of  Angular components, services and resolver  tailored to render  
    dotCMS pages.
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
```
const DOTCMS_CLIENT_CONFIG: ClientConfig = {
    dotcmsUrl: environment.dotcmsUrl,
    authToken: environment.authToken,
    siteId: environment.siteId
};
```
Add the dotcms config in the Angular app ApplicationConfig 
```
export const appConfig: ApplicationConfig = {
    providers: [
        provideDotcmsClient(DOTCMS_CLIENT_CONFIG),
        provideRouter(routes),
    ],
};
```
## Resolver
```javascript
export const routes: Routes = [
  {
    path: '**',
    resolve: {
      // This should be called `context`.
      context: DotCMSPageResolver,
    },
    component: DotCMSPagesComponent,
    runGuardsAndResolvers: 'always' // Run the resolver on every navigation. Even if the URL hasn't changed.
  },
];
```

Then, in your component, you can read the data using

```javascript
protected readonly context = signal(null);

ngOnInit() {
    // Get the context data from the route
    this.route.data.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(data => {
        this.context.set(data['context']);
    });
}
```
## Components

### `DotcmsLayoutComponent`

A component that renders a layout for a dotCMS page.

#### Inputs

-   **entity**: The context for a dotCMS page.
-   **components**: An object with the relation of contentlets and the component to render each.


#### Usage

```javascript
    <dotcms-layout [entity]="pageAsset" [components]="components()" />

    DYNAMIC_COMPONENTS: { [key: string]: DynamicComponentEntity } = {
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
| Twitter         | @dotCMS                                                             |
| Main Site       | [dotCMS.com](https://dotcms.com/)                                   |
