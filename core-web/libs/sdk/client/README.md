# dotCMS API Client - `@dotcms/client`

The `@dotcms/client` is a JavaScript/TypeScript library for interacting with a dotCMS instance. It allows you to easily fetch pages, content, and navigation information in JSON format, as well as to make complex queries on content collections.

> **⚠️ IMPORTANT:** Versions published under the `next` tag (`npm install @dotcms/client@next`) are experimental, in beta, and not code complete. For the current stable and functional version, please use `latest` (`npm install @dotcms/client@latest`). Once we release the stable version, we will provide a migration guide from the alpha to stable version. The current alpha version (under `latest`) will continue to work, allowing you to migrate progressively at your own pace.

This client library provides a streamlined, promise-based interface to fetch pages and navigation API.

## Table of Contents

- [Features](#features)
- [What's New](#whats-new)
- [What's Being Deprecated](#whats-being-deprecated)
- [Installation](#installation)
- [Browser Compatibility](#browser-compatibility)
- [Usage](#usage)
  - [ES Modules](#es-modules)
  - [CommonJS](#commonjs)
  - [Initialization](#initialization)
  - [Fetching a Page](#fetching-a-page)
    - [Example with all options](#example-with-all-options)
  - [Fetching Navigation](#fetching-navigation)
    - [Legacy Navigation Example](#legacy-navigation-example)
  - [Fetching a Collection of Content](#fetching-a-collection-of-content)
    - [Basic Usage](#basic-usage)
    - [Sorting Content](#sorting-content)
    - [Filtering by Language](#filtering-by-language)
    - [Using Complex Queries](#using-complex-queries)
    - [Fetching Draft Content](#fetching-draft-content)
    - [Setting Depth for Relationships](#setting-depth-for-relationships)
    - [Combining Multiple Methods](#combining-multiple-methods)
  - [Error Handling Example](#error-handling-example)
  - [Pagination](#pagination)
- [API Reference](#api-reference)
- [Contributing](#contributing)
- [Licensing](#licensing)
- [Support](#support)
- [Documentation](#documentation)
- [Getting Help](#getting-help)

## Features

-   Easy-to-use methods to interact with [dotCMS pages](https://www.dotcms.com/docs/latest/page-rest-api-layout-as-a-service-laas) and the [Navigation API](https://www.dotcms.com/docs/latest/navigation-rest-api).
-   Support for custom actions to communicate with the dotCMS page editor.
-   Comprehensive TypeScript typings for better development experience.

## What's New

- **Improved Collection Builder API:** Enhanced content fetching with a fluent builder pattern
- **TypeScript Support:** Comprehensive type definitions for better developer experience
- **Promise-based API:** Modern, async/await compatible interface for all API calls
- **Performance Optimizations:** Faster response times and reduced memory footprint
- **New Client Creation:** The `dotCMSCreateClient()` function replaces `DotCmsClient.init()` for better functional programming

> **Note:** Some deprecated features are being phased out. See [API Reference](#api-reference) for details.

## What's Being Deprecated

- **Legacy Editor APIs:** The `postMessageToEditor`, `initEditor`, and related functions are being phased out
- **Direct Content API:** The `content.get()` method is being replaced by the more powerful `content.getCollection()`
- **Callback-based APIs:** All callback-based methods are being replaced with Promise-based alternatives
- **DotCmsClient.init:** The static `DotCmsClient.init()` method is replaced by the `dotCMSCreateClient()` function
- **client.editor:** The editor functionality has been completely moved to the `@dotcms/uve` package

> **Note:** Deprecated items will continue to work for backward compatibility but will be removed in future major versions.

## Installation

To get started, install the client via npm or yarn:

```bash
npm install @dotcms/client
```

Or using Yarn:

```bash
yarn add @dotcms/client


## Browser Compatibility

The @dotcms/client package is compatible with the following browsers:

| Browser | Minimum Version | TLS Version |
|---------|----------------|-------------|
| Chrome  | Latest 2 versions | TLS 1.2+ |
| Edge    | Latest 2 versions | TLS 1.2+ |
| Firefox | Latest 2 versions | TLS 1.2+ |

## Usage

`@dotcms/client` supports both ES modules and CommonJS. You can import it using either syntax:

### ES Modules

```javascript
import { createDotCMSClient } from '@dotcms/client/next';
```

### CommonJS

```javascript
const { createDotCMSClient } = require('@dotcms/client/next');
```

### Initialization

First, initialize the client with your dotCMS instance details.

```javascript
const client = createDotCMSClient({
    dotcmsUrl: 'https://your-dotcms-instance.com',
    authToken: 'your-auth-token',
    siteId: 'your-site-id'
});
```

### Fetching a Page

You can retrieve the elements of any page in your dotCMS system in JSON format using the `client.page.get()` method.

```javascript
const pageData = await client.page.get('/your-page-path', {
    languageId: '1',
    personaId: 'optional-persona-id'
});
```


### Warning

If you are updating from a version that is lower than `0.0.1-beta.29`, be aware that the response from the `client.page.get` method changed the access to the page value from `page` to `pageAsset`.
This change was made to avoid redundancy on access inside of `page` object Ex. `page.page.title` -> `pageAsset.page.title`.

#### Example with all options

```javascript
// Fetching a page with all available options
const { pageAsset, content } = await client.page.get('/about-us', {
    languageId: '1',                 // Language ID (optional)
    siteId: 'demo.dotcms.com',       // Site ID (optional, defaults to the one provided during initialization)
    mode: 'PREVIEW_MODE',            // ADMIN_MODE, PREVIEW_MODE, or LIVE_MODE (optional)
    personaId: '123',                // Persona ID for personalization (optional)
    device: 'smartphone',            // Device for responsive rendering (optional)
    graphql: {                       // Extend page and/or content response (optional)
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
                search(query: "+contentType:Blog", limit: 3) {
                    title
                    identifier
                    ...blogFragment
                }
            `,
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

// Access page data
console.log(pageAsset.containers);

// Access content data
console.log(content.blogPosts);
```

### Fetching Navigation

The new API allows you to fetch navigation data using GraphQL through the page.get method:

```javascript
// Fetch navigation using the page.get method with GraphQL
const { content } = await client.page.get('/', {
    languageId: '1',
    graphql: {
        content: {
            nav: `
                query {
                    nav {
                        identifier
                        path
                        label
                        children {
                            identifier
                            path
                            label
                        }
                    }
                }
            `
        }
    }
});

// Access navigation data
console.log(content.nav);
```

> **Note:** The legacy `client.nav.get()` method is still available but deprecated.

#### Legacy Navigation Example

```javascript
// Legacy approach - still works but is deprecated
const navData = await client.nav.get({
    path: '/',
    depth: 2,
    languageId: 1
});
```

### Fetching a Collection of Content

The `getCollection` method allows you to fetch a collection of content items (sometimes called "contentlets") using a builder pattern for complex queries.

#### Basic Usage

Here's a simple example to fetch content from a collection:

```typescript
import { dotCMSCreateClient } from '@dotcms/client';

const client = dotCMSCreateClient({
    dotcmsUrl: 'https://your-dotcms-instance.com',
    authToken: 'your-auth-token'
});

const collectionResponse = await client.content
    .getCollection('Blog') // Collection name
    .limit(10) // Limit results to 10 items
    .page(1) // Fetch the first page
    .fetch(); // Execute the query

console.log(collectionResponse.contentlets);
```

#### Sorting Content

You can sort the content by any field in ascending or descending order:

```typescript
const sortedResponse = await client.content
    .getCollection('Blog')
    .sortBy([{ field: 'title', order: 'asc' }]) // Sort by title in ascending order
    .fetch();
```

#### Filtering by Language

If you need to filter content by language, you can specify the `language` parameter:

```typescript
const languageFilteredResponse = await client.content
    .getCollection('Blog')
    .language(2) // Filter by language ID (e.g., 2)
    .fetch();
```

#### Using Complex Queries

You can build more complex queries using the query builder. For example, filter by `author` and `title`:

```typescript
const complexQueryResponse = await client.content
    .getCollection('Blog')
    .query((qb) => qb.field('author').equals('John Doe').and().field('title').equals('Hello World'))
    .fetch();
```

#### Fetching Draft Content

To only fetch draft content, use the `draft()` method:

```typescript
const draftContentResponse = await client.content
    .getCollection('Blog')
    .draft() // Fetch only drafts content
    .fetch();
```

#### Setting Depth for Relationships

To fetch content with a specific relationship depth, use the `depth()` method:

```typescript
const depthResponse = await client.content
    .getCollection('Blog')
    .depth(2) // Fetch related content up to depth 2
    .fetch();
```

#### Combining Multiple Methods

You can combine multiple methods to build more complex queries. For example, limit results, sort them, and filter by author:

```typescript
const combinedResponse = await client.content
    .getCollection('Blog')
    .limit(5)
    .page(2)
    .sortBy([{ field: 'title', order: 'asc' }])
    .query((qb) => qb.field('author').equals('John Doe'))
    .depth(1)
    .fetch();
```

## Error Handling Example

To handle errors gracefully, you can use a `try-catch` block around your API calls. Here's an example:

```typescript
try {
    const pageData = await client.page.get({
        path: '/your-page-path',
        languageId: 1
    });
} catch (error) {
    console.error('Failed to fetch page data:', error);
}
```

This ensures that any errors that occur during the fetch (e.g., network issues, invalid paths, etc.) are caught and logged properly.

## Pagination

When fetching large collections of content, pagination is key to managing the number of results returned:

```typescript
const paginatedResponse = await client.content
    .getCollection('Blog')
    .limit(10) // Limit to 10 items per page
    .page(2) // Get the second page of results
    .fetch();
```

## API Reference

Detailed documentation of the `@dotcms/client` methods, parameters, and types can be found below:

| Method | Description | Parameters | Returns |
|--------|-------------|------------|---------|
| `dotCMSCreateClient(config)` | Initializes the dotCMS client | `config: DotCMSClientConfig` | `DotCMSClient` |
| `client.page.get(path, options)` | Retrieves page elements | `path: string, options: PageApiOptions` | `Promise<{page: any, content: any}>` |
| `client.nav.get(options)` | Retrieves navigation structure | `options: NavApiOptions` | `Promise<unknown>` |
| `client.content.getCollection(contentType)` | Builds a query for content | `contentType: string` | `CollectionBuilder<T>` |

## Contributing

GitHub pull requests are the preferred method to contribute code to dotCMS. Before any pull requests can be accepted, an automated tool will ask you to agree to the [dotCMS Contributor's Agreement](https://gist.github.com/wezell/85ef45298c48494b90d92755b583acb3).

## Licensing

dotCMS comes in multiple editions and as such is dual licensed. The dotCMS Community Edition is licensed under the GPL 3.0 and is freely available for download, customization and deployment for use within organizations of all stripes. dotCMS Enterprise Editions (EE) adds a number of enterprise features and is available via a supported, indemnified commercial license from dotCMS. For the differences between the editions, see [the feature page](http://dotcms.com/cms-platform/features).

## Support

If you need help or have any questions, please [open an issue](https://github.com/dotCMS/core/issues/new/choose) in the GitHub repository.

## Documentation

Always refer to the official [dotCMS documentation](https://www.dotcms.com/docs/latest/) for comprehensive guides and API references.

## Getting Help

| Source          | Location                                                            |
| --------------- | ------------------------------------------------------------------- |
| Installation    | [Installation](https://dotcms.com/docs/latest/installation)         |
| Documentation   | [Documentation](https://dotcms.com/docs/latest/table-of-contents)   |
| Videos          | [Helpful Videos](http://dotcms.com/videos/)                         |
| Forums/Listserv | [via Google Groups](https://groups.google.com/forum/#!forum/dotCMS) |
| Twitter         | @dotCMS                                                             |
| Main Site       | [dotCMS.com](https://dotcms.com/)                                   |
