import { Dispatcher, EventCreator } from '@ngrx/signals/events';
import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';

import { Location } from '@angular/common';
import { provideLocationMocks } from '@angular/common/testing';
import { provideRouter, Router } from '@angular/router';

import { ConfirmationService, Confirmation, MenuItem } from 'primeng/api';

import { DotMessageDisplayService, DotMessageService } from '@dotcms/data-access';
import { DotPushPublishDialogService } from '@dotcms/dotcms-js';
import {
    DotExperiment,
    DotExperimentStatus,
    DotMessageSeverity,
    HealthStatusTypes
} from '@dotcms/dotcms-models';
import { getExperimentMock, MockDotMessageService } from '@dotcms/utils-testing';

import { DotExperimentsListComponent } from './dot-experiments-list.component';

import { dotExperimentsListEvents } from '../store/dot-experiments-list.events';
import {
    DEFAULT_EXPERIMENTS_LIST_DIRECTION,
    DEFAULT_EXPERIMENTS_LIST_ORDER_BY,
    DEFAULT_EXPERIMENTS_LIST_PAGE,
    DEFAULT_EXPERIMENTS_LIST_PER_PAGE,
    DEFAULT_EXPERIMENTS_LIST_STATUSES,
    DotExperimentsListStore
} from '../store/dot-experiments-list.store';

const PAGE_ID = 'page-1';

const PAGE_INFO = { [PAGE_ID]: { url: '/blog/index', host: 'host-1' } };

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

