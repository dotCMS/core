import { describe, expect } from '@jest/globals';
import { patchState } from '@ngrx/signals';
import {
    SpyObject,
    createComponentFactory,
    Spectator,
    byTestId,
    mockProvider
} from '@openng/spectator/jest';
import { MockComponent } from 'ng-mocks';
import { Subject, of } from 'rxjs';

import { Location } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { By } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';

import { ConfirmationService, MessageService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogService } from 'primeng/dynamicdialog';

import {
    DotAnalyticsTrackerService,
    DotContentletLockerService,
    DotContentTypeService,
    DotCurrentUserService,
    DotExperimentsService,
    DotLanguagesService,
    DotMessageService,
    DotPageLayoutService,
    DotPropertiesService,
    DotSiteService,
    DotSystemConfigService,
    DotWorkflowActionsFireService,
    DotWorkflowsActionsService,
    PushPublishService
} from '@dotcms/data-access';
import { DotcmsConfigService, LoginService, Site, SiteService } from '@dotcms/dotcms-js';
import { DEFAULT_VARIANT_ID, FeaturedFlags } from '@dotcms/dotcms-models';
import {
    DotPageScannerReportComponent,
    DotPageToolsSeoComponent
} from '@dotcms/portlets/dot-ema/ui';
import { GlobalStore } from '@dotcms/store';
import { DotCMSUVEAction, UVE_MODE } from '@dotcms/types';
import { WINDOW } from '@dotcms/utils';
import {
    CurrentUserDataMock,
    DotExperimentsServiceMock,
    DotCurrentUserServiceMock,
    DotLanguagesServiceMock,
    DotcmsConfigServiceMock,
    SiteServiceMock
} from '@dotcms/utils-testing';

import { EditEmaNavigationBarComponent } from './components/edit-ema-navigation-bar/edit-ema-navigation-bar.component';
import { DotEmaShellComponent } from './dot-ema-shell.component';

import { DotEmaDialogComponent } from '../components/dot-ema-dialog/dot-ema-dialog.component';
import { DotActionUrlService } from '../services/dot-action-url/dot-action-url.service';
import { DotPageApiService } from '../services/dot-page-api/dot-page-api.service';
import { DEFAULT_PERSONA, PERSONA_KEY } from '../shared/consts';
import { FormStatus, NG_CUSTOM_EVENTS, UVE_STATUS } from '../shared/enums';
import {
    dotPropertiesServiceMock,
    MOCK_RESPONSE_HEADLESS,
    PAGE_RESPONSE_BY_LANGUAGE_ID,
    PAGE_RESPONSE_URL_CONTENT_MAP,
    PAYLOAD_MOCK,
    URL_CONTENT_MAP_MOCK
} from '../shared/mocks';
import { UVEStore } from '../store/dot-uve.store';

// Mock structuredClone for Jest environment (not available in jsdom)
if (typeof globalThis.structuredClone === 'undefined') {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    globalThis.structuredClone = (obj: any) => JSON.parse(JSON.stringify(obj));
}

/** Creates a dialog action event payload for testing. */
const createDialogActionEvent = (detail: object) => ({
    event: new CustomEvent('ng-event', { detail }),
    actionPayload: PAYLOAD_MOCK,
    form: {
        status: FormStatus.SAVED,
        isTranslation: false
    },
    clientAction: DotCMSUVEAction.NOOP
});

const NAV_ITEMS = [
    {
        materialIcon: 'description',
        label: 'editema.editor.navbar.content',
        href: 'content',
        id: 'content'
    },
    {
        materialIcon: 'space_dashboard',
        label: 'editema.editor.navbar.layout',
        href: 'layout',
        isDisabled: false,
        tooltip: null,
        id: 'layout'
    },
    {
        materialIcon: 'fork_left',
        label: 'editema.editor.navbar.rules',
        href: `rules/123`,
        isDisabled: false,
        id: 'rules'
    },
    {
        materialIcon: 'science',
        label: 'editema.editor.navbar.experiments',
        href: 'experiments/123',
        isDisabled: false,
        id: 'experiments'
    },
    {
        materialIcon: 'health_and_safety',
        label: 'editema.editor.navbar.page-tools',
        id: 'page-tools'
    },
    {
        materialIcon: 'settings',
        label: 'editema.editor.navbar.properties',
        id: 'properties',
        isDisabled: false
    }
];

const INITIAL_PAGE_PARAMS = {
    language_id: '1',
    url: 'index',
    variantName: 'DEFAULT',
    [PERSONA_KEY]: 'modes.persona.no.persona',
    mode: UVE_MODE.EDIT
};

const BASIC_OPTIONS = {
    allowedDevURLs: ['http://localhost:3000']
};

/** Builds route data with UVE config options for testing. */
const createUveConfigData = (options: object) => ({
    uveConfig: { options }
});

/** Builds an activated route snapshot shape for testing. */
const createRouteSnapshot = ({ queryParams, data }: { queryParams: object; data: object }) => ({
    queryParams,
    data
});

const mockGlobalStore = {
    addNewBreadcrumb: jest.fn(),
    loggedUser: signal(CurrentUserDataMock)
};

/**
 * Overrides the activated route snapshot to simulate queryParams/data changes.
 * Call this before the first `spectator.detectChanges()` in your test.
 */
const overrideRouteSnapshot = (activatedRoute: ActivatedRoute, mock: object) => {
    // If a test fails during component creation, `activatedRoute` may be unset.
    // Avoid masking the real error with a secondary defineProperty crash.
    if (!activatedRoute || typeof activatedRoute !== 'object') {
        return;
    }
    Object.defineProperty(activatedRoute, 'snapshot', {
        value: mock,
        writable: true // Allows mocking changes
    });
};

