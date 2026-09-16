import { DotCMSClientConfig } from '@dotcms/types';

import { Content } from './lib/client/content/content-api';
import { createClientContext } from './lib/client/shared/client-context';

export type { Content };

/**
 * Creates a client for content-related operations only.
 *
 * Includes the collection, query and raw-query builders, but leaves out the page, navigation
 * and AI clients that {@link createDotCMSClient} would also construct.
 *
 * @param config - Configuration options for the client
 * @returns A content client bound to the given dotCMS instance
 * @throws {TypeError} when `dotcmsUrl` is not a valid URL or `authToken` is missing
 * @example
 * ```typescript
 * import { createDotCMSContentClient } from '@dotcms/client/content';
 *
 * const content = createDotCMSContentClient({
 *   dotcmsUrl: 'https://demo.dotcms.com',
 *   authToken: 'your-auth-token'
 * });
 *
 * const blogs = await content.getCollection('Blog').limit(10);
 * ```
 */
export const createDotCMSContentClient = (config: DotCMSClientConfig): Content => {
    const { config: resolved, requestOptions, httpClient } = createClientContext(config);

    return new Content(resolved, requestOptions, httpClient);
};
