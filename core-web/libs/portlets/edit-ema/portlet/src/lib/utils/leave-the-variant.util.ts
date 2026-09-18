import { EXPERIMENT_RETURN_PARAM } from '@dotcms/dotcms-models';
import { UVE_MODE } from '@dotcms/types';

import { getIsDefaultVariant } from './index';

import { UVEStore } from '../store/dot-uve.store';

/**
 * What says the editor is inside an experiment — and nothing about what is on the canvas.
 *
 * These two are why the banner is there: `experimentId` names the experiment being previewed, and
 * the origin marker answers "where did this round trip start". Once the editor is back, neither
 * describes anything, so both go. Clearing them changes no rendered pixel, which is what lets the
 * control's return skip the page load entirely.
 */
export const CLEARED_EXPERIMENT_PARAMS = {
    experimentId: null,
    [EXPERIMENT_RETURN_PARAM]: null
} as const;

/**
 * Cleared on every way back, so no variant or experiment survives the return (FR-006).
 *
 * **`mode` is named, not nulled.** A null here used to mean "drop it from the address and let the
 * default put it back", which held while this was the `queryParams` of a `router.navigate`: the
 * shell re-read the route, found no mode and applied `UVE_MODE.EDIT`. It is handed to `pageLoad`
 * now, which writes straight into `pageParams` and is mirrored to the address with `Location.go` —
 * a write the router never hears, so `#getPageParams` never runs and the default never lands. The
 * null survived as a null: the mode selector had no option to select and the address lost `mode=`.
 *
 * `EDIT` is not a new choice. It is exactly where a return has always landed.
 */
export const CLEARED_VARIANT_PARAMS = {
    ...CLEARED_EXPERIMENT_PARAMS,
    mode: UVE_MODE.EDIT,
    variantName: null
} as const;

/**
 * Takes the editor off the variant — and only when they are actually on one.
 *
 * Shared by the two doors out of an experiment, because they have to agree: the banner's back
 * arrow, and the editor's own Experiments entry point. Either one ends with the page on the canvas
 * and nothing left on the address claiming otherwise.
 *
 * **`pageLoad`, not `router.navigate`, and that is the difference between a page changing and the
 * editor blinking.** `/edit-page` declares `data: { reuseRoute: false }` and route data is
 * inherited, so the custom reuse strategy tears down and rebuilds the whole subtree on *any* router
 * navigation beneath it — even one that only touches query params. So UVE never routes to change
 * what it is showing: it loads, and the shell mirrors the new params into the address through
 * `Location.go`.
 *
 * The load-free shortcut applies in one shape only: the editor is on the control, which is the page
 * itself, *and* already in the mode they are going back to. A mode change is not a param change —
 * `PREVIEW` and `EDIT` render different canvases, so patching the param without loading would leave
 * the chrome claiming one mode over an iframe still showing the other.
 */
export function leaveTheVariant(store: InstanceType<typeof UVEStore>): void {
    const params = store.pageParams();

    if (getIsDefaultVariant(params?.variantName) && params?.mode === UVE_MODE.EDIT) {
        store['pageUpdateParams'](CLEARED_EXPERIMENT_PARAMS);

        return;
    }

    store['pageLoad'](CLEARED_VARIANT_PARAMS);
}
