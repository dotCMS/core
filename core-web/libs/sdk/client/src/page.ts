import { DotCMSClientConfig } from '@dotcms/types';

import { PageClient } from './lib/client/page/page-api';
import { createClientContext } from './lib/client/shared/client-context';

export type { PageClient };

/**
 * Creates a client for page-related operations only.
 *
 * Use this instead of {@link createDotCMSClient} when an entry point only fetches pages: the
 * root factory constructs the navigation, content-query and AI clients too, so importing it
 * keeps the Lucene/collection query builders and the AI search code in your bundle even if
 * you never call them.
 *
 * @param config - Configuration options for the client
 * @returns A page client bound to the given dotCMS instance
 * @throws {TypeError} when `dotcmsUrl` is not a valid URL or `authToken` is missing
 * @example
 * ```typescript
 * import { createDotCMSPageClient } from '@dotcms/client/page';
 *
 * const pageClient = createDotCMSPageClient({
 *   dotcmsUrl: 'https://demo.dotcms.com',
 *   authToken: 'your-auth-token'
 * });
 *
 * const page = await pageClient.get('/about-us');
 * ```
 */
export const createDotCMSPageClient = (config: DotCMSClientConfig): PageClient => {
    const { config: resolved, requestOptions, httpClient } = createClientContext(config);

    return new PageClient(resolved, requestOptions, httpClient);
};
