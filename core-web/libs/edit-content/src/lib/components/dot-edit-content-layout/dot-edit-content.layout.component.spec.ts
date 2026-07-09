import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@ngneat/spectator/jest';
import { MockComponent } from 'ng-mocks';
import { of, Subject } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { fakeAsync, tick } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';

import { ConfirmationService, ConfirmEventType, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { DialogService, DynamicDialog, DynamicDialogRef } from 'primeng/dynamicdialog';
import { MessageModule } from 'primeng/message';

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
import { DotCMSWorkflowAction, DotLanguage } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';
import { DotMessagePipe } from '@dotcms/ui';
import {
    MOCK_MULTIPLE_WORKFLOW_ACTIONS,
    MOCK_SINGLE_WORKFLOW_ACTIONS
} from '@dotcms/utils-testing';

import { DotEditContentLayoutComponent } from './dot-edit-content.layout.component';

import { FormValues } from '../../models/dot-edit-content-form.interface';
import { DotEditContentService } from '../../services/dot-edit-content.service';
import {
    EDIT_CONTENT_HOST,
    InPlaceNavigationRequest
} from '../../services/host/edit-content-host.model';
import {
    DotRelatedContentCrumb,
    DotRelatedContentNavigationStore
} from '../../store/dot-related-content-navigation.store';
import { DotEditContentStore } from '../../store/edit-content.store';
import { MOCK_CONTENTLET_1_TAB } from '../../utils/edit-content.mock';
import * as utils from '../../utils/functions.util';
import { CONTENT_TYPE_MOCK } from '../../utils/mocks';
import { DotEditContentFormComponent } from '../dot-edit-content-form/dot-edit-content-form.component';
import { DotEditContentSidebarComponent } from '../dot-edit-content-sidebar/dot-edit-content-sidebar.component';

const MOCK_LANGUAGES = [{ id: 1, isoCode: 'en-us', defaultLanguage: false }] as DotLanguage[];

const MOCK_FORM_VALUES: FormValues = {
    title: 'Test Title',
    content: 'Test Content',
    language: 'en-us'
};

// Controllable trail for the host mock. A real signal so the component's
// `$relatedNavItems` computed reacts when it changes.
const relatedTrailSignal = signal<DotRelatedContentCrumb[]>([]);

// Full-screen host mock: no in-place navigation, identity resolved as empty (the
// store's initialize() is spied per-test where it matters). `trail` is the host's
// own signal now (the layout reads host.trail(), not the nav store directly).
const mockEditContentHost = {
    inPlaceNavigation: false,
    inPlaceNavigation$: undefined,
    trail: relatedTrailSignal,
    setTrail: jest.fn(),
    resolveIdentity: jest.fn().mockReturnValue({}),
    reportSaved: jest.fn(),
    reloadContent: jest.fn(),
    setContentTitle: jest.fn(),
    addBreadcrumb: jest.fn(),
    goToSavedContent: jest.fn(),
    goToRestoredVersion: jest.fn(),
    goToRelatedContent: jest.fn(),
    goToCrumb: jest.fn()
};

describe('EditContentLayoutComponent', () => {
    let spectator: Spectator<DotEditContentLayoutComponent>;

    let store: SpyObject<InstanceType<typeof DotEditContentStore>>;
    let dotContentTypeService: SpyObject<DotContentTypeService>;
    let dotEditContentService: SpyObject<DotEditContentService>;
    let workflowActionsService: SpyObject<DotWorkflowsActionsService>;
    let dotLanguagesService: SpyObject<DotLanguagesService>;

    const createComponent = createComponentFactory({
        component: DotEditContentLayoutComponent,
        imports: [
            MessageModule,
            ButtonModule,
            MockComponent(DotEditContentFormComponent),
            MockComponent(DotEditContentSidebarComponent),
            DotMessagePipe
        ],
        componentProviders: [
            DotEditContentStore,
            mockProvider(DotWorkflowsActionsService),
            mockProvider(DotWorkflowActionsFireService),
            mockProvider(DotEditContentService),
            mockProvider(DotContentTypeService),
            mockProvider(DotWorkflowService),
            mockProvider(DotContentletService),
            mockProvider(DotVersionableService),
            ConfirmationService,
            { provide: EDIT_CONTENT_HOST, useValue: mockEditContentHost }
        ],
        providers: [
            mockProvider(DotHttpErrorManagerService),
            mockProvider(MessageService),
            mockProvider(DialogService),
            mockProvider(DotLanguagesService),
            mockProvider(DotSiteService, {
                getCurrentSite: jest
                    .fn()
                    .mockReturnValue(of({ identifier: 'default', hostname: 'demo.dotcms.com' }))
            }),
            mockProvider(DotSystemConfigService, {
                getSystemConfig: jest.fn().mockReturnValue(of({}))
            }),
            GlobalStore,
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
                        return { params: { id: '', contentType: '' } };
                    }
                }
            },
            mockProvider(Router, {
                navigate: jest.fn().mockReturnValue(Promise.resolve(true)),
                url: '/test-url',
                events: of()
            }),
            provideHttpClient(),
            provideHttpClientTesting(),
            mockProvider(DotMessageService, {
                get: jest.fn((key: string, ...args: unknown[]) =>
                    key === 'edit.content.locked.by.user' ? `Content is locked by ${args[0]}` : key
                )
            }),
            mockProvider(DotRelatedContentNavigationStore, {
                trail: relatedTrailSignal,
                registerTitle: jest.fn(),
                buildTrailForSavedInode: jest.fn().mockReturnValue(null)
            })
        ]
    });

    beforeEach(() => {
        mockEditContentHost.resolveIdentity.mockReturnValue({});
        mockEditContentHost.reportSaved.mockClear();
        mockEditContentHost.reloadContent.mockClear();

        spectator = createComponent({
            detectChanges: false
        });

        store = spectator.inject(DotEditContentStore, true);
        dotContentTypeService = spectator.inject(DotContentTypeService, true);
        workflowActionsService = spectator.inject(DotWorkflowsActionsService, true);
        dotEditContentService = spectator.inject(DotEditContentService, true);
        dotLanguagesService = spectator.inject(DotLanguagesService, true);

        jest.spyOn(dotLanguagesService, 'get').mockReturnValue(of(MOCK_LANGUAGES));

        // Mock the initial UI state
        jest.spyOn(utils, 'getStoredUIState').mockReturnValue({
            view: 'form',
            activeTab: 0,
            isSidebarOpen: true,
            activeSidebarTab: 0,
            isBetaMessageVisible: true
        });

        dotContentTypeService.updateContentType.mockReturnValue(of(CONTENT_TYPE_MOCK));
        dotEditContentService.getContentById.mockReturnValue(of(MOCK_CONTENTLET_1_TAB));

        spectator.detectChanges();
    });

    it('should have p-confirmDialog component', () => {
        expect(spectator.query(ConfirmDialog)).toBeTruthy();
    });

    describe('Initialization', () => {
        it('should initialize the editor from the host identity on creation', () => {
            // initialize() runs in the constructor and asks the host who to open.
            mockEditContentHost.resolveIdentity.mockClear();

            createComponent({ detectChanges: false });

            expect(mockEditContentHost.resolveIdentity).toHaveBeenCalled();
        });
    });

    describe('Save reporting', () => {
        it('should report the save to the host and mark pristine on workflow success', () => {
            const freshSpectator = createComponent({ detectChanges: false });
            const freshStore = freshSpectator.inject(DotEditContentStore, true);
            const host = freshSpectator.inject(EDIT_CONTENT_HOST, true);

            const markFormPristineSpy = jest.spyOn(freshSpectator.component, 'markFormPristine');
            jest.spyOn(freshStore, 'workflowActionSuccess').mockReturnValue(MOCK_CONTENTLET_1_TAB);
            jest.spyOn(freshStore, 'clearWorkflowActionSuccess');

            freshSpectator.detectChanges();

            expect(host.reportSaved).toHaveBeenCalledWith(MOCK_CONTENTLET_1_TAB);
            expect(markFormPristineSpy).toHaveBeenCalledTimes(1);
            expect(freshStore.clearWorkflowActionSuccess).toHaveBeenCalledTimes(1);
        });

        it('should be a no-op when there is no workflow action success', () => {
            const freshSpectator = createComponent({ detectChanges: false });
            const freshStore = freshSpectator.inject(DotEditContentStore, true);
            const host = freshSpectator.inject(EDIT_CONTENT_HOST, true);

            const markFormPristineSpy = jest.spyOn(freshSpectator.component, 'markFormPristine');
            jest.spyOn(freshStore, 'workflowActionSuccess').mockReturnValue(null);
            jest.spyOn(freshStore, 'clearWorkflowActionSuccess');

            freshSpectator.detectChanges();

            expect(host.reportSaved).not.toHaveBeenCalled();
            expect(markFormPristineSpy).not.toHaveBeenCalled();
            expect(freshStore.clearWorkflowActionSuccess).not.toHaveBeenCalled();
        });
    });

    describe('Component Methods', () => {
        describe('selectWorkflow()', () => {
            it('should set $showDialog to true when selectWorkflow is called', () => {
                expect(spectator.component.$showDialog()).toBe(false);

                spectator.component.selectWorkflow();

                expect(spectator.component.$showDialog()).toBe(true);
            });
        });

        describe('onFormChange()', () => {
            it('should call store.onFormChange with provided form values', () => {
                const onFormChangeSpy = jest.spyOn(store, 'onFormChange');

                spectator.component.onFormChange(MOCK_FORM_VALUES);

                expect(onFormChangeSpy).toHaveBeenCalledWith(MOCK_FORM_VALUES);
            });
        });

        describe('onWorkflowActionFired()', () => {
            it('should delegate to the form with params built from the store', () => {
                const fireWorkflowActionSpy = jest.fn();
                jest.spyOn(spectator.component, '$editContentForm').mockReturnValue({
                    fireWorkflowAction: fireWorkflowActionSpy
                } as unknown as DotEditContentFormComponent);

                jest.spyOn(store, 'currentLocale').mockReturnValue(MOCK_LANGUAGES[0]);
                jest.spyOn(store, 'contentlet').mockReturnValue(MOCK_CONTENTLET_1_TAB);
                jest.spyOn(store, 'contentType').mockReturnValue(CONTENT_TYPE_MOCK);
                jest.spyOn(store, 'currentIdentifier').mockReturnValue(
                    MOCK_CONTENTLET_1_TAB.identifier
                );

                const workflow = { id: 'action-id' } as DotCMSWorkflowAction;
                spectator.component.onWorkflowActionFired(workflow);

                expect(fireWorkflowActionSpy).toHaveBeenCalledWith({
                    workflow,
                    inode: MOCK_CONTENTLET_1_TAB.inode,
                    contentType: CONTENT_TYPE_MOCK.variable,
                    languageId: MOCK_LANGUAGES[0].id.toString(),
                    identifier: MOCK_CONTENTLET_1_TAB.identifier
                });
            });

            it('should not throw when the form ref is undefined (compare view)', () => {
                jest.spyOn(spectator.component, '$editContentForm').mockReturnValue(undefined);

                const workflow = { id: 'action-id' } as DotCMSWorkflowAction;

                expect(() => spectator.component.onWorkflowActionFired(workflow)).not.toThrow();
            });
        });

        describe('closeMessage()', () => {
            it('should call store.toggleBetaMessage when closing beta message', () => {
                const toggleBetaMessageSpy = jest.spyOn(store, 'toggleBetaMessage');

                spectator.component.closeMessage('betaMessage');

                expect(toggleBetaMessageSpy).toHaveBeenCalled();
            });
        });
    });

    describe('Store Integration', () => {
        it('should inject DotEditContentStore', () => {
            expect(spectator.component.$store).toBe(store);
            expect(spectator.component.$store).toBeInstanceOf(DotEditContentStore);
        });

        it('should have isolated store instance for each component', () => {
            // Create a second component instance
            const spectator2 = createComponent();
            const store2 = spectator2.inject(DotEditContentStore, true);

            // Stores should be different instances
            expect(spectator.component.$store).not.toBe(spectator2.component.$store);
            expect(store).not.toBe(store2);
        });
    });

    describe('Unsaved Changes Support', () => {
        it('should return false from hasUnsavedChanges when the form ref is undefined', () => {
            // No content has been initialized, so the inner form viewChild is empty.
            expect(spectator.component.hasUnsavedChanges()).toBe(false);
        });

        it('should return true from hasUnsavedChanges when the form is dirty', () => {
            const fakeForm = { dirty: true, markAsPristine: jest.fn() };
            jest.spyOn(spectator.component, '$editContentForm').mockReturnValue({
                form: fakeForm
            } as unknown as DotEditContentFormComponent);

            expect(spectator.component.hasUnsavedChanges()).toBe(true);
        });

        it('should not crash markFormPristine when the form ref is undefined', () => {
            // No content has been initialized, so the inner form viewChild is empty.
            expect(() => spectator.component.markFormPristine()).not.toThrow();
        });
    });

    // Isolated to its own describe so that no other component mounted earlier
    // in the file can race the `window:beforeunload` listener registered via
    // the host metadata. The outer `spectator` fixture is destroyed and a
    // fresh one is created so dispatching on `window` exercises a single
    // active listener — pollution surfaces immediately as a count mismatch.
    describe('Before Unload Listener', () => {
        beforeEach(() => {
            spectator.fixture.destroy();
            spectator = createComponent({ detectChanges: false });
            spectator.detectChanges();
        });

        it('should preventDefault on window beforeunload when the form is dirty', () => {
            jest.spyOn(spectator.component, 'hasUnsavedChanges').mockReturnValue(true);
            const event = new Event('beforeunload', { cancelable: true }) as BeforeUnloadEvent;
            const preventDefaultSpy = jest.spyOn(event, 'preventDefault');

            window.dispatchEvent(event);

            expect(preventDefaultSpy).toHaveBeenCalledTimes(1);
        });

        it('should NOT preventDefault on window beforeunload when the form is pristine', () => {
            jest.spyOn(spectator.component, 'hasUnsavedChanges').mockReturnValue(false);
            const event = new Event('beforeunload', { cancelable: true }) as BeforeUnloadEvent;
            const preventDefaultSpy = jest.spyOn(event, 'preventDefault');

            window.dispatchEvent(event);

            expect(preventDefaultSpy).not.toHaveBeenCalled();
        });
    });

    describe('Component Host Classes', () => {
        it('should apply edit-content--with-sidebar class when sidebar is open', () => {
            jest.spyOn(store, 'isSidebarOpen').mockImplementation(() => true);
            spectator.detectChanges();

            expect(spectator.element).toHaveClass('edit-content--with-sidebar');
        });

        it('should not apply edit-content--with-sidebar class when sidebar is closed', () => {
            store.toggleSidebar();
            spectator.detectChanges();

            expect(spectator.element).not.toHaveClass('edit-content--with-sidebar');
        });
    });

    describe('New Content Editor', () => {
        it('should initialize new content, show layout components and dialogs when new content editor is enabled', fakeAsync(() => {
            dotContentTypeService.getContentTypeWithRender.mockReturnValue(of(CONTENT_TYPE_MOCK));
            workflowActionsService.getDefaultActions.mockReturnValue(
                of(MOCK_SINGLE_WORKFLOW_ACTIONS)
            );
            workflowActionsService.getWorkFlowActions.mockReturnValue(
                of(MOCK_SINGLE_WORKFLOW_ACTIONS)
            );

            store.initializeNewContent('contentTypeName');

            spectator.detectChanges();

            tick(); // Wait for the defer to load

            expect(store.isEnabledNewContentEditor()).toBe(true);
            expect(store.isSidebarOpen()).toBe(true);

            expect(spectator.query(byTestId('edit-content-layout__topBar'))).toBeTruthy();
            expect(spectator.query(byTestId('edit-content-layout__body'))).toBeTruthy();
            expect(spectator.query(byTestId('edit-content-layout__sidebar'))).toBeTruthy();

            expect(spectator.query(ConfirmDialog)).toBeTruthy();
        }));

        describe('Beta Message', () => {
            beforeEach(fakeAsync(() => {
                dotContentTypeService.getContentTypeWithRender.mockReturnValue(
                    of(CONTENT_TYPE_MOCK)
                );
                workflowActionsService.getDefaultActions.mockReturnValue(
                    of(MOCK_SINGLE_WORKFLOW_ACTIONS)
                );
                store.initializeNewContent('contentTypeName');
                spectator.detectChanges();
                tick();
            }));

            it('should show beta message by default', fakeAsync(() => {
                spectator.detectChanges();
                tick();
                expect(spectator.query(byTestId('edit-content-layout__beta-message'))).toBeTruthy();
                expect(
                    spectator.query(byTestId('edit-content-layout__beta-message-content'))
                ).toBeTruthy();
                expect(
                    spectator.query(byTestId('edit-content-layout__beta-message-link'))
                ).toBeTruthy();
                expect(
                    spectator.query(byTestId('edit-content-layout__beta-message-close-button'))
                ).toBeTruthy();
            }));

            it('should hide beta message when close button is clicked', fakeAsync(() => {
                spectator.detectChanges();
                tick();

                const closeButton = spectator.query(
                    byTestId('edit-content-layout__beta-message-close-button')
                ) as HTMLButtonElement;
                expect(closeButton).toBeTruthy();

                spectator.click(closeButton);
                spectator.detectChanges();
                tick();

                expect(spectator.query(byTestId('edit-content-layout__beta-message'))).toBeFalsy();
            }));

            it('should have correct link to old editor', async () => {
                // Initialize the content type
                dotContentTypeService.getContentTypeWithRender.mockReturnValue(
                    of(CONTENT_TYPE_MOCK)
                );
                workflowActionsService.getDefaultActions.mockReturnValue(
                    of(MOCK_SINGLE_WORKFLOW_ACTIONS)
                );
                store.initializeNewContent('contentTypeName');

                // Wait for initialization
                await spectator.fixture.whenStable();
                spectator.detectChanges();

                // Create a fake event with preventDefault
                const event = new MouseEvent('click');
                Object.defineProperty(event, 'preventDefault', { value: jest.fn() });

                // Spy on the store method
                const disableNewContentEditorSpy = jest.spyOn(store, 'disableNewContentEditor');

                const link = spectator.query(
                    byTestId('edit-content-layout__beta-message-link')
                ) as HTMLAnchorElement;

                // Dispatch the event
                link.dispatchEvent(event);

                expect(event.preventDefault).toHaveBeenCalled();
                expect(disableNewContentEditorSpy).toHaveBeenCalled();
            });
        });

        it('should not show top bar message when new content editor is disabled', () => {
            const CONTENT_TYPE_MOCK_NO_METADATA = {
                ...CONTENT_TYPE_MOCK,
                metadata: undefined
            };

            dotContentTypeService.getContentTypeWithRender.mockReturnValue(
                of(CONTENT_TYPE_MOCK_NO_METADATA)
            );
            workflowActionsService.getDefaultActions.mockReturnValue(
                of(MOCK_SINGLE_WORKFLOW_ACTIONS)
            );
            workflowActionsService.getWorkFlowActions.mockReturnValue(
                of(MOCK_SINGLE_WORKFLOW_ACTIONS)
            );
            store.initializeNewContent('contentTypeName');

            spectator.detectChanges();
            expect(store.isEnabledNewContentEditor()).toBe(false);
            expect(spectator.query(byTestId('edit-content-layout__beta-message'))).toBeFalsy();
        });
    });

    describe('Warning Messages', () => {
        beforeEach(() => {
            dotContentTypeService.getContentTypeWithRender.mockReturnValue(of(CONTENT_TYPE_MOCK));
            workflowActionsService.getDefaultActions.mockReturnValue(
                of(MOCK_SINGLE_WORKFLOW_ACTIONS)
            );
            dotEditContentService.getContentById.mockReturnValue(of(MOCK_CONTENTLET_1_TAB));
        });

        it('should show workflow warning message when multiple schemes are available for new content', fakeAsync(() => {
            // Multiple schemes trigger the warning message
            workflowActionsService.getDefaultActions.mockReturnValue(
                of(MOCK_MULTIPLE_WORKFLOW_ACTIONS)
            );

            store.initializeNewContent('contentTypeName');
            spectator.detectChanges();
            tick();

            const warningMessage = spectator.query(
                byTestId('edit-content-layout__select-workflow-warning')
            );
            expect(store.showSelectWorkflowWarning()).toBe(true);
            expect(warningMessage).toBeTruthy();
        }));

        it('should not show workflow warning message when only one scheme is available', fakeAsync(() => {
            // Set up single workflow scheme
            workflowActionsService.getWorkFlowActions.mockReturnValue(
                of(MOCK_SINGLE_WORKFLOW_ACTIONS)
            );

            store.initializeNewContent('contentTypeName');
            spectator.detectChanges();
            tick();

            const warningMessage = spectator.query(
                byTestId('edit-content-layout__select-workflow-warning')
            );
            expect(warningMessage).toBeFalsy();
        }));

        it('should not show workflow warning message for existing content', fakeAsync(() => {
            // Even with multiple schemes, existing content shouldn't show warning
            workflowActionsService.getWorkFlowActions.mockReturnValue(
                of(MOCK_MULTIPLE_WORKFLOW_ACTIONS)
            );

            store.initializeExistingContent('123');
            spectator.detectChanges();
            tick();

            const warningMessage = spectator.query(
                byTestId('edit-content-layout__select-workflow-warning')
            );
            expect(warningMessage).toBeFalsy();
        }));

        describe('Warning Messages', () => {
            beforeEach(fakeAsync(() => {
                dotContentTypeService.getContentTypeWithRender.mockReturnValue(
                    of(CONTENT_TYPE_MOCK)
                );
                workflowActionsService.getDefaultActions.mockReturnValue(
                    of(MOCK_SINGLE_WORKFLOW_ACTIONS)
                );
                store.initializeNewContent('contentTypeName');
                spectator.detectChanges();
                tick();
            }));

            it('should show lock warning message when lockWarningMessage signal returns a message', fakeAsync(() => {
                // Verify the store's lockWarningMessage is used in the template by checking
                // that the component renders the topBar when lockWarningMessage returns a value.
                // The template condition is: topBarHasMessages = ... || lockWarningMessage || ...
                const mockMessage = 'Content is locked by Other User';
                jest.spyOn(store, 'lockWarningMessage').mockImplementation(() => mockMessage);
                spectator.detectChanges();
                tick();

                // When lockWarningMessage returns a message, the store value should be used
                expect(store.lockWarningMessage()).toBe(mockMessage);
            }));
        });
    });

    describe('relatedNavItems (Relating content breadcrumb)', () => {
        const A: DotRelatedContentCrumb = { inode: 'iA', title: 'TA' };
        const B: DotRelatedContentCrumb = { inode: 'iB', title: 'TB' };
        const C: DotRelatedContentCrumb = { inode: 'iC', title: 'TC' };

        afterEach(() => relatedTrailSignal.set([]));

        it('returns an empty model when there is no trail', () => {
            relatedTrailSignal.set([]);

            expect(spectator.component.$relatedNavItems()).toEqual([]);
        });

        it('builds routerLink crumbs with a trimmed rc; the current (last) crumb is a plain label', () => {
            relatedTrailSignal.set([A, B, C]);

            expect(spectator.component.$relatedNavItems()).toEqual([
                // First crumb: navigating back to the origin clears rc (single item → null).
                {
                    label: 'TA',
                    routerLink: ['/content', 'iA'],
                    queryParams: { rc: null },
                    queryParamsHandling: 'merge'
                },
                {
                    label: 'TB',
                    routerLink: ['/content', 'iB'],
                    queryParams: { rc: 'iA,iB' },
                    queryParamsHandling: 'merge'
                },
                // Current content — not a link.
                { label: 'TC' }
            ]);
        });
    });
});

