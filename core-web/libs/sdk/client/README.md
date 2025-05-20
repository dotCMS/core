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


> **Note:** The `siteId` is optional, but if you don't provide it, you may need to specify it in individual API calls. Learn more about sites in the [dotCMS Multi-Site Management](https://dev.dotcms.com/docs/multi-site-management).


```javascript
const dotCMSClient = createDotCMSClient({
    dotcmsUrl: 'https://your-dotcms-instance.com',
    authToken: 'your-auth-token',
    siteId: 'your-site-id'
});
```

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










