import { VanityUrl } from '@dotcms/dotcms-models';

/**
 * Check if the param is a forward or page
 *
 * @export
 * @param {VanityUrl} vanityUrl
 * @return {*}
 */
export function isForwardOrPage(vanityUrl?: VanityUrl): boolean {
    return !vanityUrl || (!vanityUrl.permanentRedirect && !vanityUrl.temporaryRedirect);
}
