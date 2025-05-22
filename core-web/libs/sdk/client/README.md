# dotCMS Client SDK

The `@dotcms/client` is a JavaScript/TypeScript library for interacting with a dotCMS instance. It provides a streamlined, promise-based interface to fetch pages, content, and navigation information in JSON format.

## Table of Contents

* [Quickstart](#quickstart)
* [Example Projects](#example-projects)
* [What Is It?](#what-is-it)
* [Installation](#installation)
* [Key Concepts](#key-concepts)
* [API Reference](#api-reference)
  * [Client Initialization](#client-initialization)
  * [Page API](#page-api)
  * [Content API](#content-api)
  * [Navigation API](#navigation-api)
* [Common Use Cases](#common-use-cases)
* [FAQ](#faq)
* [dotCMS Support](#dotcms-support)
* [How To Contribute](#how-to-contribute)
* [Licensing Information](#licensing-information)

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
    siteId: 'your-site-id'        // Optional site identifier
});

// Start using the client!
const { pageAsset } = await client.page.get('/about-us');
console.log(pageAsset.page.title);
```

## Example Projects

While there isn't a dedicated example project specifically for the client SDK, you can see it in action within these full-stack examples:

* [Next.js Example](https://github.com/dotCMS/core/tree/main/examples/nextjs) - Modern React-based SSR implementation
* [Angular Example](https://github.com/dotCMS/core/tree/main/examples/angular) - Enterprise Angular implementation
* [Astro Example](https://github.com/dotCMS/core/tree/main/examples/astro) - Astro implementation

These examples demonstrate how to use the client SDK as part of a complete web application.

## What Is It?

The `@dotcms/client` library serves as a specialized connector between your applications and dotCMS. This SDK:

- **Simplifies API interactions** with dotCMS by providing intuitive methods and builders
- **Handles authentication and requests** securely across all API endpoints
- **Works universally** in both browser-based applications and Node.js environments
- **Abstracts complexity** of the underlying REST APIs into developer-friendly interfaces
- **Provides type definitions** for enhanced developer experience and code completion

Whether you're building a headless frontend that consumes dotCMS content or a server-side application that needs to interact with your content repository, this client SDK eliminates boilerplate code and standardizes how you work with the dotCMS APIs.

## Installation

```bash
# Using npm
npm install @dotcms/client@next

# Using yarn
yarn add @dotcms/client@next

# Using pnpm
pnpm add @dotcms/client@next
```

### Dev Dependencies

This package has the following dev dependencies:

| Dependency | Version | Description |
|------------|---------|-------------|
| `@dotcms/types` | latest | Required for type definitions |

Install dev dependencies:

```bash
npm install @dotcms/types@next --save-dev
```

### Browser Compatibility

The `@dotcms/client` package is compatible with:

| Browser | Minimum Version |
|---------|----------------|
| Chrome  | Latest 2 versions |
| Edge    | Latest 2 versions |
| Firefox | Latest 2 versions |

## Key Concepts

| Term | Description |
|------|-------------|
| `pageAsset` | The page data structure containing layout and content |
| `contentlet` | A single piece of content in dotCMS |
| `collection` | A group of contentlets of the same type |
| `graphql` | Query language used to extend API responses |

## API Reference

### Client Initialization

**Overview:**
The Client Initialization is the first step in using the dotCMS Client SDK. It allows you to create a new client instance with your dotCMS configuration.

**Configuration Options:**

| Option | Type | Required | Description |
|--------|------|----------|-------------|
| `dotcmsUrl` | string | ✅ | Your dotCMS instance URL |
| `authToken` | string | ✅ | Authentication token |
| `siteId` | string | ❌ | Site identifier |
| `requestOptions` | object | ❌ | Additional fetch options |

**Usage:**

```typescript
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

| Option | Type | Required | Default | Description |
|--------|------|----------|---------|-------------|
| `siteId` | string | ❌ | From client config | ID of the site to interact with |
| `mode` | string | ❌ | Site default | Page rendering mode: 'EDIT_MODE', 'PREVIEW_MODE', or 'LIVE' |
| `languageId` | string\|number | ❌ | Site default | Language ID for content localization |
| `personaId` | string | ❌ | — | ID of the persona for personalized content |
| `fireRules` | boolean\|string | ❌ | false | Whether to execute rules set on the page |
| `depth` | 0\|1\|2\|3 | ❌ | 0 | Depth of related content to retrieve via Relationship fields |
| `publishDate` | string | ❌ | — | Publication date for the requested page |
| `variantName` | string | ❌ | — | Name of the specific page variant to retrieve |
| `graphql` | object | ❌ | — | GraphQL options for extending the response |

**GraphQL Options:**

| Option | Type | Description |
|--------|------|-------------|
| `page` | string | GraphQL query to extend the page response |
| `content` | Record<string, string> | Named GraphQL queries to fetch additional content |
| `variables` | Record<string, string> | Variables to use in GraphQL queries |
| `fragments` | string[] | GraphQL fragments for reuse across queries |

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
The [Content API](https://dev.dotcms.com/docs/search) allows you to fetch content collections from dotCMS. It provides a builder pattern for constructing complex queries and filtering content based on various criteria.

The `getCollection` method uses immutable patterns, so each method call returns a new instance. This means you can reuse partial queries by storing them before adding additional filters.

**Builder Methods:**

| Method | Description |
|--------|-------------|
| `query()` | Filter content |
| `limit()` | Set page size |
| `page()` | Set page number |
| `sortBy()` | Order results |
| `language()` | Set content language |

**Usage:**

```typescript
const blogs = await client.content
    .getCollection('Blog')
    .query(qb => 
        qb.field('title').contains('dotCMS')
          .and()
          .field('publishDate').greaterThan('2023-01-01')
    )
    .limit(10)
    .page(1)
    .fetch();
```

**With TypeScript:**

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

| Option | Type | Description |
|--------|------|-------------|
| `depth` | number | Levels of children to include |
| `languageId` | number | Navigation language |

## Common Use Cases

### Content Listing with Search

```typescript
// Initial content load
const { content } = await client.page.get('/blog', {
    graphql: {
        content: {
            featuredPosts: `
                BlogCollection(limit: 5) {
                    title
                    urlTitle
                }
            `
        }
    }
});

// Handle search
const searchResults = await client.content
    .getCollection('Blog')
    .query(qb => qb.field('title').contains(searchTerm))
    .limit(10)
    .fetch();
```

### Multilingual Content

```typescript
// Get page in different languages
const spanishPage = await client.page.get('/about-us', {
    languageId: '2'  // Spanish
});

const englishPage = await client.page.get('/about-us', {
    languageId: '1'  // English
});
```

## FAQ

### How do I handle errors from the API?

Use try/catch blocks and check response status:

```typescript
const getPage = async () => {
    try {
        return await client.page.get('/invalid-page');
    } catch (error) {
        console.error('Failed to fetch page: ', error.message);
    }
}
```

### How can I optimize performance?

1. Use the cache option in requestOptions:
```typescript
const client = createDotCMSClient({
    requestOptions: { cache: 'default' }
});
```

2. Limit GraphQL queries to needed fields
3. Use pagination for large collections
4. Consider using the Content API separately for data not needed immediately

### How to correctly type a page response?

When using TypeScript, you can leverage generic types to get proper typing for your page and content data:

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
console.log(pageAsset.vanityUrl.url);        // TypeScript knows this exists
console.log(content.blogPosts[0].title);     // TypeScript knows this exists
console.log(content.teamMembers[0].position); // TypeScript knows this exists
```

### How do I build complex content queries?

The Content API provides a powerful builder pattern that allows you to create sophisticated queries with multiple conditions, sorting, and pagination:

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

This query demonstrates several features:
- Nested conditions using `parenthesis()`
- Logical operators (`and()`, `or()`)
- Field comparisons (`contains`, `equals`, `greaterThan`)
- Result ordering with `sortBy()`
- Pagination with `limit()` and `page()`
- Language filtering with `language()`
- Relationship depth with `depth()`

### How do I sort content using multiple fields?

The Content API allows you to sort results using multiple field variables from your content type. Each field in the `sortBy` array corresponds to a field variable defined in your content type:

```typescript
// Sort blog posts by publish date (newest first) and then by title
const response = await client.content
    .getCollection('Blog')
    .sortBy([
        { field: 'publishDate', order: 'desc' },  // publishDate is a field variable in Blog content type
        { field: 'title', order: 'asc' }         // title is a field variable in Blog content type
    ])
    .fetch();
```

Note that:
- The `field` parameter must match a field variable name from your content type
- Multiple sort criteria are applied in order (first by publishDate, then by title)
- Each field can be sorted in ascending (`'asc'`) or descending (`'desc'`) order
- This works with any field variable type (text, date, number, etc.)

## dotCMS Support

We offer multiple channels to get help:

* **GitHub Issues**: For bug reports and feature requests, please [open an issue](https://github.com/dotCMS/core/issues/new/choose)
* **Community Forum**: Join our [community discussions](https://community.dotcms.com/)
* **Stack Overflow**: Use the tag `dotcms-client`

Enterprise customers can access premium support through the [dotCMS Support Portal](https://dev.dotcms.com/docs/help).

## How To Contribute

We welcome contributions! To contribute:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

Please ensure your code follows the existing style and includes appropriate tests.

## Licensing Information

dotCMS comes in multiple editions and as such is dual licensed. The dotCMS Community Edition is licensed under the GPL 3.0 and is freely available for download, customization and deployment for use within organizations of all stripes. dotCMS Enterprise Editions (EE) adds a number of enterprise features and is available via a supported, indemnified commercial license from dotCMS. For the differences between the editions, see [the feature page](http://www.dotcms.com/cms-platform/features).
