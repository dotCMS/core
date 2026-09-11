import { EventCreator, Events, injectDispatch } from '@ngrx/signals/events';

import { formatDate } from '@angular/common';
import {
    afterNextRender,
    Component,
    computed,
    DestroyRef,
    effect,
    ElementRef,
    inject,
    Injector,
    LOCALE_ID,
    signal,
    untracked,
    viewChild
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
    applyEach,
    disabled,
    form,
    FormRoot,
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

import { take } from 'rxjs/operators';

import {
    DotExperimentsService,
    DotMessageDisplayService,
    DotMessageService,
    DotPagesBrowserService
} from '@dotcms/data-access';
import {
    CONFIGURE_SECTION_PARAM,
    CONFIGURE_SECTION_VARIANTS,
    ComponentStatus,
    CONFIGURATION_CONFIRM_DIALOG_KEY,
    DotExperiment,
    DotExperimentStatus,
    DotMessageSeverity,
    DotMessageType,
    MAX_INPUT_DESCRIPTIVE_LENGTH,
    MAX_INPUT_TITLE_LENGTH
} from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';
import { DotEmptyContainerComponent, DotMessagePipe, PrincipalConfiguration } from '@dotcms/ui';

import { DotExperimentsConfigureDetailsComponent } from './components/dot-experiments-configure-details/dot-experiments-configure-details.component';
import { DotExperimentsConfigureFooterComponent } from './components/dot-experiments-configure-footer/dot-experiments-configure-footer.component';
import { DotExperimentsConfigureGoalComponent } from './components/dot-experiments-configure-goal/dot-experiments-configure-goal.component';
import { DotExperimentsConfigureHeaderComponent } from './components/dot-experiments-configure-header/dot-experiments-configure-header.component';
import { DotExperimentsConfigurePageComponent } from './components/dot-experiments-configure-page/dot-experiments-configure-page.component';
import { DotExperimentsConfigureSchedulingComponent } from './components/dot-experiments-configure-scheduling/dot-experiments-configure-scheduling.component';
import { DotExperimentsConfigureVariantsComponent } from './components/dot-experiments-configure-variants/dot-experiments-configure-variants.component';

import {
    CONFIGURE_TITLE_KEY,
    EXPERIMENT_ID_ROUTE_PARAM,
    EXPERIMENTS_URL,
    LIST_TITLE_KEY,
    MAX_TRAFFIC_ALLOCATION,
    MIN_PROGRESS_BAR_VISIBLE_MS,
    MIN_TRAFFIC_ALLOCATION,
    NEW_EXPERIMENT_TITLE_KEY,
    SUCCESS_MESSAGE_LIFE,
    TOTAL_WEIGHT,
    WEIGHTS_TOTAL_ERROR_KIND
} from '../shared/constants';
import { ConfigureFormModel, SchedulingDateBounds } from '../shared/models';
import { dotExperimentsConfigureApiEvents } from '../store/dot-experiments-configure-api.events';
import { dotExperimentsConfigurePageEvents } from '../store/dot-experiments-configure-page.events';
import { DotExperimentsConfigureStore } from '../store/dot-experiments-configure.store';
import {
    EXPERIMENTS_LIST_CRUMB_ID,
    experimentConfigureCrumb,
    experimentsListCrumb,
    putCrumbOnTrail
} from '../util/dot-experiments-breadcrumb.util';
import {
    ConfigureFormSource,
    emptyConfigureForm,
    hasSameVariantIdentity,
    nextHalfHour,
    resolveDurationBounds,
    toConfigureFormModel,
    toVariantWeightRows
} from '../util/dot-experiments-configure-form.util';
import { totalWeight } from '../util/dot-experiments-configure.util';
import { listReturnParams } from '../util/dot-experiments-list.util';

/** Number of card placeholders drawn while an existing experiment loads. */
const SKELETON_CARDS = [0, 1, 2];

/** Route `data` key `DotExperimentsConfigResolver` publishes the backend's duration limits under. */
const CONFIG_ROUTE_DATA_KEY = 'config';
/** The Variants card, as this screen addresses it — the anchor lives on the element it scrolls. */
const VARIANTS_SECTION_SELECTOR = '[data-testid="configure-section-variants"]';

/**
 * Which experiment, in which state, the form is currently filled from — `null` for no experiment
 * at all, which is the creation form.
 *
 * The status is part of the identity on purpose: it is what tells a transition's answer apart from
 * an autosave's, and only the former may refill the form under the user. See
 * {@link DotExperimentsConfigureComponent.hydrateFormEffect}.
 */
function formKeyOf(experiment: DotExperiment | null | undefined): string | null {
    return experiment ? `${experiment.id}:${experiment.status}` : null;
}

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
        FormRoot,
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
    host: {
        class: 'flex flex-col h-full min-h-0 overflow-hidden animate-fadein animate-duration-180 animate-ease-out motion-reduce:animate-none'
    }
})
export class DotExperimentsConfigureComponent {
    readonly store = inject(DotExperimentsConfigureStore);

