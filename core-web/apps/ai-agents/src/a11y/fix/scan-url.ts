import type { FixRequest } from '../domain/contract';

/** Build the two scan URLs from the resolved page (plan §8.2 string assembly). */
export function scanUrls(req: FixRequest): { live: string; preview: string } {
    const { dotcmsBaseUrl, page } = req;
    const base = `${dotcmsBaseUrl}${page.uri}?host_id=${page.hostId}`;
    const live = base;
    // PREVIEW_MODE renders the working/draft content WITHOUT editor chrome (unlike
    // EDIT_MODE, which injects edit buttons + extra container divs axe over-flags).
    let preview = `${base}&mode=PREVIEW_MODE`;
    // language_id only needed for multilingual pages (S0 finding (c)).
    if (page.languageId && page.languageId !== 1) {
        preview += `&language_id=${page.languageId}`;
    }
    return { live, preview };
}
