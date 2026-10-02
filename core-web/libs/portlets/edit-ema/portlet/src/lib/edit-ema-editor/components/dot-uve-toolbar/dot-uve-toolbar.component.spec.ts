import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator
} from '@openng/spectator/vitest';
import { MockComponent, MockModule } from 'ng-mocks';
import { of, throwError } from 'rxjs';
import { Mock, MockInstance, describe, expect, it, vi } from 'vitest';

import { HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { HttpClientTestingModule, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component, computed, EventEmitter, model, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';

import { ConfirmationService, MessageService } from 'primeng/api';
import { DatePickerModule } from 'primeng/datepicker';

import {
    DotAnalyticsTrackerService,
    DotContentletLockerService,
    DotDevicesService,
    DotExperimentsService,
    DotLanguagesService,
    DotLicenseService,
    DotPersonalizeService,
    DotPropertiesService,
    DotWorkflowsActionsService
} from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';
import {
    DotExperiment,
    DotLanguage,
    CONFIGURE_SECTION_PARAM,
    CONFIGURE_SECTION_VARIANTS,
    DEFAULT_VARIANT_ID,
    EXPERIMENT_RETURN_PARAM,
    EXPERIMENT_RETURN_PORTLET
} from '@dotcms/dotcms-models';
import { DotExperimentsPanelStore } from '@dotcms/portlets/dot-experiments/data-access';
import { UVE_MODE } from '@dotcms/types';
import { DotLanguageSelectorComponent } from '@dotcms/ui';
import {
    DotExperimentsServiceMock,
    DotLanguagesServiceMock,
    DotLicenseServiceMock,
    getRunningExperimentMock,
    mockDotDevices
} from '@dotcms/utils-testing';

import { DotEditorModeSelectorComponent } from './components/dot-editor-mode-selector/dot-editor-mode-selector.component';
import { DotEmaBookmarksComponent } from './components/dot-ema-bookmarks/dot-ema-bookmarks.component';
import { DotEmaInfoDisplayComponent } from './components/dot-ema-info-display/dot-ema-info-display.component';
import { DotEmaRunningExperimentComponent } from './components/dot-ema-running-experiment/dot-ema-running-experiment.component';
import { DotToggleLockButtonComponent } from './components/dot-toggle-lock-button/dot-toggle-lock-button.component';
import { DotUveDeviceSelectorComponent } from './components/dot-uve-device-selector/dot-uve-device-selector.component';
import { DeviceSelectorChange } from './components/dot-uve-device-selector/dot-uve-device-selector.models';
import { DotUveWorkflowActionsComponent } from './components/dot-uve-workflow-actions/dot-uve-workflow-actions.component';
import { EditEmaPersonaSelectorComponent } from './components/edit-ema-persona-selector/edit-ema-persona-selector.component';
import { DotUveToolbarComponent } from './dot-uve-toolbar.component';

import { DotPageApiService } from '../../../services/dot-page-api/dot-page-api.service';
import {
    DEFAULT_DEVICE,
    DEFAULT_DEVICES,
    DEFAULT_PERSONA,
    PERSONA_KEY
} from '../../../shared/consts';
import { EDITOR_STATE } from '../../../shared/enums';
import {
    HEADLESS_BASE_QUERY_PARAMS,
    MOCK_RESPONSE_HEADLESS,
    MOCK_RESPONSE_VTL
} from '../../../shared/mocks';
import { DotPageAssetParams, InfoOptions } from '../../../shared/models';
import { UVEStore } from '../../../store/dot-uve.store';
import { WithPageApiMethods } from '../../../store/features/page-api/withPageApi';
import { Orientation, PageType } from '../../../store/models';
import {
    convertLocalTimeToUTC,
    createFavoritePagesURL,
    getFullPageURL,
    sanitizeURL
} from '../../../utils';

/**
 * Stub used in tests to avoid ng-mocks auto-mocking `DotLanguageSelectorComponent`,
 * which uses Angular's signal-based `viewChild()` and can crash when mocked.
 * Uses model() for value to match the real component's API so [value] binds correctly.
 * onLanguageChange matches the real component's output used by (onLanguageChange)="onLanguageSelected($event)".
 */
@Component({
    selector: 'dot-language-selector',
    template: '',
    standalone: true
})
class StubDotLanguageSelectorComponent {
    value = model<number | DotLanguage | null>(null);
    onChange = new EventEmitter<number>();
    onLanguageChange = new EventEmitter<DotLanguage>();
}

// Mock createFullURL to avoid issues with invalid URLs in tests
vi.mock('../../../utils', async () => ({
    ...(await vi.importActual('../../../utils')),
    createFullURL: vi.fn((params, siteId) => {
        const { url = '/', clientHost = 'http://localhost:3000' } = params;
        return `${clientHost}${url}?siteId=${siteId}&version=true`;
    })
}));

const API_URL = '/api/v1/page/json/123-xyz-567-xxl?host_id=123-xyz-567-xxl&language_id=1';

const params: DotPageAssetParams = HEADLESS_BASE_QUERY_PARAMS;

/**
 * The UVE Experiments entry-point switch, as the toolbar reads it (#37005).
 *
 * Only the return leg's *fallback* consults it — a variant carrying an origin marker returns to
 * where it came from at either value (FR-027) — so most tests here leave it alone.
 */
const $experimentsPortletSwitchSignal = signal(false);

/**
 * The URL's query params, as `ActivatedRoute` reports them — the frozen half of the address.
 *
 * The gesture handlers still read here: the origin marker is a routing concern that survives
 * `DotPageAssetParams` only through `#getPageParams`'s `as` cast, and a gesture happens on the
 * toolbar the navigation built, where the snapshot is still true.
 *
 * The chip does not, and cannot — see {@link setAddress}.
 */
let routeQueryParams: Record<string, string> = {};
const url = sanitizeURL(params?.url);

const pageAPIQueryParams = getFullPageURL({ url, params });
const pageAPI = `/api/v1/page/${'json'}/${pageAPIQueryParams}`;
const pageAssetResponse = MOCK_RESPONSE_HEADLESS;
const shouldShowInfoDisplay = pageAssetResponse?.page.locked;
const bookmarksUrl = createFavoritePagesURL({
    languageId: Number(params?.language_id),
    pageURI: url,
    siteId: pageAssetResponse?.site.identifier
});

const baseUVEToolbarState = {
    editor: {
        bookmarksUrl,
        apiUrl: `${'http://localhost'}${pageAPI}`
    },
    preview: null,
    currentLanguage: pageAssetResponse?.viewAs?.language,
    urlContentMap: null,
    runningExperiment: null,
    workflowActionsInode: pageAssetResponse?.page.inode,
    showInfoDisplay: shouldShowInfoDisplay
};

// Mutable signals for test control (computed properties that tests need to mutate)
// Each of these stands in for a store signal, so it is annotated with what that signal declares.
// Seeded bare, `signal(null)` infers `WritableSignal<null>` and `signal(undefined)` infers
// `WritableSignal<undefined>` — so every `.set(realValue)` below was an error, 20 of them in this
// file alone. The types are `$lockOptions`, `$infoDisplayProps` and `$urlContentMap` from the store.
const showWorkflowsActionsSignal = signal(true);
const toggleLockOptionsSignal = signal<WorkflowLockOptions | null>(null);
const infoDisplayPropsSignal = signal<InfoOptions | null>(null);
const urlContentMapSignal = signal<DotCMSURLContentMap | null>(null);

// Separate signals for view state properties (for test control)
const deviceSignal = signal(
    DEFAULT_DEVICES.find((device) => device.inode === DEFAULT_DEVICE.inode)
);
const socialMediaSignal = signal<string | null>(null);
const orientationSignal = signal(Orientation.LANDSCAPE);
const viewParamsSignal = signal({
    seo: undefined,
    device: undefined,
    orientation: undefined
});

// View signal that returns ViewState object
const viewSignal = computed(() => ({
    device: deviceSignal(),
    socialMedia: socialMediaSignal(),
    orientation: orientationSignal(),
    viewParams: viewParamsSignal(),
    isEditState: true,
    isPreviewModeActive: false,
    ogTagsResults: null
}));

// Mutable signal for pageParams control (for test control)
const pageParamsSignal = signal({ ...params, mode: UVE_MODE.EDIT });

/**
 * Put the editor at an address — both halves of it.
 *
 * The toolbar reads the same query params from two places, and a test that writes only one of
 * them describes a state the editor is never in. `ActivatedRoute.snapshot` is frozen at the
 * navigation that built the toolbar, because UVE writes its own address with `Location.go` and
 * the router never hears about it; `pageParams` is the half that stays current, so it is what the
 * chip is derived from and what the return leg clears.
 *
 * `mode` is carried over rather than reset: it is the editor's own state, not part of the address
 * a test is describing here.
 */
const setAddress = (queryParams: Record<string, string>) => {
    routeQueryParams = queryParams;
    pageParamsSignal.set({
        ...params,
        mode: pageParamsSignal().mode,
        ...queryParams
    } as ReturnType<typeof pageParamsSignal>);
};
const pageSnapshotSignal = signal({
    ...MOCK_RESPONSE_VTL,
    clientResponse: MOCK_RESPONSE_VTL
});
const $lockFeatureEnabledSignal = signal(true);

/** Shared language list used across store mocks. */
const MOCK_PAGE_LANGUAGES = [
    {
        id: 1,
        language: 'English',
        languageCode: 'en',
        countryCode: 'US',
        country: 'United States',
        translated: true
    },
    {
        id: 2,
        language: 'Spanish',
        languageCode: 'es',
        countryCode: 'ES',
        country: 'Spain',
        translated: false
    },
    {
        id: 3,
        language: 'French',
        languageCode: 'fr',
        countryCode: 'FR',
        country: 'France',
        translated: true
    }
];

const baseUVEState = {
    $uveToolbar: signal(baseUVEToolbarState),
    viewSetDevice: vi.fn(),
    viewSetSEO: vi.fn(),
    viewSetOrientation: vi.fn(),
    pageParams: pageParamsSignal,
    pageAsset: pageSnapshotSignal,
    // View state signal
    view: viewSignal,
    // Computed properties (most are functions, some are mutable signals for test control)
    $apiURL: () => API_URL,
    viewMode: computed(() => pageParamsSignal()?.mode ?? UVE_MODE.UNKNOWN),
    pageLanguage: signal({
        id: 1,
        language: 'English',
        languageCode: 'en',
        countryCode: 'US',
        country: 'United States',
        translated: true
    }),
    $showWorkflowsActions: showWorkflowsActionsSignal, // Mutable for tests
    $personaSelector: () => ({
        pageId: pageAssetResponse?.page.identifier,
        value: pageAssetResponse?.viewAs?.persona ?? DEFAULT_PERSONA
    }),
    $infoDisplayProps: infoDisplayPropsSignal, // Mutable for tests
    $urlContentMap: urlContentMapSignal, // Mutable for tests
    $lockOptions: toggleLockOptionsSignal, // Mutable for tests
    $lockFeatureEnabled: $lockFeatureEnabledSignal,
    pageReload: vi.fn(),
    editorPaletteOpen: signal(true),
    editorCanEditContent: signal(true),
    pageLanguages: signal(MOCK_PAGE_LANGUAGES),
    pageExperiment: signal<DotExperiment | null>(null),
    viewDevice: deviceSignal,
    viewSocialMedia: socialMediaSignal,
    viewDeviceOrientation: orientationSignal,
    workflowIsLoading: signal(false),
    workflowLockIsLoading: signal(false),
    // Both merge into `pageParams`, as the real store's do. A bare `vi.fn()` would let a test
    // assert the call and still describe an editor whose params never changed — which is the
    // state the chip is derived from.
    pageLoad: vi.fn((next: Record<string, unknown>) =>
        pageParamsSignal.update((current) => ({ ...current, ...next }))
    ),
    pageUpdateParams: vi.fn((next: Record<string, unknown>) =>
        pageParamsSignal.update((current) => ({ ...current, ...next }))
    ),
    $isPreviewMode: signal(false),
    $isLiveMode: signal(false),
    $isEditMode: signal(false),
    viewParams: viewParamsSignal,
    languages: signal(MOCK_PAGE_LANGUAGES),
    patchViewParams: vi.fn(),
    orientation: orientationSignal, // Use the shared signal
    viewClearDeviceAndSocialMedia: vi.fn(),
    device: deviceSignal, // Use the shared signal
    lockLoading: signal(false),
    workflowToggleLock: vi.fn(),
    socialMedia: socialMediaSignal, // Use the shared signal
    trackUVECalendarChange: vi.fn(),
    pageType: signal(PageType.TRADITIONAL),
    isTraditionalPage: signal(true),
    experiment: signal(null),
    editor: () => ({
        panels: {
            palette: {
                open: signal(false)
            },
            rightSidebar: {
                open: false
            }
        },
        dragItem: null,
        bounds: [],
        state: EDITOR_STATE.IDLE,
        activeContentlet: null,
        contentArea: null,
        ogTags: null,
        styleSchemas: []
    }),
    setPaletteOpen: vi.fn()
};

/** Creates lock options for tests. Defaults to unlocked state; pass overrides for locked/disabled cases. */
function createLockOptions(
    overrides: Partial<{
        inode: string;
        isLocked: boolean;
        lockedBy: string;
        canLock: boolean;
        isLockedByCurrentUser: boolean;
        shouldShowButton: boolean;
    }> = {}
) {
    return {
        inode: 'test-inode',
        isLocked: false,
        lockedBy: '',
        canLock: true,
        isLockedByCurrentUser: false,
        shouldShowButton: true,
        ...overrides
    };
}

/** Extracts the accept callback from the confirmation dialog (for personalization tests). */
function getConfirmationAcceptCallback(confirmationService: ConfirmationService): () => void {
    return (confirmationService.confirm as Mock).mock.calls[0][0].accept;
}

/** Shared info display props for variant/device/socialMedia tests. */
const INFO_DISPLAY_VARIANT_PROPS = {
    info: { message: 'editpage.editing.variant', args: ['Variant A'] },
    icon: 'pi pi-file-edit',
    id: 'variant',
    actionIcon: 'pi pi-arrow-left'
};

const personaEventMock = {
    identifier: '123',
    pageId: '123',
    personalized: true,
    archived: false,
    baseType: 'PERSONA',
    contentType: 'persona',
    folder: 'SYSTEM_FOLDER',
    hasLiveVersion: false,
    hasTitleImage: false,
    host: 'SYSTEM_HOST',
    hostName: 'System Host',
    inode: '',
    keyTag: 'dot:persona',
    languageId: 1,
    live: false,
    locked: false,
    modDate: '0',
    modUser: 'system',
    modUserName: 'system user system user',
    name: 'Test Persona',
    owner: 'SYSTEM_USER',
    sortOrder: 0,
    stInode: 'c938b15f-bcb6-49ef-8651-14d455a97045',
    title: 'Test Persona',
    titleImage: 'TITLE_IMAGE_NOT_FOUND',
    url: 'demo.dotcms.com',
    working: false
};

describe('DotUveToolbarComponent', () => {
    let spectator: Spectator<DotUveToolbarComponent>;
    let store: InstanceType<typeof UVEStore>;

    // `withPageApi`'s methods reach the UVEStore type through an index signature, so
    // a plain `vi.spyOn(store, ...)` cannot see them. Spying through the feature's own
    // interface keeps the call typed and still installs the spy on the real store.
    const pageApi = () => store as unknown as WithPageApiMethods;
    let messageService: MessageService;
    let confirmationService: ConfirmationService;
    let devicesService: DotDevicesService;
    let personalizeService: DotPersonalizeService;

    // Do NOT use fake timers globally: PrimeNG's equals() (deepEquals) calls .getTime() on
    // values it thinks are Date. With Jest fake timers or Date instanceof mocks, that breaks.
    // Use real timers; use fake timers only in specific calendar tests that need a fixed "today".

    const createComponent = createComponentFactory({
        component: DotUveToolbarComponent,
        overrideComponents: [
            [
                DotUveToolbarComponent,
                {
                    remove: {
                        imports: [DotLanguageSelectorComponent]
                    },
                    add: {
                        imports: [StubDotLanguageSelectorComponent]
                    }
                }
            ]
        ],
        imports: [
            HttpClientTestingModule,
            FormsModule,
            MockModule(DatePickerModule),
            MockComponent(DotEmaBookmarksComponent),
            DotEmaInfoDisplayComponent,
            MockComponent(DotEmaRunningExperimentComponent),
            DotToggleLockButtonComponent,
            MockComponent(EditEmaPersonaSelectorComponent),
            MockComponent(DotUveWorkflowActionsComponent),
            DotUveDeviceSelectorComponent,
            MockComponent(DotEditorModeSelectorComponent)
        ],
        providers: [
            UVEStore,
            provideHttpClientTesting(),
            {
                provide: DotAnalyticsTrackerService,
                useValue: {
                    track: vi.fn()
                }
            },
            mockProvider(DotContentletLockerService, {
                unlock: vi.fn().mockReturnValue(of({}))
            }),
            mockProvider(ConfirmationService, {
                confirm: vi.fn()
            }),
            mockProvider(DotWorkflowsActionsService, {
                getByInode: () => of([])
            }),
            {
                provide: DotLanguagesService,
                useValue: new DotLanguagesServiceMock()
            },
            {
                provide: DotExperimentsService,
                useValue: DotExperimentsServiceMock
            },
            {
                provide: DotLicenseService,
                useValue: new DotLicenseServiceMock()
            },
            {
                provide: DotPageApiService,
                useValue: {
                    get: () => of(MOCK_RESPONSE_HEADLESS)
                }
            },
            {
                provide: LoginService,
                useValue: {
                    getCurrentUser: () => of({})
                }
            },
            {
                provide: MessageService,
                useValue: {
                    add: vi.fn()
                }
            },
            {
                provide: Router,
                // `useFactory`, not `useValue`: a `useValue` object literal is evaluated once at
                // module scope, so its `vi.fn()` would accumulate calls across every test.
                useFactory: () => ({ navigate: vi.fn() })
            },
            {
                provide: ActivatedRoute,
                useValue: {
                    get snapshot() {
                        return { queryParams: routeQueryParams };
                    }
                }
            },
            {
                provide: DotPropertiesService,
                useValue: {
                    // The switch reads the raw key rather than a normalised flag, so that a
                    // response missing it fails closed — see `readExperimentsPortletSwitch`.
                    getKey: () => of(String($experimentsPortletSwitchSignal()))
                }
            },
            {
                provide: DotDevicesService,
                useValue: {
                    get: vi.fn().mockReturnValue(of(mockDotDevices))
                }
            }
        ],
        componentProviders: [
            { provide: DotExperimentsPanelStore, useFactory: () => panelStore },
            {
                provide: DotPersonalizeService,
                useValue: {
                    getPersonalize: vi.fn(),
                    personalized: vi.fn().mockReturnValue(of({}))
                }
            }
        ]
    });

    /**
     * The UVE panel's store, provided by the shell in the app. `null` here unless a test is about
     * the panel — its presence is what the toolbar reads to tell the two worlds apart (#37478).
     */
    let panelStore: {
        suspendedForVariant: Mock;
        resumeFromVariant: Mock;
        openVariants: Mock;
        experimentId: Mock;
        openResults?: Mock;
    } | null = null;

    describe('base state', () => {
        beforeEach(() => {
            $lockFeatureEnabledSignal.set(true);
            spectator = createComponent({
                providers: [mockProvider(UVEStore, baseUVEState)]
            });
            store = spectator.inject(UVEStore, true);
            messageService = spectator.inject(MessageService, true);
            devicesService = spectator.inject(DotDevicesService);
            confirmationService = spectator.inject(ConfirmationService, true);
            personalizeService = spectator.inject(DotPersonalizeService, true);
        });

        afterEach(() => {
            vi.clearAllMocks();
        });

        it('should have a dot-uve-workflow-actions component', () => {
            const workflowActions = spectator.query(DotUveWorkflowActionsComponent);
            expect(workflowActions).toBeTruthy();
        });

        describe('Events', () => {
            it('should emit editUrlContentMap', () => {
                const contentlet = {
                    identifier: '123',
                    inode: '456',
                    title: 'My super awesome blog post',
                    contentType: 'Blog'
                };
                const spy = vi.spyOn(spectator.component.editUrlContentMap, 'emit');

                // The edit URL content map button is only rendered in EDIT mode and when the map exists
                baseUVEState.pageParams.set({ ...params, mode: UVE_MODE.EDIT });
                // The fixture is the slice this test needs; `DotCMSURLContentMap` extends
                // `DotCMSBasicContentlet`, whose ~30 required fields none of these assertions read.
                baseUVEState.$urlContentMap.set(contentlet as unknown as DotCMSURLContentMap);
                spectator.detectChanges();

                const button = spectator.query(byTestId('edit-url-content-map'));

                spectator.click(button!);

                expect(spy).toHaveBeenCalledWith(contentlet);
            });
        });

        describe('custom devices', () => {
            it('should get custom devices', () => {
                expect(devicesService.get).toHaveBeenCalled();
            });

            it('should set default devices and custom devices', () => {
                expect(spectator.component.$devices()).toEqual([
                    ...DEFAULT_DEVICES,
                    ...mockDotDevices
                ]);
            });
        });

        describe('editor mode selector', () => {
            it('should have editor mode selector', () => {
                expect(spectator.query(byTestId('uve-toolbar-editor-mode-selector'))).toBeTruthy();
            });
        });

        describe('dot-ema-bookmarks', () => {
            it('should pass bookmarks URL to dot-ema-bookmarks component', () => {
                const bookmarks = spectator.query(DotEmaBookmarksComponent)!;

                expect(bookmarks.url).toBe('/test-url?host_id=123-xyz-567-xxl&language_id=1');
            });
        });

        describe('dot-ema-running-experiment', () => {
            it('should be null', () => {
                expect(spectator.query(byTestId('uve-toolbar-running-experiment'))).toBeNull();
            });
        });

        describe('API URL', () => {
            it('should have api link button', () => {
                expect(spectator.query(byTestId('uve-toolbar-api-link'))).toBeTruthy();
            });

            it('should have api link button with correct href', () => {
                const btn = spectator.query(byTestId('uve-toolbar-api-link'))!;
                expect(btn.getAttribute('href')).toBe(API_URL);
            });
        });

        describe('dot-edit-ema-persona-selector', () => {
            it('should pass pageId and default persona value to persona selector', () => {
                const personaSelector = spectator.query(EditEmaPersonaSelectorComponent)!;

                expect(personaSelector.pageId).toBe('123');
                expect(personaSelector.value).toEqual({
                    archived: false,
                    baseType: 'PERSONA',
                    contentType: 'persona',
                    folder: 'SYSTEM_FOLDER',
                    hasLiveVersion: false,
                    hasTitleImage: false,
                    host: 'SYSTEM_HOST',
                    hostName: 'System Host',
                    identifier: 'modes.persona.no.persona',
                    inode: '',
                    keyTag: 'dot:persona',
                    languageId: 1,
                    live: false,
                    locked: false,
                    modDate: '0',
                    modUser: 'system',
                    modUserName: 'system user system user',
                    name: 'Default Visitor',
                    owner: 'SYSTEM_USER',
                    personalized: false,
                    sortOrder: 0,
                    stInode: 'c938b15f-bcb6-49ef-8651-14d455a97045',
                    title: 'Default Visitor',
                    titleImage: 'TITLE_IMAGE_NOT_FOUND',
                    url: 'demo.dotcms.com',
                    working: false
                });
            });

            it('should personalize without confirmation when page is already personalized', () => {
                const pageLoadSpy = vi.spyOn(pageApi(), 'pageLoad');
                spectator.triggerEventHandler(EditEmaPersonaSelectorComponent, 'selected', {
                    ...personaEventMock,
                    personalized: true
                });
                spectator.detectChanges();

                expect(pageLoadSpy).toHaveBeenCalledWith({
                    [PERSONA_KEY]: '123'
                });
            });

            it('should show confirmation dialog when personalizing non-personalized page', () => {
                spectator.triggerEventHandler(EditEmaPersonaSelectorComponent, 'selected', {
                    ...personaEventMock,
                    personalized: false
                });
                spectator.detectChanges();

                expect(confirmationService.confirm).toHaveBeenCalledWith({
                    accept: expect.any(Function),
                    acceptLabel: 'dot.common.dialog.accept',
                    header: 'editpage.personalization.confirm.header',
                    message: 'editpage.personalization.confirm.message',
                    reject: expect.any(Function),
                    rejectLabel: 'dot.common.dialog.reject'
                });
            });

            it('should handle error when personalization confirmation fails', () => {
                const spyPersonalized = vi.spyOn(personalizeService, 'personalized');
                const spyMessageService = vi.spyOn(messageService, 'add');

                spectator.triggerEventHandler(EditEmaPersonaSelectorComponent, 'selected', {
                    ...personaEventMock,
                    personalized: false
                });

                spyPersonalized.mockReturnValue(
                    throwError(() => new Error('Personalization confirmation failed'))
                );

                getConfirmationAcceptCallback(confirmationService)();
                spectator.detectChanges();

                expect(spyMessageService).toHaveBeenCalledWith({
                    severity: 'error',
                    summary: 'error',
                    detail: 'uve.personalize.empty.page.error'
                });
            });

            it('should show backend message from error-message header when personalization API returns HttpErrorResponse', () => {
                const spyPersonalized = vi.spyOn(personalizeService, 'personalized');
                const spyMessageService = vi.spyOn(messageService, 'add');

                spectator.triggerEventHandler(EditEmaPersonaSelectorComponent, 'selected', {
                    ...personaEventMock,
                    personalized: false
                });

                const backendMessage =
                    'Does not exists a Persona with the tag: nonexistent-persona';
                spyPersonalized.mockReturnValue(
                    throwError(
                        () =>
                            new HttpErrorResponse({
                                status: 400,
                                headers: new HttpHeaders({ 'error-message': backendMessage })
                            })
                    )
                );

                getConfirmationAcceptCallback(confirmationService)();
                spectator.detectChanges();

                expect(spyMessageService).toHaveBeenCalledWith({
                    severity: 'error',
                    summary: 'error',
                    detail: backendMessage
                });
            });

            it('should show backend message from body error when personalization API returns HttpErrorResponse with error body', () => {
                const spyPersonalized = vi.spyOn(personalizeService, 'personalized');
                const spyMessageService = vi.spyOn(messageService, 'add');

                spectator.triggerEventHandler(EditEmaPersonaSelectorComponent, 'selected', {
                    ...personaEventMock,
                    personalized: false
                });

                spyPersonalized.mockReturnValue(
                    throwError(
                        () =>
                            new HttpErrorResponse({
                                status: 400,
                                error: {
                                    error: 'dotcms.api.error.bad_request: Page parameter is missing'
                                }
                            })
                    )
                );

                getConfirmationAcceptCallback(confirmationService)();
                spectator.detectChanges();

                expect(spyMessageService).toHaveBeenCalledWith({
                    severity: 'error',
                    summary: 'error',
                    detail: 'Page parameter is missing'
                });
            });

            it('should show full body error when response has no colon prefix', () => {
                const spyPersonalized = vi.spyOn(personalizeService, 'personalized');
                const spyMessageService = vi.spyOn(messageService, 'add');

                spectator.triggerEventHandler(EditEmaPersonaSelectorComponent, 'selected', {
                    ...personaEventMock,
                    personalized: false
                });

                const bodyMessage = 'Something went wrong';
                spyPersonalized.mockReturnValue(
                    throwError(
                        () =>
                            new HttpErrorResponse({
                                status: 500,
                                error: { error: bodyMessage }
                            })
                    )
                );

                getConfirmationAcceptCallback(confirmationService)();
                spectator.detectChanges();

                expect(spyMessageService).toHaveBeenCalledWith({
                    severity: 'error',
                    summary: 'error',
                    detail: bodyMessage
                });
            });

            it('should use i18n fallback when error-message header is only whitespace', () => {
                const spyPersonalized = vi.spyOn(personalizeService, 'personalized');
                const spyMessageService = vi.spyOn(messageService, 'add');

                spectator.triggerEventHandler(EditEmaPersonaSelectorComponent, 'selected', {
                    ...personaEventMock,
                    personalized: false
                });

                spyPersonalized.mockReturnValue(
                    throwError(
                        () =>
                            new HttpErrorResponse({
                                status: 400,
                                headers: new HttpHeaders({ 'error-message': '   \t  ' })
                            })
                    )
                );

                getConfirmationAcceptCallback(confirmationService)();
                spectator.detectChanges();

                expect(spyMessageService).toHaveBeenCalledWith({
                    severity: 'error',
                    summary: 'error',
                    detail: 'uve.personalize.empty.page.error'
                });
            });

            it('should use i18n fallback when body error is only whitespace', () => {
                const spyPersonalized = vi.spyOn(personalizeService, 'personalized');
                const spyMessageService = vi.spyOn(messageService, 'add');

                spectator.triggerEventHandler(EditEmaPersonaSelectorComponent, 'selected', {
                    ...personaEventMock,
                    personalized: false
                });

                spyPersonalized.mockReturnValue(
                    throwError(
                        () =>
                            new HttpErrorResponse({
                                status: 400,
                                error: { error: '   ' }
                            })
                    )
                );

                getConfirmationAcceptCallback(confirmationService)();
                spectator.detectChanges();

                expect(spyMessageService).toHaveBeenCalledWith({
                    severity: 'error',
                    summary: 'error',
                    detail: 'uve.personalize.empty.page.error'
                });
            });

            it('should show confirmation when depersonalizing', () => {
                spectator.triggerEventHandler(EditEmaPersonaSelectorComponent, 'despersonalize', {
                    ...personaEventMock,
                    personalized: true,
                    selected: true
                });

                spectator.detectChanges();

                expect(confirmationService.confirm).toHaveBeenCalledWith({
                    accept: expect.any(Function),
                    acceptLabel: 'dot.common.dialog.accept',
                    header: 'editpage.personalization.delete.confirm.header',
                    message: 'editpage.personalization.delete.confirm.message',
                    rejectLabel: 'dot.common.dialog.reject'
                });
            });
        });

        describe('language selector', () => {
            it('should have language selector', () => {
                expect(spectator.query(byTestId('uve-toolbar-language-selector'))).toBeTruthy();
            });

            it('should call pageLoad with language_id when selected language has translation', () => {
                const spyLoadPageAsset = vi.spyOn(baseUVEState, 'pageLoad');
                const languageWithTranslation = MOCK_PAGE_LANGUAGES[0]; // English, id 1, translated: true

                spectator.triggerEventHandler(
                    StubDotLanguageSelectorComponent,
                    'onLanguageChange',
                    languageWithTranslation
                );

                expect(spyLoadPageAsset).toHaveBeenCalledWith({ language_id: '1' });
            });

            it('should call confirmationService.confirm when selected language has no translation', () => {
                const spyConfirmationService = vi.spyOn(confirmationService, 'confirm');
                const languageWithoutTranslation = MOCK_PAGE_LANGUAGES[1]; // Spanish, id 2, translated: false

                spectator.triggerEventHandler(
                    StubDotLanguageSelectorComponent,
                    'onLanguageChange',
                    languageWithoutTranslation
                );
                spectator.detectChanges();

                expect(spyConfirmationService).toHaveBeenCalled();
            });

            it('should emit translatePage with page and newLanguage when user confirms new translation', () => {
                const translatePageSpy = vi.spyOn(spectator.component.translatePage, 'emit');
                const languageWithoutTranslation = MOCK_PAGE_LANGUAGES[1]; // Spanish, id 2, translated: false

                spectator.triggerEventHandler(
                    StubDotLanguageSelectorComponent,
                    'onLanguageChange',
                    languageWithoutTranslation
                );
                spectator.detectChanges();

                const acceptCallback = (confirmationService.confirm as Mock).mock.calls[0][0]
                    .accept;
                acceptCallback();

                const expectedPage = pageSnapshotSignal().page;
                expect(translatePageSpy).toHaveBeenCalledWith({
                    page: expectedPage,
                    newLanguage: 2
                });
            });

            it('should reset language selector to current language when user rejects new translation', () => {
                const languageWithoutTranslation = MOCK_PAGE_LANGUAGES[1]; // Spanish, id 2, translated: false
                const currentLanguage = baseUVEState.pageLanguage();
                const languageSelector = spectator.query(StubDotLanguageSelectorComponent);
                const valueSetSpy = vi.spyOn(languageSelector!.value, 'set');

                spectator.triggerEventHandler(
                    StubDotLanguageSelectorComponent,
                    'onLanguageChange',
                    languageWithoutTranslation
                );
                spectator.detectChanges();

                const rejectCallback = (confirmationService.confirm as Mock).mock.calls[0][0]
                    .reject;
                rejectCallback();

                expect(valueSetSpy).toHaveBeenCalledWith(currentLanguage);
            });
        });

        it('should not show experiments button when no experiment is running', () => {
            expect(spectator.query(byTestId('uve-toolbar-running-experiment'))).toBeFalsy();
        });

        it('should have persona selector', () => {
            expect(spectator.query(byTestId('uve-toolbar-persona-selector'))).toBeTruthy();
        });

        describe('toggle lock button', () => {
            it('should not display toggle lock button when shouldShowButton is false', () => {
                baseUVEState.$lockOptions.set(createLockOptions({ shouldShowButton: false }));
                spectator.detectChanges();

                expect(spectator.query(byTestId('toggle-lock-button'))).toBeNull();
            });

            it('should display toggle lock button when toggle lock options are available', () => {
                baseUVEState.$lockOptions.set(createLockOptions());
                spectator.detectChanges();

                expect(spectator.query(byTestId('toggle-lock-button'))).toBeTruthy();
            });

            it('should display unlocked state when page is not locked', () => {
                baseUVEState.$lockOptions.set(createLockOptions());
                spectator.detectChanges();

                const button = spectator.query(byTestId('toggle-lock-button')) as HTMLElement;
                expect(button).toBeTruthy();
                expect(button.classList.contains('lock-button--unlocked')).toBeTruthy();
                expect(button.classList.contains('lock-button--locked')).toBeFalsy();
            });

            it('should display locked state when page is locked by current user', () => {
                baseUVEState.$lockOptions.set(
                    createLockOptions({
                        inode: 'test-inode',
                        isLocked: true,
                        lockedBy: 'current-user',
                        isLockedByCurrentUser: true
                    })
                );
                spectator.detectChanges();

                const button = spectator.query(byTestId('toggle-lock-button')) as HTMLElement;
                expect(button).toBeTruthy();
                expect(button.classList.contains('lock-button--locked')).toBeTruthy();
                expect(button.classList.contains('lock-button--unlocked')).toBeFalsy();
            });

            it('should call store.toggleLock when unlocked button is clicked', () => {
                const spy = vi.spyOn(store, 'workflowToggleLock');

                baseUVEState.$lockOptions.set(createLockOptions({ inode: 'test-inode-unlock' }));
                spectator.detectChanges();

                const button = spectator.query(byTestId('toggle-lock-button'));
                spectator.click(button!);

                expect(spy).toHaveBeenCalledWith('test-inode-unlock', false, false, undefined);
            });

            it('should call store.toggleLock when locked button is clicked', () => {
                const spy = vi.spyOn(store, 'workflowToggleLock');

                baseUVEState.$lockOptions.set(
                    createLockOptions({
                        inode: 'test-inode-lock',
                        isLocked: true,
                        lockedBy: 'current-user',
                        isLockedByCurrentUser: true
                    })
                );
                spectator.detectChanges();

                const button = spectator.query(byTestId('toggle-lock-button'));
                spectator.click(button!);

                expect(spy).toHaveBeenCalledWith('test-inode-lock', true, true, undefined);
            });

            it('should disable button when lock operation is loading', () => {
                baseUVEState.$lockOptions.set(createLockOptions());
                baseUVEState.workflowLockIsLoading.set(true);
                spectator.detectChanges();

                const button = spectator.query(byTestId('toggle-lock-button')) as HTMLButtonElement;
                expect(button?.disabled).toBe(true);
            });

            it('should enable button when lock operation is not loading', () => {
                baseUVEState.$lockOptions.set(createLockOptions());
                baseUVEState.workflowLockIsLoading.set(false);
                spectator.detectChanges();

                const button = spectator.query(byTestId('toggle-lock-button')) as HTMLButtonElement;
                expect(button?.disabled).toBe(false);
            });

            it('should call store.toggleLock with correct params for page locked by another user', () => {
                const spy = vi.spyOn(store, 'workflowToggleLock');

                baseUVEState.$lockOptions.set(
                    createLockOptions({
                        inode: 'test-inode-other',
                        isLocked: true,
                        lockedBy: 'another-user',
                        isLockedByCurrentUser: false
                    })
                );
                spectator.detectChanges();

                const button = spectator.query(byTestId('toggle-lock-button'));
                spectator.click(button!);

                expect(spy).toHaveBeenCalledWith('test-inode-other', true, false, undefined);
            });
        });
    });

    describe('preview', () => {
        beforeEach(() => {
            pageParamsSignal.set({ ...params, mode: UVE_MODE.PREVIEW });
        });

        const previewBaseUveState = {
            ...baseUVEState,
            $isPreviewMode: signal(true)
        };

        beforeEach(() => {
            spectator = createComponent({
                providers: [mockProvider(UVEStore, previewBaseUveState)]
            });

            store = spectator.inject(UVEStore, true);
        });

        it('should have a dot-ema-bookmarks component', () => {
            expect(spectator.query(DotEmaBookmarksComponent)).toBeTruthy();
        });

        it('should have a api link button', () => {
            expect(spectator.query(byTestId('uve-toolbar-api-link'))).toBeTruthy();
        });

        it('should have a device selector', () => {
            expect(spectator.query(byTestId('uve-toolbar-device-selector'))).toBeTruthy();
        });

        it('should not have experiments', () => {
            spectator.detectChanges();
            expect(spectator.query(byTestId('uve-toolbar-running-experiment'))).toBeFalsy();
        });

        it('should not have a dot-uve-workflow-actions component', () => {
            baseUVEState.$showWorkflowsActions.set(false);
            spectator.detectChanges();

            const workflowActions = spectator.query(DotUveWorkflowActionsComponent);

            expect(workflowActions).toBeNull();
        });

        describe('calendar', () => {
            it('should not show calendar when in preview mode', () => {
                spectator.detectChanges();

                expect(spectator.query('p-datepicker')).toBeFalsy();
            });
        });
    });
    describe('live', () => {
        const liveSocialMediaSignal = signal<string | null>(null);
        const liveViewModeSignal = signal(UVE_MODE.LIVE);
        const liveBaseUveState = {
            ...baseUVEState,
            $isPreviewMode: signal(false),
            $isLiveMode: signal(true),
            viewMode: computed(() => liveViewModeSignal()),
            viewSocialMedia: liveSocialMediaSignal
        };

        beforeEach(() => {
            spectator = createComponent({
                providers: [mockProvider(UVEStore, liveBaseUveState)]
            });

            store = spectator.inject(UVEStore, true);
        });

        it('should have a dot-ema-bookmarks component', () => {
            expect(spectator.query(DotEmaBookmarksComponent)).toBeTruthy();
        });

        it('should have a api link button', () => {
            expect(spectator.query(byTestId('uve-toolbar-api-link'))).toBeTruthy();
        });

        it('should have a device selector', () => {
            expect(spectator.query(byTestId('uve-toolbar-device-selector'))).toBeTruthy();
        });

        it('should not have experiments', () => {
            spectator.detectChanges();
            expect(spectator.query(byTestId('uve-toolbar-running-experiment'))).toBeFalsy();
        });

        it('should not have a dot-uve-workflow-actions component', () => {
            baseUVEState.$showWorkflowsActions.set(false);
            spectator.detectChanges();

            const workflowActions = spectator.query(DotUveWorkflowActionsComponent);

            expect(workflowActions).toBeNull();
        });

        describe('calendar', () => {
            // Do not mock Date Symbol.hasInstance or Date.UTC: PrimeNG's equals() uses
            // (x instanceof Date) then x.getTime(). Making instanceof always true causes
            // getTime() to be called on non-Date values and throws.

            beforeEach(() => {
                liveViewModeSignal.set(UVE_MODE.LIVE);
                liveSocialMediaSignal.set(null);
            });

            it('should show calendar when in live mode', () => {
                liveSocialMediaSignal.set(null);
                spectator.detectChanges();

                expect(spectator.query('p-datepicker')).toBeTruthy();
            });

            it('should show calendar when in live mode and socialMedia is false', () => {
                liveSocialMediaSignal.set(null);
                spectator.detectChanges();

                expect(spectator.query('p-datepicker')).toBeTruthy();
            });

            it('should not show calendar when socialMedia has a value', () => {
                liveSocialMediaSignal.set('facebook');
                spectator.detectChanges();

                expect(spectator.query('p-datepicker')).toBeFalsy();
            });

            it('should not show calendar when not in live mode', () => {
                liveSocialMediaSignal.set(null);
                liveViewModeSignal.set(UVE_MODE.EDIT);
                spectator.detectChanges();

                expect(spectator.query('p-datepicker')).toBeFalsy();
            });

            it('should have a minDate of current date on 0h 0min 0s 0ms', () => {
                liveViewModeSignal.set(UVE_MODE.LIVE);
                liveSocialMediaSignal.set(null);
                spectator.detectChanges();

                const expectedMinDate = new Date();

                expectedMinDate.setHours(0, 0, 0, 0);

                const minDate = spectator.component['$MIN_DATE']();

                expect(minDate).toEqual(expectedMinDate);
            });

            it('should load page on date when date is selected', () => {
                pageParamsSignal.set({ ...params, mode: UVE_MODE.LIVE });
                liveBaseUveState.socialMedia.set(null);
                spectator.detectChanges();

                const spyLoadPageAsset = vi.spyOn(liveBaseUveState, 'pageReload');

                const calendar = spectator.debugElement.query(
                    By.css('[data-testId="uve-toolbar-calendar"]')
                );

                expect(calendar).toBeTruthy();

                const date = new Date();

                spectator.triggerEventHandler(calendar, 'ngModelChange', date);

                expect(spyLoadPageAsset).toHaveBeenCalledWith({
                    publishDate: convertLocalTimeToUTC(date)
                });
            });

            it('should track event on date when date is selected', () => {
                pageParamsSignal.set({ ...params, mode: UVE_MODE.LIVE });
                liveBaseUveState.socialMedia.set(null);
                spectator.detectChanges();

                const spyTrackUVECalendarChange = vi.spyOn(
                    liveBaseUveState,
                    'trackUVECalendarChange'
                );

                const calendar = spectator.debugElement.query(
                    By.css('[data-testId="uve-toolbar-calendar"]')
                );

                expect(calendar).toBeTruthy();

                const date = new Date();

                spectator.triggerEventHandler(calendar, 'ngModelChange', date);

                expect(spyTrackUVECalendarChange).toHaveBeenCalledWith({
                    selectedDate: convertLocalTimeToUTC(date)
                });
            });

            it('should fetch date when clicking on today button', () => {
                pageParamsSignal.set({ ...params, mode: UVE_MODE.LIVE });
                liveBaseUveState.socialMedia.set(null);
                spectator.detectChanges();

                const spyLoadPageAsset = vi.spyOn(liveBaseUveState, 'pageReload');
                const todayButton = spectator.query(byTestId('uve-toolbar-calendar-today-button'));

                expect(todayButton).toBeTruthy();

                spectator.click(todayButton!);

                expect(spyLoadPageAsset).toHaveBeenCalledWith({
                    publishDate: expect.any(String)
                });
            });

            it('should track event on today button', () => {
                pageParamsSignal.set({ ...params, mode: UVE_MODE.LIVE });
                liveBaseUveState.socialMedia.set(null);
                spectator.detectChanges();

                const spyTrackUVECalendarChange = vi.spyOn(
                    liveBaseUveState,
                    'trackUVECalendarChange'
                );

                const todayButton = spectator.query(byTestId('uve-toolbar-calendar-today-button'));

                expect(todayButton).toBeTruthy();

                spectator.click(todayButton!);

                expect(spyTrackUVECalendarChange).toHaveBeenCalledWith({
                    selectedDate: expect.any(String)
                });
            });
        });
    });

    describe('State changes', () => {
        beforeEach(() => {
            const runningExperiment = getRunningExperimentMock();
            const state = {
                ...baseUVEState,
                pageExperiment: signal(runningExperiment)
            };

            spectator = createComponent({
                providers: [mockProvider(UVEStore, { ...state })]
            });
        });

        describe('Experiment is running', () => {
            it('should have experiment running component', () => {
                spectator.detectChanges();
                expect(spectator.query(byTestId('uve-toolbar-running-experiment'))).toBeTruthy();
            });
        });
    });

    describe('Presentational Component Integration', () => {
        beforeEach(() => {
            spectator = createComponent({
                props: {},
                detectChanges: false,
                providers: [
                    mockProvider(UVEStore, {
                        ...baseUVEState
                    })
                ]
            });
            store = spectator.inject(UVEStore, true);
        });

        describe('DotUveDeviceSelectorComponent', () => {
            describe('Computed Properties', () => {
                describe('$deviceSelectorState', () => {
                    it('should build unified state object from store signals', () => {
                        const testDevice = DEFAULT_DEVICES[1];
                        baseUVEState.device.set(testDevice);
                        baseUVEState.socialMedia.set('facebook');
                        baseUVEState.orientation.set(Orientation.LANDSCAPE);
                        spectator.detectChanges();

                        const state = spectator.component.$deviceSelectorState();

                        expect(state).toEqual({
                            device: testDevice,
                            socialMedia: 'facebook',
                            orientation: Orientation.LANDSCAPE
                        });
                    });

                    it('should react to device changes', () => {
                        const defaultDevice = DEFAULT_DEVICES[0];
                        baseUVEState.device.set(defaultDevice);
                        spectator.detectChanges();

                        expect(spectator.component.$deviceSelectorState().device).toBe(
                            defaultDevice
                        );

                        const newDevice = DEFAULT_DEVICES[1];
                        baseUVEState.device.set(newDevice);
                        spectator.detectChanges();

                        expect(spectator.component.$deviceSelectorState().device).toBe(newDevice);
                    });

                    it('should react to social media changes', () => {
                        baseUVEState.socialMedia.set(null);
                        spectator.detectChanges();

                        expect(spectator.component.$deviceSelectorState().socialMedia).toBeNull();

                        baseUVEState.socialMedia.set('twitter');
                        spectator.detectChanges();

                        expect(spectator.component.$deviceSelectorState().socialMedia).toBe(
                            'twitter'
                        );
                    });

                    it('should react to orientation changes', () => {
                        baseUVEState.orientation.set(Orientation.PORTRAIT);
                        spectator.detectChanges();

                        expect(spectator.component.$deviceSelectorState().orientation).toBe(
                            Orientation.PORTRAIT
                        );

                        baseUVEState.orientation.set(Orientation.LANDSCAPE);
                        spectator.detectChanges();

                        expect(spectator.component.$deviceSelectorState().orientation).toBe(
                            Orientation.LANDSCAPE
                        );
                    });
                });
            });

            describe('deviceSelectorChange output', () => {
                let emittedChanges: DeviceSelectorChange[];

                beforeEach(() => {
                    emittedChanges = [];
                    spectator.component.deviceSelectorChange.subscribe((c) =>
                        emittedChanges.push(c)
                    );
                    pageParamsSignal.set({ ...params, mode: UVE_MODE.PREVIEW });
                    baseUVEState.$isPreviewMode.set(true);
                    spectator.detectChanges();
                });

                it('should emit device change when device selector emits stateChange', () => {
                    const testDevice = DEFAULT_DEVICES[1];

                    spectator.triggerEventHandler(DotUveDeviceSelectorComponent, 'stateChange', {
                        type: 'device',
                        device: testDevice
                    });

                    expect(emittedChanges).toHaveLength(1);
                    expect(emittedChanges[0]).toEqual({ type: 'device', device: testDevice });
                });

                it('should emit socialMedia change when device selector emits stateChange', () => {
                    spectator.triggerEventHandler(DotUveDeviceSelectorComponent, 'stateChange', {
                        type: 'socialMedia',
                        socialMedia: 'facebook'
                    });

                    expect(emittedChanges).toHaveLength(1);
                    expect(emittedChanges[0]).toEqual({
                        type: 'socialMedia',
                        socialMedia: 'facebook'
                    });
                });

                it('should emit orientation change when device selector emits stateChange', () => {
                    spectator.triggerEventHandler(DotUveDeviceSelectorComponent, 'stateChange', {
                        type: 'orientation',
                        orientation: Orientation.PORTRAIT
                    });

                    expect(emittedChanges).toHaveLength(1);
                    expect(emittedChanges[0]).toEqual({
                        type: 'orientation',
                        orientation: Orientation.PORTRAIT
                    });
                });

                it('should emit all change types in sequence', () => {
                    const testDevice = DEFAULT_DEVICES[0];

                    spectator.triggerEventHandler(DotUveDeviceSelectorComponent, 'stateChange', {
                        type: 'device',
                        device: testDevice
                    });
                    spectator.triggerEventHandler(DotUveDeviceSelectorComponent, 'stateChange', {
                        type: 'socialMedia',
                        socialMedia: 'twitter'
                    });
                    spectator.triggerEventHandler(DotUveDeviceSelectorComponent, 'stateChange', {
                        type: 'orientation',
                        orientation: Orientation.LANDSCAPE
                    });

                    expect(emittedChanges).toHaveLength(3);
                    expect(emittedChanges[0]).toEqual({ type: 'device', device: testDevice });
                    expect(emittedChanges[1]).toEqual({
                        type: 'socialMedia',
                        socialMedia: 'twitter'
                    });
                    expect(emittedChanges[2]).toEqual({
                        type: 'orientation',
                        orientation: Orientation.LANDSCAPE
                    });
                });
            });

            describe('Template Bindings', () => {
                beforeEach(() => {
                    pageParamsSignal.set({ ...params, mode: UVE_MODE.PREVIEW });
                    baseUVEState.$isPreviewMode.set(true);
                    spectator.detectChanges();
                });

                it('should pass state input to device selector', () => {
                    const testDevice = DEFAULT_DEVICES[1];
                    baseUVEState.device.set(testDevice);
                    baseUVEState.socialMedia.set('facebook');
                    baseUVEState.orientation.set(Orientation.LANDSCAPE);
                    spectator.detectChanges();

                    const deviceSelectorDebugElement = spectator.debugElement.query(
                        By.directive(DotUveDeviceSelectorComponent)
                    );
                    const deviceSelector =
                        deviceSelectorDebugElement.componentInstance as DotUveDeviceSelectorComponent;

                    expect(deviceSelector.$state()).toEqual({
                        device: testDevice,
                        socialMedia: 'facebook',
                        orientation: Orientation.LANDSCAPE
                    });
                });

                it('should pass devices input to device selector', () => {
                    spectator.detectChanges();

                    const deviceSelectorDebugElement = spectator.debugElement.query(
                        By.directive(DotUveDeviceSelectorComponent)
                    );
                    const deviceSelector =
                        deviceSelectorDebugElement.componentInstance as DotUveDeviceSelectorComponent;

                    expect(deviceSelector.$devices()).toBeDefined();
                });

                it('should pass isTraditionalPage input to device selector', () => {
                    baseUVEState.isTraditionalPage.set(true);
                    spectator.detectChanges();

                    const deviceSelectorDebugElement = spectator.debugElement.query(
                        By.directive(DotUveDeviceSelectorComponent)
                    );
                    const deviceSelector =
                        deviceSelectorDebugElement.componentInstance as DotUveDeviceSelectorComponent;

                    expect(deviceSelector.$isTraditionalPage()).toBe(true);
                });

                it('should forward stateChange from device selector as deviceSelectorChange output', () => {
                    const emitted: DeviceSelectorChange[] = [];
                    spectator.component.deviceSelectorChange.subscribe((c) => emitted.push(c));
                    const testDevice = DEFAULT_DEVICES[1];

                    spectator.triggerEventHandler(DotUveDeviceSelectorComponent, 'stateChange', {
                        type: 'device',
                        device: testDevice
                    });

                    expect(emitted).toHaveLength(1);
                    expect(emitted[0]).toEqual({ type: 'device', device: testDevice });
                });
            });
        });

        describe('DotToggleLockButtonComponent', () => {
            describe('Computed Properties', () => {
                describe('$lockOptions', () => {
                    it('should return null when store options are null', () => {
                        baseUVEState.$lockOptions.set(null);
                        spectator.detectChanges();

                        expect(spectator.component.$lockOptions()).toBeNull();
                    });

                    it('should build complete options object with loading state', () => {
                        baseUVEState.$lockOptions.set(createLockOptions());
                        baseUVEState.workflowLockIsLoading.set(true);
                        spectator.detectChanges();

                        const options = spectator.component.$lockOptions()!;

                        expect(options).toEqual({
                            inode: 'test-inode',
                            isLocked: false,
                            isLockedByCurrentUser: false,
                            canLock: true,
                            loading: true,
                            disabled: false,
                            lockedBy: '',
                            shouldShowButton: true,
                            message: 'editpage.toolbar.page.release.lock.locked.by.user',
                            args: []
                        });
                    });

                    it('should set disabled true when canLock is false', () => {
                        baseUVEState.$lockOptions.set(
                            createLockOptions({
                                isLocked: true,
                                lockedBy: 'another-user',
                                canLock: false
                            })
                        );
                        baseUVEState.workflowLockIsLoading.set(false);
                        spectator.detectChanges();

                        const options = spectator.component.$lockOptions()!;

                        expect(options.disabled).toBe(true);
                        expect(options.message).toBe('editpage.locked-by');
                        expect(options.args).toEqual(['another-user']);
                    });

                    it('should include lockedBy in args when provided', () => {
                        baseUVEState.$lockOptions.set(
                            createLockOptions({
                                isLocked: true,
                                lockedBy: 'john.doe@example.com',
                                canLock: false
                            })
                        );
                        spectator.detectChanges();

                        const options = spectator.component.$lockOptions()!;

                        expect(options.args).toEqual(['john.doe@example.com']);
                    });
                });
            });

            describe('Handler Methods', () => {
                describe('handleToggleLock', () => {
                    beforeEach(() => {
                        baseUVEState.$lockOptions.set(createLockOptions());
                        baseUVEState.workflowLockIsLoading.set(false);
                        spectator.detectChanges();
                    });

                    it('should call store.toggleLock with correct parameters', () => {
                        const spy = vi.spyOn(store, 'workflowToggleLock');

                        spectator.triggerEventHandler(
                            DotToggleLockButtonComponent,
                            'toggleLockClick',
                            {
                                inode: 'test-inode-123',
                                isLocked: false,
                                isLockedByCurrentUser: false
                            }
                        );

                        expect(spy).toHaveBeenCalledWith('test-inode-123', false, false, undefined);
                    });

                    it('should handle locked state correctly', () => {
                        const spy = vi.spyOn(store, 'workflowToggleLock');

                        spectator.triggerEventHandler(
                            DotToggleLockButtonComponent,
                            'toggleLockClick',
                            {
                                inode: 'locked-inode',
                                isLocked: true,
                                isLockedByCurrentUser: true
                            }
                        );

                        expect(spy).toHaveBeenCalledWith('locked-inode', true, true, undefined);
                    });

                    it('should handle page locked by another user', () => {
                        const spy = vi.spyOn(store, 'workflowToggleLock');

                        spectator.triggerEventHandler(
                            DotToggleLockButtonComponent,
                            'toggleLockClick',
                            {
                                inode: 'other-user-inode',
                                isLocked: true,
                                isLockedByCurrentUser: false
                            }
                        );

                        expect(spy).toHaveBeenCalledWith(
                            'other-user-inode',
                            true,
                            false,
                            undefined
                        );
                    });
                });
            });

            describe('Template Bindings', () => {
                beforeEach(() => {
                    baseUVEState.$lockOptions.set(createLockOptions());
                    baseUVEState.workflowLockIsLoading.set(false);
                    spectator.detectChanges();
                });

                it('should pass toggleLockOptions input to toggle lock button', () => {
                    const toggleLockButton = spectator.query(byTestId('uve-toolbar-toggle-lock'));
                    expect(toggleLockButton).toBeTruthy();

                    const buttonDebugElement = spectator.debugElement.query(
                        By.directive(DotToggleLockButtonComponent)
                    );
                    const buttonComponent =
                        buttonDebugElement.componentInstance as DotToggleLockButtonComponent;

                    expect(buttonComponent).toBeTruthy();
                    expect(buttonComponent.toggleLockOptions()).toEqual({
                        inode: 'test-inode',
                        isLocked: false,
                        isLockedByCurrentUser: false,
                        canLock: true,
                        lockedBy: '',
                        shouldShowButton: true,
                        loading: false,
                        disabled: false,
                        message: 'editpage.toolbar.page.release.lock.locked.by.user',
                        args: []
                    });
                });

                it('should call handleToggleLock when toggleLockClick emits', () => {
                    const spy = vi.spyOn(spectator.component, 'handleToggleLock');

                    spectator.triggerEventHandler(DotToggleLockButtonComponent, 'toggleLockClick', {
                        inode: 'test-inode',
                        isLocked: false,
                        isLockedByCurrentUser: false
                    });

                    expect(spy).toHaveBeenCalledWith({
                        inode: 'test-inode',
                        isLocked: false,
                        isLockedByCurrentUser: false
                    });
                });
            });
        });

        describe('DotEmaInfoDisplayComponent', () => {
            describe('Handler Methods', () => {
                describe('handleInfoDisplayAction', () => {
                    beforeEach(() => {
                        baseUVEState.$infoDisplayProps.set(INFO_DISPLAY_VARIANT_PROPS);
                        spectator.detectChanges();
                    });

                    it('should call store.viewClearDeviceAndSocialMedia when device action is triggered', () => {
                        const spy = vi.spyOn(store, 'viewClearDeviceAndSocialMedia');

                        spectator.triggerEventHandler(
                            DotEmaInfoDisplayComponent,
                            'actionClicked',
                            'device'
                        );

                        expect(spy).toHaveBeenCalled();
                    });

                    it('should call store.viewClearDeviceAndSocialMedia when socialMedia action is triggered', () => {
                        const spy = vi.spyOn(store, 'viewClearDeviceAndSocialMedia');

                        spectator.triggerEventHandler(
                            DotEmaInfoDisplayComponent,
                            'actionClicked',
                            'socialMedia'
                        );

                        expect(spy).toHaveBeenCalled();
                    });

                    it('should not call viewClearDeviceAndSocialMedia for variant action', () => {
                        const spy = vi.spyOn(store, 'viewClearDeviceAndSocialMedia');
                        spy.mockClear(); // Clear any calls from previous tests

                        spectator.triggerEventHandler(
                            DotEmaInfoDisplayComponent,
                            'actionClicked',
                            'variant'
                        );

                        expect(spy).not.toHaveBeenCalled();
                    });

                    /**
                     * The inbound leg of the variant round-trip (#37005, US1/US2).
                     *
                     * Contract:
                     * `specs/37005-experiments-uve-integration/contracts/navigation-destinations.md` §3.
                     *
                     * The destination is resolved by ORIGIN, not by the switch. A switch-only
                     * branch makes FR-018 and FR-005/FR-027 mutually exclusive on a supported
                     * path — switch off, portlet reached from the main navigation (FR-026),
                     * variant opened (FR-027), return — which would land the editor on a legacy
                     * screen they never came from. The switch is the fallback for a deep-linked
                     * variant that carries no origin.
                     */
                    describe('returning from a variant', () => {
                        const EXPERIMENT_ID = 'exp-1';
                        const PAGE_ID = 'page-1';
                        /** The marker goes with them: once the editor is back there is no round
                         * trip left for it to describe. */
                        /**
                         * `mode` is named rather than nulled. A null only ever meant "let the
                         * shell's default put it back", and the shell applies that default when it
                         * re-reads the route — which a `pageLoad` never makes it do.
                         */
                        const CLEARED = {
                            mode: UVE_MODE.EDIT,
                            variantName: null,
                            experimentId: null,
                            [EXPERIMENT_RETURN_PARAM]: null
                        };

                        let navigate: MockInstance;

                        const leaveVariant = () =>
                            spectator.triggerEventHandler(
                                DotEmaInfoDisplayComponent,
                                'actionClicked',
                                'variant'
                            );

                        beforeEach(() => {
                            setAddress({});
                            $experimentsPortletSwitchSignal.set(false);
                            baseUVEState.pageExperiment.set({
                                id: EXPERIMENT_ID,
                                pageId: PAGE_ID
                            } as DotExperiment);
                            navigate = vi.spyOn(spectator.inject(Router), 'navigate');
                        });

                        /**
                         * #37478, FR-023, FR-046. The return leg, rebuilt rather than recovered.
                         *
                         * Written from the states a real trip leaves behind, not from the tidy one:
                         * a panel that never suspended, and one whose memory was wiped while the
                         * editor was away. The first implementation consulted `suspendedForVariant`
                         * and could return from neither.
                         */
                        describe('returning from a variant with the flag on', () => {
                            const panelWith = (suspended: boolean) => {
                                panelStore = {
                                    suspendedForVariant: vi.fn().mockReturnValue(suspended),
                                    resumeFromVariant: vi.fn(),
                                    openVariants: vi.fn(),
                                    // What the panel was showing, for a reload that took the
                                    // address with it.
                                    experimentId: vi.fn().mockReturnValue(null)
                                };
                                $experimentsPortletSwitchSignal.set(true);
                                spectator = createComponent({ detectChanges: false });
                                spectator.detectChanges();
                                navigate = vi.spyOn(spectator.inject(Router), 'navigate');
                                baseUVEState.pageExperiment.set({
                                    id: EXPERIMENT_ID,
                                    pageId: PAGE_ID
                                } as DotExperiment);
                                spectator.detectChanges();
                            };

                            afterEach(() => {
                                panelStore = null;
                            });

                            /**
                             * The common path, and the faithful one: the panel still holds the
                             * view, the experiment and the card the editor was on, so the return
                             * asks for none of them. Recomputing here would replace what they left
                             * with something merely equivalent.
                             */
                            it('should hand a suspended panel back untouched', () => {
                                panelWith(true);
                                setAddress({});
                                spectator.detectChanges();

                                leaveVariant();

                                expect(panelStore?.resumeFromVariant).toHaveBeenCalledTimes(1);
                                expect(panelStore?.openVariants).not.toHaveBeenCalled();
                            });

                            /**
                             * `pageExperiment()` answers "what is running on this page", which is
                             * a different question and commonly a different experiment: the page
                             * runs one while the editor is off previewing a draft of another.
                             * Answering with it reopened the panel on an experiment the editor
                             * never clicked.
                             */
                            it('should name the previewed experiment, not the running one', () => {
                                panelWith(false);
                                baseUVEState.pageExperiment.set({
                                    id: 'the-running-one',
                                    pageId: PAGE_ID
                                } as DotExperiment);
                                setAddress({ experimentId: 'the-draft-i-clicked' });
                                spectator.detectChanges();

                                leaveVariant();

                                expect(panelStore?.openVariants).toHaveBeenCalledWith(
                                    'the-draft-i-clicked'
                                );
                            });

                            /**
                             * The bug the whole feature died on, and the reason it survived every
                             * unit test until someone used it.
                             *
                             * A DRAFT experiment is not running on the page. Once the editor is
                             * back on the original, the page asset carries no experiment and the
                             * address carries no `experimentId`, so `pageExperiment()` is null —
                             * and the guard that protects the branches *below* was swallowing the
                             * click before the panel was ever asked. The chip did nothing at all.
                             *
                             * This return needs nothing from the experiment: the panel is holding
                             * it. So it is answered first.
                             */
                            it('should reopen the panel with no experiment on the page', () => {
                                panelWith(true);
                                baseUVEState.pageExperiment.set(null);
                                setAddress({});
                                spectator.detectChanges();

                                leaveVariant();

                                expect(panelStore?.resumeFromVariant).toHaveBeenCalledTimes(1);
                                expect(navigate).not.toHaveBeenCalled();
                            });

                            /**
                             * The regression this whole branch exists for. Writing the variant off
                             * the address looks like tidying up; the shell watches those params, so
                             * it restarts UVE and the editor watches their page reload underneath
                             * the panel they just reopened.
                             */
                            /**
                             * Previewing the CONTROL leaves `variantName=DEFAULT` on the address:
                             * present, but not a variant. Reading it as presence restarted UVE for
                             * nothing — the editor was already on the page they were going back to.
                             */
                            it('should not reload at all when on the control', () => {
                                panelWith(true);
                                setAddress({ variantName: DEFAULT_VARIANT_ID });
                                spectator.detectChanges();
                                (baseUVEState.pageLoad as Mock).mockClear();

                                leaveVariant();

                                expect(baseUVEState.pageLoad).not.toHaveBeenCalled();
                                expect(navigate).not.toHaveBeenCalled();
                                // Not a no-op, though: the experiment still has to come off the
                                // params, or the chip it feeds outlives the trip.
                                expect(baseUVEState.pageUpdateParams).toHaveBeenCalledWith({
                                    experimentId: null,
                                    [EXPERIMENT_RETURN_PARAM]: null
                                });
                            });

                            /**
                             * The control previewed is still a mode change, and a mode change is
                             * not a param change. `PREVIEW` and `EDIT` render different canvases,
                             * so patching the param without loading would leave the toolbar
                             * claiming one mode over an iframe still showing the other — and the
                             * mode selector with nothing selected, which is how this surfaced.
                             *
                             * Previewing the Original is the only way into this state, and it is
                             * the common one: it is what the Variants card does for the control.
                             */
                            it('should load when the control was previewed, not just patch', () => {
                                panelWith(true);
                                setAddress({
                                    variantName: DEFAULT_VARIANT_ID,
                                    experimentId: EXPERIMENT_ID,
                                    mode: UVE_MODE.PREVIEW
                                });
                                spectator.detectChanges();
                                (baseUVEState.pageLoad as Mock).mockClear();
                                (baseUVEState.pageUpdateParams as Mock).mockClear();

                                leaveVariant();

                                expect(baseUVEState.pageLoad).toHaveBeenCalledWith(CLEARED);
                                expect(baseUVEState.pageUpdateParams).not.toHaveBeenCalled();
                                expect(navigate).not.toHaveBeenCalled();
                            });

                            /**
                             * What the editor sees, which is the point of the whole return: the
                             * blue bar offering a way back is gone once the way back has been
                             * taken.
                             *
                             * On the control the chip is the *only* bar — the store's own props
                             * are null for DEFAULT — so it is the one that has to go, and it did
                             * not: it read `ActivatedRoute.snapshot`, which UVE never updates
                             * because it writes its address with `Location.go`. The bar survived a
                             * return that had already happened. A real variant needs no equivalent
                             * test: there the bar is the store's own, and `pageLoad` rebuilds it
                             * from params that no longer name a variant.
                             */
                            it('should take the chip away once the control is left', () => {
                                panelWith(true);
                                infoDisplayPropsSignal.set(undefined);
                                setAddress({
                                    variantName: DEFAULT_VARIANT_ID,
                                    experimentId: EXPERIMENT_ID
                                });
                                spectator.detectChanges();
                                expect(spectator.component.$infoDisplayProps()).toBeTruthy();

                                leaveVariant();
                                spectator.detectChanges();

                                expect(spectator.component.$infoDisplayProps()).toBeFalsy();
                                expect(spectator.query(byTestId('info-display'))).toBeNull();
                            });

                            /**
                             * On a real variant the same button does what it does everywhere else
                             * in this bar: it gets the editor out of the state they are in. The
                             * canvas reload is inherent — the original page has to be fetched.
                             */
                            /**
                             * Loaded, not routed. `/edit-page` declares `reuseRoute: false` and
                             * route data is inherited, so any router navigation beneath it rebuilds
                             * the whole editor — toolbar, canvas and iframe — which is a visible
                             * jump for a page that only needed its content swapped.
                             */
                            it('should leave a real variant by loading, not routing', () => {
                                panelWith(true);
                                setAddress({ variantName: 'variant-b' });
                                spectator.detectChanges();

                                leaveVariant();

                                expect(baseUVEState.pageLoad).toHaveBeenCalledWith(CLEARED);
                                expect(navigate).not.toHaveBeenCalled();
                                expect(panelStore?.resumeFromVariant).toHaveBeenCalledTimes(1);
                            });

                            /**
                             * Nothing to resume — the trip began in the full-screen portlet, or a
                             * reload during it took the panel's memory. Rebuilt from the address
                             * instead, which still names the experiment.
                             */
                            it('should rebuild the return when there is nothing to resume', () => {
                                panelWith(false);
                                setAddress({ experimentId: EXPERIMENT_ID });
                                spectator.detectChanges();

                                leaveVariant();

                                expect(panelStore?.openVariants).toHaveBeenCalledWith(
                                    EXPERIMENT_ID
                                );
                            });

                            it.each([
                                {
                                    origin: 'the portlet',
                                    params: {
                                        [EXPERIMENT_RETURN_PARAM]: EXPERIMENT_RETURN_PORTLET
                                    } as Record<string, string>
                                },
                                {
                                    origin: 'nowhere — a pasted link',
                                    params: {} as Record<string, string>
                                }
                            ])('should stay in the editor, coming from $origin', ({ params }) => {
                                panelWith(false);
                                setAddress({ ...params, experimentId: EXPERIMENT_ID });
                                spectator.detectChanges();

                                leaveVariant();

                                expect(panelStore?.openVariants).toHaveBeenCalledWith(
                                    EXPERIMENT_ID
                                );
                                expect(navigate).not.toHaveBeenCalledWith(
                                    ['/experiments', EXPERIMENT_ID, 'configuration'],
                                    expect.anything()
                                );
                            });

                            /**
                             * With nothing to resume the editor may still be standing on the
                             * variant, and the original page has to be fetched to be shown. Merged,
                             * because everything else on that address is the editor's own.
                             */
                            it('should take the variant off by loading the page again', () => {
                                panelWith(false);
                                setAddress({
                                    variantName: 'variant-a',
                                    experimentId: EXPERIMENT_ID
                                });
                                spectator.detectChanges();

                                leaveVariant();

                                expect(baseUVEState.pageLoad).toHaveBeenCalledWith(CLEARED);
                                expect(navigate).not.toHaveBeenCalled();
                            });

                            /** Already back on the page: writing it again would reload for nothing. */
                            it('should not reload on the control with nothing to resume', () => {
                                panelWith(false);
                                setAddress({
                                    experimentId: EXPERIMENT_ID,
                                    variantName: DEFAULT_VARIANT_ID
                                });
                                spectator.detectChanges();

                                leaveVariant();

                                expect(navigate).not.toHaveBeenCalled();
                            });
                        });

                        /**
                         * #37478, FR-025c, FR-025d, D14. The tag announces what is running on the page the editor is
                         * looking at; asking how it is doing must not cost them that page.
                         */
                        describe('the running-experiment tag (#37478)', () => {
                            const RUNNING = { id: 'running-1', pageId: 'page-1' } as DotExperiment;

                            afterEach(() => {
                                panelStore = null;
                            });

                            it('should open its results in the panel, with the switch on', () => {
                                panelStore = {
                                    suspendedForVariant: vi.fn().mockReturnValue(false),
                                    resumeFromVariant: vi.fn(),
                                    openVariants: vi.fn(),
                                    experimentId: vi.fn().mockReturnValue(null),
                                    openResults: vi.fn()
                                };
                                $experimentsPortletSwitchSignal.set(true);
                                spectator = createComponent({ detectChanges: false });
                                spectator.detectChanges();

                                spectator.component.handleRunningExperimentClick(RUNNING);

                                expect(panelStore?.openResults).toHaveBeenCalledWith('running-1');
                            });

                            /**
                             * With the switch off the tag keeps the legacy reports route it has always had, which is
                             * what FR-017 of #37005 constrains — so it is a destination, not an action, and nothing
                             * asks the panel anything.
                             */
                            it('should stay a destination with the switch off', () => {
                                $experimentsPortletSwitchSignal.set(false);
                                spectator = createComponent({ detectChanges: false });
                                spectator.detectChanges();

                                // Protected, so not on the component's public type — read through
                                // the same cast the rest of this file uses for internals.
                                expect(
                                    (
                                        spectator.component as unknown as {
                                            $experimentsPanelEnabled: () => boolean;
                                        }
                                    ).$experimentsPanelEnabled()
                                ).toBe(false);
                            });
                        });

                        // #37005. Previewing the CONTROL from the portlet's Configure screen
                        // opens UVE on the default variant, and the store's own
                        // `$infoDisplayProps` returns null for it — so the chip, which is the only
                        // thing that reads the origin marker and offers the way back, never
                        // rendered. The editor arrived from a screen it cannot return to.
                        describe('previewing the control from the portlet', () => {
                            beforeEach(() => {
                                infoDisplayPropsSignal.set(undefined);
                                baseUVEState.pageExperiment.set({
                                    id: EXPERIMENT_ID,
                                    pageId: PAGE_ID,
                                    trafficProportion: {
                                        variants: [{ id: DEFAULT_VARIANT_ID, name: 'Original' }]
                                    }
                                } as DotExperiment);
                            });

                            it('should offer the chip, naming the control', () => {
                                setAddress({
                                    experimentId: EXPERIMENT_ID,
                                    [EXPERIMENT_RETURN_PARAM]: EXPERIMENT_RETURN_PORTLET
                                });
                                spectator.detectChanges();

                                expect(spectator.component.$infoDisplayProps()).toEqual(
                                    expect.objectContaining({
                                        id: 'variant',
                                        info: expect.objectContaining({ args: ['Original'] })
                                    })
                                );
                            });

                            // The legacy in-UVE screens send the same `experimentId` but never the
                            // marker, and #37005 must leave the switch-off path exactly as it was.
                            /**
                             * With the switch off the origin marker still decides, because the
                             * legacy in-UVE screens send the same `experimentId` and must not gain
                             * a chip they never had (FR-042, FR-047).
                             */
                            it('should not offer the chip without the origin marker, switch off', () => {
                                $experimentsPortletSwitchSignal.set(false);
                                setAddress({ experimentId: EXPERIMENT_ID });
                                spectator = createComponent({ detectChanges: false });
                                spectator.detectChanges();

                                expect(spectator.component.$infoDisplayProps()).toBeFalsy();
                            });

                            /**
                             * #37478. With the switch on, the way back is the panel and every
                             * arrival inside an experiment deserves one — so the chip keys on the
                             * experiment being on the address, which is true, rather than on a
                             * marker claiming the editor came from the portlet, which is not: the
                             * panel builds its variant links through the same helper and the
                             * marker is written unconditionally.
                             */
                            it('should offer the chip for any experiment arrival, switch on', () => {
                                $experimentsPortletSwitchSignal.set(true);
                                setAddress({ experimentId: EXPERIMENT_ID });
                                spectator = createComponent({ detectChanges: false });
                                spectator.detectChanges();

                                expect(spectator.component.$infoDisplayProps()).toBeTruthy();
                            });

                            it('should offer no chip outside an experiment, switch on', () => {
                                $experimentsPortletSwitchSignal.set(true);
                                setAddress({});
                                spectator = createComponent({ detectChanges: false });
                                spectator.detectChanges();

                                expect(spectator.component.$infoDisplayProps()).toBeFalsy();
                            });

                            it('should return to Configure when the chip is used', () => {
                                setAddress({
                                    experimentId: EXPERIMENT_ID,
                                    [EXPERIMENT_RETURN_PARAM]: EXPERIMENT_RETURN_PORTLET
                                });
                                spectator.detectChanges();

                                leaveVariant();

                                expect(navigate).toHaveBeenCalledWith(
                                    ['/experiments', EXPERIMENT_ID, 'configuration'],
                                    expect.anything()
                                );
                            });
                        });

                        // T037 / FR-005, FR-006.
                        it('should land on the portlet when the round-trip began there', () => {
                            setAddress({
                                [EXPERIMENT_RETURN_PARAM]: EXPERIMENT_RETURN_PORTLET
                            });
                            spectator.detectChanges();

                            leaveVariant();

                            expect(navigate).toHaveBeenCalledWith(
                                ['/experiments', EXPERIMENT_ID, 'configuration'],
                                {
                                    queryParams: {
                                        ...CLEARED,
                                        [EXPERIMENT_RETURN_PARAM]: null,
                                        [CONFIGURE_SECTION_PARAM]: CONFIGURE_SECTION_VARIANTS
                                    }
                                }
                            );
                        });

                        // The Variants card is where the round-trip started, so returning to the
                        // top of a four-card form loses the reader's place.
                        it('should ask Configure to land on the Variants card', () => {
                            setAddress({
                                [EXPERIMENT_RETURN_PARAM]: EXPERIMENT_RETURN_PORTLET
                            });
                            spectator.detectChanges();

                            leaveVariant();

                            expect(navigate).toHaveBeenCalledWith(
                                expect.anything(),
                                expect.objectContaining({
                                    queryParams: expect.objectContaining({
                                        [CONFIGURE_SECTION_PARAM]: CONFIGURE_SECTION_VARIANTS
                                    })
                                })
                            );
                        });

                        // T037. The portlet's URL must not inherit UVE's params — `url`,
                        // `language_id` and the persona key mean nothing to the list and its
                        // `parseViewState` would leave them in the address indefinitely.
                        it('should not merge UVE params into the portlet URL', () => {
                            setAddress({
                                [EXPERIMENT_RETURN_PARAM]: EXPERIMENT_RETURN_PORTLET
                            });
                            spectator.detectChanges();

                            leaveVariant();

                            const [, options] = navigate.mock.calls[0];
                            expect(options.queryParamsHandling).toBeUndefined();
                        });

                        // T038 / FR-027. The case a switch-only branch gets wrong.
                        it('should land on the portlet even with the switch off', () => {
                            $experimentsPortletSwitchSignal.set(false);
                            setAddress({
                                [EXPERIMENT_RETURN_PARAM]: EXPERIMENT_RETURN_PORTLET
                            });
                            spectator.detectChanges();

                            leaveVariant();

                            expect(navigate).toHaveBeenCalledWith(
                                ['/experiments', EXPERIMENT_ID, 'configuration'],
                                expect.anything()
                            );
                        });

                        // T039 / FR-005, SC-005. The target carries the experiment's id and no
                        // page segment, so a page hosting two experiments returns to the right
                        // one — US1 scenario 4.
                        it('should key the portlet destination on the experiment, not the page', () => {
                            setAddress({
                                [EXPERIMENT_RETURN_PARAM]: EXPERIMENT_RETURN_PORTLET
                            });
                            spectator.detectChanges();

                            leaveVariant();

                            const [commands] = navigate.mock.calls[0];
                            expect(commands).toContain(EXPERIMENT_ID);
                            expect(commands).not.toContain(PAGE_ID);
                        });

                        // T040 / FR-018. No origin marker and the switch off — a pasted or
                        // bookmarked variant URL on a default build. This is the existing code
                        // path, byte-identical.
                        it('should fall back to the legacy screen with no origin and the switch off', () => {
                            $experimentsPortletSwitchSignal.set(false);
                            setAddress({});
                            spectator.detectChanges();

                            leaveVariant();

                            expect(navigate).toHaveBeenCalledWith(
                                [
                                    '/edit-page/experiments/',
                                    PAGE_ID,
                                    EXPERIMENT_ID,
                                    'configuration'
                                ],
                                {
                                    queryParams: CLEARED,
                                    queryParamsHandling: 'merge'
                                }
                            );
                        });

                        // T040. No origin marker, switch on — the opted-in operator's deep link
                        // lands somewhere coherent rather than on the screen they are migrating
                        // away from.
                        it('should do nothing when there is no experiment to return to', () => {
                            baseUVEState.pageExperiment.set(null);
                            spectator.detectChanges();

                            leaveVariant();

                            expect(navigate).not.toHaveBeenCalled();
                        });
                    });
                });
            });

            describe('Template Bindings', () => {
                beforeEach(() => {
                    baseUVEState.$infoDisplayProps.set(INFO_DISPLAY_VARIANT_PROPS);
                    spectator.detectChanges();
                });

                it('should pass options input to info display', () => {
                    const infoDisplay = spectator.query(byTestId('info-display'));
                    expect(infoDisplay).toBeTruthy();

                    const infoDisplayDebugElement = spectator.debugElement.query(
                        By.directive(DotEmaInfoDisplayComponent)
                    );
                    const infoDisplayComponent =
                        infoDisplayDebugElement.componentInstance as DotEmaInfoDisplayComponent;

                    expect(infoDisplayComponent).toBeTruthy();
                    expect(infoDisplayComponent.$options()).toEqual(INFO_DISPLAY_VARIANT_PROPS);
                });

                it('should call handleInfoDisplayAction when actionClicked emits', () => {
                    const spy = vi.spyOn(spectator.component, 'handleInfoDisplayAction');

                    spectator.triggerEventHandler(
                        DotEmaInfoDisplayComponent,
                        'actionClicked',
                        'device'
                    );

                    expect(spy).toHaveBeenCalledWith('device');
                });

                it('should not render info display when options are null', () => {
                    baseUVEState.$infoDisplayProps.set(null);
                    spectator.detectChanges();

                    const infoDisplay = spectator.query(byTestId('info-display'));
                    expect(infoDisplay).toBeFalsy();
                });
            });
        });

        describe('isTraditionalPage computed property', () => {
            it('should expose store.isTraditionalPage signal', () => {
                expect(spectator.component.isTraditionalPage).toBeDefined();
                expect(typeof spectator.component.isTraditionalPage).toBe('function');
            });
        });
    });
});
