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
import { TabView, TabViewChangeEvent } from 'primeng/tabview';

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
import { DotEditContentSidebarPermissionsComponent } from './components/dot-edit-content-sidebar-permissions/dot-edit-content-sidebar-permissions.component';
import { DotEditContentSidebarWorkflowComponent } from './components/dot-edit-content-sidebar-workflow/dot-edit-content-sidebar-workflow.component';
import { DotEditContentSidebarComponent } from './dot-edit-content-sidebar.component';

import { Activity } from '../../models/dot-edit-content.model';
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
            DotEditContentSidebarHistoryComponent,
            DotEditContentSidebarPermissionsComponent
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
            {
                provide: DialogService,
                useValue: {
                    open: jest.fn().mockReturnValue({
                        onClose: { subscribe: jest.fn() },
                        close: jest.fn()
                    })
                }
            },
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

        dotEditContentService.createActivity.mockReturnValue(
            of({
                commentDescription: '',
                createdDate: 0,
                email: '',
                postedBy: '',
                roleId: '',
                taskId: '',
                type: 'comment'
            } satisfies Activity)
        );
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

    describe('Elements by data-testId', () => {
        it('should have sidebar-tabs element', () => {
            expect(spectator.query(byTestId('sidebar-tabs'))).toBeTruthy();
        });

        it('should have toggle-button element', () => {
            expect(spectator.query(byTestId('toggle-button'))).toBeTruthy();
        });

        it('should render information section with data-testId when on info tab', () => {
            expect(store.activeSidebarTab()).toBe(0);
            const informationElement = spectator.query(byTestId('information'));
            expect(informationElement).toBeTruthy();
        });

        it('should render workflow section with data-testId when on info tab', () => {
            const workflowElement = spectator.query(byTestId('workflow'));
            expect(workflowElement).toBeTruthy();
        });

        it('should render locales section with data-testId when on info tab', () => {
            const localesElement = spectator.query(byTestId('locales'));
            expect(localesElement).toBeTruthy();
        });
    });

    describe('Sidebar Controls', () => {
        it('should render toggle button', () => {
            const toggleButton = spectator.query(byTestId('toggle-button'));
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
            dotContentTypeService.getContentTypeWithRender.mockReturnValue(of(CONTENT_TYPE_MOCK));
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

            it('should switch to permissions tab and render permissions content when clicking permissions tab', fakeAsync(() => {
                spectator.detectChanges();
                tick();

                const tabView = spectator.query(byTestId('sidebar-tabs'));
                expect(tabView).toBeTruthy();

                const tabs = tabView.querySelectorAll('[role="tab"]');
                expect(tabs.length).toBeGreaterThan(3);

                const permissionsTabLink = tabs[3]; // Permissions is the fourth tab
                expect(permissionsTabLink).toBeTruthy();

                const storeSpy = jest.spyOn(store, 'setActiveSidebarTab');

                // Start on info tab (0), then click permissions tab
                expect(store.activeSidebarTab()).toBe(0);
                spectator.click(permissionsTabLink);
                tick();
                spectator.detectChanges();

                expect(storeSpy).toHaveBeenCalledWith(3);
                expect(store.activeSidebarTab()).toBe(3);

                const permissionsComponent = spectator.query(
                    DotEditContentSidebarPermissionsComponent
                );
                expect(permissionsComponent).toBeTruthy();
            }));

            it('should open permissions dialog when clicking permissions card in permissions tab', fakeAsync(() => {
                spectator.detectChanges();
                tick();

                // Switch to permissions tab first
                store.setActiveSidebarTab(3);
                tick();
                spectator.detectChanges();

                const dialogService = spectator.inject(DialogService);
                const openSpy = jest.spyOn(dialogService, 'open');

                const permissionsCard = spectator.query(byTestId('permissions-card'));
                expect(permissionsCard).toBeTruthy();
                spectator.click(permissionsCard);
                tick();

                expect(openSpy).toHaveBeenCalled();
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

        describe('onActiveIndexChange', () => {
            it('should call setActiveSidebarTab with index 0 when first tab is selected', fakeAsync(() => {
                store.setActiveSidebarTab(1);
                tick();
                const storeSpy = jest.spyOn(store, 'setActiveSidebarTab');
                spectator.component.onActiveIndexChange({ index: 0 } as TabViewChangeEvent);
                expect(storeSpy).toHaveBeenCalledWith(0);
            }));

            it('should call setActiveSidebarTab with index 4 when last tab (permissions) is selected', fakeAsync(() => {
                const storeSpy = jest.spyOn(store, 'setActiveSidebarTab');
                spectator.component.onActiveIndexChange({ index: 4 } as TabViewChangeEvent);
                expect(storeSpy).toHaveBeenCalledWith(4);
            }));

            it('should call setActiveSidebarTab with the exact index from the event', fakeAsync(() => {
                const storeSpy = jest.spyOn(store, 'setActiveSidebarTab');
                spectator.component.onActiveIndexChange({ index: 2 } as TabViewChangeEvent);
                expect(storeSpy).toHaveBeenCalledWith(2);
            }));
        });

        describe('Event Handlers - Success', () => {
            it('should call store.addComment when onCommentSubmitted is called with a comment', fakeAsync(() => {
                const storeSpy = jest.spyOn(store, 'addComment');
                const component = spectator.component;
                Object.defineProperty(component, '$identifier', {
                    value: jest.fn().mockReturnValue('test-identifier'),
                    writable: true
                });
                component.onCommentSubmitted('My comment');
                expect(storeSpy).toHaveBeenCalledWith({
                    comment: 'My comment',
                    identifier: 'test-identifier'
                });
            }));
        });

        describe('Event Handlers - Failure and Edge Cases', () => {
            it('should NOT call loadVersions when onVersionsPageChange is called and identifier is undefined', fakeAsync(() => {
                const storeSpy = jest.spyOn(store, 'loadVersions');
                const component = spectator.component;
                Object.defineProperty(component, '$identifier', {
                    value: jest.fn().mockReturnValue(undefined),
                    writable: true
                });
                component.onVersionsPageChange(1);
                expect(storeSpy).not.toHaveBeenCalled();
            }));

            it('should NOT call loadVersions when onVersionsPageChange is called and identifier is empty string', fakeAsync(() => {
                const storeSpy = jest.spyOn(store, 'loadVersions');
                const component = spectator.component;
                Object.defineProperty(component, '$identifier', {
                    value: jest.fn().mockReturnValue(''),
                    writable: true
                });
                component.onVersionsPageChange(1);
                expect(storeSpy).not.toHaveBeenCalled();
            }));

            it('should NOT call loadPushPublishHistory when onPushPublishPageChange is called and identifier is undefined', fakeAsync(() => {
                const storeSpy = jest.spyOn(store, 'loadPushPublishHistory');
                const component = spectator.component;
                Object.defineProperty(component, '$identifier', {
                    value: jest.fn().mockReturnValue(undefined),
                    writable: true
                });
                component.onPushPublishPageChange(2);
                expect(storeSpy).not.toHaveBeenCalled();
            }));

            it('should NOT call deletePushPublishHistory when onDeletePushPublishHistory is called and identifier is undefined', fakeAsync(() => {
                const storeSpy = jest.spyOn(store, 'deletePushPublishHistory');
                const component = spectator.component;
                Object.defineProperty(component, '$identifier', {
                    value: jest.fn().mockReturnValue(undefined),
                    writable: true
                });
                component.onDeletePushPublishHistory();
                expect(storeSpy).not.toHaveBeenCalled();
            }));

            it('should NOT call deletePushPublishHistory when onDeletePushPublishHistory is called and identifier is null', fakeAsync(() => {
                const storeSpy = jest.spyOn(store, 'deletePushPublishHistory');
                const component = spectator.component;
                Object.defineProperty(component, '$identifier', {
                    value: jest.fn().mockReturnValue(null),
                    writable: true
                });
                component.onDeletePushPublishHistory();
                expect(storeSpy).not.toHaveBeenCalled();
            }));

            it('should still call addComment when onCommentSubmitted is called even if identifier is undefined', fakeAsync(() => {
                const storeSpy = jest.spyOn(store, 'addComment');
                const component = spectator.component;
                Object.defineProperty(component, '$identifier', {
                    value: jest.fn().mockReturnValue(undefined),
                    writable: true
                });
                component.onCommentSubmitted('Comment with no identifier');
                expect(storeSpy).toHaveBeenCalledWith({
                    comment: 'Comment with no identifier',
                    identifier: undefined
                });
            }));
        });

        describe('Edge Cases - Tab Content Visibility', () => {
            it('should NOT render permissions-card when on info tab (tab 0)', fakeAsync(() => {
                expect(store.activeSidebarTab()).toBe(0);
                spectator.detectChanges();
                tick();
                const permissionsCard = spectator.query(byTestId('permissions-card'));
                expect(permissionsCard).toBeFalsy();
            }));

            it('should render permissions-card only when permissions tab (tab 3) is active', fakeAsync(() => {
                store.setActiveSidebarTab(3);
                tick();
                spectator.detectChanges();
                const permissionsCard = spectator.query(byTestId('permissions-card'));
                expect(permissionsCard).toBeTruthy();
            }));

            it('should render history section with data-testId when history tab is active', fakeAsync(() => {
                store.setActiveSidebarTab(1);
                tick();
                spectator.detectChanges();
                const historyElement = spectator.query(byTestId('history'));
                expect(historyElement).toBeTruthy();
            }));

            it('should render activities section with data-testId when activities tab is active', fakeAsync(() => {
                store.setActiveSidebarTab(2);
                tick();
                spectator.detectChanges();
                const activitiesElement = spectator.query(byTestId('activities'));
                expect(activitiesElement).toBeTruthy();
            }));
        });
    });
});
