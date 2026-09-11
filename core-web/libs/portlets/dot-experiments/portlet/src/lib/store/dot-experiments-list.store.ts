import { mapResponse } from '@ngrx/operators';
import { signalStore, withComputed, withHooks, withState } from '@ngrx/signals';
import { Dispatcher, Events, on, withEventHandlers, withReducer } from '@ngrx/signals/events';
import { of, SubscriptionLike } from 'rxjs';

import { Location } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { computed, effect, EffectRef, inject, untracked } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { filter, map, mergeMap, switchMap } from 'rxjs/operators';

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
    GOAL_TYPES,
    HealthStatusTypes
} from '@dotcms/dotcms-models';
import { DotExperimentsPanelStore } from '@dotcms/portlets/dot-experiments/data-access';
import { GlobalStore } from '@dotcms/store';

import { dotExperimentsApiEvents } from './dot-experiments-api.events';
import { dotExperimentsListPageEvents } from './dot-experiments-list-page.events';

import {
    DEFAULT_EXPERIMENTS_LIST_DIRECTION,
    DEFAULT_EXPERIMENTS_LIST_GOALS,
    DEFAULT_EXPERIMENTS_LIST_ORDER_BY,
    DEFAULT_EXPERIMENTS_LIST_PAGE,
    DEFAULT_EXPERIMENTS_LIST_PER_PAGE,
    DEFAULT_EXPERIMENTS_LIST_STATUSES,
    OPT_IN_STATUSES,
    PAGE_LOOKUP_LANGUAGE_HEADROOM
} from '../shared/constants';
import { DotExperimentPageInfo, DotExperimentsListViewState } from '../shared/models';
import {
    comparatorFor,
    distinctPageIds,
    emptyGoalCounts,
    emptyStatusCounts,
    fromRouteParams,
    goalTypeOfExperiment,
    normalizePagePath,
    parseViewState,
    resolvedPageInfo,
    toQueryParams
} from '../util/dot-experiments-list-store.util';
import { resolvePagePath } from '../util/dot-experiments-list.util';

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

