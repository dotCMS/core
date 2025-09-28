import { HttpClient } from '@angular/common/http';
import { EnvironmentProviders, inject, makeEnvironmentProviders } from '@angular/core';

import { createDotCMSClient } from '@dotcms/client';
import { DotCMSClientConfig, DotHttpClient } from '@dotcms/types';

/**
 * Type alias for the return type of createDotCMSClient function.
 * Used to ensure type consistency across the DotCMSClient interface and class.
 */
type ClientType = ReturnType<typeof createDotCMSClient>;

// This is a hack inspired by https://github.com/angular/angularfire/blob/c1c6af9779154caff6bc0d9b837f6c3e2d913456/src/firestore/firestore.ts#L8

/**
 * Interface that extends the client type created by createDotCMSClient.
 * This interface provides type safety and IntelliSense support for the DotCMS client
 * when used as a dependency injection token in Angular applications.
 *
 * @example
 * ```typescript
 * dotcmsClient = inject(DotCMSClient);
 * ```
 */
// eslint-disable-next-line @typescript-eslint/no-unsafe-declaration-merging, @typescript-eslint/no-empty-interface, @typescript-eslint/no-empty-object-type
export interface DotCMSClient extends ClientType {}

// eslint-disable-next-line @typescript-eslint/no-unsafe-declaration-merging
export class DotCMSClient {
    constructor(client: ClientType) {
        return client;
    }
}

/**
 * Provides Angular environment providers for the DotCMS client.
 *
 * Registers a singleton DotCMS client instance in the Angular dependency injection system,
 * configured with the given options. This allows you to inject `DotCMSClient` anywhere
 * in your app using Angular's `inject()` function.
 *
 * Should be added to the application's providers (e.g., in `main.ts` or `app.config.ts`).
 *
 * @param options - Configuration for the DotCMS client.
 *   @param options.dotcmsUrl - The base URL for the DotCMS instance (required).
 *   @param options.authToken - Authentication token for API requests (required).
 *   @param options.siteId - The site identifier (optional).
 *   @param options.requestOptions - Additional fetch options (optional).
 *   @param options.httpClient - Optional factory for a custom HTTP client, receives Angular's HttpClient.
 * @returns Angular environment providers for the DotCMS client.
 *
 * @example
 * import { provideDotCMSClient } from '@dotcms/angular';
 *
 * bootstrapApplication(AppComponent, {
 *   providers: [
 *     provideDotCMSClient({
 *       dotcmsUrl: 'https://demo.dotcms.com',
 *       authToken: 'your-auth-token',
 *       siteId: 'your-site-id',
 *       httpClient: (http) => new AngularHttpClient(http)
 *     })
 *   ]
 * });
 */
export function provideDotCMSClient(options: DotCMSAngularProviderConfig): EnvironmentProviders {
    return makeEnvironmentProviders([
        {
            provide: DotCMSClient,
            useFactory: () => {
                const httpClient = options.httpClient
                    ? options.httpClient(inject(HttpClient))
                    : undefined;
                const dotCMSClient = createDotCMSClient({
                    dotcmsUrl: options.dotcmsUrl,
                    authToken: options.authToken,
                    siteId: options.siteId,
                    httpClient: httpClient
                });

                return dotCMSClient;
            }
        }
    ]);
}

/**
 * Configuration interface for the DotCMS Angular provider.
 *
 * Extends the base `DotCMSClientConfig` but replaces the `httpClient` property
 * with an Angular-specific factory function that receives Angular's `HttpClient`
 * and returns a `DotHttpClient` implementation.
 *
 * This interface is designed to work seamlessly with Angular's dependency injection
 * system, allowing you to leverage Angular's built-in HTTP client while maintaining
 * compatibility with the DotCMS client's expected interface.
 *
 * @example
 * ```typescript
 * const config: DotCMSAngularProviderConfig = {
 *   dotcmsUrl: 'https://demo.dotcms.com',
 *   authToken: 'your-auth-token',
 *   siteId: 'your-site-id',
 *   httpClient: (http: HttpClient) => new AngularHttpClient(http)
 * };
 * ```
 *
 * @example
 * ```typescript
 * // Using with provideDotCMSClient
 * provideDotCMSClient({
 *   dotcmsUrl: 'https://demo.dotcms.com',
 *   authToken: 'your-auth-token',
 *   httpClient: (http) => new AngularHttpClient(http)
 * })
 * ```
 */
export interface DotCMSAngularProviderConfig extends Omit<DotCMSClientConfig, 'httpClient'> {
    /**
     * Optional factory function that creates a custom HTTP client implementation.
     *
     * This function receives Angular's `HttpClient` instance and should return
     * a `DotHttpClient` implementation. If not provided, the DotCMS client will
     * use its default HTTP client implementation.
     *
     * @param http - Angular's HttpClient instance from dependency injection
     * @returns A DotHttpClient implementation
     *
     * @example
     * ```typescript
     * httpClient: (http: HttpClient) => {
     *   return new AngularHttpClient(http);
     * }
     * ```
     */
    httpClient?: (http: HttpClient) => DotHttpClient;
}
