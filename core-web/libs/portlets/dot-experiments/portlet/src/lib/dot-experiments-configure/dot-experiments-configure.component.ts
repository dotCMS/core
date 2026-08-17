import { EventCreator, Events, injectDispatch } from '@ngrx/signals/events';

import {
    afterNextRender,
    Component,
    computed,
    DestroyRef,
    effect,
    ElementRef,
    inject,
    Injector,
    signal,
    untracked,
    viewChild
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
    applyEach,
    disabled,
    form,
    max,
    maxDate,
    maxLength,
    min,
    minDate,
    validate
} from '@angular/forms/signals';
import { ActivatedRoute, Router } from '@angular/router';

import { ConfirmationService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ProgressBarModule } from 'primeng/progressbar';
import { SkeletonModule } from 'primeng/skeleton';

import {
    DotExperimentsService,
    DotMessageDisplayService,
    DotMessageService,
    DotPagesBrowserService
} from '@dotcms/data-access';
import {
    ComponentStatus,
    CONFIGURATION_CONFIRM_DIALOG_KEY,
    DotExperiment,
    DotExperimentPatchBody,
    DotExperimentStatus,
    DotMessageSeverity,
    DotMessageType,
    MAX_INPUT_DESCRIPTIVE_LENGTH,
    MAX_INPUT_TITLE_LENGTH,
    TrafficProportionTypes
} from '@dotcms/dotcms-models';
import { DotEmptyContainerComponent, DotMessagePipe, PrincipalConfiguration } from '@dotcms/ui';

import { DotExperimentsConfigureDetailsComponent } from './components/dot-experiments-configure-details/dot-experiments-configure-details.component';
import { DotExperimentsConfigureFooterComponent } from './components/dot-experiments-configure-footer/dot-experiments-configure-footer.component';
import { DotExperimentsConfigureGoalComponent } from './components/dot-experiments-configure-goal/dot-experiments-configure-goal.component';
import { DotExperimentsConfigureHeaderComponent } from './components/dot-experiments-configure-header/dot-experiments-configure-header.component';
import { DotExperimentsConfigurePageComponent } from './components/dot-experiments-configure-page/dot-experiments-configure-page.component';
import { DotExperimentsConfigureSchedulingComponent } from './components/dot-experiments-configure-scheduling/dot-experiments-configure-scheduling.component';
import { DotExperimentsConfigureVariantsComponent } from './components/dot-experiments-configure-variants/dot-experiments-configure-variants.component';

import {
    EXPERIMENTS_URL,
    MAX_TRAFFIC_ALLOCATION,
    MIN_TRAFFIC_ALLOCATION,
    SUCCESS_MESSAGE_LIFE,
    TOTAL_WEIGHT,
    WEIGHTS_TOTAL_ERROR_KIND
} from '../shared/constants';
import { ConfigureFormModel, SchedulingDateBounds } from '../shared/models';
import { dotExperimentsConfigureApiEvents } from '../store/dot-experiments-configure-api.events';
import { dotExperimentsConfigurePageEvents } from '../store/dot-experiments-configure-page.events';
import { DotExperimentsConfigureStore } from '../store/dot-experiments-configure.store';
import { bindFormAutosave } from '../util/dot-experiments-autosave.util';
import {
    ConfigureFormSource,
    emptyConfigureForm,
    hasSameVariantIdentity,
    isSameVariantWeights,
    mergeVariantWeights,
    nextHalfHour,
    resolveDurationBounds,
    toConfigureFormModel,
    toConfigurePatch,
    toVariantWeightRows
} from '../util/dot-experiments-configure-form.util';
import { totalWeight } from '../util/dot-experiments-configure.util';

/** Number of card placeholders drawn while an existing experiment loads. */
const SKELETON_CARDS = [0, 1, 2];

/** Route `data` key `DotExperimentsConfigResolver` publishes the backend's duration limits under. */
const CONFIG_ROUTE_DATA_KEY = 'config';

