import { IMAGE_LOADER, ImageLoaderConfig } from '@angular/common';
import { Provider } from '@angular/core';

/**
 * Type definition for the DotCMS image loader parameters
 */
interface DotCMSImageLoaderParams {
    isOutsideSRC?: boolean;
    languageId?: string;
}

/**
 * Validates if a given path is a valid URL string
 *
 * @param path - The path to validate
 * @returns boolean indicating if the path is valid
 */
function isValidPath(path: unknown): boolean {
    if (typeof path !== 'string' || path.trim() === '') {
        return false;
    }

    try {
        new URL(path);

        return true;
    } catch {
        return false;
    }
}

/**
 * Provides a DotCMS image loader configuration for the Angular Image directive
 *
 * @param path - The base URL path to the DotCMS instance, or empty to use current site
 * @returns An array of providers for the IMAGE_LOADER token
 * @throws Error if the provided path is invalid
 * @example
 * ```typescript
 * // In your app.config.ts
 * export const appConfig: ApplicationConfig = {
 *   providers: [
 *     provideDotCMSImageLoader('https://demo.dotcms.com')
 *     // Or use current site:
 *     // provideDotCMSImageLoader()
 *   ]
 * };
 * ```
 */
export function provideDotCMSImageLoader(path?: string): Provider[] {
    // If path is provided, validate it
    if (path && !isValidPath(path)) {
        throw new Error(
            `Image loader has detected an invalid path (\`${path}\`). ` +
                `To fix this, supply either the full URL to the dotCMS site, or leave it empty to use the current site.`
        );
    }

    return [
        {
            provide: IMAGE_LOADER,
            useValue: (config: ImageLoaderConfig) => createDotCMSURL(config, path)
        }
    ];
}

/**
 * Creates a DotCMS-compatible URL for image loading
 *
 * @param config - The image loader configuration
 * @param path - The base URL path to the DotCMS instance
 * @returns A fully qualified URL for the image
 * @internal
 */
function createDotCMSURL(config: ImageLoaderConfig, path?: string): string {
    const { loaderParams, src, width } = config;
    const params = loaderParams as DotCMSImageLoaderParams;

    if (params?.isOutsideSRC) {
        return src;
    }

    // Use empty string as fallback to support using current site
    const dotcmsHost = path ? new URL(path).origin : '';
    const imageSRC = src.includes('/dA/') ? src : `/dA/${src}`;
    const languageId = params?.languageId ?? '1';

    if (width) {
        return `${dotcmsHost}${imageSRC}/${width}w?language_id=${languageId}`;
    }

    return `${dotcmsHost}${imageSRC}?language_id=${languageId}`;
}
