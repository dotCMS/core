import { mapResponse } from '@ngrx/operators';
import { signalStore, withComputed, withHooks, withState } from '@ngrx/signals';
import { Dispatcher, Events, on, withEventHandlers, withReducer } from '@ngrx/signals/events';
import { ChartData } from 'chart.js';
import { of, SubscriptionLike } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { computed, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { distinctUntilChanged, filter, map, mergeMap, switchMap } from 'rxjs/operators';

import {
    DotExperimentsService,
    DotHttpErrorManagerService,
    DotMessageService
} from '@dotcms/data-access';
import {
    BayesianNoWinnerStatus,
    BayesianStatusResponse,
    ComponentStatus,
    DEFAULT_VARIANT_ID,
    DotExperiment,
    DotExperimentStatus,
    DotResultVariant,
    MINIMUM_SESSIONS_TO_SHOW_CHART,
    ReportSummaryLegendByBayesianStatus,
    SummaryLegend,
    Variant
} from '@dotcms/dotcms-models';

import { dotExperimentsResultsApiEvents } from './dot-experiments-results-api.events';
import { dotExperimentsResultsPageEvents } from './dot-experiments-results-page.events';

import {
    buildDailyChartData,
    buildDailyChartLabels,
    getBayesianDatasets,
    getSuggestedWinner
} from '../shared/dot-experiment-results.utils';
import { DotExperimentResultVariantDetail, DotExperimentsResultsViewState } from '../shared/models';
import { buildVariantDetails } from '../util/dot-experiments-results.util';

const pageEvents = dotExperimentsResultsPageEvents;
const apiEvents = dotExperimentsResultsApiEvents;

/**
 * Statuses with nothing to report yet. They never reach `getResults`: the endpoint is uncached and
 * costs two analytics round-trips plus a Monte Carlo run, so it is not called before a single
 * session has been recorded (AC10).
 */
const STATUSES_WITHOUT_RESULTS: readonly DotExperimentStatus[] = [
    DotExperimentStatus.DRAFT,
    DotExperimentStatus.SCHEDULED
];

/** Header the old screen supplies when a rejected results call carries none of its own. */
const RESULTS_ERROR_HEADER_KEY =
    'dot.common.http.error.400.experiment.analytics-app-not-configured.header';

/** Copy for a value the backend has not computed yet, e.g. a range without enough data. */
const NO_DATA_LABEL_KEY = 'experiments.reports.not.enough.data';

/** Word between the two bounds of the 95% conversion rate range. */
const RANGE_SEPARATOR_LABEL_KEY = 'to';

const initialState: DotExperimentsResultsViewState = {
    experiment: null,
    results: null,
    status: ComponentStatus.INIT,
    refreshing: false,
    lastRefreshFailed: false
};

/**
 * Store for the Results screen, at `/experiments/:experimentId/results`.
 *
 * The screen is reachable on any status (AC1), and the experiment itself decides how much of it
 * there is to load: DRAFT and SCHEDULED render their waiting state from the experiment alone and
 * never ask for results, while everything else loads the report beside it.
 *
 * Results that have loaded once are never taken off the screen. A failed *first* load is the full
 * error state (AC24); a failed *refresh* leaves the last good results exactly where they are and
 * only raises `lastRefreshFailed`, which the shell reports without blanking anything (AC25).
 *
 * The leading variant is always the backend's `bayesianResult.suggestedWinner`, never the highest
 * conversion rate: only the backend applies a significance threshold, and a rate-based pick would
 * name a winner even when there is none to name (AC8).
 *
 * State only ever changes through dispatched events (`withReducer`); the store exposes no mutating
 * methods and never opens UI — the Stop and Promote confirmations, and the toasts that follow,
 * belong to the shell.
 *
 * Not provided in root: supply it in the Results shell's `providers` together with
 * `DotExperimentsService`.
 */
export const DotExperimentsResultsStore = signalStore(
    withState<DotExperimentsResultsViewState>(initialState),
    withComputed((store) => {
        const dotMessageService = inject(DotMessageService);

        const $status = computed<DotExperimentStatus>(
            () => store.experiment()?.status ?? DotExperimentStatus.DRAFT
        );

        /** Nothing has been measured yet, so there is nothing to fetch or to chart (AC10/AC13). */
        const $isWaitingForData = computed<boolean>(() =>
            STATUSES_WITHOUT_RESULTS.includes($status())
        );

        /**
         * The threshold below which a report says more than it knows. It gates the daily chart —
         * as it always has — and, since AC15, the summary table as a whole.
         */
        const $hasEnoughSessions = computed<boolean>(() => {
            const results = store.results();

            return !!results && results.sessions.total >= MINIMUM_SESSIONS_TO_SHOW_CHART;
        });

        /** The variant the backend suggests, or `null` when it suggests none (AC8). */
        const $suggestedWinner = computed<DotResultVariant | null>(() => {
            const results = store.results();
            const suggestedWinner = results?.bayesianResult?.suggestedWinner;

            if (!results || !suggestedWinner || BayesianNoWinnerStatus.includes(suggestedWinner)) {
                return null;
            }

            return results.goals.primary.variants[suggestedWinner] ?? null;
        });

        const $bayesianChartData = computed<ChartData<'line'> | null>(() => {
            const results = store.results();

            return results ? { datasets: getBayesianDatasets(results) } : null;
        });

        return {
            $status,
            $isWaitingForData,
            $suggestedWinner,
            $bayesianChartData,
            $isLoading: computed<boolean>(() => store.status() === ComponentStatus.LOADING),
            /** Nothing loaded and the load failed: the only state that blanks the screen (AC24). */
            $hasLoadError: computed<boolean>(() => store.status() === ComponentStatus.ERROR),
            /** A mutation is on the wire, so its buttons stay closed until it settles. */
            $isSaving: computed<boolean>(() => store.status() === ComponentStatus.SAVING),
            /** The refresh control only exists once there is something to refresh (AC9/AC10). */
            $canRefresh: computed<boolean>(() => !$isWaitingForData() && !!store.results()),
            /**
             * Which winner copy the stat strip renders, negative states included — icon and i18n
             * key both, so `null` never has to be translated into an absence downstream (AC8).
             */
            $winnerLegend: computed<SummaryLegend>(() => {
                const experiment = store.experiment();
                const results = store.results();

                return experiment && results
                    ? getSuggestedWinner(experiment, results)
                    : { ...ReportSummaryLegendByBayesianStatus.NO_ENOUGH_SESSIONS };
            }),
            /** The promoted variant, which only the experiment knows about — never the results. */
            $promotedVariant: computed<Variant | null>(
                () =>
                    store
                        .experiment()
                        ?.trafficProportion?.variants.find(({ promoted }) => promoted) ?? null
            ),
            /**
             * The gate is experiment-wide: below it the whole table is replaced by one empty state,
             * and above it every row shows its full data however few sessions it saw (AC15).
             */
            $hasEnoughSessionsForTable: $hasEnoughSessions,
            /** The same threshold the daily chart has always been gated on. */
            $hasEnoughSessionsForDailyChart: $hasEnoughSessions,
            /**
             * A posterior distribution can only be drawn once every variant has one: a dataset
             * that came back empty would render as a flat line reading like a real result.
             */
            $hasEnoughDataForBayesianChart: computed<boolean>(() => {
                const results = store.results();
                const datasets = $bayesianChartData()?.datasets;

                if (!results || !datasets) {
                    return false;
                }

                return (
                    results.bayesianResult?.suggestedWinner !== BayesianStatusResponse.NONE &&
                    datasets.every((dataset) => dataset.data.length > 0)
                );
            }),
            $dailyChartData: computed<ChartData<'line'> | null>(() => {
                const results = store.results();
                const variants = results?.goals?.primary?.variants;

                // The labels are the control's own days, so a payload without a control has no
                // axis to draw against — the empty chart state covers it.
                if (!variants?.[DEFAULT_VARIANT_ID]) {
                    return null;
                }

                return {
                    labels: buildDailyChartLabels(variants, dotMessageService),
                    datasets: buildDailyChartData(variants)
                };
            }),
            /** One summary-table row per variant, Lift vs Original included (AC14/AC16). */
            $detailData: computed<DotExperimentResultVariantDetail[]>(() => {
                const experiment = store.experiment();
                const results = store.results();

                if (!experiment || !results?.bayesianResult) {
                    return [];
                }

                return buildVariantDetails(experiment, results, {
                    noDataLabel: dotMessageService.get(NO_DATA_LABEL_KEY),
                    rangeSeparatorLabel: dotMessageService.get(RANGE_SEPARATOR_LABEL_KEY)
                });
            })
        };
    }),
    withReducer<DotExperimentsResultsViewState>(
        /**
         * A URL arriving while the screen is up drops everything: the component is reused across
         * experiments, and results left behind would be read as the new one's until its own
         * arrive.
         */
        on(pageEvents.enter, () => ({ ...initialState, status: ComponentStatus.LOADING })),
        on(apiEvents.loadSucceeded, ({ payload }) => ({
            experiment: payload.experiment,
            results: payload.results,
            status: ComponentStatus.LOADED
        })),
        // The experiment itself is missing, so there is nothing to frame a report with: this is the
        // one failure that blanks the screen.
        on(apiEvents.loadFailed, () => ({ status: ComponentStatus.ERROR })),
        /**
         * The experiment answered but its report did not. The screen settles as LOADED with a null
         * report so the header, goal and schedule still render, and reuses the same flag a failed
         * refresh raises to say the report is missing (AC25's mechanism, applied to the first load).
         */
        on(apiEvents.resultsUnavailable, ({ payload }) => ({
            experiment: payload,
            results: null,
            status: ComponentStatus.LOADED,
            lastRefreshFailed: true
        })),

        // Refresh reports itself, and only itself: `status` stays `LOADED` throughout, so the
        // results on screen are never swapped for a skeleton (AC9).
        // `refresh$` drops this event for the statuses that never reach `getResults`, so the flag
        // must not be raised for them either — it would spin forever with no request in flight.
        on(pageEvents.refreshRequested, (_event, state) =>
            STATUSES_WITHOUT_RESULTS.includes(state.experiment?.status ?? DotExperimentStatus.DRAFT)
                ? {}
                : { refreshing: true, lastRefreshFailed: false }
        ),
        on(apiEvents.refreshSucceeded, ({ payload }) => ({
            results: payload,
            refreshing: false,
            lastRefreshFailed: false
        })),
        /**
         * Deliberately does not touch `results` or `status`: the last good report stays exactly as
         * it is and the flag is all the screen needs to say the refresh failed (AC25).
         */
        on(apiEvents.refreshFailed, () => ({ refreshing: false, lastRefreshFailed: true })),

        on(pageEvents.stopRequested, pageEvents.promoteRequested, () => ({
            status: ComponentStatus.SAVING
        })),
        /**
         * Both answer with the experiment as the server now holds it, merged rather than replaced
         * so nothing the response omits is lost. Promoting a RUNNING experiment ends it in the
         * same call, so the experiment merged here already reads ENDED and the header re-renders
         * in place, with no navigation and no second call (AC4/AC20).
         */
        on(apiEvents.stopSucceeded, apiEvents.promoteSucceeded, ({ payload }, state) => ({
            experiment: { ...state.experiment, ...payload },
            status: ComponentStatus.LOADED
        })),
        // A rejected mutation changes nothing and leaves the screen usable, so it can be retried
        // (AC5). The error itself was already reported by `DotHttpErrorManagerService`.
        on(apiEvents.stopFailed, apiEvents.promoteFailed, () => ({
            status: ComponentStatus.LOADED
        }))
    ),
    withEventHandlers(
        (
            store,
            events = inject(Events),
            experimentsService = inject(DotExperimentsService),
            httpErrorManager = inject(DotHttpErrorManagerService),
            dotMessageService = inject(DotMessageService)
        ) => {
            /** Routes a failed call through the shared manager, then reports it as its event. */
            const toFailure =
                <T>(failed: (error: HttpErrorResponse) => T) =>
                (error: HttpErrorResponse): T => {
                    httpErrorManager.handle(error);

                    return failed(error);
                };

            /**
             * A rejected results call is the analytics app not being configured as often as it is
             * anything else, and the backend answers that case without a header — which would
             * leave the error dialog titleless. Same fallback the old reports screen supplies.
             */
            const toResultsFailure =
                <T>(failed: (error: HttpErrorResponse) => T) =>
                (error: HttpErrorResponse): T => {
                    httpErrorManager.handle({
                        ...error,
                        error: {
                            ...error.error,
                            header:
                                error.error?.header ??
                                dotMessageService.get(RESULTS_ERROR_HEADER_KEY)
                        }
                    } as HttpErrorResponse);

                    return failed(error);
                };

            return {
                /**
                 * The experiment comes first and decides whether its report is worth asking for:
                 * DRAFT and SCHEDULED settle on the experiment alone (AC10). The branch reads the
                 * *status*, not whether results happen to be null — an experiment that never ran
                 * has no results to skip fetching, and one that did must always fetch them.
                 *
                 * Sequential rather than the old screen's `forkJoin`, precisely because the second
                 * call depends on what the first one answers.
                 *
                 * `getById` swallows its own errors into `undefined`, so an experiment that is not
                 * there arrives as an empty answer rather than as a rejection.
                 */
                load$: events.on(pageEvents.enter).pipe(
                    switchMap(({ payload: experimentId }) =>
                        experimentsService.getById(experimentId).pipe(
                            switchMap((experiment) => {
                                if (!experiment) {
                                    return of(apiEvents.loadFailed(experimentId));
                                }

                                if (STATUSES_WITHOUT_RESULTS.includes(experiment.status)) {
                                    return of(
                                        apiEvents.loadSucceeded({ experiment, results: null })
                                    );
                                }

                                return experimentsService.getResults(experiment.id).pipe(
                                    mapResponse({
                                        next: (results) =>
                                            apiEvents.loadSucceeded({ experiment, results }),
                                        // The experiment is already in hand, so a report that fails
                                        // costs the report, not the screen.
                                        error: toResultsFailure(() =>
                                            apiEvents.resultsUnavailable(experiment)
                                        )
                                    })
                                );
                            })
                        )
                    )
                ),

                /**
                 * Results only: the experiment cannot change while the screen sits on it, and the
                 * report is the expensive half. `switchMap` so an impatient second press replaces
                 * the first request instead of queueing behind it (AC9).
                 */
                refresh$: events.on(pageEvents.refreshRequested).pipe(
                    map(() => store.experiment()),
                    filter(
                        (experiment): experiment is DotExperiment =>
                            !!experiment && !store.$isWaitingForData()
                    ),
                    switchMap((experiment) =>
                        experimentsService.getResults(experiment.id).pipe(
                            mapResponse({
                                next: (results) => apiEvents.refreshSucceeded(results),
                                error: toResultsFailure(apiEvents.refreshFailed)
                            })
                        )
                    )
                ),

                stop$: events.on(pageEvents.stopRequested).pipe(
                    switchMap(() =>
                        experimentsService.stop(store.experiment()?.id ?? '').pipe(
                            mapResponse({
                                next: (experiment) => apiEvents.stopSucceeded(experiment),
                                error: toFailure(apiEvents.stopFailed)
                            })
                        )
                    )
                ),

                /**
                 * `mergeMap`, as every per-variant action in this portlet: promoting one row must
                 * not cancel a call already made for another. The backend ends a RUNNING
                 * experiment as part of the same call, so there is exactly one request here —
                 * whatever it answers with is already the ended experiment (AC20).
                 */
                promote$: events.on(pageEvents.promoteRequested).pipe(
                    mergeMap(({ payload: variantId }) =>
                        experimentsService
                            .promoteVariant(store.experiment()?.id ?? '', variantId)
                            .pipe(
                                mapResponse({
                                    next: (experiment) => apiEvents.promoteSucceeded(experiment),
                                    error: toFailure(apiEvents.promoteFailed)
                                })
                            )
                    )
                )
            };
        }
    ),
    withHooks(() => {
        const route = inject(ActivatedRoute);
        const dispatcher = inject(Dispatcher);

        let routeSubscription: SubscriptionLike;

        return {
            onInit() {
                /**
                 * Followed for as long as the screen lives rather than read once: the component is
                 * reused across experiments, so an id arriving while the screen is up must load
                 * the experiment it names instead of leaving the previous one on screen.
                 */
                routeSubscription = route.paramMap
                    .pipe(
                        map((params) => params.get('experimentId')),
                        distinctUntilChanged(),
                        filter((experimentId): experimentId is string => !!experimentId)
                    )
                    .subscribe((experimentId) => {
                        dispatcher.dispatch(pageEvents.enter(experimentId));
                    });
            },
            onDestroy() {
                routeSubscription?.unsubscribe();
            }
        };
    })
);

/** Injectable type of {@link DotExperimentsResultsStore}, for typing component fields. */
export type DotExperimentsResultsStore = InstanceType<typeof DotExperimentsResultsStore>;
