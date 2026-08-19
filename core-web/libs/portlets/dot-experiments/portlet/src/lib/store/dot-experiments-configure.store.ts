import { mapResponse } from '@ngrx/operators';
import { signalStore, withComputed, withHooks, withState } from '@ngrx/signals';
import { Dispatcher, Events, on, withEventHandlers, withReducer } from '@ngrx/signals/events';
import { merge, of, SubscriptionLike } from 'rxjs';

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
    DotPagesBrowserService
} from '@dotcms/data-access';
import {
    ComponentStatus,
    DotCMSContentlet,
    DotExperimentPatchBody,
    DotExperimentStatus,
    EXP_CONFIG_ERROR_LABEL_CANT_EDIT,
    EXP_CONFIG_ERROR_LABEL_PAGE_BLOCKED,
    Variant
} from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';

import { dotExperimentsConfigureApiEvents } from './dot-experiments-configure-api.events';
import {
    ConfigurePagePrefill,
    dotExperimentsConfigurePageEvents
} from './dot-experiments-configure-page.events';

import {
    AUTOSAVE_DEBOUNCE_MS,
    CONFIGURATION_SEGMENT,
    LOCKED_BANNER_KEY_READ_ONLY,
    LOCKED_BANNER_KEY_RUNNING,
    PAGE_PREFILL_ERROR_KEY,
    PAGE_PREFILL_LOOKUP_ERROR_KEY,
    START_ERROR_HEADER_KEY,
    TOTAL_WEIGHT
} from '../shared/constants';
import {
    ConfigureValidationRule,
    DotExperimentsConfigureViewState,
    ExperimentListAction
} from '../shared/models';
import {
    applyPatchToExperiment,
    fromBrowserPage,
    hasPendingChanges,
    normalizePath,
    toConfigurePage,
    toOutgoingPatch,
    totalWeight,
    validateConfigure,
    withoutSentKeys
} from '../util/dot-experiments-configure.util';
import { isAllowed } from '../util/dot-experiments-list.util';

const pageEvents = dotExperimentsConfigurePageEvents;
const apiEvents = dotExperimentsConfigureApiEvents;

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
    validationRevealed: false,
    pendingPatch: null,
    lastSaveFailed: false
};

/** Shape of the `/api/content/_search` entity the page lookup reads contentlets from. */
interface PageLookupEntity {
    jsonObjectView?: { contentlets?: DotCMSContentlet[] };
}

