import { expect, describe, it } from '@jest/globals';
import { byTestId, mockProvider, Spectator, createComponentFactory } from '@ngneat/spectator/jest';
import { MockComponent } from 'ng-mocks';
import { of, throwError } from 'rxjs';

import { HttpClientTestingModule, provideHttpClientTesting } from '@angular/common/http/testing';
import { DebugElement, signal } from '@angular/core';
import { By } from '@angular/platform-browser';

import { ConfirmationService, MessageService } from 'primeng/api';

import {
    DotAnalyticsTrackerService,
    DotContentletLockerService,
    DotDevicesService,
    DotExperimentsService,
    DotLanguagesService,
    DotLicenseService,
    DotPersonalizeService,
    DotWorkflowsActionsService
} from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';
import { UVE_MODE } from '@dotcms/types';
import {
    DotExperimentsServiceMock,
    DotLanguagesServiceMock,
    DotLicenseServiceMock,
    getRunningExperimentMock,
    mockDotDevices
} from '@dotcms/utils-testing';

import { DotEditorModeSelectorComponent } from './components/dot-editor-mode-selector/dot-editor-mode-selector.component';
import { DotEmaBookmarksComponent } from './components/dot-ema-bookmarks/dot-ema-bookmarks.component';
import { DotEmaRunningExperimentComponent } from './components/dot-ema-running-experiment/dot-ema-running-experiment.component';
import { DotUveDeviceSelectorComponent } from './components/dot-uve-device-selector/dot-uve-device-selector.component';
import { DotUveWorkflowActionsComponent } from './components/dot-uve-workflow-actions/dot-uve-workflow-actions.component';
import { EditEmaLanguageSelectorComponent } from './components/edit-ema-language-selector/edit-ema-language-selector.component';
import { EditEmaPersonaSelectorComponent } from './components/edit-ema-persona-selector/edit-ema-persona-selector.component';
import { DotUveToolbarComponent } from './dot-uve-toolbar.component';

import { DotPageApiService } from '../../../services/dot-page-api.service';
import { DEFAULT_DEVICES, DEFAULT_PERSONA, PERSONA_KEY } from '../../../shared/consts';
import {
    HEADLESS_BASE_QUERY_PARAMS,
    MOCK_RESPONSE_HEADLESS,
    MOCK_RESPONSE_VTL
} from '../../../shared/mocks';
import { UVEStore } from '../../../store/dot-uve.store';
import {
    getFullPageURL,
    createFavoritePagesURL,
    sanitizeURL,
    convertLocalTimeToUTC
} from '../../../utils';

const $apiURL = '/api/v1/page/json/123-xyz-567-xxl?host_id=123-xyz-567-xxl&language_id=1';

const params = HEADLESS_BASE_QUERY_PARAMS;
const url = sanitizeURL(params?.url);

const pageAPIQueryParams = getFullPageURL({ url, params });
const pageAPI = `/api/v1/page/${'json'}/${pageAPIQueryParams}`;
const pageAPIResponse = MOCK_RESPONSE_HEADLESS;
const shouldShowInfoDisplay = false || pageAPIResponse?.page.locked;
const bookmarksUrl = createFavoritePagesURL({
    languageId: Number(params?.language_id),
    pageURI: url,
    siteId: pageAPIResponse?.site.identifier
});

const baseUVEToolbarState = {
    editor: {
        bookmarksUrl,
        apiUrl: `${'http://localhost'}${pageAPI}`
    },
    preview: null,
    currentLanguage: pageAPIResponse?.viewAs.language,
    urlContentMap: null,
    runningExperiment: null,
    workflowActionsInode: pageAPIResponse?.page.inode,
    unlockButton: null,
    showInfoDisplay: shouldShowInfoDisplay
};

