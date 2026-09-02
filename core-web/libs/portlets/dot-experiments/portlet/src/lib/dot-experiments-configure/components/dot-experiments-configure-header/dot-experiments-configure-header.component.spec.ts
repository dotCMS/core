import { Dispatcher } from '@ngrx/signals/events';
import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';

import { provideLocationMocks } from '@angular/common/testing';
import { Component, input } from '@angular/core';
import { provideRouter, Router } from '@angular/router';

import { Confirmation, ConfirmationService, MenuItem } from 'primeng/api';
import { Tooltip } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import { DotPushPublishDialogService } from '@dotcms/dotcms-js';
import {
    CONFIGURATION_CONFIRM_DIALOG_KEY,
    DotExperiment,
    DotExperimentStatus
} from '@dotcms/dotcms-models';
import { DotAddToBundleComponent } from '@dotcms/ui';
import { getExperimentMock, MockDotMessageService } from '@dotcms/utils-testing';

import { DotExperimentsConfigureHeaderComponent } from './dot-experiments-configure-header.component';

import { STATUS_LABEL_KEYS } from '../../../shared/constants';
import { DotExperimentConfigurePage, ExperimentListAction } from '../../../shared/models';
import { dotExperimentsConfigurePageEvents } from '../../../store/dot-experiments-configure-page.events';
import { DotExperimentsConfigureStore } from '../../../store/dot-experiments-configure.store';
import { isAllowed } from '../../../util/dot-experiments-list.util';

/** Stand-in for the bundle dialog: rendering the real one would need its own HTTP stack. */
@Component({
    selector: 'dot-add-to-bundle',
    template: ''
})
class MockAddToBundleComponent {
    readonly assetIdentifier = input<string>();
}

const EXPERIMENT: DotExperiment = getExperimentMock(0);

const SELECTED_PAGE: DotExperimentConfigurePage = {
    pageId: EXPERIMENT.pageId,
    title: 'Blog',
    path: '/blog/index'
};

const NEW_EXPERIMENT_TITLE = 'New Experiment';
const NO_PAGE_COPY = 'No Page selected';
const COMING_SOON_COPY = 'Coming soon';

/** Kebab item ids, as declared by the component. */
const MENU_ITEM = {
    end: 'experiments-configure-end',
    abort: 'experiments-configure-abort',
    cancelSchedule: 'experiments-configure-cancel-schedule',
    pushPublish: 'experiments-configure-push-publish',
    addToBundle: 'experiments-configure-add-to-bundle'
} as const;

const messageServiceMock = new MockDotMessageService({
    'experiments.configure.header.new-experiment': NEW_EXPERIMENT_TITLE,
    'experiments.configure.header.no-page': NO_PAGE_COPY,
    'experiments.list.action.coming-soon': COMING_SOON_COPY,
    'experiments.action.view.results': 'View Results',
    'experiments.action.stop-experiment': 'Stop Experiment',
    'experiments.action.end-experiment': 'End Experiment',
    'experiments.action.abort.experiment': 'Abort Experiment',
    'experiments.configure.scheduling.cancel': 'Cancel Schedule',
    'contenttypes.content.push_publish': 'Push Publish',
    'contenttypes.content.add_to_bundle': 'Add to Bundle',
    draft: 'Draft',
    running: 'Running',
    scheduled: 'Scheduled',
    ended: 'Ended',
    archived: 'Archived'
});

/** Gating is `AllowedActionsByExperimentStatus` — the same map the store reads. */
const allowedActionsFor = (status: DotExperimentStatus): Record<ExperimentListAction, boolean> =>
    (
        [
            'delete',
            'abort',
            'results',
            'configuration',
            'archive',
            'end',
            'addToBundle',
            'pushPublish',
            'cancelSchedule'
        ] as ExperimentListAction[]
    ).reduce(
        (allowed, action) => ({ ...allowed, [action]: isAllowed(action, status) }),
        {} as Record<ExperimentListAction, boolean>
    );

const createStoreMock = () => ({
    experiment: jest.fn().mockReturnValue(EXPERIMENT),
    draftName: jest.fn().mockReturnValue(EXPERIMENT.name),
    selectedPage: jest.fn().mockReturnValue(SELECTED_PAGE),
    $status: jest.fn().mockReturnValue(DotExperimentStatus.DRAFT),
    $allowedActions: jest.fn().mockReturnValue(allowedActionsFor(DotExperimentStatus.DRAFT))
});

