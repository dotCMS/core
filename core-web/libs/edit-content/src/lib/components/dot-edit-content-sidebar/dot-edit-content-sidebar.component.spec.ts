import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@ngneat/spectator/jest';
import { MockComponent } from 'ng-mocks';
import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { fakeAsync, tick } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';

import { ConfirmationService, MessageService } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';
import { TabView } from 'primeng/tabs';

import {
    DotContentletService,
    DotContentTypeService,
    DotCurrentUserService,
    DotHttpErrorManagerService,
    DotLanguagesService,
    DotMessageService,
    DotSiteService,
    DotSystemConfigService,
    DotVersionableService,
    DotWorkflowActionsFireService,
    DotWorkflowsActionsService,
    DotWorkflowService
} from '@dotcms/data-access';
import { DotContentletCanLock } from '@dotcms/dotcms-models';
import { MOCK_SINGLE_WORKFLOW_ACTIONS } from '@dotcms/utils-testing';

import { DotEditContentSidebarActivitiesComponent } from './components/dot-edit-content-sidebar-activities/dot-edit-content-sidebar-activities.component';
import { DotEditContentSidebarHistoryComponent } from './components/dot-edit-content-sidebar-history/dot-edit-content-sidebar-history.component';
import { DotEditContentSidebarInformationComponent } from './components/dot-edit-content-sidebar-information/dot-edit-content-sidebar-information.component';
import { DotEditContentSidebarLocalesComponent } from './components/dot-edit-content-sidebar-locales/dot-edit-content-sidebar-locales.component';
import { DotEditContentSidebarWorkflowComponent } from './components/dot-edit-content-sidebar-workflow/dot-edit-content-sidebar-workflow.component';
import { DotEditContentSidebarComponent } from './dot-edit-content-sidebar.component';

import { DotEditContentService } from '../../services/dot-edit-content.service';
import { DotEditContentStore } from '../../store/edit-content.store';
import { MOCK_WORKFLOW_STATUS } from '../../utils/edit-content.mock';
import * as utils from '../../utils/functions.util';
import { CONTENT_TYPE_MOCK } from '../../utils/mocks';

