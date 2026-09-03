import { MenuItem } from 'primeng/api';

import { CONFIGURATION_SEGMENT, EXPERIMENTS_URL } from '../shared/constants';

/**
 * What every crumb `url` has to be prefixed with.
 *
 * The trail renders `url` straight into an `href`, and dotAdmin runs on `withHashLocation()` — so
 * a router-shaped address (`/experiments`) in that slot walks the browser out of the app. Every
 * existing crumb author writes this prefix by hand; naming it keeps the two builders below honest
 * about why it is there.
 */
const ADMIN_HASH_PREFIX = '/dotAdmin/#';

/**
 * Id of the list's crumb — stable across the filtered and unfiltered list on purpose.
 *
 * `addNewBreadcrumb` replaces the last crumb when the id matches and appends otherwise, so one id
 * is what makes re-entering the list (reload, clearing the page filter, returning from Configure)
 * land on the crumb that is already there instead of stacking another.
 */
const LIST_CRUMB_ID = 'experiments-list';

/**
 * The list's crumb, narrowed to a page when the editor arrived from UVE.
 *
 * The filter is kept in the address deliberately: the trail is a way back to where you were, and
 * the list you were on was the one page's, not the site's.
 *
 * @param label the list screen's own title, already translated
 * @param pageAssetId identifier the list is filtered by, or `null` when it is site-wide
 */
export function experimentsListCrumb(label: string, pageAssetId: string | null): MenuItem {
    const filter = pageAssetId ? `?pageAsset=${pageAssetId}` : '';

    return {
        id: LIST_CRUMB_ID,
        label,
        target: '_self',
        url: `${ADMIN_HASH_PREFIX}${EXPERIMENTS_URL}${filter}`
    };
}

/**
 * The Configure screen's crumb, named after the experiment rather than the screen.
 *
 * The screen's own title ("Experiments Configuration") would say the same thing on every
 * experiment; the trail's job at this depth is to say *which* one. Identified by the experiment's
 * id so a rename replaces the crumb instead of leaving the old name beside the new.
 */
export function experimentConfigureCrumb(label: string, experimentId: string): MenuItem {
    return {
        id: experimentId,
        label,
        target: '_self',
        url: `${ADMIN_HASH_PREFIX}${EXPERIMENTS_URL}/${experimentId}/${CONFIGURATION_SEGMENT}`
    };
}
