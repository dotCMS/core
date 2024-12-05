import { expect, describe } from '@jest/globals';
import { byTestId, mockProvider, Spectator, createComponentFactory } from '@ngneat/spectator/jest';
import { MockComponent } from 'ng-mocks';
import { of } from 'rxjs';

import { HttpClientTestingModule, provideHttpClientTesting } from '@angular/common/http/testing';
import { DebugElement, signal } from '@angular/core';
import { By } from '@angular/platform-browser';

import { ConfirmationService, MessageService } from 'primeng/api';

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
import { EditEmaLanguageSelectorComponent } from '../edit-ema-language-selector/edit-ema-language-selector.component';

const $apiURL = '/api/v1/page/json/123-xyz-567-xxl?host_id=123-xyz-567-xxl&language_id=1';

describe('DotUveToolbarComponent', () => {
    let spectator: Spectator<DotUveToolbarComponent>;
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
            mockProvider(ConfirmationService, {
                confirm: jest.fn()
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
        languages: signal([
            { id: 1, translated: true },
            { id: 2, translated: false },
            { id: 3, translated: true }
        ])
    };

    describe('base state', () => {
        beforeEach(() => {
            spectator = createComponent({
                providers: [mockProvider(UVEStore, { ...baseUVEState })]
            });

            messageService = spectator.inject(MessageService);
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

        it('should have preview button', () => {
            expect(spectator.query(byTestId('uve-toolbar-preview'))).toBeTruthy();
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
                    'data-testId': 'uve-toolbar-copy-url',
                    icon: 'pi pi-external-link',
                    'ng-reflect-icon': 'pi pi-external-link',
                    'ng-reflect-style-class': 'p-button-text',
                    'ng-reflect-text': 'http://localhost:3000/test-url',
                    styleClass: 'p-button-text'
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

        it('should have not experiments button if experiment is not running', () => {
            expect(spectator.query(byTestId('uve-toolbar-running-experiment'))).toBeFalsy();
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

        it('should have persona selector', () => {
            expect(spectator.query(byTestId('uve-toolbar-persona-selector'))).toBeTruthy();
        });

        it('should have workflows button', () => {
            expect(spectator.query(byTestId('uve-toolbar-workflow-actions'))).toBeTruthy();
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
    });

    describe('State changes', () => {
        describe('Experiment is running', () => {
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

            it('should have experiment running component', () => {
                expect(spectator.query(byTestId('uve-toolbar-running-experiment'))).toBeTruthy();
            });
        });
    });
});
