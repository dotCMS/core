import { Dispatcher, EventCreator, provideDispatcher } from '@ngrx/signals/events';
import { createServiceFactory, mockProvider, SpectatorService } from '@openng/spectator/jest';
import { NEVER, of, throwError } from 'rxjs';

import { Location } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { signal, WritableSignal } from '@angular/core';
import { ActivatedRoute, Params } from '@angular/router';

import {
    DotContentSearchService,
    DotExperimentsService,
    DotHttpErrorManagerService
} from '@dotcms/data-access';
import {
    DotCMSContentlet,
    DotExperiment,
    DotExperimentStatus,
    TrafficProportionTypes
} from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';

import { dotExperimentsListEvents } from './dot-experiments-list.events';
import {
    DEFAULT_EXPERIMENTS_LIST_PAGE,
    DEFAULT_EXPERIMENTS_LIST_PER_PAGE,
    DEFAULT_EXPERIMENTS_LIST_STATUSES,
    DotExperimentsListStore
} from './dot-experiments-list.store';

const CURRENT_SITE_ID = 'site-1';
const OTHER_SITE_ID = 'site-2';

const buildExperiment = (experiment: Partial<DotExperiment>): DotExperiment => ({
    id: 'exp-id',
    pageId: 'page-1',
    name: 'Experiment',
    description: 'An experiment',
    status: DotExperimentStatus.DRAFT,
    readyToStart: false,
    archived: false,
    trafficProportion: { type: TrafficProportionTypes.SPLIT_EVENLY, variants: [] },
    trafficAllocation: 100,
    scheduling: null,
    creationDate: new Date('2026-01-01T00:00:00.000Z'),
    modDate: 0,
    goals: null,
    ...experiment
});

const buildPageContentlet = (identifier: string, url: string, host: string): DotCMSContentlet =>
    ({ identifier, url, host }) as unknown as DotCMSContentlet;

const EXPERIMENT_DRAFT = buildExperiment({
    id: 'exp-draft',
    pageId: 'page-1',
    name: 'Alpha campaign',
    status: DotExperimentStatus.DRAFT,
    modDate: 300
});

const EXPERIMENT_RUNNING = buildExperiment({
    id: 'exp-running',
    pageId: 'page-2',
    name: 'Beta rollout',
    status: DotExperimentStatus.RUNNING,
    modDate: 100
});

/** Lives on a page of another site: must never reach the list. */
const EXPERIMENT_OTHER_SITE = buildExperiment({
    id: 'exp-other-site',
    pageId: 'page-3',
    name: 'Gamma remote',
    status: DotExperimentStatus.DRAFT,
    modDate: 200
});

const EXPERIMENT_ARCHIVED = buildExperiment({
    id: 'exp-archived',
    pageId: 'page-1',
    name: 'Delta retired',
    status: DotExperimentStatus.ARCHIVED,
    archived: true,
    modDate: 400
});

/** Its `pageId` is not returned by the page lookup: unresolvable, so it must be dropped. */
const EXPERIMENT_ORPHAN = buildExperiment({
    id: 'exp-orphan',
    pageId: 'page-orphan',
    name: 'Epsilon orphan',
    status: DotExperimentStatus.DRAFT,
    modDate: 500
});

const EXPERIMENTS: DotExperiment[] = [
    EXPERIMENT_DRAFT,
    EXPERIMENT_RUNNING,
    EXPERIMENT_OTHER_SITE,
    EXPERIMENT_ARCHIVED,
    EXPERIMENT_ORPHAN
];

const PAGE_SEARCH_RESULT = {
    jsonObjectView: {
        contentlets: [
            buildPageContentlet('page-1', '/home', CURRENT_SITE_ID),
            buildPageContentlet('page-2', '/checkout', CURRENT_SITE_ID),
            buildPageContentlet('page-3', '/remote', OTHER_SITE_ID)
        ]
    }
};

