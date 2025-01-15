import { expect, describe, it } from '@jest/globals';
import { byTestId, mockProvider, Spectator, createComponentFactory } from '@ngneat/spectator/jest';
import { MockComponent } from 'ng-mocks';
import { of } from 'rxjs';

import { HttpClientTestingModule, provideHttpClientTesting } from '@angular/common/http/testing';
import { DebugElement, signal } from '@angular/core';
import { By } from '@angular/platform-browser';

import { ConfirmationService, MessageService } from 'primeng/api';

import { UVE_MODE } from '@dotcms/client';
import {
    DotContentletLockerService,
    DotDevicesService,
    DotExperimentsService,
    DotLanguagesService,
    DotLicenseService,
    DotPersonalizeService,
    DotWorkflowsActionsService
} from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';
import {
    DotExperimentsServiceMock,
    DotLanguagesServiceMock,
    DotLicenseServiceMock,
    getRunningExperimentMock,
    mockDotDevices
} from '@dotcms/utils-testing';

import { DotEmaBookmarksComponent } from './components/dot-ema-bookmarks/dot-ema-bookmarks.component';
import { DotEmaRunningExperimentComponent } from './components/dot-ema-running-experiment/dot-ema-running-experiment.component';
import { DotUveDeviceSelectorComponent } from './components/dot-uve-device-selector/dot-uve-device-selector.component';
import { DotUveWorkflowActionsComponent } from './components/dot-uve-workflow-actions/dot-uve-workflow-actions.component';
import { EditEmaLanguageSelectorComponent } from './components/edit-ema-language-selector/edit-ema-language-selector.component';
import { EditEmaPersonaSelectorComponent } from './components/edit-ema-persona-selector/edit-ema-persona-selector.component';
import { DotUveToolbarComponent } from './dot-uve-toolbar.component';

import { DotPageApiService } from '../../../services/dot-page-api.service';
import { DEFAULT_DEVICES, DEFAULT_PERSONA } from '../../../shared/consts';
import {
    HEADLESS_BASE_QUERY_PARAMS,
    MOCK_RESPONSE_HEADLESS,
    MOCK_RESPONSE_VTL
} from '../../../shared/mocks';
import { UVEStore } from '../../../store/dot-uve.store';
import {
    createFavoritePagesURL,
    createFullURL,
    createPageApiUrlWithQueryParams,
    sanitizeURL
} from '../../../utils';

const $apiURL = '/api/v1/page/json/123-xyz-567-xxl?host_id=123-xyz-567-xxl&language_id=1';

const params = HEADLESS_BASE_QUERY_PARAMS;
const url = sanitizeURL(params?.url);

const pageAPIQueryParams = createPageApiUrlWithQueryParams(url, params);
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
        copyUrl: createFullURL(params, pageAPIResponse?.site.identifier),
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
    setSocialMedia: jest.fn(),
    pageParams: signal(params),
    pageAPIResponse: signal(MOCK_RESPONSE_VTL),
    $apiURL: signal($apiURL),
    reloadCurrentPage: jest.fn(),
    loadPageAsset: jest.fn(),
    $isPreviewMode: signal(false),
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
    $unlockButton: signal(null)
};