    readonly CONFIRM_KEY = CONFIGURATION_CONFIRM_DIALOG_KEY;

    /**
     * Public so `experimentsUnsavedChangesGuard` can raise its prompt on the same instance that
     * backs the `<p-confirmDialog>` in this template — the service is provided here, so injecting
     * it in the guard would resolve a different one and the dialog would never open.
     */
    readonly confirmationService = inject(ConfirmationService);
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
     * Whether Goal, Variants and Scheduling are still behind the save gate.
     *
     * Everything those three cards hold is written by `PATCH /experiments/{id}`, so until the
     * draft exists there is nowhere for it to go — the Variants card could not even add a row.
     * Details is the way through: it carries the Name and the Page the creation POST needs.
     */
    readonly $isGated = computed<boolean>(() => this.store.isNew());

    /**
     * Variants are gated for one more reason than the rest of the form: a confirmed page change in
     * flight.
     *
     * They are copies of the page, and the server takes `pageId` only while the variants are the
     * control alone — so a variant created before the change lands is created under the old page,
     * and from then on the change can never be written. The other cards do not depend on the page
     * and stay live (#37005).
     */
    readonly $isVariantsGated = computed<boolean>(
        () => this.$isGated() || this.store.pageChanging()
    );

    /**
     * Indeterminate bar under the header while a request is on the wire — the same affordance UVE
     * gives its autosave.
     *
     * Deliberately not the whole pending window: the debounce runs from the first keystroke for as
     * long as the user types, and a prominent animated bar for that long reads as a stuck
     * operation. The footer's "Saving…" copy covers that window instead (`$isAutosaving`); the bar
     * covers the flight — `$isSaving`, which is every request this screen makes, not only the
     * autosave: a PATCH via `saveRequested`, the creation POST via `creating`, a variant
     * add/rename/delete, and a start/stop/cancel-schedule/abort.
     *
     * Nor is it exactly the flight, in the other direction: once shown the bar stays for at least
     * `MIN_PROGRESS_BAR_VISIBLE_MS` (see the constant for why). The store keeps reporting the
     * truth; how long that truth is worth showing is this screen's business.
     */
    readonly $showProgressBar = signal(false);

    /** When the bar currently on screen appeared, so the hold can be measured from it. */
    #progressBarShownAt = 0;

    /** A hide waiting out the remainder of the window, cancelled if another save starts first. */
    #progressBarHideTimeout: ReturnType<typeof setTimeout> | null = null;

