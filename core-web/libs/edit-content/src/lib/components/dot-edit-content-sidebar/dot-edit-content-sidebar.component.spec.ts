import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@ngneat/spectator/jest';
import { patchState, WritableStateSource } from '@ngrx/signals';
import { MockComponent } from 'ng-mocks';
import { NEVER, of, Subject } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { fakeAsync, tick } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';

import { ConfirmationService, MessageService } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';
import { Tabs, TabsModule } from 'primeng/tabs';

import {
    DotContentletService,
    DotContentTypeService,
    DotCurrentUserService,
    DotHttpErrorManagerService,
    DotLanguagesService,
    DotMessageService,
    DotPropertiesService,
    DotSiteService,
    DotSystemConfigService,
    DotVersionableService,
    DotWorkflowActionsFireService,
    DotWorkflowsActionsService,
    DotWorkflowService
} from '@dotcms/data-access';
import {
    DotCMSWorkflowAction,
    DotContentletCanLock,
    DotContentletDepths
} from '@dotcms/dotcms-models';
import { DotWorkflowActionsComponent } from '@dotcms/ui';
import {
    createFakeContentlet,
    createFakeLanguage,
    MOCK_SINGLE_WORKFLOW_ACTIONS,
    mockWorkflowsActions
} from '@dotcms/utils-testing';

