/**
 * Options for a single {@link DotCMSImageLoader} call.
 */
export interface DotCMSImageLoaderOptions {
    /** Target width in pixels. Requests a resized image from the dotCMS image API. */
    width?: number;
    /**
     * JPEG quality (1-100).
     * @default 50
     */
    quality?: number;
    /**
     * Language id for the asset.
     * @default '1'
     */
    languageId?: string;
}

/**
 * A function that turns a dotCMS asset identifier (or path) into an image URL.
 * Returned by {@link createDotCMSImageLoader}.
 */
export type DotCMSImageLoader = (src: string, options?: DotCMSImageLoaderOptions) => string;

/** Matches a fully qualified URL (external/stock imagery we should not rewrite). */
const ABSOLUTE_URL = /^https?:\/\//;

/**
 * Creates a dotCMS image URL builder.
 *
 * dotCMS serves images through its image API (the `/dA/` route), which handles
 * on-the-fly resizing and optimization. This is the Vue analog of Angular's
 * `provideDotCMSImageLoader`: Angular wires the same URL math into its
 * `IMAGE_LOADER` token, while Vue has no such token, so this returns a plain
 * function you call from a template.
 *
 * Absolute URLs (already `http(s)://…`) are returned unchanged, so mixing dotCMS
 * assets with external/stock imagery just works.
 *
 * @param dotcmsUrl - Base URL of the dotCMS instance. When omitted (empty), URLs
 *   are site-relative (`/dA/…`), which is what you want behind a dev proxy.
 * @returns a loader function `(src, options?) => url`
 * @throws if `dotcmsUrl` is a non-empty, invalid URL
 *
 * @example
 * ```ts
 * import { createDotCMSImageLoader } from '@dotcms/vue';
 *
 * // Absolute (production): points at the dotCMS host.
 * const image = createDotCMSImageLoader(import.meta.env.VITE_DOTCMS_HOST);
 * image(contentlet.inode, { width: 800 });
 * // → https://demo.dotcms.com/dA/<inode>/800w/50q?language_id=1
 *
 * // Relative (dev proxy): omit the host so the Vite proxy handles /dA.
 * const proxied = createDotCMSImageLoader();
 * proxied(contentlet.inode, { width: 800 }); // → /dA/<inode>/800w/50q?language_id=1
 * ```
 */
export function createDotCMSImageLoader(dotcmsUrl = ''): DotCMSImageLoader {
    // Fail loudly on a malformed host rather than silently producing broken URLs.
    const origin = dotcmsUrl ? new URL(dotcmsUrl).origin : '';

    return (src, options = {}) => {
        if (ABSOLUTE_URL.test(src)) {
            return src;
        }

        const { width, quality = 50, languageId = '1' } = options;
        const imageSrc = src.includes('/dA/') ? src : `/dA/${src}`;
        const size = width ? `/${width}w` : '';

        return `${origin}${imageSrc}${size}/${quality}q?language_id=${languageId}`;
    };
}
