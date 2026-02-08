import { describe, expect } from '@jest/globals';
import {
    SpyObject,
    createComponentFactory,
    Spectator,
    byTestId,
    mockProvider
} from '@ngneat/spectator/jest';
import { MockComponent } from 'ng-mocks';
import { of } from 'rxjs';

import { Location } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { By } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';

import { ConfirmationService, MessageService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogService } from 'primeng/dynamicdialog';
import { ToastModule } from 'primeng/toast';

import {
    DotAnalyticsTrackerService,
    DotContentletLockerService,
    DotCurrentUserService,
    DotExperimentsService,
    DotLanguagesService,
    DotLicenseService,
    DotMessageService,
    DotPropertiesService,
    DotSiteService,
    DotSystemConfigService,
    DotWorkflowActionsFireService,
    DotWorkflowsActionsService,
    PushPublishService
} from '@dotcms/data-access';
import {
    DotcmsConfigService,
    DotcmsEventsService,
    LoginService,
    SiteService
} from '@dotcms/dotcms-js';
import { DotPageToolsSeoComponent } from '@dotcms/portlets/dot-ema/ui';
import { DotCMSUVEAction, UVE_MODE } from '@dotcms/types';
import { DotNotLicenseComponent } from '@dotcms/ui';
import { WINDOW } from '@dotcms/utils';
import {
    DotExperimentsServiceMock,
    DotCurrentUserServiceMock,
    DotLanguagesServiceMock,
    DotcmsConfigServiceMock,
    DotcmsEventsServiceMock,
    SiteServiceMock
} from '@dotcms/utils-testing';

import { EditEmaNavigationBarComponent } from './components/edit-ema-navigation-bar/edit-ema-navigation-bar.component';
import { DotEmaShellComponent } from './dot-ema-shell.component';

import { DotEmaDialogComponent } from '../components/dot-ema-dialog/dot-ema-dialog.component';
import { DotActionUrlService } from '../services/dot-action-url/dot-action-url.service';
import { DotPageApiService } from '../services/dot-page-api.service';
import { DEFAULT_PERSONA, PERSONA_KEY } from '../shared/consts';
import { FormStatus, NG_CUSTOM_EVENTS } from '../shared/enums';
import {
    dotPropertiesServiceMock,
    MOCK_RESPONSE_HEADLESS,
    PAGE_RESPONSE_BY_LANGUAGE_ID,
    PAGE_RESPONSE_URL_CONTENT_MAP,
    PAYLOAD_MOCK
} from '../shared/mocks';
import { UVEStore } from '../store/dot-uve.store';

const DIALOG_ACTION_EVENT = (detail) => {
    return {
        event: new CustomEvent('ng-event', { detail }),
        actionPayload: PAYLOAD_MOCK,
        form: {
            status: FormStatus.SAVED,
            isTranslation: false
        },
        clientAction: DotCMSUVEAction.NOOP
    };
};

const NAV_ITEMS = [
    {
        icon: 'pi-file',
        label: 'editema.editor.navbar.content',
        href: 'content',
        id: 'content'
    },
    {
        icon: 'pi-table',
        label: 'editema.editor.navbar.layout',
        href: 'layout',
        isDisabled: false,
        tooltip: null,
        id: 'layout'
    },
    {
        icon: 'pi-sliders-h',
        label: 'editema.editor.navbar.rules',
        href: `rules/123`,
        isDisabled: false,
        id: 'rules'
    },
    {
        iconURL: 'experiments',
        label: 'editema.editor.navbar.experiments',
        href: 'experiments/123',
        isDisabled: false,
        id: 'experiments'
    },
    {
        icon: 'pi-th-large',
        label: 'editema.editor.navbar.page-tools',
        id: 'page-tools'
    },
    {
        icon: 'pi-ellipsis-v',
        label: 'editema.editor.navbar.properties',
        id: 'properties',
        isDisabled: false
    }
];

const INITIAL_PAGE_PARAMS = {
    language_id: 1,
    url: 'index',
    variantName: 'DEFAULT',
    [PERSONA_KEY]: 'modes.persona.no.persona',
    mode: UVE_MODE.EDIT
};

const BASIC_OPTIONS = {
    allowedDevURLs: ['http://localhost:3000']
};

