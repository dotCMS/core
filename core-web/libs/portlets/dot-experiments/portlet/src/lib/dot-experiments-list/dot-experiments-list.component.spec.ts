import { Dispatcher, EventCreator } from '@ngrx/signals/events';
import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';

import { provideLocationMocks } from '@angular/common/testing';
import { provideRouter, Router } from '@angular/router';

import { ConfirmationService, Confirmation, MenuItem } from 'primeng/api';

import { DotMessageDisplayService, DotMessageService } from '@dotcms/data-access';
import { DotPushPublishDialogService } from '@dotcms/dotcms-js';
import {
    AllowedActionsByExperimentStatus,
    ComponentStatus,
    DotExperiment,
    DotExperimentStatus,
    DotMessageSeverity,
    GOAL_TYPES,
    HealthStatusTypes
} from '@dotcms/dotcms-models';
import { getExperimentMock, MockDotMessageService } from '@dotcms/utils-testing';

import { DotExperimentsListComponent } from './dot-experiments-list.component';

import {
    DEFAULT_EXPERIMENTS_LIST_DIRECTION,
    DEFAULT_EXPERIMENTS_LIST_ORDER_BY,
    DEFAULT_EXPERIMENTS_LIST_GOALS,
    DEFAULT_EXPERIMENTS_LIST_PAGE,
    DEFAULT_EXPERIMENTS_LIST_PER_PAGE,
    DEFAULT_EXPERIMENTS_LIST_STATUSES,
    ROWS_PER_PAGE_OPTIONS,
    SEARCH_DEBOUNCE_MS
} from '../shared/constants';
import { dotExperimentsApiEvents } from '../store/dot-experiments-api.events';
import { dotExperimentsListPageEvents } from '../store/dot-experiments-list-page.events';
import { DotExperimentsListStore } from '../store/dot-experiments-list.store';

const PAGE_ID = 'page-1';

const PAGE_INFO = { [PAGE_ID]: { url: '/blog/index', host: 'host-1' } };

/** Shown on the chip when the filtered page can no longer be resolved. */
const PAGE_UNAVAILABLE_COPY = 'This page is no longer available';

const EMPTY_GOAL_COUNTS: Record<GOAL_TYPES, number> = {
    [GOAL_TYPES.REACH_PAGE]: 0,
    [GOAL_TYPES.BOUNCE_RATE]: 0,
    [GOAL_TYPES.CLICK_ON_ELEMENT]: 0,
    [GOAL_TYPES.URL_PARAMETER]: 0,
    [GOAL_TYPES.EXIT_RATE]: 0
};

const EMPTY_STATUS_COUNTS: Record<DotExperimentStatus, number> = {
    [DotExperimentStatus.DRAFT]: 0,
    [DotExperimentStatus.SCHEDULED]: 0,
    [DotExperimentStatus.RUNNING]: 0,
    [DotExperimentStatus.ENDED]: 0,
    [DotExperimentStatus.ARCHIVED]: 0
};

const experimentWith = (status: DotExperimentStatus): DotExperiment => ({
    ...getExperimentMock(0),
    id: `experiment-${status.toLowerCase()}`,
    name: `Experiment ${status}`,
    pageId: PAGE_ID,
    status
});

/** Kebab item ids, as declared by the component. */
const MENU_ITEM = {
    configure: 'experiments-configure',
    archive: 'experiments-archive',
    restore: 'experiments-restore',
    cancelSchedule: 'experiments-cancel-schedule',
    end: 'experiments-end',
    abort: 'experiments-abort',
    delete: 'experiments-delete',
    pushPublish: 'experiments-push-publish',
    addToBundle: 'experiments-add-to-bundle'
} as const;

const ARCHIVE_LABEL = 'Archive';
const RESTORE_LABEL = 'Restore';
const ACTIONS_MENU_LABEL = 'Actions';

const NOT_CONFIGURED_COPY = {
    title: 'Analytics not configured',
    subtitle: 'Configure the Analytics app to start experimenting'
};

const MISCONFIGURATION_COPY = {
    title: 'Analytics misconfigured',
    subtitle: 'Review the Analytics app configuration'
};

const ERROR_COPY = {
    title: 'Could not load experiments',
    subtitle: 'Failed to retrieve experiments data'
};

const messageServiceMock = new MockDotMessageService({
    'experiments.list.page-filter.unavailable': PAGE_UNAVAILABLE_COPY,
    'experiments.analytics-app-no-configured.title': NOT_CONFIGURED_COPY.title,
    'experiments.analytics-app-no-configured.subtitle': NOT_CONFIGURED_COPY.subtitle,
    'experiments.analytics-app-misconfiguration.title': MISCONFIGURATION_COPY.title,
    'experiments.analytics-app-misconfiguration.subtitle': MISCONFIGURATION_COPY.subtitle,
    'experiments.action.archive': ARCHIVE_LABEL,
    'experiments.action.restore': RESTORE_LABEL,
    'experiments.list.actions.menu': ACTIONS_MENU_LABEL,
    'experiments.action.archive.confirm-message': 'Experiment {0} archived',
    'experiments.action.delete.confirm-message': 'Experiment {0} deleted',
    'experiments.action.stop.confirm-message': 'Experiment {0} ended',
    'experiments.notification.abort': 'Experiment {0} aborted',
    'experiments.notification.cancel.schedule': 'Experiment {0} unscheduled',
    'experiments.list.error.title': ERROR_COPY.title,
    'experiments.error.fetching.data': ERROR_COPY.subtitle,
    'experiments.list.error.retry': 'Retry'
});

