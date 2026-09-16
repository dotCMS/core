import { DotCMSClientConfig } from '@dotcms/types';

import { AIClient } from './lib/client/ai/ai-api';
import { createClientContext } from './lib/client/shared/client-context';

export type { AIClient };

/**
 * Creates a client for AI-related operations only.
 *
 * @experimental This client is experimental and may be subject to change.
 *
 * Importing this subpath keeps the page, navigation and content-query code out of the bundle.
 * It pairs well with a lazily-loaded AI feature: `const { createDotCMSAIClient } = await
 * import('@dotcms/client/ai')` inside the handler that opens the search dialog keeps AI code
 * off the initial route entirely.
 *
 * @param config - Configuration options for the client
 * @returns An AI client bound to the given dotCMS instance
 * @throws {TypeError} when `dotcmsUrl` is not a valid URL or `authToken` is missing
 * @example
 * ```typescript
 * import { createDotCMSAIClient } from '@dotcms/client/ai';
 *
 * const ai = createDotCMSAIClient({
 *   dotcmsUrl: 'https://demo.dotcms.com',
 *   authToken: 'your-auth-token'
 * });
 * ```
 */
export const createDotCMSAIClient = (config: DotCMSClientConfig): AIClient => {
    const { config: resolved, requestOptions, httpClient } = createClientContext(config);

    return new AIClient(resolved, requestOptions, httpClient);
};