/**
 * Shell of the Configure screen, routed on both `/experiments/new` and
 * `/experiments/:experimentId/configuration`.
 *
 * It owns the fixed-height layout the cards sit in — a header that stays put, an optional
 * read-only banner, a scrolling body and a pinned footer — plus everything that is screen-wide
 * rather than card-wide: the success toasts, and scrolling to the first field that failed
 * validation, since the body element it has to search is this component's.
 *
 * It also owns **the** form. Everything editable on the screen is one model with one schema over
 * it, and each card is handed the slice it renders: `PATCH /api/v1/experiments/{id}` applies every
 * key of its body in one atomic update, so one form is what lets one binding turn any edit into one
 * multi-key call. Five separate card forms could only ever guess at each other's state.
 *
 * A variant's existence is not part of it — adding, renaming and deleting one each have their own
 * endpoint — but its weight is, since the weights are only valid as a set and travel as one key.
 * Validation is not part of it either: no *required* rule fires before Start/Schedule is pressed
 * (AC28); the schema carries only what is live while typing (lengths, ranges, date bounds, the
 * weights totalling 100, read-only), and the Start/Schedule rules stay in the store where the press
 * can materialise them.
 *
 * Which experiment to show is not read here: the store's `onInit` reads the route itself, so the
 * shell only provides it and the two services it injects that are not `providedIn: 'root'`.
 */
@Component({
    selector: 'dot-experiments-configure',
    imports: [
        ConfirmDialogModule,
        ProgressBarModule,
        SkeletonModule,
        DotEmptyContainerComponent,
        DotMessagePipe,
        DotExperimentsConfigureHeaderComponent,
        DotExperimentsConfigureDetailsComponent,
        DotExperimentsConfigureGoalComponent,
        DotExperimentsConfigurePageComponent,
        DotExperimentsConfigureVariantsComponent,
        DotExperimentsConfigureSchedulingComponent,
        DotExperimentsConfigureFooterComponent
    ],
    templateUrl: './dot-experiments-configure.component.html',
    styleUrl: './dot-experiments-configure.component.scss',
    providers: [
        DotExperimentsConfigureStore,
        ConfirmationService,
        DotExperimentsService,
        DotPagesBrowserService
    ],
    host: { class: 'flex flex-col h-full min-h-0 overflow-hidden' }
})
export class DotExperimentsConfigureComponent {
    readonly store = inject(DotExperimentsConfigureStore);

    readonly CONFIRM_KEY = CONFIGURATION_CONFIRM_DIALOG_KEY;
    readonly SKELETON_CARDS = SKELETON_CARDS;

    /**
     * The screen is loading before the store has settled. `INIT` counts: the route is read in the
     * store's `onInit`, so an existing experiment spends a tick there before its load starts, and
     * treating it as loaded would flash an empty screen first.
     */
    readonly $isLoading = computed<boolean>(() => {
        const status = this.store.status();

        return status === ComponentStatus.INIT || status === ComponentStatus.LOADING;
    });

    /** Only a failed load reaches this; a failed autosave or transition stays on `LOADED`. */
    readonly $hasError = computed<boolean>(() => this.store.status() === ComponentStatus.ERROR);

    /**
     * Indeterminate bar under the header while a request is actually on the wire — the same
     * affordance UVE gives its autosave. Deliberately not the whole pending window: the debounce
     * runs from the first keystroke for as long as the user types, and a prominent animated bar
     * for that long reads as a stuck operation. The footer's "Saving…" copy covers that window
     * instead ($isAutosaving); this bar covers the flight ($isSaving: PATCH on the wire via
     * `saveRequested`, the creation POST via `creating`).
     */
    readonly $showProgressBar = computed<boolean>(() => this.store.$isSaving());

    readonly #route = inject(ActivatedRoute);
    readonly #router = inject(Router);
    readonly #events = inject(Events);
    /** Only the weights are reported from here; everything else goes through `bindFormAutosave`. */
    readonly #dispatch = injectDispatch(dotExperimentsConfigurePageEvents);
    readonly #injector = inject(Injector);
    readonly #destroyRef = inject(DestroyRef);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #dotMessageDisplayService = inject(DotMessageDisplayService);

    /**
     * Whether the screen opened on `/experiments/new`. Read once, as the store reads the route.
     *
     * There, the form is what creates the draft, so nothing is ever read back into it: hydrating
     * from the created experiment would drop a goal or a schedule entered before the name that
     * created it, and those are still on their way to the server.
     */
    readonly #isCreationScreen = !this.#route.snapshot.paramMap.get('experimentId');

