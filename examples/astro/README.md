# dotCMS Astro Example

DotCMS provides a Astro example that shows how to build dotCMS pages heedlessly with Astro JavaScript framework.

## What do you need?

1. A dotCMS instance or you can use https://demo.dotcms.com
2. A valid AUTH token for the target instance (see: https://auth.dotcms.com/docs/latest/rest-api-authentication#creating-an-api-token-in-the-ui)
3. A valid Site Identifier where your page is located (see: https://www.dotcms.com/docs/latest/multi-site-management#multi-site-management)
4. Node js 18+ and npm installed
5. Terminal
6. And a code editor.

### Create the new Astro application

Open your terminal and let’s create the Astro app by running the following:

```bash
npm create astro@latest -- --template dotcms/core/examples/astro
```

Follow the Astro setup steps after it pulls the example.

## Add the dotCMS configuration

Now we need to tell the Astro app what dotCMS instance is going to use to get the data to build its pages.

1. Open the folder where you created the project in your code editor
2. In the root, find the file `.env.local.example` and rename to `.env.local`
3. Open the .env.local file and update the environment variable:

   - `PUBLIC_DOTCMS_AUTH_TOKEN` this is the auth token for dotCMS, you can use the dotCMS UI to create one.
   - `PUBLIC_DOTCMS_HOST` this is the instance of dotCMS where your pages and content lives (license needed) if you don’t have one, you can use [https://demo.dotcms.com](https://demo.dotcms.com) (be careful it restarts every 24h).
   - `PUBLIC_DOTCMS_SITE_ID` this is the identifier of the Site you are going to use for your website you can find it by going to Settings > Sites. Once there follow the next steps:

     1. Select the desired Site (A modal should be opened)
     2. Go to the History Tab
     3. Copy the `Identifier` that appears in the top of the tab

## Run the app

Once all the configuration is in place, it is time to run the web app.

1. Go back to your terminal and from the folder you created the project
2. Run `npm run dev`
3. Open http://localhost:4321 in your browser (Verify the port Astro is using, 4321 is the default but it can change)

🎉 And that’s it.

Consider that if you go to `localhost:4321/about`, the page `/about` needs to exist in your dotCMS instance.
