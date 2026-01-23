import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { MockComponent } from 'ng-mocks';
import { BehaviorSubject, of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { fakeAsync, tick } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';

import { ConfirmationService, MessageService } from 'primeng/api';

import {
    DotExperimentsService,
    DotHttpErrorManagerService,
    DotMessageService,
    DotFormatDateService
} from '@dotcms/data-access';
import {
    DotPushPublishDialogService,
    CoreWebService,
    DotcmsEventsService,
    LoginService,
    DotcmsConfigService,
    LoggerService
} from '@dotcms/dotcms-js';
import {
    ComponentStatus,
    DotExperimentStatus,
    DotExperimentsWithActions,
    SidebarStatus
} from '@dotcms/dotcms-models';
import { DotAddToBundleComponent } from '@dotcms/ui';
import { getExperimentMock } from '@dotcms/utils-testing';

import { DotExperimentsListComponent } from './dot-experiments-list.component';
import { DotExperimentsListStore, VmListExperiments } from './store/dot-experiments-list-store';

import { DotExperimentsStore } from '../dot-experiments-shell/store/dot-experiments.store';

const EXPERIMENT_MOCK_DRAFT = getExperimentMock(0);
const EXPERIMENT_MOCK_RUNNING = {
    ...getExperimentMock(1),
    status: DotExperimentStatus.RUNNING
};
const EXPERIMENT_MOCK_ENDED = {
    ...getExperimentMock(2),
    status: DotExperimentStatus.ENDED
};
const EXPERIMENT_MOCK_SCHEDULED = {
    ...getExperimentMock(3),
    status: DotExperimentStatus.SCHEDULED
};

describe('DotExperimentsListComponent', () => {
    let spectator: Spectator<DotExperimentsListComponent>;
    let store: DotExperimentsListStore;
    let router: jest.Mocked<Router>;
    let vmSubject: BehaviorSubject<VmListExperiments>;

    const createComponent = createComponentFactory({
        component: DotExperimentsListComponent,
        imports: [DotExperimentsListComponent, MockComponent(DotAddToBundleComponent)],
        providers: [
            provideHttpClient(),
            provideHttpClientTesting(),
            DotMessageService,
            MessageService,
            ConfirmationService,
            DotHttpErrorManagerService,
            mockProvider(DotExperimentsService),
            mockProvider(DotPushPublishDialogService),
            mockProvider(CoreWebService),
            mockProvider(DotcmsEventsService),
            mockProvider(LoginService),
            mockProvider(LoggerService),
            mockProvider(DotFormatDateService),
            mockProvider(DotcmsConfigService),
            mockProvider(DotExperimentsStore, {
                getPageId$: of('page-123'),
                getPageTitle$: of('Test Page')
            }),
            mockProvider(Router, {
                navigate: jest.fn().mockReturnValue(Promise.resolve(true))
            }),
            mockProvider(ActivatedRoute, {
                snapshot: {
                    params: { pageId: 'page-123' }
                }
            })
        ],
        detectChanges: false
    });

    beforeEach(() => {
        vmSubject = new BehaviorSubject<VmListExperiments>({
            experiments: [],
            isLoading: false,
            experimentsFiltered: [],
            filterStatus: [
                DotExperimentStatus.RUNNING,
                DotExperimentStatus.SCHEDULED,
                DotExperimentStatus.DRAFT,
                DotExperimentStatus.ENDED
            ],
            sidebar: {
                status: ComponentStatus.IDLE,
                isOpen: false
            } as SidebarStatus,
            pageId: 'page-123',
            pageTitle: 'Test Page',
            addToBundleContentId: null
        });

        const mockStore = {
            vm$: vmSubject.asObservable(),
            setFilterStatus: jest.fn(),
            openSidebar: jest.fn(),
            closeSidebar: jest.fn()
        };

        spectator = createComponent({
            providers: [mockProvider(DotExperimentsListStore, mockStore)]
        });

        store = spectator.inject(DotExperimentsListStore, true);
        router = spectator.inject(Router, true);

        spectator.detectChanges();
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    describe('template - loading state', () => {
        it('should show the skeleton component when is loading', () => {
            // Setup: Loading state
            vmSubject.next({
                experiments: [],
                isLoading: true,
                experimentsFiltered: [],
                filterStatus: [DotExperimentStatus.DRAFT],
                sidebar: { status: ComponentStatus.LOADING, isOpen: false },
                pageId: 'page-123',
                pageTitle: 'Test Page',
                addToBundleContentId: null
            });

            spectator.detectChanges();

            const skeleton = spectator.query('dot-experiments-list-skeleton');
            const emptyContainer = spectator.query('dot-empty-container');
            const tableComponent = spectator.query('dot-experiments-list-table');

            expect(skeleton).toBeTruthy();
            expect(emptyContainer).toBeNull();
            expect(tableComponent).toBeNull();
        });
    });

    describe('template - empty state', () => {
        it('should show the empty component when is not loading and no experiments', () => {
            // Setup: Empty state
            vmSubject.next({
                experiments: [],
                isLoading: false,
                experimentsFiltered: [],
                filterStatus: [DotExperimentStatus.DRAFT],
                sidebar: { status: ComponentStatus.IDLE, isOpen: false },
                pageId: 'page-123',
                pageTitle: 'Test Page',
                addToBundleContentId: null
            });

            spectator.detectChanges();

            const skeleton = spectator.query('dot-experiments-list-skeleton');
            const emptyContainer = spectator.query('dot-empty-container');
            const tableComponent = spectator.query('dot-experiments-list-table');

            expect(skeleton).toBeNull();
            expect(emptyContainer).toBeTruthy();
            expect(tableComponent).toBeNull();

            // Verify empty container configuration
            expect(emptyContainer?.textContent).toContain('experimentspage.not.experiments.founds');
        });
    });

    describe('template - experiments list', () => {
        it('should show the filters component and add experiment button exist when has experiments', () => {
            // Setup: Has experiments
            const experimentWithActions: DotExperimentsWithActions = {
                ...EXPERIMENT_MOCK_DRAFT,
                actionsItemsMenu: []
            };
            vmSubject.next({
                experiments: [EXPERIMENT_MOCK_DRAFT],
                isLoading: false,
                experimentsFiltered: [
                    {
                        status: DotExperimentStatus.DRAFT,
                        experiments: [experimentWithActions]
                    }
                ],
                filterStatus: [DotExperimentStatus.DRAFT],
                sidebar: { status: ComponentStatus.IDLE, isOpen: false },
                pageId: 'page-123',
                pageTitle: 'Test Page',
                addToBundleContentId: null
            });

            spectator.detectChanges();

            const filterComponent = spectator.query('dot-experiments-status-filter');
            const addButton = spectator.query('[data-testId="add-experiment-button"]');
            const tableComponent = spectator.query('dot-experiments-list-table');
            const emptyContainer = spectator.query('dot-empty-container');
            const skeleton = spectator.query('dot-experiments-list-skeleton');

            expect(filterComponent).toBeTruthy();
            expect(addButton).toBeTruthy();
            expect(tableComponent).toBeTruthy();
            expect(emptyContainer).toBeNull();
            expect(skeleton).toBeNull();
        });
    });

    describe('sidebar interactions', () => {
        it('should show the sidebar when click ADD EXPERIMENT', fakeAsync(() => {
            // Setup: Has experiments
            const experimentWithActions: DotExperimentsWithActions = {
                ...EXPERIMENT_MOCK_DRAFT,
                actionsItemsMenu: []
            };
            vmSubject.next({
                experiments: [EXPERIMENT_MOCK_DRAFT],
                isLoading: false,
                experimentsFiltered: [
                    {
                        status: DotExperimentStatus.DRAFT,
                        experiments: [experimentWithActions]
                    }
                ],
                filterStatus: [DotExperimentStatus.DRAFT],
                sidebar: { status: ComponentStatus.IDLE, isOpen: false },
                pageId: 'page-123',
                pageTitle: 'Test Page',
                addToBundleContentId: null
            });

            spectator.detectChanges();

            const addButton = spectator.query('[data-testId="add-experiment-button"]');
            expect(addButton).toBeTruthy();

            // Action: Click add experiment button
            spectator.click(addButton as Element);
            tick();

            // Verify: Store method was called to open sidebar
            expect(store.openSidebar).toHaveBeenCalled();
        }));
    });

    describe('navigation based on experiment status', () => {
        it('should go to report Container if the experiment status is RUNNING', () => {
            spectator.component.goToContainerAction(EXPERIMENT_MOCK_RUNNING);

            expect(router.navigate).toHaveBeenCalledWith(
                [
                    '/edit-page/experiments/',
                    EXPERIMENT_MOCK_RUNNING.pageId,
                    EXPERIMENT_MOCK_RUNNING.id,
                    'reports'
                ],
                {
                    queryParams: {
                        mode: null,
                        variantName: null,
                        experimentId: null
                    },
                    queryParamsHandling: 'merge'
                }
            );
        });

        it('should go to report Container if the experiment status is ENDED', () => {
            spectator.component.goToContainerAction(EXPERIMENT_MOCK_ENDED);

            expect(router.navigate).toHaveBeenCalledWith(
                [
                    '/edit-page/experiments/',
                    EXPERIMENT_MOCK_ENDED.pageId,
                    EXPERIMENT_MOCK_ENDED.id,
                    'reports'
                ],
                {
                    queryParams: {
                        mode: null,
                        variantName: null,
                        experimentId: null
                    },
                    queryParamsHandling: 'merge'
                }
            );
        });

        it('should go to configuration Container if the experiment status is DRAFT', () => {
            spectator.component.goToContainerAction(EXPERIMENT_MOCK_DRAFT);

            expect(router.navigate).toHaveBeenCalledWith(
                [
                    '/edit-page/experiments/',
                    EXPERIMENT_MOCK_DRAFT.pageId,
                    EXPERIMENT_MOCK_DRAFT.id,
                    'configuration'
                ],
                {
                    queryParams: {
                        mode: null,
                        variantName: null,
                        experimentId: null
                    },
                    queryParamsHandling: 'merge'
                }
            );
        });

        it('should go to configuration Container if the experiment status is SCHEDULED', () => {
            spectator.component.goToContainerAction(EXPERIMENT_MOCK_SCHEDULED);

            expect(router.navigate).toHaveBeenCalledWith(
                [
                    '/edit-page/experiments/',
                    EXPERIMENT_MOCK_SCHEDULED.pageId,
                    EXPERIMENT_MOCK_SCHEDULED.id,
                    'configuration'
                ],
                {
                    queryParams: {
                        mode: null,
                        variantName: null,
                        experimentId: null
                    },
                    queryParamsHandling: 'merge'
                }
            );
        });
    });

    describe('add to bundle dialog', () => {
        it('should show and remove add to bundle dialog', () => {
            // Setup: Show add to bundle dialog
            const experimentWithActions: DotExperimentsWithActions = {
                ...EXPERIMENT_MOCK_DRAFT,
                actionsItemsMenu: []
            };
            vmSubject.next({
                experiments: [EXPERIMENT_MOCK_DRAFT],
                isLoading: false,
                experimentsFiltered: [
                    {
                        status: DotExperimentStatus.DRAFT,
                        experiments: [experimentWithActions]
                    }
                ],
                filterStatus: [DotExperimentStatus.DRAFT],
                sidebar: { status: ComponentStatus.IDLE, isOpen: false },
                pageId: 'page-123',
                pageTitle: 'Test Page',
                addToBundleContentId: 'experiment-123'
            });

            spectator.detectChanges();

            // Verify: Add to bundle dialog is shown
            let addToBundleComponent = spectator.query('dot-add-to-bundle');
            expect(addToBundleComponent).toBeTruthy();

            // Setup: Remove add to bundle dialog
            vmSubject.next({
                experiments: [EXPERIMENT_MOCK_DRAFT],
                isLoading: false,
                experimentsFiltered: [
                    {
                        status: DotExperimentStatus.DRAFT,
                        experiments: [experimentWithActions]
                    }
                ],
                filterStatus: [DotExperimentStatus.DRAFT],
                sidebar: { status: ComponentStatus.IDLE, isOpen: false },
                pageId: 'page-123',
                pageTitle: 'Test Page',
                addToBundleContentId: null
            });

            spectator.detectChanges();

            // Verify: Add to bundle dialog is removed
            addToBundleComponent = spectator.query('dot-add-to-bundle');
            expect(addToBundleComponent).toBeNull();
        });
    });

    describe('filter interactions', () => {
        it('should call store method when filter is changed', () => {
            const newFilterStatus = [DotExperimentStatus.RUNNING, DotExperimentStatus.ENDED];

            spectator.component.selectedStatusFilter(newFilterStatus);

            expect(store.setFilterStatus).toHaveBeenCalledWith(newFilterStatus);
        });
    });

    describe('navigation - back button', () => {
        it('should navigate to edit page content when goToBrowserBack is called', () => {
            spectator.component.goToBrowserBack();

            expect(router.navigate).toHaveBeenCalledWith(['edit-page/content'], {
                queryParams: {
                    mode: null,
                    variantName: null,
                    experimentId: null
                },
                queryParamsHandling: 'merge'
            });
        });
    });
});
