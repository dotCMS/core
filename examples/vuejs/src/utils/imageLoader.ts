import { createDotCMSImageLoader } from '@dotcms/vue';

import { dotCMSHost } from '@/config/dotcms.config';

/**
 * Builds a dotCMS image URL via the SDK's image loader. dotCMS serves images
 * through its image API (the `/dA/` route), which handles resizing/optimization;
 * absolute URLs (external/stock imagery) are returned unchanged.
 *
 * Falls back to localhost so the example still builds without a configured host.
 */
const loader = createDotCMSImageLoader(dotCMSHost || 'http://localhost:8080');

export function imageLoader(src: string, width = 1000): string {
    return loader(src, { width });
}
