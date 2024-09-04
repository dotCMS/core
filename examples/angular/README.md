# dotCMS Angular Example 

DotCMS provides an Angular example that shows how to build manageable dotCMS pages headlessly with the Angular JavaScript framework.

## What do you need?

1. A dotCMS instance or you can use https://demo.dotcms.com
2. A valid AUTH token for the target instance (see: https://auth.dotcms.com/docs/latest/rest-api-authentication#creating-an-api-token-in-the-ui)
3. Node js 18+ and npm installed
4. Terminal
5. And a code editor.

### Get the Angular example code

Get the code from the Angular example directory

```bash
https://github.com/dotCMS/core/tree/master/examples/angular
```

Or just checkout the directory

```bash 
git clone -n --depth=1 --filter=tree:0 https://github.com/dotCMS/core
cd core
git sparse-checkout set --no-cone examples/angular
git checkout
```
The files will be found under the `examples/angular` folder



## Add the dotCMS configuration

Now we need to tell the Angular app what dotCMS instance is going to use to get the data to build its pages.

1. Open the folder `YOUR_NAME` in your code editor
2. Go to `src/environments`
3. Open the `environment.development.ts` file and update the environment variable:

- `authToken` this is the auth token for dotCMS, you can use the dotCMS UI to create one.
- `dotcmsUrl` this is the instance of dotCMS where your pages and content lives (license needed) if you don’t have one, you can use [https://demo.dotcms.com](https://demo.dotcms.com) (be careful it restarts every 24h)

## Run the app

Once all the configuration is in place, it is time to run the web app.

1. Go back to your terminal and from the folder YOUR_NAME
2. Run `ng serve`
3. Open http://localhost:4200 in your browser

🎉 And that’s it.

Consider that if you go to `localhost:4200/about`, the page `/about` needs to exist in your dotCMS instance.

## Folder structure
```
.
└── src/
    └── app/
        ├── content-types/
        │   ├── activity
        │   ├── banner
        │   ├── product
        │   └── ...other-content-types
        ├── pages/
        │   ├── components
        │   └── services
        └── shared/
            ├── contentlets
            └── contentlet
```
Where: </br>
    `content-types`: Represents which component is rendered for each content-type existing on the dotCMS API page </br>
    `pages`: Main component of the application, here all pages are rendered according to their path and pageAsset. It also displays an error page as appropriate. </br>
    `shared`: Contains components that can be used anywhere in the application.</br>
    &emsp;&emsp;`contentlet`: Local component for rendering a Contentlet, outside the DotCmsLayout. </br>
    &emsp;&emsp;`contentlets`: Local component for displaying a list of `contentlet`.

## Handling Vanity URLs

In dotCMS, Vanity URLs serve as alternative reference paths to internal or external URLs. They are simple yet powerful tools that can significantly aid in site maintenance and SEO.

Next.js is a robust framework that provides the capability to handle vanity URLs. It allows you to redirect or forward users to the appropriate content based on predefined logic. You can seamlessly integrate this feature of Next.js with dotCMS. For an implementation example, refer to this [link](https://github.com/dotCMS/core/blob/master/examples/nextjs/src/app/utils/index.js).

## Further help

To get more help on the Angular CLI use `ng help` or go check out the [Angular CLI Overview and Command Reference](https://angular.io/cli) page.