/**
 * The store is provided by the component itself, so it is replaced through
 * `componentProviders`. Signals are plain `jest.fn()` return values: the component only
 * reads them, and every test decides the values before the component is created.
 */
const createStoreMock = () => ({
    healthStatus: jest.fn().mockReturnValue(HealthStatusTypes.OK),
    isMisconfigured: jest.fn().mockReturnValue(false),
    pagedExperiments: jest.fn().mockReturnValue([] as DotExperiment[]),
    pageInfoByPageId: jest.fn().mockReturnValue(PAGE_INFO),
    statusCounts: jest.fn().mockReturnValue(EMPTY_STATUS_COUNTS),
    selectedStatuses: jest.fn().mockReturnValue(DEFAULT_EXPERIMENTS_LIST_STATUSES),
    goalCounts: jest.fn().mockReturnValue(EMPTY_GOAL_COUNTS),
    selectedGoals: jest.fn().mockReturnValue(DEFAULT_EXPERIMENTS_LIST_GOALS),
    filter: jest.fn().mockReturnValue(''),
    selectedPageId: jest.fn().mockReturnValue(null),
    status: jest.fn().mockReturnValue(ComponentStatus.LOADED),
    page: jest.fn().mockReturnValue(DEFAULT_EXPERIMENTS_LIST_PAGE),
    perPage: jest.fn().mockReturnValue(DEFAULT_EXPERIMENTS_LIST_PER_PAGE),
    orderBy: jest.fn().mockReturnValue(DEFAULT_EXPERIMENTS_LIST_ORDER_BY),
    direction: jest.fn().mockReturnValue(DEFAULT_EXPERIMENTS_LIST_DIRECTION),
    totalRecords: jest.fn().mockReturnValue(0)
});

