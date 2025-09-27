# DotCMS Type Definition Library

📦 [@dotcms/types on npm](https://www.npmjs.com/package/@dotcms/types)
🛠️ [View source on GitHub](https://github.com/dotCMS/core/tree/main/core-web/libs/sdk/types)

## Installation

```bash
npm install @dotcms/types@latest --save-dev
```

## Overview

This package contains TypeScript type definitions for the dotCMS ecosystem. Use it to enable type safety and an enhanced developer experience when working with dotCMS APIs and structured content.

## Table of Contents

- [Installation](#installation)
- [Overview](#overview)
- [Commonly Used Types](#commonly-used-types)
- [Type Hierarchy (Jump to Definitions)](#type-hierarchy-jump-to-definitions)
  - [dotCMS Content & Pages](#dotcms-content--pages)
  - [Universal Visual Editor (UVE)](#universal-visual-editor-uve)
  - [Block Editor](#block-editor)
  - [Client & HTTP](#client--http)
  - [Error Handling](#error-handling)
- [Type Usage](#type-usage)
  - [Error Type Checking](#error-type-checking)
- [About](#about)
- [Changelog](#changelog)

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
  DotErrorNavigation
} from '@dotcms/types';
```

## Type Hierarchy (Jump to Definitions)

### dotCMS Content & Pages

**Page Types:**

| Type | Description |
|------|-------------|
| [DotCMSPageAsset](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/page/public.ts#L18) | Complete page with layout and content |
| [DotCMSPage](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/page/public.ts#L515) | Core page data |
| [DotCMSPageResponse](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/page/public.ts#L1175) | API response for page requests |
| [DotGraphQLApiResponse](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/page/public.ts#L1186) | GraphQL API response structure |

**Content Types:**

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

**Navigation & URLs:**

| Type | Description |
|------|-------------|
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

**Request Configuration:**

| Type | Description |
|------|-------------|
| [DotCMSClientConfig](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/client/public.ts#L395) | Client configuration options |
| [DotRequestOptions](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/client/public.ts#L383) | HTTP request options |
| [DotCMSPageRequestParams](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/client/public.ts#L378) | Page request parameters |
| [DotCMSGraphQLParams](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/client/public.ts#L280) | GraphQL query parameters |
| [DotCMSNavigationRequestParams](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/client/public.ts#L436) | Navigation request options |

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
| [DotErrorNavigation](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/client/public.ts#L50) | Navigation API error handling |

## Type Usage

### Error Type Checking

```typescript
import {
  DotHttpError,
  DotErrorPage,
  DotErrorContent,
  DotErrorNavigation
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
```

> **Note**: For complete implementation examples and usage patterns, see the [@dotcms/client](../client/README.md) package documentation.

## About

This package is maintained as part of the [dotCMS core repository](https://github.com/dotCMS/core).

### Keywords

* dotcms
* typescript
* types
* cms
* content-management-system

## Changelog

### [1.1.0] - 2024-XX-XX

#### Added
- `DotHttpClient` interface for custom HTTP client implementations
- `BaseHttpClient` abstract class with built-in error handling utilities
- `DotHttpError` class for standardized HTTP error handling
- `DotErrorPage` class for page-specific errors with GraphQL query context
- `DotErrorContent` class for content API errors with operation details
- `DotErrorNavigation` class for navigation-specific error handling
- `DotGraphQLApiResponse` interface for GraphQL API responses
- `HttpErrorDetails` interface for HTTP error standardization
- All error classes include `toJSON()` methods for easy logging and serialization

#### Changed
- Renamed `RequestOptions` to `DotRequestOptions` for better naming consistency
- Renamed `DotCMSGraphQLPageResponse` to `DotGraphQLApiResponse` for clarity
- Enhanced `DotCMSClientConfig` to support custom `httpClient` implementations
