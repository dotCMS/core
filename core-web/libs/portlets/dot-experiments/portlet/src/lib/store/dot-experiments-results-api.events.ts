import { type } from '@ngrx/signals';
import { eventGroup } from '@ngrx/signals/events';

import { DotExperiment, DotExperimentResults } from '@dotcms/dotcms-models';

import { DotExperimentConfigurePage } from '../shared/models';

/** What the initial load answered with: the experiment always, its results only when it has any. */
export interface ResultsLoadPayload {
    experiment: DotExperiment;
    /** `null` for a DRAFT or SCHEDULED experiment, whose results are never fetched (AC10). */
    results: DotExperimentResults | null;
}

/**
 * What the backend answered on the Results screen: every event here is dispatched from a store
 * event handler once a request settles, never by a component. The matching intents belong to
 * `dotExperimentsResultsPageEvents`.
 *
 * The initial load settles as one `load…` pair rather than a pair per call: the experiment decides
 * whether its results are worth fetching at all, so the two travel as a single unit and the screen
 * has either both or neither.
 *
 * Every `…Succeeded` of a mutation carries the experiment the server answered with: it is the
 * source of truth after any write, and the shell needs its name for the toast copy.
 */
export const dotExperimentsResultsApiEvents = eventGroup({
    source: 'Experiments Results API',
    events: {
        loadSucceeded: type<ResultsLoadPayload>(),
        /**
         * The experiment itself could not be read, so there is nothing to frame a report with:
         * this is the one failure that blanks the screen into a full error state with a retry
         * (AC24).
         */
        loadFailed: type<unknown>(),
        /**
         * The experiment read fine but its report did not. Everything the experiment already
         * answers for — name, status, goal, schedule — is on screen, so the screen keeps its shape
         * and reports the missing report inline rather than replacing itself with an error card.
         *
         * This is the common case while experiment results still run through CubeJS: a schema
         * without the `Events` cube answers `getResults` with a 400 while `getById` succeeds. The
         * screen this one replaces degraded the same way, and blanking here would be a regression.
         */
        resultsUnavailable: type<DotExperiment>(),

        /**
         * The page the experiment runs on, once the content search resolved it. Ancillary: a page
         * that cannot be resolved settles as `null` rather than failing the screen, so it is a
         * success event carrying nothing rather than a failure of its own.
         */
        pageResolved: type<DotExperimentConfigurePage | null>(),

        stopSucceeded: type<DotExperiment>(),
        stopFailed: type<unknown>(),

        /**
         * Promoting a RUNNING experiment ends it server-side, so the experiment carried here
         * already reads ENDED — the header re-renders in place off this one event (AC20).
         */
        promoteSucceeded: type<DotExperiment>(),
        promoteFailed: type<unknown>()
    }
});