    protected readonly holdProgressBarEffect = effect(() => {
        const isSaving = this.store.$isSaving();

        untracked(() => this.#trackSavingBar(isSaving));
    });

    /**
     * Identifier of the experiment this screen is on, or `null` while the draft has none.
     *
     * Read from the store first and from the address second, because the two lead by turns:
     * creating the draft fills the store immediately and rewrites the URL right after, and a
     * screen entered on an existing experiment has the id in the address before the load answers.
     * One derivation, so the crumb's label and its address can never disagree about which of the
     * screen's two states it is in.
     */
    readonly $experimentId = computed<string | null>(
        () =>
            this.store.experiment()?.id ??
            this.#route.snapshot.paramMap.get(EXPERIMENT_ID_ROUTE_PARAM)
    );

    /**
     * Puts this screen on the breadcrumb trail (#37005).
     *
     * Without it the trail ended at the list's crumb, so the shell rendered "Experiments List" as
     * the title of a screen that is not the list — and the list, which *is* a level above, was
     * missing from the path instead of sitting in it.
     *
     * The label is the screen, not the experiment: the experiment is already named right below, in
     * the header, next to its status and its page. Naming it twice on the same screen says nothing
     * the second time, and the crumb is the one place that has to say *where* you are — which,
     * before the draft exists, is the New Experiment screen rather than the Configure one.
     *
     * An effect rather than a one-shot, because the screen changes which of those two it is
     * without being left: creating the draft swaps `/experiments/new` for the experiment's own
     * address. The crumb keeps one id across that swap, and `putCrumbOnTrail` rewrites the crumb
     * already carrying it, so it does not stack a second one.
     */
    protected readonly syncBreadcrumbEffect = effect(() => {
        const experimentId = this.$experimentId();
        const label = this.#dotMessageService.get(
            experimentId ? CONFIGURE_TITLE_KEY : NEW_EXPERIMENT_TITLE_KEY
        );

        untracked(() => this.#syncBreadcrumb(label, experimentId));
    });

    readonly #route = inject(ActivatedRoute);
    readonly #router = inject(Router);
    readonly #events = inject(Events);
    /** Only the weights are reported from here; everything else goes through `bindFormDiff`. */
    readonly #dispatch = injectDispatch(dotExperimentsConfigurePageEvents);
    readonly #injector = inject(Injector);
    readonly #destroyRef = inject(DestroyRef);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #globalStore = inject(GlobalStore);
    readonly #locale = inject(LOCALE_ID);

    /** Same format the pickers' own copy uses, so the bounds read the same wherever they appear. */
    readonly #formatDate = (value: Date) => formatDate(value, 'medium', this.#locale);
    readonly #dotMessageDisplayService = inject(DotMessageDisplayService);

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

        // Each rule carries its own message, so a card renders whatever its field reports instead of
        // enumerating the kinds it knows about. Resolved lazily: the schema runs once, at construction.
        maxLength(path.name, MAX_INPUT_TITLE_LENGTH, {
            message: () =>
                this.#dotMessageService.get(
                    'experiments.configure.details.name.max-length',
                    String(MAX_INPUT_TITLE_LENGTH)
                )
        });
        disabled(path.name, { when: isLocked });

