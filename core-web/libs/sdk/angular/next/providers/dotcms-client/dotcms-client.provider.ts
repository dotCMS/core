import { makeEnvironmentProviders, EnvironmentProviders } from '@angular/core';

import { createDotCMSClient } from '@dotcms/client/next';
import { DotCMSClientConfig } from '@dotcms/types';

/**
 * Type alias for the return type of createDotCMSClient function.
 * Used to ensure type consistency across the DotCMSClient interface and class.
 */
type clientType = ReturnType<typeof createDotCMSClient>;

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
// eslint-disable-next-line @typescript-eslint/no-unsafe-declaration-merging, @typescript-eslint/no-empty-interface
export interface DotCMSClient extends clientType {}

/**
 * Injectable class that wraps the DotCMS client for use in Angular's dependency injection system.
 * This class acts as a bridge between the DotCMS client library and Angular's DI container,
 * allowing the client to be injected into components, services, and other Angular constructs.
 *
 * The class uses declaration merging with the interface above to provide both the contract
 * and the implementation while maintaining type safety.
 *
 * @example
 * ```typescript
 * @Component({
 *   // ...
 * })
 * export class MyComponent {
 *   dotcmsClient = inject(DotCMSClient);
 *
 *   async loadContent() {
 *     const content = await this.dotcmsClient.page.get();
 *     return content;
 *   }
 * }
 * ```
 */
// eslint-disable-next-line @typescript-eslint/no-unsafe-declaration-merging
export class DotCMSClient {
    /**
     * Creates a new DotCMSClient instance that wraps the provided client.
     *
     * @param client - The DotCMS client instance created by createDotCMSClient
     * @returns The client instance (due to the return override)
     */
    constructor(client: clientType) {
        return client;
    }
}

/**
 * Creates environment providers for the DotCMS client to be used in Angular applications.
 * This function configures the DI container to provide a DotCMSClient instance
 * throughout the application using the specified configuration.
 *
 * The provider should be registered at the application level (typically in main.ts)
 * to ensure a single instance is shared across the entire application.
 *
 * @param options - Configuration object for the DotCMS client
 * @param options.apiUrl - The base URL for the DotCMS API
 * @param options.authToken - Authentication token for API requests (optional)
 * @param options.siteId - The site identifier (optional)
 * @returns Environment providers array that can be used with bootstrapApplication
 *
 * @example
 * ```typescript
 * // main.ts
 * import { bootstrapApplication } from '@angular/platform-browser';
 * import { AppComponent } from './app/app.component';
 * import { provideDotCMSClient } from '@dotcms/angular';
 *
 * bootstrapApplication(AppComponent, {
 *   providers: [
 *     provideDotCMSClient({
 *       apiUrl: 'https://demo.dotcms.com',
 *       authToken: 'your-auth-token',
 *       siteId: 'your-site-id'
 *     }),
 *     // other providers...
 *   ]
 * });
 * ```
 *
 */
export function provideDotCMSClient(options: DotCMSClientConfig): EnvironmentProviders {
    const client = createDotCMSClient(options);

    return makeEnvironmentProviders([
        {
            provide: DotCMSClient,
            useFactory: () => new DotCMSClient(client)
        }
    ]);
}
