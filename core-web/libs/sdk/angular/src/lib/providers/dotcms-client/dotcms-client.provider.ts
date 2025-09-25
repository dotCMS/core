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
export interface AngularDotCMSClient extends ClientType {}

// eslint-disable-next-line @typescript-eslint/no-unsafe-declaration-merging
export class AngularDotCMSClient {
    constructor(client: ClientType) {
        return client;
    }
}

/**
 * Provides Angular environment providers for the DotCMS client.
 *
 * Registers a singleton DotCMS client instance in the Angular dependency injection system,
 * configured with the given options. This allows you to inject `AngularDotCMSClient` anywhere
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
            provide: AngularDotCMSClient,
            useFactory: () => {
                const http = inject(HttpClient);
                const httpClient = options.httpClient ? options.httpClient(http) : undefined;
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

export interface DotCMSAngularProviderConfig extends Omit<DotCMSClientConfig, 'httpClient'> {
    httpClient?: (http: HttpClient) => DotHttpClient;
}
