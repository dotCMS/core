# dotCMS Angular Example

This example project demonstrates how to build manageable dotCMS pages headlessly using the Angular framework. It showcases the integration between dotCMS and Angular, providing a practical implementation of content-driven web applications.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
  - [Obtaining the Example Code](#obtaining-the-example-code)
  - [Configuration](#configuration)
- [Running the Application](#running-the-application)
- [Project Structure](#project-structure)
- [Key Features](#key-features)
  - [Handling Vanity URLs](#handling-vanity-urls)
  - [Universal Visual Editor](#universal-visual-editor)
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

### Obtaining the Example Code

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
     authToken: "YOUR_AUTH_TOKEN_HERE",
     dotcmsUrl: "https://demo.dotcms.com",
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

## Key Features

### How dotCMS Routes Pages

dotCMS allows a single page to be accessed via multiple URL paths (e.g., / and /index for the same "Home" page). This flexibility means your Angular application needs to handle these variations.

To ensure all paths to the same content are properly managed and to prevent 404/500 errors, we recommend using a catch-all route strategy in Angular.

How to Implement in Angular:

Configure a wildcard route `(**)` as the last route in your Angular application's routing configuration. This route will capture any undefined paths, allowing you to fetch content from dotCMS based on the full URL.

You can learn more about Angular routing strategies [here](https://angular.dev/guide/routing/common-router-tasks)

### Handling Vanity URLs

This example demonstrates how to integrate dotCMS Vanity URLs with Angular routing. Vanity URLs in dotCMS provide alternative paths to internal or external URLs, enhancing site maintenance and SEO.

For implementation details, refer to the [`DotCMSPagesComponent`](./src/app/pages/components/dotcms-pages/dotcms-pages.component.ts) in the example code, which handles routing and Vanity URL redirection.

### Universal Visual Editor

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

## Troubleshooting

If you encounter issues while setting up or running the dotCMS Angular example, here are some common problems and their solutions:

<details>
<summary><strong>Authentication errors (401 Unauthorized)</strong></summary>

This often occurs when the environment variables are not set correctly.

**Solution:**

- Double-check that you've updated the `authToken` in `src/environments/environment.development.ts` with a valid token.
- Ensure the token has the necessary permissions (at least read access) for the content you're trying to fetch.
- Verify that the token hasn't expired. If it has, generate a new one in the dotCMS UI.
</details>

<details>
<summary><strong>Connection issues</strong></summary>

If you're having trouble connecting to the dotCMS instance:

**Solution:**

- Verify that the `dotcmsUrl` in `src/environments/environment.development.ts` is correct.
- Check if you can access the dotCMS instance directly through a web browser.
- If using `https://demo.dotcms.com`, remember it restarts every 24 hours. You might need to wait or try again later.
- Ensure your network allows connections to the dotCMS instance (check firewalls, VPNs, etc.).
</details>

<details>
<summary><strong>Missing pages or content</strong></summary>

If you're getting 404 errors for pages that should exist:

**Solution:**

- Ensure the page exists in your dotCMS instance. For example, if you're trying to access `/about`, make sure an "about" page exists in dotCMS.
- Check if the content types used in the example match those in your dotCMS instance.
- Verify that the content has been published and is not in draft status.
</details>

<details>
<summary><strong>Outdated dependencies or version conflicts</strong></summary>

If you're experiencing unexpected behavior or errors related to dependencies:

**Solution:** Perform a clean reinstall of all dependencies by running:

```bash
rm -rf node_modules && rm package-lock.json && npm install
```

This command will:

1. Remove the `node_modules` directory
2. Delete the `package-lock.json` file
3. Perform a fresh install of all dependencies

After this, restart your development server:

```bash
ng serve
```

</details>

<details>
<summary><strong>Build errors or stale cache</strong></summary>

If you're experiencing build errors or changes aren't reflected in the running application:

**Solution:** Clear the Angular build cache and rebuild the project:

```bash
ng cache clean
ng build --configuration=development
ng serve
```

This sequence of commands will:

1. Clear the Angular build cache
2. Rebuild the project with development configuration
3. Start the development server

This is recommended when:

- You've made significant changes to your project configuration
- You're experiencing unexplainable build errors
- Your changes aren't reflected in the running application despite saving and restarting the dev server
- You've recently updated Angular or other critical dependencies
</details>

<details>
<summary><strong>Universal Visual Editor (UVE) not working</strong></summary>

If the Universal Visual Editor is not functioning as expected:

**Solution:**

- Ensure you've correctly configured the UVE in your dotCMS instance as described in the [Universal Visual Editor](#universal-visual-editor) section.
- Verify that your Angular application is running on `http://localhost:4200` (or update the UVE configuration if using a different port).
- Check that you're accessing the dotCMS edit mode from the correct URL.
- Clear your browser cache and try again.
</details>

If you continue to experience issues after trying these solutions, please check the [dotCMS documentation](https://dotcms.com/docs/) or reach out to the dotCMS community for further assistance.

## Further Resources

- [Angular CLI Documentation](https://angular.io/cli)
- [dotCMS Documentation](https://dotcms.com/docs/)
- [dotCMS REST API Authentication](https://auth.dotcms.com/docs/latest/rest-api-authentication)

For more assistance with Angular, use `ng help` or visit the [Angular CLI Overview and Command Reference](https://angular.io/cli) page.
