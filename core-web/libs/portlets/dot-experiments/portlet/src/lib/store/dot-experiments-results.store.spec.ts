import { Dispatcher, provideDispatcher } from '@ngrx/signals/events';
import { createServiceFactory, mockProvider, SpectatorService } from '@openng/spectator/jest';
import { NEVER, of, throwError } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, convertToParamMap, Params } from '@angular/router';

import {
    DotExperimentsService,
    DotHttpErrorManagerService,
    DotMessageService
} from '@dotcms/data-access';
import {
    ComponentStatus,
    DEFAULT_VARIANT_ID,
    DotExperiment,
    DotExperimentResults,
    DotExperimentStatus,
    DotResultVariant,
    GOAL_TYPES,
    TrafficProportionTypes,
    Variant
} from '@dotcms/dotcms-models';

import { dotExperimentsResultsPageEvents } from './dot-experiments-results-page.events';
import { DotExperimentsResultsStore } from './dot-experiments-results.store';

const pageEvents = dotExperimentsResultsPageEvents;

const EXPERIMENT_ID = 'exp-1';
const VARIANT_B_ID = 'variant-b';

/** The fallback title the store supplies when a rejected results call carries none of its own. */
const RESULTS_ERROR_HEADER_KEY =
    'dot.common.http.error.400.experiment.analytics-app-not-configured.header';

const buildVariant = (id: string, promoted = false): Variant => ({
    id,
    name: id,
    weight: 50,
    promoted
});

const buildExperiment = (experiment: Partial<DotExperiment> = {}): DotExperiment => ({
    id: EXPERIMENT_ID,
    pageId: 'page-1',
    name: 'Alpha campaign',
    description: 'Checkout funnel rework',
    status: DotExperimentStatus.RUNNING,
    readyToStart: true,
    archived: false,
    trafficProportion: {
        type: TrafficProportionTypes.SPLIT_EVENLY,
        variants: [buildVariant(DEFAULT_VARIANT_ID), buildVariant(VARIANT_B_ID)]
    },
    trafficAllocation: 100,
    scheduling: null,
    creationDate: new Date('2026-01-01T00:00:00.000Z'),
    modDate: 0,
    goals: null,
    ...experiment
});

const RUNNING_EXPERIMENT = buildExperiment();
const DRAFT_EXPERIMENT = buildExperiment({ status: DotExperimentStatus.DRAFT });
const SCHEDULED_EXPERIMENT = buildExperiment({ status: DotExperimentStatus.SCHEDULED });

const buildResultVariant = (variantName: string, conversions: number): DotResultVariant => ({
    details: {},
    multiBySession: conversions,
    uniqueBySession: { count: conversions, totalPercentage: 100, variantPercentage: 100 },
    variantName,
    variantDescription: `${variantName} name`,
    totalPageViews: 100
});

const buildResults = (sessionsTotal = 40): DotExperimentResults => ({
    bayesianResult: { value: 0.9, suggestedWinner: VARIANT_B_ID, results: [] },
    goals: {
        primary: {
            goal: { name: 'Reach page', type: GOAL_TYPES.REACH_PAGE, conditions: [] },
            variants: {
                [DEFAULT_VARIANT_ID]: buildResultVariant(DEFAULT_VARIANT_ID, 5),
                [VARIANT_B_ID]: buildResultVariant(VARIANT_B_ID, 12)
            }
        }
    },
    sessions: {
        total: sessionsTotal,
        variants: { [DEFAULT_VARIANT_ID]: sessionsTotal / 2, [VARIANT_B_ID]: sessionsTotal / 2 }
    }
});

const RESULTS = buildResults();
/** A second, distinguishable report, so a refresh that lands can be told from one that did not. */
const REFRESHED_RESULTS = buildResults(120);

/**
 * What a mutation endpoint answers with: the experiment as the server now holds it, without the
 * fields it does not echo — so a state that *replaced* the experiment would lose them and one that
 * merged it keeps them.
 */
const buildMutationResponse = (): DotExperiment =>
    ({
        id: EXPERIMENT_ID,
        name: RUNNING_EXPERIMENT.name,
        status: DotExperimentStatus.ENDED,
        trafficProportion: {
            type: TrafficProportionTypes.SPLIT_EVENLY,
            variants: [buildVariant(DEFAULT_VARIANT_ID), buildVariant(VARIANT_B_ID, true)]
        }
    }) as DotExperiment;