    /** How long an experiment may run. Read once: a modal-free screen outlives no resolve. */
    readonly #durationBounds = resolveDurationBounds(
        this.#route.snapshot.data[CONFIG_ROUTE_DATA_KEY]
    );

    /** "Now" for the whole session. The pickers offer nothing before it. */
    readonly #now = new Date();

    /** Where an empty start picker opens: the next half hour, as the old screen does. */
    readonly #initialStartDate = nextHalfHour(this.#now);

    /** Everything the screen edits. The cards write into slices of it; nothing else does. */
    protected readonly $model = signal<ConfigureFormModel>(emptyConfigureForm());

    /**
     * The end date's window, measured from the start date being edited — or from now while no start
     * has been chosen. Both the pickers and the form's rules read it, so there is one answer.
     */
    protected readonly $schedulingBounds = computed<SchedulingDateBounds>(() => {
        const from = this.$model().scheduling.startDate?.getTime() ?? this.#now.getTime();

        return {
            initialStartDate: this.#initialStartDate,
            minEndDate: new Date(from + this.#durationBounds.minDuration),
            maxEndDate: new Date(from + this.#durationBounds.maxDuration)
        };
    });

    /**
     * The one form of the screen, and the rules that are live while the user types: lengths,
     * ranges, date bounds and read-only.
     *
     * Each slice's own rules are declared by the card that renders it and applied here, so the
     * shell says where they go without owning what they are. The leaves have no subtree to own and
     * are declared directly. Every field is disabled while the experiment is not a draft (AC34) —
     * the two slices carry that rule on their root, since a disabled field disables everything
     * under it.
     *
     * Nothing required is declared anywhere, deliberately (AC28): `required` reaches the DOM as the
     * native attribute, so an empty Name would be `:invalid` — and painted red — from first render.
     * Required-ness is checked by the store when Start/Schedule is pressed, and only then do the
     * cards reveal it.
     *
     * Deferring it is a presentation choice, not a relaxation: the server enforces each of those
     * rules itself, only at different moments. Name (`@Length(min=1,max=255)`, `ExperimentForm`) and
     * page (non-optional on the immutable, re-resolved at `ExperimentsAPIImpl.java:177`) are checked
     * on every save; goal conditions and parameters by `MetricsUtil.validateGoals` on every save;
     * weights adding up to 100 by a `@Value.Check` on `TrafficProportion` *construction*, so any
     * PATCH carrying an intermediate total is a 400 — which is why the store holds that key back;
     * the scheduling min/max by `validateScheduling` on any PATCH that changes it; and the two
     * variants only at `start()` (`hasAtLeastOneVariant`, which is really `> 1`).
     */
    protected readonly formTree = form(this.$model, (path) => {
        const isLocked = () => this.store.$isLocked();

        maxLength(path.name, MAX_INPUT_TITLE_LENGTH);
        disabled(path.name, { when: isLocked });

        maxLength(path.description, MAX_INPUT_DESCRIPTIVE_LENGTH);
        disabled(path.description, { when: isLocked });

        min(path.trafficAllocation, MIN_TRAFFIC_ALLOCATION);
        max(path.trafficAllocation, MAX_TRAFFIC_ALLOCATION);
        disabled(path.trafficAllocation, { when: isLocked });

        // One `disabled` rule covers a whole slice: a disabled field disables everything under it,
        // so every condition control of the goal follows without being named.
        maxLength(path.goal.name, MAX_INPUT_DESCRIPTIVE_LENGTH);
        disabled(path.goal, { when: isLocked });

        // The pickers already keep an out-of-bounds date out of reach. These flag one that arrived
        // from the server anyway, rather than silently discarding it as the old screen did.
        minDate(path.scheduling.startDate, this.#now);
        minDate(path.scheduling.endDate, () => this.$schedulingBounds().minEndDate);
        maxDate(path.scheduling.endDate, () => this.$schedulingBounds().maxEndDate);
        disabled(path.scheduling, { when: isLocked });

        // The weights answer to the page's lock too, not only to the status: they are the one part
        // of the form a variant endpoint also writes, and the card has always frozen them for both.
        const areWeightsLocked = () => !!this.store.$disabledTooltipKey();

        applyEach(path.variantWeights, (row) => {
            min(row.weight, 0);
            max(row.weight, TOTAL_WEIGHT);
            disabled(row, { when: areWeightsLocked });
        });

        // Cross-field, so it belongs to the array and not to any row: a single weight is only wrong
        // in the company of the others.
        validate(path.variantWeights, ({ value }) => {
            const rows = value();

            return !rows.length || totalWeight(rows) === TOTAL_WEIGHT
                ? undefined
                : { kind: WEIGHTS_TOTAL_ERROR_KIND };
        });
    });

    /**
     * The scrolling region, which is also the only place a `[data-error]` can be.
     *
     * `protected` rather than `#private`: Angular rejects a signal query declared on an ES private
     * member (NG1053), since it has to write to it from outside the class.
     */
    protected readonly $body = viewChild<ElementRef<HTMLElement>>('configureBody');

    /**
     * Shown when the experiment could not be loaded. The error itself is already surfaced by
     * `DotHttpErrorManagerService`; this is the screen's own state, so a failed load reads as a
     * failure with a way out rather than as an empty form.
     */
    readonly errorConfiguration: PrincipalConfiguration = {
        title: this.#dotMessageService.get('experiments.list.error.title'),
        subtitle: this.#dotMessageService.get('experiments.error.fetching.data'),
        icon: 'error',
        iconStyle: 'material-symbols-rounded'
    };

    /** Identifier of the experiment whose values are already in the form. */
    readonly #hydratedExperimentId = signal<string | null>(null);

    /**
     * Fills the form from the store once per experiment.
     *
     * Keyed on the experiment's identifier rather than on the values themselves: every autosave
     * response replaces `experiment`, and re-reading it would drop characters typed while the PATCH
     * was travelling. On the creation screen nothing is read at all — see `#isCreationScreen`.
     */
    protected readonly hydrateFormEffect = effect(() => {
        const experimentId = this.store.experiment()?.id ?? null;

        if (
            this.#isCreationScreen ||
            !experimentId ||
            experimentId === untracked(this.#hydratedExperimentId)
        ) {
            return;
        }

        untracked(() => {
            this.#hydratedExperimentId.set(experimentId);
            this.$model.set(toConfigureFormModel(this.#formSource()));
        });
    });

    /**
     * Re-seeds the weights slice whenever the *set* of variants changes.
     *
     * Keyed on which variants there are, not on what they weigh: an added or a deleted variant is a
     * row appearing or disappearing, and the slice has to follow — including the first load, and the
     * handover from the creation screen, where the POST is what creates the control. A save response
     * echoing the weights back changes no identity, so a weight being typed survives it, exactly as
     * it survives on the store's side (see `withoutSentKeys`).
     *
     * The slice already standing for the current set is left alone whatever it holds, which is also
     * what lets the card re-split the weights the moment a variant is added (AC24) without this
     * putting the backend's own back.
     */
    protected readonly seedVariantWeightsEffect = effect(() => {
        const variants = this.store.$variants();

        untracked(() => {
            if (hasSameVariantIdentity(this.$model().variantWeights, variants)) {
                return;
            }

            this.$model.update((model) => ({
                ...model,
                variantWeights: toVariantWeightRows(variants)
            }));
        });
    });

    /**
     * Reports the weights as they are edited — including while they do not add up.
     *
     * The one key that travels invalid, deliberately: the rows on screen, the total under them and
     * the warning count a Start press produces all read the store's copy, so holding an intermediate
     * total back here would leave the screen describing something nobody typed. What must not reach
     * the server is a *PATCH* carrying one, and that gate is the store's (`toOutgoingPatch`), where
     * the key waits until the total is 100 again.
     *
     * Compared against the persisted weights rather than dispatched on every change: the store
     * applies each edit to its own copy of the experiment, so a reported edit comes straight back as
     * a change of `$variants` — and a binding that did not recognise its own echo would never stop.
     */
    protected readonly persistVariantWeightsEffect = effect(() => {
        const rows = this.$model().variantWeights;

        untracked(() => {
            const variants = this.store.$variants();

            // A slice that no longer stands for the current variants is mid-reseed and says nothing
            // about weights yet; weights the store already holds are its own echo.
            if (!hasSameVariantIdentity(rows, variants) || isSameVariantWeights(rows, variants)) {
                return;
            }

            this.#dispatch.formEdited({
                trafficProportion: {
                    type: TrafficProportionTypes.CUSTOM_PERCENTAGES,
                    variants: mergeVariantWeights(variants, rows)
                }
            });
        });
    });

    /**
     * The one autosave of the screen: whatever the form holds that the store does not is reported
     * as a single edit, and the store debounces it into one PATCH (AC6).
     */
    protected readonly persistFormEffect = bindFormAutosave<ConfigureFormModel>({
        model: this.$model,
        toPatch: (model) => this.#toOutgoingPatch(model)
    });

    /**
     * Reveals the first field that failed validation.
     *
     * `validationErrors` is only ever filled by a Start/Schedule press, so this fires exactly when
     * an invalid start was attempted. The scroll waits for the next render because the cards
     * reveal their `[data-error]` markers from the same signal — searching before they are in the
     * DOM would find nothing.
     */
    protected readonly scrollToFirstErrorEffect = effect(() => {
        const hasErrors = this.store.validationErrors().length > 0;

        if (!hasErrors) {
            return;
        }

        untracked(() =>
            afterNextRender(() => this.scrollToFirstValidationError(), { injector: this.#injector })
        );
    });

    constructor() {
        this.#listenForActionSuccess();
    }

    /** Brings the first failing field into view. Public so the footer can re-run it on a re-press. */
    scrollToFirstValidationError(): void {
        const firstError = this.$body()?.nativeElement.querySelector<HTMLElement>('[data-error]');

        firstError?.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }

    /** Leaves the Configure screen for the list. */
    onBackToList(): void {
        this.#router.navigate([EXPERIMENTS_URL]);
    }

    /**
     * The keys the form holds that the store does not, with the two bounded slices reporting
     * whether they are worth sending: an out-of-range allocation or an out-of-window end date is
     * shown on screen rather than PATCHed.
     */
    #toOutgoingPatch(model: ConfigureFormModel): DotExperimentPatchBody {
        return toConfigurePatch(model, this.#formSource(), {
            trafficAllocation: this.formTree.trafficAllocation().valid(),
            scheduling: this.formTree.scheduling().valid()
        });
    }

    /** What the form is filled from, and diffed against. */
    #formSource(): ConfigureFormSource {
        return {
            experiment: this.store.experiment(),
            draftName: this.store.draftName(),
            draftDescription: this.store.draftDescription()
        };
    }

    /**
     * The store persists and reloads on its own; the toast is a UI concern and therefore lives
     * here. Only the outcomes the user asked for get one — a failed call is already reported by
     * `DotHttpErrorManagerService` inside the store, and autosave is deliberately silent.
     */
    #listenForActionSuccess(): void {
        const successMessages: ReadonlyArray<
            [EventCreator<string, DotExperiment>, (experiment: DotExperiment) => string]
        > = [
            [
                dotExperimentsConfigureApiEvents.createSucceeded,
                () => 'experiments.configure.notification.created'
            ],
            [
                dotExperimentsConfigureApiEvents.startSucceeded,
                // A start dated in the future schedules the experiment instead of running it,
                // and the server's answer is what says which of the two happened.
                ({ status }) =>
                    status === DotExperimentStatus.SCHEDULED
                        ? 'experiments.action.scheduled.confirm-message'
                        : 'experiments.action.start.confirm-message'
            ],
            [
                dotExperimentsConfigureApiEvents.stopSucceeded,
                () => 'experiments.action.stop.confirm-message'
            ],
            [
                dotExperimentsConfigureApiEvents.cancelScheduleSucceeded,
                () => 'experiments.notification.cancel.schedule'
            ],
            [
                dotExperimentsConfigureApiEvents.abortSucceeded,
                () => 'experiments.notification.abort'
            ]
        ];

        successMessages.forEach(([event, messageKeyOf]) => {
            this.#events
                .on(event)
                .pipe(takeUntilDestroyed(this.#destroyRef))
                .subscribe(({ payload }) =>
                    this.#dotMessageDisplayService.push({
                        life: SUCCESS_MESSAGE_LIFE,
                        severity: DotMessageSeverity.SUCCESS,
                        message: this.#dotMessageService.get(messageKeyOf(payload), payload.name),
                        type: DotMessageType.SIMPLE_MESSAGE
                    })
                );
        });
    }
}
