import { DotCMSClientConfig } from '@dotcms/types';

import { NavigationClient } from './lib/client/navigation/navigation-api';
import { createClientContext } from './lib/client/shared/client-context';

export type { NavigationClient };

/**
 * Creates a client for navigation-related operations only.
 *
 * Navigation is the smallest of the four APIs; reaching it through
 * {@link createDotCMSClient} pulls in page, content-query and AI code as well.
 *
 * @param config - Configuration options for the client
 * @returns A navigation client bound to the given dotCMS instance
 * @throws {TypeError} when `dotcmsUrl` is not a valid URL or `authToken` is missing
 * @example
 * ```typescript
 * import { createDotCMSNavigationClient } from '@dotcms/client/navigation';
 *
 * const nav = createDotCMSNavigationClient({
 *   dotcmsUrl: 'https://demo.dotcms.com',
 *   authToken: 'your-auth-token'
 * });
 *
 * const tree = await nav.get('/', { depth: 2 });
 * ```
 */
export const createDotCMSNavigationClient = (config: DotCMSClientConfig): NavigationClient => {
    const { config: resolved, requestOptions, httpClient } = createClientContext(config);

    return new NavigationClient(resolved, requestOptions, httpClient);
};
