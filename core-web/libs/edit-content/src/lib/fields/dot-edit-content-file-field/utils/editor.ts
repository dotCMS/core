/**
 * Extracts the file extension from a given file name.
 *
 * @param fileName - The name of the file from which to extract the extension.
 * @returns The file extension if present, otherwise an empty string.
 */
export function extractFileExtension(fileName: string): string {
    const includesDot = fileName.includes('.');

    if (!includesDot) {
        return '';
    }

    return fileName.split('.').pop() || '';
}

/**
 * Retrieves language information based on the provided file extension.
 *
 * @param extension - The file extension to get the language information for.
 * @returns An object containing the language id, MIME type, and extension.
 *
 * @example
 * ```typescript
 * const info = getInfoByLang('vtl');
 * console.log(info);
 * // Output: { lang: 'html', mimeType: 'text/x-velocity', extension: '.vtl' }
 * ```
 *
 * @remarks
 * If the extension is 'vtl', it returns a predefined set of values.
 * Otherwise, it searches through the Monaco Editor languages to find a match.
 * If no match is found, it defaults to 'text' for the language id, 'text/plain' for the MIME type, and '.txt' for the extension.
 */
export function getInfoByLang(extension: string) {
    if (extension === 'vtl') {
        return {
            lang: 'html',
            mimeType: 'text/x-velocity',
            extension: '.vtl'
        };
    }

    // Monaco is loaded lazily; guard against it not being available yet.
    const language =
        typeof monaco !== 'undefined'
            ? monaco.languages
                  .getLanguages()
                  .find((lang) => lang.extensions?.includes(`.${extension}`))
            : undefined;

    return {
        lang: language?.id || 'text',
        mimeType: language?.mimetypes?.[0] || 'text/plain',
        extension: language?.extensions?.[0] || '.txt'
    };
}
