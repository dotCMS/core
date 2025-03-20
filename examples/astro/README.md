# dotCMS Astro Example

This example project demonstrates how to build manageable dotCMS pages headlessly using the Astro framework. It showcases the integration between dotCMS and Astro, providing a practical implementation of content-driven web applications.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
  - [Creating the Application](#creating-the-application)
  - [Configuration](#configuration)
- [Running the Application](#running-the-application)
- [Key Features](#key-features)
  - [Handling Vanity URLs](#handling-vanity-urls)
  - [Universal Visual Editor](#universal-visual-editor)
- [Troubleshooting](#troubleshooting)
- [Learn More](#learn-more)
- [Deployment](#deployment)

## Prerequisites

Before you begin, ensure you have the following:

1. Access to a dotCMS instance (you can use https://demo.dotcms.com if you don't have your own)
2. A valid AUTH token for the target dotCMS instance ([How to create an API token](https://auth.dotcms.com/docs/latest/rest-api-authentication#creating-an-api-token-in-the-ui))
3. A valid Site Identifier where your page is located ([Multi-site Management](https://www.dotcms.com/docs/latest/multi-site-management#multi-site-management))
4. Node.js (version 18 or higher) and npm installed
5. A terminal application
6. A code editor of your choice

## Getting Started

### Creating the Application

Open your terminal and create the Astro app by running the following command:

```bash
npm create astro@latest -- --template dotcms/core/examples/astro
```

Follow the Astro setup steps after it pulls the example.

### Configuration

To configure the Astro app to use your dotCMS instance:

1. Open the project folder in your code editor
2. In the root, find the file `.env.local.example` and rename it to `.env.local`
3. Open the `.env.local` file and update the following environment variables:
   - `PUBLIC_DOTCMS_AUTH_TOKEN`: Your dotCMS auth token
   - `PUBLIC_DOTCMS_HOST`: URL of your dotCMS instance (e.g., https://demo.dotcms.com)
   - `PUBLIC_DOTCMS_SITE_ID`: The identifier of the Site you are going to use for your website

   To find your Site ID:
   1. Go to Settings > Sites in your dotCMS instance
   2. Select the desired Site (A modal should open)
   3. Go to the History Tab
   4. Copy the `Identifier` that appears at the top of the tab

⚠️ **Security Note**: Ensure that the auth token used here has [read-only permissions](https://www.dotcms.com/docs/latest/user-permissions#FrontEndBackEnd) to minimize security risks in client-side applications.

## Running the Application

Once configured, follow these steps to run the app:

1. Open a terminal in the project root directory
2. Install dependencies: `npm install`
3. Start the development server: `npm run dev`
4. Open your browser and navigate to `http://localhost:4321` (Verify the port Astro is using, 4321 is the default but it can change)

🎉 Congratulations! Your dotCMS Astro example is now running.

Note: When accessing pages (e.g., `localhost:4321/about`), ensure that the corresponding page exists in your dotCMS instance.

## Key Features

### Handling Vanity URLs

While this example doesn't explicitly demonstrate Vanity URL handling, you can implement it by extending the `[...slug].astro` file to check for Vanity URLs before rendering the page. You would need to:

1. Query the dotCMS API for Vanity URLs
2. If a match is found, redirect to the corresponding internal URL
3. If no match is found, proceed with normal page rendering

### Universal Visual Editor

To enable the Universal Visual Editor (UVE) in dotCMS for your Astro application, follow these steps:

1. In your dotCMS instance, navigate to the "Apps" page
2. Find the "UVE - Universal Visual Editor" app and click on it
3. Locate the site where you want to enable the UVE and click on it
4. In the configuration field, add the following:

```json
{
  "config": [
    {
      "pattern": ".*",
      "url": "http://localhost:4321"
    }
  ]
}
```

5. Click on the "Save" button to save the changes
6. Now, when you edit any page in dotCMS, you will see the UVE integrated with your Astro application

For more information about the UVE, please refer to the [dotCMS UVE Documentation](https://dotcms.com/docs/latest/universal-visual-editor-uve).

## Troubleshooting

If you encounter issues while setting up or running the dotCMS Astro example, here are some common problems and their solutions:

<details>
<summary><strong>Authentication errors (401 Unauthorized)</strong></summary>

This often occurs when the environment variables are not set correctly.

**Solution:** 
- Double-check that you've renamed `.env.local.example` to `.env.local`.
- Ensure you've updated the `PUBLIC_DOTCMS_AUTH_TOKEN` in the `.env.local` file with a valid token.
- Verify that the token hasn't expired. If it has, generate a new one in the dotCMS UI.
</details>

<details>
<summary><strong>Connection issues</strong></summary>

If you're having trouble connecting to the dotCMS instance:

**Solution:**
- Verify that the `PUBLIC_DOTCMS_HOST` in `.env.local` is correct.
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
- Double-check that the `PUBLIC_DOTCMS_SITE_ID` in `.env.local` is correct for the site you're trying to access.
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
npm run dev
```
</details>

<details>
<summary><strong>Build errors or stale cache</strong></summary>

If you're experiencing build errors or changes aren't reflected in the running application:

**Solution:** Clear Astro's cache and rebuild the project:
```bash
npm run clean
npm run build
npm run dev
```
This sequence of commands will:
1. Clear Astro's cache
2. Rebuild the project
3. Start the development server

This is recommended when:
- You've made significant changes to your project configuration
- You're experiencing unexplainable build errors
- Your changes aren't reflected in the running application despite saving and restarting the dev server
- You've recently updated Astro or other critical dependencies
</details>

<details>
<summary><strong>Universal Visual Editor (UVE) not working</strong></summary>

If the Universal Visual Editor is not functioning as expected:

**Solution:**
- Ensure you've correctly configured the UVE in your dotCMS instance as described in the [Universal Visual Editor](#universal-visual-editor) section.
- Verify that your Astro application is running on `http://localhost:4321` (or update the UVE configuration if using a different port).
- Check that you're accessing the dotCMS edit mode from the correct URL.
- Clear your browser cache and try again.
</details>

If you continue to experience issues after trying these solutions, please check the [dotCMS documentation](https://dotcms.com/docs/) or reach out to the dotCMS community for further assistance.

## Learn More

To learn more about Astro, take a look at the following resources:

- [Astro Documentation](https://docs.astro.build) - learn about Astro features and API.
- [Astro Blog](https://astro.build/blog/) - read the latest news and articles about Astro.

You can check out [the Astro GitHub repository](https://github.com/withastro/astro) - your feedback and contributions are welcome!

## Deployment

Astro applications can be deployed to various platforms. Here are a few popular options:

- [Netlify](https://docs.astro.build/en/guides/deploy/netlify/)
- [Vercel](https://docs.astro.build/en/guides/deploy/vercel/)
- [GitHub Pages](https://docs.astro.build/en/guides/deploy/github/)

Check out the [Astro deployment guides](https://docs.astro.build/en/guides/deploy/) for more detailed information on deploying your Astro site.