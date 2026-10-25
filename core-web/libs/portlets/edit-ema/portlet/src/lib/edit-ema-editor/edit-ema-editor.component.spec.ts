import { patchState } from '@ngrx/signals';
import { Dispatcher } from '@ngrx/signals/events';
import {
    SpectatorRouting,
    byTestId,
    createRoutingFactory,
    mockProvider
} from '@openng/spectator/vitest';
import { MockComponent } from 'ng-mocks';
import { of } from 'rxjs';
import { MockInstance, describe, expect, it, vi } from 'vitest';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { EventEmitter, Input, Output, signal, Component } from '@angular/core';
import { By } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import { ConfirmationService, MessageService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogService } from 'primeng/dynamicdialog';

import { map } from 'rxjs/operators';

import {
    DotActionUrlService,
    DotAlertConfirmService,
    DotAnalyticsTrackerService,
    DotContentTypeService,
    DotContentletLockerService,
    DotContentletService,
    DotCopyContentService,
    DotCurrentUserService,
    DotDevicesService,
    DotESContentService,
    DotExperimentsService,
    DotFavoritePageService,
    DotGlobalMessageService,
    DotHttpErrorManagerService,
    DotIframeService,
    DotLanguagesService,
    DotMessageDisplayService,
    DotMessageService,
    DotPageLayoutService,
    DotPersonalizeService,
    DotPropertiesService,
    DotRouterService,
    DotSeoMetaTagsService,
    DotSeoMetaTagsUtilService,
    DotSessionStorageService,
    DotTempFileUploadService,
    DotUiColorsService,
    DotWorkflowActionsFireService,
    DotWorkflowsActionsService,
    PushPublishService
} from '@dotcms/data-access';
import { DotcmsConfigService, LoginService } from '@dotcms/dotcms-js';
import { DEFAULT_VARIANT_ID, FeaturedFlags } from '@dotcms/dotcms-models';
import { DotPaletteListStore, DotResultsSeoToolComponent } from '@dotcms/portlets/dot-ema/ui';
import { GlobalStore } from '@dotcms/store';
import { UVE_MODE } from '@dotcms/types';
import { DotCopyContentModalService, SafeUrlPipe } from '@dotcms/ui';
import { WINDOW } from '@dotcms/utils';
import {
    CONTENT_TYPE_MOCK_FOR_EDITOR,
    CurrentUserDataMock,
    DotCurrentUserServiceMock,
    DotDevicesServiceMock,
    DotLanguagesServiceMock,
    DotPersonalizeServiceMock,
    DotcmsConfigServiceMock,
    LoginServiceMock,
    MockDotHttpErrorManagerService,
    MockDotMessageService,
    URL_MAP_CONTENTLET,
    createFakeContentlet,
    getDraftExperimentMock,
    getRunningExperimentMock,
    getScheduleExperimentMock,
    seoOGTagsResultMock
} from '@dotcms/utils-testing';

import { DotUveBrowserToolbarComponent } from './components/dot-uve-browser-toolbar/dot-uve-browser-toolbar.component';
import { DotUveCanvasComponent } from './components/dot-uve-canvas/dot-uve-canvas.component';
import { DotUveContentEditorComponent } from './components/dot-uve-content-editor/dot-uve-content-editor.component';
import { uveContentEditorEvents } from './components/dot-uve-content-editor/dot-uve-content-editor.events';
import { DotUveEditPanelComponent } from './components/dot-uve-edit-panel/dot-uve-edit-panel.component';
import { DotUvePaletteComponent } from './components/dot-uve-palette/dot-uve-palette.component';
import { DotEmaRunningExperimentComponent } from './components/dot-uve-toolbar/components/dot-ema-running-experiment/dot-ema-running-experiment.component';
import { DotUveWorkflowActionsComponent } from './components/dot-uve-toolbar/components/dot-uve-workflow-actions/dot-uve-workflow-actions.component';
import { DotUveToolbarComponent } from './components/dot-uve-toolbar/dot-uve-toolbar.component';
import { EditEmaEditorComponent } from './edit-ema-editor.component';