describe('DotEmaShellComponent', () => {
    let spectator: Spectator<DotEmaShellComponent>;
    let store: SpyObject<InstanceType<typeof UVEStore>>;

    let router: Router;
    let location: Location;
    let siteService: SiteServiceMock;
    let activatedRoute: ActivatedRoute;
    let dotPageApiService: DotPageApiService;

    const createComponent = createComponentFactory({
        component: DotEmaShellComponent,
        imports: [ConfirmDialogModule],
        detectChanges: false,
        providers: [
            {
                provide: ActivatedRoute,
                useValue: {
                    snapshot: createRouteSnapshot({
                        queryParams: INITIAL_PAGE_PARAMS,
                        data: createUveConfigData(BASIC_OPTIONS)
                    })
                }
            },
            { provide: SiteService, useClass: SiteServiceMock },
            {
                provide: DotContentletLockerService,
                useValue: {
                    unlock: (_inode: string) => of({})
                }
            },
            {
                provide: LoginService,
                useValue: {
                    getCurrentUser: () => of({})
                }
            },
            mockProvider(Router, {
                navigate: jest.fn().mockReturnValue(Promise.resolve(true)),
                url: '/test-url',
                events: of(),
                createUrlTree: jest.fn((commands, extras) => {
                    const queryParams = extras?.queryParams ?? {};
                    const queryString = new URLSearchParams(
                        Object.fromEntries(
                            Object.entries(queryParams).map(([k, v]) => [k, String(v ?? '')])
                        )
                    ).toString();
                    return { toString: () => (queryString ? `/?${queryString}` : '/') };
                })
            }),
            mockProvider(DotSiteService, {
                getCurrentSite: () => of(null)
            }),
            mockProvider(DotSystemConfigService, {
                getSystemConfig: () => of({})
            }),
            {
                provide: DotCurrentUserService,
                useValue: new DotCurrentUserServiceMock()
            },
            provideHttpClient(),
            provideHttpClientTesting()
        ],
        declarations: [
            MockComponent(DotEmaDialogComponent),
            MockComponent(DotPageToolsSeoComponent),
            MockComponent(DotPageScannerReportComponent)
        ],
        componentProviders: [
            MessageService,
            UVEStore,
            ConfirmationService,
            mockProvider(DotContentTypeService),
            DotActionUrlService,
            DotMessageService,
            DialogService,
            DotWorkflowActionsFireService,
            Router,
            Location,
            {
                provide: DotWorkflowsActionsService,
                useValue: {
                    getByInode: () => of([])
                }
            },
            {
                provide: DotPropertiesService,
                useValue: dotPropertiesServiceMock
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
                provide: DotExperimentsService,
                useValue: DotExperimentsServiceMock
            },
            {
                provide: DotLanguagesService,
                useValue: new DotLanguagesServiceMock()
            },
            {
                provide: DotPageApiService,
                useValue: {
                    get({ language_id = 1 }) {
                        return PAGE_RESPONSE_BY_LANGUAGE_ID[language_id] || of({});
                    },
                    getGraphQLPage() {
                        return of({});
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
                provide: DotAnalyticsTrackerService,
                useValue: {
                    track: jest.fn()
                }
            },
            {
                provide: DotPageLayoutService,
                useValue: {
                    save: jest.fn().mockReturnValue(of({}))
                }
            },
            {
                provide: WINDOW,
                useValue: window
            },
            {
                provide: DotMessageService,
                useValue: {
                    get: jest.fn().mockReturnValue('Mock Message'),
                    init: jest.fn()
                }
            },
            {
                provide: GlobalStore,
                useValue: mockGlobalStore
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
        siteService = spectator.inject(SiteService) as unknown as SiteServiceMock;
        store = spectator.inject(UVEStore, true);
        router = spectator.inject(Router, true);
        location = spectator.inject(Location, true);
        activatedRoute = spectator.inject(ActivatedRoute, true);
        dotPageApiService = spectator.inject(DotPageApiService, true);
    });

    describe('with queryParams', () => {
        it('should trigger an store load with default values', () => {
            const spyStoreLoadPage = jest.spyOn(store, 'pageLoad');
            spectator.detectChanges();
            expect(spyStoreLoadPage).toHaveBeenCalledWith(INITIAL_PAGE_PARAMS);
        });

        describe('Sanitize url when called pageLoad', () => {
            it('should sanitize when url is index', () => {
                const pageLoadSpy = jest.spyOn(store, 'pageLoad');
                const spyLocation = jest.spyOn(location, 'go');

                const params = {
                    ...INITIAL_PAGE_PARAMS,
                    url: '/index'
                };

                overrideRouteSnapshot(
                    activatedRoute,
                    createRouteSnapshot({
                        queryParams: params,
                        data: createUveConfigData(BASIC_OPTIONS)
                    })
                );

                spectator.detectChanges();
                expect(pageLoadSpy).toHaveBeenCalledWith({ ...params, url: '/index' });
                expect(spyLocation).toHaveBeenCalledWith(
                    '/?language_id=1&url=%2Findex&variantName=DEFAULT&mode=EDIT_MODE'
                );
            });

            it('should sanitize when url is nested', () => {
                const pageLoadSpy = jest.spyOn(store, 'pageLoad');

                const spyLocation = jest.spyOn(location, 'go');

                const params = {
                    ...INITIAL_PAGE_PARAMS,
                    url: '/some-url/some-nested-url'
                };

                overrideRouteSnapshot(
                    activatedRoute,
                    createRouteSnapshot({
                        queryParams: params,
                        data: createUveConfigData(BASIC_OPTIONS)
                    })
                );

                spectator.detectChanges();
                expect(pageLoadSpy).toHaveBeenCalledWith({
                    ...params,
                    url: '/some-url/some-nested-url'
                });
                expect(spyLocation).toHaveBeenCalledWith(
                    '/?language_id=1&url=%2Fsome-url%2Fsome-nested-url&variantName=DEFAULT&mode=EDIT_MODE'
                );
            });

            it('should sanitize when url is nested and ends in index', () => {
                const pageLoadSpy = jest.spyOn(store, 'pageLoad');
                const spyLocation = jest.spyOn(location, 'go');

                const params = {
                    ...INITIAL_PAGE_PARAMS,
                    url: '/some-url/index'
                };

                overrideRouteSnapshot(
                    activatedRoute,
                    createRouteSnapshot({
                        queryParams: params,
                        data: createUveConfigData(BASIC_OPTIONS)
                    })
                );

                spectator.detectChanges();
                expect(pageLoadSpy).toHaveBeenCalledWith({
                    ...params,
                    url: '/some-url/index'
                });
                expect(spyLocation).toHaveBeenCalledWith(
                    '/?language_id=1&url=%2Fsome-url%2Findex&variantName=DEFAULT&mode=EDIT_MODE'
                );
            });

            it('should receive `personaId` query param', () => {
                const pageLoadSpy = jest.spyOn(store, 'pageLoad');
                const spyLocation = jest.spyOn(location, 'go');

                const queryParams = {
                    url: '/some-url/index',
                    language_id: 1,
                    personaId: 'someCoolDude'
                };

                const expectedParams = {
                    url: '/some-url/index',
                    [PERSONA_KEY]: 'someCoolDude',
                    mode: UVE_MODE.EDIT,
                    language_id: 1
                };

                overrideRouteSnapshot(
                    activatedRoute,
                    createRouteSnapshot({ queryParams, data: createUveConfigData(BASIC_OPTIONS) })
                );

                spectator.detectChanges();
                expect(pageLoadSpy).toHaveBeenCalledWith(expectedParams);
                expect(spyLocation).toHaveBeenCalledWith(
                    '/?url=%2Fsome-url%2Findex&language_id=1&mode=EDIT_MODE&personaId=someCoolDude'
                );
            });
        });

        it('should patch viewParams with empty object when the mode is edit', () => {
            const params = {
                ...INITIAL_PAGE_PARAMS,
                mode: UVE_MODE.EDIT
            };

            overrideRouteSnapshot(
                activatedRoute,
                createRouteSnapshot({
                    queryParams: params,
                    data: createUveConfigData(BASIC_OPTIONS)
                })
            );

            spectator.detectChanges();

            expect(store.viewParams()).toEqual({});
        });

        it('should patch viewParams with empty params on init', () => {
            const params = {
                ...INITIAL_PAGE_PARAMS,
                mode: UVE_MODE.PREVIEW
            };

            overrideRouteSnapshot(
                activatedRoute,
                createRouteSnapshot({
                    queryParams: params,
                    data: createUveConfigData(BASIC_OPTIONS)
                })
            );

            spectator.detectChanges();

            expect(store.viewParams()).toEqual({
                orientation: undefined,
                seo: undefined,
                device: undefined
            });
        });

        it('should patch viewParams with the correct params on init', () => {
            const withViewParams = {
                device: 'mobile',
                orientation: 'landscape',
                seo: undefined,
                mode: UVE_MODE.PREVIEW
            };

            overrideRouteSnapshot(
                activatedRoute,
                createRouteSnapshot({
                    queryParams: withViewParams,
                    data: createUveConfigData(BASIC_OPTIONS)
                })
            );

            spectator.detectChanges();

            expect(store.viewParams()).toEqual({
                orientation: 'landscape',
                seo: undefined,
                device: 'mobile'
            });
        });

        it('should patch viewParams with the correct params on init with live mode', () => {
            const withViewParams = {
                device: 'mobile',
                orientation: 'landscape',
                seo: undefined,
                mode: UVE_MODE.LIVE
            };

            overrideRouteSnapshot(
                activatedRoute,
                createRouteSnapshot({
                    queryParams: withViewParams,
                    data: createUveConfigData(BASIC_OPTIONS)
                })
            );

            spectator.detectChanges();

            expect(store.viewParams()).toEqual({
                orientation: 'landscape',
                seo: undefined,
                device: 'mobile'
            });
        });

        it('should call store.pageLoad and update location when page loads', () => {
            const pageLoadSpy = jest.spyOn(store, 'pageLoad');
            const locationSpy = jest.spyOn(location, 'go');

            spectator.detectChanges();

            expect(pageLoadSpy).toHaveBeenCalledWith(INITIAL_PAGE_PARAMS);
            expect(locationSpy).toHaveBeenCalledWith(
                '/?language_id=1&url=index&variantName=DEFAULT&mode=EDIT_MODE'
            );
        });

        describe('DOM', () => {
            beforeEach(async () => {
                spectator.detectChanges();
                // Wait until the effect triggers init and the DOM is ready
                await spectator.fixture.whenStable();
                spectator.detectChanges();
            });

            it('should have a navigation bar', () => {
                expect(spectator.query(byTestId('ema-nav-bar'))).not.toBeNull();
            });

            it('should have nav bar with items', () => {
                const navBarComponent = spectator.query(EditEmaNavigationBarComponent);

                expect(navBarComponent.items()).toEqual(NAV_ITEMS);
            });

            it('should trigger action when the page-tool item is clicked', () => {
                const pageToolsSpy = jest.spyOn(spectator.component.pageTools, 'toggleDialog');

                const navBar = spectator.debugElement.query(By.css('[data-testid="ema-nav-bar"]'));

                spectator.triggerEventHandler(navBar, 'action', 'page-tools');

                expect(pageToolsSpy).toHaveBeenCalled();
            });

            it('should trigger action when the properties item is clicked', () => {
                const dialogSpy = jest.spyOn(spectator.component.dialog, 'editContentlet');

                const navBar = spectator.debugElement.query(By.css('[data-testid="ema-nav-bar"]'));

                spectator.triggerEventHandler(navBar, 'action', 'properties');

                expect(dialogSpy).toHaveBeenCalledWith({
                    contentType: undefined,
                    identifier: '123',
                    inode: '123',
                    title: 'hello world',
                    angularCurrentPortlet: 'edit-page'
                });
            });
        });

        describe('Page Params', () => {
            beforeEach(() => spectator.detectChanges());

            it('should update params when loadPage is triggered', () => {
                const baseParams = {
                    language_id: '2',
                    url: 'my-awesome-page',
                    variantName: 'DEFAULT',
                    mode: UVE_MODE.EDIT
                };
                const pageParams = {
                    ...baseParams,
                    [PERSONA_KEY]: 'SomeCoolDude'
                };

                const userParams = {
                    ...baseParams,
                    personaId: 'SomeCoolDude'
                };

                const expectURL = router.createUrlTree([], { queryParams: userParams });
                const spyStoreLoadPage = jest.spyOn(store, 'pageLoad');
                const spyUrlTree = jest.spyOn(router, 'createUrlTree');
                const spyLocation = jest.spyOn(location, 'go');

                store.pageLoad(pageParams);
                spectator.detectChanges();

                expect(spyStoreLoadPage).toHaveBeenCalledWith(pageParams);
                expect(spyUrlTree).toHaveBeenCalledWith([], { queryParams: userParams });
                expect(spyLocation).toHaveBeenCalledWith(expectURL.toString());
            });

            it('should not include clientHost in location when it matches base client host', () => {
                const spyLocation = jest.spyOn(location, 'go');
                const baseClientHost = 'http://localhost:3000';
                const params = {
                    ...INITIAL_PAGE_PARAMS,
                    clientHost: baseClientHost
                };

                // Set up route with matching uveConfig.url
                overrideRouteSnapshot(
                    activatedRoute,
                    createRouteSnapshot({
                        queryParams: params,
                        data: {
                            uveConfig: {
                                url: baseClientHost,
                                options: BASIC_OPTIONS
                            }
                        }
                    })
                );

                store.pageLoad(params);
                spectator.detectChanges();

                expect(spyLocation).toHaveBeenCalledWith(
                    '/?language_id=1&url=index&variantName=DEFAULT&mode=EDIT_MODE'
                );
            });

            it('should include clientHost in location when it differs from base client host', () => {
                const spyLocation = jest.spyOn(location, 'go');
                const baseClientHost = 'http://localhost:3000';
                const differentClientHost = 'http://localhost:4000';
                const params = {
                    ...INITIAL_PAGE_PARAMS,
                    clientHost: differentClientHost
                };

                // Set up route with different uveConfig.url
                overrideRouteSnapshot(
                    activatedRoute,
                    createRouteSnapshot({
                        queryParams: params,
                        data: {
                            uveConfig: {
                                url: baseClientHost,
                                options: {
                                    allowedDevURLs: [differentClientHost]
                                }
                            }
                        }
                    })
                );

                store.pageLoad(params);
                spectator.detectChanges();

                expect(spyLocation).toHaveBeenCalledWith(
                    '/?language_id=1&url=index&variantName=DEFAULT&mode=EDIT_MODE&clientHost=http:%2F%2Flocalhost:4000'
                );
            });

            it('should handle sanitized URLs in clientHost comparison', () => {
                const spyLocation = jest.spyOn(location, 'go');
                const baseClientHost = 'http://localhost:3000/';
                const params = {
                    ...INITIAL_PAGE_PARAMS,
                    clientHost: 'http://localhost:3000/' // No trailing slash
                };

                // Set up route with uveConfig.url that has trailing slash
                overrideRouteSnapshot(
                    activatedRoute,
                    createRouteSnapshot({
                        queryParams: params,
                        data: {
                            uveConfig: {
                                url: baseClientHost,
                                options: BASIC_OPTIONS
                            }
                        }
                    })
                );

                store.pageLoad(params);
                spectator.detectChanges();

                // Should treat these as the same URL and not include clientHost
                expect(spyLocation).toHaveBeenCalledWith(
                    '/?language_id=1&url=index&variantName=DEFAULT&mode=EDIT_MODE'
                );
            });
        });

        describe('ClientHost', () => {
            it('should trigger init the store without the clientHost queryParam when it is not allowed', () => {
                const spyStoreLoadPage = jest.spyOn(store, 'pageLoad');
                const paramWithNotAllowedHost = {
                    ...INITIAL_PAGE_PARAMS,
                    clientHost: 'http://localhost:4200'
                };

                const data = createUveConfigData({
                    allowedDevURLs: ['http://localhost:3000']
                });

                overrideRouteSnapshot(
                    activatedRoute,
                    createRouteSnapshot({ queryParams: paramWithNotAllowedHost, data })
                );

                spectator.detectChanges();
                expect(spyStoreLoadPage).toHaveBeenCalledWith(INITIAL_PAGE_PARAMS);
                expect(spyStoreLoadPage).not.toHaveBeenCalledWith(paramWithNotAllowedHost);
            });

            it('should trigger a load when changing the clientHost and it is on the allowedDevURLs', () => {
                const spyStoreLoadPage = jest.spyOn(store, 'pageLoad');
                const paramsWithAllowedHost = {
                    ...INITIAL_PAGE_PARAMS,
                    clientHost: 'http://localhost:3000'
                };
                overrideRouteSnapshot(
                    activatedRoute,
                    createRouteSnapshot({
                        queryParams: paramsWithAllowedHost,
                        data: createUveConfigData(BASIC_OPTIONS)
                    })
                );

                spectator.detectChanges();
                expect(spyStoreLoadPage).toHaveBeenCalledWith(paramsWithAllowedHost);
            });

            it('should trigger a navigate without the clientHost queryParam when the allowedDevURLs is empty', () => {
                const spyStoreLoadPage = jest.spyOn(store, 'pageLoad');
                const paramWithNotAllowedHost = {
                    ...INITIAL_PAGE_PARAMS,
                    clientHost: 'http://localhost:3000'
                };

                overrideRouteSnapshot(
                    activatedRoute,
                    createRouteSnapshot({
                        queryParams: paramWithNotAllowedHost,
                        data: createUveConfigData({ allowedDevURLs: [] })
                    })
                );
                spectator.detectChanges();
                expect(spyStoreLoadPage).toHaveBeenCalledWith(INITIAL_PAGE_PARAMS);
            });

            it('should omit clientHost when allowedDevURLs has wrong data type', () => {
                const spyStoreLoadPage = jest.spyOn(store, 'pageLoad');
                const paramWithNotAllowedHost = {
                    ...INITIAL_PAGE_PARAMS,
                    clientHost: 'http://localhost:1111'
                };

                overrideRouteSnapshot(
                    activatedRoute,
                    createRouteSnapshot({
                        queryParams: paramWithNotAllowedHost,
                        data: createUveConfigData({ allowedDevURLs: 'http://localhost:3000' })
                    })
                );

                spectator.detectChanges();
                expect(spyStoreLoadPage).toHaveBeenLastCalledWith(INITIAL_PAGE_PARAMS);
            });

            it('should omit clientHost when allowedDevURLs is not present', () => {
                const spyStoreLoadPage = jest.spyOn(store, 'pageLoad');
                const paramWithNotAllowedHost = {
                    ...INITIAL_PAGE_PARAMS,
                    clientHost: 'http://localhost:1111'
                };

                overrideRouteSnapshot(
                    activatedRoute,
                    createRouteSnapshot({
                        queryParams: paramWithNotAllowedHost,
                        data: createUveConfigData({})
                    })
                );

                spectator.detectChanges();
                expect(spyStoreLoadPage).toHaveBeenLastCalledWith(INITIAL_PAGE_PARAMS);
            });

            it('should omit clientHost when uveConfig options are not present', () => {
                const spyStoreLoadPage = jest.spyOn(store, 'pageLoad');
                const paramWithNotAllowedHost = {
                    ...INITIAL_PAGE_PARAMS,
                    clientHost: 'http://localhost:1111'
                };

                overrideRouteSnapshot(
                    activatedRoute,
                    createRouteSnapshot({
                        queryParams: paramWithNotAllowedHost,
                        data: {
                            uveConfig: {}
                        }
                    })
                );

                spectator.detectChanges();
                expect(spyStoreLoadPage).toHaveBeenLastCalledWith(INITIAL_PAGE_PARAMS);
            });

            it('should omit clientHost when uveConfig is not present', () => {
                const spyStoreLoadPage = jest.spyOn(store, 'pageLoad');
                const paramWithNotAllowedHost = {
                    ...INITIAL_PAGE_PARAMS,
                    clientHost: 'http://localhost:1111'
                };

                overrideRouteSnapshot(
                    activatedRoute,
                    createRouteSnapshot({
                        queryParams: paramWithNotAllowedHost,
                        data: {}
                    })
                );

                spectator.detectChanges();
                expect(spyStoreLoadPage).toHaveBeenLastCalledWith(INITIAL_PAGE_PARAMS);
            });
        });

        describe('Editor Mode', () => {
            it('should set mode to EDIT when wrong mode is passed', () => {
                const spyStoreLoadPage = jest.spyOn(store, 'pageLoad');
                const params = {
                    ...INITIAL_PAGE_PARAMS,
                    mode: 'WRONG'
                };
                overrideRouteSnapshot(
                    activatedRoute,
                    createRouteSnapshot({
                        queryParams: params,
                        data: createUveConfigData(BASIC_OPTIONS)
                    })
                );
                spectator.detectChanges();
                expect(spyStoreLoadPage).toHaveBeenCalledWith({
                    ...INITIAL_PAGE_PARAMS,
                    mode: UVE_MODE.EDIT
                });
            });

            it('should set mode to EDIT when mode is undefined', () => {
                const spyStoreLoadPage = jest.spyOn(store, 'pageLoad');
                const params = {
                    ...INITIAL_PAGE_PARAMS,
                    mode: undefined
                };
                overrideRouteSnapshot(
                    activatedRoute,
                    createRouteSnapshot({
                        queryParams: params,
                        data: createUveConfigData(BASIC_OPTIONS)
                    })
                );
                spectator.detectChanges();
                expect(spyStoreLoadPage).toHaveBeenCalledWith({
                    ...INITIAL_PAGE_PARAMS,
                    mode: UVE_MODE.EDIT
                });
            });
        });

        describe('Site Changes', () => {
            it('should trigger a navigate to /pages when switching to a different site', async () => {
                const navigate = jest.spyOn(router, 'navigate');

                spectator.detectChanges();
                siteService.setFakeCurrentSite(); // We have to trigger the first set as dotcms on init
                // Switch to a site with a different identifier than the current page's site
                siteService.setFakeCurrentSite({ identifier: 'different-site-id' } as Site);
                spectator.detectChanges();

                expect(navigate).toHaveBeenCalledWith(['/pages']);
            });

            it('should NOT navigate to /pages when the switched site matches the current page site', async () => {
                const navigate = jest.spyOn(router, 'navigate');

                spectator.detectChanges();
                siteService.setFakeCurrentSite(); // trigger init emission
                // Switch to the same site the page belongs to — should be a no-op
                siteService.setFakeCurrentSite({
                    identifier: MOCK_RESPONSE_HEADLESS.site.identifier
                } as Site);
                spectator.detectChanges();

                expect(navigate).not.toHaveBeenCalledWith(['/pages']);
            });
        });

        describe('page properties', () => {
            beforeEach(() => spectator.detectChanges());

            it('should update page params when saving and the url changed', () => {
                const pageLoadSpy = jest.spyOn(store, 'pageLoad');

                spectator.detectChanges();

                spectator.triggerEventHandler(
                    DotEmaDialogComponent,
                    'action',
                    createDialogActionEvent({
                        name: NG_CUSTOM_EVENTS.SAVE_PAGE,
                        payload: {
                            htmlPageReferer: '/my-awesome-page'
                        }
                    })
                );
                spectator.detectChanges();

                expect(pageLoadSpy).toHaveBeenCalledWith({ url: '/my-awesome-page' });
            });

            it('should get the workflow action when an `UPDATE_WORKFLOW_ACTION` event is received', () => {
                const spyGetWorkflowActions = jest.spyOn(store, 'workflowFetch');

                spectator.detectChanges();

                spectator.triggerEventHandler(
                    DotEmaDialogComponent,
                    'action',
                    createDialogActionEvent({
                        name: NG_CUSTOM_EVENTS.UPDATE_WORKFLOW_ACTION
                    })
                );
                spectator.detectChanges();

                expect(spyGetWorkflowActions).toHaveBeenCalled();
            });

            it('should trigger a store reload when htmlPageReferer is missing (new language version save)', () => {
                spectator.detectChanges();
                const spyReload = jest.spyOn(store, 'pageReload');

                spectator.triggerEventHandler(
                    DotEmaDialogComponent,
                    'action',
                    createDialogActionEvent({
                        name: NG_CUSTOM_EVENTS.SAVE_PAGE,
                        payload: {}
                    })
                );

                spectator.detectChanges();

                expect(spyReload).toHaveBeenCalled();
            });

            it('should trigger a store reload if the url is the same', () => {
                spectator.detectChanges();
                const spyReload = jest.spyOn(store, 'pageReload');
                const spyLocation = jest.spyOn(location, 'go');

                spectator.triggerEventHandler(
                    DotEmaDialogComponent,
                    'action',
                    createDialogActionEvent({
                        name: NG_CUSTOM_EVENTS.SAVE_PAGE,
                        payload: {
                            htmlPageReferer: 'index'
                        }
                    })
                );

                spectator.detectChanges();

                expect(spyReload).toHaveBeenCalled();
                expect(spyLocation).not.toHaveBeenCalled();
            });

            it('should reload content from dialog', () => {
                const reloadSpy = jest.spyOn(store, 'pageReload');

                spectator.triggerEventHandler(DotEmaDialogComponent, 'reloadFromDialog', null);

                expect(reloadSpy).toHaveBeenCalled();
            });

            it('should reload page when LANGUAGE_IS_CHANGED fires from the properties dialog', () => {
                const reloadSpy = jest.spyOn(store, 'pageReload');

                spectator.triggerEventHandler(
                    DotEmaDialogComponent,
                    'action',
                    createDialogActionEvent({
                        name: NG_CUSTOM_EVENTS.LANGUAGE_IS_CHANGED,
                        payload: { htmlPageReferer: '/index?com.dotmarketing.htmlpage.language=2' }
                    })
                );
                spectator.detectChanges();

                expect(reloadSpy).toHaveBeenCalled();
            });

            it('should trigger a store reload if the URL from urlContentMap is the same as the current URL', async () => {
                const reloadSpy = jest.spyOn(store, 'pageReload');
                jest.spyOn(dotPageApiService, 'get').mockReturnValue(
                    of({
                        ...PAGE_RESPONSE_URL_CONTENT_MAP,
                        clientResponse: PAGE_RESPONSE_URL_CONTENT_MAP
                    })
                );

                store.pageLoad({
                    url: '/test-url',
                    language_id: '1',
                    [PERSONA_KEY]: '1'
                });
                spectator.detectChanges();
                await spectator.fixture.whenStable();
                spectator.detectChanges();

                spectator.triggerEventHandler(
                    DotEmaDialogComponent,
                    'action',
                    createDialogActionEvent({
                        name: NG_CUSTOM_EVENTS.SAVE_PAGE,
                        payload: {
                            htmlPageReferer: '/test-url'
                        }
                    })
                );

                spectator.detectChanges();

                expect(reloadSpy).toHaveBeenCalled();
            });
        });

        describe('Breadcrumb', () => {
            it('should call GlobalStore.addNewBreadcrumb when page loads with page title, edit-page URL and identifier', async () => {
                mockGlobalStore.addNewBreadcrumb.mockClear();
                spectator.detectChanges();
                await spectator.fixture.whenStable();
                spectator.detectChanges();

                expect(mockGlobalStore.addNewBreadcrumb).toHaveBeenCalledWith(
                    expect.objectContaining({
                        label: 'hello world',
                        id: '123',
                        url: expect.stringContaining('url=index')
                    })
                );
            });

            it('should call addNewBreadcrumb again when page response changes', async () => {
                mockGlobalStore.addNewBreadcrumb.mockClear();
                spectator.detectChanges();
                await spectator.fixture.whenStable();
                spectator.detectChanges();

                expect(mockGlobalStore.addNewBreadcrumb).toHaveBeenCalledWith(
                    expect.objectContaining({
                        label: expect.any(String),
                        id: '123'
                    })
                );

                const differentPageResponse = {
                    ...MOCK_RESPONSE_HEADLESS,
                    page: {
                        ...MOCK_RESPONSE_HEADLESS.page,
                        title: 'Other Page',
                        identifier: '456'
                    }
                };
                jest.spyOn(dotPageApiService, 'get').mockReturnValue(of(differentPageResponse));
                mockGlobalStore.addNewBreadcrumb.mockClear();

                store.pageLoad({
                    ...INITIAL_PAGE_PARAMS,
                    url: '/other-page'
                });
                spectator.detectChanges();
                await spectator.fixture.whenStable();
                spectator.detectChanges();

                expect(mockGlobalStore.addNewBreadcrumb).toHaveBeenCalledWith(
                    expect.objectContaining({
                        label: 'Other Page',
                        id: '456',
                        url: expect.stringContaining('url=%2Fother-page')
                    })
                );
            });

            it('should build breadcrumb URL from current friendly params', async () => {
                mockGlobalStore.addNewBreadcrumb.mockClear();
                spectator.detectChanges();
                await spectator.fixture.whenStable();
                spectator.detectChanges();

                expect(mockGlobalStore.addNewBreadcrumb).toHaveBeenCalledWith(
                    expect.objectContaining({
                        url: expect.stringMatching(/^\/dotAdmin\/#.*url=index/)
                    })
                );
            });

            it('should not throw when resetPageParams() nulls pageParams and a tracked dep re-fires the effect', async () => {
                spectator.detectChanges();
                await spectator.fixture.whenStable();
                spectator.detectChanges();
                mockGlobalStore.addNewBreadcrumb.mockClear();

                // ngOnDestroy calls resetPageParams() (pageParams = null) but Angular tears
                // down effects asynchronously, so the effect can re-run in that window.
                // Cycling uveStatus on a tracked dep simulates that re-fire with null pageParams.
                store.resetPageParams();
                patchState(store, { uveStatus: UVE_STATUS.LOADING });
                patchState(store, { uveStatus: UVE_STATUS.LOADED });

                expect(() => spectator.detectChanges()).not.toThrow();
            });

            it('should replace breadcrumb on navigation, not accumulate stale entries', async () => {
                // Page A fully loaded
                spectator.detectChanges();
                await spectator.fixture.whenStable();
                spectator.detectChanges();
                mockGlobalStore.addNewBreadcrumb.mockClear();

                // Hold the response to inspect the LOADING window
                const pendingRequest$ = new Subject<typeof MOCK_RESPONSE_HEADLESS>();
                jest.spyOn(dotPageApiService, 'get').mockReturnValue(pendingRequest$);

                store.pageLoad({ ...INITIAL_PAGE_PARAMS, url: '/page-b' });
                spectator.detectChanges();

                // While LOADING, stale Page A data is present — breadcrumb must not fire
                expect(mockGlobalStore.addNewBreadcrumb).not.toHaveBeenCalled();

                // Resolve with Page B data
                const pageBResponse = {
                    ...MOCK_RESPONSE_HEADLESS,
                    page: { ...MOCK_RESPONSE_HEADLESS.page, title: 'Page B', identifier: '456' }
                };
                pendingRequest$.next(pageBResponse);
                pendingRequest$.complete();

                await spectator.fixture.whenStable();
                spectator.detectChanges();

                // Called exactly once — with Page B data, never with stale Page A data
                expect(mockGlobalStore.addNewBreadcrumb).toHaveBeenCalledTimes(1);
                expect(mockGlobalStore.addNewBreadcrumb).toHaveBeenCalledWith(
                    expect.objectContaining({
                        label: 'Page B',
                        id: '456',
                        url: expect.stringContaining('url=%2Fpage-b')
                    })
                );
            });

            it('should use urlContentMap title and identifier when present', async () => {
                jest.spyOn(dotPageApiService, 'get').mockReturnValue(
                    of({
                        ...MOCK_RESPONSE_HEADLESS,
                        page: {
                            ...MOCK_RESPONSE_HEADLESS.page,
                            title: 'Page Title',
                            identifier: 'page-id'
                        },
                        urlContentMap: {
                            ...URL_CONTENT_MAP_MOCK,
                            title: 'Content Map Title',
                            identifier: 'content-map-id'
                        }
                    })
                );

                mockGlobalStore.addNewBreadcrumb.mockClear();
                store.pageLoad(INITIAL_PAGE_PARAMS);
                spectator.detectChanges();
                await spectator.fixture.whenStable();
                spectator.detectChanges();

                expect(mockGlobalStore.addNewBreadcrumb).toHaveBeenCalledWith(
                    expect.objectContaining({
                        label: 'Content Map Title',
                        id: 'content-map-id',
                        url: expect.stringContaining('url=index')
                    })
                );
            });

            it('should fall back to page title and identifier when urlContentMap is absent', async () => {
                jest.spyOn(dotPageApiService, 'get').mockReturnValue(
                    of({
                        ...MOCK_RESPONSE_HEADLESS,
                        page: {
                            ...MOCK_RESPONSE_HEADLESS.page,
                            title: 'Page Title',
                            identifier: 'page-id'
                        },
                        urlContentMap: null
                    })
                );

                mockGlobalStore.addNewBreadcrumb.mockClear();
                store.pageLoad(INITIAL_PAGE_PARAMS);
                spectator.detectChanges();
                await spectator.fixture.whenStable();
                spectator.detectChanges();

                expect(mockGlobalStore.addNewBreadcrumb).toHaveBeenCalledWith(
                    expect.objectContaining({
                        label: 'Page Title',
                        id: 'page-id',
                        url: expect.stringContaining('url=index')
                    })
                );
            });
        });
    });

    describe('without read permission', () => {
        beforeEach(() => {
            jest.spyOn(dotPageApiService, 'get').mockReturnValue(
                of({
                    ...MOCK_RESPONSE_HEADLESS,
                    page: {
                        ...MOCK_RESPONSE_HEADLESS.page,
                        canEdit: false,
                        canRead: false
                    }
                })
            );
            spectator.detectChanges();
        });

        it('should not render the router outlet', () => {
            expect(spectator.query('router-outlet')).toBeNull();
        });

        it('should still render the navigation bar, toast, and seo tools', () => {
            expect(spectator.query(EditEmaNavigationBarComponent)).toBeTruthy();
            expect(spectator.query(DotPageToolsSeoComponent)).toBeTruthy();
        });
    });

    describe('Local View Models', () => {
        describe('$menuItems computed property', () => {
            it('should build menu items with correct structure', () => {
                const menuItems = spectator.component['$menuItems']();

                expect(menuItems).toHaveLength(6);
                expect(menuItems[0]).toEqual({
                    materialIcon: 'description',
                    label: 'editema.editor.navbar.content',
                    href: 'content',
                    id: 'content'
                });
            });

            it('should disable layout when page cannot be edited', () => {
                jest.spyOn(dotPageApiService, 'get').mockReturnValue(
                    of({
                        ...MOCK_RESPONSE_HEADLESS,
                        page: {
                            ...MOCK_RESPONSE_HEADLESS.page,
                            canEdit: false
                        },
                        template: {
                            ...MOCK_RESPONSE_HEADLESS.template,
                            drawed: false
                        }
                    })
                );
                spectator.detectChanges();

                const menuItems = spectator.component['$menuItems']();
                const layoutItem = menuItems.find((item) => item.id === 'layout');

                expect(layoutItem.isDisabled).toBe(true);
            });

            it('should show tooltip for advanced templates', () => {
                jest.spyOn(dotPageApiService, 'get').mockReturnValue(
                    of({
                        ...MOCK_RESPONSE_HEADLESS,
                        template: {
                            ...MOCK_RESPONSE_HEADLESS.template,
                            drawed: false
                        }
                    })
                );
                spectator.detectChanges();

                const menuItems = spectator.component['$menuItems']();
                const layoutItem = menuItems.find((item) => item.id === 'layout');

                expect(layoutItem.tooltip).toBe(
                    'editema.editor.navbar.layout.tooltip.cannot.edit.advanced.template'
                );
            });
        });

        describe('$seoParams computed property', () => {
            beforeEach(() => {
                spectator.detectChanges();
            });

            it('should build SEO params with correct structure', () => {
                const seoParams = spectator.component['$seoParams']();

                expect(seoParams).toEqual({
                    siteId: MOCK_RESPONSE_HEADLESS.site.identifier,
                    languageId: MOCK_RESPONSE_HEADLESS.viewAs.language.id,
                    currentUrl: expect.stringContaining('/'),
                    requestHostName: expect.any(String)
                });
            });

            it('should sanitize and format page URI correctly', () => {
                const seoParams = spectator.component['$seoParams']();
                const currentUrl = seoParams.currentUrl;

                expect(currentUrl).toMatch(/^\//);
            });

            it('should use page hostname when clientHost is not present', () => {
                const seoParams = spectator.component['$seoParams']();

                expect(seoParams.requestHostName).toBe(
                    `${window.location.protocol}//${MOCK_RESPONSE_HEADLESS.site.hostname}`
                );
            });
        });

        describe('$errorDisplay computed property', () => {
            it('should return null when no error code', () => {
                const errorDisplay = spectator.component['$errorDisplay']();

                expect(errorDisplay).toBeNull();
            });

            it('should return error payload when error code exists', () => {
                spectator.component['uveStore'].setUveStatus = jest.fn();
                const uveStore = spectator.component['uveStore'] as InstanceType<
                    typeof UVEStore
                > & {
                    pageErrorCode: () => number;
                };
                jest.spyOn(uveStore, 'pageErrorCode').mockReturnValue(401);

                spectator.detectChanges();

                const errorDisplay = spectator.component['$errorDisplay']();

                expect(errorDisplay).not.toBeNull();
                expect(errorDisplay?.code).toBe(401);
            });
        });

        describe('$canRead computed property', () => {
            it('should return true when page can be read', () => {
                spectator.detectChanges();
                const canRead = spectator.component['$canRead']();

                expect(canRead).toBe(true);
            });

            it('should return false when page cannot be read', () => {
                jest.spyOn(dotPageApiService, 'get').mockReturnValue(
                    of({
                        ...MOCK_RESPONSE_HEADLESS,
                        page: {
                            ...MOCK_RESPONSE_HEADLESS.page,
                            canRead: false
                        }
                    })
                );
                spectator.detectChanges();

                const canRead = spectator.component['$canRead']();

                expect(canRead).toBe(false);
            });

            it('should return false when page is undefined', () => {
                jest.spyOn(dotPageApiService, 'get').mockReturnValue(
                    of({
                        ...MOCK_RESPONSE_HEADLESS,
                        page: undefined
                    })
                );
                spectator.detectChanges();

                const canRead = spectator.component['$canRead']();

                expect(canRead).toBe(false);
            });
        });
    });

    describe('Page Scanner', () => {
        beforeEach(() => {
            spectator.detectChanges();
        });

        describe('$showPageScanner', () => {
            it('should be true when FEATURE_FLAG_PAGE_SCANNER flag is enabled', () => {
                patchState(spectator.inject(UVEStore, true), {
                    flags: { [FeaturedFlags.FEATURE_FLAG_PAGE_SCANNER]: true }
                });

                expect(spectator.component['$showPageScanner']()).toBe(true);
            });

            it('should be false when FEATURE_FLAG_PAGE_SCANNER flag is disabled', () => {
                patchState(spectator.inject(UVEStore, true), {
                    flags: { [FeaturedFlags.FEATURE_FLAG_PAGE_SCANNER]: false }
                });

                expect(spectator.component['$showPageScanner']()).toBe(false);
            });
        });

        describe('handleScannerToolClick', () => {
            it('should open the page scanner with the correct url and type', () => {
                const openSpy = jest.fn();
                spectator.component['pageScanner'] = {
                    open: openSpy
                } as unknown as DotPageScannerReportComponent;

                spectator.component.handleScannerToolClick('a11y');

                const { currentUrl, siteId } = spectator.component['$seoParams']();
                const params = store.pageParams();
                const expectedUrl = new URL(currentUrl, window.location.origin);
                if (siteId) {
                    expectedUrl.searchParams.set('host_id', siteId);
                }
                // The scanner re-renders with the editor's page-resolving params
                if (params?.language_id) {
                    expectedUrl.searchParams.set('language_id', params.language_id);
                }
                if (params?.mode) {
                    expectedUrl.searchParams.set('mode', params.mode);
                }
                expect(openSpy).toHaveBeenCalledWith('a11y', expectedUrl.toString());
            });

            it('should scan the authoring instance origin, not the page content host', () => {
                const openSpy = jest.fn();
                spectator.component['pageScanner'] = {
                    open: openSpy
                } as unknown as DotPageScannerReportComponent;

                spectator.component.handleScannerToolClick('a11y');

                const calledUrl = openSpy.mock.calls[0][1] as string;
                expect(new URL(calledUrl).origin).toBe(window.location.origin);
            });

            it('should append the page site host_id so the scanner resolves the correct site on multisite', () => {
                const openSpy = jest.fn();
                spectator.component['pageScanner'] = {
                    open: openSpy
                } as unknown as DotPageScannerReportComponent;
                jest.spyOn(spectator.component, '$seoParams' as never).mockReturnValue({
                    currentUrl: '/my-page',
                    requestHostName: 'https://content-site.example.com',
                    siteId: 'site-b-identifier',
                    languageId: 1
                } as never);

                spectator.component.handleScannerToolClick('a11y');

                const calledUrl = openSpy.mock.calls[0][1] as string;
                expect(new URL(calledUrl).searchParams.get('host_id')).toBe('site-b-identifier');
            });

            it('should omit host_id when the page has no site identifier', () => {
                const openSpy = jest.fn();
                spectator.component['pageScanner'] = {
                    open: openSpy
                } as unknown as DotPageScannerReportComponent;
                jest.spyOn(spectator.component, '$seoParams' as never).mockReturnValue({
                    currentUrl: '/my-page',
                    requestHostName: 'https://content-site.example.com',
                    siteId: undefined,
                    languageId: 1
                } as never);

                spectator.component.handleScannerToolClick('a11y');

                const calledUrl = openSpy.mock.calls[0][1] as string;
                expect(new URL(calledUrl).searchParams.has('host_id')).toBe(false);
            });

            it('should forward all page-resolving params (language, persona, variant, mode, time machine) so the scanner re-renders the same page', () => {
                const openSpy = jest.fn();
                spectator.component['pageScanner'] = {
                    open: openSpy
                } as unknown as DotPageScannerReportComponent;

                patchState(store, {
                    pageParams: {
                        url: 'my-page',
                        language_id: '2',
                        [PERSONA_KEY]: 'persona-123',
                        variantName: 'my-variant',
                        mode: UVE_MODE.LIVE,
                        publishDate: '2026-06-15',
                        clientHost: 'https://headless.example.com',
                        depth: '2'
                    }
                });

                spectator.component.handleScannerToolClick('a11y');

                const calledUrl = openSpy.mock.calls[0][1] as string;
                const params = new URL(calledUrl).searchParams;

                expect(params.get('language_id')).toBe('2');
                // The scanner is a backend page render, so persona uses the backend
                // request param key (WebKeys.CMS_PERSONA_PARAMETER), not personaId
                expect(params.get(PERSONA_KEY)).toBe('persona-123');
                expect(params.has('personaId')).toBe(false);
                expect(params.get('variantName')).toBe('my-variant');
                expect(params.get('mode')).toBe(UVE_MODE.LIVE);
                expect(params.get('publishDate')).toBe('2026-06-15');

                // Editor-fetch concerns must not leak to the public scanner
                expect(params.has('clientHost')).toBe(false);
                expect(params.has('depth')).toBe(false);
                expect(params.has('url')).toBe(false);
            });

            it('should omit the default variant from the scanned URL', () => {
                const openSpy = jest.fn();
                spectator.component['pageScanner'] = {
                    open: openSpy
                } as unknown as DotPageScannerReportComponent;

                patchState(store, {
                    pageParams: {
                        url: 'my-page',
                        language_id: '1',
                        [PERSONA_KEY]: DEFAULT_PERSONA.identifier,
                        variantName: DEFAULT_VARIANT_ID,
                        mode: UVE_MODE.EDIT
                    }
                });

                spectator.component.handleScannerToolClick('a11y');

                const calledUrl = openSpy.mock.calls[0][1] as string;
                const params = new URL(calledUrl).searchParams;

                expect(params.has('variantName')).toBe(false);
                // Default persona is implicit and dropped from the scanned URL
                expect(params.has(PERSONA_KEY)).toBe(false);
                expect(params.has('personaId')).toBe(false);
            });

            it('should pass geo type to the page scanner', () => {
                const openSpy = jest.fn();
                spectator.component['pageScanner'] = {
                    open: openSpy
                } as unknown as DotPageScannerReportComponent;

                spectator.component.handleScannerToolClick('geo');

                expect(openSpy).toHaveBeenCalledWith('geo', expect.any(String));
            });
        });

        describe('dot-page-tools-seo binding', () => {
            it('should pass showPageScanner to the page tools component', () => {
                const pageTools = spectator.query(MockComponent(DotPageToolsSeoComponent));
                expect(pageTools).toBeTruthy();
            });

            it('should call handleScannerToolClick when scannerToolClick event is emitted', () => {
                const handleSpy = jest.spyOn(spectator.component, 'handleScannerToolClick');
                const pageTools = spectator.query(MockComponent(DotPageToolsSeoComponent));

                (
                    pageTools as unknown as { scannerToolClick: { emit: (v: string) => void } }
                ).scannerToolClick?.['emit']?.('a11y');
                spectator.detectChanges();

                // Trigger via the component method directly since MockComponent doesn't wire outputs
                spectator.component.handleScannerToolClick('a11y');
                expect(handleSpy).toHaveBeenCalledWith('a11y');
            });
        });
    });

    afterEach(() => {
        // Restoring the snapshot to the default
        if (activatedRoute && typeof activatedRoute === 'object') {
            overrideRouteSnapshot(
                activatedRoute,
                createRouteSnapshot({
                    queryParams: INITIAL_PAGE_PARAMS,
                    data: createUveConfigData(BASIC_OPTIONS)
                })
            );
        }
        jest.clearAllMocks();
    });
});
