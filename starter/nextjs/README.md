# dotCMS Next.js Example

DotCMS provides a Next.js example that shows how to build dotCMS pages headlessly with Next.js JavaScript framework.

## 🚀 Quick Start

1. Create a new Next.js app:
```bash
npx create-next-app YOUR_NAME --example https://github.com/dotCMS/core/tree/main/starter/nextjs
```

2. Configure your `.env.local`:
```bash
NEXT_PUBLIC_DOTCMS_HOST=https://demo.dotcms.com
NEXT_PUBLIC_DOTCMS_AUTH_TOKEN=YOUR_TOKEN
```

3. Run it:
```bash
npm run dev
```

4. Replace dummy components:
```javascript
// src/components/my-page.js
const components = {
    "Banner": BannerComponent,    // Your custom component
    "BlogPost": BlogPostComponent // Your custom component
};
```

Until you add your components, you'll see a development view showing the available data for each content type.

## 📖 What's Included

This starter includes everything you need to build dotCMS pages with Next.js:

### Core Dependencies
- `@dotcms/client` - Official dotCMS API client
- `@dotcms/react` - React components for dotCMS
- `next` (v14) - The React framework
- `react` & `react-dom` (v18) - React core libraries

### Development Tools
- `tailwindcss` - Utility-first CSS framework
- `eslint` - Code linting
- `postcss` & `autoprefixer` - CSS processing

## 📖 Detailed Documentation

### Prerequisites
1. A dotCMS instance or https://demo.dotcms.com
2. A valid AUTH token ([how to create one](https://auth.dotcms.com/docs/latest/rest-api-authentication#creating-an-api-token-in-the-ui))
3. Node.js 18+ and npm

### Understanding the Components

#### Component Development System
The application includes a development-friendly system to help you build and test components for your dotCMS content types. There are two key files that handle this:

##### 1. DummyContentlet (`src/components/dummy.js`)
This is a development utility component that:
- Displays the raw content structure from dotCMS
- Filters out system properties to show only relevant content data
- Provides a copy functionality for easy reference
- Shows an interactive JSON view of your content type's data model

It's particularly useful when:
- Understanding the content model structure
- Developing new components
- Debugging content type data

##### 2. MyPage (`src/components/my-page.js`)
This is the main page component that:
- Handles the rendering of your dotCMS page
- Contains a `components` mapping object where you define your custom components
- Falls back to `DummyContentlet` for any unmapped content types

To create your own components:
1. Create your custom component
2. Add it to the `components` object in `my-page.js`:
```javascript
const components = {
    "Banner": BannerComponent,
    "BlogPost": BlogPostComponent
    // Add more components here
};
```

Until you map a component, the page will display the `DummyContentlet` view, showing you exactly what data is available to work with.

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