const baseUVEState = {
    $uveToolbar: signal(baseUVEToolbarState),
    setDevice: jest.fn(),
    pageParams: signal(params),
    pageAPIResponse: signal(MOCK_RESPONSE_VTL),
    $apiURL: signal($apiURL),
    reloadCurrentPage: jest.fn(),
    loadPageAsset: jest.fn(),
    $isPreviewMode: signal(false),
    $isLiveMode: signal(false),
    $isEditMode: signal(false),
    $personaSelector: signal({
        pageId: pageAPIResponse?.page.identifier,
        value: pageAPIResponse?.viewAs.persona ?? DEFAULT_PERSONA
    }),
    $infoDisplayProps: signal(undefined),
    viewParams: signal({
        seo: undefined,
        device: undefined,
        orientation: undefined
    }),
    $urlContentMap: signal(undefined),
    languages: signal([
        { id: 1, translated: true },
        { id: 2, translated: false },
        { id: 3, translated: true }
    ]),
    $showWorkflowsActions: signal(true),
    patchViewParams: jest.fn(),
    orientation: signal(''),
    clearDeviceAndSocialMedia: jest.fn(),
    device: signal(DEFAULT_DEVICES.find((device) => device.inode === 'default')),
    $unlockButton: signal(null),
    $toggleLockOptions: signal(null),
    lockLoading: signal(false),
    toggleLock: jest.fn(),
    socialMedia: signal(null),
    trackUVECalendarChange: jest.fn(),
    paletteOpen: signal(false),
    setPaletteOpen: jest.fn()
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
    let messageService: MessageService;
    let confirmationService: ConfirmationService;
    let devicesService: DotDevicesService;
    let personalizeService: DotPersonalizeService;

    const fixedDate = new Date('2024-01-01');
    jest.spyOn(global, 'Date').mockImplementation(() => fixedDate);

    const createComponent = createComponentFactory({
        component: DotUveToolbarComponent,
        imports: [
            HttpClientTestingModule,
            MockComponent(DotEmaBookmarksComponent),
            MockComponent(DotEmaRunningExperimentComponent),
            MockComponent(EditEmaPersonaSelectorComponent),
            MockComponent(DotUveWorkflowActionsComponent),
            MockComponent(DotUveDeviceSelectorComponent),
            MockComponent(DotEditorModeSelectorComponent)
        ],
        providers: [
            UVEStore,
            provideHttpClientTesting(),
            {
                provide: DotAnalyticsTrackerService,
                useValue: {
                    track: jest.fn()
                }
            },
            mockProvider(DotContentletLockerService, {
                unlock: jest.fn().mockReturnValue(of({}))
            }),
            mockProvider(ConfirmationService, {
                confirm: jest.fn()
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
                    add: jest.fn()
                }
            },
            {
                provide: DotDevicesService,
                useValue: {
                    get: jest.fn().mockReturnValue(of(mockDotDevices))
                }
            }
        ],
        componentProviders: [
            {
                provide: DotPersonalizeService,
                useValue: {
                    getPersonalize: jest.fn(),
                    personalized: jest.fn().mockReturnValue(of({}))
                }
            }
        ]
    });

    describe('base state', () => {
        beforeEach(() => {
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
            jest.clearAllMocks();
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
                    title: 'My super awesome blog post'
                };
                const spy = jest.spyOn(spectator.component.editUrlContentMap, 'emit');

                baseUVEState.$urlContentMap.set(contentlet);
                spectator.detectChanges();

                const button = spectator.query(byTestId('edit-url-content-map'));

                spectator.click(button);

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

        describe('unlock button', () => {
            it('should be null', () => {
                expect(spectator.query(byTestId('uve-toolbar-unlock-button'))).toBeNull();
            });

            it('should be true', () => {
                baseUVEState.$unlockButton.set({
                    inode: '123',
                    disabled: false,
                    loading: false,
                    info: {
                        message: 'editpage.toolbar.page.release.lock.locked.by.user',
                        args: ['John Doe']
                    }
                });
                spectator.detectChanges();

                expect(spectator.query(byTestId('uve-toolbar-unlock-button'))).toBeTruthy();
            });

            it('should be disabled', () => {
                baseUVEState.$unlockButton.set({
                    disabled: true,
                    loading: false,
                    info: {
                        message: 'editpage.toolbar.page.release.lock.locked.by.user',
                        args: ['John Doe']
                    },
                    inode: '123'
                });
                spectator.detectChanges();
                expect(
                    spectator
                        .query(byTestId('uve-toolbar-unlock-button'))
                        .getAttribute('ng-reflect-disabled')
                ).toEqual('true');
            });

            it('should be loading', () => {
                baseUVEState.$unlockButton.set({
                    loading: true,
                    disabled: false,
                    inode: '123',
                    info: {
                        message: 'editpage.toolbar.page.release.lock.locked.by.user',
                        args: ['John Doe']
                    }
                });
                spectator.detectChanges();
                expect(
                    spectator
                        .query(byTestId('uve-toolbar-unlock-button'))
                        .getAttribute('ng-reflect-loading')
                ).toEqual('true');
            });

            it('should call store.toggleLock when unlock button is clicked', () => {
                const spy = jest.spyOn(store, 'toggleLock');

                baseUVEState.$unlockButton.set({
                    loading: false,
                    disabled: false,
                    inode: '123',
                    info: {
                        message: 'editpage.toolbar.page.release.lock.locked.by.user',
                        args: ['John Doe']
                    }
                });
                spectator.detectChanges();

                spectator.click(byTestId('uve-toolbar-unlock-button'));

                // The unlock button calls toggleLock with the inode, true (is locked), and false (not locked by current user)
                expect(spy).toHaveBeenCalledWith('123', true, false);
            });
        });

        describe('dot-ema-bookmarks', () => {
            it('should have attr', () => {
                const bookmarks = spectator.query(DotEmaBookmarksComponent);

                expect(bookmarks.url).toBe('/test-url?host_id=123-xyz-567-xxl&language_id=1');
            });
        });

        describe('dot-ema-running-experiment', () => {
            it('should be null', () => {
                expect(spectator.query(byTestId('uve-toolbar-running-experiment'))).toBeNull();
            });
        });

        describe('copy-url', () => {
            let button: DebugElement;

            beforeEach(() => {
                button = spectator.debugElement.query(
                    By.css('[data-testId="uve-toolbar-copy-url"]')
                );
            });

            it('should have attrs', () => {
                expect(button.attributes).toEqual({
                    icon: 'pi pi-copy',
                    'data-testId': 'uve-toolbar-copy-url',
                    'ng-reflect-style-class': 'p-button-text p-button-sm p-bu',
                    'ng-reflect-icon': 'pi pi-copy',
                    'ng-reflect-text': 'http://localhost:3000/test-url',
                    'ng-reflect-tooltip-position': 'bottom',
                    tooltipPosition: 'bottom',
                    styleClass: 'p-button-text p-button-sm p-button-rounded'
                });
            });

            it('should call messageService.add', () => {
                spectator.triggerEventHandler(button, 'cdkCopyToClipboardCopied', {});

                expect(messageService.add).toHaveBeenCalledWith({
                    severity: 'success',
                    summary: 'Copied',
                    life: 3000
                });
            });
        });

        describe('$copyURL computed signal', () => {
            it('should construct URL with clientHost and url from pageParams', () => {
                baseUVEState.pageParams.set({
                    ...params,
                    url: '/my-page',
                    clientHost: 'https://example.com'
                });
                spectator.detectChanges();

                const copyButton = spectator.query(byTestId('uve-toolbar-copy-url'));
                const copyURL = copyButton.getAttribute('ng-reflect-text');

                expect(copyURL).toBe('https://example.com/my-page');
            });

            it('should strip /index suffix from URL', () => {
                baseUVEState.pageParams.set({
                    ...params,
                    url: '/my-page/index',
                    clientHost: 'https://example.com'
                });
                spectator.detectChanges();

                const copyButton = spectator.query(byTestId('uve-toolbar-copy-url'));
                const copyURL = copyButton.getAttribute('ng-reflect-text');

                expect(copyURL).toBe('https://example.com/my-page');
            });

            it('should strip /index.html suffix from URL', () => {
                baseUVEState.pageParams.set({
                    ...params,
                    url: '/my-page/index.html',
                    clientHost: 'https://example.com'
                });
                spectator.detectChanges();

                const copyButton = spectator.query(byTestId('uve-toolbar-copy-url'));
                const copyURL = copyButton.getAttribute('ng-reflect-text');

                expect(copyURL).toBe('https://example.com/my-page');
            });

            it('should fallback to window.location.origin when clientHost is not provided', () => {
                baseUVEState.pageParams.set({
                    ...params,
                    url: '/my-page',
                    clientHost: undefined
                });
                spectator.detectChanges();

                const copyButton = spectator.query(byTestId('uve-toolbar-copy-url'));
                const copyURL = copyButton.getAttribute('ng-reflect-text');

                expect(copyURL).toBe(`${window.location.origin}/my-page`);
            });

            it('should handle root URL with index.html', () => {
                baseUVEState.pageParams.set({
                    ...params,
                    url: '/index.html',
                    clientHost: 'https://example.com'
                });
                spectator.detectChanges();

                const copyButton = spectator.query(byTestId('uve-toolbar-copy-url'));
                const copyURL = copyButton.getAttribute('ng-reflect-text');

                expect(copyURL).toBe('https://example.com/');
            });

            it('should handle root URL with just /index', () => {
                baseUVEState.pageParams.set({
                    ...params,
                    url: '/index',
                    clientHost: 'https://example.com'
                });
                spectator.detectChanges();

                const copyButton = spectator.query(byTestId('uve-toolbar-copy-url'));
                const copyURL = copyButton.getAttribute('ng-reflect-text');

                expect(copyURL).toBe('https://example.com/');
            });

            it('should handle URL without index suffix (no change)', () => {
                baseUVEState.pageParams.set({
                    ...params,
                    url: '/about-us',
                    clientHost: 'https://example.com'
                });
                spectator.detectChanges();

                const copyButton = spectator.query(byTestId('uve-toolbar-copy-url'));
                const copyURL = copyButton.getAttribute('ng-reflect-text');

                expect(copyURL).toBe('https://example.com/about-us');
            });

            it('should handle nested paths with /index.html', () => {
                baseUVEState.pageParams.set({
                    ...params,
                    url: '/docs/api/index.html',
                    clientHost: 'https://example.com'
                });
                spectator.detectChanges();

                const copyButton = spectator.query(byTestId('uve-toolbar-copy-url'));
                const copyURL = copyButton.getAttribute('ng-reflect-text');

                expect(copyURL).toBe('https://example.com/docs/api');
            });

            it('should default to root path when url is undefined', () => {
                baseUVEState.pageParams.set({
                    ...params,
                    url: undefined,
                    clientHost: 'https://example.com'
                });
                spectator.detectChanges();

                const copyButton = spectator.query(byTestId('uve-toolbar-copy-url'));
                const copyURL = copyButton.getAttribute('ng-reflect-text');

                expect(copyURL).toBe('https://example.com/');
            });

            it('should handle empty clientHost with fallback to window.location.origin', () => {
                baseUVEState.pageParams.set({
                    ...params,
                    url: '/test',
                    clientHost: ''
                });
                spectator.detectChanges();

                const copyButton = spectator.query(byTestId('uve-toolbar-copy-url'));
                const copyURL = copyButton.getAttribute('ng-reflect-text');

                expect(copyURL).toBe(`${window.location.origin}/test`);
            });
        });

        describe('API URL', () => {
            it('should have api link button', () => {
                expect(spectator.query(byTestId('uve-toolbar-api-link'))).toBeTruthy();
            });

            it('should have api link button with correct href', () => {
                const btn = spectator.query(byTestId('uve-toolbar-api-link'));
                expect(btn.getAttribute('href')).toBe($apiURL);
            });
        });

        describe('dot-edit-ema-persona-selector', () => {
            it('should have attr', () => {
                const personaSelector = spectator.query(EditEmaPersonaSelectorComponent);

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

            it('should personalize - no confirmation', () => {
                const spyloadPageAsset = jest.spyOn(store, 'loadPageAsset');
                spectator.triggerEventHandler(EditEmaPersonaSelectorComponent, 'selected', {
                    ...personaEventMock,
                    personalized: true
                });
                spectator.detectChanges();

                expect(spyloadPageAsset).toHaveBeenCalledWith({
                    [PERSONA_KEY]: '123'
                });
            });

            it('should personalize - confirmation', () => {
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
                const spyPersonalized = jest.spyOn(personalizeService, 'personalized');
                const spyMessageService = jest.spyOn(messageService, 'add');

                spectator.triggerEventHandler(EditEmaPersonaSelectorComponent, 'selected', {
                    ...personaEventMock,
                    personalized: false
                });

                const acceptFn = (confirmationService.confirm as jest.Mock).mock.calls[0][0].accept;

                spyPersonalized.mockReturnValue(
                    throwError(new Error('Personalization confirmation failed'))
                );

                acceptFn();
                spectator.detectChanges();

                expect(spyMessageService).toHaveBeenCalledWith({
                    severity: 'error',
                    summary: 'error',
                    detail: 'uve.personalize.empty.page.error'
                });
            });

            it('should despersonalize', () => {
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

            it('should call loadPageAsset when language is selected and exists that page translated', () => {
                const spyLoadPageAsset = jest.spyOn(baseUVEState, 'loadPageAsset');

                spectator.triggerEventHandler(EditEmaLanguageSelectorComponent, 'selected', 1);

                expect(spyLoadPageAsset).toHaveBeenCalled();
            });

            it('should call confirmationService.confirm when language is selected and does not exist that page translated', () => {
                const spyConfirmationService = jest.spyOn(confirmationService, 'confirm');

                spectator.triggerEventHandler(EditEmaLanguageSelectorComponent, 'selected', 2);
                spectator.detectChanges();
                expect(spyConfirmationService).toHaveBeenCalled();
            });
        });

        it('should have not experiments button if experiment is not running', () => {
            expect(spectator.query(byTestId('uve-toolbar-running-experiment'))).toBeFalsy();
        });

        it('should have persona selector', () => {
            expect(spectator.query(byTestId('uve-toolbar-persona-selector'))).toBeTruthy();
        });

        describe('toggle lock button', () => {
            it('should not display toggle lock button when feature is disabled', () => {
                baseUVEState.$toggleLockOptions.set(null);
                spectator.detectChanges();

                expect(spectator.query(byTestId('toggle-lock-button'))).toBeNull();
            });

            it('should display toggle lock button when toggle lock options are available', () => {
                baseUVEState.$toggleLockOptions.set({
                    inode: 'test-inode',
                    isLocked: false,
                    lockedBy: '',
                    canLock: true,
                    isLockedByCurrentUser: false,
                    showBanner: false,
                    showOverlay: false
                });
                spectator.detectChanges();

                expect(spectator.query(byTestId('toggle-lock-button'))).toBeTruthy();
            });

            it('should display unlocked state when page is not locked', () => {
                baseUVEState.$toggleLockOptions.set({
                    inode: 'test-inode',
                    isLocked: false,
                    lockedBy: '',
                    canLock: true,
                    isLockedByCurrentUser: false,
                    showBanner: false,
                    showOverlay: false
                });
                spectator.detectChanges();

                const button = spectator.query(byTestId('toggle-lock-button'));
                expect(button.classList.contains('lock-button--unlocked')).toBe(true);
                expect(button.classList.contains('lock-button--locked')).toBe(false);
            });

            it('should display locked state when page is locked by current user', () => {
                baseUVEState.$toggleLockOptions.set({
                    inode: 'test-inode',
                    isLocked: true,
                    lockedBy: 'current-user',
                    canLock: true,
                    isLockedByCurrentUser: true,
                    showBanner: false,
                    showOverlay: false
                });
                spectator.detectChanges();

                const button = spectator.query(byTestId('toggle-lock-button'));
                expect(button.classList.contains('lock-button--locked')).toBe(true);
                expect(button.classList.contains('lock-button--unlocked')).toBe(false);
            });

            it('should call store.toggleLock when unlocked button is clicked', () => {
                const spy = jest.spyOn(store, 'toggleLock');

                baseUVEState.$toggleLockOptions.set({
                    inode: 'test-inode-unlock',
                    isLocked: false,
                    lockedBy: '',
                    canLock: true,
                    isLockedByCurrentUser: false,
                    showBanner: false,
                    showOverlay: false
                });
                spectator.detectChanges();

                const button = spectator.query(byTestId('toggle-lock-button'));
                spectator.click(button);

                expect(spy).toHaveBeenCalledWith('test-inode-unlock', false, false);
            });

            it('should call store.toggleLock when locked button is clicked', () => {
                const spy = jest.spyOn(store, 'toggleLock');

                baseUVEState.$toggleLockOptions.set({
                    inode: 'test-inode-lock',
                    isLocked: true,
                    lockedBy: 'current-user',
                    canLock: true,
                    isLockedByCurrentUser: true,
                    showBanner: false,
                    showOverlay: false
                });
                spectator.detectChanges();

                const button = spectator.query(byTestId('toggle-lock-button'));
                spectator.click(button);

                expect(spy).toHaveBeenCalledWith('test-inode-lock', true, true);
            });

            it('should disable button when lock operation is loading', () => {
                baseUVEState.$toggleLockOptions.set({
                    inode: 'test-inode',
                    isLocked: false,
                    lockedBy: '',
                    canLock: true,
                    isLockedByCurrentUser: false,
                    showBanner: false,
                    showOverlay: false
                });
                baseUVEState.lockLoading.set(true);
                spectator.detectChanges();

                const button = spectator.query(byTestId('toggle-lock-button'));
                expect(button.hasAttribute('disabled')).toBe(true);
            });

            it('should enable button when lock operation is not loading', () => {
                baseUVEState.$toggleLockOptions.set({
                    inode: 'test-inode',
                    isLocked: false,
                    lockedBy: '',
                    canLock: true,
                    isLockedByCurrentUser: false,
                    showBanner: false,
                    showOverlay: false
                });
                baseUVEState.lockLoading.set(false);
                spectator.detectChanges();

                const button = spectator.query(byTestId('toggle-lock-button'));
                expect(button.hasAttribute('disabled')).toBe(false);
            });

            it('should call store.toggleLock with correct params for page locked by another user', () => {
                const spy = jest.spyOn(store, 'toggleLock');

                baseUVEState.$toggleLockOptions.set({
                    inode: 'test-inode-other',
                    isLocked: true,
                    lockedBy: 'another-user',
                    canLock: true,
                    isLockedByCurrentUser: false,
                    showBanner: true,
                    showOverlay: true
                });
                spectator.detectChanges();

                const button = spectator.query(byTestId('toggle-lock-button'));
                spectator.click(button);

                expect(spy).toHaveBeenCalledWith('test-inode-other', true, false);
            });
        });

        describe('palette toggle button', () => {
            it('should not display palette toggle button when not in edit mode', () => {
                baseUVEState.$isEditMode.set(false);
                spectator.detectChanges();

                expect(spectator.query(byTestId('uve-toolbar-palette-toggle'))).toBeNull();
            });

            it('should display palette toggle button when in edit mode', () => {
                baseUVEState.$isEditMode.set(true);
                spectator.detectChanges();

                expect(spectator.query(byTestId('uve-toolbar-palette-toggle'))).toBeTruthy();
            });

            it('should call setPaletteOpen with true when palette is closed', () => {
                const spy = jest.spyOn(store, 'setPaletteOpen');
                baseUVEState.$isEditMode.set(true);
                baseUVEState.paletteOpen.set(false);
                spectator.detectChanges();

                const button = spectator.query(byTestId('uve-toolbar-palette-toggle'));
                spectator.click(button);

                expect(spy).toHaveBeenCalledWith(true);
            });

            it('should call setPaletteOpen with false when palette is open', () => {
                const spy = jest.spyOn(store, 'setPaletteOpen');
                baseUVEState.$isEditMode.set(true);
                baseUVEState.paletteOpen.set(true);
                spectator.detectChanges();

                const button = spectator.query(byTestId('uve-toolbar-palette-toggle'));
                spectator.click(button);

                expect(spy).toHaveBeenCalledWith(false);
            });

            it('should show close icon and hide open icon when palette is closed', () => {
                baseUVEState.$isEditMode.set(true);
                baseUVEState.paletteOpen.set(false);
                spectator.detectChanges();

                const openIcon = spectator.query(byTestId('palette-open-icon'));
                const closeIcon = spectator.query(byTestId('palette-close-icon'));

                // When palette is closed, we show the "close" icon (to open it)
                // The open icon should be hidden
                expect(openIcon.classList.contains('hidden')).toBe(true);
                expect(closeIcon.classList.contains('hidden')).toBe(false);
            });

            it('should show open icon and hide close icon when palette is open', () => {
                baseUVEState.$isEditMode.set(true);
                baseUVEState.paletteOpen.set(true);
                spectator.detectChanges();

                const openIcon = spectator.query(byTestId('palette-open-icon'));
                const closeIcon = spectator.query(byTestId('palette-close-icon'));

                // When palette is open, we show the "open" icon (to close it)
                // The close icon should be hidden
                expect(openIcon.classList.contains('hidden')).toBe(false);
                expect(closeIcon.classList.contains('hidden')).toBe(true);
            });
        });
    });

    describe('preview', () => {
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

        it('should have a copy url button', () => {
            expect(spectator.query(byTestId('uve-toolbar-copy-url'))).toBeTruthy();
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

                expect(spectator.query('p-calendar')).toBeFalsy();
            });
        });
    });
    describe('live', () => {
        const previewBaseUveState = {
            ...baseUVEState,
            $isPreviewMode: signal(false),
            $isLiveMode: signal(true)
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

        it('should have a copy url button', () => {
            expect(spectator.query(byTestId('uve-toolbar-copy-url'))).toBeTruthy();
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
            const originalHasInstance = Object.getOwnPropertyDescriptor(Date, Symbol.hasInstance);
            const originalUTC = Object.getOwnPropertyDescriptor(Date, 'UTC');

            // We need to mock Date instanceof check and Date.UTC to avoid jest errors when running the tests
            // More info here: https://github.com/jestjs/jest/issues/11808
            Object.defineProperty(Date, Symbol.hasInstance, {
                value: function () {
                    return true;
                }
            });

            Object.defineProperty(Date, 'UTC', {
                value: function (_args) {
                    return new Date();
                }
            });

            afterAll(() => {
                // Restore original instanceof behavior
                if (originalHasInstance) {
                    Object.defineProperty(Date, Symbol.hasInstance, originalHasInstance);
                }

                // Restore original UTC behavior
                if (originalUTC) {
                    Object.defineProperty(Date, 'UTC', originalUTC);
                }
            });

            it('should show calendar when in live mode', () => {
                expect(spectator.query('p-calendar')).toBeTruthy();
            });

            it('should show calendar when in live mode and socialMedia is false', () => {
                baseUVEState.socialMedia.set(null);
                spectator.detectChanges();

                expect(spectator.query('p-calendar')).toBeTruthy();
            });

            it('should not show calendar when socialMedia has a value', () => {
                baseUVEState.socialMedia.set('faceboook');
                spectator.detectChanges();

                expect(spectator.query('p-calendar')).toBeFalsy();
            });

            it('should not show calendar when not in live mode', () => {
                baseUVEState.$isPreviewMode.set(false);
                baseUVEState.$isLiveMode.set(false);
                spectator.detectChanges();

                expect(spectator.query('p-calendar')).toBeFalsy();
            });

            it('should have a minDate of current date on 0h 0min 0s 0ms', () => {
                baseUVEState.$isPreviewMode.set(false);
                baseUVEState.$isLiveMode.set(true);
                baseUVEState.socialMedia.set(null);
                spectator.detectChanges();

                const calendar = spectator.query('p-calendar');

                const expectedMinDate = new Date(fixedDate);

                expectedMinDate.setHours(0, 0, 0, 0);

                expect(calendar.getAttribute('ng-reflect-min-date')).toBeDefined();
                expect(new Date(calendar.getAttribute('ng-reflect-min-date'))).toEqual(
                    expectedMinDate
                );
            });

            it('should load page on date when date is selected', () => {
                const spyLoadPageAsset = jest.spyOn(baseUVEState, 'loadPageAsset');

                const calendar = spectator.debugElement.query(
                    By.css('[data-testId="uve-toolbar-calendar"]')
                );

                const date = new Date();

                spectator.triggerEventHandler(calendar, 'ngModelChange', date);

                expect(spyLoadPageAsset).toHaveBeenCalledWith({
                    mode: UVE_MODE.LIVE,
                    publishDate: convertLocalTimeToUTC(date)
                });
            });

            it('should change the date to today when button "Today" is clicked', () => {
                const calendar = spectator.query('p-calendar');

                spectator.triggerEventHandler('p-calendar', 'click', new Event('click'));

                expect(calendar.getAttribute('ng-reflect-model')).toBeDefined();
                expect(new Date(calendar.getAttribute('ng-reflect-model'))).toEqual(new Date());
            });

            it('should track event on date when date is selected', () => {
                const spyTrackUVECalendarChange = jest.spyOn(
                    baseUVEState,
                    'trackUVECalendarChange'
                );

                const calendar = spectator.debugElement.query(
                    By.css('[data-testId="uve-toolbar-calendar"]')
                );

                const date = new Date();

                spectator.triggerEventHandler(calendar, 'ngModelChange', date);

                expect(spyTrackUVECalendarChange).toHaveBeenCalledWith({
                    selectedDate: convertLocalTimeToUTC(date)
                });
            });

            it('should fetch date when clicking on today button', () => {
                const spyLoadPageAsset = jest.spyOn(baseUVEState, 'loadPageAsset');
                const calendar = spectator.query(byTestId('uve-toolbar-calendar-today-button'));

                calendar.dispatchEvent(new Event('click'));

                expect(spyLoadPageAsset).toHaveBeenCalledWith({
                    mode: UVE_MODE.LIVE,
                    publishDate: expect.any(String)
                });
            });

            it('should track event on today button', () => {
                const spyTrackUVECalendarChange = jest.spyOn(
                    baseUVEState,
                    'trackUVECalendarChange'
                );

                const calendar = spectator.query(byTestId('uve-toolbar-calendar-today-button'));

                calendar.dispatchEvent(new Event('click'));

                expect(spyTrackUVECalendarChange).toHaveBeenCalledWith({
                    selectedDate: expect.any(String)
                });
            });
        });
    });

    describe('State changes', () => {
        beforeEach(() => {
            const state = {
                ...baseUVEState,
                $uveToolbar: signal({
                    ...baseUVEToolbarState,
                    runningExperiment: getRunningExperimentMock()
                })
            };

            spectator = createComponent({
                providers: [mockProvider(UVEStore, { ...state })]
            });
        });

        describe('Experiment is running', () => {
            it('should have experiment running component', () => {
                expect(spectator.query(byTestId('uve-toolbar-running-experiment'))).toBeTruthy();
            });
        });
    });
});