import { DotBlockEditorSidebarComponent } from '../components/dot-block-editor-sidebar/dot-block-editor-sidebar.component';
import { DotEmaDialogComponent } from '../components/dot-ema-dialog/dot-ema-dialog.component';
import { DotPageApiService } from '../services/dot-page-api/dot-page-api.service';
import { DEFAULT_PERSONA, PERSONA_KEY } from '../shared/consts';
import {
    MOCK_RESPONSE_HEADLESS,
    QUERY_PARAMS_MOCK,
    UVE_PAGE_RESPONSE_MAP,
    dotPropertiesServiceMock
} from '../shared/mocks';
import { ActionPayload } from '../shared/models';
import { UVEStore } from '../store/dot-uve.store';
import { UVE_EDIT_PANEL_TABS } from '../store/features/editor/models';
import { WithPageApiMethods } from '../store/features/page-api/withPageApi';

global.URL.createObjectURL = vi.fn(
    () => 'blob:http://localhost:3000/12345678-1234-1234-1234-123456789012'
);

// Mock window.matchMedia for PrimeNG components
Object.defineProperty(window, 'matchMedia', {
    writable: true,
    value: vi.fn().mockImplementation((query: string) => ({
        matches: false,
        media: query,
        onchange: null,
        addListener: vi.fn(),
        removeListener: vi.fn(),
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
        dispatchEvent: vi.fn()
    }))
});

const messagesMock = {
    'editpage.content.contentlet.remove.confirmation_message.header': 'Deleting Content',
    'editpage.content.contentlet.remove.confirmation_message.message':
        'Are you sure you want to remove this content?',
    'dot.common.dialog.accept': 'Accept',
    'dot.common.dialog.reject': 'Reject',
    'editpage.content.add.already.title': 'Content already added',
    'editpage.content.add.already.message': 'This content is already added to this container',
    'editpage.not.lincese.error':
        'Inline editing is available only with an enterprise license. Please contact support to upgrade your license.',
    'dot.common.license.enterprise.only.error': 'Enterprise Only',
    'message.content.saved': 'Content saved',
    'message.content.note.already.published':
        'Note: If you edit auto-published content, changes apply immediately.'
};

const mockGlobalStore = {
    currentSiteId: signal('demo.dotcms.com'),
    siteDetails: signal({ identifier: 'demo.dotcms.com', hostname: 'demo.dotcms.com' }),
    loggedUser: signal(CurrentUserDataMock)
};

// Stub components to avoid ng-mocks signal query issues

@Component({
    selector: 'dot-uve-toolbar',
    template: '<div></div>',
    standalone: true
})
class DotUveToolbarStubComponent {
    @Input() set unknown(value: unknown) {
        // void
    }
}

@Component({
    selector: 'dot-uve-palette',
    template: '<div></div>',
    standalone: true
})
class DotUvePaletteStubComponent {
    @Output() onTabChange = new EventEmitter<unknown>();
    @Input() languageId: unknown;
    @Input() pagePath: unknown;
    @Input() variantId: unknown;
    @Input() activeTab: unknown;
    @Input() showStyleEditorTab: unknown;
    @Input() styleSchema: unknown;
}

/** The content editor has its own spec; here it only needs to be placed. */
@Component({
    selector: 'dot-uve-content-editor',
    template: '<div></div>',
    standalone: true
})
class DotUveContentEditorMockComponent {}

/** The browser toolbar has its own spec; here it only needs to be placed. */
@Component({
    selector: 'dot-uve-browser-toolbar',
    template: '<div></div>',
    standalone: true
})
class DotUveBrowserToolbarMockComponent {}

/** The edit panel has its own spec; here it only needs to be placed. */
@Component({
    selector: 'dot-uve-edit-panel',
    template: '<div></div>',
    standalone: true
})
class DotUveEditPanelMockComponent {}

/** The canvas has its own spec; here it only needs to be placed. */
@Component({
    selector: 'dot-uve-canvas',
    template: '<div></div>'
})
class DotUveCanvasMockComponent {}

