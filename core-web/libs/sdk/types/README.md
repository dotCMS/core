# dotCMS Types Library

The `@dotcms/types` package contains TypeScript type definitions for the dotCMS ecosystem. Use it to enable type safety and an enhanced developer experience when working with dotCMS APIs and structured content.

📦 [@dotcms/types on npm](https://www.npmjs.com/package/@dotcms/types)
🛠️ [View source on GitHub](https://github.com/dotCMS/core/tree/main/core-web/libs/sdk/types)

## Table of Contents

- [Overview](#overview)
- [Installation](#installation)
- [Commonly Used Types](#commonly-used-types)
- [Type Hierarchy (Jump to Definitions)](#type-hierarchy-jump-to-definitions)
  - [AI Search](#ai-search)
  - [dotCMS Content & Pages](#dotcms-content--pages)
  - [Universal Visual Editor (UVE)](#universal-visual-editor-uve)
  - [Block Editor](#block-editor)
  - [Client & HTTP](#client--http)
  - [Error Handling](#error-handling)
- [Usage Examples](#usage-examples)
  - [Error Type Checking](#error-type-checking)
- [Support](#support)
- [Contributing](#contributing)
- [Licensing](#licensing)
- [Changelog](#changelog)

## Overview

### When to Use It

- Building TypeScript applications with dotCMS
- Enabling type safety in your dotCMS integrations
- Getting better IDE autocomplete and error checking
- Working with dotCMS Client SDK or other SDK packages

### Key Benefits

- **Type Safety**: Catch errors at compile time instead of runtime
- **IDE Support**: Rich autocomplete and inline documentation
- **Better Developer Experience**: Clear interfaces for all dotCMS data structures
- **Framework Agnostic**: Works with any TypeScript project

## Installation

```bash
npm install @dotcms/types@latest --save-dev
```

## Commonly Used Types

```ts
import {
  DotCMSPageAsset,
  DotCMSPageResponse,
  UVEEventType,
  DotCMSInlineEditingPayload,
  DotHttpClient,
  DotHttpError,
  DotErrorPage,
  DotErrorContent,
  DotErrorNavigation,
  DotErrorAISearch,
  DotCMSAISearchParams,
  DISTANCE_FUNCTIONS
} from '@dotcms/types';
```

## Type Hierarchy (Jump to Definitions)

### AI Search

**AI Search Parameters:**

| Type | Description |
|------|-------------|
| [DotCMSAISearchParams](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/ai/public.ts#L85) | Complete AI search parameters including query and AI config |
| [DotCMSAISearchQuery](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/ai/public.ts#L9) | Query parameters (limit, offset, contentType, indexName, etc.) |
| [DotCMSAIConfig](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/ai/public.ts#L50) | AI configuration (threshold, distanceFunction, responseLength) |
| [DISTANCE_FUNCTIONS](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/ai/public.ts#L103) | Available distance functions for vector similarity |

**AI Search Response:**

| Type | Description |
|------|-------------|
| [DotCMSAISearchResponse](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/ai/public.ts#L182) | AI search API response structure |
| [DotCMSAISearchContentletData](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/ai/public.ts#L213) | Contentlet with AI match information |
| [DotCMSAISearchMatch](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/ai/public.ts#L195) | Individual match with distance score and extracted text |

**AI Search Errors:**

| Type | Description |
|------|-------------|
| [DotErrorAISearch](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/ai/public.ts#L136) | AI Search API specific error with prompt and params context |

### dotCMS Content & Pages

**Page:**

| Type | Description |
|------|-------------|
| [DotCMSPageAsset](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/page/public.ts#L18) | Complete page with layout and content |
| [DotCMSPage](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/page/public.ts#L515) | Core page data |
| [DotCMSPageResponse](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/page/public.ts#L1175) | API response for page requests |
| [DotGraphQLApiResponse](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/page/public.ts#L1186) | GraphQL API response structure |

**Content:**

| Type | Description |
|------|-------------|
| [DotCMSBasicContentlet](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/page/public.ts#L1141) | Basic contentlet structure |

**Site & Layout:**

| Type | Description |
|------|-------------|
| [DotCMSSite](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/page/public.ts#L733) | Site information |
| [DotCMSTemplate](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/page/public.ts#L432) | Page templates |
| [DotCMSLayout](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/page/public.ts#L622) | Page layout structure |
| [DotCMSPageAssetContainer](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/page/public.ts#L138) | Container definitions |

**Navigation:**

| Type | Description |
|------|-------------|
| [DotCMSNavigationItem](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/nav/public.ts#L73) | Navigation structure item with hierarchy support |
| [DotCMSVanityUrl](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/page/public.ts#L80) | URL rewrites and vanity URLs |
| [DotCMSURLContentMap](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/page/public.ts#L41) | URL to content mapping |

### Universal Visual Editor (UVE)

**Editor State:**

| Type | Description |
|------|-------------|
| [UVEState](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/editor/public.ts#L30) | Current editor state |
| [UVE_MODE](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/editor/public.ts#L55) | Editor modes (EDIT, PREVIEW, PUBLISHED) |
| [DotCMSPageRendererMode](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/editor/public.ts#L44) | Page rendering modes |

**Editor Events:**

| Type | Description |
|------|-------------|
| [UVEEventHandler](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/editor/public.ts#L67) | Event handler functions |
| [UVEEventSubscriber](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/editor/public.ts#L90) | Event subscription management |
| [UVEEventType](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/editor/public.ts#L169) | Available event types |
| [UVEEventPayloadMap](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/editor/public.ts#L199) | Event payload definitions |

**Inline Editing:**

| Type | Description |
|------|-------------|
| [DotCMSInlineEditingPayload](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/events/public.ts#L13) | Inline editing data |
| [DotCMSInlineEditingType](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/events/public.ts#L1) | Types of inline editing |

### Block Editor

| Type | Description |
|------|-------------|
| [BlockEditorContent](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/components/block-editor-renderer/public.ts#L38) | Block editor content structure |
| [BlockEditorNode](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/components/block-editor-renderer/public.ts#L18) | Individual blocks/nodes |
| [BlockEditorMark](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/components/block-editor-renderer/public.ts#L7) | Text formatting marks |

### Client & HTTP

**HTTP Client:**

| Type | Description |
|------|-------------|
| [DotHttpClient](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/client/public.ts#L108) | HTTP client interface for custom implementations |
| [BaseHttpClient](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/client/public.ts#L196) | Abstract base class with error handling utilities |

**Client Configuration:**

| Type | Description |
|------|-------------|
| [DotCMSClientConfig](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/client/public.ts#L395) | Client configuration options |
| [DotRequestOptions](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/client/public.ts#L383) | HTTP request options |
| [DotCMSPageRequestParams](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/client/public.ts#L378) | Page request parameters |
| [DotCMSGraphQLParams](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/client/public.ts#L280) | GraphQL query parameters |
| [DotCMSNavigationRequestParams](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/nav/public.ts#L39) | Navigation request options |

### Error Handling

**Base Error Types:**

| Type | Description |
|------|-------------|
| [HttpErrorDetails](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/client/public.ts#L6) | HTTP error details interface |
| [DotHttpError](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/client/public.ts#L26) | Standardized HTTP error class |

**Domain-Specific Errors:**

| Type | Description |
|------|-------------|
| [DotErrorPage](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/page/public.ts#L1253) | Page API errors with GraphQL context |
| [DotErrorContent](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/content/public.ts#L7) | Content API specific error handling |
| [DotErrorNavigation](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/nav/public.ts#L7) | Navigation API error handling |
| [DotErrorAISearch](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/ai/public.ts#L136) | AI Search API error handling with prompt and params |

## Usage Examples

### Error Type Checking

```typescript
import {
  DotHttpError,
  DotErrorPage,
  DotErrorContent,
  DotErrorNavigation,
  DotErrorAISearch
} from '@dotcms/types';

// Type-safe error handling
if (error instanceof DotHttpError) {
  // Access standardized HTTP error properties
  console.error(`HTTP ${error.status}: ${error.statusText}`);
  console.error('Response data:', error.data);
}

if (error instanceof DotErrorPage) {
  // Page-specific error with GraphQL context
  console.error('GraphQL query:', error.graphql?.query);
}

if (error instanceof DotErrorContent) {
  // Content-specific error context
  console.error(`${error.operation} failed for ${error.contentType}`);
}

if (error instanceof DotErrorAISearch) {
  // AI Search-specific error context
  console.error('AI Search failed for prompt:', error.prompt);
  console.error('Search params:', error.params);
}
```

> **Note**: For complete implementation examples and usage patterns, see the [@dotcms/client](../client/README.md) package documentation.

## Support

We offer multiple channels to get help with the dotCMS Types library:

-   **GitHub Issues**: For bug reports and feature requests, please [open an issue](https://github.com/dotCMS/core/issues/new/choose) in the GitHub repository
-   **Community Forum**: Join our [community discussions](https://community.dotcms.com/) to ask questions and share solutions
-   **Stack Overflow**: Use the tag `dotcms-types` when posting questions
-   **Enterprise Support**: Enterprise customers can access premium support through the [dotCMS Support Portal](https://helpdesk.dotcms.com/support/)

When reporting issues, please include:

-   Package version you're using
-   TypeScript version
-   Minimal reproduction steps
-   Expected vs. actual behavior

## Contributing

GitHub pull requests are the preferred method to contribute code to dotCMS. We welcome contributions to the dotCMS Types library! If you'd like to contribute, please follow these steps:

1. Fork the repository [dotCMS/core](https://github.com/dotCMS/core)
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

Please ensure your code follows the existing style and includes appropriate tests.

## Changelog

### [1.1.1]

#### Added
- `DotHttpClient` interface for custom HTTP client implementations
- `BaseHttpClient` abstract class with built-in error handling utilities
- `DotHttpError` class for standardized HTTP error handling
- `DotErrorPage` class for page-specific errors with GraphQL query context
- `DotErrorContent` class for content API errors with operation details
- `DotErrorNavigation` class for navigation-specific error handling
- `DotErrorAISearch` class for AI search-specific errors with prompt and params context
- `DotCMSAISearchParams` interface for AI search parameters
- `DotCMSAISearchQuery` interface for AI search query configuration
- `DotCMSAIConfig` interface for AI configuration options
- `DotCMSAISearchResponse` interface for AI search responses
- `DotCMSAISearchMatch` interface for AI match data with distance scores
- `DotCMSAISearchContentletData` type for contentlets with AI match information
- `DISTANCE_FUNCTIONS` constant with vector similarity distance functions
- `DotGraphQLApiResponse` interface for GraphQL API responses
- `HttpErrorDetails` interface for HTTP error standardization
- All error classes include `toJSON()` methods for easy logging and serialization

#### Changed
- Renamed `RequestOptions` to `DotRequestOptions` for better naming consistency
- Renamed `DotCMSGraphQLPageResponse` to `DotGraphQLApiResponse` for clarity
- Enhanced `DotCMSClientConfig` to support custom `httpClient` implementations

## Licensing

dotCMS comes in multiple editions and as such is dual-licensed. The dotCMS Community Edition is licensed under the GPL 3.0 and is freely available for download, customization, and deployment for use within organizations of all stripes. dotCMS Enterprise Editions (EE) adds several enterprise features and is available via a supported, indemnified commercial license from dotCMS. For the differences between the editions, see [the feature page](http://www.dotcms.com/cms-platform/features).

This package is part of dotCMS's dual-licensed platform (GPL 3.0 for Community, commercial license for Enterprise).