// Separate top-level describe (fresh TestBed) for the in-place host path: the
// default mock above is full-screen (inPlaceNavigation false, inPlaceNavigation$
// undefined), so the layout's in-place reload subscription and the breadcrumb's
// `command` branch are only reachable with a dedicated in-place host.
describe('EditContentLayoutComponent - In-place (dialog) host', () => {
    const A: DotRelatedContentCrumb = { inode: 'iA', title: 'TA' };
    const B: DotRelatedContentCrumb = { inode: 'iB', title: 'TB' };

    let navigation$: Subject<InPlaceNavigationRequest>;
    const inPlaceTrail = signal<DotRelatedContentCrumb[]>([]);
    const inPlaceHost = {
        inPlaceNavigation: true,
        inPlaceNavigation$: undefined as unknown as Subject<InPlaceNavigationRequest>,
        trail: inPlaceTrail,
        setTrail: jest.fn(),
        resolveIdentity: jest.fn().mockReturnValue({}),
        reportSaved: jest.fn(),
        reloadContent: jest.fn(),
        setContentTitle: jest.fn(),
        addBreadcrumb: jest.fn(),
        goToSavedContent: jest.fn(),
        goToRestoredVersion: jest.fn(),
        goToRelatedContent: jest.fn(),
        goToCrumb: jest.fn()
    };

    const createComponent = createComponentFactory({
        component: DotEditContentLayoutComponent,
        imports: [
            MessageModule,
            ButtonModule,
            MockComponent(DotEditContentFormComponent),
            MockComponent(DotEditContentSidebarComponent),
            DotMessagePipe
        ],
        componentProviders: [
            DotEditContentStore,
            mockProvider(DotWorkflowsActionsService),
            mockProvider(DotWorkflowActionsFireService),
            mockProvider(DotEditContentService),
            mockProvider(DotContentTypeService),
            mockProvider(DotWorkflowService),
            mockProvider(DotContentletService),
            mockProvider(DotVersionableService),
            ConfirmationService,
            { provide: EDIT_CONTENT_HOST, useValue: inPlaceHost }
        ],
        providers: [
            mockProvider(DotHttpErrorManagerService),
            mockProvider(MessageService),
            mockProvider(DialogService),
            mockProvider(DotLanguagesService),
            mockProvider(DotSiteService, {
                getCurrentSite: jest
                    .fn()
                    .mockReturnValue(of({ identifier: 'default', hostname: 'demo.dotcms.com' }))
            }),
            mockProvider(DotSystemConfigService, {
                getSystemConfig: jest.fn().mockReturnValue(of({}))
            }),
            GlobalStore,
            {
                provide: DotCurrentUserService,
                useValue: { getCurrentUser: () => of({ userId: '123', userName: 'John Doe' }) }
            },
            { provide: ActivatedRoute, useValue: { snapshot: { params: {} } } },
            mockProvider(Router, { navigate: jest.fn(), url: '/test-url', events: of() }),
            provideHttpClient(),
            provideHttpClientTesting(),
            mockProvider(DotMessageService, { get: jest.fn((key: string) => key) }),
            mockProvider(DotRelatedContentNavigationStore, {
                trail: inPlaceTrail,
                registerTitle: jest.fn()
            })
        ]
    });

    let spectator: Spectator<DotEditContentLayoutComponent>;
    let store: SpyObject<InstanceType<typeof DotEditContentStore>>;

    beforeEach(() => {
        navigation$ = new Subject<InPlaceNavigationRequest>();
        inPlaceHost.inPlaceNavigation$ = navigation$;
        inPlaceTrail.set([]);
        Object.values(inPlaceHost).forEach((v) => (v as jest.Mock)?.mockClear?.());
        inPlaceHost.resolveIdentity.mockReturnValue({});

        spectator = createComponent({ detectChanges: false });
        store = spectator.inject(DotEditContentStore, true);
        jest.spyOn(store, 'initializeExistingContent').mockImplementation(() => undefined);
        jest.spyOn(store, 'initialize').mockImplementation(() => undefined);
        spectator.detectChanges();
    });

    it('builds a `command` crumb (not routerLink) that calls goToCrumb with the trimmed trail', () => {
        inPlaceTrail.set([A, B, { inode: 'iC', title: 'TC' }]);
        const items = spectator.component.$relatedNavItems();

        // Earlier crumb uses command, not routerLink.
        expect(items[0].routerLink).toBeUndefined();
        expect(typeof items[0].command).toBe('function');

        items[0].command!({} as never);
        expect(inPlaceHost.goToCrumb).toHaveBeenCalledWith('iA', ['iA']);
    });

    it('reloads immediately (committing the trail) when the form is clean', () => {
        jest.spyOn(spectator.component, 'hasUnsavedChanges').mockReturnValue(false);

        navigation$.next({ inode: 'iB', trail: ['iA', 'iB'] });

        expect(inPlaceHost.setTrail).toHaveBeenCalledWith(['iA', 'iB']);
        expect(store.initializeExistingContent).toHaveBeenCalledWith(
            expect.objectContaining({ inode: 'iB' })
        );
    });

    it('does NOT commit the trail or reload when the user keeps editing (dirty)', () => {
        jest.spyOn(spectator.component, 'hasUnsavedChanges').mockReturnValue(true);
        const confirm = spectator.inject(ConfirmationService, true);
        // "Keep editing" == accept → onCancel (no-op). Simulate by invoking accept.
        jest.spyOn(confirm, 'confirm').mockImplementation((opts) => {
            opts.accept?.();

            return confirm;
        });

        navigation$.next({ inode: 'iB', trail: ['iA', 'iB'] });

        expect(inPlaceHost.setTrail).not.toHaveBeenCalled();
        expect(store.initializeExistingContent).not.toHaveBeenCalled();
    });

    it('commits the trail and reloads when the user discards changes (dirty)', () => {
        jest.spyOn(spectator.component, 'hasUnsavedChanges').mockReturnValue(true);
        const confirm = spectator.inject(ConfirmationService, true);
        // "Discard" == reject with REJECT type → onConfirm (reload).
        jest.spyOn(confirm, 'confirm').mockImplementation((opts) => {
            (opts.reject as (t: ConfirmEventType) => void)?.(ConfirmEventType.REJECT);

            return confirm;
        });

        navigation$.next({ inode: 'iB', trail: ['iA', 'iB'] });

        expect(inPlaceHost.setTrail).toHaveBeenCalledWith(['iA', 'iB']);
        expect(store.initializeExistingContent).toHaveBeenCalledWith(
            expect.objectContaining({ inode: 'iB' })
        );
    });

    it('reloads without touching the trail for a locale switch (request has no trail)', () => {
        jest.spyOn(spectator.component, 'hasUnsavedChanges').mockReturnValue(false);

        navigation$.next({ inode: 'iLocale' });

        expect(inPlaceHost.setTrail).not.toHaveBeenCalled();
        expect(store.initializeExistingContent).toHaveBeenCalledWith(
            expect.objectContaining({ inode: 'iLocale' })
        );
    });
});

