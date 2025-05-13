# Fully Editable Page Using dotCMS + NextJS

This is a showcase of a Fully Editable Page Using [dotCMS](https://dotcms.com/) + [NextJS](https://nextjs.org/).

## Demo

[https://dotcms-nextjs-demo.vercel.app/](https://dotcms-nextjs-demo.vercel.app/)

## How to use

Execute [`create-next-app`](https://github.com/vercel/next.js/tree/canary/packages/create-next-app) with [npm](https://docs.npmjs.com/cli/init), [Yarn](https://yarnpkg.com/lang/en/docs/cli/create/), or [pnpm](https://pnpm.io) to bootstrap the example:

```bash
npx create-next-app dotcms-nextjs-demo --example https://github.com/dotCMS/core/tree/main/examples/nextjs
```

```bash
yarn create next-app dotcms-nextjs-demo --example https://github.com/dotCMS/core/tree/main/examples/nextjs
```

```bash
pnpm create next-app dotcms-nextjs-demo --example https://github.com/dotCMS/core/tree/main/examples/nextjs
```

## Configuration

### Step 1: Get a dotCMS Site

First, [get a dotCMS Site](https://www.dotcms.com/pricing). If you want to test this example, you can also use our [demo site](https://dev.dotcms.com/docs/demo-site).

After creating an account, create a new empty site from the dashboard and assign to it any name of your liking.

### Step 2: Create a dotCMS API Key

1. Go to the dotCMS admin panel.
2. Then click on System > Users
3. Select the user you want to create the API Key for
4. Go to API Access Key

To learn how to get the API Key, please refer to the [dotCMS API Documentation](https://dev.dotcms.com/docs/rest-api-authentication#ReadOnlyToken).

### Step 3: Configure the Universal Visual Editor

To begin setting up the Universal Visual Editor:

1. Browse to Settings -> Apps
2. Select the built-in integration for UVE - Universal Visual Editor.
3. Select the site that will be feeding the destination pages.
4. Add the following configuration:

```json
{ 
    "config":[ 
        { 
            "pattern":"(.*)", 
            "url":"http://localhost:3000"
        }
    ] 
}
```


To learn how to configure the Universal Visual Editor, please refer to the [dotCMS UVE Documentation](https://dev.dotcms.com/docs/uve-headless-config).

### Step 4: Configure the Environment Variables

Create a `.env.local` file in the root of the project. You can run the following command to copy the `.env.example` file and rename it to `.env.local`:

```bash
cp .env.local.example .env.local
```

Then set each variable on .env.local:

- `NEXT_PUBLIC_DOTCMS_HOST`: The URL of your dotCMS site.
- `NEXT_PUBLIC_DOTCMS_API_KEY`: The API Key you created in Step 2.
- `NEXT_PUBLIC_DOTCMS_SITE_ID`: The ID of the site you want to use. Learn more about dotCMS Multi-Site [here](https://dev.dotcms.com/docs/multi-site-management).


## Step 5: Run the project

Before running the project, you need to install the dependencies:

```bash
npm install
```

```bash
yarn install
```

```bash
pnpm install
```

Then, you can run the project using the following command:

```bash
npm run dev
```

## Walkthrough the project

### File Structure

```bash
.
├── components/
├── pages/
├── public/
├── .env.local
```