const messageServiceMock = new MockDotMessageService({
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
    'experiments.notification.cancel.schedule': 'Experiment {0} unscheduled'
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
    filter: jest.fn().mockReturnValue(''),
    status: jest.fn().mockReturnValue('loaded'),
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
    });

    afterEach(() => {
        jest.restoreAllMocks();
    });

    describe('search', () => {
        beforeEach(() => jest.useFakeTimers());
        afterEach(() => jest.useRealTimers());

        it('should dispatch filterChanged only after the debounce window', () => {
            spectator.detectChanges();

            spectator.typeInElement(
                'summer',
                spectator.query(byTestId('experiments-search-input')) as HTMLInputElement
            );

            expect(dispatchedEvents()).not.toContainEqual(
                dotExperimentsListEvents.filterChanged('summer')
            );

            jest.advanceTimersByTime(300);

            expect(dispatchedEvents()).toContainEqual(
                dotExperimentsListEvents.filterChanged('summer')
            );
        });
    });

    describe('row actions', () => {
        it.each([
            [
                DotExperimentStatus.DRAFT,
                [MENU_ITEM.delete, MENU_ITEM.pushPublish, MENU_ITEM.addToBundle]
            ],
            [
                DotExperimentStatus.RUNNING,
                [MENU_ITEM.end, MENU_ITEM.abort, MENU_ITEM.pushPublish, MENU_ITEM.addToBundle]
            ],
            [
                DotExperimentStatus.SCHEDULED,
                [
                    MENU_ITEM.cancelSchedule,
                    MENU_ITEM.delete,
                    MENU_ITEM.pushPublish,
                    MENU_ITEM.addToBundle
                ]
            ],
            [DotExperimentStatus.ENDED, [MENU_ITEM.pushPublish, MENU_ITEM.addToBundle]],
            [DotExperimentStatus.ARCHIVED, [MENU_ITEM.pushPublish, MENU_ITEM.addToBundle]]
        ])('should only offer the actions allowed for %s', (status, expectedItemIds) => {
            renderRowWith(status);

            expect(visibleMenuItemIds().sort()).toEqual([...expectedItemIds].sort());
        });

        it.each([
            [DotExperimentStatus.ENDED, [ARCHIVE_LABEL, ACTIONS_MENU_LABEL]],
            [DotExperimentStatus.ARCHIVED, [RESTORE_LABEL, ACTIONS_MENU_LABEL]],
            [DotExperimentStatus.DRAFT, [ACTIONS_MENU_LABEL]],
            [DotExperimentStatus.RUNNING, [ACTIONS_MENU_LABEL]],
            [DotExperimentStatus.SCHEDULED, [ACTIONS_MENU_LABEL]]
        ])(
            'should render no primary action for %s, only the allowed icon buttons',
            (status, expectedLabels) => {
                renderRowWith(status);

                const labels = Array.from(
                    spectator
                        .query(byTestId('experiment-row'))
                        ?.querySelectorAll('td:last-child p-button') ?? []
                ).map((button) => button.getAttribute('aria-label'));

                expect(labels).toEqual(expectedLabels);
            }
        );

        it('should render the restore affordance disabled — there is no restore transition yet', () => {
            renderRowWith(DotExperimentStatus.ARCHIVED);

            const restore = spectator
                .query(byTestId('experiment-restore-btn'))
                ?.querySelector('button');

            expect(restore).not.toBeNull();
            expect((restore as HTMLButtonElement).disabled).toBe(true);
        });

        it('should never offer a configure or a view-results control', () => {
            Object.values(DotExperimentStatus).forEach((status) => {
                renderRowWith(status);

                expect(spectator.query(byTestId('experiment-configure-btn'))).toBeNull();
                expect(spectator.query(byTestId('experiment-results-btn'))).toBeNull();
                expect(visibleMenuItemIds()).not.toContain('experiments-configuration');
                expect(visibleMenuItemIds()).not.toContain('experiments-results');
            });
        });
    });

    describe('confirm then dispatch', () => {
        it('should dispatch archiveRequested once the archive confirmation is accepted', () => {
            const experiment = renderRowWith(DotExperimentStatus.ENDED);

            clickButton('experiment-archive-btn');

            expect(dispatchedEvents()).not.toContainEqual(
                dotExperimentsListEvents.archiveRequested(experiment)
            );

            acceptConfirmation();

            expect(dispatchedEvents()).toContainEqual(
                dotExperimentsListEvents.archiveRequested(experiment)
            );
        });

        it.each([
            [DotExperimentStatus.DRAFT, MENU_ITEM.delete, dotExperimentsListEvents.deleteRequested],
            [DotExperimentStatus.RUNNING, MENU_ITEM.end, dotExperimentsListEvents.endRequested],
            [DotExperimentStatus.RUNNING, MENU_ITEM.abort, dotExperimentsListEvents.abortRequested],
            [
                DotExperimentStatus.SCHEDULED,
                MENU_ITEM.cancelSchedule,
                dotExperimentsListEvents.cancelScheduleRequested
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
            ['archived', dotExperimentsListEvents.archiveSucceeded],
            ['deleted', dotExperimentsListEvents.deleteSucceeded],
            ['ended', dotExperimentsListEvents.endSucceeded],
            ['aborted', dotExperimentsListEvents.abortSucceeded],
            ['unscheduled', dotExperimentsListEvents.cancelScheduleSucceeded]
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

        it('should hide the toolbar, the filters and the table', () => {
            renderMisconfigured(HealthStatusTypes.NOT_CONFIGURED);

            expect(spectator.query(byTestId('experiments-search-input'))).toBeNull();
            expect(spectator.query(byTestId('experiments-status-filter'))).toBeNull();
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

    describe('url write-back', () => {
        let router: Router;
        let go: jest.SpyInstance;

        /** Navigates first, then flushes the sync effect with the store values under test. */
        const flushUrlEffect = async (url: string) => {
            router = spectator.inject(Router);
            await router.navigateByUrl(url);
            go = jest.spyOn(spectator.inject(Location), 'go');
            spectator.detectChanges();
        };

        const writtenUrl = (): string => go.mock.calls[0][0] as string;

        it('should strip every param whose value equals its default', async () => {
            await flushUrlEffect('/experiments?page=3&per_page=10&filter=old&status=DRAFT');

            expect(writtenUrl()).toBe('/experiments');
        });

        it('should write only the params that differ from their defaults', async () => {
            storeMock.page.mockReturnValue(2);
            storeMock.perPage.mockReturnValue(10);
            storeMock.filter.mockReturnValue('summer');
            storeMock.direction.mockReturnValue('ASC');

            await flushUrlEffect('/experiments');

            const params = new URLSearchParams(writtenUrl().split('?')[1]);
            expect(params.get('page')).toBe('2');
            expect(params.get('per_page')).toBe('10');
            expect(params.get('filter')).toBe('summer');
            expect(params.get('direction')).toBe('ASC');
            expect(params.get('orderby')).toBeNull();
            expect(params.getAll('status')).toEqual([]);
        });

        it('should write a repeatable status param for a non-default selection', async () => {
            storeMock.selectedStatuses.mockReturnValue([
                DotExperimentStatus.DRAFT,
                DotExperimentStatus.ARCHIVED
            ]);

            await flushUrlEffect('/experiments');

            const params = new URLSearchParams(writtenUrl().split('?')[1]);
            expect(params.getAll('status')).toEqual([
                DotExperimentStatus.DRAFT,
                DotExperimentStatus.ARCHIVED
            ]);
        });

        it('should not write the status param when the default selection is reordered', async () => {
            storeMock.selectedStatuses.mockReturnValue(
                [...DEFAULT_EXPERIMENTS_LIST_STATUSES].reverse()
            );

            await flushUrlEffect('/experiments?status=DRAFT');

            expect(writtenUrl()).toBe('/experiments');
        });
    });
});