import { DotEditContentSidebarActivitiesComponent } from './components/dot-edit-content-sidebar-activities/dot-edit-content-sidebar-activities.component';
import { DotEditContentSidebarHistoryComponent } from './components/dot-edit-content-sidebar-history/dot-edit-content-sidebar-history.component';
import { DotEditContentSidebarInformationComponent } from './components/dot-edit-content-sidebar-information/dot-edit-content-sidebar-information.component';
import { DotEditContentSidebarLocalesComponent } from './components/dot-edit-content-sidebar-locales/dot-edit-content-sidebar-locales.component';
import { DotEditContentSidebarSectionComponent } from './components/dot-edit-content-sidebar-section/dot-edit-content-sidebar-section.component';
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
            mockProvider(DotPropertiesService, {
                getFeatureFlagWithDefault: jest.fn().mockReturnValue(of(false))
            }),
            {
                provide: DialogService,
                useValue: {
                    open: jest.fn().mockReturnValue({
                        onClose: new Subject<void>(),
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
                        }),
                    isPortletInMenu: jest.fn().mockReturnValue(of(false))
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
            isBetaMessageVisible: true,
            localeSelectorTab: 'all'
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

        it('should render PrimeNG Tabs', () => {
            const tabs = spectator.query(Tabs);
            expect(tabs).toBeTruthy();
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

            it('should render DotEditContentSidebarLocalesComponent when locale data is available', () => {
                const mockLocale = createFakeLanguage();
                patchState(store as unknown as WritableStateSource<object>, {
                    systemDefaultLocale: mockLocale,
                    currentLocale: mockLocale
                });
                spectator.detectChanges();
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

        it('should render information section with data-testId when on info tab', () => {
            expect(store.activeSidebarTab()).toBe(0);
            const informationElement = spectator.query(byTestId('information'));
            expect(informationElement).toBeTruthy();
        });

        it('should render workflow section with data-testId when on info tab', () => {
            const workflowElement = spectator.query(byTestId('workflow'));
            expect(workflowElement).toBeTruthy();
        });

        it('should render locales section with data-testId when on info tab and locale data is available', () => {
            const mockLocale = createFakeLanguage();
            patchState(store as unknown as WritableStateSource<object>, {
                systemDefaultLocale: mockLocale,
                currentLocale: mockLocale
            });
            spectator.detectChanges();
            const localesElement = spectator.query(byTestId('locales'));
            expect(localesElement).toBeTruthy();
        });
    });

    describe('Tabs', () => {
        it('should render the first tab as the Actions tab with the bolt icon', () => {
            const messageService = spectator.inject(DotMessageService);
            const getSpy = jest.spyOn(messageService, 'get');

            const tabView = spectator.query(byTestId('sidebar-tabs'));
            const tabs = tabView.querySelectorAll('[role="tab"]');

            // Only the three remaining tabs (actions, history, comments)
            expect(tabs.length).toBe(3);

            // The first tab swapped the old info-circle icon for the new bolt icon
            expect(tabs[0].querySelector('i.pi.pi-bolt')).toBeTruthy();
            expect(tabs[0].querySelector('i.pi.pi-info-circle')).toBeFalsy();

            // The old "information" tooltip key is no longer requested
            expect(getSpy).not.toHaveBeenCalledWith('edit.content.sidebar.tab.information');
        });

        it('should NOT render a Settings tab', () => {
            const tabView = spectator.query(byTestId('sidebar-tabs'));
            const tabs = tabView.querySelectorAll('[role="tab"]');
            expect(tabs.length).toBe(3);
            expect(tabs[0].querySelector('i.pi.pi-cog')).toBeFalsy();
        });

        it('should NOT render the permissions or rules components', () => {
            expect(spectator.query(byTestId('permissions'))).toBeFalsy();
            expect(spectator.query(byTestId('rules'))).toBeFalsy();
        });
    });

    describe('Actions tab content', () => {
        beforeEach(fakeAsync(() => {
            const dotContentTypeService = spectator.inject(DotContentTypeService);
            const workflowActionsService = spectator.inject(DotWorkflowsActionsService);
            const dotWorkflowService = spectator.inject(DotWorkflowService);
            const dotEditContentService = spectator.inject(DotEditContentService);

            const mockContentlet = createFakeContentlet({
                inode: '123',
                contentType: 'testContentType',
                identifier: '123-456',
                title: 'Test Content'
            });

            dotEditContentService.getContentById.mockReturnValue(of(mockContentlet));
            dotContentTypeService.getContentTypeWithRender.mockReturnValue(of(CONTENT_TYPE_MOCK));
            // Flat actions for this inode keyed under the single scheme so that
            // showWorkflowActions/getActions resolve to a non-empty list.
            workflowActionsService.getByInode.mockReturnValue(of(mockWorkflowsActions));
            workflowActionsService.getWorkFlowActions.mockReturnValue(
                of(MOCK_SINGLE_WORKFLOW_ACTIONS)
            );
            dotWorkflowService.getWorkflowStatus.mockReturnValue(of(MOCK_WORKFLOW_STATUS));
            dotContentletService.canLock.mockReturnValue(
                of({ locked: false, canLock: true } as DotContentletCanLock)
            );

            store.initializeExistingContent({
                inode: '123',
                depth: DotContentletDepths.TWO
            });
            tick();
            spectator.detectChanges();
        }));

        describe('lock button', () => {
            it('should render the lock button when the content can be locked', () => {
                expect(store.canLock()).toBe(true);
                expect(spectator.query(byTestId('sidebar-lock-button'))).toBeTruthy();
            });

            it('should call store.lockContent when clicking the button on unlocked content', () => {
                const lockSpy = jest.spyOn(store, 'lockContent').mockImplementation();
                jest.spyOn(store, 'isContentLocked').mockReturnValue(false);
                spectator.detectChanges();

                spectator.click(byTestId('sidebar-lock-button'));

                expect(lockSpy).toHaveBeenCalled();
            });

            it('should call store.unlockContent when clicking the button on locked content', () => {
                const unlockSpy = jest.spyOn(store, 'unlockContent').mockImplementation();
                jest.spyOn(store, 'isContentLocked').mockReturnValue(true);
                spectator.detectChanges();

                spectator.click(byTestId('sidebar-lock-button'));

                expect(unlockSpy).toHaveBeenCalled();
            });

            it('should put the store in a loading state when the lock button is clicked', () => {
                // Use a request that never resolves so the store stays in the loading
                // state after the click; let the real lockContent method run (no spy).
                jest.spyOn(store, 'isContentLocked').mockReturnValue(false);
                dotContentletService.lockContent.mockReturnValue(NEVER);
                spectator.detectChanges();

                expect(store.isLocking()).toBe(false);

                spectator.click(byTestId('sidebar-lock-button'));

                expect(store.isLocking()).toBe(true);
            });

            it('should confirm before releasing a lock held by another user', () => {
                const confirmationService = spectator.inject(ConfirmationService, true);
                const confirmSpy = jest.spyOn(confirmationService, 'confirm');
                const unlockSpy = jest.spyOn(store, 'unlockContent').mockImplementation();
                jest.spyOn(store, 'isLockedByAnotherUser').mockReturnValue(true);
                jest.spyOn(store, 'lockedByName').mockReturnValue('Anna García');
                spectator.detectChanges();

                spectator.click(byTestId('sidebar-lock-button'));

                // A confirmation is requested and the lock is NOT released yet.
                expect(confirmSpy).toHaveBeenCalled();
                expect(unlockSpy).not.toHaveBeenCalled();

                // Accepting the confirmation releases (steals) the lock.
                confirmSpy.mock.calls[0][0].accept?.();
                expect(unlockSpy).toHaveBeenCalled();
            });
        });

        describe('workflow actions', () => {
            it('should render the dot-workflow-actions component when there are actions', () => {
                expect(store.showWorkflowActions()).toBe(true);
                expect(spectator.query(byTestId('sidebar-workflow-actions'))).toBeTruthy();
            });

            it('should emit workflowActionFired when the actions component fires an action', () => {
                const emitSpy = jest.spyOn(spectator.component.workflowActionFired, 'emit');
                const actionsComponent = spectator.query(DotWorkflowActionsComponent);
                const action = { id: 'action-1' } as DotCMSWorkflowAction;

                actionsComponent.actionFired.emit(action);

                expect(emitSpy).toHaveBeenCalledWith(action);
            });
        });

        describe('sections', () => {
            it('should carry the expected persistence keys on the three sections', () => {
                const sections = spectator.queryAll(DotEditContentSidebarSectionComponent);
                const keys = sections.map((section) => section.key());

                expect(keys).toEqual(['actions.locales', 'actions.workflow', 'actions.details']);
            });
        });
    });

    describe('Tabs Behavior', () => {
        beforeEach(fakeAsync(() => {
            const dotContentTypeService = spectator.inject(DotContentTypeService);
            const workflowActionsService = spectator.inject(DotWorkflowsActionsService);
            const dotWorkflowService = spectator.inject(DotWorkflowService);
            const dotEditContentService = spectator.inject(DotEditContentService);

            const mockContentlet = createFakeContentlet({
                inode: '123',
                contentType: 'testContentType',
                identifier: '123-456',
                title: 'Test Content'
            });

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

            store.initializeExistingContent({
                inode: '123',
                depth: DotContentletDepths.TWO
            });
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

        describe('onActiveIndexChange', () => {
            it('should call setActiveSidebarTab with index 0 when first tab is selected', fakeAsync(() => {
                store.setActiveSidebarTab(1);
                tick();
                const storeSpy = jest.spyOn(store, 'setActiveSidebarTab');
                spectator.component.onActiveIndexChange(0);
                expect(storeSpy).toHaveBeenCalledWith(0);
            }));

            it('should call setActiveSidebarTab with index 1 when history tab is selected', fakeAsync(() => {
                const storeSpy = jest.spyOn(store, 'setActiveSidebarTab');
                spectator.component.onActiveIndexChange(1);
                expect(storeSpy).toHaveBeenCalledWith(1);
            }));

            it('should call setActiveSidebarTab with the exact index from the event', fakeAsync(() => {
                const storeSpy = jest.spyOn(store, 'setActiveSidebarTab');
                spectator.component.onActiveIndexChange(2);
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

            it('should call store.fireWorkflowAction when fireResetWorkflowAction (reset path) is invoked', fakeAsync(() => {
                const storeSpy = jest.spyOn(store, 'fireWorkflowAction').mockImplementation();

                spectator.component.fireResetWorkflowAction('reset-action-id');

                expect(storeSpy).toHaveBeenCalledWith(
                    expect.objectContaining({ actionId: 'reset-action-id' })
                );
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
            it('should show information section when on info tab (tab 0)', fakeAsync(() => {
                expect(store.activeSidebarTab()).toBe(0);
                spectator.detectChanges();
                tick();
                const informationElement = spectator.query(byTestId('information'));
                expect(informationElement).toBeTruthy();
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
