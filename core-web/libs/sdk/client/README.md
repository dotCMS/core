# dotCMS Client SDK

The `@dotcms/client` is a JavaScript/TypeScript library for interacting with a dotCMS instance. It allows you to easily fetch pages, content, and navigation information in JSON format, as well as to make complex queries on content collections.

This client library provides a streamlined, promise-based interface to fetch pages and navigation API.


## What is it?

The `@dotcms/client` library serves as a specialized connector between your applications and dotCMS. This SDK:

- **Simplifies API interactions** with dotCMS by providing intuitive methods and builders
- **Handles authentication and requests** securely across all API endpoints
- **Works universally** in both browser-based applications and Node.js environments
- **Abstracts complexity** of the underlying REST APIs into developer-friendly interfaces
- **Provides type definitions** for enhanced developer experience and code completion

Whether you're building a headless frontend that consumes dotCMS content or a server-side application that needs to interact with your content repository, this client SDK eliminates boilerplate code and standardizes how you work with the dotCMS APIs.

## Getting Started

The dotCMS Client SDK provides a simple, powerful way to interact with your dotCMS instance. Follow these steps to quickly get up and running:

### Quick Start

1. **Install the SDK** (see [Installation](#how-to-install) below)
2. **Initialize the client**:

```javascript
// Import the client creator
import { createDotCMSClient } from '@dotcms/client/next';

// Create a client instance
const client = createDotCMSClient({
    dotcmsUrl: 'https://your-dotcms-instance.com',
    authToken: 'your-auth-token', // Optional for public content
    siteId: 'your-site-id'        // Optional site identifier
});

// Start using the client!
const pageData = await client.page.get('/about-us');
console.log(pageData.pageAsset.title);
```

### Example Projects

While there isn't a dedicated example project specifically for the client SDK, you can see the SDK in action within these full-stack examples:

* [Next.js Example](https://github.com/dotCMS/core/tree/main/examples/nextjs) - Modern React-based SSR implementation
* [Angular Example](https://github.com/dotCMS/core/tree/main/examples/angular) - Enterprise Angular implementation

These examples demonstrate how to use the client SDK as part of a complete web application.

## Table of Contents

- [Getting Started](#getting-started)
  - [Quick Start](#quick-start)
  - [Example Projects](#example-projects)
- [Module Formats](#module-formats)
  - [ES Modules](#es-modules)
  - [CommonJS](#commonjs)
- [How to Install](#how-to-install)
- [Dev Dependencies](#dev-dependencies)
- [Browser Compatibility](#browser-compatibility)
- [Common Use Cases](#common-use-cases)
  - [Content Listing with Search](#content-listing-with-search)
  - [Multilingual Content](#multilingual-content)
- [Detailed API Documentation](#detailed-api-documentation)
  - [Initialization: createDotCMSClient](#initialization-createdotcmsclient)
  - [Page API: client.page.get()](#page-api-clientpageget)
    - [Parameters](#parameters)
    - [DotCMSPageRequestParams Options](#dotcmspagerequestparams-options)
    - [GraphQL Options](#graphql-options)
    - [Return Value](#return-value)
    - [Basic Usage](#basic-usage)
    - [With Custom Options](#with-custom-options)
    - [Advanced Usage with GraphQL](#advanced-usage-with-graphql)
    - [TypeScript Example with Typed Content](#typescript-example-with-typed-content)
    - [Error Handling](#error-handling)
    - [Implementation Details](#implementation-details)
  - [Navigation API: client.navigation.get()](#navigation-api-clientnavigationget)
    - [Parameters](#parameters-1)
    - [DotCMSNavigationRequestParams Options](#dotcmsnavigationrequestparams-options)
    - [Return Value](#return-value-1)
    - [Basic Usage](#basic-usage-1)
    - [Error Handling](#error-handling-1)
  - [Content API: client.content.getCollection()](#content-api-clientcontentgetcollection)
    - [Parameters](#parameters-2)
    - [Return Value](#return-value-2)
    - [CollectionBuilder Methods](#collectionbuilder-methods)
    - [Basic Usage](#basic-usage-2)
    - [With TypeScript](#with-typescript)
    - [Pagination Example](#pagination-example)
    - [Filtering with Query Builder](#filtering-with-query-builder)
    - [Sorting Example](#sorting-example)
    - [Language Filtering](#language-filtering)
    - [Combining Multiple Methods](#combining-multiple-methods)
    - [Error Handling](#error-handling-2)
    - [Advanced: Getting Related Content](#advanced-getting-related-content)
    - [Implementation Details](#implementation-details-1)
- [Performance Considerations](#performance-considerations)
- [dotCMS Support](#dotcms-support)
- [How To Contribute](#how-to-contribute)
- [Licensing Information](#licensing-information)

## Module Formats
`@dotcms/client` supports both ES modules and CommonJS. You can import it using either syntax:

### ES Modules

```javascript
import { createDotCMSClient } from '@dotcms/client/next';
```

### CommonJS

```javascript
const { createDotCMSClient } = require('@dotcms/client/next');
```

## How to Install

Install the client SDK in your project using your preferred package manager:

```bash
# Using npm
npm install @dotcms/client@next

# Using yarn
yarn add @dotcms/client@next

# Using pnpm
pnpm add @dotcms/client@next
```

## Dev Dependencies

This package has the following dev dependencies that you'll need to install in your project:

| Dependency | Version | Description |
|------------|---------|-------------|
| `@dotcms/types` | latest | Required for type definitions |

Install dev dependencies:

```bash
# Using npm
npm install @dotcms/types --save-dev

# Using yarn
yarn add @dotcms/types --dev

# Using pnpm
pnpm add @dotcms/types --dev
```

## Browser Compatibility

The `@dotcms/client` package is compatible with the following browsers:

| Browser | Minimum Version | TLS Version |
|---------|----------------|-------------|
| Chrome  | Latest 2 versions | TLS 1.2+ |
| Edge    | Latest 2 versions | TLS 1.2+ |
| Firefox | Latest 2 versions | TLS 1.2+ |

## Common Use Cases

### Content Listing with Search

A common pattern is to create a listing page with initial content, then allow users to search and filter that content:

```typescript
// 1. Initial page load with default content
const initialPageLoad = async () => {
  // Get the page structure and initial blog posts
  const pageData = await client.page.get('/blog', {
    graphql: {
      content: {
        featuredBlogs: `
          BlogCollection(limit: 5) {
            title
            urlTitle
            publishDate
          }
        `
      }
    }
  });
  
  return {
    pageAsset: pageData.pageAsset,
    featuredBlogs: pageData.content.featuredBlogs
  };
};

// 2. Handle user search - client side
const handleSearch = async (searchTerm) => {
  // When user searches, fetch filtered content
  const searchResults = await client.content
    .getCollection('Blog')
    .query(qb => qb.field('title').contains(searchTerm))
    .limit(10)
    .page(1)
    .fetch();
    
  return searchResults.contentlets;
};
```

### Multilingual Content

Another common use case is retrieving content in different languages:

```typescript
// Function to get page in specified language
const getPageInLanguage = async (path, languageId) => {
  return client.page.get(path, {
    languageId: languageId.toString()
  });
};

// Spanish version
const spanishPage = await getPageInLanguage('/about-us', 2);

// English version
const englishPage = await getPageInLanguage('/about-us', 1);
```

## Detailed API Documentation

### Initialization: `createDotCMSClient`

#### Description:

Initializes the client with your dotCMS instance details.

#### Parameters:

| Argument | Type | Required | Default | Description |
|------|------|----------|---------|-------------|
| `dotcmsUrl` | `string` | Yes | --- | The URL of your dotCMS instance |
| `authToken` | `string` | Yes | --- | The authentication token for your dotCMS instance |
| `siteId` | `string` | No | "default" | The ID of the site you want to interact with |
| `requestOptions` | `RequestOptions` | No | {} | Additional options for fetch requests |


> **Note:** The `siteId` is optional, but if you don't provide it, you may need to specify it in individual API calls. Learn more about sites in the [dotCMS Multi-Site Management](https://dev.dotcms.com/docs/multi-site-management).


```javascript
const dotCMSClient = createDotCMSClient({
    dotcmsUrl: 'https://your-dotcms-instance.com',
    authToken: 'your-auth-token',
    siteId: 'your-site-id',
    requestOptions: {
      headers: {
        'Custom-Header': 'value'
      },
      cache: 'default'
    }
});
```

#### Implementation Details

The `createDotCMSClient` function works by:

1. **Validating the dotCMS URL**: Ensures the URL is properly formatted and accessible
2. **Setting up authentication**: Configures the authorization header with the provided token
3. **Creating client instances**:
   - Initializes the Page API client for page operations
   - Initializes the Navigation API client for navigation structures
   - Initializes the Content API client for content operations

The client maintains a single connection to your dotCMS instance, making it efficient to reuse across your application. It's recommended to:

- Create a single client instance at the application level
- Reuse this instance throughout your components/services
- Avoid creating new client instances for each request

### Page API: `client.page.get()`

#### Description:

The Page API allows you to retrieve pages from your dotCMS instance along with their associated content. This method returns a structured response containing both the page asset and any additional content requested through GraphQL.

#### Method Signature:

```typescript
get<T>(url: string, options?: DotCMSPageRequestParams): Promise<DotCMSComposedPageResponse<T>>
```

#### Parameters:

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `url` | `string` | Yes | The path/URL of the page to retrieve (e.g., '/about-us') |
| `options` | `DotCMSPageRequestParams` | No | Configuration options for the page request |

#### DotCMSPageRequestParams Options:

| Option | Type | Required | Default | Description |
|--------|------|----------|---------|-------------|
| `siteId` | `string` | No | From client config | ID of the site you want to interact with |
| `mode` | `string` | No | 'LIVE' | Page rendering mode: 'EDIT_MODE', 'PREVIEW_MODE', or 'LIVE' |
| `languageId` | `string\|number` | No | '1' | Language ID for content localization |
| `personaId` | `string` | No | — | ID of the persona for personalized content |
| `fireRules` | `boolean\|string` | No | false | Whether to execute rules set on the page |
| `depth` | `0\|1\|2\|3\|'0'\|'1'\|'2'\|'3'` | No | '0' | Depth of related content to retrieve via Relationship fields |
| `publishDate` | `string` | No | — | Publication date for the requested page |
| `variantName` | `string` | No | — | Name of the specific page variant to retrieve |
| `graphql` | `object` | No | — | GraphQL options for extending the response |

#### GraphQL Options:

| Option | Type | Description |
|--------|------|-------------|
| `page` | `string` | GraphQL query to extend the page response |
| `content` | `Record<string, string>` | Named GraphQL queries to fetch additional content |
| `variables` | `Record<string, string>` | Variables to use in GraphQL queries |
| `fragments` | `string[]` | GraphQL fragments for reuse across queries |

#### Return Value:

The method returns a Promise that resolves to an object with the following structure:

```typescript
{
  pageAsset: T, // The page data
  content: Record<string, unknown>, // Additional content requested via GraphQL
  graphql: {
    query: string, // The GraphQL query that was executed
    variables: Record<string, unknown> // The variables used in the query
  }
}
```

#### Basic Usage:

Fetch a page with default options:

```javascript
// Get a page with default options
const { pageAsset, content } = await client.page.get('/about-us');

// Access page data
console.log(pageAsset.title);
console.log(pageAsset.containers);
```

#### With Custom Options:

```javascript
// Get a page with specific language and mode
const pageData = await client.page.get('/about-us', {
    languageId: '2', // Spanish language ID
    mode: 'PREVIEW_MODE',
    depth: '1' // Include first level of related content
});
```

#### Advanced Usage with GraphQL:

Fetch a page with additional content and custom queries:

```javascript
const { pageAsset, content } = await client.page.get('/about-us', {
    languageId: '1',
    mode: 'PREVIEW_MODE',
    graphql: {
        // Extend page data with container information
        page: `
            title
            containers {
                containerContentlets {
                    uuid
                    contentlets {
                        title
                    }
                }
            }
        `,
        // Fetch additional content collections
        content: {
            // This will be available as content.blogPosts
            blogPosts: `
                BlogCollection(limit: 3) {
                    title
                    identifier
                    ...blogFragment
                }
            `,
            // This will be available as content.teamMembers
            teamMembers: `
                TeamMemberCollection(limit: 5) {
                    name
                    position
                }
            `
        },
        // Define reusable fragments
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
});

// Access page data
console.log(pageAsset.title);
console.log(pageAsset.containers);

// Access additional content
console.log(content.blogPosts); // List of blog posts
console.log(content.teamMembers); // List of team members
```

#### TypeScript Example with Typed Content:

When using TypeScript, you can leverage the generic type parameter to get proper typing for your page and content data:

```typescript
// Import the base DotCMS types
import { DotCMSPageAsset } from '@dotcms/types';

// Define the page structure by extending the base DotCMSPageAsset
interface AboutUsPage extends DotCMSPageAsset {
    vanityUrl: {
        url: string;
    }
}

// Define interfaces for your content types
interface BlogPost {
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
const response = await client.page.get<{ pageAsset: AboutUsPage; content: AboutUsContent; }>('/about-us', {
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
});

const { pageAsset, content } = response;

// Now you get full TypeScript support
const firstBlogPost = content.blogPosts[0];
console.log(firstBlogPost.title);         // TypeScript knows this exists

const firstTeamMember = content.teamMembers[0];
console.log(firstTeamMember.name);         // TypeScript knows this exists

// The page asset is also fully typed
console.log(pageAsset.title);
console.log(pageAsset.vanityUrl.url);
```

#### Error Handling:

```javascript
try {
    const pageData = await client.page.get('/invalid-page');
} catch (error) {
    console.error('Failed to fetch page:', error.message);
    // You can also access the original GraphQL query and variables
    console.log(error.graphql?.query);
    console.log(error.graphql?.variables);
}
```

#### Implementation Details

The Page API works by making GraphQL requests to the dotCMS instance to retrieve page data. It combines:

1. **Page Asset Data**: The page structure, layout, and metadata
2. **Content Data**: The actual content populated within the page containers
3. **GraphQL Extensions**: Additional content queries to enhance the response

When fetching a page, the client:
- Constructs a GraphQL query based on the provided options
- Adds necessary variables like languageId, mode, and siteId
- Makes the request with proper authentication
- Processes the response to extract both page and content data
- Returns a structured object containing everything needed to render the page

The GraphQL approach provides significant flexibility by allowing you to:
- Precisely specify which page fields you need
- Request related content in the same API call
- Optimize network requests by getting exactly what you need in one call

> **Note:** When working with very complex pages that contain many content items, consider paginating large content collections or using the Content API separately for content that doesn't need to be loaded immediately.

### Navigation API: `client.navigation.get()`

#### Description:

The Navigation API allows you to retrieve navigation information from your dotCMS instance. This method returns a structured response containing the navigation data.

#### Method Signature:

```typescript
get(path: string, options?: DotCMSNavigationRequestParams): Promise<unknown>
```

#### Parameters:

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `path` | `string` | Yes | The path/URL of the navigation to retrieve (e.g., '/about-us') |
| `options` | `DotCMSNavigationRequestParams` | No | Configuration options for the navigation request |

#### DotCMSNavigationRequestParams Options:

| Option | Type | Required | Default | Description |
|--------|------|----------|---------|-------------|
| `depth` | `number` | No | 0 | Depth of the navigation to retrieve |
| `languageId` | `number` | No | 1 | Language ID for content localization |

#### Return Value:

The method returns a Promise that resolves to the navigation data.

#### Basic Usage:

```javascript
const navigationData = await client.navigation.get('/about-us');
console.log(navigationData);
```

#### Error Handling:

```javascript
try {
    const navigationData = await client.navigation.get('/invalid-path');
} catch (error) {
    console.error('Failed to fetch navigation:', error.message);
}
```

### Content API: `client.content.getCollection()`

#### Description:

The Content API allows you to retrieve collections of content from your dotCMS instance using a powerful fluent builder pattern. This approach lets you construct complex queries step by step, making it easier to filter, sort, and paginate content.

#### Method Signature:

```typescript
getCollection<T = unknown>(contentType: string): CollectionBuilder<T>
```

#### Parameters:

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `contentType` | `string` | Yes | The name of the content type to retrieve (e.g., 'Blog', 'Product', 'News') |

#### Return Value:

Returns a `CollectionBuilder<T>` instance that provides methods for configuring and executing the query.

### CollectionBuilder Methods

The CollectionBuilder offers a fluent interface for building content queries:

| Method | Description |
|--------|-------------|
| `query(buildFn)` | Filters content using a query builder function or raw Lucene query string |
| `language(id)` | Sets the language ID for content localization |
| `limit(n)` | Sets the maximum number of items to return (pagination) |
| `page(n)` | Sets the page number to retrieve (pagination) |
| `depth(n)` | Sets the depth for retrieving related content (0-3) |
| `sortBy(fields)` | Specifies fields and order for sorting results |
| `draft()` | Retrieves only draft content |
| `render()` | Renders velocity templates in content |
| `fetch()` | Executes the query and returns results |

#### Basic Usage:

Retrieve a collection of content items with default settings:

```typescript
const response = await client.content
    .getCollection('Blog')
    .fetch();

console.log(response.contentlets); // Array of blog items
```

#### With TypeScript:

```typescript
// Define your content type interface
interface BlogPost {
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
const response = await client.content
    .getCollection<BlogPost>('Blog')
    .fetch();

// Now you get full TypeScript support
response.contentlets.forEach(post => {
    console.log(post.title);       // TypeScript knows this exists
    console.log(post.author);      // TypeScript knows this exists
    console.log(post.tags.join(', ')); // Type-safe array access
});
```

#### Pagination Example:

```typescript
// Get the second page of blog posts, 10 items per page
const response = await client.content
    .getCollection('Blog')
    .limit(10)
    .page(2)
    .fetch();

console.log(`Showing items ${(response.page - 1) * response.limit + 1} to ${response.page * response.limit}`);
console.log(`Total items: ${response.totalRecords}`);
```

#### Filtering with Query Builder:

```typescript
// Find blogs by a specific author published after a certain date
const response = await client.content
    .getCollection('Blog')
    .query(qb => 
        qb.field('author').equals('John Doe')
          .and()
          .field('publishDate').greaterThan('2023-01-01')
    )
    .fetch();
```

#### Sorting Example:

```typescript
// Sort blog posts by publish date (newest first) and then by title
const response = await client.content
    .getCollection('Blog')
    .sortBy([
        { field: 'publishDate', order: 'desc' },
        { field: 'title', order: 'asc' }
    ])
    .fetch();
```

#### Language Filtering:

```typescript
// Get content in Spanish (assuming language ID 2 is Spanish)
const response = await client.content
    .getCollection('Blog')
    .language(2)
    .fetch();
```

#### Combining Multiple Methods:

```typescript
// Complex query combining multiple filters and options
const response = await client.content
    .getCollection('Blog')
    .query(qb => 
        qb.field('tags').contains('technology')
          .and()
          .parenthesis(
              subQb => subQb.field('featured').equals('true')
                    .or()
                    .field('views').greaterThan('1000')
          )
    )
    .sortBy([{ field: 'publishDate', order: 'desc' }])
    .limit(5)
    .page(1)
    .language(1)
    .depth(1)
    .fetch();
```

#### Error Handling:

```typescript
try {
    const response = await client.content
        .getCollection('InvalidContentType')
        .fetch();
} catch (error) {
    console.error('Failed to fetch content:', error.message);
}
```

#### Implementation Details

The `getCollection` method uses a builder pattern to construct and execute content queries. This approach provides several advantages:

1. **Progressive Query Building**: Build complex queries step by step
2. **Type Safety**: Full TypeScript support at every stage of query building
3. **Method Chaining**: Clean, readable syntax for complex operations
4. **Deferred Execution**: The query only executes when `fetch()` is called

Under the hood, the builder:
- Constructs Lucene query syntax for content filtering
- Handles pagination parameter calculation
- Manages sorting and ordering parameters
- Ensures proper request formatting for the Content API endpoint

The method automatically handles:
- Error response processing
- Content structure normalization
- Relationship processing for related content (when using `depth()`)

> **Note:** The CollectionBuilder uses immutable patterns, so each method call returns a new instance. This means you can reuse partial queries by storing them before adding additional filters.

```typescript
// Create a base query for published blog posts
const baseQuery = client.content
  .getCollection('Blog')
  .limit(10);

// Reuse with different filters
const recentPosts = await baseQuery
  .sortBy([{ field: 'publishDate', order: 'desc' }])
  .fetch();

const popularPosts = await baseQuery
  .sortBy([{ field: 'views', order: 'desc' }])
  .fetch();
```

## Performance Considerations

For optimal performance when using the dotCMS Client SDK:

### Caching Strategies

You can leverage the browser's caching mechanisms by configuring the `requestOptions`:

```javascript
const client = createDotCMSClient({
  dotcmsUrl: 'https://your-dotcms-instance.com',
  authToken: 'your-auth-token',
  requestOptions: {
    // Use the browser's default cache behavior
    cache: 'default',
    
    // Or disable caching for real-time data
    // cache: 'no-cache'
  }
});
```

Setting `cache: 'no-cache'` will always fetch fresh data but may increase response times.

### Query Optimization

When using GraphQL with `page.get()` or building queries with `content.getCollection()`:

1. **Request only what you need**:
   - Specify explicit fields rather than fetching everything
   - Limit results to reasonable page sizes (10-20 items)

2. **Use appropriate depth**:
   - Set relationship depth carefully (0-3) - higher values increase response size
   - Consider separate requests for deeply nested content

3. **Structure your queries**:
   - Use GraphQL fragments for reusable field selections
   - Build complex queries incrementally for better maintainability

> **Note:** For large content collections (100+ items), consider implementing pagination or infinite scrolling rather than fetching everything at once.

## dotCMS Support

We offer multiple channels to get help with the dotCMS React SDK:

* **GitHub Issues**: For bug reports and feature requests, please [open an issue](https://github.com/dotCMS/core/issues/new/choose) in the GitHub repository.
* **Community Forum**: Join our [community discussions](https://community.dotcms.com/) to ask questions and share solutions.
* **Stack Overflow**: Use the tag `dotcms-react` when posting questions.

When reporting issues, please include:
- SDK version you're using
- React version
- Minimal reproduction steps
- Expected vs. actual behavior

Enterprise customers can access premium support through the [dotCMS Support Portal](https://dev.dotcms.com/docs/help).


## How To Contribute

GitHub pull requests are the preferred method to contribute code to dotCMS. We welcome contributions to the DotCMS UVE SDK! If you'd like to contribute, please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

Please ensure your code follows the existing style and includes appropriate tests.


## Licensing Information

dotCMS comes in multiple editions and as such is dual licensed. The dotCMS Community Edition is licensed under the GPL 3.0 and is freely available for download, customization and deployment for use within organizations of all stripes. dotCMS Enterprise Editions (EE) adds a number of enterprise features and is available via a supported, indemnified commercial license from dotCMS. For the differences between the editions, see [the feature page](http://www.dotcms.com/cms-platform/features).
