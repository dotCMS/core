import { DotPageRenderParameters } from './models/dot-page/dot-rendered-page.model';

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
export function generateDotFavoritePageUrl(params: DotPageRenderParameters): string {
    const { page, site, viewAs } = params;

    return (
        `${page?.pageURI}?` +
        (site?.identifier ? `host_id=${site?.identifier}` : '') +
        `&language_id=${viewAs.language.id}` +
        (viewAs.device?.inode ? `&device_inode=${viewAs.device?.inode}` : '&device_inode=')
    );
}
