import { tapResponse } from '@ngrx/operators';
import { signalStore, withComputed, withHooks, withState } from '@ngrx/signals';
import {
    Dispatcher,
    EventCreator,
    Events,
    on,
    withEventHandlers,
    withReducer
} from '@ngrx/signals/events';
import { EMPTY, Observable, SubscriptionLike } from 'rxjs';

import { Location } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { computed, effect, EffectRef, inject, untracked } from '@angular/core';
import { ActivatedRoute, Params } from '@angular/router';

import { mergeMap, switchMap } from 'rxjs/operators';

import {
    DotContentSearchService,
    DotExperimentsService,
    DotHttpErrorManagerService
} from '@dotcms/data-access';
import {
    ComponentStatus,
    DotCMSContentlet,
    DotExperiment,
    DotExperimentStatus,
    HealthStatusTypes
} from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';

import {
    DotExperimentPageInfo,
    dotExperimentsListEvents,
    DotExperimentsListSortDirection,
    DotExperimentsListViewState
} from './dot-experiments-list.events';

/** Full state of the experiments list. */
export interface DotExperimentsListState extends DotExperimentsListViewState {
    status: ComponentStatus;
    /** Analytics health, `null` until the gate resolves. Anything but `OK` blocks the list. */
    healthStatus: HealthStatusTypes | null;
    /** Every experiment returned by the API, across all sites. Narrowed by `siteScopedExperiments`. */
    experiments: DotExperiment[];
    /** Page url/host resolved per `pageId`; the only source of site information for an experiment. */
    pageInfoByPageId: Record<string, DotExperimentPageInfo>;
    error: unknown;
}

export const DEFAULT_EXPERIMENTS_LIST_PAGE = 1;
export const DEFAULT_EXPERIMENTS_LIST_PER_PAGE = 25;
export const DEFAULT_EXPERIMENTS_LIST_ORDER_BY = 'modDate';
export const DEFAULT_EXPERIMENTS_LIST_DIRECTION: DotExperimentsListSortDirection = 'DESC';

/**
 * No status is selected by default: the filter starts empty, like every other filter in the
 * admin, so nothing is pre-ticked and the chip reads as unfiltered.
 */
export const DEFAULT_EXPERIMENTS_LIST_STATUSES: DotExperimentStatus[] = [];

/**
 * Statuses hidden while the filter is empty. Archived experiments are opt-in — an unfiltered
 * list means "everything still in play", and archived rows would otherwise pad it out
 * permanently with work nobody is looking at. Selecting ARCHIVED shows them.
 */
const OPT_IN_STATUSES: readonly DotExperimentStatus[] = [DotExperimentStatus.ARCHIVED];

const initialState: DotExperimentsListState = {
    status: ComponentStatus.LOADING,
    healthStatus: null,
    experiments: [],
    pageInfoByPageId: {},
    filter: '',
    selectedStatuses: DEFAULT_EXPERIMENTS_LIST_STATUSES,
    page: DEFAULT_EXPERIMENTS_LIST_PAGE,
    perPage: DEFAULT_EXPERIMENTS_LIST_PER_PAGE,
    orderBy: DEFAULT_EXPERIMENTS_LIST_ORDER_BY,
    direction: DEFAULT_EXPERIMENTS_LIST_DIRECTION,
    error: null
};

/** Shape of the `/api/content/_search` entity the page lookup reads contentlets from. */
interface ContentSearchEntity {
    jsonObjectView: { contentlets: DotCMSContentlet[] };
}

/**
 * Store for the experiments list.
 *
 * Nothing is fetched until the Analytics health gate passes: `isMisconfigured` is the inline
 * equivalent of the legacy `AnalyticsAppGuard` redirect, so `/experiments` stays the URL.
 *
 * The API returns every experiment regardless of site and `DotExperiment` has no host, so the
 * list is narrowed client-side: one bulk `htmlpageasset` lookup resolves each distinct `pageId`
 * to its `url` (Page column) and `host` (site filter). Search, status narrowing, sorting and
 * paging are then derived from that set, all in computed signals.
 *
 * State only ever changes through dispatched events (`withReducer`); the store exposes no
 * mutating methods and never opens UI — confirmations and toasts belong to the component.
 *
 * Not provided in root: supply it in the route (or component) `providers` together with
 * `DotExperimentsService`, so each list instance is isolated.
 */
