# dotCMS Next.js Example

DotCMS provides a Next.js example that shows how to build dotCMS pages heedlessly with Next.js JavaScript framework.

## What do you need?
1. A dotCMS instance or you can use https://demo.dotcms.com
2. A valid AUTH token for the target instance (see: https://auth.dotcms.com/docs/latest/rest-api-authentication#creating-an-api-token-in-the-ui)
3. Node js 18+ and npm installed
4. Terminal
5. And a code editor.

### Create the new Next.js application
Open your terminal and let’s create the Next.js app by running the following:

```bash
npx create-next-app YOUR_NAME --example https://github.com/dotCMS/core/tree/main/examples/nextjs
```

This will create a new Next.js app with dotCMS example

## Add the dotCMS configuration
Now we need to tell the Next.js app what dotCMS instance is going to use to get the data to build its pages.

1. Open the folder `YOUR_NAME` in your code editor
2. In the root, find the file .env.local.example and rename to .env.local
3. Open the .env.local file and update the environment variable:
  - `NEXT_PUBLIC_DOTCMS_AUTH_TOKEN` this is the auth token for dotCMS, you can use the dotCMS UI to create one.
  - `NEXT_PUBLIC_DOTCMS_HOST` this is the instance of dotCMS where your pages and content lives (license needed) if you don’t have one, you can use [https://demo.dotcms.com](https://demo.dotcms.com) (be careful it restarts every 24h)

## Run the app
Once all the configuration is in place, it is time to run the web app.

1. Go back to your terminal and from the folder YOUR_NAME
3. Run `npm run dev`
3. Open http://localhost:3000 in your browser

🎉 And that’s it.

Consider that if you go to `localhost:3000/about`, the page `/about` needs to exist in your dotCMS instance.

## Handling Vanity URLs

In dotCMS, Vanity URLs serve as alternative reference paths to internal or external URLs. They are simple yet powerful tools that can significantly aid in site maintenance and SEO.

Next.js is a robust framework that provides the capability to handle vanity URLs. It allows you to redirect or forward users to the appropriate content based on predefined logic. You can seamlessly integrate this feature of Next.js with dotCMS. For an implementation example, refer to this [link](https://github.com/dotCMS/core/blob/main/examples/nextjs/src/app/utils/index.js).

## Learn More

To learn more about Next.js, take a look at the following resources:

-   [Next.js Documentation](https://nextjs.org/docs) - learn about Next.js features and API.
-   [Learn Next.js](https://nextjs.org/learn) - an interactive Next.js tutorial.

You can check out [the Next.js GitHub repository](https://github.com/vercel/next.js/) - your feedback and contributions are welcome!

## Deploy on Vercel

The easiest way to deploy your Next.js app is to use the [Vercel Platform](https://vercel.com/new?utm_medium=default-template&filter=next.js&utm_source=create-next-app&utm_campaign=create-next-app-readme) from the creators of Next.js.

Check out our [Next.js deployment documentation](https://nextjs.org/docs/deployment) for more details.