/**
 * Store for the Configure screen, on both `/experiments/new` and
 * `/experiments/:experimentId/configuration`.
 *
 * There is no "save" button: the first Name + Page combination POSTs the draft (once — the URL is
 * then swapped for the created one) and every later change is persisted by a debounced PATCH.
 * `PATCH /api/v1/experiments/{id}` applies every key of its body in one atomic update, so the
 * screen keeps one accumulated diff and one timer: a Name edit and a Goal edit in the same window
 * leave as a single multi-key call (AC6). `pageId` and `targetingConditions` are never part of a
 * body — the page is immutable once the draft exists, and sending targeting conditions would have
 * the backend rebuild the experiment's Rule (AC7).
 *
 * The page is chosen once: the PATCH endpoint does not accept `pageId`, so after creation the
 * selected page is only ever *resolved* from the experiment, never sent back.
 *
 * Validation is deliberately absent until Start/Schedule is pressed (AC28): the reducer for
 * `startRequested` sets `validationRevealed`, and the handler only calls the API while
 * `$validationErrors` is empty. The reveal is what latches — the errors themselves are derived
 * from the form, so a field the user fixes stops showing one straight away.
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

        /**
         * The part of the pending diff that would actually leave now — what the flush computes,
         * computed the same way (`toOutgoingPatch`).
         *
         * Before the draft exists nothing can leave at all: no PATCH without an experiment, and
         * the POST cannot fire until a page is picked. Those keys travel with the creation call
         * (whose own feedback is `creating`/`$isSaving`), so reporting them as "autosaving" here
         * would pin the footer on "Saving…" from the first keystroke on `/new` (#37003).
         */
        const $outgoingPatch = computed<DotExperimentPatchBody | null>(() => {
            const experiment = store.experiment();

            return experiment ? toOutgoingPatch(store.pendingPatch(), experiment) : null;
        });

        /**
         * Nothing to show until Start/Schedule has been pressed (AC28); after that the rules are
         * re-run against the current form, so an error disappears as soon as its field is fixed.
         */
        const $validationErrors = computed<ConfigureValidationRule[]>(() =>
            store.validationRevealed()
                ? validateConfigure({
                      draftName: store.draftName(),
                      selectedPage: store.selectedPage(),
                      experiment: store.experiment(),
                      pendingPatch: store.pendingPatch()
                  })
                : []
        );

        return {
            $status,
            $isLocked,
            $lockedByAnotherUser,
            $variants,
            $totalWeight,
            /**
             * Whether the weights the store holds do not add up (AC25). An experiment with no
             * variants yet is not "wrong", which is the exception the backend makes as well.
             *
             * The Variants card states the same thing from its own slice, where the rule lives in
             * the schema; this is what the *store* knows, and it is why an intermediate total is
             * held back rather than PATCHed (see `toOutgoingPatch`).
             */
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
            /**
             * The failing rules, or empty while they are still hidden. Derived rather than
             * latched: a Start press reveals them, and every later edit re-runs them, so a field
             * the user fixes stops showing an error without pressing Start again (#37003).
             */
            $validationErrors,
            $validationErrorCount: computed<number>(() => $validationErrors().length),
            $isSaving: computed<boolean>(
                () => store.status() === ComponentStatus.SAVING || store.creating()
            ),
            /**
             * True while a PATCH is debounced or in flight: the diff stays pending until the server
             * accepts it, so one signal covers both.
             *
             * Read off the *sendable* part of the diff, not the whole of it: a pending
             * `trafficProportion` whose weights are still being fixed is going nowhere, and a
             * footer saying "Saving…" while it waits would never stop (#37003).
             *
             * A failed save is excluded for the same reason even though its diff is sendable —
             * nothing is on its way any more until the next edit re-sends it.
             */
            $isAutosaving: computed<boolean>(
                () => !store.lastSaveFailed() && hasPendingChanges($outgoingPatch())
            )
        };
    }),
    withReducer<DotExperimentsConfigureViewState>(
        on(pageEvents.enterNew, () => ({ ...initialState, status: ComponentStatus.LOADED })),
        /**
         * Everything is dropped, not just the pending diff: one route serves `/experiments/new`
         * and `/:experimentId/configuration` and the component is reused across them, so a URL
         * arriving while the screen is up must leave nothing of the experiment being left behind
         * — a validation error revealed by a Start on the previous one would otherwise show
         * against this one before Start is ever pressed (AC28).
         */
        on(pageEvents.enterExisting, () => ({
            ...initialState,
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
        // A rejected lookup says nothing about whether the page is there, so it gets its own copy
        // rather than reading as "not found". The error itself was already reported by
        // `DotHttpErrorManagerService`.
        on(apiEvents.pagePrefillLookupFailed, () => ({
            selectedPage: null,
            pagePrefillError: PAGE_PREFILL_LOOKUP_ERROR_KEY
        })),
        on(apiEvents.pageLockResolved, ({ payload }) => ({ pageLockInfo: payload })),

        /**
         * A field changed. The keys are merged into the diff waiting to be flushed — later value
         * per key wins — and applied locally at once: the PATCH is debounced, and no card may lag
         * half a second behind the keystroke that caused it.
         *
         * The drafts follow `name`/`description` so the header title tracks what is being typed,
         * while `experiment` keeps the persisted pair (see `applyPatchToExperiment`).
         */
        on(pageEvents.formEdited, ({ payload }, state) => ({
            pendingPatch: { ...state.pendingPatch, ...payload },
            lastSaveFailed: false,
            draftName: payload.name ?? state.draftName,
            draftDescription: payload.description ?? state.draftDescription,
            experiment: applyPatchToExperiment(state.experiment, payload)
        })),

        /**
         * A PATCH is on the wire. `SAVING` drives the flight-only progress indicator; the
         * debounce window before it deliberately does not reach this state, or the bar would run
         * from the first keystroke for as long as the user keeps typing.
         */
        on(apiEvents.saveRequested, () => ({ status: ComponentStatus.SAVING })),
        /**
         * Only what was written settles: a key whose pending value is still the one the body
         * carried leaves the diff, and what `toOutgoingPatch` held back — or what was re-edited
         * while the request travelled — stays pending. See `withoutSentKeys`.
         *
         * The response is the source of truth for what was written, so the experiment is replaced
         * by it — but with the held-back keys re-applied on top, or the weights the user is still
         * fixing would snap back to the older ones the server answered with (#37003).
         */
        on(apiEvents.saveSucceeded, ({ payload }, state) => {
            const heldBack = withoutSentKeys(state.pendingPatch, payload.sent);

            return {
                experiment: heldBack
                    ? applyPatchToExperiment(payload.experiment, heldBack)
                    : payload.experiment,
                pendingPatch: heldBack,
                lastSaveFailed: false,
                status: ComponentStatus.LOADED
            };
        }),
        /**
         * A rejected write keeps its diff: the next edit re-sends it merged with whatever changed,
         * which is the only retry the screen has. The failure itself was already reported by
         * `DotHttpErrorManagerService`, and the screen stays usable.
         */
        on(apiEvents.saveFailed, () => ({
            lastSaveFailed: true,
            status: ComponentStatus.LOADED
        })),
        /**
         * A skipped flush never touches the diff: nothing was written, so nothing can settle, and
         * dropping it would take the held-back keys with it — the next successful PATCH of another
         * key would then replace the experiment with a response that knows nothing about the
         * weights being fixed (#37003). `$isAutosaving` stays honest regardless, since it reads
         * only the sendable part of the diff.
         *
         * The status reset IS needed though: an edit can cancel an in-flight PATCH (`switchMap`)
         * after `saveRequested` set `SAVING`, and if the re-debounced flush then finds nothing to
         * send, this is the only event left to bring the status home.
         */
        on(apiEvents.saveSkipped, () => ({ status: ComponentStatus.LOADED })),

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
         * Pressing Start is what *reveals* the rules (AC28/AC29) — it does not freeze them. The
         * errors are derived from the form (`$validationErrors`), so fixing a field clears its
         * own error straight away instead of staying red until Start is pressed again.
         */
        on(pageEvents.startRequested, (_event, state) => ({
            validationRevealed: true,
            status: validateConfigure(state).length ? state.status : ComponentStatus.SAVING
        })),
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
                validationRevealed: false,
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
                            catchError((error: HttpErrorResponse) =>
                                of(toFailure(apiEvents.pagePrefillLookupFailed)(error))
                            )
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
                        catchError((error: HttpErrorResponse) =>
                            of(toFailure(apiEvents.pagePrefillLookupFailed)(error))
                        )
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
                    events.on(pageEvents.formEdited),
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

                /**
                 * The one autosave: every edit feeds a single timer, and what goes out is the diff
                 * accumulated by the time it elapses (AC6). Two cards edited in the same window
                 * therefore reach the server as one multi-key call, not as two.
                 *
                 * `switchMap`, not `mergeMap`: an edit arriving while a PATCH is on the wire
                 * replaces it rather than queuing behind it — that is what "collapses into a single
                 * call" means, and it is also what makes clearing the whole diff safe when a
                 * response *is* processed.
                 *
                 * `createSucceeded` is merged in to flush what was typed while the creation POST
                 * was travelling: those keys were accumulated against an experiment that did not
                 * exist yet, and this is the first moment they can be written. The keys the POST
                 * itself carried are dropped by `toOutgoingPatch`, so the flush stays a no-op when
                 * nothing was typed in the meantime.
                 *
                 * The body is reported back with the response: it is not always every pending key,
                 * and only the ones it carried may settle (#37003).
                 */
                autosave$: merge(
                    events.on(pageEvents.formEdited),
                    events.on(apiEvents.createSucceeded)
                ).pipe(
                    debounceTime(AUTOSAVE_DEBOUNCE_MS),
                    switchMap(() => {
                        const experiment = store.experiment();
                        // Nothing to patch before the draft exists: those values reach the server
                        // through the creation POST, and whatever is left over is flushed here as
                        // soon as it answers.
                        const body = experiment
                            ? toOutgoingPatch(store.pendingPatch(), experiment)
                            : null;

                        if (!experiment || !body) {
                            return of(apiEvents.saveSkipped());
                        }

                        // `saveRequested` marks the moment a real request leaves — the visible
                        // progress indicator keys on it, so it runs for the flight only, not for
                        // the whole debounce window while the user is still typing.
                        return merge(
                            of(apiEvents.saveRequested()),
                            experimentsService.patch(experiment.id, body).pipe(
                                mapResponse({
                                    next: (updated) =>
                                        apiEvents.saveSucceeded({
                                            experiment: updated,
                                            sent: body
                                        }),
                                    error: toFailure(apiEvents.saveFailed)
                                })
                            )
                        );
                    })
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
                            !store.$validationErrors().length &&
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
    withHooks((store) => {
        const route = inject(ActivatedRoute);
        const router = inject(Router);
        const dispatcher = inject(Dispatcher);
        const events = inject(Events);

        let createdSubscription: SubscriptionLike;
        let routeSubscription: SubscriptionLike;

        return {
            onInit() {
                /**
                 * Swap `/experiments/new` for the created experiment's own URL. `replaceUrl`
                 * keeps `/new` out of the history, so Back leaves the screen instead of
                 * returning to a creation form for an experiment that already exists (AC3).
                 * Relative navigation, so the portlet's mount point is not restated here.
                 */
                createdSubscription = events
                    .on(dotExperimentsConfigureApiEvents.createSucceeded)
                    .subscribe(({ payload }) => {
                        router.navigate(['..', payload.id, CONFIGURATION_SEGMENT], {
                            relativeTo: route,
                            replaceUrl: true
                        });
                    });

                /**
                 * Followed for as long as the screen lives, not read once from the snapshot: one
                 * route serves both `/experiments/new` and `/experiments/:experimentId/configuration`
                 * and the component is reused across them, so a URL arriving while the screen is up
                 * — the created experiment's own, or one pasted into the address bar — would
                 * otherwise leave the store showing the wrong experiment, or a creation form on an
                 * existing one's URL.
                 */
                routeSubscription = route.paramMap
                    .pipe(
                        map((params) => params.get('experimentId')),
                        distinctUntilChanged()
                    )
                    .subscribe((experimentId) => {
                        if (experimentId) {
                            // Not for the swap this store just triggered: that experiment is already
                            // here, possibly with edits the reload would throw away.
                            if (experimentId !== store.experiment()?.id) {
                                dispatcher.dispatch(pageEvents.enterExisting(experimentId));
                            }

                            return;
                        }

                        dispatcher.dispatch(pageEvents.enterNew());

                        const pageId = route.snapshot.queryParamMap.get('pageId');
                        const url = route.snapshot.queryParamMap.get('url');

                        if (pageId || url) {
                            dispatcher.dispatch(pageEvents.pagePrefillRequested({ pageId, url }));
                        }
                    });
            },
            onDestroy() {
                createdSubscription?.unsubscribe();
                routeSubscription?.unsubscribe();
            }
        };
    })
);

/** Injectable type of {@link DotExperimentsConfigureStore}, for typing component fields. */
export type DotExperimentsConfigureStore = InstanceType<typeof DotExperimentsConfigureStore>;