export const DotExperimentsListStore = signalStore(
    withState<DotExperimentsListState>(initialState),
    withComputed((store) => {
        const globalStore = inject(GlobalStore);

        /**
         * Fails closed: an experiment whose `pageId` could not be resolved is dropped, so an
         * experiment from another site can never leak into the list.
         */
        const siteScopedExperiments = computed<DotExperiment[]>(() => {
            const currentSiteId = globalStore.currentSiteId();

            if (!currentSiteId) {
                return [];
            }

            const pageInfoByPageId = store.pageInfoByPageId();

            return store
                .experiments()
                .filter(
                    (experiment) => pageInfoByPageId[experiment.pageId]?.host === currentSiteId
                );
        });

        const searchedExperiments = computed<DotExperiment[]>(() => {
            const term = store.filter().trim().toLowerCase();

            if (!term) {
                return siteScopedExperiments();
            }

            const pageInfoByPageId = store.pageInfoByPageId();

            return siteScopedExperiments().filter((experiment) => {
                const pagePath = pageInfoByPageId[experiment.pageId]?.url ?? '';

                return (
                    experiment.name.toLowerCase().includes(term) ||
                    pagePath.toLowerCase().includes(term)
                );
            });
        });

        /**
         * Counts per status over the site + search filtered set, deliberately independent of
         * `selectedStatuses` so selecting a status never changes the numbers shown in the chips.
         */
        const statusCounts = computed<Record<DotExperimentStatus, number>>(() => {
            const counts = emptyStatusCounts();

            for (const experiment of searchedExperiments()) {
                counts[experiment.status] = (counts[experiment.status] ?? 0) + 1;
            }

            return counts;
        });

        const filteredExperiments = computed<DotExperiment[]>(() => {
            const selectedStatuses = store.selectedStatuses();

            // An empty selection is "no status filter", not "match nothing" — clearing the chip
            // widens the list back out the way clearing any other filter does, rather than
            // leaving an empty table whose only escape is re-picking every status.
            // Archived stays out of that default view; it is opt-in.
            if (!selectedStatuses.length) {
                return searchedExperiments().filter(
                    (experiment) => !OPT_IN_STATUSES.includes(experiment.status)
                );
            }

            return searchedExperiments().filter((experiment) =>
                selectedStatuses.includes(experiment.status)
            );
        });

        const sortedExperiments = computed<DotExperiment[]>(() => {
            const experiments = filteredExperiments();

            // `modDate` is the only sortable column for now; anything else keeps the API order.
            if (store.orderBy() !== DEFAULT_EXPERIMENTS_LIST_ORDER_BY) {
                return experiments;
            }

            const factor = store.direction() === 'ASC' ? 1 : -1;

            return [...experiments].sort((a, b) => (a.modDate - b.modDate) * factor);
        });

        const pagedExperiments = computed<DotExperiment[]>(() => {
            const perPage = store.perPage();
            const start = (store.page() - 1) * perPage;

            return sortedExperiments().slice(start, start + perPage);
        });

        return {
            siteScopedExperiments,
            searchedExperiments,
            statusCounts,
            filteredExperiments,
            sortedExperiments,
            pagedExperiments,
            totalRecords: computed<number>(() => filteredExperiments().length),
            /**
             * Same rule as the legacy `AnalyticsAppGuard`: only `OK` passes. Stays `false`
             * while the gate is pending, so the list is never blocked on a guess.
             */
            isMisconfigured: computed<boolean>(() => {
                const healthStatus = store.healthStatus();

                return healthStatus !== null && healthStatus !== HealthStatusTypes.OK;
            })
        };
    }),
    withReducer<DotExperimentsListState>(
        on(dotExperimentsListEvents.healthCheckSucceeded, ({ payload }) => ({
            healthStatus: payload,
            // A non-OK gate stops the flow here: nothing else is fetched, so settle instead of
            // leaving the table stuck on its skeleton.
            status:
                payload === HealthStatusTypes.OK ? ComponentStatus.LOADING : ComponentStatus.LOADED
        })),
        on(dotExperimentsListEvents.healthCheckFailed, ({ payload }) => ({
            status: ComponentStatus.ERROR,
            error: payload
        })),
        on(dotExperimentsListEvents.listRequested, () => ({
            status: ComponentStatus.LOADING,
            error: null
        })),
        on(dotExperimentsListEvents.listSucceeded, ({ payload }) => ({
            experiments: payload,
            pageInfoByPageId: {},
            // Stay in `loading` until the page lookup resolves: without it the site filter fails
            // closed and the table would flash an empty "loaded" list first.
            status: payload.length > 0 ? ComponentStatus.LOADING : ComponentStatus.LOADED,
            error: null
        })),
        on(dotExperimentsListEvents.listFailed, ({ payload }) => ({
            status: ComponentStatus.ERROR,
            experiments: [],
            pageInfoByPageId: {},
            error: payload
        })),
        on(dotExperimentsListEvents.pageInfoSucceeded, ({ payload }) => ({
            pageInfoByPageId: payload,
            status: ComponentStatus.LOADED
        })),
        // Without page info no experiment can be attributed to a site, so this is a failed load
        // rather than an empty list.
        on(dotExperimentsListEvents.pageInfoFailed, ({ payload }) => ({
            status: ComponentStatus.ERROR,
            pageInfoByPageId: {},
            error: payload
        })),
        on(dotExperimentsListEvents.filterChanged, ({ payload }) => ({
            filter: payload,
            page: DEFAULT_EXPERIMENTS_LIST_PAGE
        })),
        on(dotExperimentsListEvents.statusesChanged, ({ payload }) => ({
            selectedStatuses: payload,
            page: DEFAULT_EXPERIMENTS_LIST_PAGE
        })),
        on(dotExperimentsListEvents.pageChanged, ({ payload }) => ({
            page: payload.page,
            perPage: payload.perPage
        })),
        on(dotExperimentsListEvents.sortChanged, ({ payload }) => ({
            orderBy: payload.orderBy,
            direction: payload.direction,
            page: DEFAULT_EXPERIMENTS_LIST_PAGE
        })),
        on(dotExperimentsListEvents.hydratedFromUrl, ({ payload }) => ({ ...payload })),
        // A site switch keeps search, sort and status selection but always restarts paging.
        on(dotExperimentsListEvents.siteChanged, () => ({
            page: DEFAULT_EXPERIMENTS_LIST_PAGE,
            status: ComponentStatus.LOADING
        })),
        on(
            dotExperimentsListEvents.archiveRequested,
            dotExperimentsListEvents.deleteRequested,
            dotExperimentsListEvents.endRequested,
            dotExperimentsListEvents.abortRequested,
            dotExperimentsListEvents.cancelScheduleRequested,
            () => ({ status: ComponentStatus.LOADING })
        ),
        // A failed action leaves the list usable instead of blanking it with an error screen.
        on(
            dotExperimentsListEvents.archiveFailed,
            dotExperimentsListEvents.deleteFailed,
            dotExperimentsListEvents.endFailed,
            dotExperimentsListEvents.abortFailed,
            dotExperimentsListEvents.cancelScheduleFailed,
            () => ({ status: ComponentStatus.LOADED })
        )
    ),
    withEventHandlers(() => {
        const events = inject(Events);
        const dispatcher = inject(Dispatcher);
        const experimentsService = inject(DotExperimentsService);
        const contentSearchService = inject(DotContentSearchService);
        const httpErrorManager = inject(DotHttpErrorManagerService);

        /**
         * Runs one row action and reloads the list on success. The confirmation has already been
         * accepted in the component by the time the `Requested` event lands here.
         */
        const runAction = ({ requested, succeeded, failed, execute }: CrudFlow) =>
            events.on(requested).pipe(
                mergeMap(({ payload }) =>
                    execute(payload).pipe(
                        tapResponse({
                            next: () => {
                                dispatcher.dispatch(succeeded(payload));
                                dispatcher.dispatch(dotExperimentsListEvents.listRequested());
                            },
                            error: (error: HttpErrorResponse) => {
                                httpErrorManager.handle(error);
                                dispatcher.dispatch(failed(error));
                            }
                        })
                    )
                )
            );

        return {
            healthCheck$: events.on(dotExperimentsListEvents.healthCheckRequested).pipe(
                switchMap(() =>
                    experimentsService.healthCheck().pipe(
                        tapResponse({
                            next: (healthStatus) => {
                                dispatcher.dispatch(
                                    dotExperimentsListEvents.healthCheckSucceeded(healthStatus)
                                );

                                // Querying experiments on a broken Analytics install is pointless.
                                if (healthStatus === HealthStatusTypes.OK) {
                                    dispatcher.dispatch(dotExperimentsListEvents.listRequested());
                                }
                            },
                            error: (error: HttpErrorResponse) => {
                                httpErrorManager.handle(error);
                                dispatcher.dispatch(
                                    dotExperimentsListEvents.healthCheckFailed(error)
                                );
                            }
                        })
                    )
                )
            ),

            loadList$: events.on(dotExperimentsListEvents.listRequested).pipe(
                switchMap(() =>
                    experimentsService.getAllUnfiltered().pipe(
                        tapResponse({
                            next: (experiments) =>
                                dispatcher.dispatch(
                                    dotExperimentsListEvents.listSucceeded(experiments)
                                ),
                            error: (error: HttpErrorResponse) => {
                                httpErrorManager.handle(error);
                                dispatcher.dispatch(dotExperimentsListEvents.listFailed(error));
                            }
                        })
                    )
                )
            ),

            resolvePageInfo$: events.on(dotExperimentsListEvents.listSucceeded).pipe(
                switchMap(({ payload }) => {
                    const pageIds = distinctPageIds(payload);

                    if (pageIds.length === 0) {
                        return EMPTY;
                    }

                    return contentSearchService
                        .get<ContentSearchEntity>({
                            query: `+contentType:htmlpageasset +working:true +identifier:(${pageIds.join(' ')})`,
                            limit: pageIds.length
                        })
                        .pipe(
                            tapResponse({
                                next: (entity) =>
                                    dispatcher.dispatch(
                                        dotExperimentsListEvents.pageInfoSucceeded(
                                            toPageInfoByPageId(
                                                entity?.jsonObjectView?.contentlets ?? []
                                            )
                                        )
                                    ),
                                error: (error: HttpErrorResponse) => {
                                    httpErrorManager.handle(error);
                                    dispatcher.dispatch(
                                        dotExperimentsListEvents.pageInfoFailed(error)
                                    );
                                }
                            })
                        );
                })
            ),

            archive$: runAction({
                requested: dotExperimentsListEvents.archiveRequested,
                succeeded: dotExperimentsListEvents.archiveSucceeded,
                failed: dotExperimentsListEvents.archiveFailed,
                execute: (experiment) => experimentsService.archive(experiment.id)
            }),

            delete$: runAction({
                requested: dotExperimentsListEvents.deleteRequested,
                succeeded: dotExperimentsListEvents.deleteSucceeded,
                failed: dotExperimentsListEvents.deleteFailed,
                execute: (experiment) => experimentsService.delete(experiment.id)
            }),

            end$: runAction({
                requested: dotExperimentsListEvents.endRequested,
                succeeded: dotExperimentsListEvents.endSucceeded,
                failed: dotExperimentsListEvents.endFailed,
                execute: (experiment) => experimentsService.stop(experiment.id)
            }),

            // There is no dedicated abort endpoint; aborting a running experiment cancels it,
            // same as the legacy per-page list store does.
            abort$: runAction({
                requested: dotExperimentsListEvents.abortRequested,
                succeeded: dotExperimentsListEvents.abortSucceeded,
                failed: dotExperimentsListEvents.abortFailed,
                execute: (experiment) => experimentsService.cancelSchedule(experiment.id)
            }),

            cancelSchedule$: runAction({
                requested: dotExperimentsListEvents.cancelScheduleRequested,
                succeeded: dotExperimentsListEvents.cancelScheduleSucceeded,
                failed: dotExperimentsListEvents.cancelScheduleFailed,
                execute: (experiment) => experimentsService.cancelSchedule(experiment.id)
            })
        };
    }),
    withHooks((store) => {
        const route = inject(ActivatedRoute);
        const location = inject(Location);
        const globalStore = inject(GlobalStore);
        const dispatcher = inject(Dispatcher);

        let siteEffect: EffectRef;
        let locationSubscription: SubscriptionLike;

        return {
            onInit() {
                // Hydrate before the first fetch so the initial render already honours the URL.
                dispatcher.dispatch(
                    dotExperimentsListEvents.hydratedFromUrl(
                        parseViewState(fromRouteParams(route.snapshot.queryParams))
                    )
                );
                // The health gate owns the first fetch: the list is only requested once
                // Analytics reports `OK`.
                dispatcher.dispatch(dotExperimentsListEvents.healthCheckRequested());

                /**
                 * Back/Forward re-hydration. The component writes the view state with
                 * `Location.go`/`replaceState` (which do not notify), so only a real popstate
                 * reaches here — re-read the restored URL and fold it back in. No reload is
                 * needed: paging, sorting and filtering are all derived client-side.
                 */
                locationSubscription = location.subscribe((event) => {
                    const params = new URLSearchParams(event.url?.split('?')[1] ?? '');

                    dispatcher.dispatch(
                        dotExperimentsListEvents.hydratedFromUrl(parseViewState(params))
                    );
                });

                // Site is resolved asynchronously, so seed with whatever is known at init and only
                // react to actual switches.
                let knownSiteId = untracked(() => globalStore.currentSiteId());

                siteEffect = effect(() => {
                    const currentSiteId = globalStore.currentSiteId();

                    if (currentSiteId === knownSiteId) {
                        return;
                    }

                    knownSiteId = currentSiteId;

                    untracked(() => {
                        dispatcher.dispatch(dotExperimentsListEvents.siteChanged(currentSiteId));

                        // Same rule as the initial load: never query experiments on an install
                        // whose Analytics app is not configured. Without this the switch would
                        // fire a request behind the misconfiguration screen.
                        if (store.healthStatus() === HealthStatusTypes.OK) {
                            dispatcher.dispatch(dotExperimentsListEvents.listRequested());
                        }
                    });
                });
            },
            onDestroy() {
                siteEffect?.destroy();
                locationSubscription?.unsubscribe();
            }
        };
    })
);

