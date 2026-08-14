import { mapResponse } from '@ngrx/operators';
import { signalStore, withComputed, withHooks, withState } from '@ngrx/signals';
import { Dispatcher, Events, on, withEventHandlers, withReducer } from '@ngrx/signals/events';
import { merge, Observable, of, SubscriptionLike } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { computed, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import {
    catchError,
    debounceTime,
    distinctUntilChanged,
    filter,
    map,
    mergeMap,
    switchMap
} from 'rxjs/operators';

import {
    DotContentSearchService,
    DotExperimentsService,
    DotHttpErrorManagerService,
    DotMessageService,
    DotPageBrowserPage,
    DotPagesBrowserService
} from '@dotcms/data-access';
import {
    ComponentStatus,
    DotCMSContentlet,
    DotExperiment,
    DotExperimentStatus,
    EXP_CONFIG_ERROR_LABEL_CANT_EDIT,
    EXP_CONFIG_ERROR_LABEL_PAGE_BLOCKED,
    GOAL_OPERATORS,
    GOAL_TYPES,
    Goals,
    ReachPageGoalCondition,
    TrafficProportion,
    TrafficProportionTypes,
    UrlParameterGoalCondition,
    Variant
} from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';

import { dotExperimentsConfigureApiEvents } from './dot-experiments-configure-api.events';
import {
    ConfigurePagePrefill,
    dotExperimentsConfigurePageEvents
} from './dot-experiments-configure-page.events';

import { AUTOSAVE_DEBOUNCE_MS } from '../shared/constants';
import {
    ConfigureValidationRule,
    DotExperimentConfigurePage,
    DotExperimentsConfigureViewState,
    ExperimentFieldGroup,
    ExperimentListAction
} from '../shared/models';
import { isAllowed } from '../util/dot-experiments-list.util';

const pageEvents = dotExperimentsConfigurePageEvents;
const apiEvents = dotExperimentsConfigureApiEvents;

/** Read-only banner copy while an experiment is running, which is not the generic one (AC35). */
export const LOCKED_BANNER_KEY_RUNNING = 'experiments.configure.locked.running';

/** Read-only banner copy for every other non-DRAFT status. */
export const LOCKED_BANNER_KEY_READ_ONLY = 'experiments.configure.locked.read-only';

/** Page card's inline error when `?pageId=`/`?url=` named a page that could not be resolved. */
export const PAGE_PREFILL_ERROR_KEY = 'experiments.configure.page.prefill.not-found';

/** Fallback header the old screen supplies when the backend rejects a start with no header of its own. */
const START_ERROR_HEADER_KEY = 'dot.common.http.error.400.experiment.run-scheduling-error.header';

/** Total the variant weights must add up to. */
const TOTAL_WEIGHT = 100;

/** Weights are stored as percentages with two decimals, so compare at that resolution. */
const WEIGHT_PRECISION = 100;

const initialState: DotExperimentsConfigureViewState = {
    experiment: null,
    status: ComponentStatus.INIT,
    isNew: true,
    creating: false,
    starting: false,
    draftName: '',
    draftDescription: '',
    selectedPage: null,
    pagePrefillError: null,
    pageLockInfo: null,
    validationErrors: [],
    pendingFieldGroups: []
};

/** Shape of the `/api/content/_search` entity the page lookup reads contentlets from. */
interface PageLookupEntity {
    jsonObjectView?: { contentlets?: DotCMSContentlet[] };
}

/** What an autosave handler reports when it settles a field group without calling the API. */
type AutosaveSkipped = ReturnType<typeof apiEvents.autosaveSkipped>;

/**
 * Splits 100% across the variants: `floor(100/n)` each, with the remainder absorbed by the first
 * one so the total is exactly 100 for any variant count (AC23).
 */
export function splitWeightsEvenly(variants: Variant[]): Variant[] {
    if (!variants.length) {
        return variants;
    }

    const share = Math.floor(TOTAL_WEIGHT / variants.length);
    const remainder = TOTAL_WEIGHT - share * variants.length;

    return variants.map((variant, index) => ({
        ...variant,
        weight: index === 0 ? share + remainder : share
    }));
}

/** Sum of the variant weights, rounded to the precision they are stored at. */
export function totalWeight(variants: Variant[]): number {
    const total = variants.reduce((sum, { weight }) => sum + (weight ?? 0), 0);

    return Math.round(total * WEIGHT_PRECISION) / WEIGHT_PRECISION;
}

/**
 * The eight rules Start/Schedule checks, in the order the screen reads top to bottom — which is
 * also the order the shell scrolls through to find the first failing field.
 *
 * Nothing here runs before Start is pressed (AC28), so this is a plain function over the state
 * rather than a computed: materialising it early is exactly what the screen must not do.
 */
export function validateConfigure(
    state: Pick<DotExperimentsConfigureViewState, 'draftName' | 'selectedPage' | 'experiment'>
): ConfigureValidationRule[] {
    const errors: ConfigureValidationRule[] = [];
    const experiment = state.experiment;
    const goal = experiment?.goals?.primary ?? null;

    if (!state.draftName.trim()) {
        errors.push('name');
    }

    if (!state.selectedPage && !experiment?.pageId) {
        errors.push('page');
    }

    if (!goal?.type) {
        errors.push('goalType');
    }

    if (!goal?.name?.trim()) {
        errors.push('goalName');
    }

    errors.push(...validateGoalCondition(goal?.type, goal?.conditions?.[0]));

    const variants = experiment?.trafficProportion?.variants ?? [];

    if (variants.length < 2) {
        errors.push('minVariants');
    }

    if (variants.length && totalWeight(variants) !== TOTAL_WEIGHT) {
        errors.push('weightsTotal');
    }

    return errors;
}

/**
 * Condition rules for the two goal types that have conditions. BOUNCE_RATE and EXIT_RATE have no
 * server-side conditions, so they are complete without one.
 */
function validateGoalCondition(
    goalType: GOAL_TYPES | undefined,
    condition: ReachPageGoalCondition | UrlParameterGoalCondition | undefined
): ConfigureValidationRule[] {
    if (goalType === GOAL_TYPES.REACH_PAGE) {
        const value = condition?.value as string | undefined;

        return value?.trim() ? [] : ['goalConditionValue'];
    }

    if (goalType !== GOAL_TYPES.URL_PARAMETER) {
        return [];
    }

    const value = condition?.value as { name?: string; value?: string } | undefined;
    const errors: ConfigureValidationRule[] = [];

    // EXISTS only asks whether the parameter is there, so it needs no value — the name it looks
    // for is still required.
    if (condition?.operator !== GOAL_OPERATORS.EXISTS && !value?.value?.trim()) {
        errors.push('goalConditionValue');
    }

    if (!value?.name?.trim()) {
        errors.push('goalParameterName');
    }

    return errors;
}

function addPendingGroup(
    groups: ExperimentFieldGroup[],
    group: ExperimentFieldGroup
): ExperimentFieldGroup[] {
    return groups.includes(group) ? groups : [...groups, group];
}

function removePendingGroup(
    groups: ExperimentFieldGroup[],
    group: ExperimentFieldGroup
): ExperimentFieldGroup[] {
    return groups.filter((pending) => pending !== group);
}

function toConfigurePage(contentlet: DotCMSContentlet): DotExperimentConfigurePage {
    const path = contentlet.url ?? '';

    return {
        pageId: contentlet.identifier,
        title: contentlet.title || path,
        path
    };
}

function fromBrowserPage(page: DotPageBrowserPage): DotExperimentConfigurePage {
    return {
        pageId: page.identifier,
        title: page.title,
        path: page.path || page.url
    };
}

/** Trailing slashes and casing are not part of the identity of a page path. */
function normalizePath(path: string): string {
    return path.trim().toLowerCase().replace(/\/+$/, '');
}

/**
 * Store for the Configure screen, on both `/experiments/new` and
 * `/experiments/:experimentId/configuration`.
 *
 * There is no "save" button: the first Name + Page combination POSTs the draft (once — the URL is
 * then swapped for the created one) and every later change is persisted by a debounced PATCH.
 * `PATCH /api/v1/experiments/{id}` takes a single-key body, so each field group has its own
 * handler and its own timer: rapid edits to one group collapse into one call (`switchMap`), while
 * an edit to another group in the same window still fires its own (AC6). `targetingConditions` is
 * never part of any body — the Targeting card is out of scope, and sending it would have the
 * backend rebuild the rule (AC7).
 *
 * The page is chosen once: the PATCH endpoint does not accept `pageId`, so after creation the
 * selected page is only ever *resolved* from the experiment, never sent back.
 *
 * Validation is deliberately absent until Start/Schedule is pressed (AC28): the reducer for
 * `startRequested` is what materialises `validationErrors`, and the handler only calls the API
 * when that came back empty.
 *
 * State only ever changes through dispatched events (`withReducer`); the store exposes no
 * mutating methods and never opens UI — confirmations and toasts belong to the shell.
 *
 * Not provided in root: supply it in the Configure shell's `providers` together with
 * `DotExperimentsService` and `DotPagesBrowserService`.
 */
export const DotExperimentsConfigureStore = signalStore(
    withState<DotExperimentsConfigureViewState>(initialState),
    withComputed((store) => {
        const globalStore = inject(GlobalStore);

        /** A screen with no experiment yet is a draft: nothing on it is locked. */
        const $status = computed<DotExperimentStatus>(
            () => store.experiment()?.status ?? DotExperimentStatus.DRAFT
        );

        /**
         * Any status but DRAFT is read-only — SCHEDULED included, matching the old screen. The
         * kebab still offers its status-specific actions; it is the *fields* that are frozen.
         */
        const $isLocked = computed<boolean>(() => $status() !== DotExperimentStatus.DRAFT);

        const $lockedByAnotherUser = computed<boolean>(() => {
            const lock = store.pageLockInfo();

            if (!lock?.locked || !lock.lockedBy) {
                return false;
            }

            return lock.lockedBy !== globalStore.loggedUser()?.userId;
        });

        const $variants = computed<Variant[]>(
            () => store.experiment()?.trafficProportion?.variants ?? []
        );

        const $totalWeight = computed<number>(() => totalWeight($variants()));

        return {
            $status,
            $isLocked,
            $lockedByAnotherUser,
            $variants,
            $totalWeight,
            /** Warning bar condition (AC25). An experiment with no variants yet is not "wrong". */
            $hasInvalidWeights: computed<boolean>(
                () => $variants().length > 0 && $totalWeight() !== TOTAL_WEIGHT
            ),
            /** `null` while the screen is editable, so the shell can branch on its presence. */
            $lockedBannerKey: computed<string | null>(() => {
                if (!$isLocked()) {
                    return null;
                }

                return $status() === DotExperimentStatus.RUNNING
                    ? LOCKED_BANNER_KEY_RUNNING
                    : LOCKED_BANNER_KEY_READ_ONLY;
            }),
            /**
             * Same precedence as the old screen: a non-DRAFT status explains the disabled state
             * before a page lock does, since it is the stronger reason.
             */
            $disabledTooltipKey: computed<string | null>(() => {
                if ($isLocked()) {
                    return EXP_CONFIG_ERROR_LABEL_CANT_EDIT;
                }

                return $lockedByAnotherUser() ? EXP_CONFIG_ERROR_LABEL_PAGE_BLOCKED : null;
            }),
            /** Kebab gating, straight off `AllowedActionsByExperimentStatus`. No license gates. */
            $allowedActions: computed<Record<ExperimentListAction, boolean>>(() => {
                const status = $status();

                return {
                    delete: isAllowed('delete', status),
                    abort: isAllowed('abort', status),
                    results: isAllowed('results', status),
                    configuration: isAllowed('configuration', status),
                    archive: isAllowed('archive', status),
                    end: isAllowed('end', status),
                    addToBundle: isAllowed('addToBundle', status),
                    pushPublish: isAllowed('pushPublish', status),
                    cancelSchedule: isAllowed('cancelSchedule', status)
                };
            }),
            /** A start date in the future schedules the experiment instead of starting it (AC32). */
            $isScheduledStart: computed<boolean>(() => {
                const startDate = store.experiment()?.scheduling?.startDate;

                return !!startDate && startDate > Date.now();
            }),
            $validationErrorCount: computed<number>(() => store.validationErrors().length),
            $isSaving: computed<boolean>(
                () => store.status() === ComponentStatus.SAVING || store.creating()
            ),
            /** True while any field group has a PATCH debounced or in flight. */
            $isAutosaving: computed<boolean>(() => store.pendingFieldGroups().length > 0)
        };
    }),
    withReducer<DotExperimentsConfigureViewState>(
        on(pageEvents.enterNew, () => ({ ...initialState, status: ComponentStatus.LOADED })),
        on(pageEvents.enterExisting, () => ({
            isNew: false,
            status: ComponentStatus.LOADING
        })),
        on(apiEvents.loadSucceeded, ({ payload }) => ({
            experiment: payload,
            isNew: false,
            draftName: payload.name,
            draftDescription: payload.description ?? '',
            status: ComponentStatus.LOADED
        })),
        on(apiEvents.loadFailed, () => ({ status: ComponentStatus.ERROR })),

        // Creation: the flag closes the door on a second POST as the first one leaves.
        on(apiEvents.createRequested, () => ({ creating: true })),
        on(apiEvents.createSucceeded, ({ payload }) => ({
            experiment: payload,
            isNew: false,
            creating: false,
            draftName: payload.name,
            draftDescription: payload.description ?? '',
            status: ComponentStatus.LOADED
        })),
        // The user stays on `/experiments/new` with everything they typed still there (AC4).
        on(apiEvents.createFailed, () => ({ creating: false })),

        on(pageEvents.pageSelected, ({ payload }, state) =>
            // The page is immutable once the experiment exists, so a late selection is ignored
            // rather than silently diverging from what the server holds.
            state.experiment ? {} : { selectedPage: payload, pagePrefillError: null }
        ),
        on(apiEvents.pagePrefillResolved, ({ payload }) => ({
            selectedPage: payload,
            pagePrefillError: null
        })),
        // An unresolvable `?url=`/`?pageId=` leaves the page unselected and says so inline (AC15).
        on(apiEvents.pagePrefillFailed, () => ({
            selectedPage: null,
            pagePrefillError: PAGE_PREFILL_ERROR_KEY
        })),
        on(apiEvents.pageLockResolved, ({ payload }) => ({ pageLockInfo: payload })),

        // Field groups. Each change is applied locally at once — the PATCH is debounced, and the
        // card must not lag half a second behind the keystroke that caused it.
        on(pageEvents.nameChanged, ({ payload }, state) => ({
            draftName: payload,
            pendingFieldGroups:
                state.experiment && payload.trim()
                    ? addPendingGroup(state.pendingFieldGroups, 'name')
                    : state.pendingFieldGroups
        })),
        on(pageEvents.descriptionChanged, ({ payload }, state) => ({
            draftDescription: payload,
            pendingFieldGroups: state.experiment
                ? addPendingGroup(state.pendingFieldGroups, 'description')
                : state.pendingFieldGroups
        })),
        on(pageEvents.goalChanged, ({ payload }, state) =>
            state.experiment
                ? {
                      experiment: { ...state.experiment, goals: payload },
                      pendingFieldGroups: addPendingGroup(state.pendingFieldGroups, 'goal')
                  }
                : {}
        ),
        on(pageEvents.schedulingChanged, ({ payload }, state) =>
            state.experiment
                ? {
                      experiment: { ...state.experiment, scheduling: payload },
                      pendingFieldGroups: addPendingGroup(state.pendingFieldGroups, 'scheduling')
                  }
                : {}
        ),
        on(pageEvents.trafficAllocationChanged, ({ payload }, state) =>
            state.experiment
                ? {
                      experiment: { ...state.experiment, trafficAllocation: payload },
                      pendingFieldGroups: addPendingGroup(
                          state.pendingFieldGroups,
                          'trafficAllocation'
                      )
                  }
                : {}
        ),
        on(pageEvents.trafficProportionChanged, ({ payload }, state) =>
            state.experiment
                ? {
                      experiment: { ...state.experiment, trafficProportion: payload },
                      pendingFieldGroups: addPendingGroup(
                          state.pendingFieldGroups,
                          'trafficProportion'
                      )
                  }
                : {}
        ),

        // Autosave outcomes. The response is the source of truth after any write, so the whole
        // experiment is replaced rather than the one key that was sent.
        //
        // A group whose handler decided there was nothing to send settles the same way as one that
        // was written: it is not pending any more, and only its handler knows a call was never
        // made — which is why the handler says so rather than the reducer guessing (#37003).
        on(apiEvents.autosaveSkipped, ({ payload }, state) => ({
            pendingFieldGroups: removePendingGroup(state.pendingFieldGroups, payload)
        })),
        on(apiEvents.nameSucceeded, ({ payload }, state) => ({
            experiment: payload,
            pendingFieldGroups: removePendingGroup(state.pendingFieldGroups, 'name')
        })),
        on(apiEvents.nameFailed, (_event, state) => ({
            pendingFieldGroups: removePendingGroup(state.pendingFieldGroups, 'name')
        })),
        on(apiEvents.descriptionSucceeded, ({ payload }, state) => ({
            experiment: payload,
            pendingFieldGroups: removePendingGroup(state.pendingFieldGroups, 'description')
        })),
        on(apiEvents.descriptionFailed, (_event, state) => ({
            pendingFieldGroups: removePendingGroup(state.pendingFieldGroups, 'description')
        })),
        on(apiEvents.goalSucceeded, ({ payload }, state) => ({
            experiment: payload,
            pendingFieldGroups: removePendingGroup(state.pendingFieldGroups, 'goal')
        })),
        on(apiEvents.goalFailed, (_event, state) => ({
            pendingFieldGroups: removePendingGroup(state.pendingFieldGroups, 'goal')
        })),
        on(apiEvents.schedulingSucceeded, ({ payload }, state) => ({
            experiment: payload,
            pendingFieldGroups: removePendingGroup(state.pendingFieldGroups, 'scheduling')
        })),
        on(apiEvents.schedulingFailed, (_event, state) => ({
            pendingFieldGroups: removePendingGroup(state.pendingFieldGroups, 'scheduling')
        })),
        on(apiEvents.trafficAllocationSucceeded, ({ payload }, state) => ({
            experiment: payload,
            pendingFieldGroups: removePendingGroup(state.pendingFieldGroups, 'trafficAllocation')
        })),
        on(apiEvents.trafficAllocationFailed, (_event, state) => ({
            pendingFieldGroups: removePendingGroup(state.pendingFieldGroups, 'trafficAllocation')
        })),
        on(apiEvents.trafficProportionSucceeded, ({ payload }, state) => ({
            experiment: payload,
            pendingFieldGroups: removePendingGroup(state.pendingFieldGroups, 'trafficProportion')
        })),
        on(apiEvents.trafficProportionFailed, (_event, state) => ({
            pendingFieldGroups: removePendingGroup(state.pendingFieldGroups, 'trafficProportion')
        })),

        // Variants have their own endpoints, each answering with the recomputed proportion.
        on(
            apiEvents.addVariantSucceeded,
            apiEvents.editVariantSucceeded,
            apiEvents.removeVariantSucceeded,
            ({ payload }) => ({ experiment: payload, status: ComponentStatus.LOADED })
        ),
        on(pageEvents.variantAdded, pageEvents.variantRenamed, pageEvents.variantDeleted, () => ({
            status: ComponentStatus.SAVING
        })),
        on(
            apiEvents.addVariantFailed,
            apiEvents.editVariantFailed,
            apiEvents.removeVariantFailed,
            () => ({ status: ComponentStatus.LOADED })
        ),

        /**
         * The only place validation errors are ever produced (AC28/AC29). Pressing Start with a
         * complete form leaves them empty, which is what lets the handler through to the API.
         */
        on(pageEvents.startRequested, (_event, state) => {
            const validationErrors = validateConfigure(state);

            return {
                validationErrors,
                status: validationErrors.length ? state.status : ComponentStatus.SAVING
            };
        }),
        // Same contract as creation: the flag closes the door on a second start as the first
        // call leaves, so an impatient double press cannot run the experiment twice.
        on(apiEvents.startRequested, () => ({ starting: true })),
        on(
            pageEvents.stopRequested,
            pageEvents.cancelScheduleRequested,
            pageEvents.abortRequested,
            () => ({ status: ComponentStatus.SAVING })
        ),
        on(
            apiEvents.startSucceeded,
            apiEvents.stopSucceeded,
            apiEvents.cancelScheduleSucceeded,
            apiEvents.abortSucceeded,
            ({ payload }) => ({
                experiment: payload,
                validationErrors: [],
                starting: false,
                status: ComponentStatus.LOADED
            })
        ),
        // A failed transition leaves the screen usable rather than blanking it with an error.
        on(
            apiEvents.startFailed,
            apiEvents.stopFailed,
            apiEvents.cancelScheduleFailed,
            apiEvents.abortFailed,
            () => ({ starting: false, status: ComponentStatus.LOADED })
        )
    ),
    withEventHandlers(
        (
            store,
            events = inject(Events),
            experimentsService = inject(DotExperimentsService),
            pagesBrowserService = inject(DotPagesBrowserService),
            contentSearchService = inject(DotContentSearchService),
            httpErrorManager = inject(DotHttpErrorManagerService),
            dotMessageService = inject(DotMessageService),
            globalStore = inject(GlobalStore)
        ) => {
            /** Routes a failed call through the shared manager, then reports it as its event. */
            const toFailure =
                <T>(failed: (error: HttpErrorResponse) => T) =>
                (error: HttpErrorResponse): T => {
                    httpErrorManager.handle(error);

                    return failed(error);
                };

            /**
             * One debounced PATCH for one field group.
             *
             * `switchMap`, not `mergeMap`: a second edit to the *same* group replaces the call
             * that is already going out rather than queuing behind it — that is what "collapses
             * into a single call" means. Each group calls this separately, so their timers are
             * independent of one another.
             *
             * Every emission that gets through the debounce settles the group, whether or not it
             * results in a call: the reducer marked the group pending on the edit, and the only
             * thing that can unmark it is an event from here.
             */
            const autosave = <T, S, F>(
                group: ExperimentFieldGroup,
                source: Observable<{ payload: T }>,
                call: (experimentId: string, payload: T) => Observable<DotExperiment>,
                succeeded: (experiment: DotExperiment) => S,
                failed: (error: HttpErrorResponse) => F,
                canSend: (payload: T, experiment: DotExperiment) => boolean = () => true
            ): Observable<S | F | AutosaveSkipped> =>
                source.pipe(
                    debounceTime(AUTOSAVE_DEBOUNCE_MS),
                    switchMap(({ payload }) => {
                        const experiment = store.experiment();

                        // Nothing to patch before the draft exists: those values reach the
                        // server through the creation POST instead.
                        if (!experiment || !canSend(payload, experiment)) {
                            return of(apiEvents.autosaveSkipped(group));
                        }

                        return call(experiment.id, payload).pipe(
                            mapResponse({ next: succeeded, error: toFailure(failed) })
                        );
                    })
                );

            /** Resolves `?pageId=` / `?url=` to the page the Page card shows. */
            const resolvePrefill = ({ pageId, url }: ConfigurePagePrefill) => {
                if (pageId) {
                    // The page-search endpoint filters by path only, so an identifier is
                    // resolved with the same content search the list uses for its Page column.
                    return contentSearchService
                        .get<PageLookupEntity>({
                            query: `+contentType:htmlpageasset +working:true +identifier:${pageId}`,
                            limit: 1
                        })
                        .pipe(
                            map((entity) => entity?.jsonObjectView?.contentlets?.[0]),
                            map((contentlet) =>
                                contentlet
                                    ? apiEvents.pagePrefillResolved(toConfigurePage(contentlet))
                                    : apiEvents.pagePrefillFailed(pageId)
                            ),
                            catchError((error) => of(apiEvents.pagePrefillFailed(error)))
                        );
                }

                if (!url) {
                    return of(apiEvents.pagePrefillFailed(null));
                }

                const wanted = normalizePath(url);

                return pagesBrowserService
                    .searchPages({
                        hostname: globalStore.siteDetails()?.hostname,
                        path: url
                    })
                    .pipe(
                        map((pages) =>
                            pages.find(
                                (page) =>
                                    normalizePath(page.path) === wanted ||
                                    normalizePath(page.url) === wanted
                            )
                        ),
                        map((page) =>
                            page
                                ? apiEvents.pagePrefillResolved(fromBrowserPage(page))
                                : apiEvents.pagePrefillFailed(url)
                        ),
                        catchError((error) => of(apiEvents.pagePrefillFailed(error)))
                    );
            };

            return {
                load$: events.on(pageEvents.enterExisting).pipe(
                    switchMap(({ payload }) =>
                        experimentsService.getById(payload).pipe(
                            mapResponse({
                                next: (experiment) =>
                                    experiment
                                        ? apiEvents.loadSucceeded(experiment)
                                        : apiEvents.loadFailed(payload),
                                error: toFailure(apiEvents.loadFailed)
                            })
                        )
                    )
                ),

                /**
                 * Creation is the one call with no debounce: the draft exists the moment a name
                 * and a page are both there (AC2).
                 *
                 * A second POST is stopped by the `creating` filter, not by the flattening
                 * operator: the flag is raised by `createRequested` as the first request leaves,
                 * so an edit arriving mid-flight is dropped here and never reaches the
                 * `switchMap` — which would otherwise cancel the POST already creating the draft.
                 */
                create$: merge(
                    events.on(pageEvents.nameChanged),
                    events.on(pageEvents.pageSelected)
                ).pipe(
                    filter(() => store.isNew() && !store.creating()),
                    map(() => ({
                        name: store.draftName().trim(),
                        description: store.draftDescription(),
                        page: store.selectedPage()
                    })),
                    filter(({ name, page }) => !!name && !!page),
                    switchMap(({ name, description, page }) =>
                        merge(
                            of(apiEvents.createRequested()),
                            experimentsService
                                .add({
                                    pageId: page?.pageId ?? '',
                                    name,
                                    description
                                })
                                .pipe(
                                    mapResponse({
                                        next: (experiment) => apiEvents.createSucceeded(experiment),
                                        error: toFailure(apiEvents.createFailed)
                                    })
                                )
                        )
                    )
                ),

                // A loaded experiment carries a pageId but no page title or path, so the same
                // prefill path resolves them — and, through it, the page's lock state.
                resolvePageOfExperiment$: events
                    .on(apiEvents.loadSucceeded)
                    .pipe(
                        map(({ payload }) =>
                            pageEvents.pagePrefillRequested({ pageId: payload.pageId })
                        )
                    ),

                prefillPage$: events
                    .on(pageEvents.pagePrefillRequested)
                    .pipe(switchMap(({ payload }) => resolvePrefill(payload))),

                /**
                 * Lock state is ancillary: a failed lookup reports the page as unlocked rather
                 * than blocking a screen that is otherwise fully usable.
                 */
                resolvePageLock$: merge(
                    events.on(pageEvents.pageSelected).pipe(map(({ payload }) => payload.pageId)),
                    events
                        .on(apiEvents.pagePrefillResolved)
                        .pipe(map(({ payload }) => payload.pageId))
                ).pipe(
                    filter((pageId): pageId is string => !!pageId),
                    distinctUntilChanged(),
                    switchMap((pageId) =>
                        pagesBrowserService.getPageLockState(pageId).pipe(
                            map((lockInfo) => apiEvents.pageLockResolved(lockInfo)),
                            catchError(() => of(apiEvents.pageLockResolved({ locked: false })))
                        )
                    )
                ),

                // One handler per field group: two groups edited in the same tick must reach the
                // server as two calls, since no endpoint accepts them together.
                autosaveName$: autosave(
                    'name',
                    events.on(pageEvents.nameChanged),
                    (experimentId, name: string) => experimentsService.setName(experimentId, name),
                    apiEvents.nameSucceeded,
                    apiEvents.nameFailed,
                    // A blank name is rejected by the backend, and the name the creation POST
                    // just sent does not need sending again.
                    (name, experiment) => !!name.trim() && name !== experiment.name
                ),

                autosaveDescription$: autosave(
                    'description',
                    events.on(pageEvents.descriptionChanged),
                    (experimentId, description: string) =>
                        experimentsService.setDescription(experimentId, description),
                    apiEvents.descriptionSucceeded,
                    apiEvents.descriptionFailed,
                    (description, experiment) => description !== experiment.description
                ),

                autosaveGoal$: autosave(
                    'goal',
                    events.on(pageEvents.goalChanged),
                    (experimentId, goals: Goals | null) =>
                        // `setGoal` sends `{ goals }` and nothing else: `targetingConditions` is
                        // never part of an outgoing body (AC7).
                        experimentsService.setGoal(experimentId, goals as Goals),
                    apiEvents.goalSucceeded,
                    apiEvents.goalFailed,
                    (goals) => !!goals
                ),

                autosaveScheduling$: autosave(
                    'scheduling',
                    events.on(pageEvents.schedulingChanged),
                    (experimentId, scheduling) =>
                        experimentsService.setScheduling(experimentId, scheduling),
                    apiEvents.schedulingSucceeded,
                    apiEvents.schedulingFailed
                ),

                autosaveTrafficAllocation$: autosave(
                    'trafficAllocation',
                    events.on(pageEvents.trafficAllocationChanged),
                    (experimentId, trafficAllocation: number) =>
                        experimentsService.setTrafficAllocation(experimentId, trafficAllocation),
                    apiEvents.trafficAllocationSucceeded,
                    apiEvents.trafficAllocationFailed
                ),

                autosaveTrafficProportion$: autosave(
                    'trafficProportion',
                    events.on(pageEvents.trafficProportionChanged),
                    (experimentId, trafficProportion: TrafficProportion) =>
                        experimentsService.setTrafficProportion(experimentId, trafficProportion),
                    apiEvents.trafficProportionSucceeded,
                    apiEvents.trafficProportionFailed
                ),

                /**
                 * Split Evenly is a weight change like any other: it folds back into the same
                 * debounced `trafficProportion` PATCH instead of having a path of its own.
                 */
                splitEvenly$: events.on(pageEvents.splitEvenly).pipe(
                    map(() => store.experiment()?.trafficProportion),
                    filter(
                        (trafficProportion): trafficProportion is TrafficProportion =>
                            !!trafficProportion?.variants?.length
                    ),
                    map((trafficProportion) =>
                        pageEvents.trafficProportionChanged({
                            type: TrafficProportionTypes.SPLIT_EVENLY,
                            variants: splitWeightsEvenly(trafficProportion.variants)
                        })
                    )
                ),

                // Variants have dedicated endpoints; `mergeMap` so deleting one row does not
                // cancel the rename of another.
                addVariant$: events.on(pageEvents.variantAdded).pipe(
                    mergeMap(({ payload }) =>
                        experimentsService.addVariant(store.experiment()?.id ?? '', payload).pipe(
                            mapResponse({
                                next: (experiment) => apiEvents.addVariantSucceeded(experiment),
                                error: toFailure(apiEvents.addVariantFailed)
                            })
                        )
                    )
                ),

                editVariant$: events.on(pageEvents.variantRenamed).pipe(
                    mergeMap(({ payload }) =>
                        experimentsService
                            .editVariant(store.experiment()?.id ?? '', payload.variantId, {
                                description: payload.name
                            })
                            .pipe(
                                mapResponse({
                                    next: (experiment) =>
                                        apiEvents.editVariantSucceeded(experiment),
                                    error: toFailure(apiEvents.editVariantFailed)
                                })
                            )
                    )
                ),

                removeVariant$: events.on(pageEvents.variantDeleted).pipe(
                    mergeMap(({ payload }) =>
                        experimentsService
                            .removeVariant(store.experiment()?.id ?? '', payload)
                            .pipe(
                                mapResponse({
                                    next: (experiment) =>
                                        apiEvents.removeVariantSucceeded(experiment),
                                    error: toFailure(apiEvents.removeVariantFailed)
                                })
                            )
                    )
                ),

                /**
                 * Start only reaches the API once every rule passes — the reducer has already
                 * run by the time this filter reads `validationErrors`, so an invalid press
                 * stops here and only reveals the failing fields (AC29/AC30).
                 *
                 * `starting` is what makes a second press a no-op rather than a second run: it is
                 * raised by `startRequested` as the call leaves, so the double press is dropped
                 * here instead of reaching the `switchMap`, which would cancel the first call and
                 * start another.
                 */
                start$: events.on(pageEvents.startRequested).pipe(
                    filter(
                        () =>
                            !store.validationErrors().length &&
                            !!store.experiment() &&
                            !store.starting()
                    ),
                    switchMap(() =>
                        merge(
                            of(apiEvents.startRequested()),
                            experimentsService.start(store.experiment()?.id ?? '').pipe(
                                mapResponse({
                                    next: (experiment) => apiEvents.startSucceeded(experiment),
                                    error: (error: HttpErrorResponse) => {
                                        // A scheduling rejection comes back without a header of
                                        // its own, which would leave the dialog titleless.
                                        httpErrorManager.handle({
                                            ...error,
                                            error: {
                                                ...error.error,
                                                header:
                                                    error.error?.header ??
                                                    dotMessageService.get(START_ERROR_HEADER_KEY)
                                            }
                                        } as HttpErrorResponse);

                                        return apiEvents.startFailed(error);
                                    }
                                })
                            )
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

                cancelSchedule$: events.on(pageEvents.cancelScheduleRequested).pipe(
                    switchMap(() =>
                        experimentsService.cancelSchedule(store.experiment()?.id ?? '').pipe(
                            mapResponse({
                                next: (experiment) => apiEvents.cancelScheduleSucceeded(experiment),
                                error: toFailure(apiEvents.cancelScheduleFailed)
                            })
                        )
                    )
                ),

                // There is no dedicated abort endpoint; aborting a running experiment cancels it,
                // same as the list store does. Only the toast copy differs.
                abort$: events.on(pageEvents.abortRequested).pipe(
                    switchMap(() =>
                        experimentsService.cancelSchedule(store.experiment()?.id ?? '').pipe(
                            mapResponse({
                                next: (experiment) => apiEvents.abortSucceeded(experiment),
                                error: toFailure(apiEvents.abortFailed)
                            })
                        )
                    )
                )
            };
        }
    ),
    withHooks(() => {
        const route = inject(ActivatedRoute);
        const router = inject(Router);
        const dispatcher = inject(Dispatcher);
        const events = inject(Events);

        let createdSubscription: SubscriptionLike;

        return {
            onInit() {
                // Read once. The Configure screen has no filters to mirror back into the URL, so
                // unlike the list there is nothing to keep in sync afterwards.
                const experimentId = route.snapshot.paramMap.get('experimentId');

                if (experimentId) {
                    dispatcher.dispatch(pageEvents.enterExisting(experimentId));

                    return;
                }

                dispatcher.dispatch(pageEvents.enterNew());

                const pageId = route.snapshot.queryParamMap.get('pageId');
                const url = route.snapshot.queryParamMap.get('url');

                if (pageId || url) {
                    dispatcher.dispatch(pageEvents.pagePrefillRequested({ pageId, url }));
                }

                /**
                 * Swap `/experiments/new` for the created experiment's own URL. `replaceUrl`
                 * keeps `/new` out of the history, so Back leaves the screen instead of
                 * returning to a creation form for an experiment that already exists (AC3).
                 * Relative navigation, so the portlet's mount point is not restated here.
                 */
                createdSubscription = events
                    .on(dotExperimentsConfigureApiEvents.createSucceeded)
                    .subscribe(({ payload }) => {
                        router.navigate(['..', payload.id, 'configuration'], {
                            relativeTo: route,
                            replaceUrl: true
                        });
                    });
            },
            onDestroy() {
                createdSubscription?.unsubscribe();
            }
        };
    })
);

/** Injectable type of {@link DotExperimentsConfigureStore}, for typing component fields. */
export type DotExperimentsConfigureStore = InstanceType<typeof DotExperimentsConfigureStore>;
