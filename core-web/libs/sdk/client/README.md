# dotCMS Client SDK

The `@dotcms/client` is a powerful JavaScript/TypeScript SDK designed to simplify the integration of dotCMS content into your applications. Whether building dynamic websites, content-driven apps, or managing content programmatically, this SDK offers an intuitive API for seamless and type-safe content management, enabling you to create engaging experiences effortlessly.

### When to Use It:

-   Building headless frontends that need dotCMS content
-   Creating server-side applications that manage content
-   Developing multi-language or personalized experiences
-   Implementing dynamic navigation and page structures
-   Automating content workflows and operations

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

```bash
npm install @dotcms/client@next
```

#### Dev Dependencies

This package has the following dev dependencies for type definitions:

| Dependency      | Version | Description                   |
| --------------- | ------- | ----------------------------- |
| `@dotcms/types` | latest  | Required for type definitions |

Install dev dependencies:

```bash
npm install @dotcms/types@next --save-dev
```

## Quickstart

Install the SDK and required dependencies:

```bash
npm install @dotcms/client@next @dotcms/types
```

Initialize and use the client:

```javascript
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

### Page API

**Overview:**

The [Page API](https://dev.dotcms.com/docs/page-rest-api-layout-as-a-service-laas) allows you to fetch pages and their associated content from dotCMS. It provides a streamlined interface for retrieving page data and related content.

**Parameters:**

| Option        | Type            | Required | Default            | Description                                                 |
| ------------- | --------------- | -------- | ------------------ | ----------------------------------------------------------- |
| `siteId`      | string          | ❌       | From client config | ID of the site to interact with                             |
| `mode`        | string          | ❌       | `LIVE`             | Page rendering mode: 'EDIT_MODE', 'PREVIEW_MODE', or 'LIVE' |
| `languageId`  | string\|number  | ❌       | `1`                | Language ID for content localization                        |
| `personaId`   | string          | ❌       | —                  | ID of the persona for personalized content                  |
| `fireRules`   | boolean\|string | ❌       | false              | Whether to execute rules set on the page                    |
| `publishDate` | string          | ❌       | —                  | Publication date for the requested page                     |
| `variantName` | string          | ❌       | —                  | Name of the specific page variant to retrieve               |
| `graphql`     | object          | ❌       | —                  | GraphQL options for extending the response                  |

**GraphQL Options:**

| Option      | Type                   | Description                                       |
| ----------- | ---------------------- | ------------------------------------------------- |
| `page`      | string                 | GraphQL query to extend the page response         |
| `content`   | Record<string, string> | Named GraphQL queries to fetch additional content |
| `variables` | Record<string, string> | Variables to use in GraphQL queries               |
| `fragments` | string[]               | GraphQL fragments for reuse across queries        |

The GraphQL options allow you to create powerful, flexible queries that can fetch exactly the data you need in a single request. With GraphQL, you can:

-   Deeply traverse relationships between content
-   Combine multiple content types in a single query
-   Specify precise field selection to optimize response size
-   Use fragments for reusable query parts

For detailed information about GraphQL capabilities, query syntax, and best practices, refer to the [official dotCMS GraphQL documentation](https://dev.dotcms.com/docs/graphql).

**Usage:**

```typescript
const { pageAsset, content } = await client.page.get('/about-us', {
    languageId: '1',
    mode: 'PREVIEW_MODE',
    depth: '1',
    graphql: {
        page: `
            containers {
                containerContentlets {
                    uuid
                    contentlets {
                        title
                    }
                }
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
                  hash
                  href
                  title
                  target
                  type
                  children {
                      hash
                      href
                      title
                      target
                      type
                  }
              }
            `
        }
    }
});
```

#### Method Signature:

```typescript
get<T extends DotCMSExtendedPageResponse = DotCMSPageResponse>(
    url: string,
    options?: DotCMSPageRequestParams
): Promise<DotCMSComposedPageResponse<T>>;
```

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

### Navigation API

**Overview:**

The [Navigation API](https://dev.dotcms.com/docs/navigation-rest-api) allows you to fetch site navigation structures from dotCMS. It provides a streamlined interface for retrieving navigation data.

**Usage:**

```typescript
const nav = await client.navigation.get('/', {
    depth: 2,
    languageId: 1
});
```

**Options:**

| Option       | Type   | Description                   |
| ------------ | ------ | ----------------------------- |
| `depth`      | number | Levels of children to include |
| `languageId` | number | Navigation language           |

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
        mode: 'PREVIEW_MODE',
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
