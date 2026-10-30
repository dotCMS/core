import { EnvironmentProviders, makeEnvironmentProviders } from '@angular/core';

import { createDotCMSClient } from '@dotcms/client';
import { DotCMSClientConfig } from '@dotcms/types';

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
    const dotCMSClient = createDotCMSClient(options);

    return makeEnvironmentProviders([
        {
            provide: DotCMSClient,
            useFactory: () => new DotCMSClient(dotCMSClient)
        }
    ]);
}
