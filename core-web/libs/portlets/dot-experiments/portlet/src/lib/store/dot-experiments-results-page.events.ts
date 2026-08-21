import { type } from '@ngrx/signals';
import { eventGroup } from '@ngrx/signals/events';

/**
 * What the Results page asks for: user intent and lifecycle, never a result.
 *
 * Every event here is dispatched by the screen itself — the shell coming up on a URL, the refresh
 * control, a confirmed Stop or Promote. What comes *back* lives in
 * `dotExperimentsResultsApiEvents`, so the two halves of an async flow are never confused.
 *
 * Both mutations are already confirmed by the time they are dispatched: the store never opens UI,
 * so the confirm dialogs and the toasts that follow belong to the shell.
 */
export const dotExperimentsResultsPageEvents = eventGroup({
    source: 'Experiments Results Page',
    events: {
        /** The screen came up on `/experiments/:experimentId/results`, carrying that id. */
        enter: type<string>(),

        /**
         * The refresh control was pressed. Only the results are re-fetched — the experiment itself
         * cannot change while the screen sits on it — and the ones on screen stay put until the
         * new ones arrive (AC9).
         */
        refreshRequested: type<void>(),

        /** A confirmed Stop. Only reachable while the experiment is RUNNING (AC3). */
        stopRequested: type<void>(),

        /**
         * A confirmed Promote, carrying the variant id. Promoting a RUNNING experiment also ends
         * it, which the backend does in the same call — so this is one event, not two (AC20).
         */
        promoteRequested: type<string>()
    }
});
