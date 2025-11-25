# dotCMS Angular Example

This example project demonstrates how to build manageable dotCMS pages headlessly using the Angular framework. It showcases the integration between dotCMS and Angular, providing a practical implementation of content-driven web applications.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
  - [Obtaining the Example Code](#downloading-the-example)
  - [Configuration](#configuration)
- [Running the Application](#running-the-application)
- [Project Structure](#project-structure)
- [Key Features](#handling-vanity-urls)
  - [Handling Vanity URLs](#handling-vanity-urls)
- [Troubleshooting](#troubleshooting)
- [Further Resources](#further-resources)

## Prerequisites

Before you begin, ensure you have the following:

1. Access to a dotCMS instance (you can use https://demo.dotcms.com if you don't have your own)
2. A valid AUTH token for the target dotCMS instance ([How to create an API token](https://auth.dotcms.com/docs/latest/rest-api-authentication#creating-an-api-token-in-the-ui))
3. Node.js (version 18 or higher) and npm installed
4. A terminal application
5. A code editor of your choice

## Getting Started

### Downloading the example

You can get the code in two ways:

1. Direct download:
   ```
   https://github.com/dotCMS/core/tree/main/examples/angular
   ```

2. Using Git sparse checkout:
   ```bash
   git clone -n --depth=1 --filter=tree:0 https://github.com/dotCMS/core
   cd core
   git sparse-checkout set --no-cone examples/angular
   git checkout
   ```
   The example files will be in the `examples/angular` folder.

### Configuration

To configure the Angular app to use your dotCMS instance:

1. Open the project folder in your code editor
2. Navigate to `src/environments`
3. Open `environment.development.ts` and update the following variables:
   - `authToken`: Your dotCMS auth token
   - `dotcmsUrl`: URL of your dotCMS instance (e.g., https://demo.dotcms.com)

   ```typescript
   export const environment = {
     production: false,
     authToken: 'YOUR_AUTH_TOKEN_HERE',
     dotcmsUrl: 'https://demo.dotcms.com',
   };
   ```

   ⚠️ **Security Note**: Ensure that the `authToken` used here has [read-only permissions](https://www.dotcms.com/docs/latest/user-permissions#FrontEndBackEnd) to minimize security risks in client-side applications.

## Running the Application

Once configured, follow these steps to run the app:

1. Open a terminal in the project root directory
2. Install dependencies: `npm install`
3. Start the development server: `ng serve`
4. Open your browser and navigate to `http://localhost:4200`

🎉 Congratulations! Your dotCMS Angular example is now running.

Note: When accessing `localhost:4200/about`, ensure that the `/about` page exists in your dotCMS instance.

## Project Structure

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
            └── contentlets-wrapper/
                └── contentlet/  
```

- `content-types/`: Components for rendering specific dotCMS content types
- `pages/`: Main application component for rendering pages based on their path and pageAsset
- `shared/`: Reusable components
  - `contentlets-wrapper/`: Component for displaying lists of Contentlets
    - `contentlet/`: Component for rendering individual Contentlets

## Universal Visual Editor
To enable the Universal Visual Editor in dotCMS, follow these steps:

1. In your dotCMS instance, navigate to the "Apps" page
2. Find the "UVE - Universal Visual Editor" app and click on it
3. Then locate the site where you want to enable the UVE and click on it
4. In the configuration field add the following:

```json
{
  "config": [
    {
      "pattern": ".*",
      "url": "http://localhost:4200"
    }
  ]
}
```

5. Click on the "Save" button to save the changes.
6. Now edit any page and you will see the UVE.

If you want more information about the UVE, please refer to the [dotCMS UVE Documentation](https://dotcms.com/docs/latest/universal-visual-editor-uve).

## Key Features
### Handling Vanity URLs

This example demonstrates how to integrate dotCMS Vanity URLs with Angular routing. Vanity URLs in dotCMS provide alternative paths to internal or external URLs, enhancing site maintenance and SEO.

For implementation details, refer to the [`DotCMSPagesComponent`](./src/app/pages/components/dotcms-pages/dotcms-pages.component.ts) in the example code, which handles routing and Vanity URL redirection.

## Troubleshooting

If you encounter issues:

1. Verify that your dotCMS instance is accessible and the AUTH token is valid
2. Check the browser console for any error messages
3. Ensure all dependencies are correctly installed (`npm install`)
4. Verify that the required pages and content exist in your dotCMS instance

## Further Resources

- [Angular CLI Documentation](https://angular.io/cli)
- [dotCMS Documentation](https://dotcms.com/docs/)
- [dotCMS REST API Authentication](https://auth.dotcms.com/docs/latest/rest-api-authentication)

For more assistance with Angular, use `ng help` or visit the [Angular CLI Overview and Command Reference](https://angular.io/cli) page.