        maxLength(path.description, MAX_INPUT_DESCRIPTIVE_LENGTH, {
            message: () =>
                this.#dotMessageService.get(
                    'experiments.configure.details.description.max-length',
                    String(MAX_INPUT_DESCRIPTIVE_LENGTH)
                )
        });
        disabled(path.description, { when: isLocked });

        const trafficRangeMessage = () =>
            this.#dotMessageService.get(
                'experiments.configure.page.traffic.range.error',
                String(MIN_TRAFFIC_ALLOCATION),
                String(MAX_TRAFFIC_ALLOCATION)
            );

        min(path.trafficAllocation, MIN_TRAFFIC_ALLOCATION, { message: trafficRangeMessage });
        max(path.trafficAllocation, MAX_TRAFFIC_ALLOCATION, { message: trafficRangeMessage });
        disabled(path.trafficAllocation, { when: isLocked });

        // One `disabled` rule covers a whole slice: a disabled field disables everything under it,
        // so every condition control of the goal follows without being named.
        maxLength(path.goal.name, MAX_INPUT_DESCRIPTIVE_LENGTH);
        disabled(path.goal, { when: isLocked });

        // The pickers already keep an out-of-bounds date out of reach. These flag one that arrived
        // from the server anyway, rather than silently discarding it as the old screen did.
        const endDateBoundsMessage = () => {
            const { minEndDate, maxEndDate } = this.$schedulingBounds();

            return this.#dotMessageService.get(
                'experiments.configure.scheduling.end.error.out-of-bounds',
                this.#formatDate(minEndDate),
                this.#formatDate(maxEndDate)
            );
        };

        minDate(path.scheduling.startDate, this.#now, {
            message: () =>
                this.#dotMessageService.get('experiments.configure.scheduling.start.error.past')
        });
        minDate(path.scheduling.endDate, () => this.$schedulingBounds().minEndDate, {
            message: endDateBoundsMessage
        });
        maxDate(path.scheduling.endDate, () => this.$schedulingBounds().maxEndDate, {
            message: endDateBoundsMessage
        });
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

            if (!rows.length || totalWeight(rows) === TOTAL_WEIGHT) {
                return undefined;
            }

            // The total is right here, so the message quoting it is written where it is known.
            return {
                kind: WEIGHTS_TOTAL_ERROR_KIND,
                message: this.#dotMessageService.get(
                    'experiments.configure.variants.weights.warning',
                    String(totalWeight(rows))
                )
            };
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

    /**
     * Which experiment's values are in the form, and in which state — see {@link formKeyOf}.
     * `null` means an empty creation form: the screen opened on `/experiments/new`, or a URL took
     * it back there.
     */
    readonly #hydratedFormKey = signal<string | null>(null);

    /**
     * Keeps the form on whichever experiment the store is showing, filling it once per experiment
     * *state*.
     *
     * Not on the values: every autosave response replaces `experiment`, and re-reading it would
     * drop characters typed while the PATCH was travelling. But not on the identifier alone
     * either — a status transition is the one response that is not an autosave. The server dates
     * the experiment as part of running it, so an experiment started with both pickers empty comes
     * back carrying a real window, and keying on the id alone left the card locked showing the two
     * empty pickers it was started with. Nothing can be in flight at that moment for the refill to
     * lose: the transition is what locks the card.
     *
     * A draft created on this screen is claimed as hydrated the moment its POST answers (see the
     * constructor), which is what keeps this from reading it back: the form is what created it, and
     * a goal or a schedule entered before the name is still on its way to the server.
     */
    protected readonly hydrateFormEffect = effect(() => {
        const formKey = formKeyOf(this.store.experiment());

        if (formKey === untracked(this.#hydratedFormKey)) {
            return;
        }

        untracked(() => {
            this.#hydratedFormKey.set(formKey);
            this.$model.set(
                formKey ? toConfigureFormModel(this.#formSource()) : emptyConfigureForm()
            );
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
     * Mirrors the form into the store on every change.
     *
     * The whole value, not a diff. The store holds one copy of what is on screen and one of what
     * was last written, and comparing those is the entire dirty state; a save then sends the form
     * as it stands. Working out which individual keys had moved bought nothing once saving stopped
     * racing the keystrokes, and cost a layer of bookkeeping that had to be right in both
     * directions.
     *
     * `untracked` still guards the dispatch: the store re-seeds the form from every response, so a
     * binding that read anything else would turn one save into a round trip.
     */
    protected readonly mirrorFormEffect = effect(() => {
        const value = this.$model();

        untracked(() => {
            this.#dispatch.formChanged({
                value,
                validity: {
                    trafficAllocation: this.formTree.trafficAllocation().valid(),
                    scheduling: this.formTree.scheduling().valid()
                }
            });
        });
    });

    constructor() {
        this.#listenForActionSuccess();
        this.#scrollToFirstErrorOnFailedStart();
        this.#scrollToRequestedSection();

        // The form created this draft, so it already holds it: claiming it here is what stops
        // `hydrateFormEffect` from reading the POST's answer back over what is still being typed.
        this.#events
            .on(dotExperimentsConfigureApiEvents.createSucceeded)
            .pipe(takeUntilDestroyed(this.#destroyRef))
            .subscribe(({ payload }) => this.#hydratedFormKey.set(formKeyOf(payload)));

        // A save that settled as the screen was left would otherwise fire into a dead component.
        this.#destroyRef.onDestroy(() => this.#cancelProgressBarHide());
    }

    /**
     * Lands on the section the caller asked for, instead of at the top of the form (#37005).
     *
     * Only the variant round-trip asks: it starts and ends at the Variants card, three cards down,
     * and returning to the top of the form loses the reader's place. Everything else &mdash; the
     * list, creating one &mdash; wants the top, and gets it by not asking.
     *
     * Read once from the snapshot, and keyed on `loadSucceeded` rather than on a signal: the cards
     * only exist once the experiment is in, so searching earlier finds nothing. Same shape, and
     * same reason, as {@link #scrollToFirstErrorOnFailedStart}.
     */
    #scrollToRequestedSection(): void {
        const requested = this.#route.snapshot.queryParamMap.get(CONFIGURE_SECTION_PARAM);

        if (requested !== CONFIGURE_SECTION_VARIANTS) {
            return;
        }

        this.#events
            .on(dotExperimentsConfigureApiEvents.loadSucceeded)
            .pipe(take(1), takeUntilDestroyed(this.#destroyRef))
            .subscribe(() => {
                afterNextRender(
                    () =>
                        this.$body()
                            ?.nativeElement.querySelector<HTMLElement>(VARIANTS_SECTION_SELECTOR)
                            ?.scrollIntoView({ block: 'start' }),
                    { injector: this.#injector }
                );
            });
    }

    /**
     * Brings the first failing field into view when a Start/Schedule press did not get through.
     *
     * Keyed on the press, not on the error list: the errors are derived from the form, so an
     * effect over them would scroll again on every keystroke that changed one. The scroll waits
     * for the next render because the cards reveal their `[data-error]` markers from the same
     * press — searching before they are in the DOM would find nothing.
     */
    #scrollToFirstErrorOnFailedStart(): void {
        this.#events
            .on(dotExperimentsConfigurePageEvents.startRequested)
            .pipe(takeUntilDestroyed(this.#destroyRef))
            .subscribe(() => {
                if (!this.store.$validationErrors().length) {
                    return;
                }

                afterNextRender(() => this.scrollToFirstValidationError(), {
                    injector: this.#injector
                });
            });
    }

    /**
     * Appends this screen's crumb, and the list's above it when the trail does not already carry
     * one.
     *
     * The list's crumb is normally already there — it puts itself on the trail when it renders,
     * and this screen is reached through it. It is not there on a reload of a deep link into a
     * fresh session, and the level above still belongs in the path, so it is added rather than
     * assumed. Guarded on the trail's contents rather than on an id match: `addNewBreadcrumb` only
     * replaces the *last* crumb, so re-adding the list while this screen's crumb sits on top of it
     * would append a second copy at the end.
     *
     * Both crumbs keep the page narrowing in their address, for the same reason the back button
     * carries it: a return that dropped the filter would leave the screen it returns to pointing
     * at the site-wide list.
     */
    #syncBreadcrumb(label: string, experimentId: string | null): void {
        const pageFilter = listReturnParams(this.#route.snapshot.queryParams);
        const hasListCrumb = this.#globalStore
            .breadcrumbs()
            .some(({ id }) => id === EXPERIMENTS_LIST_CRUMB_ID);

        if (!hasListCrumb) {
            putCrumbOnTrail(
                this.#globalStore,
                experimentsListCrumb(this.#dotMessageService.get(LIST_TITLE_KEY), pageFilter)
            );
        }

        putCrumbOnTrail(
            this.#globalStore,
            experimentConfigureCrumb(label, experimentId, pageFilter)
        );
    }

    /**
     * Shows the bar as soon as a request leaves, and holds it for the rest of the window once the
     * request settles inside it.
     *
     * A save starting while a hide is pending cancels that hide and leaves the bar where it is,
     * rather than restarting the window: back-to-back saves read as one continuous bar instead of
     * a blink between them.
     */
    #trackSavingBar(isSaving: boolean): void {
        if (isSaving) {
            this.#cancelProgressBarHide();

            if (!this.$showProgressBar()) {
                this.#progressBarShownAt = Date.now();
                this.$showProgressBar.set(true);
            }

            return;
        }

        if (!this.$showProgressBar() || this.#progressBarHideTimeout) {
            return;
        }

        const remaining = MIN_PROGRESS_BAR_VISIBLE_MS - (Date.now() - this.#progressBarShownAt);

        if (remaining <= 0) {
            this.$showProgressBar.set(false);

            return;
        }

        this.#progressBarHideTimeout = setTimeout(() => {
            this.#progressBarHideTimeout = null;
            this.$showProgressBar.set(false);
        }, remaining);
    }

    #cancelProgressBarHide(): void {
        if (this.#progressBarHideTimeout) {
            clearTimeout(this.#progressBarHideTimeout);
            this.#progressBarHideTimeout = null;
        }
    }

    /** Brings the first failing field into view. Public so the footer can re-run it on a re-press. */
    scrollToFirstValidationError(): void {
        const firstError = this.$body()?.nativeElement.querySelector<HTMLElement>('[data-error]');

        firstError?.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }

    /**
     * Leaves the Configure screen for the list it was opened from, narrowing included (FR-021c).
     *
     * The error state's only way out, and it has the same job as the header's back arrow — so it
     * answers with the same address. See {@link listReturnParams}.
     */
    onBackToList(): void {
        this.#router.navigate([EXPERIMENTS_URL], {
            queryParams: listReturnParams(this.#route.snapshot.queryParams)
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
