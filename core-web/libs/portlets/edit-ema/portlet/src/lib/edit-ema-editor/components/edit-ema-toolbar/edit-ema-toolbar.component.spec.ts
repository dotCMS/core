import { expect, describe, it } from '@jest/globals';
import {
    Spectator,
    SpyObject,
    byTestId,
    createComponentFactory,
    mockProvider
} from '@ngneat/spectator/jest';
import { MockComponent } from 'ng-mocks';
import { of } from 'rxjs';

import { DebugElement, signal } from '@angular/core';
import { By } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';

import { ConfirmationService, MessageService } from 'primeng/api';

import {
    DotContentletLockerService,
    DotExperimentsService,
    DotLanguagesService,
    DotLicenseService,
    DotMessageService,
    DotPersonalizeService
} from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';
import { DotDeviceSelectorSeoComponent } from '@dotcms/portlets/dot-ema/ui';
import {
    CurrentUserDataMock,
    DotLanguagesServiceMock,
    getRunningExperimentMock,
    mockDotDevices
} from '@dotcms/utils-testing';

import { EditEmaToolbarComponent } from './edit-ema-toolbar.component';

import { DotPageApiService } from '../../../services/dot-page-api.service';
import { DEFAULT_PERSONA } from '../../../shared/consts';
import {
    HEADLESS_BASE_QUERY_PARAMS,
    MOCK_RESPONSE_HEADLESS,
    PAGE_RESPONSE_BY_LANGUAGE_ID,
    URL_CONTENT_MAP_MOCK
} from '../../../shared/mocks';
import { UVEStore } from '../../../store/dot-uve.store';
import {
    sanitizeURL,
    createPageApiUrlWithQueryParams,
    createFavoritePagesURL,
    createFullURL
} from '../../../utils';
import { DotEditEmaWorkflowActionsComponent } from '../dot-edit-ema-workflow-actions/dot-edit-ema-workflow-actions.component';
import { DotEmaBookmarksComponent } from '../dot-ema-bookmarks/dot-ema-bookmarks.component';
import { DotEmaInfoDisplayComponent } from '../dot-ema-info-display/dot-ema-info-display.component';
import { DotEmaRunningExperimentComponent } from '../dot-ema-running-experiment/dot-ema-running-experiment.component';
import { EditEmaLanguageSelectorComponent } from '../edit-ema-language-selector/edit-ema-language-selector.component';
import { EditEmaPersonaSelectorComponent } from '../edit-ema-persona-selector/edit-ema-persona-selector.component';

