import { byTestId, mockProvider, Spectator } from '@ngneat/spectator';
import { createComponentFactory } from '@ngneat/spectator/jest';
import { MockComponent } from 'ng-mocks';
import { of } from 'rxjs';

import { HttpClientTestingModule, provideHttpClientTesting } from '@angular/common/http/testing';
import { DebugElement, signal } from '@angular/core';
import { By } from '@angular/platform-browser';

import { MessageService } from 'primeng/api';

import { DotExperimentsService, DotLanguagesService, DotLicenseService } from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';
import {
    DotExperimentsServiceMock,
    DotLanguagesServiceMock,
    DotLicenseServiceMock,
    getRunningExperimentMock
} from '@dotcms/utils-testing';

import { DotUveToolbarComponent } from './dot-uve-toolbar.component';

import { DotPageApiService } from '../../../services/dot-page-api.service';
import { DEFAULT_PERSONA } from '../../../shared/consts';
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
import { DotEmaBookmarksComponent } from '../dot-ema-bookmarks/dot-ema-bookmarks.component';
import { DotEmaRunningExperimentComponent } from '../dot-ema-running-experiment/dot-ema-running-experiment.component';

const $apiURL = '/api/v1/page/json/123-xyz-567-xxl?host_id=123-xyz-567-xxl&language_id=1';

const params = HEADLESS_BASE_QUERY_PARAMS;
const url = sanitizeURL(params?.url);

const pageAPIQueryParams = createPageApiUrlWithQueryParams(url, params);
const pageAPI = `/api/v1/page/${'json'}/${pageAPIQueryParams}`;
const pageAPIResponse = MOCK_RESPONSE_HEADLESS;
const shouldShowInfoDisplay = false || pageAPIResponse?.page.locked || false || false;
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
    showInfoDisplay: shouldShowInfoDisplay,
    personaSelector: {
        pageId: pageAPIResponse?.page.identifier,
        value: pageAPIResponse?.viewAs.persona ?? DEFAULT_PERSONA
    }
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
    $isPreviewMode: signal(false)
};

describe('DotUveToolbarComponent', () => {
    let spectator: Spectator<DotUveToolbarComponent>;
    let store: InstanceType<typeof UVEStore>;
    let messageService: MessageService;

    const createComponent = createComponentFactory({
        component: DotUveToolbarComponent,
        imports: [
            HttpClientTestingModule,
            MockComponent(DotEmaBookmarksComponent),
            MockComponent(DotEmaRunningExperimentComponent)
        ],
        providers: [
            UVEStore,
            provideHttpClientTesting(),
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
            }
        ]
    });

    describe('base state', () => {
        beforeEach(() => {
            spectator = createComponent({
                providers: [mockProvider(UVEStore, { ...baseUVEState })]
            });

            store = spectator.inject(UVEStore, true);
            messageService = spectator.inject(MessageService, true);
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
                    'data-testId': 'uve-toolbar-copy-url',
                    icon: 'pi pi-external-link',
                    'ng-reflect-icon': 'pi pi-external-link',
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

                expect(spy).toHaveBeenCalledWith({ preview: 'true' });
            });
        });

        it('should have not experiments button if experiment is not running', () => {
            expect(spectator.query(byTestId('uve-toolbar-running-experiment'))).toBeFalsy();
        });

        it('should have language selector', () => {
            expect(spectator.query(byTestId('uve-toolbar-language-selector'))).toBeTruthy();
        });

        it('should have persona selector', () => {
            expect(spectator.query(byTestId('uve-toolbar-persona-selector'))).toBeTruthy();
        });

        it('should have workflows button', () => {
            expect(spectator.query(byTestId('uve-toolbar-workflow-actions'))).toBeTruthy();
        });
    });

    describe('preview', () => {
        beforeEach(() => {
            spectator = createComponent({
                providers: [
                    mockProvider(UVEStore, { ...baseUVEState, $isPreviewMode: signal(true) })
                ]
            });

            store = spectator.inject(UVEStore, true);
        });

        describe('Close Preview Mode', () => {
            it('should have api link button', () => {
                expect(spectator.query(byTestId('close-preview-mode'))).toBeTruthy();
            });

            it('should call store.loadPageAsset with preview null', () => {
                const spy = jest.spyOn(store, 'loadPageAsset');

                spectator.click(byTestId('close-preview-mode'));

                spectator.detectChanges();
                expect(spy).toHaveBeenCalledWith({ preview: null });
            });
        });

        it('should have desktop button', () => {
            expect(spectator.query(byTestId('desktop-preview'))).toBeTruthy();
        });

        it('should have mobile button', () => {
            expect(spectator.query(byTestId('mobile-preview'))).toBeTruthy();
        });

        it('should have tablet button', () => {
            expect(spectator.query(byTestId('tablet-preview'))).toBeTruthy();
        });

        it('should have more devices button', () => {
            expect(spectator.query(byTestId('more-devices-preview'))).toBeTruthy();
        });

        it('should not have experiments', () => {
            expect(spectator.query(byTestId('uve-toolbar-running-experiment'))).toBeFalsy();
        });

        it('should not have workflow actions', () => {
            expect(spectator.query(byTestId('uve-toolbar-workflow-actions'))).toBeFalsy();
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
