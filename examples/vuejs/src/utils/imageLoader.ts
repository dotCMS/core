import { dotCMSHost } from '@/config/dotcms.config';

/**
 * Builds a dotCMS image URL. dotCMS serves images through its image API (`/dA/`
 * route) which handles resizing/optimization; a `{width}w` suffix requests the
 * right size. Absolute URLs (external/stock imagery) are returned as-is.
 */
export function imageLoader(src: string, width = 1000): string {
    if (/^https?:\/\//.test(src)) {
        return src;
    }

    const dotcmsURL = new URL(dotCMSHost || 'http://localhost:8080').origin;
    const imageSRC = src.includes('/dA/') ? src : `/dA/${src}`;

    return `${dotcmsURL}${imageSRC}/${width}w`;
}
