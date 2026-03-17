# Base Builder

This directory contains the base class for DotCMS **builder-style** clients (fluent, chainable APIs that ultimately execute an HTTP request).

## Overview

The `BaseBuilder<T>` abstract class provides shared functionality used by content query builders, including:

- Pagination: `limit()`, `page()` (with `offset` computed internally)
- Sorting: `sortBy()` (serialized into a `sort` string)
- Rendering: `render()`
- Relationship depth: `depth()` (validated `0..3`)
- Response mapping: converts the raw Content API response into `GetCollectionResponse<T>`
- Promise-like interface: implements `then(...)` so builders can be `await`-ed directly

Like `BaseApiClient`, it uses **JavaScript private fields (`#`)** for state while exposing behavior through methods.

## Benefits

1. **DRY (Don't Repeat Yourself)**: eliminates duplicated pagination/sort/render/depth logic across builders
2. **Consistency**: all builders send the same request shape to the Content API
3. **Maintainability**: fixes to shared query execution or response mapping happen in one place
4. **Extensibility**: new builders only implement the parts that are truly specific to their query type

## How it works

`BaseBuilder` is responsible for assembling and executing the Content API request:

- It asks the subclass to provide the final Lucene query via `buildFinalQuery()`
- It optionally includes `languageId` in the request body only when `getLanguageId()` returns a value
- It maps the response to `GetCollectionResponse<T>`
- It wraps request failures into `DotErrorContent` via the subclass’ `wrapError(...)`

### Abstract methods you must implement

- `protected buildFinalQuery(): string`
- `protected getLanguageId(): number | string | undefined`
- `protected wrapError(error: unknown): DotErrorContent`

## Usage

When creating a new builder, extend `BaseBuilder<T>` and implement the abstract methods:

```typescript
import { DotCMSClientConfig, DotRequestOptions, DotHttpClient, DotErrorContent } from '@dotcms/types';
import { BaseBuilder } from './base-builder';

export class MyBuilder<T = unknown> extends BaseBuilder<T> {
  constructor(params: {
    requestOptions: DotRequestOptions;
    config: DotCMSClientConfig;
    httpClient: DotHttpClient;
  }) {
    super(params);
  }

  protected buildFinalQuery(): string {
    return '+contentType:MyType';
  }

  // Return undefined to omit languageId from the request body
  protected getLanguageId(): number | string | undefined {
    return 1;
  }

  protected wrapError(error: unknown): DotErrorContent {
    return new DotErrorContent('MyBuilder failed', 'my-type', 'fetch', undefined, this.buildFinalQuery());
  }
}
```

Then consumers can chain options and execute:

```typescript
const response = await new MyBuilder({ requestOptions, config, httpClient })
  .limit(10)
  .page(2)
  .sortBy([{ field: 'modDate', order: 'desc' }])
  .depth(1)
  .render();
```

## Existing Builders

Current builders that rely on `BaseBuilder` include:

- `CollectionBuilder` (content-type oriented querying)
- `RawQueryBuilder` (raw Lucene string queries)


