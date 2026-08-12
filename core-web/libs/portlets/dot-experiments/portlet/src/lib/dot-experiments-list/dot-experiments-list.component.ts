import { EventCreator, Events, injectDispatch } from '@ngrx/signals/events';
import { Subject } from 'rxjs';

import { DatePipe, Location } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    computed,
    DestroyRef,
    effect,
    inject,
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
import { TooltipModule } from 'primeng/tooltip';

import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import {
    DotExperimentsService,
    DotMessageDisplayService,
    DotMessageService
} from '@dotcms/data-access';
import { DotPushPublishDialogService } from '@dotcms/dotcms-js';
import {
    AllowedActionsByExperimentStatus,
    CONFIGURATION_CONFIRM_DIALOG_KEY,
    DotExperiment,
    DotExperimentStatus,
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

import { DotExperimentStatusFilterComponent } from '../components/dot-experiment-status-filter/dot-experiment-status-filter.component';
import {
    NO_GOAL_PLACEHOLDER,
    ROWS_PER_PAGE_OPTIONS,
    SKELETON_COLUMNS,
    SKELETON_ROW_COUNT,
    STATUS_LABEL_KEYS,
    STATUS_SEVERITIES,
    SUCCESS_MESSAGE_LIFE
} from '../shared/constants';
import { ExperimentListAction, ExperimentRow } from '../shared/models';
import { dotExperimentsListEvents } from '../store/dot-experiments-list.events';
import {
    DEFAULT_EXPERIMENTS_LIST_DIRECTION,
    DEFAULT_EXPERIMENTS_LIST_ORDER_BY,
    DEFAULT_EXPERIMENTS_LIST_PAGE,
    DEFAULT_EXPERIMENTS_LIST_PER_PAGE,
    DEFAULT_EXPERIMENTS_LIST_STATUSES,
    DotExperimentsListStore
} from '../store/dot-experiments-list.store';
import {
    ExperimentScheduleLabels,
    formatSchedule,
    goalTypeOf,
    resolvePagePath,
    variantsCount
} from '../util/dot-experiments-list.util';

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
        TooltipModule,
        DotAddToBundleComponent,
        DotEmptyContainerComponent,
        DotExperimentStatusFilterComponent,
        DotMessagePipe
    ],
    templateUrl: './dot-experiments-list.component.html',
    // `DotExperimentsService` is `@Injectable()` with no `providedIn` and is not in the app-wide
    // `providers.ts`, so the store cannot inject it unless this component provides it. The legacy
    // screens do the same in `old/dot-experiments-shell`.
    providers: [DotExperimentsListStore, ConfirmationService, DotExperimentsService],
    changeDetection: ChangeDetectionStrategy.OnPush,
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
                canArchive: isAllowed('archive', experiment.status),
                isArchived: experiment.status === DotExperimentStatus.ARCHIVED,
                statusSeverity: STATUS_SEVERITIES[experiment.status] ?? 'secondary',
                statusLabelKey: STATUS_LABEL_KEYS.get(experiment.status) ?? ''
            };
        });
    });

    /**
     * What the table renders. During the very first load there is nothing to show yet, so the
     * value is padded with placeholders and the body template swaps every cell for a skeleton.
     */
    readonly $tableValue = computed<(ExperimentRow | null)[]>(() => {
        const rows = this.$rows();

        return this.store.status() === 'loading' && rows.length === 0
            ? new Array<null>(SKELETON_ROW_COUNT).fill(null)
            : rows;
    });

    /**
     * True until the Analytics health check answers. Neither branch of the gate can be trusted
     * yet, so the template renders an empty shell rather than flashing the list and then
     * replacing it with the misconfiguration notice.
     */
    readonly $isGatePending = computed<boolean>(() => this.store.healthStatus() === null);

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

    readonly #dispatch = injectDispatch(dotExperimentsListEvents);
    readonly #events = inject(Events);
    readonly #router = inject(Router);
    readonly #location = inject(Location);
    readonly #confirmationService = inject(ConfirmationService);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #dotMessageDisplayService = inject(DotMessageDisplayService);
    readonly #pushPublishDialogService = inject(DotPushPublishDialogService);
    readonly #destroyRef = inject(DestroyRef);

    readonly #searchSubject = new Subject<string>();

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
    readonly emptyConfiguration: PrincipalConfiguration = {
        title: this.#dotMessageService.get('experiments.list.empty.title'),
        subtitle: this.#dotMessageService.get('experiments.list.empty.description'),
        icon: 'science',
        iconStyle: 'material-symbols-rounded'
    };

    /**
     * Mirrors the view state into the URL so the list is shareable and survives a reload.
     * Values equal to their default are written as `null`, which removes the param — a plain
     * `/experiments` URL therefore stays free of query params. `Location.go` is used instead of
     * `Router.navigate` so a filter change never reloads the route.
     */
    protected readonly syncUrlEffect = effect(() => {
        const queryParams: Record<string, string | string[] | null> = {
            page: nullWhenDefault(this.store.page(), DEFAULT_EXPERIMENTS_LIST_PAGE),
            per_page: nullWhenDefault(this.store.perPage(), DEFAULT_EXPERIMENTS_LIST_PER_PAGE),
            orderby: nullWhenDefault(this.store.orderBy(), DEFAULT_EXPERIMENTS_LIST_ORDER_BY),
            direction: nullWhenDefault(this.store.direction(), DEFAULT_EXPERIMENTS_LIST_DIRECTION),
            filter: this.store.filter() || null,
            status: isDefaultStatusSelection(this.store.selectedStatuses())
                ? null
                : this.store.selectedStatuses()
        };

        untracked(() => this.#writeUrl(queryParams));
    });

    constructor() {
        this.#searchSubject
            .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed(this.#destroyRef))
            .subscribe((value) => this.#dispatch.filterChanged(value));

        this.#listenForActionSuccess();
    }

    onSearch(value: string): void {
        this.#searchSubject.next(value);
    }

    onStatusesChange(statuses: DotExperimentStatus[]): void {
        this.#dispatch.statusesChanged(statuses);
    }

    onLazyLoad(event: TableLazyLoadEvent): void {
        const rows = (event.rows as number) ?? this.store.perPage();
        const first = (event.first as number) ?? 0;
        const page = Math.floor(first / rows) + 1;

        this.#dispatch.pageChanged({ page, perPage: rows });

        if (event.sortField) {
            const field = Array.isArray(event.sortField) ? event.sortField[0] : event.sortField;

            this.#dispatch.sortChanged({
                orderBy: field,
                direction: event.sortOrder === -1 ? 'DESC' : 'ASC'
            });
        }
    }

    /** Rebuilds the kebab menu for the given row before the popup opens. */
    onRowMenuToggle(experiment: DotExperiment): void {
        this.$rowMenuItems.set(this.#buildRowMenuItems(experiment));
    }

    confirmArchive(experiment: DotExperiment): void {
        this.#confirm({
            headerKey: 'experiments.action.archive',
            messageKey: 'experiments.action.archive.confirm-question',
            acceptLabelKey: 'experiments.action.archive',
            accept: () => this.#dispatch.archiveRequested(experiment)
        });
    }

    #buildRowMenuItems(experiment: DotExperiment): MenuItem[] {
        const { status } = experiment;

        return [
            {
                id: 'experiments-cancel-schedule',
                label: this.#dotMessageService.get('experiments.configure.scheduling.cancel'),
                visible: isAllowed('cancelSchedule', status),
                command: () =>
                    this.#confirm({
                        headerKey: 'experiments.configure.scheduling.cancel',
                        messageKey: 'experiments.action.cancel.schedule-confirm',
                        acceptLabelKey: 'dot.common.dialog.accept',
                        accept: () => this.#dispatch.cancelScheduleRequested(experiment)
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
                        accept: () => this.#dispatch.endRequested(experiment)
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
                        accept: () => this.#dispatch.abortRequested(experiment)
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
                        accept: () => this.#dispatch.deleteRequested(experiment)
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
                dotExperimentsListEvents.archiveSucceeded,
                'experiments.action.archive.confirm-message'
            ],
            [dotExperimentsListEvents.deleteSucceeded, 'experiments.action.delete.confirm-message'],
            [dotExperimentsListEvents.endSucceeded, 'experiments.action.stop.confirm-message'],
            [dotExperimentsListEvents.abortSucceeded, 'experiments.notification.abort'],
            [
                dotExperimentsListEvents.cancelScheduleSucceeded,
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

    #writeUrl(queryParams: Record<string, string | string[] | null>): void {
        const newUrl = this.#router
            .createUrlTree([], { queryParams, queryParamsHandling: 'merge' })
            .toString();

        if (newUrl !== this.#location.path(true)) {
            this.#location.go(newUrl);
        }
    }
}

function isAllowed(action: ExperimentListAction, status: DotExperimentStatus): boolean {
    return AllowedActionsByExperimentStatus[action].includes(status);
}

/** Defaults are never written to the URL, so a pristine list has no query params at all. */
function nullWhenDefault<T extends string | number>(value: T, defaultValue: T): string | null {
    return value === defaultValue ? null : String(value);
}

/** Order-insensitive set comparison: a reordered default selection is still the default. */
function isDefaultStatusSelection(statuses: DotExperimentStatus[]): boolean {
    if (statuses.length !== DEFAULT_EXPERIMENTS_LIST_STATUSES.length) {
        return false;
    }

    const selected = new Set(statuses);

    return DEFAULT_EXPERIMENTS_LIST_STATUSES.every((status) => selected.has(status));
}