describe('DotUveToolbarComponent', () => {
    let spectator: Spectator<DotUveToolbarComponent>;
    let store: InstanceType<typeof UVEStore>;
    let messageService: MessageService;
    let confirmationService: ConfirmationService;
    let devicesService: DotDevicesService;
    let dotContentletLockerService: DotContentletLockerService;

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
            MockComponent(DotUveDeviceSelectorComponent)
        ],
        providers: [
            UVEStore,
            provideHttpClientTesting(),
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
                    getPersonalize: jest.fn()
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
            dotContentletLockerService = spectator.inject(DotContentletLockerService);
        });

        it('should have a dot-uve-workflow-actions component', () => {
            const workflowActions = spectator.query(DotUveWorkflowActionsComponent);
            expect(workflowActions).toBeTruthy();
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

            it('should call dotContentletLockerService.unlockPage', () => {
                const spy = jest.spyOn(dotContentletLockerService, 'unlock');

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

                spectator.click(byTestId('uve-toolbar-unlock-button'));

                expect(spy).toHaveBeenCalledWith('123');
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
                    class: 'ng-star-inserted',
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

        describe('API URL', () => {
            it('should have api link button', () => {
                expect(spectator.query(byTestId('uve-toolbar-api-link'))).toBeTruthy();
            });

            it('should have api link button with correct href', () => {
                const btn = spectator.query(byTestId('uve-toolbar-api-link'));
                expect(btn.getAttribute('href')).toBe($apiURL);
            });
        });

        describe('Preview', () => {
            it('should have preview button', () => {
                expect(spectator.query(byTestId('uve-toolbar-preview'))).toBeTruthy();
            });

            it('should call store.loadPageAsset with preview true', () => {
                const spy = jest.spyOn(store, 'loadPageAsset');

                spectator.click(byTestId('uve-toolbar-preview'));

                expect(spy).toHaveBeenCalledWith({
                    editorMode: UVE_MODE.PREVIEW,
                    publishDate: fixedDate.toISOString()
                });
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
                    hostFolder: 'SYSTEM_HOST',
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
                    identifier: '123',
                    pageId: '123',
                    personalized: true
                    // eslint-disable-next-line @typescript-eslint/no-explicit-any
                } as any);
                spectator.detectChanges();

                expect(spyloadPageAsset).toHaveBeenCalledWith({
                    'com.dotmarketing.persona.id': '123'
                });
            });

            it('should personalize - confirmation', () => {
                spectator.triggerEventHandler(EditEmaPersonaSelectorComponent, 'selected', {
                    identifier: '123',
                    pageId: '123',
                    personalized: false
                    // eslint-disable-next-line @typescript-eslint/no-explicit-any
                } as any);
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

            it('should despersonalize', () => {
                spectator.triggerEventHandler(EditEmaPersonaSelectorComponent, 'despersonalize', {
                    identifier: '123',
                    pageId: '123',
                    personalized: true
                    // eslint-disable-next-line @typescript-eslint/no-explicit-any
                } as any);

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
                const spyConfirmationService = jest.spyOn(baseUVEState, 'loadPageAsset');

                spectator.triggerEventHandler(EditEmaLanguageSelectorComponent, 'selected', 2);

                expect(spyConfirmationService).toHaveBeenCalled();
            });
        });

        it('should have not experiments button if experiment is not running', () => {
            expect(spectator.query(byTestId('uve-toolbar-running-experiment'))).toBeFalsy();
        });

        it('should have persona selector', () => {
            expect(spectator.query(byTestId('uve-toolbar-persona-selector'))).toBeTruthy();
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

        describe('Close Preview Mode', () => {
            it('should have api link button', () => {
                expect(spectator.query(byTestId('close-preview-mode'))).toBeTruthy();
            });

            it('should call store.loadPageAsset without editorMode and publishDate', () => {
                const spy = jest.spyOn(store, 'loadPageAsset');

                spectator.click(byTestId('close-preview-mode'));

                spectator.detectChanges();
                expect(spy).toHaveBeenCalledWith({ editorMode: undefined, publishDate: undefined });
            });

            it('should call store.loadPageAsset when datePreview model is updated', () => {
                const spy = jest.spyOn(store, 'loadPageAsset');

                spectator.debugElement.componentInstance.$previewDate.set(new Date('2024-02-01'));
                spectator.detectChanges();

                expect(spy).toHaveBeenCalledWith({
                    editorMode: UVE_MODE.PREVIEW,
                    publishDate: new Date('2024-02-01').toISOString()
                });
            });

            it('should call store.loadPageAsset with currentDate when datePreview model is updated with a past date', () => {
                const spy = jest.spyOn(store, 'loadPageAsset');

                spectator.debugElement.componentInstance.$previewDate.set(new Date('2023-02-01'));
                spectator.detectChanges();

                expect(spy).toHaveBeenCalledWith({
                    editorMode: UVE_MODE.PREVIEW,
                    publishDate: fixedDate.toISOString()
                });
            });
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