describe('DotEditContentSidebarComponent', () => {
    let spectator: Spectator<DotEditContentSidebarComponent>;
    let dotEditContentService: SpyObject<DotEditContentService>;
    let dotWorkflowService: SpyObject<DotWorkflowService>;
    let dotContentletService: SpyObject<DotContentletService>;
    let store: SpyObject<InstanceType<typeof DotEditContentStore>>;

    const createComponent = createComponentFactory({
        component: DotEditContentSidebarComponent,
        declarations: [
            MockComponent(DotEditContentSidebarInformationComponent),
            MockComponent(DotEditContentSidebarWorkflowComponent)
        ],
        imports: [
            TabsModule,
            DotEditContentSidebarActivitiesComponent,
            DotEditContentSidebarHistoryComponent
        ], // I need the real components to be rendered in the p-template="content"
        providers: [
            DotEditContentStore,
            mockProvider(DotWorkflowsActionsService),
            mockProvider(DotWorkflowActionsFireService),
            mockProvider(DotEditContentService),
            mockProvider(DotContentTypeService),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(DotMessageService),
            mockProvider(Router, {
                navigate: jest.fn().mockReturnValue(Promise.resolve(true)),
                url: '/test-url',
                events: of()
            }),
            mockProvider(DotWorkflowService),
            mockProvider(MessageService),
            mockProvider(ConfirmationService),
            mockProvider(DotContentletService),
            mockProvider(DotLanguagesService),
            mockProvider(DotVersionableService),
            mockProvider(DotSiteService),
            mockProvider(DotSystemConfigService),
            mockProvider(DialogService),
            provideHttpClient(),
            provideHttpClientTesting(),
            {
                provide: DotCurrentUserService,
                useValue: {
                    getCurrentUser: () =>
                        of({
                            userId: '123',
                            userName: 'John Doe'
                        })
                }
            },
            {
                provide: ActivatedRoute,
                useValue: {
                    get snapshot() {
                        return { params: { id: undefined, contentType: undefined } };
                    }
                }
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({ detectChanges: false });

        store = spectator.inject(DotEditContentStore, true);
        dotEditContentService = spectator.inject(DotEditContentService);
        dotWorkflowService = spectator.inject(DotWorkflowService);
        dotContentletService = spectator.inject(DotContentletService);

        // Mock the initial UI state
        jest.spyOn(utils, 'getStoredUIState').mockReturnValue({
            view: 'form',
            activeTab: 0,
            isSidebarOpen: true,
            activeSidebarTab: 0,
            isBetaMessageVisible: true
        });

        dotEditContentService.getReferencePages.mockReturnValue(of(1));
        dotEditContentService.getActivities.mockReturnValue(of([]));
        dotEditContentService.getVersions.mockReturnValue(
            of({
                entity: [],
                pagination: null,
                errors: [],
                i18nMessagesMap: {},
                messages: [],
                permissions: []
            })
        );
        dotEditContentService.getPushPublishHistory.mockReturnValue(
            of({
                entity: [],
                pagination: null,
                errors: [],
                i18nMessagesMap: {},
                messages: [],
                permissions: []
            })
        );
        dotEditContentService.deletePushPublishHistory.mockReturnValue(of({}));
        dotWorkflowService.getWorkflowStatus.mockReturnValue(of(MOCK_WORKFLOW_STATUS));
        dotContentletService.canLock.mockReturnValue(of({ canLock: true } as DotContentletCanLock));

        spectator.detectChanges();
    });

    describe('Initial Render', () => {
        it('should create the component', () => {
            expect(spectator.component).toBeTruthy();
        });

        it('should render PrimeNG TabView', () => {
            const tabView = spectator.query(TabView);
            expect(tabView).toBeTruthy();
        });

        describe('Components', () => {
            it('should render DotEditContentSidebarInformationComponent', () => {
                const informationComponent = spectator.query(
                    DotEditContentSidebarInformationComponent
                );
                expect(informationComponent).toBeTruthy();
            });

            it('should render DotEditContentSidebarWorkflowComponent', () => {
                const workflowComponent = spectator.query(DotEditContentSidebarWorkflowComponent);
                expect(workflowComponent).toBeTruthy();
            });

            it('should render DotEditContentSidebarLocalesComponent', () => {
                const localesComponent = spectator.query(DotEditContentSidebarLocalesComponent);
                expect(localesComponent).toBeTruthy();
            });

            it('should render DotEditContentSidebarHistoryComponent when history tab is active', fakeAsync(() => {
                spectator.detectChanges();
                tick();

                const tabView = spectator.query(byTestId('sidebar-tabs'));
                expect(tabView).toBeTruthy();

                const tabs = tabView.querySelectorAll('[role="tab"]');
                expect(tabs.length).toBeGreaterThan(1);

                const historyTabLink = tabs[1]; // History is the second tab
                expect(historyTabLink).toBeTruthy();

                // Click the history tab to activate it
                spectator.click(historyTabLink);
                tick();
                spectator.detectChanges();

                // Now the history component should be rendered
                const historyElement = spectator.query('[data-testId="history"]');
                expect(historyElement).toBeTruthy();

                const historyComponent = spectator.query(DotEditContentSidebarHistoryComponent);
                expect(historyComponent).toBeTruthy();
            }));
        });
    });

    describe('Sidebar Controls', () => {
        it('should render toggle button', () => {
            const toggleButton = spectator.query('[data-testId="toggle-button"]');
            expect(toggleButton).toBeTruthy();
        });

        it('should render append content in TabView', () => {
            const tabViewElement = spectator.query('p-tabview');
            const appendContent = tabViewElement.querySelector(
                '[data-testid="tabview-append-content"]'
            );
            expect(appendContent).toBeTruthy();
        });

        it('should call toggleSidebar when toggle button is clicked', () => {
            const storeSpy = jest.spyOn(store, 'toggleSidebar');
            spectator.click(byTestId('toggle-button'));
            expect(storeSpy).toHaveBeenCalled();
        });
    });

    describe('Tabs Behavior', () => {
        beforeEach(fakeAsync(() => {
            // Mock the services needed for initializeExistingContent
            const dotContentTypeService = spectator.inject(DotContentTypeService);
            const workflowActionsService = spectator.inject(DotWorkflowsActionsService);
            const dotWorkflowService = spectator.inject(DotWorkflowService);
            const dotEditContentService = spectator.inject(DotEditContentService);

            // Mock contentlet response with all required DotCMSContentlet properties
            const mockContentlet = {
                inode: '123',
                contentType: 'testContentType',
                archived: false,
                baseType: 'CONTENT',
                folder: 'SYSTEM_FOLDER',
                hasTitleImage: false,
                host: 'demo.dotcms.com',
                hostName: 'demo.dotcms.com',
                identifier: '123-456',
                languageId: 1,
                live: true,
                locked: false,
                modDate: new Date().toISOString(),
                modUser: 'admin',
                modUserName: 'Admin User',
                owner: 'admin',
                permissionId: '123',
                permissionType: 'CONTENT',
                title: 'Test Content',
                working: true,
                URL_MAP_FOR_CONTENT: '/test',
                sortOrder: 0,
                stInode: '123-stInode',
                structure: {
                    name: 'Test Structure',
                    inode: '456'
                },
                titleImage: '',
                url: '/test-content'
            };

            dotEditContentService.getContentById.mockReturnValue(of(mockContentlet));
            dotContentTypeService.getContentType.mockReturnValue(of(CONTENT_TYPE_MOCK));
            workflowActionsService.getByInode.mockReturnValue(of([]));
            workflowActionsService.getWorkFlowActions.mockReturnValue(
                of(MOCK_SINGLE_WORKFLOW_ACTIONS)
            );
            dotWorkflowService.getWorkflowStatus.mockReturnValue(of(MOCK_WORKFLOW_STATUS));
            dotContentletService.canLock.mockReturnValue(
                of({ locked: false, canLock: true } as DotContentletCanLock)
            );

            // Initialize existing content
            store.initializeExistingContent('123');
            tick();
            spectator.detectChanges();
        }));

        describe('Initial State', () => {
            it('should initialize with correct UI state', fakeAsync(() => {
                expect(store.isSidebarOpen()).toBe(true);
                expect(store.activeSidebarTab()).toBe(0);
            }));
        });

        describe('Tab Navigation', () => {
            it('should update active tab when changed programmatically', fakeAsync(() => {
                store.setActiveSidebarTab(1);
                tick();
                expect(store.activeSidebarTab()).toBe(1);
            }));

            it('should update store and render content when clicking activities tab', fakeAsync(() => {
                spectator.detectChanges();
                tick();

                // Find and click the activities tab
                const tabView = spectator.query(byTestId('sidebar-tabs'));
                expect(tabView).toBeTruthy();

                const tabs = tabView.querySelectorAll('[role="tab"]');
                expect(tabs.length).toBeGreaterThan(2);

                const activitiesTabLink = tabs[2]; // Activities is now the third tab (0: info, 1: history, 2: activities)
                expect(activitiesTabLink).toBeTruthy();

                // Verify store update
                const storeSpy = jest.spyOn(store, 'setActiveSidebarTab');
                spectator.click(activitiesTabLink);
                tick();

                expect(storeSpy).toHaveBeenCalledWith(2);
                expect(store.activeSidebarTab()).toBe(2);

                // Verify content rendering
                const activitiesComponent = spectator.query(
                    DotEditContentSidebarActivitiesComponent
                );
                expect(activitiesComponent).toBeTruthy();
            }));

            it('should update store and render content when clicking history tab', fakeAsync(() => {
                spectator.detectChanges();
                tick();

                // Find and click the history tab
                const tabView = spectator.query(byTestId('sidebar-tabs'));
                expect(tabView).toBeTruthy();

                const tabs = tabView.querySelectorAll('[role="tab"]');
                expect(tabs.length).toBeGreaterThan(1);

                const historyTabLink = tabs[1]; // History is the second tab
                expect(historyTabLink).toBeTruthy();

                // Verify store update
                const storeSpy = jest.spyOn(store, 'setActiveSidebarTab');
                spectator.click(historyTabLink);
                tick();

                expect(storeSpy).toHaveBeenCalledWith(1);
                expect(store.activeSidebarTab()).toBe(1);

                // Verify content rendering
                const historyComponent = spectator.query(DotEditContentSidebarHistoryComponent);
                expect(historyComponent).toBeTruthy();
            }));
        });

        describe('Version History Integration', () => {
            it('should call onVersionsPageChange when history component emits pageChange', fakeAsync(() => {
                // Switch to history tab first
                store.setActiveSidebarTab(1);
                tick();
                spectator.detectChanges();

                const storeSpy = jest.spyOn(store, 'loadVersions');
                const component = spectator.component;

                // Mock the identifier signal to return a test value
                Object.defineProperty(component, '$identifier', {
                    value: jest.fn().mockReturnValue('test-identifier'),
                    writable: true
                });

                // Call the method directly
                component.onVersionsPageChange(2);

                expect(storeSpy).toHaveBeenCalledWith({ identifier: 'test-identifier', page: 2 });
            }));
        });

        describe('Push Publish History Integration', () => {
            it('should call onPushPublishPageChange when history component emits pushPublishPageChange', fakeAsync(() => {
                // Switch to history tab first
                store.setActiveSidebarTab(1);
                tick();
                spectator.detectChanges();

                const storeSpy = jest.spyOn(store, 'loadPushPublishHistory');
                const component = spectator.component;

                // Mock the identifier signal to return a test value
                Object.defineProperty(component, '$identifier', {
                    value: jest.fn().mockReturnValue('test-identifier'),
                    writable: true
                });

                // Call the method directly
                component.onPushPublishPageChange(3);

                expect(storeSpy).toHaveBeenCalledWith({ identifier: 'test-identifier', page: 3 });
            }));

            it('should call onDeletePushPublishHistory when history component emits deletePushPublishHistory', fakeAsync(() => {
                // Switch to history tab first
                store.setActiveSidebarTab(1);
                tick();
                spectator.detectChanges();

                const storeSpy = jest.spyOn(store, 'deletePushPublishHistory');
                const component = spectator.component;

                // Mock the identifier signal to return a test value
                Object.defineProperty(component, '$identifier', {
                    value: jest.fn().mockReturnValue('test-identifier'),
                    writable: true
                });

                // Call the method directly
                component.onDeletePushPublishHistory();

                expect(storeSpy).toHaveBeenCalledWith('test-identifier');
            }));
        });

        describe('Sidebar Visibility', () => {
            it('should toggle sidebar visibility', fakeAsync(() => {
                const initialState = store.isSidebarOpen();
                store.toggleSidebar();
                tick();
                expect(store.isSidebarOpen()).toBe(!initialState);
            }));
        });
    });
});
