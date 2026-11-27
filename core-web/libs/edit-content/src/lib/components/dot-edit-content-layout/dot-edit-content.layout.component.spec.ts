import { patchState } from '@ngrx/signals';
import { unprotected } from '@ngrx/signals/testing';
import {
    byTestId,
    createComponentFactory,
    createHostFactory,
    mockProvider,
    Spectator,
    SpectatorHost,
    SpyObject
} from '@openng/spectator/vitest';
import { MockComponent } from 'ng-mocks';
import { Observable, of, Subject } from 'rxjs';
import { Mock, vi } from 'vitest';

import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Component, signal } from '@angular/core';
import { fakeAsync, tick } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';

import { ConfirmationService, ConfirmEventType, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { DialogService } from 'primeng/dynamicdialog';
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
import {
    ComponentStatus,
    DotCMSContentlet,
    DotCMSWorkflowAction,
    DotContentletCanLock,
    DotCurrentUser,
    DotLanguage
} from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';
import { DotMessagePipe } from '@dotcms/ui';
import {
    MOCK_MULTIPLE_WORKFLOW_ACTIONS,
    MOCK_SINGLE_WORKFLOW_ACTIONS
} from '@dotcms/utils-testing';

import { DotEditContentLayoutComponent } from './dot-edit-content.layout.component';

import { FormValues } from '../../models/dot-edit-content-form.interface';
import { EDIT_CONTENT_NAVIGATION_OVERRIDE } from '../../models/edit-content-navigation-override';
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
import { MOCK_CONTENTLET_1_TAB, MOCK_WORKFLOW_STATUS } from '../../utils/edit-content.mock';
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
    setTrail: vi.fn(),
    resolveIdentity: vi.fn().mockReturnValue({}),
    reportSaved: vi.fn(),
    reloadContent: vi.fn(),
    setContentTitle: vi.fn(),
    addBreadcrumb: vi.fn(),
    goToSavedContent: vi.fn(),
    leaveDeletedContent: vi.fn(),
    goToRestoredVersion: vi.fn(),
    goToRelatedContent: vi.fn(),
    goToCrumb: vi.fn()
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
                getCurrentSite: vi
                    .fn()
                    .mockReturnValue(of({ identifier: 'default', hostname: 'demo.dotcms.com' }))
            }),
            mockProvider(DotSystemConfigService, {
                getSystemConfig: vi.fn().mockReturnValue(of({}))
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
                navigate: vi.fn().mockReturnValue(Promise.resolve(true)),
                url: '/test-url',
                events: of()
            }),
            provideHttpClient(),
            provideHttpClientTesting(),
            mockProvider(DotMessageService, {
                get: vi.fn((key: string, ...args: unknown[]) =>
                    key === 'edit.content.locked.by.user' ? `Content is locked by ${args[0]}` : key
                )
            }),
            mockProvider(DotRelatedContentNavigationStore, {
                trail: relatedTrailSignal,
                registerTitle: vi.fn(),
                buildTrailForSavedInode: vi.fn().mockReturnValue(null)
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

        vi.spyOn(dotLanguagesService, 'get').mockReturnValue(of(MOCK_LANGUAGES));

        // Mock the initial UI state
        vi.spyOn(utils, 'getStoredUIState').mockReturnValue({
            view: 'form',
            activeTab: 0,
            isSidebarOpen: true,
            activeSidebarTab: 0,
            isBetaMessageVisible: true,
            localeSelectorTab: 'all'
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

            const markFormPristineSpy = vi.spyOn(freshSpectator.component, 'markFormPristine');
            vi.spyOn(freshStore, 'workflowActionSuccess').mockReturnValue(MOCK_CONTENTLET_1_TAB);
            vi.spyOn(freshStore, 'clearWorkflowActionSuccess');

            freshSpectator.detectChanges();

            expect(host.reportSaved).toHaveBeenCalledWith(MOCK_CONTENTLET_1_TAB);
            expect(markFormPristineSpy).toHaveBeenCalledTimes(1);
            expect(freshStore.clearWorkflowActionSuccess).toHaveBeenCalledTimes(1);
        });

        it('should be a no-op when there is no workflow action success', () => {
            const freshSpectator = createComponent({ detectChanges: false });
            const freshStore = freshSpectator.inject(DotEditContentStore, true);
            const host = freshSpectator.inject(EDIT_CONTENT_HOST, true);

            const markFormPristineSpy = vi.spyOn(freshSpectator.component, 'markFormPristine');
            vi.spyOn(freshStore, 'workflowActionSuccess').mockReturnValue(null);
            vi.spyOn(freshStore, 'clearWorkflowActionSuccess');

            freshSpectator.detectChanges();

            expect(host.reportSaved).not.toHaveBeenCalled();
            expect(markFormPristineSpy).not.toHaveBeenCalled();
            expect(freshStore.clearWorkflowActionSuccess).not.toHaveBeenCalled();
        });
    });

    /**
     * An opener that asks for content in a language it has no version in gets the content in
     * another language, with the asked one on the request (the UVE's translate, a deep link with
     * `editContentLang`). The editor then offers that translation, as its own locale switch does:
     * the populate or manual dialog, once (#38303, T178).
     */
    describe('A requested language the content lacks', () => {
        const ENGLISH = {
            id: 1,
            isoCode: 'en-us',
            language: 'English',
            translated: true,
            defaultLanguage: true
        } as DotLanguage;
        const SPANISH = {
            id: 2,
            isoCode: 'es-es',
            language: 'Spanish',
            translated: false
        } as DotLanguage;
        const FRENCH = {
            id: 3,
            isoCode: 'fr-fr',
            language: 'French',
            translated: true
        } as DotLanguage;

        /**
         * Opens the editor on English content with the request naming a language, and lets the
         * store load the content's locales.
         */
        const openAsking = (languageId?: number) => {
            mockEditContentHost.resolveIdentity.mockReturnValue(languageId ? { languageId } : {});
            const fresh = createComponent({ detectChanges: false });
            const freshStore = fresh.inject(DotEditContentStore, true);
            // Should the store load the locales again, it gets the same ones. The component
            // provides its own DotLanguagesService, which the store uses.
            fresh
                .inject(DotContentletService, true)
                .getLanguages.mockReturnValue(of([ENGLISH, SPANISH, FRENCH]));
            vi.spyOn(fresh.inject(DotLanguagesService, true), 'getDefault').mockReturnValue(
                of(ENGLISH)
            );
            const switchLocale = vi
                .spyOn(freshStore, 'switchLocale')
                .mockImplementation(() => undefined);

            fresh.detectChanges();
            // English content with its locales loaded, as the store leaves them.
            const contentlet = { ...MOCK_CONTENTLET_1_TAB, languageId: ENGLISH.id };
            patchState(unprotected(freshStore), {
                contentlet,
                locales: [ENGLISH, FRENCH, SPANISH],
                currentLocale: ENGLISH,
                currentIdentifier: contentlet.identifier,
                localesStatus: { status: ComponentStatus.LOADED, error: '' }
            });
            // Twice: the first pass schedules the editor's effects on the new state, the second
            // runs them.
            fresh.detectChanges();
            fresh.detectChanges();

            return { fresh, freshStore, switchLocale };
        };

        it('should offer the translation into an untranslated language the request names', () => {
            const { switchLocale } = openAsking(SPANISH.id);
            expect(switchLocale).toHaveBeenCalledTimes(1);
            expect(switchLocale).toHaveBeenCalledWith(expect.objectContaining({ id: SPANISH.id }));
        });

        it('should offer it once, though the locales load again', () => {
            const { fresh, freshStore, switchLocale } = openAsking(SPANISH.id);

            patchState(unprotected(freshStore), {
                localesStatus: { status: ComponentStatus.LOADING, error: '' }
            });
            fresh.detectChanges();
            patchState(unprotected(freshStore), {
                localesStatus: { status: ComponentStatus.LOADED, error: '' }
            });
            fresh.detectChanges();

            expect(switchLocale).toHaveBeenCalledTimes(1);
        });

        it('should not offer it when the request names the content’s own language', () => {
            const { switchLocale } = openAsking(ENGLISH.id);

            expect(switchLocale).not.toHaveBeenCalled();
        });

        it('should not offer it when the request names a language the content has', () => {
            const { switchLocale } = openAsking(FRENCH.id);

            expect(switchLocale).not.toHaveBeenCalled();
        });

        it('should not offer it when the request names no language', () => {
            const { switchLocale } = openAsking();

            expect(switchLocale).not.toHaveBeenCalled();
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
                const onFormChangeSpy = vi.spyOn(store, 'onFormChange');

                spectator.component.onFormChange(MOCK_FORM_VALUES);

                expect(onFormChangeSpy).toHaveBeenCalledWith(MOCK_FORM_VALUES);
            });
        });

        describe('onWorkflowActionFired()', () => {
            it('should delegate to the form with params built from the store', () => {
                const fireWorkflowActionSpy = vi.fn();
                vi.spyOn(spectator.component, '$editContentForm').mockReturnValue({
                    fireWorkflowAction: fireWorkflowActionSpy
                } as unknown as DotEditContentFormComponent);

                vi.spyOn(store, 'currentLocale').mockReturnValue(MOCK_LANGUAGES[0]);
                vi.spyOn(store, 'contentlet').mockReturnValue(MOCK_CONTENTLET_1_TAB);
                vi.spyOn(store, 'contentType').mockReturnValue(CONTENT_TYPE_MOCK);
                vi.spyOn(store, 'currentIdentifier').mockReturnValue(
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
                vi.spyOn(spectator.component, '$editContentForm').mockReturnValue(undefined);

                const workflow = { id: 'action-id' } as DotCMSWorkflowAction;

                expect(() => spectator.component.onWorkflowActionFired(workflow)).not.toThrow();
            });
        });

        describe('closeMessage()', () => {
            it('should call store.toggleBetaMessage when closing beta message', () => {
                const toggleBetaMessageSpy = vi.spyOn(store, 'toggleBetaMessage');

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
            const fakeForm = { dirty: true, markAsPristine: vi.fn() };
            vi.spyOn(spectator.component, '$editContentForm').mockReturnValue({
                form: fakeForm
            } as unknown as DotEditContentFormComponent);

            expect(spectator.component.hasUnsavedChanges()).toBe(true);
        });

        it('should not crash markFormPristine when the form ref is undefined', () => {
            // No content has been initialized, so the inner form viewChild is empty.
            expect(() => spectator.component.markFormPristine()).not.toThrow();
        });
    });

    describe('confirmClose (chrome-agnostic close guard)', () => {
        it('bypasses the prompt while the editor is loading/saving (form disabled, nothing to discard)', () => {
            vi.spyOn(spectator.component, 'hasUnsavedChanges').mockReturnValue(true);
            vi.spyOn(store, 'workflowActionSuccess').mockReturnValue(null);
            vi.spyOn(store, 'isLoading').mockReturnValue(true);
            const onProceed = vi.fn();

            spectator.component.confirmClose(onProceed);

            expect(onProceed).toHaveBeenCalledTimes(1);
        });

        it('does NOT bypass the prompt once loading has settled, even if the sidebar has not (isFullyLoaded no longer gates this)', () => {
            const confirmationService = spectator.inject(ConfirmationService, true);
            vi.spyOn(spectator.component, 'hasUnsavedChanges').mockReturnValue(true);
            vi.spyOn(store, 'workflowActionSuccess').mockReturnValue(null);
            vi.spyOn(store, 'isLoading').mockReturnValue(false);
            // A real edit made while `isFullyLoaded()` was still false (sidebar still settling)
            // must still prompt once loading has finished — the previous `!isFullyLoaded()` bypass
            // would have discarded it silently instead.
            vi.spyOn(store, 'isFullyLoaded').mockReturnValue(false);
            const confirmSpy = vi.spyOn(confirmationService, 'confirm');
            const onProceed = vi.fn();

            spectator.component.confirmClose(onProceed);

            expect(confirmSpy).toHaveBeenCalledTimes(1);
            expect(onProceed).not.toHaveBeenCalled();
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
            vi.spyOn(spectator.component, 'hasUnsavedChanges').mockReturnValue(true);
            const event = new Event('beforeunload', { cancelable: true }) as BeforeUnloadEvent;
            const preventDefaultSpy = vi.spyOn(event, 'preventDefault');

            window.dispatchEvent(event);

            expect(preventDefaultSpy).toHaveBeenCalledTimes(1);
        });

        it('should NOT preventDefault on window beforeunload when the form is pristine', () => {
            vi.spyOn(spectator.component, 'hasUnsavedChanges').mockReturnValue(false);
            const event = new Event('beforeunload', { cancelable: true }) as BeforeUnloadEvent;
            const preventDefaultSpy = vi.spyOn(event, 'preventDefault');

            window.dispatchEvent(event);

            expect(preventDefaultSpy).not.toHaveBeenCalled();
        });
    });

    describe('Component Host Classes', () => {
        it('should apply edit-content--with-sidebar class when sidebar is open', () => {
            vi.spyOn(store, 'isSidebarOpen').mockImplementation(() => true);
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
                Object.defineProperty(event, 'preventDefault', { value: vi.fn() });

                // Spy on the store method
                const disableNewContentEditorSpy = vi.spyOn(store, 'disableNewContentEditor');

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

            it('should show the no-edit-permission message when the content is read-only (R4, #38724)', fakeAsync(() => {
                // READ without EDIT: `_canlock` answers false and the EDITING action list is
                // empty. The store is patched (not spied) so the OnPush template re-renders.
                spectator
                    .inject(DotContentletService, true)
                    .canLock.mockReturnValue(
                        of({ canLock: false, locked: false } as DotContentletCanLock)
                    );
                workflowActionsService.getByInode.mockReturnValue(of([]));
                // The information, activities and history hooks also react to the contentlet.
                const emptyPage = {
                    entity: [],
                    pagination: null,
                    errors: [],
                    i18nMessagesMap: {},
                    messages: [],
                    permissions: []
                };
                dotEditContentService.getReferencePages.mockReturnValue(of(0));
                dotEditContentService.getActivities.mockReturnValue(of([]));
                dotEditContentService.getVersions.mockReturnValue(of(emptyPage));
                dotEditContentService.getPushPublishHistory.mockReturnValue(of(emptyPage));
                patchState(unprotected(store), {
                    contentlet: MOCK_CONTENTLET_1_TAB,
                    initialContentletState: 'existing',
                    canLock: false,
                    currentContentActions: {}
                });
                spectator.detectChanges();
                tick();
                spectator.detectChanges();

                const message = spectator.query(byTestId('edit-content-layout__read-only-message'));

                expect(message).toBeTruthy();
                // DotMessageService is mocked to echo the key.
                expect(message).toContainText('edit.content.no.edit.permission');
            }));

            it('should show only the lock banner when another user holds the lock (#38724 review)', fakeAsync(() => {
                // canLock false + no actions is also what an EDIT user blocked by someone
                // else's lock gets. The form is read-only either way; the lock banner names
                // the reason, so the no-edit-permission message is not added on top.
                spectator
                    .inject(DotContentletService, true)
                    .canLock.mockReturnValue(
                        of({ canLock: false, locked: true } as DotContentletCanLock)
                    );
                workflowActionsService.getByInode.mockReturnValue(of([]));
                const emptyPage = {
                    entity: [],
                    pagination: null,
                    errors: [],
                    i18nMessagesMap: {},
                    messages: [],
                    permissions: []
                };
                dotEditContentService.getReferencePages.mockReturnValue(of(0));
                dotEditContentService.getActivities.mockReturnValue(of([]));
                dotEditContentService.getVersions.mockReturnValue(of(emptyPage));
                dotEditContentService.getPushPublishHistory.mockReturnValue(of(emptyPage));
                patchState(unprotected(store), {
                    currentUser: { userId: 'viewer' } as DotCurrentUser,
                    contentlet: {
                        ...MOCK_CONTENTLET_1_TAB,
                        locked: true,
                        lockedBy: { userId: 'someone-else', firstName: 'Ann', lastName: 'Lee' }
                    } as unknown as DotCMSContentlet,
                    initialContentletState: 'existing',
                    canLock: false,
                    currentContentActions: {}
                });
                spectator.detectChanges();
                tick();
                spectator.detectChanges();

                expect(store.isReadOnly()).toBe(true);
                expect(spectator.query(byTestId('edit-content-layout__lock-warning'))).toBeTruthy();
                expect(
                    spectator.query(byTestId('edit-content-layout__read-only-message'))
                ).toBeNull();
            }));

            it('should not show the no-edit-permission message when the content is editable', fakeAsync(() => {
                // New content is never read-only.
                spectator.detectChanges();
                tick();

                expect(
                    spectator.query(byTestId('edit-content-layout__read-only-message'))
                ).toBeNull();
            }));

            it('should show lock warning message when lockWarningMessage signal returns a message', fakeAsync(() => {
                // Verify the store's lockWarningMessage is used in the template by checking
                // that the component renders the topBar when lockWarningMessage returns a value.
                // The template condition is: topBarHasMessages = ... || lockWarningMessage || ...
                const mockMessage = 'Content is locked by Other User';
                vi.spyOn(store, 'lockWarningMessage').mockImplementation(() => mockMessage);
                spectator.detectChanges();
                tick();

                // When lockWarningMessage returns a message, the store value should be used
                expect(store.lockWarningMessage()).toBe(mockMessage);
            }));
        });
    });

    /**
     * The banners are flat strips and the top bar draws the one line under them, in the tab
     * list's colour (`surface-200`). A banner's own outline doubled the side panel's header
     * divider above it and the top bar's line below it (#37958).
     */
    describe('Top bar line under the banners (#37958)', () => {
        const topBar = () => spectator.query(byTestId('edit-content-layout__topBar'));

        beforeEach(fakeAsync(() => {
            dotContentTypeService.getContentTypeWithRender.mockReturnValue(of(CONTENT_TYPE_MOCK));
            workflowActionsService.getDefaultActions.mockReturnValue(
                of(MOCK_SINGLE_WORKFLOW_ACTIONS)
            );
            store.initializeNewContent('contentTypeName');
            spectator.detectChanges();
            tick();
        }));

        afterEach(() => relatedTrailSignal.set([]));

        it('draws the line in the tab list colour under the beta banner', () => {
            expect(spectator.query(byTestId('edit-content-layout__beta-message'))).toBeTruthy();
            expect(topBar()).toHaveClass('border-b');
            expect(topBar()).toHaveClass('border-surface-200');
            expect(topBar()).not.toHaveClass('border-gray-300');
        });

        it('draws the line when only the Relating content banner shows', () => {
            spectator.component.closeMessage('betaMessage');
            relatedTrailSignal.set([
                { inode: 'iA', title: 'TA' },
                { inode: 'iB', title: 'TB' }
            ]);
            spectator.detectChanges();

            expect(spectator.query(byTestId('edit-content-layout__relating-content'))).toBeTruthy();
            expect(topBar()).toHaveClass('border-b');
            expect(topBar()).toHaveClass('border-surface-200');
        });

        it('draws no line when no banner shows', () => {
            spectator.component.closeMessage('betaMessage');
            spectator.detectChanges();

            expect(topBar()).not.toHaveClass('border-b');
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

        it('builds `command` crumbs that navigate via the host with the trimmed trail; the current (last) crumb is a plain label', () => {
            mockEditContentHost.goToCrumb.mockClear();
            relatedTrailSignal.set([A, B, C]);

            const items = spectator.component.$relatedNavItems();

            // Every crumb is a command (not a declarative routerLink) even
            // full-screen, so the unsaved-changes prompt runs at the source now
            // that the reused route no longer fires canDeactivate on :id → :id.
            expect(items.map((i) => i.label)).toEqual(['TA', 'TB', 'TC']);
            expect(items[0].routerLink).toBeUndefined();
            expect(items[1].routerLink).toBeUndefined();

            // First crumb trims the trail to the origin; second to [iA, iB].
            items[0].command!({} as never);
            expect(mockEditContentHost.goToCrumb).toHaveBeenLastCalledWith('iA', ['iA']);

            items[1].command!({} as never);
            expect(mockEditContentHost.goToCrumb).toHaveBeenLastCalledWith('iB', ['iA', 'iB']);

            // Current content — plain label, no navigation.
            expect(items[2].command).toBeUndefined();
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
        setTrail: vi.fn(),
        resolveIdentity: vi.fn().mockReturnValue({}),
        reportSaved: vi.fn(),
        reloadContent: vi.fn(),
        setContentTitle: vi.fn(),
        addBreadcrumb: vi.fn(),
        goToSavedContent: vi.fn(),
        leaveDeletedContent: vi.fn(),
        goToRestoredVersion: vi.fn(),
        goToRelatedContent: vi.fn(),
        goToCrumb: vi.fn(),
        reportLanguage: vi.fn()
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
                getCurrentSite: vi
                    .fn()
                    .mockReturnValue(of({ identifier: 'default', hostname: 'demo.dotcms.com' }))
            }),
            mockProvider(DotSystemConfigService, {
                getSystemConfig: vi.fn().mockReturnValue(of({}))
            }),
            GlobalStore,
            {
                provide: DotCurrentUserService,
                useValue: { getCurrentUser: () => of({ userId: '123', userName: 'John Doe' }) }
            },
            { provide: ActivatedRoute, useValue: { snapshot: { params: {} } } },
            mockProvider(Router, { navigate: vi.fn(), url: '/test-url', events: of() }),
            provideHttpClient(),
            provideHttpClientTesting(),
            mockProvider(DotMessageService, { get: vi.fn((key: string) => key) }),
            mockProvider(DotRelatedContentNavigationStore, {
                trail: inPlaceTrail,
                registerTitle: vi.fn()
            })
        ]
    });

    let spectator: Spectator<DotEditContentLayoutComponent>;
    let store: SpyObject<InstanceType<typeof DotEditContentStore>>;

    beforeEach(() => {
        navigation$ = new Subject<InPlaceNavigationRequest>();
        inPlaceHost.inPlaceNavigation$ = navigation$;
        inPlaceTrail.set([]);
        Object.values(inPlaceHost).forEach((v) => (v as Mock)?.mockClear?.());
        inPlaceHost.resolveIdentity.mockReturnValue({});

        spectator = createComponent({ detectChanges: false });
        store = spectator.inject(DotEditContentStore, true);
        vi.spyOn(store, 'initializeExistingContent').mockImplementation(() => undefined);
        vi.spyOn(store, 'initialize').mockImplementation(() => undefined);
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
        vi.spyOn(spectator.component, 'hasUnsavedChanges').mockReturnValue(false);

        navigation$.next({ inode: 'iB', trail: ['iA', 'iB'] });

        expect(inPlaceHost.setTrail).toHaveBeenCalledWith(['iA', 'iB']);
        expect(store.initializeExistingContent).toHaveBeenCalledWith(
            expect.objectContaining({ inode: 'iB' })
        );
    });

    it('does NOT commit the trail or reload when the user keeps editing (dirty)', () => {
        vi.spyOn(spectator.component, 'hasUnsavedChanges').mockReturnValue(true);
        const confirm = spectator.inject(ConfirmationService, true);
        // "Keep editing" == accept → onCancel (no-op). Simulate by invoking accept.
        vi.spyOn(confirm, 'confirm').mockImplementation((opts) => {
            opts.accept?.();

            return confirm;
        });

        navigation$.next({ inode: 'iB', trail: ['iA', 'iB'] });

        expect(inPlaceHost.setTrail).not.toHaveBeenCalled();
        expect(store.initializeExistingContent).not.toHaveBeenCalled();
    });

    it('commits the trail and reloads when the user discards changes (dirty)', () => {
        vi.spyOn(spectator.component, 'hasUnsavedChanges').mockReturnValue(true);
        const confirm = spectator.inject(ConfirmationService, true);
        // "Discard" == reject with REJECT type → onConfirm (reload).
        vi.spyOn(confirm, 'confirm').mockImplementation((opts) => {
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
        vi.spyOn(spectator.component, 'hasUnsavedChanges').mockReturnValue(false);

        navigation$.next({ inode: 'iLocale' });

        expect(inPlaceHost.setTrail).not.toHaveBeenCalled();
        expect(store.initializeExistingContent).toHaveBeenCalledWith(
            expect.objectContaining({ inode: 'iLocale' })
        );
        // No language on the request: nothing to report.
        expect(inPlaceHost.reportLanguage).not.toHaveBeenCalled();
    });

    /**
     * The language of a locale switch reaches the opener only once the reload runs, so its URL
     * never names a language the editor isn't showing (#37759, FR-020, T125).
     */
    describe('reporting the language of a locale switch', () => {
        it('reports it when the form is clean', () => {
            vi.spyOn(spectator.component, 'hasUnsavedChanges').mockReturnValue(false);

            navigation$.next({ inode: 'iLocale', languageId: 2 });

            expect(inPlaceHost.reportLanguage).toHaveBeenCalledWith(2);
        });

        it('does not report it when the user keeps editing (dirty)', () => {
            vi.spyOn(spectator.component, 'hasUnsavedChanges').mockReturnValue(true);
            const confirm = spectator.inject(ConfirmationService, true);
            vi.spyOn(confirm, 'confirm').mockImplementation((opts) => {
                opts.accept?.();

                return confirm;
            });

            navigation$.next({ inode: 'iLocale', languageId: 2 });

            expect(store.initializeExistingContent).not.toHaveBeenCalled();
            expect(inPlaceHost.reportLanguage).not.toHaveBeenCalled();
        });

        it('reports it when the user discards changes (dirty)', () => {
            vi.spyOn(spectator.component, 'hasUnsavedChanges').mockReturnValue(true);
            const confirm = spectator.inject(ConfirmationService, true);
            vi.spyOn(confirm, 'confirm').mockImplementation((opts) => {
                (opts.reject as (t: ConfirmEventType) => void)?.(ConfirmEventType.REJECT);

                return confirm;
            });

            navigation$.next({ inode: 'iLocale', languageId: 2 });

            expect(inPlaceHost.reportLanguage).toHaveBeenCalledWith(2);
        });
    });
});

/**
 * The navigation override reaches the editor's store through the element tree: the store is
 * provided by this layout, so it sees an override any ancestor component provides. Content Drive
 * provides it on its shell, which renders the side panel that renders this layout (#37759,
 * FR-028, FR-029).
 */
describe('EditContentLayoutComponent - navigation override from an ancestor', () => {
    const openerOverride = { switchToLegacyEditor: vi.fn(), leaveOnLoadError: vi.fn() };

    @Component({
        standalone: false,
        selector: 'dot-test-opener',
        template: '',
        providers: [{ provide: EDIT_CONTENT_NAVIGATION_OVERRIDE, useValue: openerOverride }]
    })
    class OpenerComponent {}

    const inPlaceHost = {
        inPlaceNavigation: true,
        inPlaceNavigation$: new Subject<InPlaceNavigationRequest>(),
        trail: signal<DotRelatedContentCrumb[]>([]),
        setTrail: vi.fn(),
        resolveIdentity: vi.fn().mockReturnValue({ inode: 'opened-inode' }),
        reportSaved: vi.fn(),
        reloadContent: vi.fn(),
        setContentTitle: vi.fn(),
        addBreadcrumb: vi.fn(),
        goToSavedContent: vi.fn(),
        leaveDeletedContent: vi.fn(),
        goToRestoredVersion: vi.fn(),
        goToRelatedContent: vi.fn(),
        goToCrumb: vi.fn()
    };

    const createHost = createHostFactory({
        component: DotEditContentLayoutComponent,
        host: OpenerComponent,
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
            // The content this editor was opened for fails to load.
            mockProvider(DotEditContentService, {
                getContentById: vi
                    .fn()
                    .mockReturnValue(
                        new Observable((subscriber) =>
                            subscriber.error(new HttpErrorResponse({ status: 404 }))
                        )
                    )
            }),
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
                getCurrentSite: vi
                    .fn()
                    .mockReturnValue(of({ identifier: 'default', hostname: 'demo.dotcms.com' }))
            }),
            mockProvider(DotSystemConfigService, {
                getSystemConfig: vi.fn().mockReturnValue(of({}))
            }),
            GlobalStore,
            {
                provide: DotCurrentUserService,
                useValue: { getCurrentUser: () => of({ userId: '123', userName: 'John Doe' }) }
            },
            { provide: ActivatedRoute, useValue: { snapshot: { params: {} } } },
            mockProvider(Router, { navigate: vi.fn(), url: '/test-url', events: of() }),
            provideHttpClient(),
            provideHttpClientTesting(),
            mockProvider(DotMessageService, { get: vi.fn((key: string) => key) }),
            mockProvider(DotRelatedContentNavigationStore, {
                trail: signal<DotRelatedContentCrumb[]>([]),
                registerTitle: vi.fn()
            })
        ]
    });

    beforeEach(() => openerOverride.leaveOnLoadError.mockReset());

    it("hands a load error to the opener's override instead of navigating", () => {
        openerOverride.leaveOnLoadError.mockReturnValue(true);

        // Opening the layout loads the content the host names, as inside Content Drive's panel.
        const spectator: SpectatorHost<DotEditContentLayoutComponent> = createHost(
            '<dot-edit-content-form-layout />'
        );

        expect(openerOverride.leaveOnLoadError).toHaveBeenCalledWith({ inode: 'opened-inode' });
        expect(spectator.inject(Router).navigate).not.toHaveBeenCalled();
    });
});

