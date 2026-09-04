import { type } from '@ngrx/signals';
import { eventGroup } from '@ngrx/signals/events';

import { DotExperiment, DotExperimentStatus, GOAL_TYPES } from '@dotcms/dotcms-models';

import {
    DotExperimentsListPageChange,
    DotExperimentsListSortChange,
    DotExperimentsListViewState
} from '../shared/models';

/**
 * What the list page asks for: user intent and lifecycle, never a result.
 *
 * Every event here is dispatched by the page itself — a click, a keystroke, a URL change, the
 * component coming up. What comes *back* lives in `dotExperimentsApiEvents`, so the two halves
 * of an async flow are never confused for one another: the page states an intent
 * (`archiveExperiment`), the API reports the outcome (`archiveSucceeded` / `archiveFailed`).
 *
 * Hence the imperative names: these are commands, not results. CRUD commands carry the whole
 * `DotExperiment` (not just the id) because the component needs the name for its confirmation
 * and toast copy.
 *
 * Confirmations and toasts are the component's job — the store never opens UI.
 */
export const dotExperimentsListPageEvents = eventGroup({
    source: 'Experiments List Page',
    events: {
        // Analytics health gate: runs before anything is fetched, since a misconfigured
        // Analytics app makes the whole list meaningless.
        checkHealth: type<void>(),

        // Load
        loadExperiments: type<void>(),

        // View state (URL-backed)
        filterChanged: type<string>(),
        statusesChanged: type<DotExperimentStatus[]>(),
        goalsChanged: type<GOAL_TYPES[]>(),
        /**
         * Narrows the list to one page, or clears it with `null` (#37005).
         *
         * Named for the param it is serialised as (`pageAsset`) rather than "page", which in this
         * group already means the pagination cursor — see `pageChanged` below.
         */
        pageAssetFilterChanged: type<string | null>(),
        pageChanged: type<DotExperimentsListPageChange>(),
        sortChanged: type<DotExperimentsListSortChange>(),
        hydratedFromUrl: type<DotExperimentsListViewState>(),

        // Site
        siteChanged: type<string | null>(),

        // CRUD intent, already confirmed in the component
        archiveExperiment: type<DotExperiment>(),
        deleteExperiment: type<DotExperiment>(),
        endExperiment: type<DotExperiment>(),
        abortExperiment: type<DotExperiment>(),
        cancelScheduleExperiment: type<DotExperiment>()
    }
});
