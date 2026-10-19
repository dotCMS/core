import { type } from '@ngrx/signals';
import { eventGroup } from '@ngrx/signals/events';

import { DotExperiment, DotExperimentStatus, GOAL_TYPES } from '@dotcms/dotcms-models';

import {
    DotExperimentsListPageChange,
    DotExperimentsListPanelScope,
    DotExperimentsListSortChange,
    DotExperimentsListViewState,
    ExperimentsListSchedulePeriod
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
        /** Ids of the users whose experiments the list narrows to; empty clears the filter. */
        creatorsChanged: type<string[]>(),
        /**
         * Period the list narrows scheduled starts to. Both bounds `null` clears the constraint.
         *
         * One event carrying both bounds, not one per bound: the calendar hands over a range, and
         * two events would put a half-applied period on screen between them.
         */
        scheduleChanged: type<ExperimentsListSchedulePeriod>(),
        pageChanged: type<DotExperimentsListPageChange>(),
        sortChanged: type<DotExperimentsListSortChange>(),
        hydratedFromUrl: type<DotExperimentsListViewState>(),

        /**
         * Widens the list back out: the search term, the chips and the page narrowing all go at
         * once. The way out of the **dead end**, dispatched only from the no-results state.
         *
         * One event rather than one per control, because it is one intent and because the
         * narrowings have to go together. Dispatched separately they are four state transitions,
         * each recomputing the row set and re-deriving the address from a view state that is only
         * half cleared; a reader then has to work out for themselves that the intermediate states
         * are never observed.
         *
         * The page narrowing is included for the case this exists for. It otherwise has exactly
         * one writer, the address (`hydratedFromUrl`), and no control on the screen widens a page
         * filter that is *working* — the page-scoped empty state offers to create an experiment
         * for the page instead, which is the help that case wants. A narrowing that matched
         * nothing is the dead end, and clearing it is the only way out.
         */
        filtersCleared: type<void>(),

        /**
         * Widens everything the toolbar's own controls set, plus the page *path*, and nothing else.
         *
         * The filter bar's **Clear all** rather than the empty state's button, and the difference
         * is not cosmetic: that button is on screen while the list is *working*, so sharing
         * `filtersCleared` with it made it drop the page scope the editor handed down.
         *
         * The line between the two page fields is which one the app writes. `pageId` is the scope
         * this screen hands out on all four ways out (`pageFilterParams`) and reads back on the
         * way in (`listReturnParams`), so it survives. `?url=` has no writer anywhere in the app —
         * it arrives typed or pasted and is echoed back on every change — so it is a filter the
         * user applied, and it goes. Keeping it would leave it unremovable while it still matched
         * rows, since the empty state's button appears only once it matches nothing.
         *
         * The search term goes with the chips. `$hasNonDefaultFilters` ignores it — a term alone
         * never reveals the button — but once the button is there, "clear all" that leaves the
         * search box full does not mean what it says.
         */
        chipFiltersCleared: type<void>(),

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