describe('DotExperimentsListStore', () => {
    let spectator: SpectatorService<InstanceType<typeof DotExperimentsListStore>>;
    let store: InstanceType<typeof DotExperimentsListStore>;
    let dispatcher: Dispatcher;
    let httpErrorManager: jest.Mocked<DotHttpErrorManagerService>;

    const getAllUnfiltered = jest.fn();
    const archive = jest.fn();
    const remove = jest.fn();
    const stop = jest.fn();
    const cancelSchedule = jest.fn();
    const contentSearchGet = jest.fn();
    const locationSubscribe = jest.fn();

    let currentSiteId: WritableSignal<string | null>;
    let queryParams: Params;

    const createService = createServiceFactory({
        service: DotExperimentsListStore,
        providers: [
            // `Dispatcher`/`Events` are `providedIn: 'platform'`, so they outlive TestBed resets
            // and a store from a previous test would keep reacting to this test's events.
            provideDispatcher(),
            mockProvider(DotExperimentsService, {
                getAllUnfiltered,
                archive,
                delete: remove,
                stop,
                cancelSchedule
            }),
            mockProvider(DotContentSearchService, { get: contentSearchGet }),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(GlobalStore, {
                get currentSiteId() {
                    return currentSiteId;
                }
            }),
            // The store subscribes to Location (popstate re-hydration).
            mockProvider(Location, { subscribe: locationSubscribe }),
            {
                provide: ActivatedRoute,
                useValue: {
                    snapshot: {
                        get queryParams() {
                            return queryParams;
                        }
                    }
                }
            }
        ]
    });

    /**
     * Creates the store. Called from the tests (not from a global `beforeEach`) because the
     * whole load flow runs in `onInit`, so every arrangement has to be in place first.
     */
    const initStore = () => {
        spectator = createService();
        store = spectator.service;
        dispatcher = spectator.inject(Dispatcher);
        httpErrorManager = spectator.inject(
            DotHttpErrorManagerService
        ) as jest.Mocked<DotHttpErrorManagerService>;
        spectator.flushEffects();
    };

    const httpError = (status: number) => new HttpErrorResponse({ status });

    beforeEach(() => {
        jest.resetAllMocks();

        getAllUnfiltered.mockReturnValue(of(EXPERIMENTS));
        contentSearchGet.mockReturnValue(of(PAGE_SEARCH_RESULT));
        archive.mockReturnValue(of({}));
        remove.mockReturnValue(of({}));
        stop.mockReturnValue(of({}));
        cancelSchedule.mockReturnValue(of({}));
        locationSubscribe.mockReturnValue({ unsubscribe: jest.fn() });

        currentSiteId = signal<string | null>(CURRENT_SITE_ID);
        queryParams = {};
    });

    describe('initial load', () => {
        it('should request the list once on init', () => {
            initStore();

            expect(getAllUnfiltered).toHaveBeenCalledTimes(1);
        });

        it('should look the distinct page ids up in bulk once the list arrives', () => {
            initStore();

            expect(contentSearchGet).toHaveBeenCalledWith({
                query: '+contentType:htmlpageasset +working:true +identifier:(page-1 page-2 page-3 page-orphan)',
                limit: 4
            });
        });

        it('should store the experiments and the resolved page info and end up loaded', () => {
            initStore();

            expect(store.experiments()).toEqual(EXPERIMENTS);
            expect(store.pageInfoByPageId()).toEqual({
                'page-1': { url: '/home', host: CURRENT_SITE_ID },
                'page-2': { url: '/checkout', host: CURRENT_SITE_ID },
                'page-3': { url: '/remote', host: OTHER_SITE_ID }
            });
            expect(store.status()).toBe('loaded');
        });

        it('should stay loading while the page lookup is in flight', () => {
            contentSearchGet.mockReturnValue(NEVER);

            initStore();

            expect(store.experiments()).toEqual(EXPERIMENTS);
            expect(store.status()).toBe('loading');
        });

        it('should skip the page lookup and land loaded when the list is empty', () => {
            getAllUnfiltered.mockReturnValue(of([]));

            initStore();

            expect(contentSearchGet).not.toHaveBeenCalled();
            expect(store.status()).toBe('loaded');
        });
    });

    describe('load failure', () => {
        it('should end in error and report the failure when the list request fails', () => {
            const error = httpError(500);
            getAllUnfiltered.mockReturnValue(throwError(() => error));

            initStore();

            expect(store.status()).toBe('error');
            expect(store.experiments()).toEqual([]);
            expect(store.error()).toBe(error);
            expect(httpErrorManager.handle).toHaveBeenCalledWith(error);
        });

        it('should end in error when the page lookup fails, since no experiment can be scoped', () => {
            const error = httpError(403);
            contentSearchGet.mockReturnValue(throwError(() => error));

            initStore();

            expect(store.status()).toBe('error');
            expect(store.pageInfoByPageId()).toEqual({});
            expect(httpErrorManager.handle).toHaveBeenCalledWith(error);
        });
    });

    describe('CRUD actions', () => {
        interface CrudCase {
            action: string;
            requested: EventCreator<string, DotExperiment>;
            serviceCall: jest.Mock;
        }

        const CRUD_CASES: CrudCase[] = [
            {
                action: 'archive',
                requested: dotExperimentsListEvents.archiveRequested,
                serviceCall: archive
            },
            {
                action: 'delete',
                requested: dotExperimentsListEvents.deleteRequested,
                serviceCall: remove
            },
            {
                action: 'end',
                requested: dotExperimentsListEvents.endRequested,
                serviceCall: stop
            },
            // `abort` deliberately cancels the schedule, mirroring the legacy per-page store.
            {
                action: 'abort',
                requested: dotExperimentsListEvents.abortRequested,
                serviceCall: cancelSchedule
            },
            {
                action: 'cancelSchedule',
                requested: dotExperimentsListEvents.cancelScheduleRequested,
                serviceCall: cancelSchedule
            }
        ];

        describe.each(CRUD_CASES)('$action', ({ requested, serviceCall }) => {
            beforeEach(() => initStore());

            it('should call the service with the experiment id and reload the list on success', () => {
                dispatcher.dispatch(requested(EXPERIMENT_DRAFT));

                expect(serviceCall).toHaveBeenCalledWith(EXPERIMENT_DRAFT.id);
                expect(getAllUnfiltered).toHaveBeenCalledTimes(2);
                expect(store.status()).toBe('loaded');
            });

            it('should report the failure and keep the list usable on error', () => {
                const error = httpError(400);
                serviceCall.mockReturnValue(throwError(() => error));

                dispatcher.dispatch(requested(EXPERIMENT_DRAFT));

                expect(httpErrorManager.handle).toHaveBeenCalledWith(error);
                expect(store.status()).toBe('loaded');
                expect(getAllUnfiltered).toHaveBeenCalledTimes(1);
            });
        });
    });

    describe('site scoping', () => {
        beforeEach(() => initStore());

        it('should keep only the experiments whose page resolves to the current site', () => {
            expect(store.siteScopedExperiments()).toEqual([
                EXPERIMENT_DRAFT,
                EXPERIMENT_RUNNING,
                EXPERIMENT_ARCHIVED
            ]);
        });

        it('should drop an experiment whose page id could not be resolved', () => {
            expect(store.siteScopedExperiments()).not.toContain(EXPERIMENT_ORPHAN);
        });

        it('should drop an experiment whose page belongs to another site', () => {
            expect(store.siteScopedExperiments()).not.toContain(EXPERIMENT_OTHER_SITE);
        });

        it('should show nothing while there is no current site', () => {
            currentSiteId.set(null);

            expect(store.siteScopedExperiments()).toEqual([]);
        });
    });

    describe('search', () => {
        beforeEach(() => initStore());

        it('should match the experiment name case-insensitively', () => {
            dispatcher.dispatch(dotExperimentsListEvents.filterChanged('ALPHA'));

            expect(store.searchedExperiments()).toEqual([EXPERIMENT_DRAFT]);
        });

        it('should match the resolved page path', () => {
            dispatcher.dispatch(dotExperimentsListEvents.filterChanged('/CheckOut'));

            expect(store.searchedExperiments()).toEqual([EXPERIMENT_RUNNING]);
        });

        it('should return nothing when neither name nor page path match', () => {
            dispatcher.dispatch(dotExperimentsListEvents.filterChanged('no-match'));

            expect(store.searchedExperiments()).toEqual([]);
        });

        it('should never match an experiment outside the current site', () => {
            dispatcher.dispatch(dotExperimentsListEvents.filterChanged('gamma'));

            expect(store.searchedExperiments()).toEqual([]);
        });
    });

    describe('statusCounts', () => {
        beforeEach(() => initStore());

        it('should count the site scoped experiments per status', () => {
            expect(store.statusCounts()).toEqual({
                [DotExperimentStatus.DRAFT]: 1,
                [DotExperimentStatus.RUNNING]: 1,
                [DotExperimentStatus.ARCHIVED]: 1,
                [DotExperimentStatus.SCHEDULED]: 0,
                [DotExperimentStatus.ENDED]: 0
            });
        });

        it('should not change when the status selection changes', () => {
            const countsBefore = store.statusCounts();

            dispatcher.dispatch(
                dotExperimentsListEvents.statusesChanged([DotExperimentStatus.DRAFT])
            );

            expect(store.statusCounts()).toEqual(countsBefore);
            expect(store.filteredExperiments()).toEqual([EXPERIMENT_DRAFT]);
        });

        it('should follow the search term', () => {
            dispatcher.dispatch(dotExperimentsListEvents.filterChanged('delta'));

            expect(store.statusCounts()).toEqual({
                [DotExperimentStatus.DRAFT]: 0,
                [DotExperimentStatus.RUNNING]: 0,
                [DotExperimentStatus.ARCHIVED]: 1,
                [DotExperimentStatus.SCHEDULED]: 0,
                [DotExperimentStatus.ENDED]: 0
            });
        });
    });

    describe('status selection', () => {
        beforeEach(() => initStore());

        it('should select every status except ARCHIVED by default', () => {
            expect(store.selectedStatuses()).toEqual(DEFAULT_EXPERIMENTS_LIST_STATUSES);
            expect(store.selectedStatuses()).not.toContain(DotExperimentStatus.ARCHIVED);
        });

        it('should hide archived experiments until they are explicitly selected', () => {
            expect(store.filteredExperiments()).not.toContain(EXPERIMENT_ARCHIVED);

            dispatcher.dispatch(
                dotExperimentsListEvents.statusesChanged([DotExperimentStatus.ARCHIVED])
            );

            expect(store.filteredExperiments()).toEqual([EXPERIMENT_ARCHIVED]);
        });
    });

    describe('sorting', () => {
        beforeEach(() => initStore());

        it('should sort by modDate DESC by default', () => {
            expect(store.sortedExperiments()).toEqual([EXPERIMENT_DRAFT, EXPERIMENT_RUNNING]);
        });

        it('should sort by modDate ASC when the direction flips', () => {
            dispatcher.dispatch(
                dotExperimentsListEvents.sortChanged({ orderBy: 'modDate', direction: 'ASC' })
            );

            expect(store.sortedExperiments()).toEqual([EXPERIMENT_RUNNING, EXPERIMENT_DRAFT]);
        });

        it('should keep the API order for a column that is not sortable yet', () => {
            dispatcher.dispatch(
                dotExperimentsListEvents.sortChanged({ orderBy: 'name', direction: 'ASC' })
            );

            expect(store.sortedExperiments()).toEqual([EXPERIMENT_DRAFT, EXPERIMENT_RUNNING]);
        });
    });

    describe('paging', () => {
        beforeEach(() => initStore());

        it('should return the slice of the requested page', () => {
            dispatcher.dispatch(dotExperimentsListEvents.pageChanged({ page: 1, perPage: 1 }));
            expect(store.pagedExperiments()).toEqual([EXPERIMENT_DRAFT]);

            dispatcher.dispatch(dotExperimentsListEvents.pageChanged({ page: 2, perPage: 1 }));
            expect(store.pagedExperiments()).toEqual([EXPERIMENT_RUNNING]);
        });

        it('should count every filtered experiment, not just the current page', () => {
            dispatcher.dispatch(dotExperimentsListEvents.pageChanged({ page: 1, perPage: 1 }));

            expect(store.totalRecords()).toBe(2);
        });
    });

    describe('URL hydration', () => {
        it('should hydrate filter, paging and sort from the query params', () => {
            queryParams = {
                page: '3',
                per_page: '10',
                orderby: 'name',
                direction: 'asc',
                filter: 'beta'
            };

            initStore();

            expect(store.page()).toBe(3);
            expect(store.perPage()).toBe(10);
            expect(store.orderBy()).toBe('name');
            expect(store.direction()).toBe('ASC');
            expect(store.filter()).toBe('beta');
        });

        it('should hydrate a single status param provided as a string', () => {
            queryParams = { status: 'draft' };

            initStore();

            expect(store.selectedStatuses()).toEqual([DotExperimentStatus.DRAFT]);
        });

        it('should hydrate a repeated status param provided as an array', () => {
            queryParams = { status: ['draft', 'RUNNING', 'not-a-status'] };

            initStore();

            expect(store.selectedStatuses()).toEqual([
                DotExperimentStatus.DRAFT,
                DotExperimentStatus.RUNNING
            ]);
        });

        it('should fall back to the default selection when the status param is absent', () => {
            queryParams = { filter: 'beta' };

            initStore();

            expect(store.selectedStatuses()).toEqual(DEFAULT_EXPERIMENTS_LIST_STATUSES);
        });

        it('should ignore unusable paging params', () => {
            queryParams = { page: '0', per_page: 'many' };

            initStore();

            expect(store.page()).toBe(DEFAULT_EXPERIMENTS_LIST_PAGE);
            expect(store.perPage()).toBe(DEFAULT_EXPERIMENTS_LIST_PER_PAGE);
        });

        it('should hydrate before the first fetch is requested', () => {
            queryParams = { page: '3' };
            const dispatchSpy = jest.spyOn(Dispatcher.prototype, 'dispatch');

            initStore();

            const dispatchedTypes = dispatchSpy.mock.calls.map(([event]) => event.type);
            expect(dispatchedTypes.indexOf(dotExperimentsListEvents.hydratedFromUrl.type)).toBe(0);
            expect(dispatchedTypes.indexOf(dotExperimentsListEvents.listRequested.type)).toBe(1);

            dispatchSpy.mockRestore();
        });
    });

    describe('site switch', () => {
        beforeEach(() => initStore());

        it('should keep the view state, restart paging and reload the list', () => {
            dispatcher.dispatch(dotExperimentsListEvents.filterChanged('alpha'));
            dispatcher.dispatch(
                dotExperimentsListEvents.statusesChanged([DotExperimentStatus.DRAFT])
            );
            dispatcher.dispatch(
                dotExperimentsListEvents.sortChanged({ orderBy: 'modDate', direction: 'ASC' })
            );
            dispatcher.dispatch(dotExperimentsListEvents.pageChanged({ page: 3, perPage: 10 }));

            currentSiteId.set(OTHER_SITE_ID);
            spectator.flushEffects();

            expect(store.page()).toBe(DEFAULT_EXPERIMENTS_LIST_PAGE);
            expect(store.perPage()).toBe(10);
            expect(store.filter()).toBe('alpha');
            expect(store.orderBy()).toBe('modDate');
            expect(store.direction()).toBe('ASC');
            expect(store.selectedStatuses()).toEqual([DotExperimentStatus.DRAFT]);
            expect(getAllUnfiltered).toHaveBeenCalledTimes(2);
        });

        it('should not reload when the site signal emits the same site', () => {
            currentSiteId.set(CURRENT_SITE_ID);
            spectator.flushEffects();

            expect(getAllUnfiltered).toHaveBeenCalledTimes(1);
        });
    });
});