/** Injectable type of {@link DotExperimentsListStore}, for typing component/service fields. */
export type DotExperimentsListStore = InstanceType<typeof DotExperimentsListStore>;

/** One row action: which events frame it and which service call performs it. */
interface CrudFlow {
    requested: EventCreator<string, DotExperiment>;
    succeeded: EventCreator<string, DotExperiment>;
    failed: EventCreator<string, unknown>;
    execute: (experiment: DotExperiment) => Observable<unknown>;
}

/** Reads query params from either an `ActivatedRoute` snapshot or a parsed popstate URL. */
interface QueryParamReader {
    get(key: string): string | null;
    getAll(key: string): string[];
}

function fromRouteParams(params: Params): QueryParamReader {
    const values = (key: string): string[] => {
        const value: unknown = params[key];

        if (value == null) {
            return [];
        }

        return Array.isArray(value) ? value.map(String) : [String(value)];
    };

    return {
        get: (key) => values(key)[0] ?? null,
        getAll: values
    };
}

function parseViewState(reader: QueryParamReader): DotExperimentsListViewState {
    return {
        filter: reader.get('filter') ?? '',
        selectedStatuses: parseStatuses(reader.getAll('status')),
        page: parsePositiveInteger(reader.get('page'), DEFAULT_EXPERIMENTS_LIST_PAGE),
        perPage: parsePositiveInteger(reader.get('per_page'), DEFAULT_EXPERIMENTS_LIST_PER_PAGE),
        orderBy: reader.get('orderby') || DEFAULT_EXPERIMENTS_LIST_ORDER_BY,
        direction: reader.get('direction')?.toUpperCase() === 'ASC' ? 'ASC' : 'DESC'
    };
}

