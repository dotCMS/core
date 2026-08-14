import { EventCreator, Events } from '@ngrx/signals/events';

import {
    afterNextRender,
    Component,
    computed,
    DestroyRef,
    effect,
    ElementRef,
    inject,
    Injector,
    untracked,
    viewChild
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';

import { ConfirmationService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
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
    DotExperimentStatus,
    DotMessageSeverity,
    DotMessageType
} from '@dotcms/dotcms-models';
import { DotEmptyContainerComponent, DotMessagePipe, PrincipalConfiguration } from '@dotcms/ui';

import { DotExperimentsConfigureDetailsComponent } from './components/dot-experiments-configure-details/dot-experiments-configure-details.component';
import { DotExperimentsConfigureFooterComponent } from './components/dot-experiments-configure-footer/dot-experiments-configure-footer.component';
import { DotExperimentsConfigureGoalComponent } from './components/dot-experiments-configure-goal/dot-experiments-configure-goal.component';
import { DotExperimentsConfigureHeaderComponent } from './components/dot-experiments-configure-header/dot-experiments-configure-header.component';
import { DotExperimentsConfigurePageComponent } from './components/dot-experiments-configure-page/dot-experiments-configure-page.component';
import { DotExperimentsConfigureSchedulingComponent } from './components/dot-experiments-configure-scheduling/dot-experiments-configure-scheduling.component';
import { DotExperimentsConfigureVariantsComponent } from './components/dot-experiments-configure-variants/dot-experiments-configure-variants.component';

import { EXPERIMENTS_URL, SUCCESS_MESSAGE_LIFE } from '../shared/constants';
import { dotExperimentsConfigureApiEvents } from '../store/dot-experiments-configure-api.events';
import { DotExperimentsConfigureStore } from '../store/dot-experiments-configure.store';

/** Number of card placeholders drawn while an existing experiment loads. */
const SKELETON_CARDS = [0, 1, 2];

/**
 * Shell of the Configure screen, routed on both `/experiments/new` and
 * `/experiments/:experimentId/configuration`.
 *
 * It owns the fixed-height layout the cards sit in — a header that stays put, an optional
 * read-only banner, a scrolling body and a pinned footer — plus everything that is screen-wide
 * rather than card-wide: the success toasts, and scrolling to the first field that failed
 * validation, since the body element it has to search is this component's.
 *
 * Which experiment to show is not read here: the store's `onInit` reads the route itself, so the
 * shell only provides it and the two services it injects that are not `providedIn: 'root'`.
 */
@Component({
    selector: 'dot-experiments-configure',
    imports: [
        ConfirmDialogModule,
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

    readonly #router = inject(Router);
    readonly #events = inject(Events);
    readonly #injector = inject(Injector);
    readonly #destroyRef = inject(DestroyRef);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #dotMessageDisplayService = inject(DotMessageDisplayService);

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