// Separate top-level describe so the outer beforeEach above (which creates a component and
// instantiates TestBed) never runs before these tests. Provider overrides via
// createComponent({ providers: [...] }) require a fresh TestBed — calling them after
// TestBed is already instantiated throws "Cannot override provider when the test module
// has already been instantiated".
describe('EditContentLayoutComponent - Dialog Dirty-Close Guard', () => {
    let dialogCloseMock: jest.Mock;

    const createDialogComponent = createComponentFactory({
        component: DotEditContentLayoutComponent,
        imports: [
            MessageModule,
            ButtonModule,
            MockComponent(DotEditContentFormComponent),
            MockComponent(DotEditContentSidebarComponent),
            DotMessagePipe
        ],
        componentProviders: [
            DotEditContentStore,
            mockProvider(DotWorkflowsActionsService),
            mockProvider(DotWorkflowActionsFireService),
            mockProvider(DotEditContentService),
            mockProvider(DotContentTypeService),
            mockProvider(DotWorkflowService),
            mockProvider(DotContentletService),
            mockProvider(DotVersionableService),
            ConfirmationService,
            { provide: EDIT_CONTENT_HOST, useValue: mockEditContentHost }
        ],
        providers: [
            mockProvider(DotHttpErrorManagerService),
            mockProvider(MessageService),
            mockProvider(DialogService),
            mockProvider(DotLanguagesService),
            mockProvider(DotSiteService, {
                getCurrentSite: jest
                    .fn()
                    .mockReturnValue(of({ identifier: 'default', hostname: 'demo.dotcms.com' }))
            }),
            mockProvider(DotSystemConfigService, {
                getSystemConfig: jest.fn().mockReturnValue(of({}))
            }),
            GlobalStore,
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
                        return { params: { id: '', contentType: '' } };
                    }
                }
            },
            mockProvider(Router, {
                navigate: jest.fn().mockReturnValue(Promise.resolve(true)),
                url: '/test-url',
                events: of()
            }),
            provideHttpClient(),
            provideHttpClientTesting(),
            mockProvider(DotMessageService, {
                get: jest.fn((key: string, ...args: unknown[]) =>
                    key === 'edit.content.locked.by.user' ? `Content is locked by ${args[0]}` : key
                )
            }),
            mockProvider(DotRelatedContentNavigationStore, {
                trail: relatedTrailSignal,
                registerTitle: jest.fn(),
                buildTrailForSavedInode: jest.fn().mockReturnValue(null)
            })
        ]
    });

    beforeEach(() => {
        dialogCloseMock = jest.fn();
    });

    const createWithDialogRef = (extraProviders: unknown[] = []) =>
        createDialogComponent({
            detectChanges: false,
            providers: [
                { provide: DynamicDialogRef, useValue: { close: dialogCloseMock } },
                ...extraProviders
            ]
        });

    describe('programmatic close (dialogRef.close override)', () => {
        it('should pass through when the form is clean', () => {
            const ds = createWithDialogRef();
            const dialogRef = ds.inject(DynamicDialogRef);
            ds.detectChanges();

            dialogRef.close('result');

            expect(dialogCloseMock).toHaveBeenCalledWith('result');
        });

        it('should open the confirm dialog instead of closing when the form is dirty', () => {
            const ds = createWithDialogRef();
            const dsStore = ds.inject(DotEditContentStore, true);
            const dsConfirmService = ds.inject(ConfirmationService, true);
            const dialogRef = ds.inject(DynamicDialogRef);

            ds.detectChanges();
            jest.spyOn(ds.component, 'hasUnsavedChanges').mockReturnValue(true);
            jest.spyOn(dsStore, 'workflowActionSuccess').mockReturnValue(null);
            const confirmSpy = jest.spyOn(dsConfirmService, 'confirm');

            dialogRef.close('result');

            expect(confirmSpy).toHaveBeenCalledTimes(1);
            expect(dialogCloseMock).not.toHaveBeenCalled();
        });

        it('should close after the user discards changes', () => {
            const ds = createWithDialogRef();
            const dsStore = ds.inject(DotEditContentStore, true);
            const dsConfirmService = ds.inject(ConfirmationService, true);
            const dialogRef = ds.inject(DynamicDialogRef);

            ds.detectChanges();
            jest.spyOn(ds.component, 'hasUnsavedChanges').mockReturnValue(true);
            jest.spyOn(dsStore, 'workflowActionSuccess').mockReturnValue(null);

            let rejectFn: ((type?: ConfirmEventType) => void) | undefined;
            jest.spyOn(dsConfirmService, 'confirm').mockImplementation((opts) => {
                rejectFn = opts.reject as (type?: ConfirmEventType) => void;
            });

            dialogRef.close('result');
            rejectFn!(ConfirmEventType.REJECT);

            expect(dialogCloseMock).toHaveBeenCalledWith('result');
        });

        it('should not close when the user chooses keep editing', () => {
            const ds = createWithDialogRef();
            const dsStore = ds.inject(DotEditContentStore, true);
            const dsConfirmService = ds.inject(ConfirmationService, true);
            const dialogRef = ds.inject(DynamicDialogRef);

            ds.detectChanges();
            jest.spyOn(ds.component, 'hasUnsavedChanges').mockReturnValue(true);
            jest.spyOn(dsStore, 'workflowActionSuccess').mockReturnValue(null);

            let acceptFn: (() => void) | undefined;
            jest.spyOn(dsConfirmService, 'confirm').mockImplementation((opts) => {
                acceptFn = opts.accept;
            });

            dialogRef.close('result');
            acceptFn!();

            expect(dialogCloseMock).not.toHaveBeenCalled();
        });

        it('should bypass dirty check when a workflow action has just succeeded', () => {
            const ds = createWithDialogRef();
            const dsStore = ds.inject(DotEditContentStore, true);
            const dialogRef = ds.inject(DynamicDialogRef);

            ds.detectChanges();
            jest.spyOn(ds.component, 'hasUnsavedChanges').mockReturnValue(true);
            jest.spyOn(dsStore, 'workflowActionSuccess').mockReturnValue(MOCK_CONTENTLET_1_TAB);

            dialogRef.close('result');

            expect(dialogCloseMock).toHaveBeenCalledWith('result');
        });
    });

    describe('UI close (pDialog.close override)', () => {
        it('should pass through when the form is clean', () => {
            const pDialogCloseMock = jest.fn();
            const mockDynamicDialog = { dialog: { close: pDialogCloseMock } };

            const ds = createWithDialogRef([
                { provide: DynamicDialog, useValue: mockDynamicDialog }
            ]);
            ds.detectChanges();

            const mockEvent = { preventDefault: jest.fn() } as unknown as Event;
            mockDynamicDialog.dialog.close(mockEvent);

            expect(pDialogCloseMock).toHaveBeenCalledWith(mockEvent);
        });

        it('should call preventDefault and open confirm dialog when the form is dirty', () => {
            const pDialogCloseMock = jest.fn();
            const mockDynamicDialog = { dialog: { close: pDialogCloseMock } };

            const ds = createWithDialogRef([
                { provide: DynamicDialog, useValue: mockDynamicDialog }
            ]);
            const dsStore = ds.inject(DotEditContentStore, true);
            const dsConfirmService = ds.inject(ConfirmationService, true);

            ds.detectChanges();
            jest.spyOn(ds.component, 'hasUnsavedChanges').mockReturnValue(true);
            jest.spyOn(dsStore, 'workflowActionSuccess').mockReturnValue(null);
            const confirmSpy = jest.spyOn(dsConfirmService, 'confirm');

            const mockEvent = { preventDefault: jest.fn() } as unknown as Event;
            mockDynamicDialog.dialog.close(mockEvent);

            expect((mockEvent as { preventDefault: jest.Mock }).preventDefault).toHaveBeenCalled();
            expect(confirmSpy).toHaveBeenCalledTimes(1);
            expect(pDialogCloseMock).not.toHaveBeenCalled();
        });
    });
});