describe('EditEmaToolbarComponent', () => {
    let spectator: Spectator<EditEmaToolbarComponent>;
    let store: SpyObject<InstanceType<typeof UVEStore>>;
    let messageService: MessageService;
    let router: Router;
    let confirmationService: ConfirmationService;

    const createComponent = createComponentFactory({
        component: EditEmaToolbarComponent,
        imports: [
            MockComponent(DotDeviceSelectorSeoComponent),
            MockComponent(DotEditEmaWorkflowActionsComponent),
            MockComponent(DotEmaBookmarksComponent),
            MockComponent(DotEmaInfoDisplayComponent),
            MockComponent(DotEmaRunningExperimentComponent),
            MockComponent(EditEmaLanguageSelectorComponent),
            MockComponent(EditEmaPersonaSelectorComponent)
        ],
        providers: [
            UVEStore,
            mockProvider(ActivatedRoute),
            mockProvider(DotExperimentsService),
            mockProvider(Router),
            mockProvider(DotContentletLockerService),
            mockProvider(ConfirmationService, {
                confirm: jest.fn()
            }),
            mockProvider(MessageService, {
                add: jest.fn()
            }),
            mockProvider(DotMessageService, {
                get: (key) => {
                    const data = {
                        Copied: 'Copied',
                        'editpage.personalization.confirm.header': 'Personalize',
                        'editpage.personalization.confirm.message': 'Confirm personalization?',
                        'editpage.personalization.delete.confirm.header': 'Despersonalization?',
                        'editpage.personalization.delete.confirm.message':
                            'Confirm despersonalization?',
                        'dot.common.dialog.accept': 'Accept',
                        'dot.common.dialog.reject': 'Reject'
                    };

                    return data[key];
                }
            }),
            {
                provide: DotLanguagesService,
                useValue: new DotLanguagesServiceMock()
            },
            {
                provide: DotLicenseService,
                useValue: {
                    isEnterprise: () => of(true)
                }
            },
            {
                provide: LoginService,
                useValue: {
                    getCurrentUser: () => of(CurrentUserDataMock)
                }
            },
            {
                provide: DotPageApiService,
                useValue: {
                    get({ language_id }) {
                        return PAGE_RESPONSE_BY_LANGUAGE_ID[language_id];
                    }
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

    const params = HEADLESS_BASE_QUERY_PARAMS;
    const url = sanitizeURL(params?.url);

    const pageAPIQueryParams = createPageApiUrlWithQueryParams(url, params);
    const pageAPIResponse = MOCK_RESPONSE_HEADLESS;

    const pageAPI = `/api/v1/page/${'json'}/${pageAPIQueryParams}`;

    const shouldShowInfoDisplay = false || pageAPIResponse?.page.locked || false || false;

    const bookmarksUrl = createFavoritePagesURL({
        languageId: Number(params?.language_id),
        pageURI: url,
        siteId: pageAPIResponse?.site.identifier
    });

    describe('base state', () => {
        beforeEach(() => {
            spectator = createComponent({
                providers: [
                    mockProvider(UVEStore, {
                        $toolbarProps: signal({
                            bookmarksUrl,
                            copyUrl: createFullURL(params, pageAPIResponse?.site.identifier),
                            apiUrl: `${'http://localhost'}${pageAPI}`,
                            currentLanguage: pageAPIResponse?.viewAs.language,
                            urlContentMap: null,
                            runningExperiment: null,
                            workflowActionsInode: pageAPIResponse?.page.inode,
                            unlockButton: null,
                            showInfoDisplay: shouldShowInfoDisplay,
                            deviceSelector: {
                                apiLink: `${params?.clientHost ?? 'http://localhost'}${pageAPI}`,
                                hideSocialMedia: true
                            },
                            personaSelector: {
                                pageId: pageAPIResponse?.page.identifier,
                                value: pageAPIResponse?.viewAs.persona ?? DEFAULT_PERSONA
                            }
                        }),
                        setDevice: jest.fn(),
                        setSocialMedia: jest.fn(),
                        params: signal(params)
                    })
                ]
            });

            store = spectator.inject(UVEStore);
            messageService = spectator.inject(MessageService);
            router = spectator.inject(Router);
            confirmationService = spectator.inject(ConfirmationService);
        });

        describe('dot-device-selector-seo', () => {
            let deviceSelector: DebugElement;
            let emaPreviewButton: DebugElement;

            beforeEach(() => {
                deviceSelector = spectator.debugElement.query(
                    By.css('[data-testId="dot-device-selector"]')
                );
                emaPreviewButton = spectator.debugElement.query(
                    By.css('[data-testId="ema-preview"]')
                );
            });
            it('should have correct values', () => {
                const deviceSelectorComponent = deviceSelector.componentInstance;

                expect(deviceSelectorComponent.apiLink).toBe(
                    `${'http://localhost:3000'}${pageAPI}`
                );
                expect(deviceSelectorComponent.hideSocialMedia).toBe(true);
            });

            it('should call deviceSelector.openMenu', () => {
                const deviceSelector = spectator.debugElement.query(
                    By.css('[data-testId="dot-device-selector"]')
                );

                jest.spyOn(deviceSelector.componentInstance, 'openMenu');

                spectator.triggerEventHandler(emaPreviewButton, 'onClick', { hello: 'world' });
                spectator.detectChanges();

                expect(deviceSelector.componentInstance.openMenu).toHaveBeenCalledWith({
                    hello: 'world'
                });
            });

            it('should call store.setDevice', () => {
                jest.spyOn(store, 'setDevice');

                const iphone = { ...mockDotDevices[0], icon: 'someIcon' };

                spectator.triggerEventHandler(deviceSelector, 'selected', iphone);

                expect(store.setDevice).toHaveBeenCalledWith(iphone);
            });

            it('should call store.setSocialMedia', () => {
                jest.spyOn(store, 'setSocialMedia');

                const deviceSelector = spectator.debugElement.query(
                    By.css('[data-testId="dot-device-selector"]')
                );

                spectator.triggerEventHandler(deviceSelector, 'changeSeoMedia', 'facebook');
                spectator.detectChanges();

                expect(store.setSocialMedia).toHaveBeenCalledWith('facebook');
            });
        });

        describe('edit-url-content-map', () => {
            it('should be hidden', () => {
                const editURLContentButton = spectator.debugElement.query(
                    By.css('[data-testId="edit-url-content-map"]')
                );
                expect(editURLContentButton).toBeNull();
            });
        });

        describe('dot-ema-bookmarks', () => {
            it('should have attr', () => {
                const bookmarks = spectator.query(DotEmaBookmarksComponent);

                expect(bookmarks.url).toBe('/test-url?host_id=123-xyz-567-xxl&language_id=1');
            });
        });

        describe('ema-copy-url', () => {
            let button: DebugElement;

            beforeEach(() => {
                button = spectator.debugElement.query(By.css('[data-testId="ema-copy-url"]'));
            });

            it('should have attr', () => {
                expect(button.attributes).toEqual({
                    'data-testId': 'ema-copy-url',
                    icon: 'pi pi-copy',
                    'ng-reflect-icon': 'pi pi-copy',
                    'ng-reflect-style-class': 'p-button-text p-button-sm',
                    'ng-reflect-text': 'http://localhost:3000/test-url',
                    styleClass: 'p-button-text p-button-sm'
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

        describe('dot-edit-ema-language-selector', () => {
            it('should have attr', () => {
                const languageSelector = spectator.query(EditEmaLanguageSelectorComponent);

                expect(languageSelector.language).toEqual({
                    country: 'United States',
                    countryCode: 'US',
                    id: 1,
                    language: 'English',
                    languageCode: '1'
                });
            });

            it('should set language', () => {
                spectator.triggerEventHandler(EditEmaLanguageSelectorComponent, 'selected', 2);
                spectator.detectChanges();

                expect(router.navigate).toHaveBeenCalledWith([], {
                    queryParams: { language_id: 2 },
                    queryParamsHandling: 'merge'
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
                spectator.triggerEventHandler(EditEmaPersonaSelectorComponent, 'selected', {
                    identifier: '123',
                    pageId: '123',
                    personalized: true
                    // eslint-disable-next-line @typescript-eslint/no-explicit-any
                } as any);
                spectator.detectChanges();

                expect(router.navigate).toHaveBeenCalledWith([], {
                    queryParams: { 'com.dotmarketing.persona.id': '123' },
                    queryParamsHandling: 'merge'
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
                    acceptLabel: 'Accept',
                    header: 'Personalize',
                    message: 'Confirm personalization?',
                    reject: expect.any(Function),
                    rejectLabel: 'Reject'
                });
            });

            xit('should personalize - call service', () => {
                expect(true).toBe(true);
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
                    acceptLabel: 'Accept',
                    header: 'Despersonalization?',
                    message: 'Confirm despersonalization?',
                    rejectLabel: 'Reject'
                });
            });

            xit('should dpersonalize - call service', () => {
                expect(true).toBe(true);
            });
        });

        describe('dot-edit-ema-workflow-actions', () => {
            it('should have attr', () => {
                const workflowActions = spectator.query(DotEditEmaWorkflowActionsComponent);

                expect(workflowActions.inode).toBe('123-i');
            });

            it('should update page', () => {
                spectator.triggerEventHandler(DotEditEmaWorkflowActionsComponent, 'newPage', {
                    pageURI: '/path-and-stuff',
                    url: 'path',
                    languageId: 1
                    // eslint-disable-next-line @typescript-eslint/no-explicit-any
                } as any);

                spectator.detectChanges();

                expect(router.navigate).toHaveBeenCalledWith([], {
                    queryParams: {
                        clientHost: 'http://localhost:3000',
                        'com.dotmarketing.persona.id': 'dot:persona',
                        language_id: '1',
                        url: '/path-and-stuff',
                        variantName: 'DEFAULT'
                    },
                    queryParamsHandling: 'merge'
                });
            });
        });

        describe('dot-ema-info-display', () => {
            it('should be hidden', () => {
                const infoDisplay = spectator.query(byTestId('info-display'));
                expect(infoDisplay).toBeNull();
            });
        });

        describe('dot-ema-running-experiment', () => {
            it('should be hidden', () => {
                const experiments = spectator.query(byTestId('ema-running-experiment'));
                expect(experiments).toBeNull();
            });
        });
    });

    describe('constrains', () => {
        describe('dot-ema-info-display', () => {
            beforeEach(() => {
                spectator = createComponent({
                    providers: [
                        mockProvider(UVEStore, {
                            $toolbarProps: signal({
                                bookmarksUrl,
                                copyUrl: '',
                                apiUrl: '',
                                currentLanguage: pageAPIResponse?.viewAs.language,
                                urlContentMap: null,
                                runningExperiment: null,
                                workflowActionsInode: '',
                                unlockButton: null,
                                showInfoDisplay: true,
                                deviceSelector: {
                                    apiLink: '',
                                    hideSocialMedia: true
                                },
                                personaSelector: {
                                    pageId: '',
                                    value: DEFAULT_PERSONA
                                }
                            }),
                            setDevice: jest.fn(),
                            setSocialMedia: jest.fn(),
                            params: signal(params)
                        })
                    ]
                });
                store = spectator.inject(UVEStore);
                messageService = spectator.inject(MessageService);
                router = spectator.inject(Router);
                confirmationService = spectator.inject(ConfirmationService);
            });
            it('should show when showInfoDisplay is true in the store', () => {
                const infoDisplay = spectator.query(DotEmaInfoDisplayComponent);
                expect(infoDisplay).toBeDefined();
            });
        });
        describe('experiments', () => {
            const experiment = getRunningExperimentMock();

            beforeEach(() => {
                spectator = createComponent({
                    providers: [
                        mockProvider(UVEStore, {
                            $toolbarProps: signal({
                                bookmarksUrl,
                                copyUrl: '',
                                apiUrl: '',
                                currentLanguage: pageAPIResponse?.viewAs.language,
                                urlContentMap: null,
                                runningExperiment: experiment,
                                workflowActionsInode: '',
                                unlockButton: null,
                                showInfoDisplay: true,
                                deviceSelector: {
                                    apiLink: '',
                                    hideSocialMedia: true
                                },
                                personaSelector: {
                                    pageId: '',
                                    value: DEFAULT_PERSONA
                                }
                            }),
                            setDevice: jest.fn(),
                            setSocialMedia: jest.fn(),
                            params: signal(params)
                        })
                    ]
                });
            });

            describe('dot-ema-running-experiment', () => {
                it('should have attr', () => {
                    const experiments = spectator.query(DotEmaRunningExperimentComponent);
                    expect(experiments.runningExperiment).toEqual(experiment);
                });
            });
        });
        describe('urlContentMap', () => {
            beforeEach(() => {
                spectator = createComponent({
                    providers: [
                        mockProvider(UVEStore, {
                            $toolbarProps: signal({
                                bookmarksUrl,
                                copyUrl: '',
                                apiUrl: '',
                                currentLanguage: pageAPIResponse?.viewAs.language,
                                urlContentMap: URL_CONTENT_MAP_MOCK,
                                runningExperiment: getRunningExperimentMock(),
                                workflowActionsInode: '',
                                unlockButton: null,
                                showInfoDisplay: true,
                                deviceSelector: {
                                    apiLink: '',
                                    hideSocialMedia: true
                                },
                                personaSelector: {
                                    pageId: '',
                                    value: DEFAULT_PERSONA
                                }
                            }),
                            setDevice: jest.fn(),
                            setSocialMedia: jest.fn(),
                            params: signal(params)
                        })
                    ]
                });
            });

            it('should have attr', () => {
                const editURLContentButton = spectator.debugElement.query(
                    By.css('[data-testId="edit-url-content-map"]')
                );

                expect(editURLContentButton.attributes).toEqual({
                    'data-testId': 'edit-url-content-map',
                    icon: 'pi pi-pencil',
                    'ng-reflect-icon': 'pi pi-pencil',
                    'ng-reflect-style-class': 'p-button-text p-button-sm',
                    styleClass: 'p-button-text p-button-sm'
                });
            });
            it('should emit', () => {
                let output;
                spectator.output('editUrlContentMap').subscribe((result) => (output = result));

                const editURLContentButton = spectator.debugElement.query(
                    By.css('[data-testId="edit-url-content-map"]')
                );

                spectator.triggerEventHandler(editURLContentButton, 'onClick', {});

                expect(output).toEqual(URL_CONTENT_MAP_MOCK);
            });
        });

        describe('locked', () => {
            beforeEach(() => {
                spectator = createComponent({
                    providers: [
                        mockProvider(UVEStore, {
                            $toolbarProps: signal({
                                bookmarksUrl,
                                copyUrl: '',
                                apiUrl: '',
                                currentLanguage: pageAPIResponse?.viewAs.language,
                                urlContentMap: URL_CONTENT_MAP_MOCK,
                                runningExperiment: getRunningExperimentMock(),
                                workflowActionsInode: '',
                                unlockButton: {
                                    inode: '1234',
                                    isLoading: false
                                },
                                showInfoDisplay: true,
                                deviceSelector: {
                                    apiLink: '',
                                    hideSocialMedia: true
                                },
                                personaSelector: {
                                    pageId: '',
                                    value: DEFAULT_PERSONA
                                }
                            }),
                            setDevice: jest.fn(),
                            setSocialMedia: jest.fn(),
                            params: signal(params)
                        })
                    ]
                });
            });

            it('should render a unlock button when unlockButton is not null', () => {
                spectator.detectChanges();
                expect(spectator.query(byTestId('unlock-button'))).toBeDefined();
            });
        });
    });
});
