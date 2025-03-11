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
 * @example
 * ```typescript
 * isValidPath('https://example.com') // returns true
 * isValidPath('invalid-url') // returns false
 * isValidPath('') // returns false
 * ```
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
 * @param path - The base URL path to the DotCMS instance
 * @returns An array of providers for the IMAGE_LOADER token
 * @throws Error if the provided path is invalid
 * @example
 * ```typescript
 * // In your app.config.ts
 * export const appConfig: ApplicationConfig = {
 *   providers: [
 *     provideDotCMSImageLoader('https://demo.dotcms.com')
 *   ]
 * };
 * ```
 */
export function provideDotCMSImageLoader(path: string): Provider[] {

  if (!path) {
    throw new Error('No path provided to DotCMS image loader. Supply either the full URL to the dotCMS site.');
  }

  if (!isValidPath(path)) {
    throw new Error(
      `Image loader has detected an invalid path (\`${path}\`). ` +
      `To fix this, supply either the full URL to the dotCMS site, or leave it empty to use the current site.`
    );
  }

  return [{
    provide: IMAGE_LOADER,
    useValue: (config: ImageLoaderConfig) => createDotCMSUrl(config, path)
  }];
}

/**
 * Creates a DotCMS-compatible URL for image loading
 *
 * @param config - The image loader configuration
 * @param path - The base URL path to the DotCMS instance
 * @returns A fully qualified URL for the image
 * @internal
 */
function createDotCMSUrl(config: ImageLoaderConfig, path: string): string {
  const { loaderParams, src, width } = config;
  const params = loaderParams as DotCMSImageLoaderParams;

  if (params?.isOutsideSRC) {
    return src;
  }

  const dotcmsHost = new URL(path).origin;
  const imageSRC = src.includes('/dA/') ? src : `/dA/${src}`;
  const languageId = params?.languageId ?? '1';

  return `${dotcmsHost}${imageSRC}/${width}?language_id=${languageId}`;
}
