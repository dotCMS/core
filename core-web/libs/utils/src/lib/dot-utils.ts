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
 * Get the query parameter separator
 * @param url
 * @returns
 */
export function getQueryParameterSeparator(url: string): string {
    const regex = /[?&]/;

    return url.match(regex) ? '&' : '?';
}

/**
 * This method is used to get the runnable link for the tool
 * @param url
 * @returns
 */
export function getRunnableLink(url: string, currentPageUrlParams: DotPageToolUrlParams): string {
    const { currentUrl, requestHostName, siteId, languageId } = currentPageUrlParams;

    url = url
        .replace('{requestHostName}', requestHostName ?? '')
        .replace('{currentUrl}', currentUrl ?? '')
        .replace('{siteId}', siteId ? `${getQueryParameterSeparator(url)}host_id=${siteId}` : '')
        .replace(
            '{languageId}',
            languageId ? `${getQueryParameterSeparator(url)}language_id=${String(languageId)}` : ''
        );

    return url;
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
