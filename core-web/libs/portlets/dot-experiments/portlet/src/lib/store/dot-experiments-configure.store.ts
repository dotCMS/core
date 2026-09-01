import { mapResponse } from '@ngrx/operators';
import { signalStore, withComputed, withHooks, withState } from '@ngrx/signals';
import { Dispatcher, Events, on, withEventHandlers, withReducer } from '@ngrx/signals/events';
import { defer, from, merge, Observable, of, SubscriptionLike } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { computed, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import {
    catchError,
    concatMap,
    distinctUntilChanged,
    filter,
    last,
    map,
    mergeMap,
    switchMap,
    tap
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
    DotExperimentStatus,
    DotExperiment,
    EXP_CONFIG_ERROR_LABEL_CANT_EDIT,
    EXP_CONFIG_ERROR_LABEL_PAGE_BLOCKED,
    Variant
} from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';
import { isDotIdentifier } from '@dotcms/utils';

import { dotExperimentsConfigureApiEvents } from './dot-experiments-configure-api.events';
import {
    ConfigurePagePrefill,
    dotExperimentsConfigurePageEvents
} from './dot-experiments-configure-page.events';

import {
    CONFIGURATION_SEGMENT,
    DEFAULT_TRAFFIC_ALLOCATION,
    LOCKED_BANNER_KEY_READ_ONLY,
    LOCKED_BANNER_KEY_RUNNING,
    PAGE_PREFILL_ERROR_KEY,
    PAGE_PREFILL_LOOKUP_ERROR_KEY,
    START_ERROR_HEADER_KEY
} from '../shared/constants';
import {
    ConfigureFormModel,
    ConfigureValidationRule,
    DotExperimentsConfigureViewState,
    ExperimentListAction
} from '../shared/models';
import {
    toConfigureFormModel,
    isSendableSplit,
    toConfigurePatch
} from '../util/dot-experiments-configure-form.util';
import {
    canChangePage,
    deletableVariants,
    fromBrowserPage,
    isSameFormValue,
    normalizePath,
    toConfigurePage,
    validateConfigure
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
    deletingVariants: false,
    deleteVariantsFailed: false,
    validationRevealed: false,
    formValue: null,
    formValidity: { trafficAllocation: true, scheduling: true },
    savedFormValue: null
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
 * leave as a single multi-key call (AC6). `targetingConditions` is never part of a body — sending
 * it would have the backend rebuild the experiment's Rule (AC7) — and `pageId` only when the page
 * actually moved, since a body carrying the stored one is the shape `save()` refuses on an
 * experiment that has variants.
 *
 * The page of a draft can still be changed (#37176), and the variants are what stand in the way:
 * a non-control variant holds a copy of the current page, so the change deletes them first
 * (`pageChangeConfirmed`) and only then is a new page worth selecting.
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

/**
 * The form a stored experiment implies — the one baseline every clean state is measured against.
 *
 * Always derived from what the server answered with, never snapshotted from the screen, and
 * derived by the same function the shell fills the form with, so the two agree by construction.
 * That is the whole rule: clean means the screen holds what the server holds.
 *
 * Snapshotting the request instead is wrong in both directions. It calls saved whatever the user
 * typed while the request was in flight, which was never sent; and it calls saved the slices
 * `toConfigurePatch` deliberately withholds — a goal without a type, a split mid-edit — which were
 * not sent either. A just-created draft is the same story from the other side: the POST makes the
 * control variant, the card seeds a row from it, and only the response knows about it.
 */
function baselineOf(experiment: DotExperiment): ConfigureFormModel {
    return toConfigureFormModel({
        experiment,
        draftName: experiment.name,
        draftDescription: experiment.description ?? ''
    });
}

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

        /** The variants a page change has to delete first — see `deletableVariants`. */
        const $deletableVariants = computed<Variant[]>(() => deletableVariants(store.experiment()));

        /**
         * Whether the weights on screen stand in the way of a save.
         *
         * An empty slice does not: it means the form has nothing to say about the weights yet —
         * the state a just-created draft is in before the shell seeds them — and the body simply
         * leaves `trafficProportion` out. Blocking there would make the first save after creation
         * impossible.
         *
         * A slice that exists has to be one the backend would take: standing for the current
         * variants, and totalling exactly 100. Refusing rather than quietly omitting it is what
         * keeps `saveSucceeded` free to call the whole form saved.
         */
        const $hasSendableWeights = computed<boolean>(() => {
            const rows = store.formValue()?.variantWeights ?? [];

            return !rows.length || isSendableSplit(rows, $variants());
        });

        /**
         * Whether the screen holds work the server has not accepted.
         *
         * Two questions, because the page lives beside the form rather than in it: has the form
         * moved since the last write, and does the selected page still match the stored one.
         * Before the experiment exists there is nothing to compare against, so anything the user
         * has put in counts.
         *
         * A failed save needs no flag. It leaves the snapshot where it was, so the screen simply
         * goes on being dirty — which is the truth, and is what keeps the button live.
         */
        const $hasUnsavedChanges = computed<boolean>(() => {
            if (store.isNew()) {
                return !!store.draftName().trim() || !!store.selectedPage();
            }

            const selected = store.selectedPage();
            const pageChanged = !!selected && selected.pageId !== store.experiment()?.pageId;

            /**
             * A weights slice the card has not seeded yet says nothing about the weights, so it is
             * not a difference — the same reading that lets a save go out without one. Without it a
             * just-created draft is dirty for the instant between the POST answering and the card
             * mirroring the control it made, and that instant is long enough to fire a PATCH that
             * carries nothing new, and to make leaving the screen ask about nothing.
             */
            const form = store.formValue();
            const saved = store.savedFormValue();

            // No form at all is not unsaved work: the screen has been reset and the shell has not
            // put anything on it yet, which is where a URL arriving mid-session leaves it.
            if (!form) {
                return pageChanged;
            }

            const comparable =
                form && saved && !form.variantWeights.length
                    ? { ...form, variantWeights: saved.variantWeights }
                    : form;

            return pageChanged || !isSameFormValue(comparable, saved);
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
                      formValue: store.formValue()
                  })
                : []
        );

        return {
            $status,
            $isLocked,
            $lockedByAnotherUser,
            $variants,
            $deletableVariants,
            /**
             * Share of the page's traffic entering the Experiment, as the form currently holds it.
             *
             * Read off the form rather than the experiment so the Variants card can show what each
             * weight means against it while the number is still being typed.
             */
            $trafficAllocation: computed<number>(
                () =>
                    store.formValue()?.trafficAllocation ??
                    store.experiment()?.trafficAllocation ??
                    DEFAULT_TRAFFIC_ALLOCATION
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
             * Whether the screen is holding work the server has not accepted.
             *
             * Drives the Save draft button and the leave-confirmation guard, so it has to answer
             * for both halves of the screen's life. Before the experiment exists there is no diff
             * to read — the name and the page live in their own slices until the POST carries
             * them — so anything typed there counts. Afterwards it is the accumulated diff.
             *
             * A failed save deliberately stays dirty. The user's work is still only on screen, and
             * the whole point of the button is that they can press it again.
             */
            /**
             * Whether the Page card may still offer a different page. Mirrors the server rule, so
             * the screen refuses in the same cases rather than offering a choice that returns 400.
             */
            $canChangePage: computed<boolean>(() => canChangePage(store.experiment())),
            $hasUnsavedChanges,
            /**
             * Whether pressing Save Draft would write anything.
             *
             * The name is asked for on both paths because both send it: it is required by the POST
             * and non-optional on the PATCH, so a blank one is a rejection either way.
             *
             * Creating needs the page too, since the POST carries it. Afterwards a change is
             * sendable as long as the weights are not mid-edit — the body is built from the whole
             * form, and the one slice the backend would refuse is a split that does not total 100.
             */
            $canSave: computed<boolean>(() => {
                if (!store.draftName().trim()) {
                    return false;
                }

                if (store.isNew()) {
                    return !!store.selectedPage();
                }

                return $hasUnsavedChanges() && $hasSendableWeights();
            })
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
            savedFormValue: baselineOf(payload),
            status: ComponentStatus.LOADED
        })),
        on(apiEvents.loadFailed, () => ({ status: ComponentStatus.ERROR })),

        // Creation: the flag closes the door on a second POST as the first one leaves.
        on(apiEvents.createRequested, () => ({ creating: true })),
        /**
         * The POST carried only the name, the description and the page, so a goal, a schedule or
         * an allocation entered before the press still differs from the draft that came back —
         * which is what leaves them dirty, and what the follow-up PATCH this event triggers
         * carries. The control variant the POST created is on both sides, so it does not.
         */
        on(apiEvents.createSucceeded, ({ payload }) => ({
            experiment: payload,
            isNew: false,
            creating: false,
            draftName: payload.name,
            draftDescription: payload.description ?? '',
            savedFormValue: baselineOf(payload),
            status: ComponentStatus.LOADED
        })),
        // The user stays on `/experiments/new` with everything they typed still there (AC4).
        on(apiEvents.createFailed, () => ({ creating: false })),

        /**
         * A page was picked. Before creation it is held until the POST carries it; on an existing
         * experiment the next Save Draft sends it, and the difference against the stored page is
         * what makes the screen dirty in the meantime.
         *
         * A selection the rule does not allow is ignored rather than applied optimistically: the
         * card does not offer the choice in the first place, and showing a page the server would
         * refuse is worse than showing none.
         */
        on(pageEvents.pageSelected, ({ payload }, state) => {
            if (!canChangePage(state.experiment)) {
                return {};
            }

            return { selectedPage: payload, pagePrefillError: null };
        }),
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
         * The form moved. The mirror is replaced wholesale — there is nothing to merge, because
         * the payload already *is* the whole form.
         *
         * The drafts follow name and description so the header title tracks what is being typed,
         * while `experiment` keeps whatever the server last answered with. The two are allowed to
         * disagree until a save reconciles them; that gap is precisely the unsaved state.
         */
        on(pageEvents.formChanged, ({ payload }) => ({
            formValue: payload.value,
            formValidity: payload.validity,
            draftName: payload.value.name,
            draftDescription: payload.value.description
        })),

        /**
         * A PATCH is on the wire. `SAVING` drives the flight-only progress indicator; the
         * debounce window before it deliberately does not reach this state, or the bar would run
         * from the first keystroke for as long as the user keeps typing.
         */
        on(apiEvents.saveRequested, () => ({ status: ComponentStatus.SAVING })),
        /**
         * The response is the source of truth for what was written: it replaces the experiment,
         * and the baseline is derived from it. Anything the user typed while the request was in
         * flight is therefore still dirty, which is what makes the next press send it.
         */
        on(apiEvents.saveSucceeded, ({ payload }) => ({
            experiment: payload.experiment,
            savedFormValue: baselineOf(payload.experiment),
            status: ComponentStatus.LOADED
        })),
        /**
         * A rejected write moves no snapshot, so the screen stays dirty and the button stays live —
         * pressing it again is the retry. The failure itself was already reported by
         * `DotHttpErrorManagerService`.
         */
        on(apiEvents.saveFailed, () => ({ status: ComponentStatus.LOADED })),
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

        // A press only clears the last run's outcome: the confirmation opens on a clean slate
        // rather than on the error the previous attempt ended with.
        on(pageEvents.pageChangeRequested, () => ({ deleteVariantsFailed: false })),
        /**
         * Clearing the variants for a page change is one command over several calls, and reads as
         * the same `SAVING` a single deletion does — plus a flag of its own, which is what the
         * confirmation's button spins on.
         *
         * Keyed on the request the handler raises rather than on the confirmation that triggers it.
         * Two reasons, and the second is the one that forces it: nothing rises for a run that never
         * goes out (the dialog would hang waiting on it), and the handler's own re-entrancy guard
         * reads this flag — a reducer on the intent would set it before the handler ran and the
         * guard would refuse the very first press.
         */
        on(apiEvents.deleteVariantsRequested, () => ({
            status: ComponentStatus.SAVING,
            deletingVariants: true,
            deleteVariantsFailed: false
        })),
        on(apiEvents.deleteVariantsSucceeded, ({ payload }) => ({
            experiment: payload,
            status: ComponentStatus.LOADED,
            deletingVariants: false
        })),
        /**
         * A rejection halfway leaves the variants that were already deleted deleted, so the last
         * answer the run did get is folded in rather than dropped: the card would otherwise go on
         * offering weights for rows that no longer exist. `null` means the first call failed, and
         * nothing about the experiment moved.
         *
         * The dialog stays open — it is the retry — which is what `deleteVariantsFailed` is for.
         */
        on(apiEvents.deleteVariantsFailed, ({ payload }) => ({
            ...(payload.experiment ? { experiment: payload.experiment } : {}),
            status: ComponentStatus.LOADED,
            deletingVariants: false,
            deleteVariantsFailed: true
        })),

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
            /**
             * The accumulated diff as a request, or `null` when there is nothing worth writing.
             *
             * Shared by Save draft and Start, because the backend validates a *start* against what
             * is persisted, not against what the screen holds: a goal picked and never flushed is a
             * goal the server has never seen, and `ExperimentsAPIImpl.start()` rejects it with
             * "The Experiment needs to have the Goal set." Start therefore writes before it
             * transitions, rather than trusting that a save happened to have gone out.
             *
             * Nothing to patch before the draft exists: those values reach the server through the
             * creation POST, and the rest follows as soon as it answers. Nothing to patch either
             * when the form is not in a state worth sending — the same question the Save button
             * asks. That matters most on the run that follows `createSucceeded`, where the POST has
             * just written the form and a second call would carry the identical body.
             *
             * The page travels only when it actually moved. Sending the stored one back on every
             * save would put `pageId` in the body of an experiment that has variants, which is the
             * one shape `ExperimentsAPIImpl.save()` refuses with a 400 — for a change the user
             * never made.
             */
            const pendingSave = (): {
                form: ConfigureFormModel;
                save$: Observable<DotExperiment>;
            } | null => {
                const experiment = store.experiment();
                const model = store.formValue();

                if (!experiment || !model || !store.$canSave()) {
                    return null;
                }

                const selectedPageId = store.selectedPage()?.pageId;
                const body = toConfigurePatch(
                    model,
                    store.formValidity(),
                    experiment.trafficProportion?.variants ?? [],
                    selectedPageId === experiment.pageId ? undefined : selectedPageId
                );

                return { form: model, save$: experimentsService.patch(experiment.id, body) };
            };

            /** Routes a failed call through the shared manager, then reports it as its event. */
            const toFailure =
                <T>(failed: (error: HttpErrorResponse) => T) =>
                (error: HttpErrorResponse): T => {
                    httpErrorManager.handle(error);

                    return failed(error);
                };

            /** The pending write as its own outcome — what Save draft reports and nothing more. */
            const savedBy = (pending: NonNullable<ReturnType<typeof pendingSave>>) =>
                pending.save$.pipe(
                    mapResponse({
                        next: (updated: DotExperiment) =>
                            apiEvents.saveSucceeded({ experiment: updated, form: pending.form }),
                        error: toFailure(apiEvents.saveFailed)
                    })
                );

            /** Resolves `?pageId=` / `?url=` to the page the Page card shows. */
            const resolvePrefill = ({ pageId, url }: ConfigurePagePrefill) => {
                if (pageId) {
                    // `?pageId=` is whatever the address bar carries, and it is concatenated into
                    // a Lucene query below: a value with spaces or operators would widen the
                    // search and prefill the card with some other contentlet. Nothing outside the
                    // identifier shape can name a page, so it is the same answer as not finding
                    // one — reported without spending a request on it.
                    if (!isDotIdentifier(pageId)) {
                        return of(apiEvents.pagePrefillFailed(pageId));
                    }

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
                 * The first Save draft is what creates the experiment: until it is pressed nothing
                 * exists on the server, so leaving the screen leaves no half-filled draft behind.
                 *
                 * A second POST is stopped by the `creating` filter, not by the flattening
                 * operator: the flag is raised by `createRequested` as the first request leaves,
                 * so a second press arriving mid-flight is dropped here and never reaches the
                 * `switchMap` — which would otherwise cancel the POST already creating the draft.
                 */
                create$: events.on(pageEvents.saveDraftRequested).pipe(
                    filter(() => store.isNew() && !store.creating()),
                    map(() => ({
                        name: store.draftName().trim(),
                        description: store.draftDescription(),
                        page: store.selectedPage(),
                        /**
                         * Carried by the POST rather than by a PATCH behind it. The endpoint takes
                         * it — `ExperimentForm` has the field and defaults it to 100 below zero —
                         * and a creation that delivers everything the screen holds is a creation
                         * with nothing left over to reconcile.
                         *
                         * Withheld while the field is invalid, exactly as the PATCH withholds it:
                         * the server would take a number the form is showing as an error.
                         */
                        trafficAllocation: store.formValidity().trafficAllocation
                            ? store.$trafficAllocation()
                            : undefined
                    })),
                    filter(({ name, page }) => !!name && !!page),
                    switchMap(({ name, description, page, trafficAllocation }) =>
                        merge(
                            of(apiEvents.createRequested()),
                            experimentsService
                                .add({
                                    pageId: page?.pageId ?? '',
                                    name,
                                    description,
                                    ...(trafficAllocation === undefined
                                        ? {}
                                        : { trafficAllocation })
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
                 * The one write of the form: Save draft flushes the whole accumulated diff as a
                 * single multi-key PATCH, whichever cards it came from.
                 *
                 * `createSucceeded` is merged in because the first press does two things. The POST
                 * carries the name, description and page; anything else the user had already
                 * filled in — a goal, a schedule, a traffic split — was accumulated against an
                 * experiment that did not exist yet, and this is the first moment it can be
                 * written. The keys the POST itself carried are dropped by `toOutgoingPatch`, so
                 * the follow-up stays a no-op when there was nothing else.
                 *
                 * `switchMap`, not `mergeMap`: a second press while a PATCH is on the wire replaces
                 * it rather than queuing behind it. That is also what makes settling the diff safe
                 * when a response *is* processed.
                 *
                 * The body is reported back with the response: it is not always every pending key,
                 * and only the ones it carried may settle (#37003).
                 */
                saveDraft$: merge(
                    events.on(pageEvents.saveDraftRequested),
                    events.on(apiEvents.createSucceeded)
                ).pipe(
                    switchMap(() => {
                        const pending = pendingSave();

                        if (!pending) {
                            return of(apiEvents.saveSkipped());
                        }

                        // `saveRequested` marks the moment a real request leaves — the visible
                        // progress indicator keys on it, so it runs for the flight only, not for
                        // the whole debounce window while the user is still typing.
                        return merge(of(apiEvents.saveRequested()), savedBy(pending));
                    })
                ),

                /**
                 * A refused save that carried a page puts the displayed page back.
                 *
                 * The card applies a pick optimistically, which is right for every field that
                 * cannot be refused on its own. `pageId` can: the screen gates on the same rule the
                 * server enforces, but that rule reads state — a variant added in another tab is
                 * enough to make the two disagree. Leaving the refused page on screen would show
                 * the user something that is not stored anywhere, so it is re-resolved from the
                 * experiment. The message explaining why was already raised by the error handler.
                 */
                revertRefusedPage$: events.on(apiEvents.saveFailed).pipe(
                    filter(() => store.selectedPage()?.pageId !== store.experiment()?.pageId),
                    map(() => store.experiment()?.pageId),
                    filter((pageId): pageId is string => !!pageId),
                    map((pageId) => pageEvents.pagePrefillRequested({ pageId }))
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

                /**
                 * Deletes every non-control variant so the page can move (#37176).
                 *
                 * One call per variant, run one after another rather than at once: each has the
                 * backend recompute the traffic proportion over what is left, so it is the answer
                 * to the *last* one that describes the experiment afterwards. Firing them in
                 * parallel would leave which of the answers is current up to arrival order.
                 *
                 * `deletingVariants` is what makes a second confirmation a no-op rather than a
                 * second run: the dialog guards its own button, but the guard that matters is here,
                 * where every dispatcher passes — the same contract `create$` and `start$` keep. A
                 * `switchMap` alone would not do: it would cancel a run whose deletions have
                 * already partly landed and start another from a list that has not caught up yet.
                 *
                 * Nothing left to delete is answered as success rather than dropped. It is
                 * reachable — the variants can go from another tab between opening the dialog and
                 * confirming it — and the page is free either way, so the alternative is a
                 * confirmation that sits there having done nothing with nothing to show for it.
                 */
                deleteVariantsForPageChange$: events.on(pageEvents.pageChangeConfirmed).pipe(
                    filter(() => !!store.experiment() && !store.deletingVariants()),
                    switchMap(() => {
                        const experiment = store.experiment() as DotExperiment;
                        const variantIds = store.$deletableVariants().map(({ id }) => id);

                        if (!variantIds.length) {
                            return of(apiEvents.deleteVariantsSucceeded(experiment));
                        }

                        // What the last successful deletion answered with, so a rejection halfway
                        // can still report the variants that did go.
                        let deleted: DotExperiment | null = null;

                        return merge(
                            of(apiEvents.deleteVariantsRequested()),
                            from(variantIds).pipe(
                                concatMap((variantId) =>
                                    experimentsService.removeVariant(experiment.id, variantId)
                                ),
                                tap((settled) => (deleted = settled)),
                                last(),
                                map((settled) => apiEvents.deleteVariantsSucceeded(settled)),
                                catchError((error: HttpErrorResponse) => {
                                    // Unobtrusive on purpose: the Change Page dialog cannot be
                                    // dismissed while this runs, so the inline message beside its
                                    // buttons is what carries the failure. An alert would only bury
                                    // the retry it offers.
                                    httpErrorManager.handle(error, true);

                                    return of(
                                        apiEvents.deleteVariantsFailed({
                                            error,
                                            experiment: deleted
                                        })
                                    );
                                })
                            )
                        );
                    })
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
                    switchMap(() => {
                        const experimentId = store.experiment()?.id ?? '';
                        // `defer` so the transition is only asked for once the write ahead of it
                        // has been accepted: built eagerly, a refused flush would still have
                        // called `start`.
                        const started$ = defer(() =>
                            experimentsService.start(experimentId).pipe(
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
                        );
                        const pending = pendingSave();

                        return merge(
                            of(apiEvents.startRequested()),
                            pending
                                ? pending.save$.pipe(
                                      switchMap((updated) =>
                                          merge(
                                              of(
                                                  apiEvents.saveSucceeded({
                                                      experiment: updated,
                                                      form: pending.form
                                                  })
                                              ),
                                              started$
                                          )
                                      ),
                                      catchError((error: HttpErrorResponse) => {
                                          httpErrorManager.handle(error);

                                          // Both, and in this order: the refused write leaves the
                                          // screen dirty and usable, and the start that never left
                                          // has to release `starting` or the button stays dead.
                                          return of(
                                              apiEvents.saveFailed(error),
                                              apiEvents.startFailed(error)
                                          );
                                      })
                                  )
                                : started$
                        );
                    })
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
