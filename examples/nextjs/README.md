# dotCMS Next.js Example

This example project demonstrates how to build manageable dotCMS pages headlessly using the Next.js framework. It showcases the integration between dotCMS and Next.js, providing a practical implementation of content-driven web applications.


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
3. Node.js (version 18 or higher) and npm installed
4. A terminal application
5. A code editor of your choice

## Getting Started

### Creating the Application

Open your terminal and create the Next.js app by running the following command:

```bash
npx create-next-app YOUR_NAME --example https://github.com/dotCMS/core/tree/main/examples/nextjs
```

This will create a new Next.js app with the dotCMS example.

### Configuration

To configure the Next.js app to use your dotCMS instance:

1. Open the folder `YOUR_NAME` in your code editor
2. In the root, find the file `.env.local.example` and rename it to `.env.local`
3. Open the `.env.local` file and update the following environment variables:
   - `NEXT_PUBLIC_DOTCMS_AUTH_TOKEN`: Your dotCMS auth token
   - `NEXT_PUBLIC_DOTCMS_HOST`: URL of your dotCMS instance (e.g., https://demo.dotcms.com)


⚠️ **Security Note**: Ensure that the auth token used here has [read-only permissions](https://www.dotcms.com/docs/latest/user-permissions#FrontEndBackEnd) to minimize security risks in client-side applications.

## Running the Application

Once configured, follow these steps to run the app:

1. Open a terminal in the project root directory
2. Install dependencies: `npm install`
3. Start the development server: `npm run dev`
4. Open your browser and navigate to `http://localhost:3000`

🎉 Congratulations! Your dotCMS Next.js example is now running.

Note: When accessing pages (e.g., `localhost:3000/about`), ensure that the corresponding page exists in your dotCMS instance.

## Key Features

### Handling Vanity URLs

This example demonstrates how to integrate dotCMS Vanity URLs with Next.js routing. Vanity URLs in dotCMS provide alternative paths to internal or external URLs, enhancing site maintenance and SEO.

For implementation details, refer to the `utils/index.js` file in the example code, which handles Vanity URL redirection.

### Universal Visual Editor

To enable the Universal Visual Editor (UVE) in dotCMS for your Next.js application, follow these steps:

1. In your dotCMS instance, navigate to the "Apps" page
2. Find the "UVE - Universal Visual Editor" app and click on it
3. Locate the site where you want to enable the UVE and click on it
4. In the configuration field, add the following:

```json
{
  "config": [
    {
      "pattern": ".*",
      "url": "http://localhost:3000"
    }
  ]
}
```

5. Click on the "Save" button to save the changes
6. Now, when you edit any page in dotCMS, you will see the UVE integrated with your Next.js application

For more information about the UVE, please refer to the [dotCMS UVE Documentation](https://dotcms.com/docs/latest/universal-visual-editor-uve).

## Troubleshooting

If you encounter issues while setting up or running the dotCMS Next.js example, here are some common problems and their solutions:

<details>
<summary><strong>Outdated npm packages or dependency issues</strong></summary>

The `@dotcms/xxx` npm libraries are set to use the `latest` tag. Sometimes, `npm install` might not install the most recent version, or you might encounter dependency conflicts.

**Solution:** Perform a clean reinstall of all dependencies by running:
```bash
rm -rf .next node_modules && rm package-lock.json && npm i && npm run dev
```
This command will:
1. Remove the `.next` build folder and `node_modules` directory
2. Delete the `package-lock.json` file
3. Perform a fresh install of all dependencies
4. Start the development server

This ensures you have the latest versions of all packages and resolves most dependency-related issues.
</details>

<details>
<summary><strong>Stale build cache</strong></summary>

Sometimes, the Next.js build cache can become stale, leading to unexpected behavior or errors that persist even after updating your code.

**Solution:** Delete the `.next` folder to clear the build cache:
```bash
rm -rf .next && npm run dev
```

This is recommended when:
- You've made significant changes to your project configuration (e.g., `next.config.js`)
- You're experiencing unexplainable build errors
- Your changes aren't reflected in the running application despite saving and restarting the dev server
- You've recently updated Next.js or other critical dependencies

After running this command, Next.js will rebuild the entire project from scratch, ensuring you're working with a fresh build.
</details>

<details>
<summary><strong>Authentication errors (401 Unauthorized)</strong></summary>

This often occurs when the environment variables are not set correctly.

**Solution:** 
- Double-check that you've renamed `.env.local.example` to `.env.local`.
- Ensure you've updated the `NEXT_PUBLIC_DOTCMS_AUTH_TOKEN` in the `.env.local` file with a valid token.
- Verify that the token hasn't expired. If it has, generate a new one in the dotCMS UI.
</details>

<details>
<summary><strong>Connection issues</strong></summary>

If you're having trouble connecting to the dotCMS instance.

**Solution:**
- Verify that the `NEXT_PUBLIC_DOTCMS_HOST` in `.env.local` is correct.
- If using `https://demo.dotcms.com`, remember it restarts every 24 hours. You might need to wait or try again later.
- Ensure your network allows connections to the dotCMS instance (check firewalls, VPNs, etc.).
</details>

<details>
<summary><strong>Missing pages</strong></summary>

If you're getting 404 errors for pages that should exist.

**Solution:**
- Ensure the page exists in your dotCMS instance. For example, if you're trying to access `/about`, make sure an "about" page exists in dotCMS.
- Check if the content types used in the example match those in your dotCMS instance.
- Verify that the content has been published and is not in draft status.
</details>

<details>
<summary><strong>Universal Visual Editor (UVE) not working</strong></summary>

If the Universal Visual Editor is not functioning as expected:

**Solution:**
- Ensure you've correctly configured the UVE in your dotCMS instance as described in the [Universal Visual Editor](#universal-visual-editor) section.
- Verify that your Next.js application is running on `http://localhost:3000` (or update the UVE configuration if using a different port).
- Check that you're accessing the dotCMS edit mode from the correct URL.
- Clear your browser cache and try again.
</details>

If you continue to experience issues after trying these solutions, please check the [dotCMS documentation](https://dotcms.com/docs/) or reach out to the dotCMS community for further assistance.

## Learn More

To learn more about Next.js, take a look at the following resources:

- [Next.js Documentation](https://nextjs.org/docs) - learn about Next.js features and API.
- [Learn Next.js](https://nextjs.org/learn) - an interactive Next.js tutorial.

You can check out [the Next.js GitHub repository](https://github.com/vercel/next.js/) - your feedback and contributions are welcome!

## Deployment

The easiest way to deploy your Next.js app is to use the [Vercel Platform](https://vercel.com/new?utm_medium=default-template&filter=next.js&utm_source=create-next-app&utm_campaign=create-next-app-readme) from the creators of Next.js.

Check out the [Next.js deployment documentation](https://nextjs.org/docs/deployment) for more details.