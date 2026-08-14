import { type } from '@ngrx/signals';
import { eventGroup } from '@ngrx/signals/events';

import { Goals, RangeOfDateAndTime, TrafficProportion } from '@dotcms/dotcms-models';

import { DotExperimentConfigurePage } from '../shared/models';

/** What `?pageId=`/`?url=` asked the Page card to preselect. Either one may be present. */
export interface ConfigurePagePrefill {
    pageId?: string | null;
    url?: string | null;
}

/** A rename of a single variant, which has its own endpoint rather than a `trafficProportion` PATCH. */
export interface ConfigureVariantRename {
    variantId: string;
    name: string;
}

/**
 * What the Configure page asks for: user intent and lifecycle, never a result.
 *
 * Every event here is dispatched by the screen itself — a keystroke in a card, a page picked in
 * the dialog, a Start press, the shell coming up. What comes *back* lives in
 * `dotExperimentsConfigureApiEvents`, so the two halves of an async flow are never confused.
 *
 * The field-change events are deliberately one per PATCH body key: there is no combined update
 * endpoint, so a Name edit and a Goal edit in the same tick have to reach the server as two
 * independent calls. Each one is debounced on its own timer in the store.
 *
 * Confirmations and toasts are the shell's job — the store never opens UI.
 */
export const dotExperimentsConfigurePageEvents = eventGroup({
    source: 'Experiments Configure Page',
    events: {
        // Entry. `/experiments/new` starts empty; `/experiments/:id/configuration` loads.
        enterNew: type<void>(),
        enterExisting: type<string>(),

        // Page selection. `pageSelected` only applies before creation — the page is immutable
        // afterwards, since `PATCH /api/v1/experiments/{id}` does not accept `pageId`.
        pageSelected: type<DotExperimentConfigurePage>(),
        pagePrefillRequested: type<ConfigurePagePrefill>(),

        // Field groups, one per PATCH body key
        nameChanged: type<string>(),
        descriptionChanged: type<string>(),
        goalChanged: type<Goals | null>(),
        schedulingChanged: type<RangeOfDateAndTime | null>(),
        trafficAllocationChanged: type<number>(),
        trafficProportionChanged: type<TrafficProportion>(),

        // Variants: dedicated endpoints, not a `trafficProportion` PATCH
        variantAdded: type<string>(),
        variantRenamed: type<ConfigureVariantRename>(),
        variantDeleted: type<string>(),
        /** Re-splits the weights; persisted through the same debounced `trafficProportion` PATCH. */
        splitEvenly: type<void>(),

        // Transitions. Each is already confirmed in the shell where a confirmation applies.
        startRequested: type<void>(),
        stopRequested: type<void>(),
        cancelScheduleRequested: type<void>(),
        abortRequested: type<void>()
    }
});
