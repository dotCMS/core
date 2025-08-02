# DotCMS SDK HTTP Client Examples

This document provides examples of how to use the pluggable HTTP client feature in the DotCMS SDK.

## Default Usage (No Changes Required)

The SDK continues to work exactly as before with the default fetch implementation:

```typescript
import { createDotCMSClient } from '@dotcms/sdk';

const client = createDotCMSClient({
  dotcmsUrl: 'https://demo.dotcms.com',
  authToken: 'your-auth-token'
});

// Works exactly as before
const pages = await client.page.get('/about-us');
const navigation = await client.nav.get('/');
const content = await client.content.getCollection('Blog').limit(10);
```

## Using Axios

```typescript
import axios from 'axios';
import { createDotCMSClient, HttpClient } from '@dotcms/sdk';

class AxiosHttpClient implements HttpClient {
  async request<T = any>(url: string, options?: any): Promise<T> {
    const response = await axios({
      url,
      method: options?.method || 'GET',
      headers: options?.headers,
      data: options?.body,
      ...options
    });

    return response.data;
  }
}

const client = createDotCMSClient({
  dotcmsUrl: 'https://demo.dotcms.com',
  authToken: 'your-auth-token',
  httpClient: new AxiosHttpClient()
});

// Now uses Axios for all HTTP requests
const pages = await client.page.get('/about-us');
```

## Using Angular HttpClient

```typescript
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { Injectable } from '@angular/core';
import { createDotCMSClient, HttpClient as DotCMSHttpClient } from '@dotcms/sdk';

class AngularHttpClient implements DotCMSHttpClient {
  constructor(private http: HttpClient) {}

  async request<T = any>(url: string, options?: any): Promise<T> {
    return firstValueFrom(
      this.http.request<T>(options?.method || 'GET', url, {
        headers: options?.headers,
        body: options?.body,
        ...options
      })
    );
  }
}

@Injectable()
export class DotCMSService {
  private client: any;

  constructor(private http: HttpClient) {
    this.client = createDotCMSClient({
      dotcmsUrl: 'https://demo.dotcms.com',
      authToken: 'your-auth-token',
      httpClient: new AngularHttpClient(this.http)
    });
  }

  getPages() {
    return this.client.page.get('/about-us');
  }
}
```

## Custom Implementation with Retry Logic

```typescript
import { createDotCMSClient, HttpClient } from '@dotcms/sdk';

class RetryHttpClient implements HttpClient {
  private maxRetries = 3;

  async request<T = any>(url: string, options?: any): Promise<T> {
    let lastError: Error;

    for (let i = 0; i < this.maxRetries; i++) {
      try {
        const response = await fetch(url, options);

        if (!response.ok) {
          throw new Error(`HTTP ${response.status}`);
        }

        return response.json();
      } catch (error) {
        lastError = error as Error;
        if (i < this.maxRetries - 1) {
          await new Promise(resolve => setTimeout(resolve, 1000 * (i + 1)));
        }
      }
    }

    throw lastError!;
  }
}

const client = createDotCMSClient({
  dotcmsUrl: 'https://demo.dotcms.com',
  authToken: 'your-auth-token',
  httpClient: new RetryHttpClient()
});
```

## Custom Implementation with Logging

```typescript
import { createDotCMSClient, HttpClient } from '@dotcms/sdk';

class LoggingHttpClient implements HttpClient {
  constructor(private httpClient: HttpClient) {}

  async request<T = any>(url: string, options?: any): Promise<T> {
    console.log(`[HTTP Request] ${options?.method || 'GET'} ${url}`);
    const startTime = Date.now();

    try {
      const result = await this.httpClient.request<T>(url, options);
      const duration = Date.now() - startTime;
      console.log(`[HTTP Response] ${options?.method || 'GET'} ${url} - ${duration}ms`);
      return result;
    } catch (error) {
      const duration = Date.now() - startTime;
      console.error(`[HTTP Error] ${options?.method || 'GET'} ${url} - ${duration}ms`, error);
      throw error;
    }
  }
}

const client = createDotCMSClient({
  dotcmsUrl: 'https://demo.dotcms.com',
  authToken: 'your-auth-token',
  httpClient: new LoggingHttpClient(new FetchHttpClient())
});
```

## Using with Node.js HTTP/HTTPS

```typescript
import { createDotCMSClient, HttpClient } from '@dotcms/sdk';
import * as https from 'https';
import * as http from 'http';

class NodeHttpClient implements HttpClient {
  async request<T = any>(url: string, options?: any): Promise<T> {
    return new Promise((resolve, reject) => {
      const urlObj = new URL(url);
      const isHttps = urlObj.protocol === 'https:';
      const client = isHttps ? https : http;

      const requestOptions = {
        hostname: urlObj.hostname,
        port: urlObj.port || (isHttps ? 443 : 80),
        path: urlObj.pathname + urlObj.search,
        method: options?.method || 'GET',
        headers: options?.headers || {}
      };

      const req = client.request(requestOptions, (res) => {
        let data = '';

        res.on('data', (chunk) => {
          data += chunk;
        });

        res.on('end', () => {
          if (res.statusCode && res.statusCode >= 200 && res.statusCode < 300) {
            try {
              const jsonData = JSON.parse(data);
              resolve(jsonData);
            } catch {
              resolve(data as any);
            }
          } else {
            reject(new Error(`HTTP ${res.statusCode}: ${res.statusMessage}`));
          }
        });
      });

      req.on('error', (error) => {
        reject(error);
      });

      if (options?.body) {
        req.write(options.body);
      }

      req.end();
    });
  }
}

const client = createDotCMSClient({
  dotcmsUrl: 'https://demo.dotcms.com',
  authToken: 'your-auth-token',
  httpClient: new NodeHttpClient()
});
```

## Testing with Mock HTTP Client

```typescript
import { createDotCMSClient, HttpClient } from '@dotcms/sdk';

// In your tests
const mockHttpClient: HttpClient = {
  request: jest.fn().mockResolvedValue({
    entity: [{ name: 'Test Page', title: 'Test' }]
  })
};

const client = createDotCMSClient({
  dotcmsUrl: 'https://demo.dotcms.com',
  authToken: 'token',
  httpClient: mockHttpClient
});

// Test your client
const result = await client.nav.get('/test');
expect(mockHttpClient.request).toHaveBeenCalled();
```

## Interface Definition

The `HttpClient` interface is defined as:

```typescript
export interface HttpClient {
  /**
   * Makes an HTTP request.
   *
   * @param url - The URL to request
   * @param options - Request options (method, headers, body, etc.)
   * @returns A promise that resolves with the response data
   */
  request<T = any>(url: string, options?: RequestOptions): Promise<T>;
}

export interface HttpClientRequestOptions extends Omit<RequestInit, 'body' | 'method'> {
  /**
   * The HTTP method to use for the request.
   */
  method?: string;

  /**
   * The request body.
   */
  body?: string | FormData | URLSearchParams | ReadableStream | null;
}
```

## Migration Guide

If you're upgrading from a previous version of the SDK, no changes are required. The default behavior remains the same using the native fetch API.

To use a custom HTTP client, simply add the `httpClient` property to your configuration:

```typescript
// Before (still works)
const client = createDotCMSClient({
  dotcmsUrl: 'https://demo.dotcms.com',
  authToken: 'your-auth-token'
});

// After (with custom HTTP client)
const client = createDotCMSClient({
  dotcmsUrl: 'https://demo.dotcms.com',
  authToken: 'your-auth-token',
  httpClient: new CustomHttpClient()
});
```
