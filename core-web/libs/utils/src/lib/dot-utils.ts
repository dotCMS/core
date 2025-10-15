import {
    DotCMSBaseTypesContentTypes,
    DotCMSContentlet,
    DotPageToolUrlParams
} from '@dotcms/dotcms-models';

/**
 * Generate an anchor element with a Blob file to eventually be click to force a download
 * This approach is needed because FF do not hear WS events while waiting for a request.
 */
export function getDownloadLink(blob: Blob, fileName: string): HTMLAnchorElement {
    const anchor = document.createElement('a');
    anchor.download = fileName;
    anchor.href = window.URL.createObjectURL(blob);

    return anchor;
}

// Replace {n} in the string with the strings in the args array
export function formatMessage(message: string, args: string[]): string {
    return message.replace(/{(\d+)}/g, (match, number) => {
        return typeof args[number] !== 'undefined' ? args[number] : match;
    });
}

// Generates an unique Url with host, language and device Ids
export function generateDotFavoritePageUrl(params: {
    languageId: number;
    pageURI: string;
    deviceInode?: string;
    siteId?: string;
}): string {
    const { deviceInode, languageId, pageURI, siteId } = params;

    return (
        `${pageURI}?` +
        (siteId ? `host_id=${siteId}` : '') +
        `&language_id=${languageId}` +
        (deviceInode ? `&device_inode=${deviceInode}` : '&device_inode=')
    );
}

/**
 * Generates a runnable link by replacing placeholders in the base URL and appending query parameters.
 *
 * This function takes a base URL that may contain placeholders for the request host name and current URL,
 * and it replaces these placeholders with actual values. Additionally, it appends optional query parameters
 * based on the properties in the `currentPageUrlParams` object.
 *
 * @param url - The base URL for the tool, which may contain placeholders such as `{requestHostName}` and `{currentUrl}`.
 * @param currentPageUrlParams - An object containing values to replace in the base URL and to add as query parameters:
 *   - `currentUrl` (optional): The URL to replace the `{currentUrl}` placeholder in the base URL.
 *   - `requestHostName` (optional): The host name to replace the `{requestHostName}` placeholder in the base URL.
 *   - `siteId` (optional): The site ID to include as a query parameter (`host_id`).
 *   - `languageId` (optional): The language ID to include as a query parameter (`language_id`).
 *
 * @returns A string representing the constructed URL with placeholders replaced and query parameters appended.
 */
export function getRunnableLink(url: string, currentPageUrlParams: DotPageToolUrlParams): string {
    // If URL is empty, return an empty string
    if (!url) return '';
    // Destructure properties from currentPageUrlParams
    const { currentUrl, requestHostName, siteId, languageId } = currentPageUrlParams;

    // Create a URLSearchParams object to manage query parameters
    const pageParams = new URLSearchParams();

    // Append site ID and language ID as query parameters if they are provided
    if (siteId) pageParams.append('host_id', siteId);
    if (languageId) pageParams.append('language_id', String(languageId));

    // Replace placeholders in the base URL with actual values and append query parameters if they exist
    const requestHostUrl = requestHostName ? new URL(requestHostName) : null;
    const finalUrl = url
        .replace(/{requestHostName}/g, requestHostUrl ? requestHostUrl.origin : '')
        .replace(/{domainName}/g, requestHostUrl ? requestHostUrl.hostname : '')
        .replace(/{currentUrl}/g, currentUrl ?? '')
        .replace(/{urlSearchParams}/g, pageParams.toString() ? `?${pageParams.toString()}` : '');

    // Create a URL object from the finalUrl and return its fully qualified and normalized form.
    return new URL(finalUrl).toString();
}

/**
 * Get the image asset URL
 *
 * @export
 * @param {DotCMSContentlet} asset
 * @return {*}  {string}
 */
export function getImageAssetUrl(contentlet: DotCMSContentlet): string {
    if (!contentlet?.baseType) {
        return contentlet.asset;
    }

    switch (contentlet?.baseType) {
        case DotCMSBaseTypesContentTypes.FILEASSET:
            return contentlet.fileAssetVersion || contentlet.fileAsset;

        case DotCMSBaseTypesContentTypes.DOTASSET:
            return contentlet.assetVersion || contentlet.asset;

        default:
            return contentlet?.asset || '';
    }
}

/**
 * This method is used to truncate a text with ellipsis.
 * It ensures that the text is truncated at the nearest word boundary.
 * @param text - The text to be truncated.
 * @param limit - The maximum length of the truncated text.
 * @returns The truncated text with ellipsis if it exceeds the limit, otherwise the original text.
 */
export function ellipsizeText(text: string, limit: number): string {
    if (!text || typeof text !== 'string' || limit <= 0 || isNaN(limit)) {
        return '';
    }

    if (text.length <= limit) {
        return text;
    }

    const truncated = text.slice(0, limit);

    return truncated.slice(0, truncated.lastIndexOf(' ')) + '...';
}

/**
 * Checks if a string value is meaningful (not empty string, null, or undefined)
 * @param value The value to check
 * @returns {boolean} True if the value is meaningful, false otherwise
 */
export function hasValidValue(value: string | undefined | null): value is string {
    return value !== null && value !== undefined && value.trim() !== '';
}