const UVE_CONFIG_MOCK = (options) => {
    return {
        uveConfig: {
            options
        }
    };
};

const SNAPSHOT_MOCK = (
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    { queryParams, data }: any
) => {
    return {
        queryParams,
        data
    };
};

/**
 * Override the snapshot of the activated route
 * To simulate the queryParams change
 *
 * Note: Be sure run this before your first `spectator.detectChanges()`
 *
 * @param {*} activatedRoute
 * @param {*} mock
 */
const overrideRouteSnashot = (activatedRoute, mock) => {
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
    let dotLicenseService: DotLicenseService;
    let dotPageApiService: DotPageApiService;

    const createComponent = createComponentFactory({
        component: DotEmaShellComponent,
        imports: [ConfirmDialogModule],
        detectChanges: false,
        providers: [
            {
                provide: ActivatedRoute,
                useValue: {
                    snapshot: SNAPSHOT_MOCK({
                        queryParams: INITIAL_PAGE_PARAMS,
                        data: UVE_CONFIG_MOCK(BASIC_OPTIONS)
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
                events: of()
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
            MockComponent(DotPageToolsSeoComponent)
        ],
        componentProviders: [
            MessageService,
            UVEStore,
            ConfirmationService,
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
                provide: DotcmsEventsService,
                useValue: new DotcmsEventsServiceMock()
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
                provide: WINDOW,
                useValue: window
            },
            {
                provide: DotMessageService,
                useValue: {
                    get: jest.fn().mockReturnValue('Mock Message'),
                    init: jest.fn()
                }
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            providers: [
                {
                    provide: DotLicenseService,
                    useValue: {
                        isEnterprise: () => of(true),
                        canAccessEnterprisePortlet: () => of(true)
                    }
                }
            ]
        });
        siteService = spectator.inject(SiteService) as unknown as SiteServiceMock;
        store = spectator.inject(UVEStore, true);
        router = spectator.inject(Router, true);
        location = spectator.inject(Location, true);
        activatedRoute = spectator.inject(ActivatedRoute, true);
        dotPageApiService = spectator.inject(DotPageApiService, true);
        dotLicenseService = spectator.inject(DotLicenseService, true);
    });

    describe('with queryParams', () => {
        it('should trigger an store load with default values', () => {
            const spyStoreLoadPage = jest.spyOn(store, 'loadPageAsset');
            spectator.detectChanges();
            expect(spyStoreLoadPage).toHaveBeenCalledWith(INITIAL_PAGE_PARAMS);
        });

        describe('Sanitize url when called loadPageAsset', () => {
            it('should sanitize when url is index', () => {
                const spyloadPageAsset = jest.spyOn(store, 'loadPageAsset');
                const spyLocation = jest.spyOn(location, 'go');

                const params = {
                    ...INITIAL_PAGE_PARAMS,
                    url: '/index'
                };

                overrideRouteSnashot(
                    activatedRoute,
                    SNAPSHOT_MOCK({ queryParams: params, data: UVE_CONFIG_MOCK(BASIC_OPTIONS) })
                );

                spectator.detectChanges();
                expect(spyloadPageAsset).toHaveBeenCalledWith({ ...params, url: '/index' });
                expect(spyLocation).toHaveBeenCalledWith(
                    '/?language_id=1&url=%2Findex&variantName=DEFAULT&mode=EDIT_MODE'
                );
            });

            it('should sanitize when url is nested', () => {
                const spyloadPageAsset = jest.spyOn(store, 'loadPageAsset');

                const spyLocation = jest.spyOn(location, 'go');

                const params = {
                    ...INITIAL_PAGE_PARAMS,
                    url: '/some-url/some-nested-url'
                };

                overrideRouteSnashot(
                    activatedRoute,
                    SNAPSHOT_MOCK({ queryParams: params, data: UVE_CONFIG_MOCK(BASIC_OPTIONS) })
                );

                spectator.detectChanges();
                expect(spyloadPageAsset).toHaveBeenCalledWith({
                    ...params,
                    url: '/some-url/some-nested-url'
                });
                expect(spyLocation).toHaveBeenCalledWith(
                    '/?language_id=1&url=%2Fsome-url%2Fsome-nested-url&variantName=DEFAULT&mode=EDIT_MODE'
                );
            });

            it('should sanitize when url is nested and ends in index', () => {
                const spyloadPageAsset = jest.spyOn(store, 'loadPageAsset');
                const spyLocation = jest.spyOn(location, 'go');

                const params = {
                    ...INITIAL_PAGE_PARAMS,
                    url: '/some-url/index'
                };

                overrideRouteSnashot(
                    activatedRoute,
                    SNAPSHOT_MOCK({ queryParams: params, data: UVE_CONFIG_MOCK(BASIC_OPTIONS) })
                );

                spectator.detectChanges();
                expect(spyloadPageAsset).toHaveBeenCalledWith({
                    ...params,
                    url: '/some-url/index'
                });
                expect(spyLocation).toHaveBeenCalledWith(
                    '/?language_id=1&url=%2Fsome-url%2Findex&variantName=DEFAULT&mode=EDIT_MODE'
                );
            });

            it('should receive `personaId` query param', () => {
                const spyloadPageAsset = jest.spyOn(store, 'loadPageAsset');
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

                overrideRouteSnashot(
                    activatedRoute,
                    SNAPSHOT_MOCK({ queryParams, data: UVE_CONFIG_MOCK(BASIC_OPTIONS) })
                );

                spectator.detectChanges();
                expect(spyloadPageAsset).toHaveBeenCalledWith(expectedParams);
                expect(spyLocation).toHaveBeenCalledWith(
                    '/?url=%2Fsome-url%2Findex&language_id=1&mode=EDIT_MODE&personaId=someCoolDude'
                );
            });
        });

        it('should patch viewParams with empty object when the mode is edit', () => {
            const patchViewParamsSpy = jest.spyOn(store, 'patchViewParams');
            const params = {
                ...INITIAL_PAGE_PARAMS,
                mode: UVE_MODE.EDIT
            };

            overrideRouteSnashot(
                activatedRoute,
                SNAPSHOT_MOCK({ queryParams: params, data: UVE_CONFIG_MOCK(BASIC_OPTIONS) })
            );

            spectator.detectChanges();

            expect(patchViewParamsSpy).toHaveBeenCalledWith({});
        });

        it('should patch viewParams with empty params on init', () => {
            const patchViewParamsSpy = jest.spyOn(store, 'patchViewParams');

            const params = {
                ...INITIAL_PAGE_PARAMS,
                mode: UVE_MODE.PREVIEW
            };

            overrideRouteSnashot(
                activatedRoute,
                SNAPSHOT_MOCK({ queryParams: params, data: UVE_CONFIG_MOCK(BASIC_OPTIONS) })
            );

            spectator.detectChanges();

            expect(patchViewParamsSpy).toHaveBeenCalledWith({
                orientation: undefined,
                seo: undefined,
                device: undefined
            });
        });

        it('should patch viewParams with the correct params on init', () => {
            const patchViewParamsSpy = jest.spyOn(store, 'patchViewParams');

            const withViewParams = {
                device: 'mobile',
                orientation: 'landscape',
                seo: undefined,
                mode: UVE_MODE.PREVIEW
            };

            overrideRouteSnashot(
                activatedRoute,
                SNAPSHOT_MOCK({ queryParams: withViewParams, data: UVE_CONFIG_MOCK(BASIC_OPTIONS) })
            );

            spectator.detectChanges();

            expect(patchViewParamsSpy).toHaveBeenCalledWith({
                orientation: 'landscape',
                seo: undefined,
                device: 'mobile'
            });
        });

        it('should patch viewParams with the correct params on init with live mode', () => {
            const patchViewParamsSpy = jest.spyOn(store, 'patchViewParams');

            const withViewParams = {
                device: 'mobile',
                orientation: 'landscape',
                seo: undefined,
                mode: UVE_MODE.LIVE
            };

            overrideRouteSnashot(
                activatedRoute,
                SNAPSHOT_MOCK({ queryParams: withViewParams, data: UVE_CONFIG_MOCK(BASIC_OPTIONS) })
            );

            spectator.detectChanges();

            expect(patchViewParamsSpy).toHaveBeenCalledWith({
                orientation: 'landscape',
                seo: undefined,
                device: 'mobile'
            });
        });

        it('should call store.loadPageAsset when the `loadPageAsset` is called', () => {
            const spyloadPageAsset = jest.spyOn(store, 'loadPageAsset');
            const spyStoreLoadPage = jest.spyOn(store, 'loadPageAsset');
            const spyLocation = jest.spyOn(location, 'go');

            spectator.detectChanges();
            expect(spyloadPageAsset).toHaveBeenCalledWith(INITIAL_PAGE_PARAMS);
            expect(spyStoreLoadPage).toHaveBeenCalledWith(INITIAL_PAGE_PARAMS);
            expect(spyLocation).toHaveBeenCalledWith(
                '/?language_id=1&url=index&variantName=DEFAULT&mode=EDIT_MODE'
            );
        });

        describe('DOM', () => {
            beforeEach(async () => {
                spectator.detectChanges();
                // Wait until the effect triggers the init and intialize the DOM
                await spectator.fixture.whenStable();
                spectator.detectChanges();
            });

            it('should have a navigation bar', () => {
                expect(spectator.query(byTestId('ema-nav-bar'))).not.toBeNull();
            });

            it('should have nav bar with items', () => {
                const navBarComponent = spectator.query(EditEmaNavigationBarComponent);

                expect(navBarComponent.items).toEqual(NAV_ITEMS);
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

            it('should update parms when loadPage is triggered', () => {
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
                const spyStoreLoadPage = jest.spyOn(store, 'loadPageAsset');
                const spyUrlTree = jest.spyOn(router, 'createUrlTree');
                const spyLocation = jest.spyOn(location, 'go');

                store.loadPageAsset(pageParams);
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
                overrideRouteSnashot(
                    activatedRoute,
                    SNAPSHOT_MOCK({
                        queryParams: params,
                        data: {
                            uveConfig: {
                                url: baseClientHost,
                                options: BASIC_OPTIONS
                            }
                        }
                    })
                );

                store.loadPageAsset(params);
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
                overrideRouteSnashot(
                    activatedRoute,
                    SNAPSHOT_MOCK({
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

                store.loadPageAsset(params);
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
                overrideRouteSnashot(
                    activatedRoute,
                    SNAPSHOT_MOCK({
                        queryParams: params,
                        data: {
                            uveConfig: {
                                url: baseClientHost,
                                options: BASIC_OPTIONS
                            }
                        }
                    })
                );

                store.loadPageAsset(params);
                spectator.detectChanges();

                // Should treat these as the same URL and not include clientHost
                expect(spyLocation).toHaveBeenCalledWith(
                    '/?language_id=1&url=index&variantName=DEFAULT&mode=EDIT_MODE'
                );
            });
        });

        describe('ClientHost', () => {
            it('should trigger init the store without the clientHost queryParam when it is not allowed', () => {
                const spyStoreLoadPage = jest.spyOn(store, 'loadPageAsset');
                const paramWithNotAllowedHost = {
                    ...INITIAL_PAGE_PARAMS,
                    clientHost: 'http://localhost:4200'
                };

                const data = UVE_CONFIG_MOCK({
                    allowedDevURLs: ['http://localhost:3000']
                });

                overrideRouteSnashot(
                    activatedRoute,
                    SNAPSHOT_MOCK({ queryParams: paramWithNotAllowedHost, data })
                );

                spectator.detectChanges();
                expect(spyStoreLoadPage).toHaveBeenCalledWith(INITIAL_PAGE_PARAMS);
                expect(spyStoreLoadPage).not.toHaveBeenCalledWith(paramWithNotAllowedHost);
            });

            it('should trigger a load when changing the clientHost and it is on the allowedDevURLs', () => {
                const spyStoreLoadPage = jest.spyOn(store, 'loadPageAsset');
                const paramsWithAllowedHost = {
                    ...INITIAL_PAGE_PARAMS,
                    clientHost: 'http://localhost:3000'
                };
                overrideRouteSnashot(
                    activatedRoute,
                    SNAPSHOT_MOCK({
                        queryParams: paramsWithAllowedHost,
                        data: UVE_CONFIG_MOCK(BASIC_OPTIONS)
                    })
                );

                spectator.detectChanges();
                expect(spyStoreLoadPage).toHaveBeenCalledWith(paramsWithAllowedHost);
            });

            it('should trigger a navigate without the clientHost queryParam when the allowedDevURLs is empty', () => {
                const spyStoreLoadPage = jest.spyOn(store, 'loadPageAsset');
                const paramWithNotAllowedHost = {
                    ...INITIAL_PAGE_PARAMS,
                    clientHost: 'http://localhost:3000'
                };

                overrideRouteSnashot(
                    activatedRoute,
                    SNAPSHOT_MOCK({
                        queryParams: paramWithNotAllowedHost,
                        data: UVE_CONFIG_MOCK({ allowedDevURLs: [] })
                    })
                );
                spectator.detectChanges();
                expect(spyStoreLoadPage).toHaveBeenCalledWith(INITIAL_PAGE_PARAMS);
            });

            it('should trigger a navigate without the clientHost queryParam when the allowedDevURLs is has a wrong data type', () => {
                const spyStoreLoadPage = jest.spyOn(store, 'loadPageAsset');
                const paramWithNotAllowedHost = {
                    ...INITIAL_PAGE_PARAMS,
                    clientHost: 'http://localhost:1111'
                };

                overrideRouteSnashot(
                    activatedRoute,
                    SNAPSHOT_MOCK({
                        queryParams: paramWithNotAllowedHost,
                        data: UVE_CONFIG_MOCK({ allowedDevURLs: 'http://localhost:3000' })
                    })
                );

                spectator.detectChanges();
                expect(spyStoreLoadPage).toHaveBeenLastCalledWith(INITIAL_PAGE_PARAMS);
            });

            it('should trigger a navigate without the clientHost queryParam when the allowedDevURLs is is not present', () => {
                const spyStoreLoadPage = jest.spyOn(store, 'loadPageAsset');
                const paramWithNotAllowedHost = {
                    ...INITIAL_PAGE_PARAMS,
                    clientHost: 'http://localhost:1111'
                };

                overrideRouteSnashot(
                    activatedRoute,
                    SNAPSHOT_MOCK({
                        queryParams: paramWithNotAllowedHost,
                        data: UVE_CONFIG_MOCK({})
                    })
                );

                spectator.detectChanges();
                expect(spyStoreLoadPage).toHaveBeenLastCalledWith(INITIAL_PAGE_PARAMS);
            });

            it('should trigger a navigate without the clientHost queryParam when the options are not present', () => {
                const spyStoreLoadPage = jest.spyOn(store, 'loadPageAsset');
                const paramWithNotAllowedHost = {
                    ...INITIAL_PAGE_PARAMS,
                    clientHost: 'http://localhost:1111'
                };

                overrideRouteSnashot(
                    activatedRoute,
                    SNAPSHOT_MOCK({
                        queryParams: paramWithNotAllowedHost,
                        data: {
                            uveConfig: {}
                        }
                    })
                );

                spectator.detectChanges();
                expect(spyStoreLoadPage).toHaveBeenLastCalledWith(INITIAL_PAGE_PARAMS);
            });

            it('should trigger a navigate without the clientHost queryParam when the uveConfig is not present', () => {
                const spyStoreLoadPage = jest.spyOn(store, 'loadPageAsset');
                const paramWithNotAllowedHost = {
                    ...INITIAL_PAGE_PARAMS,
                    clientHost: 'http://localhost:1111'
                };

                overrideRouteSnashot(
                    activatedRoute,
                    SNAPSHOT_MOCK({
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
                const spyStoreLoadPage = jest.spyOn(store, 'loadPageAsset');
                const params = {
                    ...INITIAL_PAGE_PARAMS,
                    mode: 'WRONG'
                };
                overrideRouteSnashot(
                    activatedRoute,
                    SNAPSHOT_MOCK({ queryParams: params, data: UVE_CONFIG_MOCK(BASIC_OPTIONS) })
                );
                spectator.detectChanges();
                expect(spyStoreLoadPage).toHaveBeenCalledWith({
                    ...INITIAL_PAGE_PARAMS,
                    mode: UVE_MODE.EDIT
                });
            });

            it('should set mode to EDIT when wrong mode is not passed', () => {
                const spyStoreLoadPage = jest.spyOn(store, 'loadPageAsset');
                const params = {
                    ...INITIAL_PAGE_PARAMS,
                    mode: undefined
                };
                overrideRouteSnashot(
                    activatedRoute,
                    SNAPSHOT_MOCK({ queryParams: params, data: UVE_CONFIG_MOCK(BASIC_OPTIONS) })
                );
                spectator.detectChanges();
                expect(spyStoreLoadPage).toHaveBeenCalledWith({
                    ...INITIAL_PAGE_PARAMS,
                    mode: UVE_MODE.EDIT
                });
            });
        });

        describe('Site Changes', () => {
            it('should trigger a navigate to /pages when site changes', async () => {
                const navigate = jest.spyOn(router, 'navigate');

                spectator.detectChanges();
                siteService.setFakeCurrentSite(); // We have to trigger the first set as dotcms on init
                siteService.setFakeCurrentSite();
                spectator.detectChanges();

                expect(navigate).toHaveBeenCalledWith(['/pages']);
            });
        });

        describe('page properties', () => {
            beforeEach(() => spectator.detectChanges());

            it('should update page params when saving and the url changed', () => {
                const spyloadPageAsset = jest.spyOn(store, 'loadPageAsset');

                spectator.detectChanges();

                spectator.triggerEventHandler(
                    DotEmaDialogComponent,
                    'action',
                    DIALOG_ACTION_EVENT({
                        name: NG_CUSTOM_EVENTS.SAVE_PAGE,
                        payload: {
                            htmlPageReferer: '/my-awesome-page'
                        }
                    })
                );
                spectator.detectChanges();

                expect(spyloadPageAsset).toHaveBeenCalledWith({ url: '/my-awesome-page' });
            });

            it('should get the workflow action when an `UPDATE_WORKFLOW_ACTION` event is received', () => {
                const spyGetWorkflowActions = jest.spyOn(store, 'getWorkflowActions');

                spectator.detectChanges();

                spectator.triggerEventHandler(
                    DotEmaDialogComponent,
                    'action',
                    DIALOG_ACTION_EVENT({
                        name: NG_CUSTOM_EVENTS.UPDATE_WORKFLOW_ACTION
                    })
                );
                spectator.detectChanges();

                expect(spyGetWorkflowActions).toHaveBeenCalled();
            });

            it('should trigger a store reload if the url is the same', () => {
                spectator.detectChanges();
                const spyReload = jest.spyOn(store, 'reloadCurrentPage');
                const spyLocation = jest.spyOn(location, 'go');

                spectator.triggerEventHandler(
                    DotEmaDialogComponent,
                    'action',
                    DIALOG_ACTION_EVENT({
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
                const reloadSpy = jest.spyOn(store, 'reloadCurrentPage');

                spectator.triggerEventHandler(DotEmaDialogComponent, 'reloadFromDialog', null);

                expect(reloadSpy).toHaveBeenCalled();
            });

            it('should trigger a store reload if the URL from urlContentMap is the same as the current URL', () => {
                const reloadSpy = jest.spyOn(store, 'reloadCurrentPage');
                jest.spyOn(store, 'pageAPIResponse').mockReturnValue(PAGE_RESPONSE_URL_CONTENT_MAP);
                store.loadPageAsset({
                    url: '/test-url',
                    language_id: '1',
                    [PERSONA_KEY]: '1'
                });

                spectator.detectChanges();
                spectator.triggerEventHandler(
                    DotEmaDialogComponent,
                    'action',
                    DIALOG_ACTION_EVENT({
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
    });

    describe('without license', () => {
        beforeEach(() => {
            jest.spyOn(dotLicenseService, 'isEnterprise').mockReturnValue(of(false));
            jest.spyOn(dotLicenseService, 'canAccessEnterprisePortlet').mockReturnValue(of(false));
            spectator.detectChanges();
        });

        it('should render not-license component', () => {
            expect(spectator.query(DotNotLicenseComponent)).toBeDefined();
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

        it('should not render components', () => {
            expect(spectator.query(EditEmaNavigationBarComponent)).toBeNull();
            expect(spectator.query(ToastModule)).toBeNull();
            expect(spectator.query(DotPageToolsSeoComponent)).toBeNull();
        });
    });

    afterEach(() => {
        // Restoring the snapshot to the default
        overrideRouteSnashot(
            activatedRoute,
            SNAPSHOT_MOCK({
                queryParams: INITIAL_PAGE_PARAMS,
                data: UVE_CONFIG_MOCK(BASIC_OPTIONS)
            })
        );
        jest.clearAllMocks();
    });
});
