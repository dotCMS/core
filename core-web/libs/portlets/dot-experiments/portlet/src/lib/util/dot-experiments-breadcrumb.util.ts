import { Params } from '@angular/router';

import { MenuItem } from 'primeng/api';

import {
    CONFIGURATION_SEGMENT,
    EXPERIMENTS_URL,
    NEW_EXPERIMENT_SEGMENT,
    RESULTS_SEGMENT
} from '../shared/constants';

/**
 * What every crumb `url` has to be prefixed with.
 *
 * The trail renders `url` straight into an `href`, and dotAdmin runs on `withHashLocation()` — so
 * a router-shaped address (`/experiments`) in that slot walks the browser out of the app. Every
 * existing crumb author writes this prefix by hand; naming it keeps the builders below honest
 * about why it is there.
 */
const ADMIN_HASH_PREFIX = '/dotAdmin/#';

/**
 * Id of the list's crumb — stable across the filtered and unfiltered list on purpose.
 *
 * One id is what makes re-entering the list (a reload, clearing the page filter, returning from
 * Configure) land on the crumb that is already there instead of stacking another — see
 * {@link putCrumbOnTrail}, which is what actually does the landing.
 *
 * Exported because the deeper screens have to ask whether the list is already on the trail before
 * putting themselves on top of it.
 */
export const EXPERIMENTS_LIST_CRUMB_ID = 'experiments-list';

/**
 * Id of the Configure screen's crumb — one id for the whole screen, the unsaved draft included.
 *
 * Deliberately *not* the experiment's id. Creating the draft swaps `/experiments/new` for the
 * experiment's own address without leaving the screen, and a rename rewrites the label on every
 * keystroke; with the experiment as the id, the first of those appends a second crumb beside the
 * first ("New Experiment › My Experiment") instead of replacing it.
 */
const CONFIGURE_CRUMB_ID = 'experiments-configure';

/** Id of the Results screen's crumb, stable for the same reason. */
const RESULTS_CRUMB_ID = 'experiments-results';

/**
 * The trail, as the screens here use it.
 *
 * Declared structurally rather than as `GlobalStore` so this file stays a pure builder with no
 * dependency on the store — the real store satisfies it, and so does a plain object in a test.
 */
interface BreadcrumbTrail {
    lastBreadcrumb: () => MenuItem | null | undefined;
    addNewBreadcrumb: (crumb: MenuItem) => void;
    setLastBreadcrumb: (crumb: MenuItem) => void;
}

/**
 * Puts one of this portlet's crumbs on the trail, in place when it is already the last one.
 *
 * `addNewBreadcrumb` **skips** an item whose id or url matches the last crumb's — it does not
 * replace it, whatever its name suggests. Every crumb here is written more than once (a rename
 * rewrites the label on each keystroke; creating a draft swaps `/experiments/new` for the
 * experiment's own address; clearing the page filter widens the list's), so relying on it left the
 * trail showing the first version forever: the label the screen opened with, and a link back to an
 * address that no longer exists.
 *
 * Appending is still right when the last crumb is somebody else's — that one is the level above.
 *
 * @param trail the breadcrumb trail
 * @param crumb one of the crumbs built below, carrying the stable id its screen owns
 */
export function putCrumbOnTrail(trail: BreadcrumbTrail, crumb: MenuItem): void {
    if (trail.lastBreadcrumb()?.id === crumb.id) {
        trail.setLastBreadcrumb(crumb);

        return;
    }

    trail.addNewBreadcrumb(crumb);
}

/**
 * `?a=1&b=2`, or the empty string — the crumb `url` is a plain string, not a router command.
 *
 * Values are stringified rather than asserted to be strings: `Params` is `{[key: string]: any}`
 * and the page narrowing carries `language_id` as a number, so telling the compiler otherwise
 * would have been a lie that `URLSearchParams` happens to cover for.
 */
function queryOf(params: Params): string {
    const query = new URLSearchParams(
        Object.entries(params).map(([key, value]) => [key, String(value)])
    ).toString();

    return query ? `?${query}` : '';
}

/**
 * The list's crumb, narrowed to a page when the editor arrived from UVE.
 *
 * The narrowing is kept in the address deliberately: the trail is a way back to where you were,
 * and the list you were on was the one page's, not the site's.
 *
 * @param label the list screen's own title, already translated
 * @param pageFilter the page narrowing as an address, empty when the list is site-wide
 */
export function experimentsListCrumb(label: string, pageFilter: Params): MenuItem {
    return {
        id: EXPERIMENTS_LIST_CRUMB_ID,
        label,
        target: '_self',
        url: `${ADMIN_HASH_PREFIX}${EXPERIMENTS_URL}${queryOf(pageFilter)}`
    };
}

/**
 * The Configure screen's crumb, named after the experiment rather than the screen.
 *
 * The screen's own title ("Experiments Configuration") would say the same thing on every
 * experiment; the trail's job at this depth is to say *which* one — and, on a draft that has no
 * name yet, that this is a new one.
 *
 * The narrowing travels in the address for the same reason the back button carries it: the crumb
 * is a way back *to this screen*, and a return that dropped the filter would leave the screen's
 * own back button pointing at the site-wide list.
 *
 * @param label the experiment's name, or the new-experiment title while it has none
 * @param experimentId `null` while the draft has not been created yet
 * @param pageFilter the page narrowing the screen was opened with, empty when there was none
 */
export function experimentConfigureCrumb(
    label: string,
    experimentId: string | null,
    pageFilter: Params
): MenuItem {
    const path = experimentId
        ? `${EXPERIMENTS_URL}/${experimentId}/${CONFIGURATION_SEGMENT}`
        : `${EXPERIMENTS_URL}/${NEW_EXPERIMENT_SEGMENT}`;

    return {
        id: CONFIGURE_CRUMB_ID,
        label,
        target: '_self',
        url: `${ADMIN_HASH_PREFIX}${path}${queryOf(pageFilter)}`
    };
}

/** The Results screen's crumb — the same reasoning as Configure, one screen deeper. */
export function experimentResultsCrumb(
    label: string,
    experimentId: string,
    pageFilter: Params
): MenuItem {
    return {
        id: RESULTS_CRUMB_ID,
        label,
        target: '_self',
        url: `${ADMIN_HASH_PREFIX}${EXPERIMENTS_URL}/${experimentId}/${RESULTS_SEGMENT}${queryOf(
            pageFilter
        )}`
    };
}