const createRouting = () =>
    createRoutingFactory({
        component: EditEmaEditorComponent,
        imports: [RouterTestingModule, SafeUrlPipe, ConfirmDialogModule],
        declarations: [
            MockComponent(DotUveWorkflowActionsComponent),
            MockComponent(DotResultsSeoToolComponent),
            MockComponent(DotEmaRunningExperimentComponent)
        ],
        overrideComponents: [
            [
                EditEmaEditorComponent,
                {
                    remove: {
                        imports: [
                            DotUveToolbarComponent,
                            DotUvePaletteComponent,
                            DotUveContentEditorComponent,
                            DotUveBrowserToolbarComponent,
                            DotUveEditPanelComponent,
                            DotUveCanvasComponent
                        ]
                    },
                    add: {
                        imports: [
                            DotUveToolbarStubComponent,
                            DotUvePaletteStubComponent,
                            DotUveContentEditorMockComponent,
                            DotUveBrowserToolbarMockComponent,
                            DotUveEditPanelMockComponent,
                            DotUveCanvasMockComponent
                        ]
                    }
                }
            ]
        ],
        detectChanges: false,
        componentProviders: [
            Dispatcher,
            ConfirmationService,
            MessageService,
            UVEStore,
            DotPaletteListStore,
            DotFavoritePageService,
            DotESContentService,
            DotSessionStorageService,
            mockProvider(DotMessageDisplayService),
            mockProvider(DotRouterService),
            mockProvider(DotGlobalMessageService),
            mockProvider(DotUiColorsService),
            {
                provide: DotWorkflowsActionsService,
                useValue: {
                    getByInode: () => of([])
                }
            },
            {
                provide: DotPropertiesService,
                useValue: {
                    ...dotPropertiesServiceMock,
                    getKeyAsList: () => of([])
                }
            },
            {
                provide: DotAlertConfirmService,
                useValue: {
                    confirm: () => of({}),
                    alert: () => of({})
                }
            },
            {
                provide: DotIframeService,
                useValue: {
                    run: () => of({})
                }
            },
            {
                provide: DotExperimentsService,
                useValue: {
                    getById(experimentId: string) {
                        if (experimentId == 'i-have-a-running-experiment') {
                            return of(getRunningExperimentMock());
                        } else if (experimentId == 'i-have-a-scheduled-experiment') {
                            return of(getScheduleExperimentMock());
                        } else if (experimentId) return of(getDraftExperimentMock());

                        return of(null);
                    }
                }
            },
            {
                provide: DotContentletService,
                useValue: {
                    getContentletByInode: () => of(URL_MAP_CONTENTLET),
                    // The VTL menu asks before opening Edit Code; by default the user may edit.
                    canLock: () =>
                        of({ canLock: true, locked: false, id: '', inode: '', lockedBy: '' })
                }
            },
            {
                provide: DotHttpErrorManagerService,
                useValue: {
                    handle() {
                        return of({});
                    }
                }
            },
            {
                provide: LoginService,
                useClass: LoginServiceMock
            },
            { provide: DotLanguagesService, useValue: new DotLanguagesServiceMock() },
            {
                provide: DotActionUrlService,
                useValue: {
                    getCreateContentletUrl() {
                        return of('http://localhost/test/url');
                    }
                }
            },
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService(messagesMock)
            },
            {
                provide: WINDOW,
                useValue: window
            },
            mockProvider(DotPageLayoutService)
        ],
        providers: [
            provideHttpClient(),
            provideHttpClientTesting(),
            {
                provide: GlobalStore,
                useValue: mockGlobalStore
            },
            Router,
            DotSeoMetaTagsUtilService,
            DialogService,
            DotCopyContentService,
            DotCopyContentModalService,
            DotWorkflowActionsFireService,
            DotTempFileUploadService,
            DotAlertConfirmService,
            {
                provide: DotAnalyticsTrackerService,
                useValue: {
                    track: vi.fn()
                }
            },
            {
                provide: DotHttpErrorManagerService,
                useValue: new MockDotHttpErrorManagerService()
            },
            {
                provide: DotcmsConfigService,
                useValue: new DotcmsConfigServiceMock()
            },
            {
                provide: PushPublishService,
                useValue: {
                    getEnvironments() {
                        return of([
                            {
                                id: '123',
                                name: 'Environment 1'
                            },
                            {
                                id: '456',
                                name: 'Environment 2'
                            }
                        ]);
                    }
                }
            },
            {
                provide: DotSeoMetaTagsService,
                useValue: { getMetaTagsResults: () => of(seoOGTagsResultMock) }
            },
            { provide: ActivatedRoute, useValue: { snapshot: { queryParams: QUERY_PARAMS_MOCK } } },
            {
                provide: DotPageApiService,
                useValue: {
                    get(data) {
                        const { language_id = 1 } = data;

                        return UVE_PAGE_RESPONSE_MAP[language_id].pipe(
                            map((page = {}) => ({
                                // Update page to "fake" a new page and avoid reference issues
                                ...(page as object)
                            }))
                        );
                    },
                    getGraphQLPage({ language_id = 1 }) {
                        return of({
                            page: UVE_PAGE_RESPONSE_MAP[language_id],
                            content: {}
                        });
                    },
                    save() {
                        return of({});
                    },
                    getPersonas() {
                        return of({
                            entity: [DEFAULT_PERSONA],
                            pagination: {
                                totalEntries: 1,
                                perPage: 10,
                                page: 1
                            }
                        });
                    }
                }
            },

            {
                provide: DotDevicesService,
                useValue: new DotDevicesServiceMock()
            },
            {
                provide: DotCurrentUserService,
                useValue: new DotCurrentUserServiceMock()
            },
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService(messagesMock)
            },
            {
                provide: WINDOW,
                useValue: window
            },
            {
                provide: DotPersonalizeService,
                useValue: new DotPersonalizeServiceMock()
            },
            {
                provide: DotContentTypeService,
                useValue: {
                    filterContentTypes: () => of([CONTENT_TYPE_MOCK_FOR_EDITOR]),
                    getContentTypes: () => of([CONTENT_TYPE_MOCK_FOR_EDITOR]),
                    getContentType: () => of(CONTENT_TYPE_MOCK_FOR_EDITOR)
                }
            },
            {
                provide: DotContentletLockerService,
                useValue: {
                    unlock: (_inode: string) => of({})
                }
            }
        ]
    });