describe('DotExperimentsConfigureHeaderComponent', () => {
    let spectator: Spectator<DotExperimentsConfigureHeaderComponent>;
    let storeMock: ReturnType<typeof createStoreMock>;
    let dispatch: jest.SpyInstance;
    let confirm: jest.SpyInstance;

    const createComponent = createComponentFactory({
        component: DotExperimentsConfigureHeaderComponent,
        componentImports: [[DotAddToBundleComponent, MockAddToBundleComponent]],
        providers: [
            provideRouter([{ path: 'experiments', children: [] }]),
            provideLocationMocks(),
            { provide: DotExperimentsConfigureStore, useFactory: () => storeMock },
            { provide: DotMessageService, useValue: messageServiceMock },
            mockProvider(DotPushPublishDialogService),
            ConfirmationService
        ],
        detectChanges: false
    });

    /** `injectDispatch` appends a scope argument, so only the event itself is compared. */
    const dispatchedEvents = () => dispatch.mock.calls.map(([event]) => event);

    /** Renders the header for a status and returns nothing: every assertion reads the DOM. */
    const renderWith = (status: DotExperimentStatus) => {
        storeMock.$status.mockReturnValue(status);
        storeMock.$allowedActions.mockReturnValue(allowedActionsFor(status));
        storeMock.experiment.mockReturnValue({ ...EXPERIMENT, status });
        spectator.detectChanges();
    };

    const clickButton = (testId: string) => {
        const host = spectator.query(byTestId(testId));
        spectator.click(host?.querySelector('button') as HTMLElement);
        spectator.detectChanges();
    };

    const visibleMenuItemIds = (): string[] =>
        spectator.component
            .$menuItems()
            .filter(({ visible }) => visible)
            .map(({ id }) => id as string);

    const runMenuItem = (itemId: string) => {
        const item = spectator.component.$menuItems().find(({ id }) => id === itemId) as MenuItem;
        item.command?.({ originalEvent: new MouseEvent('click'), item });
        spectator.detectChanges();
    };

    /** Accepts the confirmation opened by the last action and returns it. */
    const acceptConfirmation = (): Confirmation => {
        const confirmation = confirm.mock.calls[0][0] as Confirmation;
        confirmation.accept?.();

        return confirmation;
    };

    beforeEach(() => {
        storeMock = createStoreMock();
        spectator = createComponent();
        dispatch = jest.spyOn(spectator.inject(Dispatcher), 'dispatch');
        const confirmationService = spectator.inject(ConfirmationService, true);
        confirm = jest
            .spyOn(confirmationService, 'confirm')
            .mockReturnValue(confirmationService) as jest.SpyInstance;
    });

    afterEach(() => {
        jest.restoreAllMocks();
    });

    describe('title', () => {
        it('should render the name as typed, without waiting for the autosave', () => {
            storeMock.draftName.mockReturnValue('Summer landing test');
            spectator.detectChanges();

            expect(spectator.query(byTestId('experiments-configure-title'))?.textContent).toContain(
                'Summer landing test'
            );
        });

        it('should fall back to New Experiment while the draft has no name', () => {
            storeMock.draftName.mockReturnValue('   ');
            spectator.detectChanges();

            expect(spectator.query(byTestId('experiments-configure-title'))?.textContent).toContain(
                NEW_EXPERIMENT_TITLE
            );
        });
    });

    describe('status tag', () => {
        it.each([
            [DotExperimentStatus.RUNNING, 'success'],
            [DotExperimentStatus.SCHEDULED, 'info'],
            [DotExperimentStatus.DRAFT, 'warn'],
            [DotExperimentStatus.ENDED, 'info'],
            [DotExperimentStatus.ARCHIVED, 'secondary']
        ])('should render %s as a %s tag', (status, severity) => {
            renderWith(status);

            const tag = spectator.query(byTestId('experiments-configure-status-tag'));

            expect(tag?.className).toContain(`p-tag-${severity}`);
            expect(tag?.textContent).toContain(
                messageServiceMock.get(STATUS_LABEL_KEYS.get(status) as string)
            );
        });
    });

    describe('subline', () => {
        it('should name the page the experiment runs on', () => {
            spectator.detectChanges();

            expect(
                spectator.query(byTestId('experiments-configure-subline'))?.textContent
            ).toContain(`${SELECTED_PAGE.title} · ${SELECTED_PAGE.path}`);
        });

        it('should say no page is selected while none is', () => {
            storeMock.selectedPage.mockReturnValue(null);
            spectator.detectChanges();

            expect(
                spectator.query(byTestId('experiments-configure-subline'))?.textContent
            ).toContain(NO_PAGE_COPY);
        });
    });

    describe('view results', () => {
        it.each([DotExperimentStatus.RUNNING, DotExperimentStatus.ENDED])(
            'should offer results for %s, disabled until the screen exists',
            (status) => {
                renderWith(status);

                const button = spectator
                    .query(byTestId('experiments-configure-results-btn'))
                    ?.querySelector('button') as HTMLButtonElement;

                expect(button).not.toBeNull();
                expect(button.disabled).toBe(true);
                expect(
                    (
                        spectator.query('[data-testid="experiments-configure-results-btn"]', {
                            read: Tooltip
                        }) as Tooltip
                    ).content
                ).toBe(COMING_SOON_COPY);
            }
        );

        it.each([
            DotExperimentStatus.DRAFT,
            DotExperimentStatus.SCHEDULED,
            DotExperimentStatus.ARCHIVED
        ])('should not offer results for %s', (status) => {
            renderWith(status);

            expect(spectator.query(byTestId('experiments-configure-results-btn'))).toBeNull();
        });
    });

    describe('stop', () => {
        it('should offer Stop while the experiment is running', () => {
            renderWith(DotExperimentStatus.RUNNING);

            expect(spectator.query(byTestId('experiments-configure-stop-btn'))).not.toBeNull();
        });

        // One status per test: the store mock's signals are plain `jest.fn()`s, so a second
        // `renderWith` in the same test would not recompute what the header derives from them.
        it.each([
            DotExperimentStatus.DRAFT,
            DotExperimentStatus.SCHEDULED,
            DotExperimentStatus.ENDED,
            DotExperimentStatus.ARCHIVED
        ])('should not offer Stop for %s', (status) => {
            renderWith(status);

            expect(spectator.query(byTestId('experiments-configure-stop-btn'))).toBeNull();
        });

        it('should confirm on the shell dialog before dispatching stopRequested', () => {
            renderWith(DotExperimentStatus.RUNNING);

            clickButton('experiments-configure-stop-btn');

            expect(confirm).toHaveBeenCalledTimes(1);
            expect(confirm.mock.calls[0][0].key).toBe(CONFIGURATION_CONFIRM_DIALOG_KEY);
            expect(dispatchedEvents()).not.toContainEqual(
                dotExperimentsConfigurePageEvents.stopRequested()
            );

            acceptConfirmation();

            expect(dispatchedEvents()).toContainEqual(
                dotExperimentsConfigurePageEvents.stopRequested()
            );
        });
    });

    describe('kebab', () => {
        it('should stay hidden until the experiment has been created', () => {
            storeMock.experiment.mockReturnValue(null);
            spectator.detectChanges();

            expect(spectator.query(byTestId('experiments-configure-actions-btn'))).toBeNull();
            expect(spectator.component.$menuItems()).toEqual([]);
        });

        it.each([
            [DotExperimentStatus.DRAFT, [MENU_ITEM.pushPublish, MENU_ITEM.addToBundle]],
            [
                DotExperimentStatus.RUNNING,
                [MENU_ITEM.end, MENU_ITEM.abort, MENU_ITEM.pushPublish, MENU_ITEM.addToBundle]
            ],
            [
                DotExperimentStatus.SCHEDULED,
                [MENU_ITEM.cancelSchedule, MENU_ITEM.pushPublish, MENU_ITEM.addToBundle]
            ],
            [DotExperimentStatus.ENDED, [MENU_ITEM.pushPublish, MENU_ITEM.addToBundle]],
            // An archived experiment can still be shipped to another environment, nothing else.
            [DotExperimentStatus.ARCHIVED, [MENU_ITEM.pushPublish, MENU_ITEM.addToBundle]]
        ])('should only offer the actions allowed for %s', (status, expectedItemIds) => {
            renderWith(status);

            expect(spectator.query(byTestId('experiments-configure-actions-btn'))).not.toBeNull();
            expect(visibleMenuItemIds().sort()).toEqual([...expectedItemIds].sort());
        });
    });

    describe('kebab actions', () => {
        it.each([
            [
                DotExperimentStatus.RUNNING,
                MENU_ITEM.end,
                dotExperimentsConfigurePageEvents.stopRequested()
            ],
            [
                DotExperimentStatus.RUNNING,
                MENU_ITEM.abort,
                dotExperimentsConfigurePageEvents.abortRequested()
            ],
            [
                DotExperimentStatus.SCHEDULED,
                MENU_ITEM.cancelSchedule,
                dotExperimentsConfigurePageEvents.cancelScheduleRequested()
            ]
        ])('should confirm %s / %s before dispatching', (status, itemId, expectedEvent) => {
            renderWith(status);

            runMenuItem(itemId);

            expect(confirm).toHaveBeenCalledTimes(1);
            expect(dispatchedEvents()).not.toContainEqual(expectedEvent);

            acceptConfirmation();

            expect(dispatchedEvents()).toContainEqual(expectedEvent);
        });

        it('should open the push publish dialog without a confirmation', () => {
            renderWith(DotExperimentStatus.DRAFT);
            const pushPublishDialogService = spectator.inject(DotPushPublishDialogService, true);

            runMenuItem(MENU_ITEM.pushPublish);

            expect(confirm).not.toHaveBeenCalled();
            expect(pushPublishDialogService.open).toHaveBeenCalledWith(
                expect.objectContaining({ assetIdentifier: EXPERIMENT.id })
            );
        });

        it('should open the add to bundle dialog on the experiment', () => {
            renderWith(DotExperimentStatus.DRAFT);

            runMenuItem(MENU_ITEM.addToBundle);

            expect(spectator.component.$addToBundleAssetId()).toBe(EXPERIMENT.id);
            expect(spectator.query('dot-add-to-bundle')).not.toBeNull();
        });

        it('should not render the add to bundle dialog until it is asked for', () => {
            renderWith(DotExperimentStatus.DRAFT);

            expect(spectator.query('dot-add-to-bundle')).toBeNull();
        });
    });

    describe('back', () => {
        it('should leave for the experiments list', () => {
            const navigate = jest.spyOn(spectator.inject(Router), 'navigate');
            spectator.detectChanges();

            clickButton('experiments-configure-back-btn');

            expect(navigate).toHaveBeenCalledWith(['/experiments']);
        });
    });
});
