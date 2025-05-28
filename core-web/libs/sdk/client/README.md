# dotCMS Client SDK

The `@dotcms/client` is a powerful JavaScript/TypeScript SDK designed to simplify the integration of dotCMS content into your applications. Whether building dynamic websites or content-driven apps, this SDK offers an intuitive API for seamless and type-safe content retrieval, enabling you to create engaging experiences effortlessly by accessing and displaying content from dotCMS.

### When to Use It:

-   Building headless frontends that need dotCMS content
-   Building content-driven apps that need to access dotCMS content
-   Developing multi-language or personalized experiences
-   Implementing dynamic navigation and page structures

### Key Benefits:

-   **Simplified Development**: Write less code with intuitive methods and builders
-   **Type Safety**: Built-in TypeScript definitions prevent runtime errors
-   **Universal Compatibility**: Works in both browser and Node.js environments
-   **Performance Optimized**: Built-in caching and efficient data fetching
-   **Security First**: Handles authentication and requests securely
-   **Developer Experience**: Rich autocompletion and documentation

## Table of Contents

-   [Prerequisites & Setup](#prerequisites--setup)
    -   [Get a dotCMS Instance](#get-a-dotcms-instance)
    -   [Create a dotCMS API Key](#create-a-dotcms-api-key)
    -   [Installation](#installation)
-   [Quickstart](#quickstart)
    -   [Example Projects](#example-projects)
-   [Key Concepts](#key-concepts)
-   [API Reference](#api-reference)
    -   [Client Initialization](#client-initialization)
    -   [Page API](#page-api)
    -   [Content API](#content-api)
    -   [Navigation API](#navigation-api)
-   [TypeScript Support](#typescript-support)
    -   [Usage Example](#usage-example)
-   [Best Practices](#best-practices)
    -   [Fetching Content and Navigation](#fetching-content-and-navigation)
-   [dotCMS Support](#dotcms-support)
-   [How To Contribute](#how-to-contribute)
-   [Licensing Information](#licensing-information)

## Prerequisites & Setup

### Get a dotCMS Instance

**For Production Use:**

-   ☁️ [Cloud hosting options](https://www.dotcms.com/pricing) - managed solutions with SLA
-   🛠️ [Self-hosted options](https://dev.dotcms.com/docs/current-releases) - deploy on your infrastructure

**For Testing & Development:**

-   📝 [dotCMS demo site](https://dev.dotcms.com/docs/demo-site) - perfect for trying out the SDK
-   📝 Read-only access, ideal for building proof-of-concepts

**For Local Development:**

-   🐳 [Docker setup guide](https://github.com/dotCMS/core/tree/main/docker/docker-compose-examples/single-node-demo-site)
-   💻 [Local installation guide](https://dev.dotcms.com/docs/quick-start-guide)

#### Version Requirements

-   **Recommended**: dotCMS Evergreen
-   **Minimum**: dotCMS v25.05
-   **Best Experience**: Latest Evergreen release

### Create a dotCMS API Key

> [!TIP]
> Make sure your API Token has read-only permissions for Pages, Folders, Assets, and Content. Using a key with minimal permissions follows security best practices.

This integration requires an API Key with read-only permissions for security best practices:

1. Go to the **dotCMS admin panel**.
2. Click on **System** > **Users**.
3. Select the user you want to create the API Key for.
4. Go to **API Access Key** and generate a new key.

For detailed instructions, please refer to the [dotCMS API Documentation - Read-only token](https://dev.dotcms.com/docs/rest-api-authentication#ReadOnlyToken).

### Installation

Install the SDK and required dependencies:

```bash
npm install @dotcms/client@next @dotcms/types@next
```

> [!TIP]
> If you are working with pure JavaScript, you can avoid installing the `@dotcms/types` package.

## Quickstart

Here's a basic setup of the dotCMS Client SDK to help you get started:

```typescript
import { createDotCMSClient } from '@dotcms/client/next';

// Create a client instance
const client = createDotCMSClient({
    dotcmsUrl: 'https://your-dotcms-instance.com',
    authToken: 'your-auth-token', // Optional for public content
    siteId: 'your-site-id' // Optional site identifier
});

// Start using the client!
const { pageAsset } = await client.page.get('/about-us');
console.log(pageAsset.page.title);
```

### Example Projects

While there isn't a dedicated example project specifically for the client SDK, you can see it in action within these full-stack examples:

-   [Next.js Example](https://github.com/dotCMS/core/tree/main/examples/nextjs) - Modern React-based SSR implementation
-   [Angular Example](https://github.com/dotCMS/core/tree/main/examples/angular) - Enterprise Angular implementation
-   [Astro Example](https://github.com/dotCMS/core/tree/main/examples/astro) - Astro implementation

These examples demonstrate how to use the client SDK as part of a complete web application.

## Key Concepts

| Term         | Description                                           | Documentation                                                                  |
| ------------ | ----------------------------------------------------- | ------------------------------------------------------------------------------ |
| `pageAsset`  | The page data structure containing layout and content | [Page API](https://dev.dotcms.com/docs/page-rest-api-layout-as-a-service-laas) |
| `contentlet` | A single piece of content in dotCMS                   | [Content API](https://dev.dotcms.com/docs/content)                             |
| `collection` | A group of contentlets of the same type               | [Content API](https://dev.dotcms.com/docs/search)                              |
| `graphql`    | Query language used to extend API responses           | [GraphQL](https://dev.dotcms.com/docs/graphql)                                 |

## API Reference

### Client Initialization

**Overview:**
The Client Initialization is the first step in using the dotCMS Client SDK. It allows you to create a new client instance with your dotCMS configuration.

**Configuration Options:**

| Option           | Type           | Required | Description                                                   |
| ---------------- | -------------- | -------- | ------------------------------------------------------------- |
| `dotcmsUrl`      | string         | ✅       | Your dotCMS instance URL                                      |
| `authToken`      | string         | ✅       | Authentication token                                          |
| `siteId`         | string         | ❌       | Site identifier (falls back to default site if not specified) |
| `requestOptions` | RequestOptions | ❌       | Additional fetch options                                      |

**Usage:**

```typescript
import { createDotCMSClient } from '@dotcms/client/next';

const client = createDotCMSClient({
    dotcmsUrl: 'https://your-dotcms-instance.com',
    authToken: 'your-auth-token',
    siteId: 'your-site-id',
    requestOptions: {
        headers: { 'Custom-Header': 'value' },
        cache: 'default'
    }
});
```

### client.page.get(): Fetching Page Content

The `client.page.get()` method is your primary way to retrieve page content from dotCMS using the SDK. It abstracts away the complexity of raw REST or GraphQL calls, letting you fetch structured page data with just a single method.

#### What It Does

* Retrieves a **page asset** from dotCMS given its path (e.g., `/about-us`)
* Automatically includes layout, containers, and content
* Lets you **customize the request** with options like language, preview mode, or user persona
* Supports **GraphQL extensions** so you can request exactly the fields or content fragments you need

#### Basic Usage

Here's the simplest way to fetch a page by its URL:

```ts
const { pageAsset } = await client.page.get('/about-us');
console.log(pageAsset.page.title);
```

You can now render this content or pass it to your components.

#### Customizing the Request

You can customize the request to fetch different languages, rendering modes, or user personas:

```ts
const { pageAsset } = await client.page.get('/about-us', {
    languageId: '2',
    fireRules: true,
    personaId: '1234'
});
```

#### Fetching Additional Content with GraphQL

You can also pull in related content, like blog posts or navigation menus, using the `graphql` option:

```ts
const { pageAsset, content } = await client.page.get('/about-us', {
    graphql: {
        page: `
            title
            vanityUrl {
                url
            }
        `,
        content: {
            blogPosts: `
                BlogCollection(limit: 3) {
                    title
                    urlTitle
                }
            `,
            navigation: `
                DotNavigation(uri: "/", depth: 2) {
                    href
                    title
                    children {
                        href
                        title
                    }
                }
            `
        }
    }
});
```

#### Request Options

The `page.get()` method accepts an optional second argument of type [`DotCMSPageRequestParams`](https://github.com/dotCMS/core/blob/6e003eb697554ea9636a1fec59bc0fa020b84390/core-web/libs/sdk/types/src/lib/client/public.ts#L5-L54). This lets you customize how the page is fetched. Common options include:

| Option       | Type             | Description                                      |
| ------------ | ---------------- | ------------------------------------------------ |
| `languageId` | string \| number | Language version of the page                     |
| `mode`       | string           | Rendering mode: `LIVE`, `PREVIEW_MODE`, etc.     |
| `personaId`  | string           | Personalize content based on persona ID          |
| `graphql`    | object           | Extend the response with additional content/data |
| `fireRules`  | boolean          | Whether to trigger page rules                    |

> 💡 See [`DotCMSPageRequestParams`](https://github.com/dotCMS/core/blob/6e003eb697554ea9636a1fec59bc0fa020b84390/core-web/libs/sdk/types/src/lib/client/public.ts#L5-L54) for a full list of supported options.

#### Method Signature

```ts
get<T extends DotCMSExtendedPageResponse = DotCMSPageResponse>(
  url: string,
  options?: DotCMSPageRequestParams
): Promise<DotCMSComposedPageResponse<T>>;
```

#### Why Use `page.get()`?

* Fetch everything needed to render a page with one call
* Avoid building multiple API queries manually
* Type-safe, customizable, and extensible with GraphQL
* Works with dotCMS content localization and personalization out of the box

### Content API

**Overview:**
The Content API allows you to fetch content collections from dotCMS. It provides a builder pattern for constructing complex queries and filtering content based on various criteria.

The `getCollection` method uses immutable patterns, so each method call returns a new instance. This means you can reuse partial queries by storing them before adding additional filters.

**Builder Methods:**

| Method       | Arguments                | Description                        |
| ------------ | ------------------------ | ---------------------------------- |
| `query()`    | `string` \| `BuildQuery` | Filter content using query builder |
| `limit()`    | `number`                 | Set page size                      |
| `page()`     | `number`                 | Set page number                    |
| `sortBy()`   | `SortBy[]`               | Order results                      |
| `language()` | `number \| string`       | Set content language               |

**Usage:**

```typescript
const blogs = await client.content
    .getCollection('Blog')
    .query((qb) =>
        qb.field('title').contains('dotCMS').and().field('publishDate').greaterThan('2023-01-01')
    )
    .limit(10)
    .page(1)
    .fetch();
```

**With TypeScript:**

```typescript
import { DotCMSBasicContentlet } from '@dotcms/types';

// Define your content type interface
interface BlogPost extends DotCMSBasicContentlet {
    title: string;
    publishDate: string;
    author: string;
    blogContent: {
        json: string;
    };
    urlTitle: string;
    tags: string[];
}

// Use the generic type parameter for type safety
const response = await client.content.getCollection<BlogPost>('Blog').fetch();

// Now you get full TypeScript support
response.contentlets.forEach((post: BlogPost) => {
    console.log(post.title); // TypeScript knows this exists
    console.log(post.author); // TypeScript knows this exists
    console.log(post.tags.join(', ')); // Type-safe array access
});
```

### client.navigation.get(): Fetching Navigation Structure

The `client.navigation.get()` method fetches a structured view of a site's file and folder hierarchy from dotCMS. It's useful for building menus and navigation UIs.

#### 📌 Prefer Fetching Navigation via `page.get()`

In most cases, you **don't need to call** `client.navigation.get()` separately. You can fetch navigation data as part of your page request using the `graphql.content` option in `client.page.get()`. This approach:

* Combines page content and navigation in **one request**
* Reduces network overhead
* Keeps your data fetching consistent and performant

```ts
const { pageAsset, content } = await client.page.get('/about-us', {
  graphql: {
    content: {
      navigation: `DotNavigation(uri: "/", depth: 2) { title href children { title href } }`
    }
  }
});
```

#### Basic Usage

Here's the simplest way to fetch the root-level navigation:

```ts
const nav = await client.navigation.get('/');
console.log(nav);
```

#### Customizing the Request

You can tailor the navigation structure using optional parameters:

```ts
const nav = await client.navigation.get('/', {
    depth: 2,
    languageId: 1
});
```

#### Request Options

The `navigation.get()` method accepts an optional second argument with the following parameters:

| Option       | Type   | Description                                |
| ------------ | ------ | ------------------------------------------ |
| `depth`      | number | Number of child levels to include          |
| `languageId` | number | Language ID for localized navigation names |

> 💡 For typical use cases, setting `depth: 2` will include top-level navigation and one level of children.

#### Method Signature

```ts
get(
  uri: string,
  options?: {
    depth?: number;
    languageId?: number;
  }
): Promise<DotCMSNavigationItem[]>;
```

### Why Use `navigation.get()`?

* Build dynamic menus and site trees with minimal configuration
* Avoid manual parsing or custom REST calls
* Support localized navigation out of the box
* Easily control navigation depth for responsive and nested UIs

## TypeScript Support

As mentioned earlier, dotCMS provides a rich set of types provided by the `@dotcms/types@next` package. These types can be leveraged to ensure proper typing for your page and content data, enhancing type safety and developer experience.

### Usage Example

You can use these types to define interfaces for your content and page structures, ensuring that your application benefits from TypeScript's type-checking capabilities:

```typescript
// Import the base DotCMS types
import { DotCMSPageAsset, DotCMSBasicContentlet } from '@dotcms/types';

// Define the page structure by extending the base DotCMSPageAsset
interface AboutUsPage extends DotCMSPageAsset {
    vanityUrl: {
        url: string;
    };
}

// Define interfaces for your content types
interface BlogPost extends DotCMSBasicContentlet {
    title: string;
    identifier: string;
    urlTitle: string;
    blogContent: {
        json: string;
    };
}

interface TeamMember {
    name: string;
    position: string;
    bio?: string;
}

// Define the content response structure
interface AboutUsContent {
    blogPosts: BlogPost[];
    teamMembers: TeamMember[];
}

// Use the type parameters to get fully typed responses
const response = await client.page.get<{ pageAsset: AboutUsPage; content: AboutUsContent }>(
    '/about-us',
    {
        languageId: '1',
        fireRules: true,
        graphql: {
            page: `
            title
            pageId
            vanityUrl {
                url
            }
        `,
            content: {
                blogPosts: `
                BlogCollection(limit: 3) {
                    title
                    identifier
                    ...blogFragment
                }
            `,
                teamMembers: `
                TeamMemberCollection(limit: 5) {
                    name
                    position
                    bio
                }
            `
            },
            fragments: [
                `
                fragment blogFragment on Blog {
                    urlTitle
                    blogContent {
                        json
                    }
                }
            `
            ]
        }
    }
);

const { pageAsset, content } = response;

// Now you get full TypeScript support
console.log(pageAsset.vanityUrl.url); // TypeScript knows this exists
console.log(content.blogPosts[0].title); // TypeScript knows this exists
console.log(content.teamMembers[0].position); // TypeScript knows this exists
```

## Best Practices

### Fetching Content and Navigation

While the Content and Navigation APIs can be called independently, it's recommended to fetch this data as part of your page requests using GraphQL options. This approach:

-   **Reduces API Calls**: Get all needed data in a single request
-   **Improves Performance**: Less network overhead and faster page loads
-   **Maintains Consistency**: Data is fetched at the same time as the page

```typescript
// ✅ Recommended: Single API call with GraphQL
const { pageAsset, content } = await client.page.get('/about', {
    graphql: {
        content: {
            blogs: 'BlogCollection(limit: 3) { ... }',
            navigation: 'DotNavigation(uri: "/", depth: 2) { ... }'
        }
    }
});

// ❌ Not Recommended: Multiple separate API calls
const { pageAsset } = await client.page.get('/about');
const blogs = await client.content.getCollection('Blog').fetch();
const nav = await client.navigation.get('/');
```

Only use standalone API calls when you need to:

-   Fetch data independently from pages
-   Update content collections dynamically
-   Handle navigation changes without page reloads

## dotCMS Support

We offer multiple channels to get help with the dotCMS Client SDK:

-   **GitHub Issues**: For bug reports and feature requests, please [open an issue](https://github.com/dotCMS/core/issues/new/choose) in the GitHub repository.
-   **Community Forum**: Join our [community discussions](https://community.dotcms.com/) to ask questions and share solutions.
-   **Stack Overflow**: Use the tag `dotcms-client` when posting questions.

When reporting issues, please include:

-   SDK version you're using
-   dotCMS version
-   Minimal reproduction steps
-   Expected vs. actual behavior

Enterprise customers can access premium support through the [dotCMS Support Portal](https://dev.dotcms.com/docs/help).

## How To Contribute

GitHub pull requests are the preferred method to contribute code to dotCMS. We welcome contributions to the DotCMS UVE SDK! If you'd like to contribute, please follow these steps:

1. Fork the repository [dotCMS/core](https://github.com/dotCMS/core)
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

Please ensure your code follows the existing style and includes appropriate tests.

## Licensing Information

dotCMS comes in multiple editions and as such is dual-licensed. The dotCMS Community Edition is licensed under the GPL 3.0 and is freely available for download, customization, and deployment for use within organizations of all stripes. dotCMS Enterprise Editions (EE) adds several enterprise features and is available via a supported, indemnified commercial license from dotCMS. For the differences between the editions, see [the feature page](http://www.dotcms.com/cms-platform/features).

This SDK is part of dotCMS's dual-licensed platform (GPL 3.0 for Community, commercial license for Enterprise).

[Learn more ](https://www.dotcms.com)at [dotcms.com](https://www.dotcms.com).