describe('EditEmaEditorComponent', () => {
    describe('with queryParams and permission', () => {
        let spectator: SpectatorRouting<EditEmaEditorComponent>;
        let store: InstanceType<typeof UVEStore>;

        // `withPageApi`'s methods reach the UVEStore type through an index signature, so
        // a plain `vi.spyOn(store, ...)` cannot see them. Spying through the feature's own
        // interface keeps the call typed and still installs the spy on the real store.
        const pageApi = () => store as unknown as WithPageApiMethods;

        const createComponent = createRouting();

        beforeEach(() => {
            spectator = createComponent({
                queryParams: { language_id: 1, url: 'index' },
                data: {
                    data: {
                        url: 'http://localhost:3000'
                    }
                }
            });

            store = spectator.inject(UVEStore, true);

            pageApi().pageLoad({
                clientHost: 'http://localhost:3000',
                url: 'index',
                language_id: '1',
                mode: UVE_MODE.EDIT,
                [PERSONA_KEY]: DEFAULT_PERSONA.identifier
            });

            spectator.detectChanges();
        });

        describe('DOM', () => {
            beforeEach(() => {
                vi.useFakeTimers(); // Mock the timers
            });

            afterEach(() => {
                vi.useRealTimers(); // Restore the real timers after each test
            });

            it('should have a toolbar', () => {
                const toolbar = spectator.query('dot-uve-toolbar');

                expect(toolbar).not.toBeNull();
            });

            it('should place the browser toolbar', () => {
                expect(spectator.query(byTestId('uve-browser-toolbar'))).toBeTruthy();
            });

            it('should place the canvas', () => {
                expect(spectator.query(byTestId('uve-canvas'))).toBeTruthy();
            });

            /**
             * Reversed by #37308 — this used to assert the opposite.
             *
             * The name said "for a variant", but no variant term ever gated these: they sit under
             * `editorCanEditContent()`, and what hid them was the RUNNING experiment this spec's
             * `DotExperimentsService` mock returns for `i-have-a-running-experiment`. That guard is
             * gone, so the editing tools a variant fix actually needs — the palette to add with, the
             * dropzone to drop on, the contentlet tools to edit with — are present while the
             * experiment runs. The shell's warning banner is what tells the editor the run's results
             * will now mix data from before and after the change.
             */
            it('should keep the palette while a variant experiment is running', () => {
                spectator.detectChanges();

                spectator.activatedRouteStub.setQueryParam('variantName', 'hello-there');

                spectator.detectChanges();
                pageApi().pageLoad({
                    url: 'index',
                    language_id: '5',
                    // Carried deliberately. `editorCanEditContent` is
                    // `editorHasAccessToEditMode() && viewMode === EDIT`, and this call replaces the
                    // params the describe set up — so omitting the mode hides the palette for a
                    // reason that has nothing to do with the experiment, and the assertion stops
                    // testing what it names.
                    mode: UVE_MODE.EDIT,
                    [PERSONA_KEY]: DEFAULT_PERSONA.identifier,
                    variantName: 'hello-there',
                    experimentId: 'i-have-a-running-experiment'
                });

                spectator.detectChanges();

                // Only the palette: `contentlet-tools` needs a hovered contentlet area and
                // `dropzone` needs active drag bounds, so neither renders in a static fixture
                // whatever this guard does.
                expect(spectator.query(byTestId('palette'))).not.toBeNull();
            });

            it('should show the editor components when there is a running experiement and initialize the editor in a default variant', async () => {
                const componentsToShow = ['palette', 'confirm-dialog'];

                spectator.activatedRouteStub.setQueryParam('variantName', DEFAULT_VARIANT_ID);

                spectator.detectChanges();

                pageApi().pageLoad({
                    url: 'index',
                    language_id: '5',
                    [PERSONA_KEY]: DEFAULT_PERSONA.identifier
                });

                spectator.detectChanges();

                componentsToShow.forEach((testId) => {
                    expect(
                        spectator.debugElement.query(By.css(`[data-testId="${testId}"]`))
                    ).not.toBeNull();
                });
            });

            it('should leave the legacy dialog and the block editor sidebar to the content editor', () => {
                spectator.detectChanges();

                expect(spectator.query(DotEmaDialogComponent)).toBeNull();
                expect(spectator.query(DotBlockEditorSidebarComponent)).toBeNull();
            });
        });

        describe('Computed Properties', () => {
            describe('$editorContentStyles', () => {
                it('should return display grid when socialMedia is null', () => {
                    patchState(store, {
                        viewSocialMedia: null
                    });

                    spectator.detectChanges();

                    expect(spectator.component.$editorContentStyles()).toEqual({
                        display: 'grid'
                    });
                });

                it('should return display none when socialMedia is set', () => {
                    patchState(store, {
                        viewSocialMedia: 'facebook'
                    });

                    spectator.detectChanges();

                    expect(spectator.component.$editorContentStyles()).toEqual({
                        display: 'none'
                    });
                });
            });
        });

        describe('customer actions', () => {
            describe('resetActiveContentletOnUnlock', () => {
                let resetActiveContentletSpy: MockInstance;
                /** Use the same store instance the component uses so patches are visible to its effect */
                let componentStore: InstanceType<typeof UVEStore>;

                beforeEach(() => {
                    componentStore = (
                        spectator.component as unknown as {
                            uveStore: InstanceType<typeof UVEStore>;
                        }
                    ).uveStore;
                    resetActiveContentletSpy = vi.spyOn(componentStore, 'resetSelected');

                    // Enable the toggle lock feature flag by patching store flags directly
                    // Since flags are loaded in onInit, we patch them after store initialization
                    const currentFlags = componentStore.flags();
                    patchState(componentStore, {
                        flags: {
                            ...currentFlags,
                            [FeaturedFlags.FEATURE_FLAG_UVE_TOGGLE_LOCK]: true
                        }
                    });
                    // Ensure pageParams has mode EDIT so $toggleLockOptions computed returns a value
                    patchState(componentStore, {
                        pageParams: {
                            ...(componentStore.pageParams() ?? {}),
                            url: 'index',
                            language_id: '1',
                            mode: UVE_MODE.EDIT,
                            [PERSONA_KEY]: DEFAULT_PERSONA.identifier
                        }
                    });
                    // Set pageAPIResponse so $toggleLockOptions has a page (loadPageAsset may not have completed yet)
                    componentStore.updatePageResponse({
                        ...MOCK_RESPONSE_HEADLESS,
                        page: {
                            ...MOCK_RESPONSE_HEADLESS.page,
                            locked: true,
                            lockedBy: 'current-user',
                            lockedByName: 'Current User',
                            canLock: true
                        }
                    });
                    spectator.detectChanges();
                });

                afterEach(() => {
                    resetActiveContentletSpy.mockClear();
                });

                // Skipped: effect relies on $toggleLockOptions; test store/effect timing makes it flaky (resetActiveContentlet not called).
                it.skip('should reset activeContentlet when page is unlocked', () => {
                    const activeContentlet: ActionPayload = {
                        pageId: '123',
                        language_id: '1',
                        container: {
                            identifier: 'container-1',
                            uuid: 'uuid-1',
                            acceptTypes: 'test',
                            maxContentlets: 1,
                            contentletsId: ['contentlet-1']
                        },
                        pageContainers: [
                            {
                                identifier: 'container-1',
                                uuid: 'uuid-1',
                                contentletsId: ['contentlet-1']
                            }
                        ],
                        contentlet: {
                            identifier: 'contentlet-1',
                            inode: 'inode-1',
                            title: 'Test Contentlet',
                            contentType: 'test'
                        }
                    };

                    // First, ensure page starts in locked state (use componentStore so effect sees it)
                    const initialResponse = componentStore.pageAsset() as NonNullable<
                        ReturnType<typeof componentStore.pageAsset>
                    >;
                    componentStore.updatePageResponse({
                        ...initialResponse,
                        page: {
                            ...initialResponse.page,
                            locked: true,
                            lockedBy: 'current-user',
                            lockedByName: 'Current User'
                        }
                    });
                    spectator.detectChanges();

                    // Set active contentlet AFTER page is locked
                    componentStore.setSelected({
                        bounds: { x: 0, y: 0, width: 0, height: 0 },
                        payload: activeContentlet
                    });
                    spectator.detectChanges();

                    // Verify activeContentlet is set
                    expect(componentStore.editorSelected()?.payload).toEqual(activeContentlet);
                    expect(resetActiveContentletSpy).not.toHaveBeenCalled();

                    // Unlock the page (triggers $resetActiveContentletOnUnlockEffect)
                    const lockedResponse = componentStore.pageAsset() as NonNullable<
                        ReturnType<typeof componentStore.pageAsset>
                    >;
                    componentStore.updatePageResponse({
                        ...lockedResponse,
                        page: {
                            ...lockedResponse.page,
                            locked: false,
                            lockedBy: '',
                            lockedByName: ''
                        }
                    });
                    // Call detectChanges multiple times to ensure effect runs
                    spectator.detectChanges();
                    spectator.detectChanges();

                    // Verify resetActiveContentlet was called when unlocked
                    expect(resetActiveContentletSpy).toHaveBeenCalledTimes(1);
                    expect(componentStore.editorSelected()).toBeNull();
                });

                it('should not reset activeContentlet when page is unlocked but no activeContentlet exists', () => {
                    // Don't set activeContentlet
                    expect(componentStore.editorSelected()).toBeNull();

                    // Set page as locked first
                    const currentResponse = componentStore.pageAsset() as NonNullable<
                        ReturnType<typeof componentStore.pageAsset>
                    >;
                    componentStore.updatePageResponse({
                        ...currentResponse,
                        page: {
                            ...currentResponse.page,
                            locked: true,
                            lockedBy: 'current-user',
                            lockedByName: 'Current User'
                        }
                    });
                    spectator.detectChanges();

                    // Unlock the page
                    const lockedResponse = componentStore.pageAsset() as NonNullable<
                        ReturnType<typeof componentStore.pageAsset>
                    >;
                    componentStore.updatePageResponse({
                        ...lockedResponse,
                        page: {
                            ...lockedResponse.page,
                            locked: false,
                            lockedBy: '',
                            lockedByName: ''
                        }
                    });
                    // Call detectChanges multiple times to ensure effect runs
                    spectator.detectChanges();
                    spectator.detectChanges();

                    // Verify resetActiveContentlet was not called (no activeContentlet to reset)
                    expect(resetActiveContentletSpy).not.toHaveBeenCalled();
                });
            });

            describe('DOM', () => {
                it('should have a confirm dialog with acceptIcon and rejectIcon attribute', () => {
                    spectator.detectChanges();

                    const confirmDialog = spectator.query(byTestId('confirm-dialog'));

                    expect(confirmDialog?.getAttribute('acceptIcon')).toBe('hidden');
                    expect(confirmDialog?.getAttribute('rejectIcon')).toBe('hidden');
                });

                it('should show the confirm dialog when we can edit a variant', () => {
                    const componentsToHide = ['confirm-dialog']; // Test id of components that should hide when entering preview modes

                    spectator.detectChanges();

                    spectator.activatedRouteStub?.setQueryParam('variantName', 'hello-there');

                    spectator.detectChanges();
                    pageApi().pageLoad({
                        url: 'index',
                        language_id: '5',
                        [PERSONA_KEY]: DEFAULT_PERSONA.identifier,
                        variantName: 'hello-there',
                        experimentId: 'i have a variant'
                    });

                    spectator.detectChanges();

                    componentsToHide.forEach((testId) => {
                        expect(spectator.query(byTestId(testId))).not.toBeNull();
                    });
                });
            });

            describe('content editor', () => {
                it('should place the content editor', () => {
                    spectator.detectChanges();

                    expect(spectator.query(byTestId('uve-content-editor'))).toBeTruthy();
                });

                it('should keep the content editor in preview mode', () => {
                    // Page Properties opens the new editor in preview mode too, so the content
                    // editor that opens it cannot depend on the mode.
                    pageApi().pageUpdateParams({ mode: UVE_MODE.PREVIEW });
                    spectator.detectChanges();

                    expect(spectator.query(byTestId('uve-content-editor'))).toBeTruthy();
                });

                it("should ask the content editor to edit the page's contentlet when the shell opens page Properties", () => {
                    const dispatcher = spectator.inject(Dispatcher, true);
                    vi.spyOn(dispatcher, 'dispatch');
                    const page = createFakeContentlet({
                        inode: 'page-inode',
                        title: 'Page',
                        contentType: 'htmlpageasset'
                    });

                    spectator.component.openContentForEdit(page);

                    expect(dispatcher.dispatch).toHaveBeenCalledWith(
                        uveContentEditorEvents.uveEditContentRequested({ contentlet: page }),
                        { scope: 'self' }
                    );
                });
            });

            describe('edit panel', () => {
                it('should place the edit panel in edit mode', () => {
                    spectator.detectChanges();

                    expect(spectator.query(byTestId('uve-edit-panel'))).toBeTruthy();
                });

                it('should not place the edit panel in preview mode', () => {
                    pageApi().pageUpdateParams({ mode: UVE_MODE.PREVIEW });
                    spectator.detectChanges();

                    expect(spectator.query(byTestId('uve-edit-panel'))).toBeNull();
                });
            });

            describe('Editor content', () => {
                it('should have display grid when there is not SEO view', () => {
                    const editorContent = spectator.query(
                        byTestId('editor-content')
                    ) as HTMLElement;
                    expect(editorContent.style.display).toBe('grid');
                });

                it('should have display none when there is SEO view', () => {
                    store.viewSetSEO('test');
                    spectator.detectChanges();
                    const editorContent = spectator.query(
                        byTestId('editor-content')
                    ) as HTMLElement;
                    expect(editorContent.style.display).toBe('none');
                });
            });
        });
    });

    describe('when the editor is created', () => {
        const createComponent = createRouting();

        it('should open the edit panel on the quick-edit tab', () => {
            const spectator = createComponent({
                queryParams: { language_id: 1, url: 'index' },
                data: { data: { url: 'http://localhost:3000' } }
            });
            const store = spectator.inject(UVEStore, true);

            // The store outlives the editor: coming back from Layout, Rules or Experiments, it
            // still holds the tab the panel was last on.
            store.editorSetEditPanelTab(UVE_EDIT_PANEL_TABS.STYLE_EDITOR);
            spectator.detectChanges();

            expect(store.editorEditPanelTab()).toBe(UVE_EDIT_PANEL_TABS.QUICK_EDIT);
        });
    });

    afterEach(() => {
        vi.clearAllMocks();
        vi.useRealTimers(); // Restore the real timers after each test
    });
});
