# Base API Client

This directory contains the base class for all DotCMS API clients.

## Overview

The `BaseApiClient` abstract class provides common functionality and properties that all API clients need. It uses **JavaScript private fields (`#`)** with **protected getters** to maintain strict encapsulation while allowing child classes controlled access.

This approach provides:
- **True runtime privacy**: Fields are genuinely private, not just at compile-time
- **Read-only access**: Child classes can only read values through getters, not modify them
- **Type safety**: TypeScript enforces access patterns at compile-time

### Protected Getters

- `config` - Client configuration
- `requestOptions` - Request options including auth headers
- `httpClient` - HTTP client for making requests
- `dotcmsUrl` - Helper to access the DotCMS URL
- `siteId` - Helper to access the site ID

## Benefits

1. **DRY (Don't Repeat Yourself)**: Eliminates duplicate constructor logic across all API clients
2. **Consistency**: Ensures all API clients have the same base configuration
3. **Maintainability**: Changes to common functionality only need to be made in one place
4. **Extensibility**: Makes it easy to add new API clients
5. **Encapsulation**: Properties are private with controlled access through protected getters

## Usage

When creating a new API client, extend `BaseApiClient`:

```typescript
import { BaseApiClient } from '../base/base-api';
import { DotCMSClientConfig, DotRequestOptions, DotHttpClient } from '@dotcms/types';

export class MyNewClient extends BaseApiClient {
    constructor(
        config: DotCMSClientConfig,
        requestOptions: DotRequestOptions,
        httpClient: DotHttpClient
    ) {
        super(config, requestOptions, httpClient);
    }

    // Add your API-specific methods here
    async myMethod() {
        // You have access to:
        // - this.config
        // - this.requestOptions
        // - this.httpClient
        // - this.dotcmsUrl (getter)
        // - this.siteId (getter)

        const url = `${this.dotcmsUrl}/api/v1/my-endpoint`;
        return await this.httpClient.request(url, this.requestOptions);
    }
}
```

## Existing API Clients

The following clients now extend `BaseApiClient`:

- `PageClient` - Page-related operations
- `NavigationClient` - Navigation-related operations
- `Content` - Content collection operations
- `AIClient` - AI-related operations (new)

