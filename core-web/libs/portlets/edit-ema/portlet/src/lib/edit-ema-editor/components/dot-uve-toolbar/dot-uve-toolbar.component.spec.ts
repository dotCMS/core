import { byTestId, mockProvider, Spectator } from '@ngneat/spectator';
import { createComponentFactory } from '@ngneat/spectator/jest';
import { MockComponent } from 'ng-mocks';
import { of } from 'rxjs';

import { HttpClientTestingModule, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';

import { DotExperimentsService, DotLanguagesService, DotLicenseService } from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';
import {
    DotExperimentsServiceMock,
    DotLanguagesServiceMock,
    DotLicenseServiceMock
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

describe('DotUveToolbarComponent', () => {
    let spectator: Spectator<DotUveToolbarComponent>;
    const createComponent = createComponentFactory({
        component: DotUveToolbarComponent,
        imports: [HttpClientTestingModule, MockComponent(DotEmaBookmarksComponent)],
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
                        $uveToolbar: signal({
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
                        }),
                        setDevice: jest.fn(),
                        setSocialMedia: jest.fn(),
                        pageParams: signal(params),
                        pageAPIResponse: signal(MOCK_RESPONSE_VTL),
                        reloadCurrentPage: jest.fn(),
                        loadPageAsset: jest.fn()
                    })
                ]
            });
        });

        describe('dot-ema-bookmarks', () => {
            it('should have attr', () => {
                const bookmarks = spectator.query(DotEmaBookmarksComponent);

                expect(bookmarks.url).toBe('/test-url?host_id=123-xyz-567-xxl&language_id=1');
            });
        });

        it('should have preview button', () => {
            expect(spectator.query(byTestId('uve-toolbar-preview'))).toBeTruthy();
        });

        it('should have copy url button', () => {
            expect(spectator.query(byTestId('uve-toolbar-copy-url'))).toBeTruthy();
        });

        it('should have api link button', () => {
            expect(spectator.query(byTestId('uve-toolbar-api-link'))).toBeTruthy();
        });

        it('should have experiments button', () => {
            expect(spectator.query(byTestId('uve-toolbar-running-experiment'))).toBeTruthy();
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
});