const initialState: DotExperimentsListState = {
    status: ComponentStatus.LOADING,
    healthStatus: null,
    experiments: [],
    pageInfoByPageId: {},
    filter: '',
    selectedStatuses: DEFAULT_EXPERIMENTS_LIST_STATUSES,
    selectedGoals: DEFAULT_EXPERIMENTS_LIST_GOALS,
    page: DEFAULT_EXPERIMENTS_LIST_PAGE,
    perPage: DEFAULT_EXPERIMENTS_LIST_PER_PAGE,
    orderBy: DEFAULT_EXPERIMENTS_LIST_ORDER_BY,
    direction: DEFAULT_EXPERIMENTS_LIST_DIRECTION,
    selectedPageId: null,
    selectedPageUrl: null,
    languageId: null,
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
        const panel = inject(DotExperimentsPanelStore, { optional: true });

        /**
         * Fails closed: an experiment whose `pageId` could not be resolved is dropped, so an
         * experiment from another site can never leak into the list.
         *
         * **Bypassed entirely in the panel**, not merely left unfed. The narrowing reads
         * `pageInfoByPageId`, which the panel never fills — it skips the bulk page lookup that
         * fills it (FR-010) because it has no Page column to feed. Leaving the filter in place
         * would therefore drop every experiment for want of a host it never asked for, and the
         * screen would say "no experiments on this page" about a page that has them. There is
         * nothing to narrow anyway: the server returned the experiments *of this page*, and the
         * page is the one the editor is standing on (FR-029, SC-008).
         */
        const siteScopedExperiments = computed<DotExperiment[]>(() => {
            if (panel) {
                return store.experiments();
            }

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
                // `resolvePagePath`, not the raw url, so this searches exactly what the Page
                // column renders — including the pageId it falls back to when a page has no url.
                // Otherwise a row showing an id could not be found by typing that id.
                const pagePath = resolvePagePath(experiment.pageId, pageInfoByPageId);

                // Every field the row actually shows as text: an experiment the user can read
                // on screen should be findable by anything they can read on it.
                return (
                    experiment.name.toLowerCase().includes(term) ||
                    (experiment.description ?? '').toLowerCase().includes(term) ||
                    pagePath.toLowerCase().includes(term)
                );
            });
        });

        /**
         * Narrowed to one page, or the whole searched set when no page filter is set (#37005).
         *
         * By `pageId` **equality**, deliberately not by matching the path: `searchedExperiments`
         * above already matches the Page column as a substring, so narrowing by path would make
         * `/about` include `/about-us`. FR-021b asks for "all of that page's experiments and no
         * other page's", which only equality gives.
         *
         * Sits after the site scoping and the search, and *before* the status and goal counts, so
         * the chips describe the set the user is actually looking at.
         */
        const pageAssetFilteredExperiments = computed<DotExperiment[]>(() => {
            const pageId = store.selectedPageId();
            // Normalised on both sides of the comparison rather than trusted from the writer: the
            // address is one caller today, and a spelling difference would read as "no experiments
            // on that page" — the failure this narrowing exists to avoid.
            const pageUrl = normalizePagePath(store.selectedPageUrl());

            if (!pageId && !pageUrl) {
                return searchedExperiments();
            }

            const pageInfoByPageId = store.pageInfoByPageId();

            return searchedExperiments().filter(
                (experiment) =>
                    (!pageId || experiment.pageId === pageId) &&
                    // By path, against the same value the Page column renders — and by equality,
                    // for the reason above. A path nothing matches leaves this empty, which is the
                    // honest answer to "the experiments for that page": none, rather than all.
                    (!pageUrl ||
                        normalizePagePath(resolvePagePath(experiment.pageId, pageInfoByPageId)) ===
                            pageUrl)
            );
        });

        /**
         * Counts per status over the site + search + page filtered set, deliberately independent of
         * `selectedStatuses` so selecting a status never changes the numbers shown in the chips.
         */
        const statusCounts = computed<Record<DotExperimentStatus, number>>(() => {
            const counts = emptyStatusCounts();

            for (const experiment of pageAssetFilteredExperiments()) {
                counts[experiment.status] = (counts[experiment.status] ?? 0) + 1;
            }

            return counts;
        });

        /**
         * Counts per goal, over the same set as `statusCounts` and independent of both
         * selections for the same reason: picking a value must not move the numbers next to
         * the values you have not picked yet.
         */
        const goalCounts = computed<Record<GOAL_TYPES, number>>(() => {
            const counts = emptyGoalCounts();

            for (const experiment of pageAssetFilteredExperiments()) {
                const goal = goalTypeOfExperiment(experiment);

                if (goal) {
                    counts[goal] = (counts[goal] ?? 0) + 1;
                }
            }

            return counts;
        });

        const statusFilteredExperiments = computed<DotExperiment[]>(() => {
            const selectedStatuses = store.selectedStatuses();

            // An empty selection is "no status filter", not "match nothing" — clearing the chip
            // widens the list back out the way clearing any other filter does, rather than
            // leaving an empty table whose only escape is re-picking every status.
            // Archived stays out of that default view; it is opt-in.
            if (!selectedStatuses.length) {
                return pageAssetFilteredExperiments().filter(
                    (experiment) => !OPT_IN_STATUSES.includes(experiment.status)
                );
            }

            return pageAssetFilteredExperiments().filter((experiment) =>
                selectedStatuses.includes(experiment.status)
            );
        });

        /**
         * The two chips narrow together: an experiment has to satisfy both. An experiment with
         * no goal at all therefore drops out as soon as any goal is picked, since it matches
         * none of them.
         */
        const filteredExperiments = computed<DotExperiment[]>(() => {
            const selectedGoals = store.selectedGoals();

            if (!selectedGoals.length) {
                return statusFilteredExperiments();
            }

            return statusFilteredExperiments().filter((experiment) => {
                const goal = goalTypeOfExperiment(experiment);

                return goal !== null && selectedGoals.includes(goal);
            });
        });

        const sortedExperiments = computed<DotExperiment[]>(() => {
            const experiments = filteredExperiments();
            const compare = comparatorFor(store.orderBy(), store.pageInfoByPageId());

            // An unrecognised `orderby` keeps the API order rather than throwing.
            if (!compare) {
                return experiments;
            }

            const factor = store.direction() === 'ASC' ? 1 : -1;

            return [...experiments].sort((a, b) => compare(a, b) * factor);
        });

        const pagedExperiments = computed<DotExperiment[]>(() => {
            const perPage = store.perPage();
            const start = (store.page() - 1) * perPage;

            return sortedExperiments().slice(start, start + perPage);
        });

        return {
            siteScopedExperiments,
            searchedExperiments,
            pageAssetFilteredExperiments,
            statusCounts,
            goalCounts,
            statusFilteredExperiments,
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
        on(dotExperimentsApiEvents.healthCheckSucceeded, ({ payload }) => ({
            healthStatus: payload,
            // A non-OK gate stops the flow here: nothing else is fetched, so settle instead of
            // leaving the table stuck on its skeleton.
            status:
                payload === HealthStatusTypes.OK ? ComponentStatus.LOADING : ComponentStatus.LOADED
        })),
        on(dotExperimentsApiEvents.healthCheckFailed, ({ payload }) => ({
            status: ComponentStatus.ERROR,
            error: payload
        })),
        on(dotExperimentsListPageEvents.loadExperiments, () => ({
            status: ComponentStatus.LOADING,
            error: null
        })),
        on(dotExperimentsApiEvents.listSucceeded, ({ payload }) => ({
            experiments: payload,
            pageInfoByPageId: {},
            // Stay in `loading` until the page lookup resolves: without it the site filter fails
            // closed and the table would flash an empty "loaded" list first.
            status: payload.length > 0 ? ComponentStatus.LOADING : ComponentStatus.LOADED,
            error: null
        })),
        on(dotExperimentsApiEvents.listFailed, ({ payload }) => ({
            status: ComponentStatus.ERROR,
            experiments: [],
            pageInfoByPageId: {},
            error: payload
        })),
        on(dotExperimentsApiEvents.pageInfoSucceeded, ({ payload }) => ({
            pageInfoByPageId: payload,
            status: ComponentStatus.LOADED
        })),
        // Without page info no experiment can be attributed to a site, so this is a failed load
        // rather than an empty list.
        on(dotExperimentsApiEvents.pageInfoFailed, ({ payload }) => ({
            status: ComponentStatus.ERROR,
            pageInfoByPageId: {},
            error: payload
        })),
        on(dotExperimentsListPageEvents.filterChanged, ({ payload }) => ({
            filter: payload,
            page: DEFAULT_EXPERIMENTS_LIST_PAGE
        })),
        on(dotExperimentsListPageEvents.statusesChanged, ({ payload }) => ({
            selectedStatuses: payload,
            page: DEFAULT_EXPERIMENTS_LIST_PAGE
        })),
        on(dotExperimentsListPageEvents.goalsChanged, ({ payload }) => ({
            selectedGoals: payload,
            page: DEFAULT_EXPERIMENTS_LIST_PAGE
        })),
        on(dotExperimentsListPageEvents.pageChanged, ({ payload }) => ({
            page: payload.page,
            perPage: payload.perPage
        })),
        on(dotExperimentsListPageEvents.sortChanged, ({ payload }) => ({
            orderBy: payload.orderBy,
            direction: payload.direction,
            page: DEFAULT_EXPERIMENTS_LIST_PAGE
        })),
        on(dotExperimentsListPageEvents.hydratedFromUrl, ({ payload }) => ({ ...payload })),
        /**
         * FR-034b. The editor is on another page, so every answer on screen describes a page that
         * is no longer there — a search, a status narrowing, a sort and a page number asked about
         * the page they left. Carrying them across would silently answer the old question with the
         * new page's data, which reads as a result rather than as a leftover.
         *
         * The reset is total for that reason, and it is expressed as the defaults rather than as a
         * list of fields to clear: a field added to the view state later is reset by construction
         * instead of by remembering to add it here.
         */
        on(dotExperimentsListPageEvents.scopedToPage, ({ payload }) => ({
            filter: '',
            selectedStatuses: DEFAULT_EXPERIMENTS_LIST_STATUSES,
            selectedGoals: DEFAULT_EXPERIMENTS_LIST_GOALS,
            page: DEFAULT_EXPERIMENTS_LIST_PAGE,
            perPage: DEFAULT_EXPERIMENTS_LIST_PER_PAGE,
            orderBy: DEFAULT_EXPERIMENTS_LIST_ORDER_BY,
            direction: DEFAULT_EXPERIMENTS_LIST_DIRECTION,
            selectedPageId: payload.pageId,
            selectedPageUrl: null,
            languageId: payload.languageId,
            status: ComponentStatus.LOADING
        })),
        /**
         * A site switch keeps search, sort and status selection but always restarts paging — and
         * drops the page narrowing, which belongs to the site being left (#37005).
         *
         * The lookup behind `pageInfoByPageId` carries no host restriction, so a narrowing carried
         * across a switch resolved to a real path on the other site: the page-scoped empty state
         * rendered with that path and offered to create an experiment for a page the editor could
         * no longer see. `syncUrlEffect` follows the reset, so the address loses `pageId` too.
         */
        on(dotExperimentsListPageEvents.siteChanged, () => ({
            page: DEFAULT_EXPERIMENTS_LIST_PAGE,
            selectedPageId: null,
            selectedPageUrl: null,
            languageId: null,
            status: ComponentStatus.LOADING
        })),
        /**
         * `languageId` goes with it: it exists only to return the editor to the version of the
         * narrowed page they came from, so it means nothing once that page is gone.
         * `syncUrlEffect` follows, which is what takes the params out of the address.
         */
        on(dotExperimentsListPageEvents.pageNarrowingCleared, () => ({
            page: DEFAULT_EXPERIMENTS_LIST_PAGE,
            selectedPageId: null,
            selectedPageUrl: null,
            languageId: null
        })),
        on(
            dotExperimentsListPageEvents.archiveExperiment,
            dotExperimentsListPageEvents.deleteExperiment,
            dotExperimentsListPageEvents.endExperiment,
            dotExperimentsListPageEvents.abortExperiment,
            dotExperimentsListPageEvents.cancelScheduleExperiment,
            () => ({ status: ComponentStatus.LOADING })
        ),
        // A failed action leaves the list usable instead of blanking it with an error screen.
        on(
            dotExperimentsApiEvents.archiveFailed,
            dotExperimentsApiEvents.deleteFailed,
            dotExperimentsApiEvents.endFailed,
            dotExperimentsApiEvents.abortFailed,
            dotExperimentsApiEvents.cancelScheduleFailed,
            () => ({ status: ComponentStatus.LOADED })
        )
    ),
    withEventHandlers(
        (
            store,
            events = inject(Events),
            experimentsService = inject(DotExperimentsService),
            contentSearchService = inject(DotContentSearchService),
            httpErrorManager = inject(DotHttpErrorManagerService),
            panel = inject(DotExperimentsPanelStore, { optional: true })
        ) => {
            /**
             * Turns a failed row action into its `Failed` event, after routing the error through
             * the shared manager. Only the toast differs between actions, so this is the one
             * piece of the CRUD flows worth naming.
             */
            const toFailure =
                <T>(failed: (error: HttpErrorResponse) => T) =>
                (error: HttpErrorResponse): T => {
                    httpErrorManager.handle(error);

                    return failed(error);
                };

            return {
                healthCheck$: events.on(dotExperimentsListPageEvents.checkHealth).pipe(
                    switchMap(() =>
                        experimentsService.healthCheck().pipe(
                            mapResponse({
                                next: (healthStatus) =>
                                    dotExperimentsApiEvents.healthCheckSucceeded(healthStatus),
                                error: toFailure(dotExperimentsApiEvents.healthCheckFailed)
                            })
                        )
                    )
                ),

                // Querying experiments on a broken Analytics install is pointless, so the first
                // load hangs off the gate rather than off init.
                loadAfterHealthCheck$: events.on(dotExperimentsApiEvents.healthCheckSucceeded).pipe(
                    filter(({ payload }) => payload === HealthStatusTypes.OK),
                    map(() => dotExperimentsListPageEvents.loadExperiments())
                ),

                /**
                 * The portlet asks for everything and narrows in the browser, because its view
                 * state can name any page or none. The panel already knows the one page it is
                 * about, so it asks the server for that page and nothing else — one request
                 * instead of one plus a bulk lookup over every distinct page in the install
                 * (FR-010, D4, SC-005).
                 */
                loadList$: events.on(dotExperimentsListPageEvents.loadExperiments).pipe(
                    switchMap(() => {
                        const pageId = panel?.pageId();

                        const request$ = panel
                            ? // No page in hand yet is not an error and not an empty page: the
                              // editor's asset simply has not resolved. The re-scope effect
                              // below reloads as soon as it does.
                              pageId
                                ? experimentsService.getAll(pageId)
                                : of([])
                            : experimentsService.getAllUnfiltered();

                        return request$.pipe(
                            mapResponse({
                                next: (experiments) =>
                                    dotExperimentsApiEvents.listSucceeded(experiments),
                                error: toFailure(dotExperimentsApiEvents.listFailed)
                            })
                        );
                    })
                ),

                resolvePageInfo$: events.on(dotExperimentsApiEvents.listSucceeded).pipe(
                    switchMap(({ payload }) => {
                        // The panel has no Page column to fill and no site narrowing to feed, so
                        // this lookup has no consumer left there — and it is the most expensive
                        // request this store makes. The status still has to leave `loading`.
                        if (panel) {
                            return of(dotExperimentsApiEvents.pageInfoSucceeded({}));
                        }

                        const experimentPageIds = distinctPageIds(payload);
                        const filteredPageId = store.selectedPageId();

                        // The page the list is narrowed to resolves as well, whether or not it
                        // has experiments of its own (FR-021c). Keyed on the experiments alone,
                        // this lookup never asked for a page arriving from UVE with none — and
                        // everything the filter renders is built from that url: the chip's
                        // label, the empty state's copy, and the link back to the editor. The
                        // chip ended up claiming the page was gone on the one page where
                        // creating the first experiment is the point of the visit.
                        const pageIds =
                            filteredPageId && !experimentPageIds.includes(filteredPageId)
                                ? [...experimentPageIds, filteredPageId]
                                : experimentPageIds;

                        // Nothing to resolve, but the status still has to leave `loading`:
                        // `listSucceeded` set it there for any non-empty payload, and no other
                        // event would follow. Returning EMPTY left the skeleton spinning forever.
                        if (pageIds.length === 0) {
                            return of(dotExperimentsApiEvents.pageInfoSucceeded({}));
                        }

                        return contentSearchService
                            .get<ContentSearchEntity>({
                                // No content-type filter: the page picker offers URL-mapped
                                // content as an experiment's page, and this is the query that has
                                // to read it back for the Page column and its editor link
                                // (#37005). Identifiers come from the experiments themselves.
                                query: `+working:true +identifier:(${pageIds.join(' ')})`,
                                limit: pageIds.length * PAGE_LOOKUP_LANGUAGE_HEADROOM
                            })
                            .pipe(
                                mapResponse({
                                    next: (entity) =>
                                        dotExperimentsApiEvents.pageInfoSucceeded(
                                            // Only the experiments' own pages are checked for a
                                            // shortfall: an unresolvable filtered page hides no
                                            // experiment, and the chip has copy of its own for
                                            // a page that really was deleted.
                                            resolvedPageInfo(entity, experimentPageIds)
                                        ),
                                    error: toFailure(dotExperimentsApiEvents.pageInfoFailed)
                                })
                            );
                    })
                ),

                // Each row action is written out rather than generated from a table, so the
                // service call behind an action is readable where the action is declared.
                // `mergeMap`, not `switchMap`: acting on a second row must not cancel the first.
                // The confirmation is already accepted in the component by the time these run.
                archive$: events.on(dotExperimentsListPageEvents.archiveExperiment).pipe(
                    mergeMap(({ payload }) =>
                        experimentsService.archive(payload.id).pipe(
                            mapResponse({
                                next: () => dotExperimentsApiEvents.archiveSucceeded(payload),
                                error: toFailure(dotExperimentsApiEvents.archiveFailed)
                            })
                        )
                    )
                ),

                delete$: events.on(dotExperimentsListPageEvents.deleteExperiment).pipe(
                    mergeMap(({ payload }) =>
                        experimentsService.delete(payload.id).pipe(
                            mapResponse({
                                next: () => dotExperimentsApiEvents.deleteSucceeded(payload),
                                error: toFailure(dotExperimentsApiEvents.deleteFailed)
                            })
                        )
                    )
                ),

                end$: events.on(dotExperimentsListPageEvents.endExperiment).pipe(
                    mergeMap(({ payload }) =>
                        experimentsService.stop(payload.id).pipe(
                            mapResponse({
                                next: () => dotExperimentsApiEvents.endSucceeded(payload),
                                error: toFailure(dotExperimentsApiEvents.endFailed)
                            })
                        )
                    )
                ),

                // There is no dedicated abort endpoint; aborting a running experiment cancels it,
                // same as the legacy per-page list store does.
                abort$: events.on(dotExperimentsListPageEvents.abortExperiment).pipe(
                    mergeMap(({ payload }) =>
                        experimentsService.cancelSchedule(payload.id).pipe(
                            mapResponse({
                                next: () => dotExperimentsApiEvents.abortSucceeded(payload),
                                error: toFailure(dotExperimentsApiEvents.abortFailed)
                            })
                        )
                    )
                ),

                cancelSchedule$: events
                    .on(dotExperimentsListPageEvents.cancelScheduleExperiment)
                    .pipe(
                        mergeMap(({ payload }) =>
                            experimentsService.cancelSchedule(payload.id).pipe(
                                mapResponse({
                                    next: () =>
                                        dotExperimentsApiEvents.cancelScheduleSucceeded(payload),
                                    error: toFailure(dotExperimentsApiEvents.cancelScheduleFailed)
                                })
                            )
                        )
                    ),

                // Every row action mutates the server-side list, so all five reload through the
                // same path instead of each repeating the request.
                reloadAfterAction$: events
                    .on(
                        dotExperimentsApiEvents.archiveSucceeded,
                        dotExperimentsApiEvents.deleteSucceeded,
                        dotExperimentsApiEvents.endSucceeded,
                        dotExperimentsApiEvents.abortSucceeded,
                        dotExperimentsApiEvents.cancelScheduleSucceeded
                    )
                    .pipe(map(() => dotExperimentsListPageEvents.loadExperiments()))
            };
        }
    ),
    withHooks((store) => {
        const route = inject(ActivatedRoute);
        const router = inject(Router);
        const location = inject(Location);
        const globalStore = inject(GlobalStore);
        const dispatcher = inject(Dispatcher);
        /**
         * Present only inside the UVE panel (#37478). Its presence — nothing else — is what makes
         * this store panel-scoped; the portlet never provides it.
         */
        const panel = inject(DotExperimentsPanelStore, { optional: true });

        let siteEffect: EffectRef;
        let syncUrlEffect: EffectRef;
        let rescopeEffect: EffectRef;
        let locationSubscription: SubscriptionLike;

        /**
         * `Location.go` rather than `Router.navigate`: the view state is derived client-side, so
         * a filter change must not re-run the route. The guard keeps a no-op write (most notably
         * the one this effect triggers on its own hydration) from pushing a duplicate history
         * entry.
         */
        const writeUrl = (queryParams: Record<string, string | string[] | null>): void => {
            const newUrl = router
                .createUrlTree([], { queryParams, queryParamsHandling: 'merge' })
                .toString();

            if (newUrl !== location.path(true)) {
                location.go(newUrl);
            }
        };

        /**
         * The four concerns below are the ones bound to the browser address, and all four are the
         * portlet's alone.
         *
         * In the panel the address belongs to the editor: it names the page being edited, its
         * language, its device and its preview mode. A list that hydrated from it would read the
         * editor's params as its own view state; one that wrote to it would push history entries
         * for a filter change, so Back would step through the panel instead of leaving the page
         * (FR-031, FR-032, FR-035). Split into named units rather than left as one block, because
         * "which of these runs in the panel" is the question this file now has to answer.
         */
        const hydrateFromUrl = (): void => {
            // Hydrate before the first fetch so the initial render already honours the URL.
            dispatcher.dispatch(
                dotExperimentsListPageEvents.hydratedFromUrl(
                    parseViewState(fromRouteParams(route.snapshot.queryParams))
                )
            );
        };

        /**
         * Back/Forward re-hydration. `writeUrl` uses `Location.go` (which does not notify), so
         * only a real popstate reaches here — re-read the restored URL and fold it back in. No
         * reload is needed: paging, sorting and filtering are all derived client-side.
         */
        const subscribeToPopstate = (): void => {
            locationSubscription = location.subscribe((event) => {
                const params = new URLSearchParams(event.url?.split('?')[1] ?? '');

                dispatcher.dispatch(
                    dotExperimentsListPageEvents.hydratedFromUrl(parseViewState(params))
                );
            });
        };

        /**
         * Mirrors the view state back into the URL, so the list is shareable and survives a
         * reload. Lives here rather than in the component because the store already owns the other
         * half of this contract — it parses the URL on entry and on popstate — and splitting read
         * from write invites the two to drift.
         */
        const mirrorViewStateToUrl = (): void => {
            syncUrlEffect = effect(() => {
                const queryParams = toQueryParams({
                    filter: store.filter(),
                    selectedStatuses: store.selectedStatuses(),
                    selectedGoals: store.selectedGoals(),
                    page: store.page(),
                    perPage: store.perPage(),
                    selectedPageId: store.selectedPageId(),
                    selectedPageUrl: store.selectedPageUrl(),
                    languageId: store.languageId(),
                    orderBy: store.orderBy(),
                    direction: store.direction()
                });

                untracked(() => writeUrl(queryParams));
            });
        };

        /**
         * The site the portlet is scoped to changed, so the list is about a different set of
         * pages. The panel is scoped to one page instead, which is on the current site by
         * construction — it is the page open in the editor — so a site switch cannot reach it.
         */
        const reloadOnSiteChange = (): void => {
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
                    dispatcher.dispatch(dotExperimentsListPageEvents.siteChanged(currentSiteId));

                    // Same rule as the initial load: never query experiments on an install whose
                    // Analytics app is not configured. Without this the switch would fire a
                    // request behind the misconfiguration screen.
                    if (store.healthStatus() === HealthStatusTypes.OK) {
                        dispatcher.dispatch(dotExperimentsListPageEvents.loadExperiments());
                    }
                });
            });
        };

        /**
         * The panel's equivalent of the two above: it follows the editor's page instead of the
         * address and the site.
         *
         * Reads **only** `pageId`. A language change therefore cannot reach it by construction
         * rather than by a guard someone could later remove — which is the point, because
         * refetching on a language change would re-request a provably identical set, and resetting
         * would discard the editor's view state for no change in the answer (FR-034a, D10). The
         * language is still carried into the state, as return context for the variant round trip.
         */
        const followTheEditorsPage = (): void => {
            let knownPageId: string | null | undefined;

            rescopeEffect = effect(() => {
                const pageId = panel?.pageId() ?? null;

                if (pageId === knownPageId) {
                    return;
                }

                const isFirstScope = knownPageId === undefined;
                knownPageId = pageId;

                untracked(() => {
                    dispatcher.dispatch(
                        dotExperimentsListPageEvents.scopedToPage({
                            pageId,
                            languageId: panel?.languageId() ?? null
                        })
                    );

                    // The first scope is followed by the health gate, which owns the first fetch.
                    // Every later one is a page the editor navigated to, and the gate has already
                    // answered — so this is the only thing that would reload the list.
                    if (!isFirstScope && store.healthStatus() === HealthStatusTypes.OK) {
                        dispatcher.dispatch(dotExperimentsListPageEvents.loadExperiments());
                    }
                });
            });
        };

        return {
            onInit() {
                if (panel) {
                    followTheEditorsPage();
                } else {
                    hydrateFromUrl();
                }

                // The health gate owns the first fetch in both modes: the list is only requested
                // once Analytics reports `OK`.
                dispatcher.dispatch(dotExperimentsListPageEvents.checkHealth());

                if (panel) {
                    return;
                }

                subscribeToPopstate();
                mirrorViewStateToUrl();
                reloadOnSiteChange();
            },
            onDestroy() {
                siteEffect?.destroy();
                syncUrlEffect?.destroy();
                rescopeEffect?.destroy();
                locationSubscription?.unsubscribe();
            }
        };
    })
);

/** Injectable type of {@link DotExperimentsListStore}, for typing component/service fields. */
export type DotExperimentsListStore = InstanceType<typeof DotExperimentsListStore>;
