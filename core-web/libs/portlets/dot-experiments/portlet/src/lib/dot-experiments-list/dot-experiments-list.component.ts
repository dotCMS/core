import { EventCreator, Events, injectDispatch } from '@ngrx/signals/events';

import { DatePipe } from '@angular/common';
import {
    Component,
    computed,
    debounced,
    DestroyRef,
    effect,
    inject,
    linkedSignal,
    signal,
    untracked
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { ConfirmationService, MenuItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { MenuModule } from 'primeng/menu';
import { SkeletonModule } from 'primeng/skeleton';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ToolbarModule } from 'primeng/toolbar';

import {
    DotExperimentsService,
    DotMessageDisplayService,
    DotMessageService
} from '@dotcms/data-access';
import { DotPushPublishDialogService } from '@dotcms/dotcms-js';
import {
    ComponentStatus,
    CONFIGURATION_CONFIRM_DIALOG_KEY,
    DotExperiment,
    DotExperimentStatus,
    ExperimentsStatusList,
    GOAL_TYPES,
    DotMessageSeverity,
    DotMessageType,
    GOALS_METADATA_MAP,
    HealthStatusTypes
} from '@dotcms/dotcms-models';
import {
    DotAddToBundleComponent,
    DotEmptyContainerComponent,
    DotMessagePipe,
    PrincipalConfiguration
} from '@dotcms/ui';

import { DotExperimentListFilterComponent } from '../components/dot-experiment-list-filter/dot-experiment-list-filter.component';
import {
    GOAL_LABEL_KEYS,
    NO_GOAL_PLACEHOLDER,
    ROWS_PER_PAGE_OPTIONS,
    SEARCH_DEBOUNCE_MS,
    SKELETON_COLUMNS,
    SKELETON_ROWS,
    STATUS_LABEL_KEYS,
    STATUS_SEVERITIES,
    SUCCESS_MESSAGE_LIFE
} from '../shared/constants';
import {
    DotExperimentsListSortDirection,
    ExperimentFilterOption,
    ExperimentRow
} from '../shared/models';
import { dotExperimentsApiEvents } from '../store/dot-experiments-api.events';
import { dotExperimentsListPageEvents } from '../store/dot-experiments-list-page.events';
import { DotExperimentsListStore } from '../store/dot-experiments-list.store';
import {
    ExperimentScheduleLabels,
    formatSchedule,
    goalTypeOf,
    isAllowed,
    resolvePagePath,
    variantsCount
} from '../util/dot-experiments-list.util';

/** Mount point of the portlet. Absolute, since the list is always at the root of it. */
const EXPERIMENTS_URL = '/experiments';

/** Where the New Experiment button goes: the Configure screen with nothing created yet. */
const NEW_EXPERIMENT_COMMANDS = [EXPERIMENTS_URL, 'new'];

/** Configure URL of an experiment that already exists. */
const configureCommandsOf = (experimentId: string): string[] => [
    EXPERIMENTS_URL,
    experimentId,
    'configuration'
];

@Component({
    selector: 'dot-experiments-list',
    imports: [
        DatePipe,
        FormsModule,
        ButtonModule,
        ConfirmDialogModule,
        IconFieldModule,
        InputIconModule,
        InputTextModule,
        MenuModule,
        SkeletonModule,
        TableModule,
        TagModule,
        ToolbarModule,
        DotAddToBundleComponent,
        DotEmptyContainerComponent,
        DotExperimentListFilterComponent,
        DotMessagePipe
    ],
    templateUrl: './dot-experiments-list.component.html',
    styleUrls: ['./dot-experiments-list.component.scss'],
    // `DotExperimentsService` is `@Injectable()` with no `providedIn` and is not in the app-wide
    // `providers.ts`, so the store cannot inject it unless this component provides it. The legacy
    // screens do the same in `old/dot-experiments-shell`.
    providers: [DotExperimentsListStore, ConfirmationService, DotExperimentsService],
    host: { class: 'flex flex-col h-full min-h-0' }
})
export class DotExperimentsListComponent {
    readonly store = inject(DotExperimentsListStore);

    readonly CONFIRM_KEY = CONFIGURATION_CONFIRM_DIALOG_KEY;
    readonly NO_GOAL_PLACEHOLDER = NO_GOAL_PLACEHOLDER;
    readonly ROWS_PER_PAGE_OPTIONS = ROWS_PER_PAGE_OPTIONS;
    readonly SKELETON_COLUMNS = SKELETON_COLUMNS;

    /** Rows currently rendered by the table, already resolved for display. */
    readonly $rows = computed<ExperimentRow[]>(() => {
        const pageInfoByPageId = this.store.pageInfoByPageId();
        const scheduleLabels = this.#scheduleLabels;

        return this.store.pagedExperiments().map((experiment) => {
            const goalType = goalTypeOf(experiment.goals);

            return {
                experiment,
                pagePath: resolvePagePath(experiment.pageId, pageInfoByPageId),
                goalLabelKey: goalType ? GOALS_METADATA_MAP[goalType].label : null,
                variants: variantsCount(experiment.trafficProportion),
                schedule: formatSchedule(experiment.scheduling, scheduleLabels),
                statusSeverity: STATUS_SEVERITIES[experiment.status] ?? 'secondary',
                statusLabelKey: STATUS_LABEL_KEYS.get(experiment.status) ?? ''
            };
        });
    });

    /**
     * What the table renders. During the very first load there is nothing to show yet, so the
     * value is padded with placeholders and the body template swaps every cell for a skeleton.
     */
    readonly $tableValue = computed<ExperimentRow[]>(() => {
        const rows = this.$rows();

        return this.$isLoading() && rows.length === 0 ? SKELETON_ROWS : rows;
    });

    /**
     * The screen is loading while the Analytics health check is still out (`healthStatus` null)
     * as well as while the list itself is in flight.
     *
     * Folding the two together means the skeleton is on screen from the first paint instead of
     * a blank shell, and the skeleton → rows transition is seamless because both render the
     * same table. The trade-off is a swap when the gate comes back misconfigured — acceptable,
     * since loading happens on every entry and a broken Analytics install does not.
     */
    readonly $isLoading = computed<boolean>(
        () => this.store.healthStatus() === null || this.store.status() === ComponentStatus.LOADING
    );

    /** Only a failed load reaches this; a failed CRUD action returns the store to `LOADED`. */
    readonly $hasError = computed<boolean>(() => this.store.status() === ComponentStatus.ERROR);

    /**
     * Copy shown instead of the list when Analytics is not usable. Mirrors the legacy
     * misconfiguration screen: only `NOT_CONFIGURED` means "never set up", every other
     * non-OK status is a broken configuration. Genuinely reactive — it reads `healthStatus`.
     */
    readonly $misconfiguredConfiguration = computed<PrincipalConfiguration>(() => {
        const isNotConfigured = this.store.healthStatus() === HealthStatusTypes.NOT_CONFIGURED;

        return {
            title: this.#dotMessageService.get(
                isNotConfigured
                    ? 'experiments.analytics-app-no-configured.title'
                    : 'experiments.analytics-app-misconfiguration.title'
            ),
            subtitle: this.#dotMessageService.get(
                isNotConfigured
                    ? 'experiments.analytics-app-no-configured.subtitle'
                    : 'experiments.analytics-app-misconfiguration.subtitle'
            ),
            // Material symbols, matching the empty state above — the legacy screen used a
            // PrimeIcon, which would be the only one on this page.
            icon: 'analytics',
            iconStyle: 'material-symbols-rounded'
        };
    });

    /** Actions of the row whose kebab menu is open; rebuilt on every toggle. */
    readonly $rowMenuItems = signal<MenuItem[]>([]);

    /** Identifier of the experiment being added to a bundle, or `null` when the dialog is closed. */
    readonly $addToBundleAssetId = signal<string | null>(null);

    // The page dispatches only page events; `…Succeeded` / `…Failed` are the API's to raise, and
    // are listened to (never dispatched) here — see `#listenForActionSuccess`.
    readonly #dispatch = injectDispatch(dotExperimentsListPageEvents);
    readonly #events = inject(Events);
    readonly #router = inject(Router);
    readonly #confirmationService = inject(ConfirmationService);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #dotMessageDisplayService = inject(DotMessageDisplayService);
    readonly #pushPublishDialogService = inject(DotPushPublishDialogService);
    readonly #destroyRef = inject(DestroyRef);

    /**
     * Raw search-box text. A `linkedSignal` rather than a plain one so it re-seeds from the store
     * whenever the filter changes underneath it — URL hydration on entry, and back/forward — while
     * staying writable by the input.
     */
    readonly $searchTerm = linkedSignal<string>(() => this.store.filter());

    /**
     * Debounced view of the search box, using Angular's `debounced` rather than an rxjs
     * `Subject` + `debounceTime`. Experimental in 22.0, so it may change shape.
     */
    readonly #debouncedSearch = debounced(() => this.$searchTerm(), SEARCH_DEBOUNCE_MS);

    /**
     * Translated fallbacks handed to `formatSchedule`, which stays free of user-facing English.
     *
     * Resolved once, not `computed`: `DotMessageService.get` is a plain lookup with no signal
     * behind it, so a computed would memoise on first read and never recompute — reactivity it
     * does not have. Messages are loaded before the portlet renders and only change on reload.
     */
    readonly #scheduleLabels: ExperimentScheduleLabels = {
        open: this.#dotMessageService.get('experiments.list.schedule.open'),
        none: this.#dotMessageService.get('experiments.list.schedule.none')
    };

    /**
     * Empty-state copy. Resolved once for the same reason as `#scheduleLabels`, and declared
     * after the injections because field initialisers run in declaration order.
     */
    /** Any narrowing the user applied, as opposed to a site that simply has no experiments. */
    readonly $hasActiveFilters = computed<boolean>(
        () =>
            this.store.filter().length > 0 ||
            this.store.selectedStatuses().length > 0 ||
            this.store.selectedGoals().length > 0
    );

    /** The table is replaced by an empty state once a settled load has nothing to show. */
    readonly $isEmpty = computed<boolean>(
        () => !this.$isLoading() && !this.$hasError() && this.$rows().length === 0
    );

    /**
     * "Nothing here" and "nothing matched" are different situations and get different copy: the
     * first is a site with no experiments, the second is the user's own filters hiding them, and
     * only the second is worth offering a way out of.
     */
    readonly $emptyConfiguration = computed<PrincipalConfiguration>(() =>
        this.$hasActiveFilters()
            ? {
                  title: this.#dotMessageService.get('experiments.list.no-results.title'),
                  subtitle: this.#dotMessageService.get('experiments.list.no-results.description'),
                  icon: 'filter_alt_off',
                  iconStyle: 'material-symbols-rounded'
              }
            : {
                  title: this.#dotMessageService.get('experiments.list.empty.title'),
                  subtitle: this.#dotMessageService.get('experiments.list.empty.description'),
                  icon: 'science',
                  iconStyle: 'material-symbols-rounded'
              }
    );

    /**
     * Shown when the load fails. The error itself is already surfaced by
     * `DotHttpErrorManagerService`; this is the screen's own state, so a failed load reads as a
     * failure with a way out rather than as an empty list.
     */
    readonly errorConfiguration: PrincipalConfiguration = {
        title: this.#dotMessageService.get('experiments.list.error.title'),
        subtitle: this.#dotMessageService.get('experiments.error.fetching.data'),
        icon: 'error',
        iconStyle: 'material-symbols-rounded'
    };

    /**
     * Pushes the settled search term into the store.
     *
     * Guarded against the store's own value: on entry the debounced term and the store agree
     * (both seeded from the URL), so nothing is dispatched and a hydrated `?filter=` survives.
     * It also stands in for `distinctUntilChanged` — re-typing the same text dispatches nothing.
     */
    protected readonly dispatchSearchEffect = effect(() => {
        const term = this.#debouncedSearch.value();

        untracked(() => {
            if (term !== this.store.filter()) {
                this.#dispatch.filterChanged(term);
            }
        });
    });

    constructor() {
        this.#listenForActionSuccess();
    }

    /**
     * Options for the two chip filters. Both are built here rather than inside the filter so it
     * stays domain-agnostic: it receives translated labels and counts and knows nothing about
     * statuses or goals.
     */
    readonly $statusFilterOptions = computed<ExperimentFilterOption[]>(() => {
        const counts = this.store.statusCounts();

        return ExperimentsStatusList.map(({ label, value }) => ({
            value,
            label: this.#dotMessageService.get(label),
            count: String(counts[value as DotExperimentStatus] ?? 0),
            testId: `experiment-status-filter-option-${value.toLowerCase()}`
        }));
    });

    readonly $goalFilterOptions = computed<ExperimentFilterOption[]>(() => {
        const counts = this.store.goalCounts();

        return [...GOAL_LABEL_KEYS].map(([goal, labelKey]) => ({
            value: goal,
            label: this.#dotMessageService.get(labelKey),
            count: String(counts[goal] ?? 0),
            testId: `experiment-goal-filter-option-${goal.toLowerCase()}`
        }));
    });

    /**
     * Clears the search box. Writing the signal is enough: the debounced dispatch is driven from
     * it, so the store follows on the next tick like any other edit — no separate dispatch here,
     * which would race the debounce and apply the empty term twice.
     */
    onClearSearch(): void {
        this.$searchTerm.set('');
    }

    /** Clears every narrowing at once, from the no-results state. */
    onClearFilters(): void {
        this.$searchTerm.set('');
        this.#dispatch.statusesChanged([]);
        this.#dispatch.goalsChanged([]);
    }

    onStatusesChange(statuses: string[]): void {
        this.#dispatch.statusesChanged(statuses as DotExperimentStatus[]);
    }

    onGoalsChange(goals: string[]): void {
        this.#dispatch.goalsChanged(goals as GOAL_TYPES[]);
    }

    /**
     * Re-runs the whole flow from the health gate, not just the list.
     *
     * A failed health check leaves `healthStatus` null, so retrying the list alone would fetch
     * experiments the gate never cleared — and `$isLoading` keys off that null, which would pin
     * the table to skeletons even after the list came back. Re-checking sets it either way.
     */
    onRetry(): void {
        this.#dispatch.checkHealth();
    }

    onLazyLoad(event: TableLazyLoadEvent): void {
        const rows = (event.rows as number) ?? this.store.perPage();
        const first = (event.first as number) ?? 0;
        const page = Math.floor(first / rows) + 1;

        this.#dispatch.pageChanged({ page, perPage: rows });

        if (event.sortField) {
            const field = Array.isArray(event.sortField) ? event.sortField[0] : event.sortField;
            const direction: DotExperimentsListSortDirection =
                event.sortOrder === -1 ? 'DESC' : 'ASC';

            // Only when the sort actually moved. PrimeNG's `createLazyLoadMetadata()` puts the
            // current sortField and sortOrder on *every* lazy-load event — the initial render and
            // each pagination included — and `sortChanged` resets the page. Dispatching it
            // unconditionally meant paging to 2 immediately reset to 1, and a `?page=N` deep link
            // was undone by the table's own first event.
            if (field !== this.store.orderBy() || direction !== this.store.direction()) {
                this.#dispatch.sortChanged({ orderBy: field, direction });
            }
        }
    }

    /** Rebuilds the kebab menu for the given row before the popup opens. */
    onRowMenuToggle(experiment: DotExperiment): void {
        this.$rowMenuItems.set(this.#buildRowMenuItems(experiment));
    }

    /** Opens the Configure screen with nothing created yet: the draft is POSTed from there. */
    onNewExperiment(): void {
        this.#router.navigate(NEW_EXPERIMENT_COMMANDS);
    }

    /** Opens the Configure screen of an existing experiment. */
    onConfigure(experiment: DotExperiment): void {
        this.#router.navigate(configureCommandsOf(experiment.id));
    }

    confirmArchive(experiment: DotExperiment): void {
        this.#confirm({
            headerKey: 'experiments.action.archive',
            messageKey: 'experiments.action.archive.confirm-question',
            acceptLabelKey: 'experiments.action.archive',
            accept: () => this.#dispatch.archiveExperiment(experiment)
        });
    }

    #buildRowMenuItems(experiment: DotExperiment): MenuItem[] {
        const { status } = experiment;

        return [
            {
                // The primary action of the row: it leads the menu, and is the only entry every
                // status allows.
                id: 'experiments-configure',
                label: this.#dotMessageService.get('experiments.list.action.configure'),
                visible: isAllowed('configuration', status),
                command: () => this.onConfigure(experiment)
            },
            {
                id: 'experiments-archive',
                label: this.#dotMessageService.get('experiments.action.archive'),
                visible: isAllowed('archive', status),
                command: () => this.confirmArchive(experiment)
            },
            {
                id: 'experiments-restore',
                label: this.#dotMessageService.get('experiments.action.restore'),
                visible: status === DotExperimentStatus.ARCHIVED,
                // No restore transition exists yet — it lands with #36988.
                disabled: true
            },
            {
                id: 'experiments-cancel-schedule',
                label: this.#dotMessageService.get('experiments.configure.scheduling.cancel'),
                visible: isAllowed('cancelSchedule', status),
                command: () =>
                    this.#confirm({
                        headerKey: 'experiments.configure.scheduling.cancel',
                        messageKey: 'experiments.action.cancel.schedule-confirm',
                        acceptLabelKey: 'dot.common.dialog.accept',
                        accept: () => this.#dispatch.cancelScheduleExperiment(experiment)
                    })
            },
            {
                id: 'experiments-end',
                label: this.#dotMessageService.get('experiments.action.end-experiment'),
                visible: isAllowed('end', status),
                command: () =>
                    this.#confirm({
                        headerKey: 'experiments.action.end-experiment',
                        messageKey: 'experiments.action.stop.delete-confirm',
                        acceptLabelKey: 'experiments.action.end',
                        accept: () => this.#dispatch.endExperiment(experiment)
                    })
            },
            {
                id: 'experiments-abort',
                label: this.#dotMessageService.get('experiments.action.abort.experiment'),
                visible: isAllowed('abort', status),
                command: () =>
                    this.#confirm({
                        headerKey: 'experiments.action.abort.experiment',
                        messageKey: 'experiments.action.abort.confirm.message',
                        acceptLabelKey: 'experiments.action.abort.experiment',
                        accept: () => this.#dispatch.abortExperiment(experiment)
                    })
            },
            {
                id: 'experiments-delete',
                label: this.#dotMessageService.get('experiments.action.delete'),
                visible: isAllowed('delete', status),
                command: () =>
                    this.#confirm({
                        headerKey: 'experiments.action.delete',
                        messageKey: 'experiments.action.delete.confirm-question',
                        messageArg: experiment.name,
                        acceptLabelKey: 'experiments.action.delete',
                        accept: () => this.#dispatch.deleteExperiment(experiment)
                    })
            },
            {
                id: 'experiments-push-publish',
                label: this.#dotMessageService.get('contenttypes.content.push_publish'),
                visible: isAllowed('pushPublish', status),
                command: () =>
                    this.#pushPublishDialogService.open({
                        assetIdentifier: experiment.id,
                        title: this.#dotMessageService.get('contenttypes.content.push_publish')
                    })
            },
            {
                id: 'experiments-add-to-bundle',
                label: this.#dotMessageService.get('contenttypes.content.add_to_bundle'),
                visible: isAllowed('addToBundle', status),
                command: () => this.$addToBundleAssetId.set(experiment.id)
            }
        ];
    }

    #confirm({
        headerKey,
        messageKey,
        messageArg,
        acceptLabelKey,
        accept
    }: {
        headerKey: string;
        messageKey: string;
        messageArg?: string;
        acceptLabelKey: string;
        accept: () => void;
    }): void {
        this.#confirmationService.confirm({
            key: CONFIGURATION_CONFIRM_DIALOG_KEY,
            header: this.#dotMessageService.get(headerKey),
            message: this.#dotMessageService.get(messageKey, messageArg ?? ''),
            acceptLabel: this.#dotMessageService.get(acceptLabelKey),
            rejectLabel: this.#dotMessageService.get('dot.common.dialog.reject'),
            rejectButtonStyleClass: 'p-button-secondary',
            defaultFocus: 'reject',
            closable: true,
            closeOnEscape: true,
            accept
        });
    }

    /**
     * The store reloads the list on its own after an action succeeds; the toast is a UI concern
     * and therefore lives here.
     */
    #listenForActionSuccess(): void {
        const successMessages: ReadonlyArray<[EventCreator<string, DotExperiment>, string]> = [
            [
                dotExperimentsApiEvents.archiveSucceeded,
                'experiments.action.archive.confirm-message'
            ],
            [dotExperimentsApiEvents.deleteSucceeded, 'experiments.action.delete.confirm-message'],
            [dotExperimentsApiEvents.endSucceeded, 'experiments.action.stop.confirm-message'],
            [dotExperimentsApiEvents.abortSucceeded, 'experiments.notification.abort'],
            [
                dotExperimentsApiEvents.cancelScheduleSucceeded,
                'experiments.notification.cancel.schedule'
            ]
        ];

        successMessages.forEach(([event, messageKey]) => {
            this.#events
                .on(event)
                .pipe(takeUntilDestroyed(this.#destroyRef))
                .subscribe(({ payload }) =>
                    this.#dotMessageDisplayService.push({
                        life: SUCCESS_MESSAGE_LIFE,
                        severity: DotMessageSeverity.SUCCESS,
                        message: this.#dotMessageService.get(messageKey, payload.name),
                        type: DotMessageType.SIMPLE_MESSAGE
                    })
                );
        });
    }
}