/**
 * An absent `status` param means "the default selection"; a present but unusable one (e.g.
 * `?status=`) means the user deselected everything, which is not the same thing.
 */
function parseStatuses(rawStatuses: string[]): DotExperimentStatus[] {
    if (rawStatuses.length === 0) {
        return DEFAULT_EXPERIMENTS_LIST_STATUSES;
    }

    const allStatuses = Object.values(DotExperimentStatus);

    return rawStatuses
        .map((rawStatus) => rawStatus.toUpperCase() as DotExperimentStatus)
        .filter((status) => allStatuses.includes(status));
}

function parsePositiveInteger(rawValue: string | null, fallback: number): number {
    const parsed = Number.parseInt(rawValue ?? '', 10);

    return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}

function distinctPageIds(experiments: DotExperiment[]): string[] {
    return [...new Set(experiments.map(({ pageId }) => pageId).filter(Boolean))];
}

function toPageInfoByPageId(
    contentlets: DotCMSContentlet[]
): Record<string, DotExperimentPageInfo> {
    return contentlets.reduce<Record<string, DotExperimentPageInfo>>((pageInfo, contentlet) => {
        if (contentlet.identifier) {
            pageInfo[contentlet.identifier] = {
                url: contentlet.url ?? '',
                host: contentlet.host ?? ''
            };
        }

        return pageInfo;
    }, {});
}

function emptyStatusCounts(): Record<DotExperimentStatus, number> {
    return Object.values(DotExperimentStatus).reduce(
        (counts, status) => {
            counts[status] = 0;

            return counts;
        },
        {} as Record<DotExperimentStatus, number>
    );
}