describe('DotExperimentsListComponent', () => {
    let spectator: Spectator<DotExperimentsListComponent>;
    let storeMock: ReturnType<typeof createStoreMock>;
    let dispatch: jest.SpyInstance;
    let confirm: jest.SpyInstance;
    let navigate: jest.SpyInstance;

    const createComponent = createComponentFactory({
        component: DotExperimentsListComponent,
        // `componentProviders` replaces the component's own `providers`, so the real
        // `ConfirmationService` has to be re-declared here (`p-confirmDialog` needs it).
        componentProviders: [
            { provide: DotExperimentsListStore, useFactory: () => storeMock },
            ConfirmationService
        ],
        providers: [
            provideRouter([{ path: 'experiments', children: [] }]),
            provideLocationMocks(),
            { provide: DotMessageService, useValue: messageServiceMock },
            mockProvider(DotMessageDisplayService),
            mockProvider(DotPushPublishDialogService)
        ],
        detectChanges: false
    });

    /** Renders a single row of the given status and returns that experiment. */
    const renderRowWith = (status: DotExperimentStatus): DotExperiment => {
        const experiment = experimentWith(status);
        storeMock.pagedExperiments.mockReturnValue([experiment]);
        storeMock.totalRecords.mockReturnValue(1);
        spectator.detectChanges();

        return experiment;
    };

    const clickButton = (testId: string) => {
        const button = spectator.query(byTestId(testId))?.querySelector('button');
        spectator.click(button as HTMLElement);
        spectator.detectChanges();
    };

    /** Ids of the kebab entries the user can actually see for the currently rendered row. */
    const visibleMenuItemIds = (): string[] => {
        clickButton('experiment-actions-btn');

        return spectator.component
            .$rowMenuItems()
            .filter(({ visible }) => visible)
            .map(({ id }) => id as string);
    };

    const runMenuItem = (itemId: string) => {
        clickButton('experiment-actions-btn');
        const item = spectator.component
            .$rowMenuItems()
            .find(({ id }) => id === itemId) as MenuItem;
        item.command?.({ originalEvent: new MouseEvent('click'), item });
    };

    /** Accepts the confirmation opened by the last action and returns it. */
    const acceptConfirmation = (): Confirmation => {
        const confirmation = confirm.mock.calls[0][0] as Confirmation;
        confirmation.accept?.();

        return confirmation;
    };

    /** `injectDispatch` appends a scope argument, so only the event itself is compared. */
    const dispatchedEvents = () => dispatch.mock.calls.map(([event]) => event);

    const emitSucceeded = (
        event: EventCreator<string, DotExperiment>,
        experiment: DotExperiment
    ) => {
        spectator.inject(Dispatcher).dispatch(event(experiment));
        spectator.detectChanges();
    };

    beforeEach(() => {
        storeMock = createStoreMock();
        spectator = createComponent();
        dispatch = jest.spyOn(spectator.inject(Dispatcher), 'dispatch');
        // `p-confirmDialog` needs the real service (it subscribes to its streams), so the
        // confirmation is intercepted instead of mocked away.
        const confirmationService = spectator.inject(ConfirmationService, true);
        confirm = jest
            .spyOn(confirmationService, 'confirm')
            .mockReturnValue(confirmationService) as jest.SpyInstance;
        // The Configure screen is a real route of the portlet, so navigation is intercepted
        // rather than let through to a component this spec does not render.
        navigate = jest.spyOn(spectator.inject(Router), 'navigate').mockResolvedValue(true);
    });

    afterEach(() => {
        jest.restoreAllMocks();
    });

    describe('search', () => {
        beforeEach(() => jest.useFakeTimers());
        afterEach(() => jest.useRealTimers());

        const type = (text: string) =>
            spectator.typeInElement(
                text,
                spectator.query(byTestId('experiments-search-input')) as HTMLInputElement
            );

        it('should dispatch filterChanged only after the debounce window', async () => {
            spectator.detectChanges();

            type('summer');
            // `debounced` arms itself inside an internal effect that reads the source, so the
            // timer is not even scheduled until a change-detection pass runs.
            spectator.detectChanges();

            expect(dispatchedEvents()).not.toContainEqual(
                dotExperimentsListPageEvents.filterChanged('summer')
            );

            // It settles a Resource, so the timer alone is not enough — microtasks have to
            // drain too, hence the async variant. The dispatch then lands in an effect on the
            // next pass, rather than inside the timer callback as the rxjs version did.
            await jest.advanceTimersByTimeAsync(SEARCH_DEBOUNCE_MS);
            spectator.detectChanges();

            expect(dispatchedEvents()).toContainEqual(
                dotExperimentsListPageEvents.filterChanged('summer')
            );
        });

        it('should not re-dispatch when the settled term already matches the store', async () => {
            // Stands in for `distinctUntilChanged`: re-typing the same text, or arriving with a
            // hydrated `?filter=`, must not push a redundant filterChanged.
            storeMock.filter.mockReturnValue('summer');
            spectator.detectChanges();

            type('summer');
            spectator.detectChanges();
            await jest.advanceTimersByTimeAsync(SEARCH_DEBOUNCE_MS);
            spectator.detectChanges();

            expect(dispatchedEvents()).not.toContainEqual(
                dotExperimentsListPageEvents.filterChanged('summer')
            );
        });
    });

    describe('status tag', () => {
        // Severities mirror `DotExperimentsUiHeaderComponent` so a status looks the same here
        // and in the UVE header. `warn` is PrimeNG's spelling; anything else renders unstyled.
        it.each([
            [DotExperimentStatus.RUNNING, 'success', 'running'],
            [DotExperimentStatus.SCHEDULED, 'info', 'scheduled'],
            [DotExperimentStatus.DRAFT, 'warn', 'draft'],
            [DotExperimentStatus.ENDED, 'info', 'ended'],
            [DotExperimentStatus.ARCHIVED, 'secondary', 'archived']
        ])('should render %s as a %s tag', (status, severity, labelKey) => {
            renderRowWith(status);

            const tag = spectator.query(byTestId('experiment-status-tag'));

            expect(tag?.className).toContain(`p-tag-${severity}`);
            expect(tag?.textContent).toContain(messageServiceMock.get(labelKey));
        });
    });

    describe('row actions', () => {
        it.each([
            [
                DotExperimentStatus.DRAFT,
                [
                    MENU_ITEM.configure,
                    MENU_ITEM.delete,
                    MENU_ITEM.pushPublish,
                    MENU_ITEM.addToBundle
                ]
            ],
            [
                DotExperimentStatus.RUNNING,
                [
                    MENU_ITEM.configure,
                    MENU_ITEM.end,
                    MENU_ITEM.abort,
                    MENU_ITEM.pushPublish,
                    MENU_ITEM.addToBundle
                ]
            ],
            [
                DotExperimentStatus.SCHEDULED,
                [
                    MENU_ITEM.configure,
                    MENU_ITEM.cancelSchedule,
                    MENU_ITEM.delete,
                    MENU_ITEM.pushPublish,
                    MENU_ITEM.addToBundle
                ]
            ],
            [
                DotExperimentStatus.ENDED,
                [
                    MENU_ITEM.configure,
                    MENU_ITEM.archive,
                    MENU_ITEM.pushPublish,
                    MENU_ITEM.addToBundle
                ]
            ],
            [
                DotExperimentStatus.ARCHIVED,
                [
                    MENU_ITEM.configure,
                    MENU_ITEM.restore,
                    MENU_ITEM.pushPublish,
                    MENU_ITEM.addToBundle
                ]
            ]
        ])('should only offer the actions allowed for %s', (status, expectedItemIds) => {
            renderRowWith(status);

            expect(visibleMenuItemIds().sort()).toEqual([...expectedItemIds].sort());
        });

        it.each(Object.values(DotExperimentStatus))(
            'should expose the kebab as the only control for %s',
            (status) => {
                // Every action lives in the menu, archive and restore included, so the cell
                // holds exactly one button whatever the row's status.
                renderRowWith(status);

                const labels = Array.from(
                    spectator
                        .query(byTestId('experiment-row'))
                        ?.querySelectorAll('td:last-child p-button[aria-label]') ?? []
                ).map((button) => button.getAttribute('aria-label'));

                expect(labels).toEqual([ACTIONS_MENU_LABEL]);
            }
        );

        // One status per test: the store mock's signals are plain `jest.fn()`s, so a second
        // `renderRowWith` in the same test would not recompute the row.
        it('should offer archive in the kebab once the experiment has ended', () => {
            renderRowWith(DotExperimentStatus.ENDED);

            expect(visibleMenuItemIds()).toContain(MENU_ITEM.archive);
        });

        it('should not offer archive while the experiment is still running', () => {
            renderRowWith(DotExperimentStatus.RUNNING);

            expect(visibleMenuItemIds()).not.toContain(MENU_ITEM.archive);
        });

        it('should offer restore disabled on archived rows — no restore transition yet', () => {
            renderRowWith(DotExperimentStatus.ARCHIVED);
            clickButton('experiment-actions-btn');

            const restore = spectator.component
                .$rowMenuItems()
                .find(({ id }) => id === MENU_ITEM.restore) as MenuItem;

            expect(restore.visible).toBe(true);
            expect(restore.disabled).toBe(true);
            expect(restore.command).toBeUndefined();
        });

        it('should keep Configure in the kebab rather than as its own row control', () => {
            // The design leads the cell with "View Results" / "Configure", but View Results
            // lands with the reports screen — a lone button would read as the row's only
            // action, so Configure stays the first kebab entry until then.
            Object.values(DotExperimentStatus).forEach((status) => {
                renderRowWith(status);

                expect(spectator.query(byTestId('experiment-primary-action'))).toBeNull();
            });
        });

        it('should never route into the legacy configure or results screens', () => {
            Object.values(DotExperimentStatus).forEach((status) => {
                renderRowWith(status);

                expect(visibleMenuItemIds()).not.toContain('experiments-configuration');
                expect(visibleMenuItemIds()).not.toContain('experiments-results');
                expect(
                    spectator.query(byTestId('experiment-row'))?.querySelector('a[href]')
                ).toBeNull();
            });
        });
    });

    describe('configure', () => {
        // Sourced from the shared table so the row menu and the gate cannot drift apart.
        const CONFIGURABLE_STATUSES = AllowedActionsByExperimentStatus['configuration'];

        it.each(CONFIGURABLE_STATUSES)('should lead the kebab with Configure for %s', (status) => {
            renderRowWith(status);

            expect(visibleMenuItemIds()[0]).toBe(MENU_ITEM.configure);
        });

        it('should open the Configure screen of the row', () => {
            const experiment = renderRowWith(DotExperimentStatus.RUNNING);

            runMenuItem(MENU_ITEM.configure);

            expect(navigate).toHaveBeenCalledWith(['/experiments', experiment.id, 'configuration']);
        });
    });

    describe('new experiment', () => {
        it('should offer an enabled new-experiment button', () => {
            renderRowWith(DotExperimentStatus.DRAFT);

            const button = spectator.query(byTestId('experiments-new'))?.querySelector('button');

            expect(button).not.toBeNull();
            expect((button as HTMLButtonElement).disabled).toBe(false);
        });

        it('should open the Configure screen with nothing created yet', () => {
            // The draft is POSTed from the Configure screen, so the button carries no id.
            renderRowWith(DotExperimentStatus.DRAFT);

            clickButton('experiments-new');

            expect(navigate).toHaveBeenCalledWith(['/experiments', 'new']);
        });
    });

    describe('confirm then dispatch', () => {
        it('should dispatch archiveRequested once the archive confirmation is accepted', () => {
            const experiment = renderRowWith(DotExperimentStatus.ENDED);

            runMenuItem(MENU_ITEM.archive);

            expect(dispatchedEvents()).not.toContainEqual(
                dotExperimentsListPageEvents.archiveExperiment(experiment)
            );

            acceptConfirmation();

            expect(dispatchedEvents()).toContainEqual(
                dotExperimentsListPageEvents.archiveExperiment(experiment)
            );
        });

        it.each([
            [
                DotExperimentStatus.DRAFT,
                MENU_ITEM.delete,
                dotExperimentsListPageEvents.deleteExperiment
            ],
            [
                DotExperimentStatus.RUNNING,
                MENU_ITEM.end,
                dotExperimentsListPageEvents.endExperiment
            ],
            [
                DotExperimentStatus.RUNNING,
                MENU_ITEM.abort,
                dotExperimentsListPageEvents.abortExperiment
            ],
            [
                DotExperimentStatus.SCHEDULED,
                MENU_ITEM.cancelSchedule,
                dotExperimentsListPageEvents.cancelScheduleExperiment
            ]
        ])(
            'should confirm %s / %s before dispatching',
            (status, itemId, expectedEvent: EventCreator<string, DotExperiment>) => {
                const experiment = renderRowWith(status);

                runMenuItem(itemId);

                expect(confirm).toHaveBeenCalledTimes(1);
                expect(dispatchedEvents()).not.toContainEqual(expectedEvent(experiment));

                acceptConfirmation();

                expect(dispatchedEvents()).toContainEqual(expectedEvent(experiment));
            }
        );

        it('should open the push publish dialog without a confirmation', () => {
            const experiment = renderRowWith(DotExperimentStatus.DRAFT);
            const pushPublishDialogService = spectator.inject(DotPushPublishDialogService, true);

            runMenuItem(MENU_ITEM.pushPublish);

            expect(pushPublishDialogService.open).toHaveBeenCalledWith(
                expect.objectContaining({ assetIdentifier: experiment.id })
            );
        });

        it('should open the add to bundle dialog without a confirmation', () => {
            const experiment = renderRowWith(DotExperimentStatus.DRAFT);

            runMenuItem(MENU_ITEM.addToBundle);

            expect(spectator.component.$addToBundleAssetId()).toBe(experiment.id);
        });
    });

    describe('success toasts', () => {
        it.each([
            ['archived', dotExperimentsApiEvents.archiveSucceeded],
            ['deleted', dotExperimentsApiEvents.deleteSucceeded],
            ['ended', dotExperimentsApiEvents.endSucceeded],
            ['aborted', dotExperimentsApiEvents.abortSucceeded],
            ['unscheduled', dotExperimentsApiEvents.cancelScheduleSucceeded]
        ])('should push a success toast once the experiment is %s', (expectedVerb, event) => {
            const experiment = renderRowWith(DotExperimentStatus.DRAFT);
            const messageDisplayService = spectator.inject(DotMessageDisplayService, true);

            emitSucceeded(event as EventCreator<string, DotExperiment>, experiment);

            expect(messageDisplayService.push).toHaveBeenCalledWith(
                expect.objectContaining({
                    severity: DotMessageSeverity.SUCCESS,
                    message: `Experiment ${experiment.name} ${expectedVerb}`
                })
            );
        });
    });

    /**
     * The page filter's UI (#37005, US3, FR-021b, FR-021c, FR-024).
     *
     * With the entry-point switch on, an editor arrives here from a page and the list is already
     * narrowed to it. The narrowing has to be *visible* — an invisibly filtered list looks like a
     * site with almost no experiments — and clearable, and it has to offer a way back to the page
     * and a way to create an experiment for it when there are none.
     */
    describe('page filter', () => {
        const PAGE_ID = 'page-1';

        const withPageFilter = (pageId: string | null = PAGE_ID) => {
            storeMock.selectedPageId.mockReturnValue(pageId);
            spectator.detectChanges();
        };

        // FR-021c, first half.
        it('should render a chip naming the filtered page', () => {
            withPageFilter();

            const chip = spectator.query(byTestId('experiments-page-filter-chip'));

            expect(chip).not.toBeNull();
            // The path the Page column already resolves, not the raw id: the editor recognises
            // their page by its path, and `pageInfoByPageId` is already loaded for the column.
            expect(chip?.textContent).toContain(PAGE_INFO[PAGE_ID].url);
        });

        it('should render no chip when the list is not narrowed to a page', () => {
            withPageFilter(null);

            expect(spectator.query(byTestId('experiments-page-filter-chip'))).toBeNull();
        });

        // FR-021c, second half — "the filter is a starting point, not a cage".
        it('should clear the filter when the chip is dismissed', () => {
            withPageFilter();

            const clear = spectator
                .query(byTestId('experiments-page-filter-clear'))
                ?.querySelector('button') as HTMLElement;
            spectator.click(clear);

            expect(dispatchedEvents()).toContainEqual(
                dotExperimentsListPageEvents.pageAssetFilterChanged(null)
            );
        });

        // FR-024. Reuses the deep-link builder, so the address is the one the round-trip uses
        // minus the experiment context — not a second URL assembler.
        it('should offer a link back to the page in the editor', () => {
            withPageFilter();

            const back = spectator.query(byTestId('experiments-page-filter-back'));

            expect(back).not.toBeNull();
            expect(back?.getAttribute('href')).toContain('/edit-page/content');
            expect(back?.getAttribute('href')).toContain(`url=${PAGE_INFO[PAGE_ID].url}`);
        });

        // FR-021b, zero. The generic "your filters are hiding them" state is wrong here: the
        // editor did not apply this filter, they arrived with it, and the useful offer is to
        // create an experiment for the page rather than to clear a filter they did not set.
        describe('when the filtered page has no experiments', () => {
            beforeEach(() => {
                storeMock.pagedExperiments.mockReturnValue([]);
                storeMock.totalRecords.mockReturnValue(0);
                withPageFilter();
            });

            it('should scope the empty state to the page', () => {
                const empty = spectator.query(byTestId('experiments-empty-state'));

                expect(empty).not.toBeNull();
                expect(empty?.textContent).toContain(PAGE_INFO[PAGE_ID].url);
            });

            it('should offer to create an experiment for that page', () => {
                const create = spectator.query(byTestId('experiments-empty-create-for-page'));

                expect(create).not.toBeNull();
            });

            it('should carry the page into the creation screen so it arrives prefilled', () => {
                const create = spectator
                    .query(byTestId('experiments-empty-create-for-page'))
                    ?.querySelector('button') as HTMLElement;
                spectator.click(create);

                expect(navigate).toHaveBeenCalledWith(['/experiments', 'new'], {
                    queryParams: { pageId: PAGE_ID }
                });
            });
        });

        // The spec's last edge case: an editor arrives from a page that has since been deleted.
        // `pageInfoByPageId` cannot resolve it, so the chip has no path to show — it must say the
        // page is gone rather than render an unlabelled chip over a silently empty list.
        it('should report a filtered page that no longer resolves', () => {
            storeMock.selectedPageId.mockReturnValue('page-deleted');
            storeMock.pagedExperiments.mockReturnValue([]);
            spectator.detectChanges();

            const chip = spectator.query(byTestId('experiments-page-filter-chip'));

            expect(chip).not.toBeNull();
            expect(chip?.textContent).toContain(PAGE_UNAVAILABLE_COPY);
        });
    });

    describe('empty state', () => {
        it('should render the empty state when there are no rows', () => {
            spectator.detectChanges();

            expect(spectator.query(byTestId('experiments-empty-state'))).not.toBeNull();
            expect(spectator.query(byTestId('experiment-row'))).toBeNull();
        });

        it('should not render the empty state when there are rows', () => {
            renderRowWith(DotExperimentStatus.DRAFT);

            expect(spectator.query(byTestId('experiments-empty-state'))).toBeNull();
        });
    });

    describe('loading state', () => {
        it('should render skeleton rows while the list is loading', () => {
            storeMock.status.mockReturnValue(ComponentStatus.LOADING);
            spectator.detectChanges();

            expect(spectator.queryAll(byTestId('experiments-loading-row')).length).toBeGreaterThan(
                0
            );
            expect(spectator.query(byTestId('experiment-row'))).toBeNull();
        });

        it('should not show the empty state while loading', () => {
            // Otherwise a slow load momentarily claims there are no experiments.
            storeMock.status.mockReturnValue(ComponentStatus.LOADING);
            spectator.detectChanges();

            expect(spectator.query(byTestId('experiments-empty-state'))).toBeNull();
        });

        it('should keep showing rows while a reload is in flight', () => {
            // Paging and filtering re-enter 'loading' with rows already on screen; replacing
            // them with skeletons on every keystroke would make the table flicker.
            const experiment = renderRowWith(DotExperimentStatus.DRAFT);
            storeMock.status.mockReturnValue(ComponentStatus.LOADING);
            spectator.detectChanges();

            expect(spectator.query(byTestId('experiment-name'))?.textContent).toContain(
                experiment.name
            );
        });
    });

    describe('load error', () => {
        const renderError = () => {
            storeMock.status.mockReturnValue(ComponentStatus.ERROR);
            spectator.detectChanges();
        };

        it('should render the error state instead of the table', () => {
            // A failed load must not read as "no experiments" — the distinction matters, since
            // an empty table invites the user to create one that may already exist.
            renderError();

            const error = spectator.query(byTestId('experiments-error'));

            expect(error).not.toBeNull();
            expect(error?.textContent).toContain(ERROR_COPY.title);
            expect(error?.textContent).toContain(ERROR_COPY.subtitle);
            expect(spectator.query(byTestId('experiments-table-wrapper'))).toBeNull();
            expect(spectator.query(byTestId('experiments-empty-state'))).toBeNull();
        });

        it('should re-run the health gate when retry is pressed, not just the list', () => {
            renderError();

            clickButton('experiments-error');

            // Requesting the list alone would fetch experiments the gate never cleared, and
            // `$isLoading` keys off a null healthStatus — so the table would sit on skeletons
            // even after the list came back.
            expect(dispatchedEvents()).toContainEqual(dotExperimentsListPageEvents.checkHealth());
            expect(dispatchedEvents()).not.toContainEqual(
                dotExperimentsListPageEvents.loadExperiments()
            );
        });

        it('should not render the error state on a healthy load', () => {
            spectator.detectChanges();

            expect(spectator.query(byTestId('experiments-error'))).toBeNull();
        });
    });

    describe('analytics misconfiguration', () => {
        const renderMisconfigured = (healthStatus: HealthStatusTypes) => {
            storeMock.isMisconfigured.mockReturnValue(true);
            storeMock.healthStatus.mockReturnValue(healthStatus);
            spectator.detectChanges();
        };

        it.each([
            [HealthStatusTypes.NOT_CONFIGURED, NOT_CONFIGURED_COPY],
            [HealthStatusTypes.CONFIGURATION_ERROR, MISCONFIGURATION_COPY]
        ])('should render the %s message', (healthStatus, copy) => {
            renderMisconfigured(healthStatus);

            const container = spectator.query(byTestId('experiments-misconfiguration'));

            expect(container?.textContent).toContain(copy.title);
            expect(container?.textContent).toContain(copy.subtitle);
        });

        it('should show the loading skeleton while the health check is still in flight', () => {
            // The gate counts as loading: the skeleton is on screen from the first paint and
            // resolves seamlessly into rows, rather than a blank shell that then fills in.
            storeMock.isMisconfigured.mockReturnValue(false);
            storeMock.healthStatus.mockReturnValue(null);
            spectator.detectChanges();

            expect(spectator.queryAll(byTestId('experiments-loading-row')).length).toBeGreaterThan(
                0
            );
            expect(spectator.query(byTestId('experiments-misconfiguration'))).toBeNull();
            expect(spectator.query(byTestId('experiments-empty-state'))).toBeNull();
        });

        it('should hide the toolbar, the filters and the table', () => {
            renderMisconfigured(HealthStatusTypes.NOT_CONFIGURED);

            expect(spectator.query(byTestId('experiments-search-input'))).toBeNull();
            expect(spectator.query(byTestId('experiments-status-filter'))).toBeNull();
            expect(spectator.query(byTestId('experiments-goal-filter'))).toBeNull();
            expect(spectator.query(byTestId('experiments-table-wrapper'))).toBeNull();
            expect(spectator.query(byTestId('experiments-table'))).toBeNull();
            expect(spectator.query(byTestId('experiments-empty-state'))).toBeNull();
        });

        it('should render the list untouched when analytics is healthy', () => {
            renderRowWith(DotExperimentStatus.DRAFT);

            expect(spectator.query(byTestId('experiments-misconfiguration'))).toBeNull();
            expect(spectator.query(byTestId('experiments-table'))).not.toBeNull();
            expect(spectator.query(byTestId('experiment-row'))).not.toBeNull();
        });
    });

    describe('column widths', () => {
        it('should lay the table out fixed so widths do not follow the visible rows', () => {
            // Regression: with the default `auto` layout the browser sizes each column to its
            // content, so filtering to a status whose rows carry no date range (Draft shows
            // "Not scheduled") collapsed the Schedule column and shifted every column after it.
            renderRowWith(DotExperimentStatus.DRAFT);

            const table = spectator.query('table.p-datatable-table') as HTMLTableElement;

            expect(table).not.toBeNull();
            expect(table.style.tableLayout).toBe('fixed');
        });
    });

    describe('filters', () => {
        it('should render a chip for both status and goal', () => {
            renderRowWith(DotExperimentStatus.DRAFT);

            expect(spectator.query(byTestId('experiments-status-filter'))).not.toBeNull();
            expect(spectator.query(byTestId('experiments-goal-filter'))).not.toBeNull();
        });

        it('should offer one option per goal, counted', () => {
            storeMock.goalCounts.mockReturnValue({
                ...EMPTY_GOAL_COUNTS,
                [GOAL_TYPES.BOUNCE_RATE]: 4
            });
            renderRowWith(DotExperimentStatus.DRAFT);

            const options = spectator.component['$goalFilterOptions']();

            expect(options.length).toBe(Object.values(GOAL_TYPES).length);
            expect(options.find(({ value }) => value === GOAL_TYPES.BOUNCE_RATE)?.count).toBe('4');
        });

        it('should dispatch goalsChanged with the picked goals', () => {
            renderRowWith(DotExperimentStatus.DRAFT);

            spectator.component.onGoalsChange([GOAL_TYPES.EXIT_RATE]);

            expect(dispatchedEvents()).toContainEqual(
                dotExperimentsListPageEvents.goalsChanged([GOAL_TYPES.EXIT_RATE])
            );
        });
    });

    describe('search clear', () => {
        beforeEach(() => {
            jest.useFakeTimers();
            // The component is created without an initial render, so the toolbar has to be
            // rendered before anything can be typed into it.
            spectator.detectChanges();
        });
        afterEach(() => jest.useRealTimers());

        const searchInput = () =>
            spectator.query(byTestId('experiments-search-input')) as HTMLInputElement;

        it('should not offer a clear control while the box is empty', () => {
            expect(spectator.query(byTestId('experiments-search-clear'))).toBeNull();
        });

        it('should offer a clear control once something is typed', () => {
            spectator.typeInElement('alpha', searchInput());
            spectator.detectChanges();

            expect(spectator.query(byTestId('experiments-search-clear'))).not.toBeNull();
        });

        it('should empty the box and hide itself when clicked', async () => {
            spectator.typeInElement('alpha', searchInput());
            spectator.detectChanges();

            spectator.click(spectator.query(byTestId('experiments-search-clear')) as HTMLElement);
            spectator.detectChanges();

            // `NgModel` pushes the model back to the input on a microtask, so the DOM value is
            // one tick behind the signal.
            await jest.advanceTimersByTimeAsync(0);
            spectator.detectChanges();

            expect(searchInput().value).toBe('');
            expect(spectator.query(byTestId('experiments-search-clear'))).toBeNull();
        });

        it('should dispatch the emptied term after the debounce window', async () => {
            // Arrive at "a term is applied": type it, then let the store report it as applied.
            // The mock's signals are plain functions, so the term has to be typed rather than
            // seeded through `filter` — a linkedSignal would never recompute from it.
            spectator.typeInElement('alpha', searchInput());
            spectator.detectChanges();
            storeMock.filter.mockReturnValue('alpha');
            await jest.advanceTimersByTimeAsync(SEARCH_DEBOUNCE_MS);
            spectator.detectChanges();

            spectator.click(spectator.query(byTestId('experiments-search-clear')) as HTMLElement);
            spectator.detectChanges();

            // Clearing writes the same signal typing does, so it settles through the debounce
            // rather than dispatching straight away.
            await jest.advanceTimersByTimeAsync(SEARCH_DEBOUNCE_MS);
            spectator.detectChanges();

            expect(dispatchedEvents()).toContainEqual(
                dotExperimentsListPageEvents.filterChanged('')
            );
        });
    });

    describe('paginator', () => {
        it('should render the content-drive paginator shape: a page report, prev and next only', () => {
            renderRowWith(DotExperimentStatus.DRAFT);

            const paginator = spectator.query('p-paginator');

            expect(paginator?.querySelector('.p-paginator-current')?.textContent).toContain(
                'Page 1'
            );
            // First/last jumps and the numbered page links are both off, as in content-drive.
            expect(paginator?.querySelector('.p-paginator-first')).toBeNull();
            expect(paginator?.querySelector('.p-paginator-last')).toBeNull();
            expect(paginator?.querySelector('.p-paginator-pages')).toBeNull();
            expect(paginator?.querySelector('.p-paginator-prev')).not.toBeNull();
            expect(paginator?.querySelector('.p-paginator-next')).not.toBeNull();
        });
    });

    describe('table events', () => {
        it('should translate a lazy-load offset into a 1-based page', () => {
            renderRowWith(DotExperimentStatus.DRAFT);

            spectator.component.onLazyLoad({ first: 50, rows: 25 });

            expect(dispatchedEvents()).toContainEqual(
                dotExperimentsListPageEvents.pageChanged({ page: 3, perPage: 25 })
            );
        });

        it('should fall back to the store paging when the event omits it', () => {
            renderRowWith(DotExperimentStatus.DRAFT);

            spectator.component.onLazyLoad({});

            expect(dispatchedEvents()).toContainEqual(
                dotExperimentsListPageEvents.pageChanged({
                    page: 1,
                    perPage: DEFAULT_EXPERIMENTS_LIST_PER_PAGE
                })
            );
        });

        it('should map the sort order onto a direction', () => {
            renderRowWith(DotExperimentStatus.DRAFT);

            spectator.component.onLazyLoad({
                first: 0,
                rows: 25,
                sortField: 'name',
                sortOrder: -1
            });

            expect(dispatchedEvents()).toContainEqual(
                dotExperimentsListPageEvents.sortChanged({ orderBy: 'name', direction: 'DESC' })
            );
        });

        it('should take the first field when the table reports an array', () => {
            renderRowWith(DotExperimentStatus.DRAFT);

            spectator.component.onLazyLoad({
                first: 0,
                rows: 25,
                sortField: ['status', 'name'],
                sortOrder: 1
            });

            expect(dispatchedEvents()).toContainEqual(
                dotExperimentsListPageEvents.sortChanged({ orderBy: 'status', direction: 'ASC' })
            );
        });

        it('should not dispatch a sort when the event carries no field', () => {
            renderRowWith(DotExperimentStatus.DRAFT);
            // Rendering the table fires its own lazy load, and that one does carry the current
            // sort field — so only what happens after this point is under test.
            dispatch.mockClear();

            spectator.component.onLazyLoad({ first: 0, rows: 25 });

            expect(dispatchedEvents().some(({ type }) => type.includes('sortChanged'))).toBe(false);
        });

        it('should not dispatch a sort when the event only carries the sort already applied', () => {
            renderRowWith(DotExperimentStatus.DRAFT);
            dispatch.mockClear();

            // What PrimeNG actually emits when you click page 2: `createLazyLoadMetadata()` puts
            // the *current* sortField and sortOrder on every lazy-load event, pagination included.
            spectator.component.onLazyLoad({
                first: 25,
                rows: 25,
                sortField: DEFAULT_EXPERIMENTS_LIST_ORDER_BY,
                sortOrder: -1
            });

            // Dispatching that no-op sort resets the page, so paging never advanced past 1 and a
            // `?page=N` deep link snapped back on load.
            expect(dispatchedEvents()).toContainEqual(
                dotExperimentsListPageEvents.pageChanged({ page: 2, perPage: 25 })
            );
            expect(dispatchedEvents().some(({ type }) => type.includes('sortChanged'))).toBe(false);
        });

        it('should still dispatch a sort when the direction actually changes', () => {
            renderRowWith(DotExperimentStatus.DRAFT);
            dispatch.mockClear();

            spectator.component.onLazyLoad({
                first: 0,
                rows: 25,
                sortField: DEFAULT_EXPERIMENTS_LIST_ORDER_BY,
                sortOrder: 1
            });

            expect(dispatchedEvents()).toContainEqual(
                dotExperimentsListPageEvents.sortChanged({
                    orderBy: DEFAULT_EXPERIMENTS_LIST_ORDER_BY,
                    direction: 'ASC'
                })
            );
        });

        it('should dispatch statusesChanged with the picked statuses', () => {
            renderRowWith(DotExperimentStatus.DRAFT);

            spectator.component.onStatusesChange([DotExperimentStatus.ENDED]);

            expect(dispatchedEvents()).toContainEqual(
                dotExperimentsListPageEvents.statusesChanged([DotExperimentStatus.ENDED])
            );
        });
    });

    describe('pagination', () => {
        const renderWith = (totalRecords: number) => {
            storeMock.pagedExperiments.mockReturnValue([experimentWith(DotExperimentStatus.DRAFT)]);
            storeMock.totalRecords.mockReturnValue(totalRecords);
            spectator.detectChanges();
        };

        const rowsPerPageSelect = () =>
            spectator.query('.p-paginator .p-select, .p-paginator p-select');

        it('should offer no page size while everything fits in the smallest one', () => {
            renderWith(Math.min(...ROWS_PER_PAGE_OPTIONS));

            // Every option would render the same single page, and the arrows are already disabled.
            expect(rowsPerPageSelect()).toBeNull();
        });

        it('should offer the page sizes once there is more than one page to reach', () => {
            renderWith(Math.min(...ROWS_PER_PAGE_OPTIONS) + 1);

            expect(rowsPerPageSelect()).not.toBeNull();
        });
    });

    describe('empty states', () => {
        const renderEmpty = () => {
            storeMock.pagedExperiments.mockReturnValue([]);
            storeMock.totalRecords.mockReturnValue(0);
            spectator.detectChanges();
        };

        const emptyTitle = () =>
            spectator.query(byTestId('experiments-empty-state'))?.textContent ?? '';

        it('should replace the table so the message can centre in the space it leaves', () => {
            renderEmpty();

            // Rendered inside the table the message sat in a short band under the header, with
            // the table's bottom border cutting across it.
            expect(spectator.query(byTestId('experiments-empty-state'))).not.toBeNull();
            expect(spectator.query(byTestId('experiments-table'))).toBeNull();
        });

        it('should say the site has none when nothing is filtered', () => {
            renderEmpty();

            expect(emptyTitle()).toContain('experiments.list.empty.title');
            expect(spectator.query(byTestId('experiments-empty-state'))?.textContent).not.toContain(
                'experiments.list.no-results.clear'
            );
        });

        it('should say nothing matched when a status is selected', () => {
            storeMock.selectedStatuses.mockReturnValue([DotExperimentStatus.SCHEDULED]);
            renderEmpty();

            // The site may well have experiments; these filters are hiding them.
            expect(emptyTitle()).toContain('experiments.list.no-results.title');
        });

        it('should say nothing matched when only a search term is set', () => {
            storeMock.filter.mockReturnValue('nothing-matches-this');
            renderEmpty();

            expect(emptyTitle()).toContain('experiments.list.no-results.title');
        });

        it('should say nothing matched when only a goal is selected', () => {
            storeMock.selectedGoals.mockReturnValue([GOAL_TYPES.BOUNCE_RATE]);
            renderEmpty();

            expect(emptyTitle()).toContain('experiments.list.no-results.title');
        });

        it('should offer a way out of the filtered empty state', () => {
            storeMock.selectedStatuses.mockReturnValue([DotExperimentStatus.SCHEDULED]);
            storeMock.selectedGoals.mockReturnValue([GOAL_TYPES.EXIT_RATE]);
            renderEmpty();

            spectator.component.onClearFilters();

            expect(dispatchedEvents()).toContainEqual(
                dotExperimentsListPageEvents.statusesChanged([])
            );
            expect(dispatchedEvents()).toContainEqual(
                dotExperimentsListPageEvents.goalsChanged([])
            );
        });

        it('should not show an empty state while the first load is still out', () => {
            storeMock.healthStatus.mockReturnValue(null);
            renderEmpty();

            // Skeletons, not "no experiments" — the answer is not in yet.
            expect(spectator.query(byTestId('experiments-empty-state'))).toBeNull();
        });
    });
});
