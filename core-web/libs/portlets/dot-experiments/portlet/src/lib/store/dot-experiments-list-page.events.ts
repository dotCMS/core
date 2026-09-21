import { type } from '@ngrx/signals';
import { eventGroup } from '@ngrx/signals/events';

import { DotExperiment, DotExperimentStatus, GOAL_TYPES } from '@dotcms/dotcms-models';

import {
    DotExperimentsListPageChange,
    DotExperimentsListPanelScope,
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
        pageChanged: type<DotExperimentsListPageChange>(),
        sortChanged: type<DotExperimentsListSortChange>(),
        hydratedFromUrl: type<DotExperimentsListViewState>(),

        /**
         * Drops the page narrowing, whichever way it arrived — by identifier or by path.
         *
         * The narrowing otherwise has exactly one writer, the address (`hydratedFromUrl`), and no
         * control on the screen widens a page filter that is *working*: the page-scoped empty state
         * offers to create an experiment for the page instead, which is the help that case wants.
         * This exists for the other case — a narrowing that matched nothing, where the list is a
         * dead end and clearing it is the only way out. The empty state's own button is the only
         * caller.
         */
        pageNarrowingCleared: type<void>(),

        /**
         * The panel is now about this page (#37478).
         *
         * Dispatched on open and again whenever the editor navigates to another page. It carries
         * the language only as the return context the variant round trip needs — the language
         * never narrows the list, because an experiment belongs to a page and not to a language
         * version of one (FR-034, FR-034a, D10).
         *
         * Distinct from `hydratedFromUrl` on purpose even though both replace the whole view
         * state: that one means "the address said so" and is the portlet's only writer, and
         * collapsing them would put the panel's re-scope on the address-backed path this work
         * exists to keep it off (FR-031).
         */
        scopedToPage: type<DotExperimentsListPanelScope>(),

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