describe('DotExperimentsResultsStore', () => {
    let spectator: SpectatorService<InstanceType<typeof DotExperimentsResultsStore>>;
    let store: InstanceType<typeof DotExperimentsResultsStore>;
    let dispatcher: Dispatcher;
    let httpErrorManager: jest.Mocked<DotHttpErrorManagerService>;

    const getById = jest.fn();
    const getResults = jest.fn();
    const stop = jest.fn();
    const promoteVariant = jest.fn();
    const messageGet = jest.fn();

    let routeParams: Params;

    const activatedRouteStub = {
        get paramMap() {
            return of(convertToParamMap(routeParams));
        }
    };

    const createService = createServiceFactory({
        service: DotExperimentsResultsStore,
        providers: [
            // `Dispatcher`/`Events` are `providedIn: 'platform'`, so they outlive TestBed resets
            // and a store from a previous test would keep reacting to this test's events.
            provideDispatcher(),
            mockProvider(DotExperimentsService, { getById, getResults, stop, promoteVariant }),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(DotMessageService, { get: messageGet }),
            { provide: ActivatedRoute, useValue: activatedRouteStub }
        ]
    });

    /**
     * Creates the store. Called from the tests rather than from a global `beforeEach` because the
     * route is read and the whole load flow runs in `onInit`, so every arrangement has to be in
     * place first.
     */
    const initStore = (experimentId = EXPERIMENT_ID) => {
        routeParams = { experimentId };
        spectator = createService();
        store = spectator.service;
        dispatcher = spectator.inject(Dispatcher);
        httpErrorManager = spectator.inject(
            DotHttpErrorManagerService
        ) as jest.Mocked<DotHttpErrorManagerService>;
        spectator.flushEffects();
    };

    /** A screen that has already loaded an experiment and its report. */
    const initLoaded = (
        experiment: DotExperiment = RUNNING_EXPERIMENT,
        results: DotExperimentResults = RESULTS
    ) => {
        getById.mockReturnValue(of(experiment));
        getResults.mockReturnValue(of(results));
        initStore(experiment.id);
    };

    const httpError = (status: number, error: unknown = {}) =>
        new HttpErrorResponse({ status, error });

    beforeEach(() => {
        jest.resetAllMocks();

        getById.mockReturnValue(of(RUNNING_EXPERIMENT));
        getResults.mockReturnValue(of(RESULTS));
        stop.mockReturnValue(of(buildMutationResponse()));
        promoteVariant.mockReturnValue(of(buildMutationResponse()));
        messageGet.mockImplementation((key: string) => key);
    });

    describe('initial load', () => {
        it('should load the experiment and its results from the id on the route', () => {
            initStore();

            expect(getById).toHaveBeenCalledWith(EXPERIMENT_ID);
            expect(getResults).toHaveBeenCalledWith(EXPERIMENT_ID);
            expect(store.experiment()).toBe(RUNNING_EXPERIMENT);
            expect(store.results()).toBe(RESULTS);
            expect(store.status()).toBe(ComponentStatus.LOADED);
            expect(store.$isLoading()).toBe(false);
            expect(store.$hasLoadError()).toBe(false);
            expect(store.$canRefresh()).toBe(true);
        });

        it.each([
            ['DRAFT', DRAFT_EXPERIMENT],
            ['SCHEDULED', SCHEDULED_EXPERIMENT]
        ])('should not ask for the results of a %s experiment', (_status, experiment) => {
            getById.mockReturnValue(of(experiment));

            initStore();

            // The endpoint is uncached and costs two analytics round-trips plus a Monte Carlo run,
            // so it is never called before a single session has been recorded (AC10).
            expect(getResults).not.toHaveBeenCalled();
            expect(store.experiment()).toBe(experiment);
            expect(store.results()).toBeNull();
            expect(store.status()).toBe(ComponentStatus.LOADED);
            expect(store.$isWaitingForData()).toBe(true);
            expect(store.$canRefresh()).toBe(false);
        });

        it('should stay loading while the results call is in flight', () => {
            getResults.mockReturnValue(NEVER);

            initStore();

            expect(store.status()).toBe(ComponentStatus.LOADING);
            expect(store.$isLoading()).toBe(true);
            expect(store.results()).toBeNull();
        });

        it('should end in the error state when the experiment cannot be found', () => {
            getById.mockReturnValue(of(undefined));

            initStore();

            expect(getResults).not.toHaveBeenCalled();
            expect(store.experiment()).toBeNull();
            expect(store.$hasLoadError()).toBe(true);
            expect(store.status()).toBe(ComponentStatus.ERROR);
        });

        it('should keep the screen and report inline when only the results fail to load', () => {
            const error = httpError(400);
            getResults.mockReturnValue(throwError(() => error));

            initStore();

            // The experiment answered, so everything it accounts for — name, status, goal,
            // schedule — still renders. Only the report is missing, and it is reported inline
            // rather than replacing the screen with an error card. Blanking here would be a
            // regression against the screen this one replaces, which degrades the same way when
            // `getResults` 400s while `getById` succeeds.
            expect(store.status()).toBe(ComponentStatus.LOADED);
            expect(store.$hasLoadError()).toBe(false);
            expect(store.experiment()).toEqual(RUNNING_EXPERIMENT);
            expect(store.results()).toBeNull();
            expect(store.lastRefreshFailed()).toBe(true);
            expect(httpErrorManager.handle).toHaveBeenCalledTimes(1);
        });

        it('should title a headerless results failure with the analytics fallback', () => {
            getResults.mockReturnValue(throwError(() => httpError(400, { message: 'boom' })));

            initStore();

            expect(httpErrorManager.handle).toHaveBeenCalledWith(
                expect.objectContaining({
                    error: { message: 'boom', header: RESULTS_ERROR_HEADER_KEY }
                })
            );
        });

        it('should keep the header the backend sent when there is one', () => {
            getResults.mockReturnValue(
                throwError(() => httpError(500, { header: 'Server error' }))
            );

            initStore();

            expect(httpErrorManager.handle).toHaveBeenCalledWith(
                expect.objectContaining({ error: { header: 'Server error' } })
            );
        });
    });

    describe('refresh', () => {
        it('should replace the results and leave the experiment alone', () => {
            initLoaded();
            getResults.mockReturnValue(of(REFRESHED_RESULTS));

            dispatcher.dispatch(pageEvents.refreshRequested());

            expect(getResults).toHaveBeenCalledTimes(2);
            expect(store.results()).toBe(REFRESHED_RESULTS);
            expect(store.experiment()).toBe(RUNNING_EXPERIMENT);
            expect(getById).toHaveBeenCalledTimes(1);
            expect(store.refreshing()).toBe(false);
            expect(store.lastRefreshFailed()).toBe(false);
            expect(store.status()).toBe(ComponentStatus.LOADED);
        });

        it('should keep the results on screen while the new ones are in flight', () => {
            initLoaded();
            getResults.mockReturnValue(NEVER);

            dispatcher.dispatch(pageEvents.refreshRequested());

            expect(store.refreshing()).toBe(true);
            // Never swapped for a skeleton: the report on screen stays put until its replacement
            // arrives (AC9).
            expect(store.results()).toBe(RESULTS);
            expect(store.status()).toBe(ComponentStatus.LOADED);
            expect(store.$isLoading()).toBe(false);
        });

        it('should keep the last good results when the refresh fails', () => {
            initLoaded();
            getResults.mockReturnValue(throwError(() => httpError(500)));

            dispatcher.dispatch(pageEvents.refreshRequested());

            // A failed refresh is reported without blanking a screen that has already loaded
            // (AC25): the results, and the status behind them, are untouched.
            expect(store.results()).toBe(RESULTS);
            expect(store.experiment()).toBe(RUNNING_EXPERIMENT);
            expect(store.status()).toBe(ComponentStatus.LOADED);
            expect(store.$hasLoadError()).toBe(false);
            expect(store.lastRefreshFailed()).toBe(true);
            expect(store.refreshing()).toBe(false);
            expect(httpErrorManager.handle).toHaveBeenCalledTimes(1);
        });

        it('should clear the previous failure when a later refresh lands', () => {
            initLoaded();
            getResults.mockReturnValue(throwError(() => httpError(500)));
            dispatcher.dispatch(pageEvents.refreshRequested());

            getResults.mockReturnValue(of(REFRESHED_RESULTS));
            dispatcher.dispatch(pageEvents.refreshRequested());

            expect(store.lastRefreshFailed()).toBe(false);
            expect(store.results()).toBe(REFRESHED_RESULTS);
        });

        it('should ignore a refresh for an experiment that has nothing to report', () => {
            initLoaded(DRAFT_EXPERIMENT);

            dispatcher.dispatch(pageEvents.refreshRequested());

            // The control does not exist on a screen with nothing to refresh (AC9/AC10), and the
            // handler drops the event even if something else raises it.
            expect(store.$canRefresh()).toBe(false);
            expect(getResults).not.toHaveBeenCalled();
            expect(store.results()).toBeNull();
        });
    });

    describe('promote', () => {
        it('should close its buttons while the promotion is on the wire', () => {
            initLoaded();
            promoteVariant.mockReturnValue(NEVER);

            dispatcher.dispatch(pageEvents.promoteRequested(VARIANT_B_ID));

            expect(store.$isSaving()).toBe(true);
            expect(store.status()).toBe(ComponentStatus.SAVING);
        });

        it('should merge the already-ended experiment the promotion answered with', () => {
            initLoaded();

            dispatcher.dispatch(pageEvents.promoteRequested(VARIANT_B_ID));

            expect(promoteVariant).toHaveBeenCalledWith(EXPERIMENT_ID, VARIANT_B_ID);
            // Promoting a RUNNING experiment ends it in the same call, so the experiment that
            // comes back already reads ENDED and the header re-renders in place (AC20).
            expect(store.experiment()?.status).toBe(DotExperimentStatus.ENDED);
            expect(store.$status()).toBe(DotExperimentStatus.ENDED);
            // Merged, not replaced: what the response omits is still on the experiment.
            expect(store.experiment()?.description).toBe(RUNNING_EXPERIMENT.description);
            expect(store.experiment()?.pageId).toBe(RUNNING_EXPERIMENT.pageId);
            expect(store.$promotedVariant()).toEqual(buildVariant(VARIANT_B_ID, true));
            expect(store.results()).toBe(RESULTS);
            expect(store.status()).toBe(ComponentStatus.LOADED);
        });

        it('should leave the screen usable when the promotion fails', () => {
            initLoaded();
            const error = httpError(500);
            promoteVariant.mockReturnValue(throwError(() => error));

            dispatcher.dispatch(pageEvents.promoteRequested(VARIANT_B_ID));

            expect(httpErrorManager.handle).toHaveBeenCalledWith(error);
            // A rejected mutation changes nothing and can be retried (AC5).
            expect(store.experiment()).toBe(RUNNING_EXPERIMENT);
            expect(store.$promotedVariant()).toBeNull();
            expect(store.results()).toBe(RESULTS);
            expect(store.status()).toBe(ComponentStatus.LOADED);
            expect(store.$hasLoadError()).toBe(false);
            expect(store.$isSaving()).toBe(false);
        });
    });

    describe('stop', () => {
        it('should end the experiment in place', () => {
            initLoaded();

            dispatcher.dispatch(pageEvents.stopRequested());

            expect(stop).toHaveBeenCalledWith(EXPERIMENT_ID);
            expect(store.experiment()?.status).toBe(DotExperimentStatus.ENDED);
            expect(store.$status()).toBe(DotExperimentStatus.ENDED);
            expect(store.experiment()?.description).toBe(RUNNING_EXPERIMENT.description);
            expect(store.results()).toBe(RESULTS);
            expect(store.status()).toBe(ComponentStatus.LOADED);
        });

        it('should leave the experiment running and retryable when the stop fails', () => {
            initLoaded();
            const error = httpError(500);
            stop.mockReturnValue(throwError(() => error));

            dispatcher.dispatch(pageEvents.stopRequested());

            expect(httpErrorManager.handle).toHaveBeenCalledWith(error);
            expect(store.experiment()).toBe(RUNNING_EXPERIMENT);
            expect(store.$status()).toBe(DotExperimentStatus.RUNNING);
            expect(store.status()).toBe(ComponentStatus.LOADED);
            expect(store.$hasLoadError()).toBe(false);
            expect(store.$isSaving()).toBe(false);
        });
    });
});