/**
 * Integration coverage for the sidebar-refresh invariant (issue #36617).
 *
 * The feature specs (`activities.feature.spec.ts`, `information.feature.spec.ts`,
 * `history.feature.spec.ts`) verify the store effects against a synthetic store with
 * NO component mounted. That cannot catch the regression this fix addresses: those
 * effects used to live on `DotEditContentSidebarComponent`, which the layout's
 * `@if ($store.isLoaded() || $store.isSaving() || $store.isReloading())` destroys and
 * recreates — taking the effects with it.
 *
 * Here the store is real and mounted inside the real layout, while the sidebar is a
 * `MockComponent` (no effects of its own). So if the fetching ever moves back into
 * that component, these tests fail where the feature specs would still pass.
 *
 * Both hosts are covered because they differ in what happens after a save: the
 * full-screen host navigates (re-initializing and clearing the lists), while the
 * dialog host does not — and the refresh must not depend on that difference.
 */
describe.each([
    ['full-screen', false],
    ['dialog', true]
])(
    'EditContentLayoutComponent - sidebar refresh is store-owned (%s host)',
    (_hostName, inPlaceNavigation) => {
        let spectator: Spectator<DotEditContentLayoutComponent>;
        let store: SpyObject<InstanceType<typeof DotEditContentStore>>;
        let dotEditContentService: SpyObject<DotEditContentService>;

        const IDENTIFIER = MOCK_CONTENTLET_1_TAB.identifier;
        const hostTrail = signal<DotRelatedContentCrumb[]>([]);
        const host = {
            inPlaceNavigation,
            inPlaceNavigation$: undefined,
            trail: hostTrail,
            setTrail: vi.fn(),
            resolveIdentity: vi.fn().mockReturnValue({}),
            reportSaved: vi.fn(),
            reloadContent: vi.fn(),
            setContentTitle: vi.fn(),
            addBreadcrumb: vi.fn(),
            goToSavedContent: vi.fn(),
            leaveDeletedContent: vi.fn(),
            goToRestoredVersion: vi.fn(),
            goToRelatedContent: vi.fn(),
            goToCrumb: vi.fn()
        };

        const emptyPage = {
            entity: [],
            pagination: null,
            errors: [],
            i18nMessagesMap: {},
            messages: [],
            permissions: []
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
                { provide: EDIT_CONTENT_HOST, useValue: host }
            ],
            providers: [
                mockProvider(DotHttpErrorManagerService),
                mockProvider(MessageService),
                mockProvider(DialogService),
                mockProvider(DotLanguagesService),
                mockProvider(DotSiteService, {
                    getCurrentSite: vi
                        .fn()
                        .mockReturnValue(of({ identifier: 'default', hostname: 'demo.dotcms.com' }))
                }),
                mockProvider(DotSystemConfigService, {
                    getSystemConfig: vi.fn().mockReturnValue(of({}))
                }),
                GlobalStore,
                {
                    provide: DotCurrentUserService,
                    useValue: { getCurrentUser: () => of({ userId: '123', userName: 'John Doe' }) }
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
                    navigate: vi.fn().mockReturnValue(Promise.resolve(true)),
                    url: '/test-url',
                    events: of()
                }),
                provideHttpClient(),
                provideHttpClientTesting(),
                mockProvider(DotMessageService, { get: vi.fn((key: string) => key) }),
                mockProvider(DotRelatedContentNavigationStore, {
                    trail: hostTrail,
                    registerTitle: vi.fn(),
                    buildTrailForSavedInode: vi.fn().mockReturnValue(null)
                })
            ]
        });

        /** Puts the store in the state a loaded contentlet produces, so the sidebar renders. */
        const loadContentlet = (contentlet = MOCK_CONTENTLET_1_TAB) => {
            patchState(unprotected(store), {
                contentlet,
                contentType: CONTENT_TYPE_MOCK,
                state: ComponentStatus.LOADED
            });
            spectator.detectChanges();
        };

        beforeEach(() => {
            spectator = createComponent({ detectChanges: false });
            store = spectator.inject(DotEditContentStore, true);
            dotEditContentService = spectator.inject(DotEditContentService, true);

            dotEditContentService.getActivities.mockReturnValue(of([]));
            dotEditContentService.getReferencePages.mockReturnValue(of(0));
            dotEditContentService.getVersions.mockReturnValue(of(emptyPage));
            dotEditContentService.getPushPublishHistory.mockReturnValue(of(emptyPage));

            // The lock and workflow features also react to `contentlet`, so their services
            // need observables or their effects blow up before the ones under test run.
            spectator
                .inject(DotContentletService, true)
                .canLock.mockReturnValue(of({ entity: { canLock: true } }));
            spectator
                .inject(DotWorkflowsActionsService, true)
                .getByInode.mockReturnValue(of(MOCK_SINGLE_WORKFLOW_ACTIONS));
            spectator
                .inject(DotWorkflowService, true)
                .getWorkflowStatus.mockReturnValue(of(MOCK_WORKFLOW_STATUS));
        });

        it('should fetch sidebar data even while the sidebar component is not rendered', fakeAsync(() => {
            // `@if` is false here (not loaded, not saving, not reloading), so the sidebar is
            // never mounted — yet the data must still load, because the store owns it.
            patchState(unprotected(store), {
                contentlet: MOCK_CONTENTLET_1_TAB,
                state: ComponentStatus.LOADING
            });
            spectator.detectChanges();
            tick();

            expect(spectator.query('dot-edit-content-sidebar')).toBeNull();
            expect(dotEditContentService.getActivities).toHaveBeenCalledWith(IDENTIFIER);
            expect(dotEditContentService.getReferencePages).toHaveBeenCalledWith(IDENTIFIER);
            expect(dotEditContentService.getVersions).toHaveBeenCalled();
        }));

        // Before there is any content the editor renders nothing, so a spinner fills it.
        it('should show a spinner on the first load, until the content is loaded', () => {
            const loading = () => spectator.query(byTestId('edit-content-layout__loading'));
            patchState(unprotected(store), { contentType: null, state: ComponentStatus.LOADING });
            spectator.detectChanges();

            expect(loading()).not.toBeNull();
            // The same spinner the side-panel drawer shows while this component's chunk loads,
            // in the same place, so opening the panel reads as one continuous loading.
            expect(loading()?.querySelector('p-progressspinner')).not.toBeNull();

            loadContentlet();

            expect(loading()).toBeNull();
        });

        it('should keep the loading screen until the lock permission is known (#38724 R4)', () => {
            // On a slow network the form used to render enabled and flip to disabled when
            // _canlock answered; the spinner now holds until the answer is in.
            const canLock$ = new Subject<DotContentletCanLock>();
            spectator.inject(DotContentletService, true).canLock.mockReturnValue(canLock$);
            patchState(unprotected(store), {
                contentlet: MOCK_CONTENTLET_1_TAB,
                contentType: CONTENT_TYPE_MOCK,
                state: ComponentStatus.LOADED,
                canLock: null
            });
            spectator.detectChanges();

            expect(spectator.query(byTestId('edit-content-layout__loading'))).not.toBeNull();
            expect(spectator.query(byTestId('edit-content-layout__body'))).toBeNull();

            canLock$.next({ canLock: false, locked: false } as DotContentletCanLock);
            spectator.detectChanges();

            expect(spectator.query(byTestId('edit-content-layout__loading'))).toBeNull();
            expect(spectator.query(byTestId('edit-content-layout__body'))).not.toBeNull();
        });

        it('should keep the form mounted while the permission is re-checked after a save (#38724)', () => {
            // The dialog's first save patches the saved contentlet straight to LOADED (no
            // reload, no overlay): the brand-new content had no inode to check, so canLock is
            // still null and only now gets checked. The editor on screen must stay mounted —
            // dropping to the first-load spinner remounts the sidebar on its first tab.
            const canLock$ = new Subject<DotContentletCanLock>();
            spectator.inject(DotContentletService, true).canLock.mockReturnValue(canLock$);
            patchState(unprotected(store), {
                contentlet: null,
                contentType: CONTENT_TYPE_MOCK,
                state: ComponentStatus.LOADED,
                canLock: null
            });
            spectator.detectChanges();
            expect(spectator.query(byTestId('edit-content-layout__body'))).not.toBeNull();

            // Saved: the contentlet now has an identifier, its lock not yet checked.
            patchState(unprotected(store), { contentlet: MOCK_CONTENTLET_1_TAB });
            spectator.detectChanges();

            expect(spectator.query(byTestId('edit-content-layout__loading'))).toBeNull();
            expect(spectator.query(byTestId('edit-content-layout__body'))).not.toBeNull();

            canLock$.next({ canLock: true, locked: false } as DotContentletCanLock);
            spectator.detectChanges();

            expect(spectator.query(byTestId('edit-content-layout__body'))).not.toBeNull();
        });

        it('should keep the form mounted under the reload overlay while the permission re-settles (#38724)', () => {
            // After a save in the dialog (or an in-place reload) the content re-initialises and
            // forgets canLock. The previous form must stay under the overlay until the answer is
            // in — dropping to the first-load spinner unmounts the sidebar and resets its tab.
            const canLock$ = new Subject<DotContentletCanLock>();
            spectator.inject(DotContentletService, true).canLock.mockReturnValue(canLock$);
            loadContentlet();
            patchState(unprotected(store), { canLock: true });
            spectator.detectChanges();
            expect(spectator.query(byTestId('edit-content-layout__body'))).not.toBeNull();

            // Reload starts: stale-while-revalidate keeps the form, arms the overlay.
            patchState(unprotected(store), { state: ComponentStatus.LOADING, canLock: null });
            spectator.detectChanges();
            expect(
                spectator.query(byTestId('edit-content-layout__reloading-overlay'))
            ).not.toBeNull();

            // Content loaded, permission still unknown.
            patchState(unprotected(store), {
                state: ComponentStatus.LOADED,
                contentlet: { ...MOCK_CONTENTLET_1_TAB, inode: 'new-inode' }
            });
            spectator.detectChanges();

            expect(spectator.query(byTestId('edit-content-layout__loading'))).toBeNull();
            expect(spectator.query(byTestId('edit-content-layout__body'))).not.toBeNull();
            expect(
                spectator.query(byTestId('edit-content-layout__reloading-overlay'))
            ).not.toBeNull();

            canLock$.next({ canLock: true, locked: false } as DotContentletCanLock);
            spectator.detectChanges();

            expect(spectator.query(byTestId('edit-content-layout__reloading-overlay'))).toBeNull();
            expect(spectator.query(byTestId('edit-content-layout__body'))).not.toBeNull();
        });

        it('should refresh after the sidebar component is destroyed and recreated', fakeAsync(() => {
            loadContentlet();
            tick();
            expect(spectator.query('dot-edit-content-sidebar')).not.toBeNull();

            dotEditContentService.getActivities.mockClear();
            dotEditContentService.getReferencePages.mockClear();
            dotEditContentService.getVersions.mockClear();

            // Flip the `@if` off — Angular destroys the sidebar and, before this fix, its effects.
            patchState(unprotected(store), { contentType: null, state: ComponentStatus.LOADING });
            spectator.detectChanges();
            tick();
            expect(spectator.query('dot-edit-content-sidebar')).toBeNull();

            // Come back with a new inode, as a save does.
            loadContentlet({ ...MOCK_CONTENTLET_1_TAB, inode: 'inode-after-save' });
            tick();

            expect(spectator.query('dot-edit-content-sidebar')).not.toBeNull();
            expect(dotEditContentService.getActivities).toHaveBeenCalledWith(IDENTIFIER);
            expect(dotEditContentService.getReferencePages).toHaveBeenCalledWith(IDENTIFIER);
            expect(dotEditContentService.getVersions).toHaveBeenCalled();
        }));

        it('should refresh on a save that mints a new inode without any re-initialization', fakeAsync(() => {
            // This is the dialog host's path: no navigation, so nothing clears the lists and
            // the identifier/locale never change. Asserted for both hosts because the refresh
            // must no longer depend on which one is mounted.
            loadContentlet();
            tick();

            dotEditContentService.getActivities.mockClear();
            dotEditContentService.getReferencePages.mockClear();
            dotEditContentService.getVersions.mockClear();

            patchState(unprotected(store), {
                contentlet: { ...MOCK_CONTENTLET_1_TAB, inode: 'inode-after-publish' }
            });
            spectator.detectChanges();
            tick();

            expect(dotEditContentService.getActivities).toHaveBeenCalledWith(IDENTIFIER);
            expect(dotEditContentService.getReferencePages).toHaveBeenCalledWith(IDENTIFIER);
            expect(dotEditContentService.getVersions).toHaveBeenCalled();
        }));
    }
);
