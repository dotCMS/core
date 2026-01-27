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
-   **Security First**: Handles authentication and requests securely
-   **Developer Experience**: Rich autocompletion and documentation

> **📋 Migration Guides:**
> - **From Alpha Version?** If you're upgrading from the alpha version of `@dotcms/client`, please see our [Migration Guide](./MIGRATION.md) for step-by-step instructions.
> - **From v1.2.x to v1.3.0?** See the [Changelog](#v130) section below for new AI Search API features.
> - **From v1.0.x to v1.1.1?** See the [Changelog](#v111) section below for new features and improvements.

## Table of Contents

-   [Getting Started](#getting-started)
    -   [Prerequisites & Setup](#prerequisites--setup)
    -   [Installation](#installation)
    -   [Your First API Call](#your-first-api-call)
    -   [Example Projects](#example-projects)
-   [How-to Guides](#how-to-guides)
    -   [How to Fetch Complete Pages](#how-to-fetch-complete-pages)
    -   [How to Query Content Collections](#how-to-query-content-collections)
    -   [How to Run Raw Lucene Content Queries](#how-to-run-raw-lucene-content-queries)
    -   [How to Use AI-Powered Search](#how-to-use-ai-powered-search)
    -   [How to Work with GraphQL](#how-to-work-with-graphql)
    -   [How to Use with TypeScript](#how-to-use-with-typescript)
    -   [How to Enable Page Editing](#how-to-enable-page-editing)
-   [API Reference](#api-reference)
    -   [Client Initialization](#client-initialization)
    -   [HTTP Client Configuration](#http-client-configuration)
    -   [page.get() Method](#pageget-method)
    -   [content.getCollection() Method](#contentgetcollection-method)
    -   [content.query() Method](#contentquery-method)
    -   [ai.search() Method](#aisearch-method)
    -   [navigation.get() Method](#navigationget-method)
    -   [Error Handling](#error-handling)
-   [Concepts & Architecture](#concepts--architecture)
    -   [Key Concepts](#key-concepts)
    -   [Choosing the Right Method](#choosing-the-right-method)
    -   [Architecture Overview](#architecture-overview)
-   [Support](#support)
-   [Contributing](#contributing)
-   [Licensing](#licensing)
-   [Changelog](#changelog)
    -   [v1.3.0](#v130)
    -   [v1.1.1](#v111)

## Getting Started

### Prerequisites & Setup

#### Get a dotCMS Environment

-   **Recommended**: dotCMS Evergreen
-   **Minimum**: dotCMS v25.05
-   **Best Experience**: Latest Evergreen release

#### Environment Setup

**For Production Use:**

-   ☁️ [Cloud hosting options](https://www.dotcms.com/pricing) - managed solutions with SLA
-   🛠️ [Self-hosted options](https://dev.dotcms.com/docs/current-releases) - deploy on your infrastructure

**For Testing & Development:**

-   🧑🏻‍💻 [dotCMS demo site](https://demo.dotcms.com/dotAdmin/#/public/login) - perfect for trying out the SDK
-   📘 [Learn how to use the demo site](https://dev.dotcms.com/docs/demo-site)
-   📝 Read-only access, ideal for building proof-of-concepts

**For Local Development:**

-   🐳 [Docker setup guide](https://github.com/dotCMS/core/tree/main/docker/docker-compose-examples/single-node-demo-site)
-   💻 [Local installation guide](https://dev.dotcms.com/docs/quick-start-guide)

#### Create a dotCMS API Key

> [!TIP]
> Make sure your API Token has read-only permissions for Pages, Folders, Assets, and Content. Using a key with minimal permissions follows security best practices.

This integration requires an API Key with read-only permissions for security best practices:

1. Go to the **dotCMS admin panel**.
2. Click on **System** > **Users**.
3. Select the user you want to create the API Key for.
4. Go to **API Access Key** and generate a new key.

For detailed instructions, please refer to the [dotCMS API Documentation - Read-only token](https://dev.dotcms.com/docs/rest-api-authentication#ReadOnlyToken).

#### Installation

Install the SDK and required dependencies:

```bash
npm install @dotcms/client@latest @dotcms/types@latest
```

> [!TIP]
> If you are working with pure JavaScript, you can avoid installing the `@dotcms/types` package.

### Your First API Call

Here's a basic setup of the dotCMS Client SDK to help you get started:

```typescript
import { createDotCMSClient } from '@dotcms/client';

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

## How-to Guides

### How to Fetch Complete Pages

The `client.page.get()` method is your primary tool for retrieving full page content. Here's how to use it effectively:

#### Basic Page Fetching
```typescript
// Fetch a page with its layout and content
const { pageAsset } = await client.page.get('/about-us');
console.log(pageAsset.page.title);
```

#### Customizing Page Requests
```typescript
// Fetch with specific language and persona
const { pageAsset } = await client.page.get('/about-us', {
    languageId: '2',
    fireRules: true,
    personaId: '1234'
});
```

#### Fetching Related Content
```typescript
// Pull in additional content with GraphQL
const { pageAsset, content } = await client.page.get('/about-us', {
    graphql: {
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
                }
            `
        }
    }
});
```

### How to Query Content Collections

The `client.content.getCollection()` method uses a fluent builder pattern for querying content:

#### Basic Collection Queries
```typescript
// Fetch first 10 blog posts
const blogs = await client.content.getCollection('Blog').limit(10).page(1);
```

#### Filtering and Sorting
```typescript
// Filter by title and sort by date
const filtered = await client.content
    .getCollection('Blog')
    .query((qb) => qb.field('title').equals('dotCMS*'))
    .limit(5)
    .sortBy([{ field: 'publishDate', direction: 'desc' }]);
```

#### Complex Queries
```typescript
// Multiple filters with operators
const products = await client.content
    .getCollection('Product')
    .query((qb) => qb
        .field('category').equals('electronics')
        .and()
        .field('price').raw(':[100 TO 500]')
        .not()
        .field('discontinued').equals('true')
    )
    .limit(10);
```

### How to Run Raw Lucene Content Queries

Use `client.content.query()` when you want to execute a **raw Lucene query string** (without the query-builder DSL), and you want full control over constraints like `contentType`, `live`, `languageId`, `conhost`, etc.

> [!NOTE]
> `content.query()` does **not** prefix fields with `contentType.` (unlike `getCollection()`), and it does **not** inject system constraints into your query string.

#### Basic Raw Query

```typescript
const response = await client.content
    .query('+contentType:Blog +title:"Hello World"')
    .limit(10)
    .page(1);
```

#### Setting Language (request body)

```typescript
const response = await client.content
    .query('+contentType:Blog +title:"Hello World"')
    .language(1) // sets languageId in the request body (does not alter the Lucene string)
    .limit(10);
```

### How to Use AI-Powered Search

> [!WARNING]
> **Experimental Feature:** The AI API is currently experimental and may undergo breaking changes in future releases. Use with caution in production environments.

The `client.ai.search()` method enables semantic search using AI embeddings to find content based on meaning rather than exact keyword matches.

#### Prerequisites

Before using AI-powered search, ensure your dotCMS instance is properly configured:

> [!IMPORTANT]
> **Required Setup:**
> 1. **dotAI must be activated** in your dotCMS instance
> 2. **OpenAI API Key** must be configured in dotCMS
> 3. **PostgreSQL 15+ with pgvector extension** must be installed
> 4. **Content indexes** must be created and configured in dotAI
>
> For detailed setup instructions, see the [dotCMS dotAI documentation](https://dev.dotcms.com/docs/dotai).

#### Basic AI Search
```typescript
// Search for content semantically related to your query
const response = await client.ai.search(
    'articles about machine learning',
    'content_index'
);
console.log(response.results);
```

#### Customizing Search Parameters
```typescript
// Fine-tune search with query parameters
const response = await client.ai.search(
    'artificial intelligence tutorials',
    'content_index',
    {
        query: {
            limit: 20,
            offset: 0,
            contentType: 'BlogPost',
            languageId: '1'
        }
    }
);
```

#### Adjusting AI Configuration
```typescript
import { DISTANCE_FUNCTIONS } from '@dotcms/types';

// Customize AI search behavior with threshold and distance function
const response = await client.ai.search(
    'deep learning concepts',
    'content_index',
    {
        config: {
            threshold: 0.75,              // Higher threshold = more relevant results
            distanceFunction: DISTANCE_FUNCTIONS.cosine, // Distance calculation method
            responseLength: 2048           // Maximum response length
        }
    }
);
```

#### Complete Example with All Options
```typescript
// Combine query and AI parameters for precise control
const response = await client.ai.search(
    'best practices for content management',
    'articles_index',
    {
        query: {
            limit: 10,
            offset: 0,
            contentType: 'Article',
            languageId: '1',
            siteId: 'my-site'
        },
        config: {
            threshold: 0.8,
            distanceFunction: DISTANCE_FUNCTIONS.innerProduct,
            responseLength: 1024
        }
    }
);

// Access results with match scores
resp[onse].results.forEach(result => {
    console.log(result.title);
    console.log('Matches:', result.matches); // Distance and extracted text
});
```

#### Understanding Distance Functions

The SDK supports multiple distance functions for vector similarity:

```typescript
import { DISTANCE_FUNCTIONS } from '@dotcms/types';

// Available distance functions:
DISTANCE_FUNCTIONS.cosine        // '<=>' - Cosine similarity (default)
DISTANCE_FUNCTIONS.innerProduct  // '<#>' - Inner product similarity
DISTANCE_FUNCTIONS.L2            // '<->' - Euclidean distance
DISTANCE_FUNCTIONS.L1            // '<+>' - Manhattan distance
DISTANCE_FUNCTIONS.hamming       // '<~>' - Hamming distance
DISTANCE_FUNCTIONS.jaccard       // '<%>' - Jaccard similarity
```

### How to Use with TypeScript

The SDK is built with TypeScript and provides comprehensive type definitions through the `@dotcms/types` package for enhanced developer experience and type safety.

#### Basic Type Usage

```typescript
import { createDotCMSClient } from '@dotcms/client';
import type {
    DotCMSPageAsset,
    DotCMSPageResponse,
    DotCMSBasicContentlet,
    DotCMSNavigationItem,
    DotCMSAISearchResponse,
    DotErrorPage,
    DotErrorContent,
    DotErrorAISearch
} from '@dotcms/types';

const client = createDotCMSClient({
    dotcmsUrl: 'https://your-dotcms-instance.com',
    authToken: 'your-auth-token',
    siteId: 'your-site-id'
});
```

#### Typing Page Responses

```typescript
import type { DotCMSPageAsset } from '@dotcms/types';

// Basic page fetch with type inference
const { pageAsset } = await client.page.get('/about-us');
// pageAsset is automatically typed as DotCMSPageAsset

// Explicit typing
const response: { pageAsset: DotCMSPageAsset } = await client.page.get('/about-us');

// Access typed properties
console.log(pageAsset.page.title);        // string
console.log(pageAsset.layout.body.rows);  // Row[]
console.log(pageAsset.viewAs.language);   // LanguageView
```

#### Typing Content Collections

```typescript
import type { DotCMSBasicContentlet } from '@dotcms/types';

// Define your custom content type interface
interface BlogPost extends DotCMSBasicContentlet {
    title: string;
    body: string;
    author: string;
    publishDate: string;
    tags: string[];
}

// Use the generic type parameter for type-safe content
const response = await client.content
    .getCollection<BlogPost>('Blog')
    .limit(10);

// Now contentlets are typed as BlogPost[]
response.contentlets.forEach(post => {
    console.log(post.title);      // ✅ Type-safe: string
    console.log(post.author);     // ✅ Type-safe: string
    console.log(post.publishDate); // ✅ Type-safe: string
});
```

#### Typing AI Search Results

```typescript
import type {
    DotCMSAISearchResponse,
    DotCMSBasicContentlet,
    DISTANCE_FUNCTIONS
} from '@dotcms/types';

// Define your content type
interface Article extends DotCMSBasicContentlet {
    title: string;
    content: string;
    category: string;
}

// Type-safe AI search
const response: DotCMSAISearchResponse<Article> = await client.ai.search<Article>(
    'machine learning tutorials',
    'content_index',
    {
        query: {
            limit: 20,
            contentType: 'Article'
        },
        config: {
            threshold: 0.75,
            distanceFunction: DISTANCE_FUNCTIONS.cosine
        }
    }
);

// Access typed results with match information
response.results.forEach(article => {
    console.log(article.title);           // ✅ Type-safe: string
    console.log(article.category);        // ✅ Type-safe: string

    // Access AI match data
    article.matches?.forEach(match => {
        console.log(match.distance);      // ✅ Type-safe: number
        console.log(match.extractedText); // ✅ Type-safe: string
    });
});
```

#### Typing Navigation

```typescript
import type { DotCMSNavigationItem } from '@dotcms/types';

// Navigation is automatically typed
const nav: DotCMSNavigationItem[] = await client.navigation.get('/', {
    depth: 2,
    languageId: 1
});

// Access typed navigation properties
nav.forEach(item => {
    console.log(item.title);    // ✅ Type-safe: string
    console.log(item.href);     // ✅ Type-safe: string
    console.log(item.children); // ✅ Type-safe: DotCMSNavigationItem[] | undefined
});
```

#### Type-Safe Error Handling

```typescript
import type {
    DotHttpError,
    DotErrorPage,
    DotErrorContent,
    DotErrorAISearch
} from '@dotcms/types';

try {
    const { pageAsset } = await client.page.get('/about-us');
} catch (error) {
    // Type guard for specific error types
    if (error instanceof DotErrorPage) {
        // TypeScript knows this is DotErrorPage
        console.error('Page error:', error.message);
        console.error('Context:', error.context);
        if (error.httpError) {
            console.error('Status:', error.httpError.status); // ✅ Type-safe
        }
    } else if (error instanceof DotErrorContent) {
        // TypeScript knows this is DotErrorContent
        console.error('Content error:', error.contentType);
        console.error('Operation:', error.operation);
    } else if (error instanceof DotErrorAISearch) {
        // TypeScript knows this is DotErrorAISearch
        console.error('AI Search error:', error.prompt);
        console.error('Parameters:', error.params);
    }
}
```

#### Custom Content Types with Relationships

```typescript
import type { DotCMSBasicContentlet } from '@dotcms/types';

// Define related content types
interface Author extends DotCMSBasicContentlet {
    name: string;
    email: string;
    bio: string;
}

interface Category extends DotCMSBasicContentlet {
    categoryName: string;
    description: string;
}

// Main content type with relationships
interface BlogPost extends DotCMSBasicContentlet {
    title: string;
    body: string;
    author: Author;        // Nested type
    category: Category;    // Nested type
    tags: string[];
    publishDate: string;
}

// Fetch with depth to include relationships
const response = await client.content
    .getCollection<BlogPost>('Blog')
    .depth(2)
    .limit(5);

// Access nested typed properties
response.contentlets.forEach(post => {
    console.log(post.title);              // ✅ string
    console.log(post.author.name);        // ✅ string
    console.log(post.author.email);       // ✅ string
    console.log(post.category.categoryName); // ✅ string
});
```

#### Using with Async/Await and Promises

```typescript
import type {
    DotCMSPageAsset,
    GetCollectionResponse
} from '@dotcms/types';

// Async function with typed return
async function fetchBlogPosts(): Promise<GetCollectionResponse<BlogPost>> {
    return await client.content
        .getCollection<BlogPost>('Blog')
        .limit(10);
}

// Promise chain with types
client.page.get('/about-us')
    .then((response: { pageAsset: DotCMSPageAsset }) => {
        console.log(response.pageAsset.page.title);
        return response;
    })
    .catch((error: DotErrorPage) => {
        console.error(error.message);
    });
```

### How to Work with GraphQL

GraphQL allows you to fetch exactly the data you need in a single request:

#### Fetching All Page Fields
```typescript
// Use _map to get all page fields including custom ones
const { pageAsset } = await client.page.get('/about-us', {
    graphql: {
        page: '_map'
    }
});
```

#### Querying Relationships
```typescript
// Fetch related content using fragments
const { pageAsset } = await client.page.get('/blog-post', {
    graphql: {
        page: `
            containers {
                containerContentlets {
                    contentlets {
                        ... on Blog {
                            author {
                                title
                                email
                            }
                            category {
                                categoryName
                            }
                            tags {
                                tagName
                            }
                        }
                    }
                }
            }
        `
    }
});
```

#### Using Variables and Fragments
```typescript
// Reusable fragments with variables
const response = await client.page.get('/about-us', {
    graphql: {
        content: {
            blogPosts: `
                BlogCollection(limit: $limit) {
                    ...blogFragment
                }
            `
        },
        fragments: [
            `fragment blogFragment on Blog {
                title
                urlTitle
                blogContent {
                    json
                }
            }`
        ],
        variables: { limit: 5 }
    }
});
```


## API Reference

### Client Initialization

```typescript
createDotCMSClient(config: DotCMSClientConfig): DotCMSClient
```

#### Parameters

| Option           | Type              | Required | Description                                                   |
| ---------------- | ----------------- | -------- | ------------------------------------------------------------- |
| `dotcmsUrl`      | string            | ✅       | Your dotCMS instance URL                                      |
| `authToken`      | string            | ✅       | Authentication token                                          |
| `siteId`         | string            | ❌       | Site identifier (falls back to default site if not specified) |
| `requestOptions` | DotRequestOptions | ❌       | Additional request options                                    |
| `httpClient`     | DotHttpClient     | ❌       | Custom HTTP client implementation                             |

#### Example
```typescript
const client = createDotCMSClient({
    dotcmsUrl: 'https://your-dotcms-instance.com',
    authToken: 'your-auth-token',
    siteId: 'your-site-id',
    httpClient: customHttpClient // Optional: provide custom HTTP client
});
```

### HTTP Client Configuration

The SDK now supports custom HTTP client implementations for advanced use cases. By default, it uses the built-in `FetchHttpClient` based on the native Fetch API.

#### Default HTTP Client
```typescript
// The SDK automatically uses FetchHttpClient if no custom client is provided
const client = createDotCMSClient({
    dotcmsUrl: 'https://your-dotcms-instance.com',
    authToken: 'your-auth-token'
    // No httpClient specified - uses FetchHttpClient internally
});
```

#### Custom HTTP Client
```typescript
import { BaseHttpClient } from '@dotcms/types';

// Implement your own HTTP client by extending BaseHttpClient
class CustomHttpClient extends BaseHttpClient {
    async request<T>(url: string, options?: DotRequestOptions): Promise<T> {
        // Your custom implementation
        // Must handle JSON parsing, error conversion, etc.
        return customRequest(url, options);
    }
}

const client = createDotCMSClient({
    dotcmsUrl: 'https://your-dotcms-instance.com',
    authToken: 'your-auth-token',
    httpClient: new CustomHttpClient()
});
```

#### When You Might Need a Custom HTTP Client

- **Corporate Proxies**: "All our API calls must go through our corporate proxy with authentication"
- **Request Monitoring**: "We need to log every dotCMS API call for our compliance audit trail"
- **Custom Authentication**: "Our enterprise SSO requires adding custom headers to every request"
- **Performance Optimization**: "We want to reuse HTTP connections and implement our own retry logic"

#### HTTP Client Features
- **Automatic Response Parsing**: JSON responses are automatically parsed
- **Error Standardization**: All HTTP failures are converted to `DotHttpError` instances
- **Type Safety**: Full TypeScript support with generic response types
- **Universal Compatibility**: Works in both browser and Node.js environments

### page.get() Method

```typescript
get<T extends DotCMSExtendedPageResponse = DotCMSPageResponse>(
  url: string,
  options?: DotCMSPageRequestParams
): Promise<DotCMSComposedPageResponse<T>>
```

#### Parameters

| Parameter | Type                     | Required | Description                     |
| --------- | ------------------------ | -------- | ------------------------------- |
| `url`     | string                   | ✅       | Page URL path                   |
| `options` | DotCMSPageRequestParams  | ❌       | Request customization options   |

#### Options

| Option       | Type                | Description                              |
| ------------ | ------------------- | ---------------------------------------- |
| `languageId` | string \| number    | Language version of the page             |
| `mode`       | string              | Rendering mode: `LIVE`, `PREVIEW_MODE`   |
| `personaId`  | string              | Personalize content based on persona ID  |
| `graphql`    | DotCMSGraphQLParams | GraphQL options for extending response   |
| `fireRules`  | boolean             | Whether to trigger page rules            |

#### Example
```typescript
const { pageAsset } = await client.page.get('/about-us');
```

### content.getCollection() Method

```typescript
getCollection<T = DotCMSBasicContentlet>(
  contentType: string
): CollectionBuilder<T>
```

#### Parameters

| Parameter     | Type   | Required | Description                    |
| ------------- | ------ | -------- | ------------------------------ |
| `contentType` | string | ✅       | Content type variable name     |

#### Builder Methods

| Method       | Arguments                     | Description                              |
| ------------ | ----------------------------- | ---------------------------------------- |
| `query()`    | `string` \| `BuildQuery`      | Filter content using query builder       |
| `limit()`    | `number`                      | Set number of items to return            |
| `page()`     | `number`                      | Set which page of results to fetch       |
| `sortBy()`   | `SortBy[]`                    | Sort by one or more fields               |
| `language()` | `number \| string`            | Set content language                     |
| `depth()`    | `number`                      | Set depth of related content             |

#### Example
```typescript
const blogs = await client.content.getCollection('Blog').limit(10).page(1);
```

### content.query() Method

```typescript
query<T = DotCMSBasicContentlet>(
  rawQuery: string
): RawQueryBuilder<T>
```

#### Parameters

| Parameter  | Type     | Required | Description            |
| ---------- | -------- | -------- | ---------------------- |
| `rawQuery` | `string` | ✅       | Raw Lucene query string |

#### Builder Methods

| Method       | Arguments          | Description                                                                 |
| ------------ | ------------------ | --------------------------------------------------------------------------- |
| `limit()`    | `number`           | Set number of items to return                                               |
| `page()`     | `number`           | Set which page of results to fetch                                          |
| `sortBy()`   | `SortBy[]`         | Sort by one or more fields                                                  |
| `render()`   | -                  | Enable server-side rendering (velocity) for widgets in returned content     |
| `depth()`    | `number`           | Set depth of related content                                                |
| `language()` | `number \| string` | Set `languageId` in the request body (does not modify the raw Lucene string) |

#### Example

```typescript
const response = await client.content
    .query('+contentType:Blog +title:"Hello World"')
    .language(1)
    .limit(10);
```

### ai.search() Method

> [!WARNING]
> **Experimental Feature:** The AI API is currently experimental and may undergo breaking changes in future releases. Use with caution in production environments.

Performs semantic search using AI embeddings to find content based on meaning rather than exact keyword matches.

> [!NOTE]
> **Prerequisites:** This feature requires dotAI to be activated in your dotCMS instance with a configured OpenAI API key. See the [dotAI setup documentation](https://dev.dotcms.com/docs/dotai) for configuration details.

```typescript
search<T extends DotCMSBasicContentlet>(
  prompt: string,
  indexName: string,
  params?: DotCMSAISearchParams
): Promise<DotCMSAISearchResponse<T>>
```

#### Parameters

| Parameter   | Type                  | Required | Description                           |
| ----------- | --------------------- | -------- | ------------------------------------- |
| `prompt`    | string                | ✅       | Natural language search query         |
| `indexName` | string                | ✅       | Name of the AI search index to query  |
| `params`    | DotCMSAISearchParams  | ❌       | Search configuration options          |

#### Search Parameters (params.query)

| Option        | Type             | Description                              |
| ------------- | ---------------- | ---------------------------------------- |
| `limit`       | number           | Maximum number of results (default: 1000) |
| `offset`      | number           | Number of results to skip (default: 0)   |
| `contentType` | string           | Filter by specific content type          |
| `languageId`  | string           | Filter by language ID                    |
| `siteId`      | string           | Filter by site ID                        |

#### AI Configuration (params.config)

| Option             | Type   | Description                                     |
| ------------------ | ------ | ----------------------------------------------- |
| `threshold`        | number | Minimum similarity score (0-1, default: 0.5)    |
| `distanceFunction` | string | Distance calculation method (default: cosine)   |
| `responseLength`   | number | Maximum response length (default: 1024)         |

#### Response

```typescript
interface DotCMSAISearchResponse<T> {
    results: Array<T & {
        matches?: Array<{
            distance: number;      // Similarity score
            extractedText: string; // Matched text excerpt
        }>;
    }>;
}
```

#### Examples

**Basic Search:**
```typescript
const results = await client.ai.search(
    'machine learning articles',
    'content_index'
);
```

**With Parameters:**
```typescript
import { DISTANCE_FUNCTIONS } from '@dotcms/types';

const results = await client.ai.search(
    'AI tutorials',
    'content_index',
    {
        query: {
            limit: 20,
            contentType: 'BlogPost',
            languageId: '1'
        },
        config: {
            threshold: 0.75,
            distanceFunction: DISTANCE_FUNCTIONS.cosine
        }
    }
);
```

**Promise-Style:**
```typescript
client.ai.search(
    'content management best practices',
    'content_index',
    {
        query: { limit: 10 },
        config: { threshold: 0.8 }
    }
).then((response) => {
    console.log('Found:', response.results.length);
    return response;
}).catch((error) => {
    console.error('Search failed:', error.message);
});
```

### navigation.get() Method

```typescript
get(
  uri: string,
  options?: {
    depth?: number;
    languageId?: number;
  }
): Promise<DotCMSNavigationItem[]>
```

#### Parameters

| Parameter | Type   | Required | Description                     |
| --------- | ------ | -------- | ------------------------------- |
| `uri`     | string | ✅       | Navigation root URI             |
| `options` | object | ❌       | Navigation options              |

#### Options

| Option       | Type   | Description                                |
| ------------ | ------ | ------------------------------------------ |
| `depth`      | number | Number of child levels to include          |
| `languageId` | number | Language ID for localized navigation names |

#### Example
```typescript
const nav = await client.navigation.get('/', { depth: 2 });
```

### Error Handling

The SDK provides comprehensive error handling with specific error types for different API operations. These domain-specific errors may wrap `DotHttpError` instances when HTTP failures occur and include contextual information to help with debugging.

#### Error Types

| Error Type           | When It's Thrown                           | Properties                                          |
| -------------------- | ------------------------------------------ | --------------------------------------------------- |
| `DotHttpError`       | HTTP/network failures (4xx/5xx, timeouts) | `status`, `statusText`, `headers`, `body`           |
| `DotErrorPage`       | Page API failures                          | `httpError?`, `context` (query, variables)         |
| `DotErrorContent`    | Content API failures                       | `contentType`, `operation`, `httpError?`, `query?` |
| `DotErrorAISearch`   | AI Search API failures                     | `prompt`, `params`, `httpError?`                   |
| `DotErrorNavigation` | Navigation API failures                    | `path`, `httpError?`                               |

#### Basic Error Handling
```typescript
try {
    const { pageAsset } = await client.page.get('/about-us');
    console.log(pageAsset.page.title);
} catch (error) {
    if (error instanceof DotErrorPage) {
        console.error('Page error:', error.message);
        console.error('Context:', error.context);
    } else {
        console.error('Unexpected error:', error);
    }
}
```

#### Content Collection Error Handling
```typescript
try {
    const blogs = await client.content
        .getCollection('Blog')
        .limit(10);
} catch (error) {
    if (error instanceof DotErrorContent) {
        console.error('Content error:', error.message);
        console.error('Content type:', error.contentType);
        console.error('Operation:', error.operation);
        if (error.httpError) {
            console.error('HTTP status:', error.httpError.status);
        }
    }
}
```

#### Navigation Error Handling
```typescript
try {
    const nav = await client.navigation.get('/missing-path');
} catch (error) {
    if (error instanceof DotErrorNavigation) {
        console.error('Navigation error:', error.message);
        console.error('Path:', error.path);
        if (error.httpError) {
            console.error('HTTP status:', error.httpError.status);
        }
    }
}
```

#### AI Search Error Handling
```typescript
try {
    const results = await client.ai.search('machine learning', 'content_index');
} catch (error) {
    if (error instanceof DotErrorAISearch) {
        console.error('AI Search error:', error.message);
        console.error('Prompt:', error.prompt);
        console.error('Index Name:', error.indexName);
        console.error('Parameters:', error.params);
        if (error.httpError) {
            console.error('HTTP status:', error.httpError.status);
        }
    }
}
```

#### Promise-Style Error Handling
```typescript
// Content collections support .then() with error handling
const result = await client.content
    .getCollection('Blog')
    .limit(10)
    .then(
        (response) => {
            console.log('Success:', response.contentlets);
            return response;
        },
        (error) => {
            console.error('Error:', error.message);
            // Return fallback data or re-throw
            return { contentlets: [], total: 0 };
        }
    );
```

#### Common Error Scenarios

**401 Unauthorized**
```typescript
// Missing or invalid authentication token
DotHttpError: status 401, message: "Authentication required"
```

**403 Forbidden**
```typescript
// Valid token but insufficient permissions
DotHttpError: status 403, message: "Access denied"
```

**404 Not Found**
```typescript
// Page, content, or navigation path not found
DotErrorPage: "Page /missing-page not found. Check the page URL and permissions."
```

**Network Errors**
```typescript
// Connection issues, timeouts, etc.
DotHttpError: "Network request failed"
```

## Concepts & Architecture

### Key Concepts

| Term         | Description                                           | Documentation                                                                  |
| ------------ | ----------------------------------------------------- | ------------------------------------------------------------------------------ |
| `pageAsset`  | The page data structure containing layout and content | [Page API](https://dev.dotcms.com/docs/page-rest-api-layout-as-a-service-laas) |
| `contentlet` | A single piece of content in dotCMS                   | [Content API](https://dev.dotcms.com/docs/content)                             |
| `collection` | A group of contentlets of the same type               | [Content API](https://dev.dotcms.com/docs/search)                              |
| `graphql`    | Query language used to extend API responses           | [GraphQL](https://dev.dotcms.com/docs/graphql)                                 |

### Choosing the Right Method

The dotCMS Client SDK provides four core methods for fetching data. Use this quick guide to decide which one is best for your use case:

| Method                           | Use When You Need...                                          | Best For                                                                                                                                                                                         |
| -------------------------------- | ------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `client.page.get()`              | A full page with layout, containers, and related content      | **Rendering entire pages** with a single request. Ideal for headless setups, SSR/SSG frameworks, and cases where you want everything—page structure, content, and navigation—tied to a URL path. |
| `client.content.getCollection()` | A filtered list of content items from a specific content type | Populating dynamic blocks, lists, search results, widgets, or reusable components.                                                                                                               |
| `client.ai.search()`             | Semantic/AI-powered content discovery based on natural language | **Intelligent search experiences** where users describe what they're looking for in natural language. Great for search features, content recommendations, and finding relevant content by meaning rather than exact keywords. ⚠️ **Experimental API** |
| `client.navigation.get()`        | Only the site's navigation structure (folders and links)      | Standalone menus or use cases where navigation is needed outside of page context.                                                                                                                |

#### Start with `page.get()`: The One-Request Solution

For most use cases, `client.page.get()` is all you need. It lets you retrieve:

-   The full page layout
-   Related content
-   Navigation structure

All in a single request using GraphQL.

Only use `content.getCollection()` or `navigation.get()` if you have advanced needs, like real-time data fetching or building custom dynamic components.

> 🔍 **For comprehensive examples of advanced GraphQL querying including relationships and custom fields,** see the [How to Work with GraphQL](#how-to-work-with-graphql) section.

### Architecture Overview

The SDK follows a client-builder pattern with four main APIs:

- **Page API** (`client.page.get()`) - Fetches complete page content with layout and containers
- **Content API** (`client.content.getCollection()`) - Builder pattern for querying content collections
- **AI API** (`client.ai.search()`) - AI-powered semantic search using embeddings and vector similarity ⚠️ **Experimental**
- **Navigation API** (`client.navigation.get()`) - Fetches site navigation structure

All APIs support:
- Type-safe responses with TypeScript
- GraphQL query extensions (Page API)
- Localization and personalization
- Browser and Node.js compatibility

## Support

We offer multiple channels to get help with the dotCMS Client SDK:

-   **GitHub Issues**: For bug reports and feature requests, please [open an issue](https://github.com/dotCMS/core/issues/new/choose) in the GitHub repository.
-   **Community Forum**: Join our [community discussions](https://community.dotcms.com/) to ask questions and share solutions.
-   **Stack Overflow**: Use the tag `dotcms-client` when posting questions.
-   **Enterprise Support**: Enterprise customers can access premium support through the [dotCMS Support Portal](https://helpdesk.dotcms.com/support/).

When reporting issues, please include:

-   SDK version you're using
-   dotCMS version
-   Minimal reproduction steps
-   Expected vs. actual behavior

## Contributing

GitHub pull requests are the preferred method to contribute code to dotCMS. We welcome contributions to the dotCMS Client SDK! If you'd like to contribute, please follow these steps:

1. Fork the repository [dotCMS/core](https://github.com/dotCMS/core)
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

Please ensure your code follows the existing style and includes appropriate tests.

## Changelog

### v1.3.0

#### ✨ Added - AI Search API (Experimental)

> **⚠️ Experimental Feature:** The AI API is experimental and may undergo breaking changes in future releases.

**New Features:**
- **`client.ai.search()`** - AI-powered semantic search using embeddings and vector similarity
- Search content by meaning rather than exact keyword matches
- Support for custom AI search indexes
- Configurable similarity thresholds and distance functions
- `DotErrorAISearch` error class for AI-specific error handling with prompt and index context

**Prerequisites:**
- Requires dotAI to be activated in your dotCMS instance
- OpenAI API key must be configured
- PostgreSQL 15+ with pgvector extension
- Content indexes must be created in dotAI

**Basic Usage:**
```typescript
// Semantic search with required index name
const results = await client.ai.search(
    'machine learning articles',
    'content_index'
);

// With advanced configuration
const results = await client.ai.search(
    'AI tutorials',
    'content_index',
    {
        query: {
            limit: 20,
            contentType: 'BlogPost'
        },
        config: {
            threshold: 0.75,
            distanceFunction: DISTANCE_FUNCTIONS.cosine
        }
    }
);
```

**Key Features:**
- Type-safe with full TypeScript support
- Supports multiple distance functions (cosine, L2, inner product, etc.)
- Returns match scores and extracted text excerpts
- Integrates seamlessly with existing content workflows

### v1.1.1

Version 1.1.1 introduces significant improvements to error handling and HTTP client architecture. Most applications will continue to work without changes.

#### ✨ Added - Enhanced Error Handling

**New Features:**
- Introduced specific error types: `DotErrorPage`, `DotErrorContent`, `DotErrorNavigation`
- Domain-specific errors wrap `DotHttpError` instances with contextual information
- Enhanced error context and debugging information
- Improved error handling in promise chains

**Migration Required If:**
- You're catching generic `Error` instances from SDK calls
- You're using `.then()` callbacks on content collections without return values
- You're parsing raw HTTP error responses manually

**Before (v1.0.x):**
```typescript
try {
    const { pageAsset } = await client.page.get('/about');
} catch (error) {
    // Generic error handling
    console.error('Error:', error.message);
    console.error('Status:', error.status); // May not exist
}

// Collection error handling
client.content.getCollection('Blog').then(
    (response) => console.log(response),
    (error) => {
        console.error(error.status); // Raw HTTP status
        // No return value required
    }
);
```

**After (v1.1.1):**
```typescript
try {
    const { pageAsset } = await client.page.get('/about');
} catch (error) {
    // Specific error type checking
    if (error instanceof DotErrorPage) {
        console.error('Page Error:', error.message);
        console.error('Context:', error.context);
        if (error.httpError) {
            console.error('HTTP Status:', error.httpError.status);
        }
    }
}

// Collection error handling with required return values
client.content.getCollection('Blog').then(
    (response) => {
        console.log(response);
        return response; // Return value recommended
    },
    (error) => {
        if (error instanceof DotErrorContent) {
            console.error('Content Error:', error.contentType, error.operation);
        }
        return { contentlets: [], total: 0 }; // Return fallback or re-throw
    }
);
```

#### ✨ Added - HTTP Client Architecture

**New Features:**
- New `DotHttpClient` interface for pluggable HTTP implementations
- Default `FetchHttpClient` replaces direct `fetch()` calls
- Better TypeScript support for custom HTTP clients

**Migration Required If:**
- You're extending or mocking SDK internals
- You need custom HTTP behavior (proxies, interceptors, etc.)

**New Capabilities:**
```typescript
// Custom HTTP client support
const client = createDotCMSClient({
    dotcmsUrl: 'https://your-instance.com',
    authToken: 'your-token',
    httpClient: new CustomHttpClient() // Optional: custom implementation
});
```

#### 🔄 Changed - Type Updates

**Improvements:**
- `RequestOptions` renamed to `DotRequestOptions`
- GraphQL response types improved
- Error response types standardized

**Migration Required If:**
- You're importing `RequestOptions` directly
- You're using internal type definitions

**Update Imports:**
```typescript
// Before
import { RequestOptions } from '@dotcms/types';

// After
import { DotRequestOptions } from '@dotcms/types';
```

## Licensing

dotCMS comes in multiple editions and as such is dual-licensed. The dotCMS Community Edition is licensed under the GPL 3.0 and is freely available for download, customization, and deployment for use within organizations of all stripes. dotCMS Enterprise Editions (EE) adds several enterprise features and is available via a supported, indemnified commercial license from dotCMS. For the differences between the editions, see [the feature page](http://www.dotcms.com/cms-platform/features).

This SDK is part of dotCMS's dual-licensed platform (GPL 3.0 for Community, commercial license for Enterprise).

[Learn more ](https://www.dotcms.com)at [dotcms.com](https://www.dotcms.com).
