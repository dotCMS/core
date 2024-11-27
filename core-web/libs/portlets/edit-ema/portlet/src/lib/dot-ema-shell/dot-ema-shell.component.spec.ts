import { describe, expect } from '@jest/globals';
import { createMouseEvent } from '@ngneat/spectator';
import { SpyObject, createComponentFactory, Spectator, byTestId } from '@ngneat/spectator/jest';
import { MockComponent } from 'ng-mocks';
import { of } from 'rxjs';

import { Location } from '@angular/common';
import { fakeAsync, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';

import { ConfirmationService, MessageService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogService } from 'primeng/dynamicdialog';
import { ToastModule } from 'primeng/toast';

import { CLIENT_ACTIONS } from '@dotcms/client';
import {
    DotContentletLockerService,
    DotExperimentsService,
    DotLanguagesService,
    DotLicenseService,
    DotMessageService,
    DotPropertiesService,
    DotWorkflowActionsFireService,
    PushPublishService
} from '@dotcms/data-access';
import {
    DotcmsConfigService,
    DotcmsEventsService,
    LoginService,
    SiteService
} from '@dotcms/dotcms-js';
import { DotPageToolsSeoComponent } from '@dotcms/portlets/dot-ema/ui';
import { DotNotLicenseComponent } from '@dotcms/ui';
import {
    DotExperimentsServiceMock,
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
import { DEFAULT_PERSONA, WINDOW } from '../shared/consts';
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
        clientAction: CLIENT_ACTIONS.NOOP
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
    'com.dotmarketing.persona.id': 'modes.persona.no.persona'
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
    let confirmationService: ConfirmationService;

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
            }
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
                    get({ language_id }) {
                        return PAGE_RESPONSE_BY_LANGUAGE_ID[language_id] || of({});
                    },
                    getClientPage({ language_id }, _clientConfig) {
                        return PAGE_RESPONSE_BY_LANGUAGE_ID[language_id] || of({});
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
                provide: WINDOW,
                useValue: window
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
        confirmationService = spectator.inject(ConfirmationService, true);
    });

    describe('with queryParams', () => {
        it('should trigger an store load with default values', () => {
            const spyStoreLoadPage = jest.spyOn(store, 'loadPageAsset');
            spectator.detectChanges();
            expect(spyStoreLoadPage).toHaveBeenCalledWith(INITIAL_PAGE_PARAMS);
        });

        it('should call store.loadPageAsset when the `loadPageAsset` is called', () => {
            const spyloadPageAsset = jest.spyOn(store, 'loadPageAsset');
            const spyStoreLoadPage = jest.spyOn(store, 'loadPageAsset');
            const spyLocation = jest.spyOn(location, 'replaceState');

            spectator.detectChanges();
            expect(spyloadPageAsset).toHaveBeenCalledWith(INITIAL_PAGE_PARAMS);
            expect(spyStoreLoadPage).toHaveBeenCalledWith(INITIAL_PAGE_PARAMS);
            expect(spyLocation).toHaveBeenCalledWith(
                '/?language_id=1&url=index&variantName=DEFAULT&com.dotmarketing.persona.id=modes.persona.no.persona'
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
                    title: 'hello world'
                });
            });
        });

        describe('Page Params', () => {
            beforeEach(() => spectator.detectChanges());

            it('should update parms when loadPage is triggered', () => {
                const newParams = {
                    language_id: 2,
                    url: 'my-awesome-page',
                    variantName: 'DEFAULT',
                    'com.dotmarketing.persona.id': 'SomeCoolDude'
                };

                const url = router.createUrlTree([], { queryParams: newParams });

                const spyStoreLoadPage = jest.spyOn(store, 'loadPageAsset');
                const spyUrlTree = jest.spyOn(router, 'createUrlTree');
                const spyLocation = jest.spyOn(location, 'replaceState');

                store.loadPageAsset(newParams);
                spectator.detectChanges();
                expect(spyStoreLoadPage).toHaveBeenCalledWith(newParams);
                expect(spyUrlTree).toHaveBeenCalledWith([], { queryParams: newParams });
                expect(spyLocation).toHaveBeenCalledWith(url.toString());
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

        describe('language checking', () => {
            let confirmationServiceSpy: jest.SpyInstance;

            beforeEach(() => {
                spectator.detectChanges();
                confirmationServiceSpy = jest.spyOn(confirmationService, 'confirm');
            });

            it('should not trigger the confirmation service if the page is translated to the current language', () => {
                expect(confirmationServiceSpy).not.toHaveBeenCalled();
            });

            it('should not trigger the confirmation service if the page dont have current language', () => {
                store.loadPageAsset({
                    language_id: 3,
                    url: 'index',
                    'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
                });

                spectator.detectChanges();

                expect(confirmationServiceSpy).not.toHaveBeenCalled();
            });

            it("should trigger the confirmation service if the page isn't translated to the current language", fakeAsync(() => {
                store.loadPageAsset({
                    language_id: 2,
                    url: 'index',
                    'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
                });
                tick();
                spectator.detectChanges();
                expect(confirmationServiceSpy).toHaveBeenCalledWith({
                    accept: expect.any(Function),
                    acceptEvent: expect.any(Object),
                    reject: expect.any(Function),
                    rejectEvent: expect.any(Object),
                    rejectIcon: 'hidden',
                    acceptIcon: 'hidden',
                    key: 'shell-confirm-dialog',
                    header: 'editpage.language-change-missing-lang-populate.confirm.header',
                    message: 'editpage.language-change-missing-lang-populate.confirm.message'
                });
            }));

            it('should trigger a navigation to default language when the user rejects the creation', fakeAsync(() => {
                const spyloadPageAsset = jest.spyOn(store, 'loadPageAsset');

                store.loadPageAsset({
                    language_id: 2,
                    url: 'index',
                    'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
                });

                spectator.detectChanges();

                tick(1000);
                spectator.detectChanges();

                const confirmDialog = spectator.query(byTestId('confirm-dialog'));

                const clickEvent = createMouseEvent('click');

                confirmDialog.querySelector('.p-confirm-dialog-reject').dispatchEvent(clickEvent);

                spectator.detectChanges();

                expect(spyloadPageAsset).toHaveBeenCalledWith({ language_id: '1' });
            }));

            it('should open a dialog to create the page in the new language when the user accepts the creation', fakeAsync(() => {
                store.loadPageAsset({
                    language_id: 2,
                    url: 'index',
                    'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
                });

                spectator.detectChanges();
                const dialog = spectator.component.dialog;
                const translatePageSpy = jest.spyOn(dialog, 'translatePage');

                tick(1000);
                spectator.detectChanges();

                const confirmDialog = spectator.query(byTestId('confirm-dialog'));

                confirmDialog
                    .querySelector('.p-confirm-dialog-accept')
                    .dispatchEvent(new MouseEvent('click'));

                expect(translatePageSpy).toHaveBeenCalledWith({
                    newLanguage: 2,
                    page: {
                        canEdit: true,
                        canRead: true,
                        identifier: '123',
                        inode: '123',
                        live: true,
                        liveInode: '1234',
                        pageURI: 'index',
                        stInode: '12345',
                        title: 'hello world'
                    }
                });
            }));

            it('should open a dialog to create the page and navigate to default language if the user closes the dialog without saving', fakeAsync(() => {
                const spyloadPageAsset = jest.spyOn(store, 'loadPageAsset');
                store.loadPageAsset({
                    language_id: 2,
                    url: 'index',
                    'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
                });

                spectator.detectChanges();

                tick(1000);
                spectator.detectChanges();

                const confirmDialog = spectator.query(byTestId('confirm-dialog'));

                const clickEvent = createMouseEvent('click');

                confirmDialog.querySelector('.p-confirm-dialog-accept').dispatchEvent(clickEvent);

                spectator.detectChanges();

                spectator.triggerEventHandler(DotEmaDialogComponent, 'action', {
                    event: new CustomEvent('ng-event', {
                        detail: {
                            name: NG_CUSTOM_EVENTS.DIALOG_CLOSED
                        }
                    }),
                    actionPayload: PAYLOAD_MOCK,
                    form: {
                        status: FormStatus.DIRTY,
                        isTranslation: true
                    },
                    clientAction: CLIENT_ACTIONS.NOOP
                });

                expect(spyloadPageAsset).toHaveBeenCalledWith({ language_id: '1' });
            }));

            it('should open a dialog to create the page and navigate to default language if the user closes the dialog without saving and without editing ', fakeAsync(() => {
                const spyloadPageAsset = jest.spyOn(store, 'loadPageAsset');
                store.loadPageAsset({
                    language_id: 2,
                    url: 'index',
                    'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
                });

                spectator.detectChanges();

                tick(1000);
                spectator.detectChanges();

                const confirmDialog = spectator.query(byTestId('confirm-dialog'));

                const clickEvent = createMouseEvent('click');

                confirmDialog.querySelector('.p-confirm-dialog-accept').dispatchEvent(clickEvent);

                spectator.detectChanges();

                spectator.triggerEventHandler(DotEmaDialogComponent, 'action', {
                    event: new CustomEvent('ng-event', {
                        detail: {
                            name: NG_CUSTOM_EVENTS.DIALOG_CLOSED
                        }
                    }),
                    actionPayload: PAYLOAD_MOCK,
                    form: {
                        status: FormStatus.PRISTINE,
                        isTranslation: true
                    },
                    clientAction: CLIENT_ACTIONS.NOOP
                });

                expect(spyloadPageAsset).toHaveBeenCalledWith({ language_id: '1' });
            }));

            it('should open a dialog to create the page and do nothing when the user creates the page correctly with SAVE_PAGE and closes the dialog', fakeAsync(() => {
                const pageParams = {
                    language_id: 2,
                    url: 'index',
                    'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
                };

                const spyloadPageAsset = jest.spyOn(store, 'loadPageAsset');
                store.loadPageAsset(pageParams);
                spectator.detectChanges();

                tick(1000);
                spectator.detectChanges();

                const confirmDialog = spectator.query(byTestId('confirm-dialog'));

                const clickEvent = createMouseEvent('click');

                confirmDialog.querySelector('.p-confirm-dialog-accept').dispatchEvent(clickEvent);

                spectator.detectChanges();

                spectator.triggerEventHandler(DotEmaDialogComponent, 'action', {
                    event: new CustomEvent('ng-event', {
                        detail: {
                            name: NG_CUSTOM_EVENTS.DIALOG_CLOSED
                        }
                    }),
                    actionPayload: PAYLOAD_MOCK,
                    form: {
                        isTranslation: true,
                        status: FormStatus.SAVED
                    },
                    clientAction: CLIENT_ACTIONS.NOOP
                });

                spectator.detectChanges();

                expect(spyloadPageAsset).toHaveBeenNthCalledWith(1, pageParams);
            }));
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

            it('should trigger a store reload if the url is the same', () => {
                const spyReload = jest.spyOn(store, 'reloadCurrentPage');
                const spyLocation = jest.spyOn(location, 'replaceState');
                spectator.detectChanges();

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
                    'com.dotmarketing.persona.id': '1'
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

            it('should trigger a spyStoreLoadPage when url path property is changed', () => {
                const spyStoreLoadPage = jest.spyOn(store, 'loadPageAsset');

                spectator.detectChanges();

                spectator.triggerEventHandler(DotEmaDialogComponent, 'action', {
                    event: new CustomEvent('ng-event', {
                        detail: {
                            name: NG_CUSTOM_EVENTS.URL_IS_CHANGED,
                            payload: {
                                htmlPageReferer: '/a-new-url'
                            }
                        }
                    }),
                    actionPayload: PAYLOAD_MOCK,
                    form: {
                        status: FormStatus.SAVED,
                        isTranslation: false
                    },
                    clientAction: CLIENT_ACTIONS.NOOP
                });
                spectator.detectChanges();

                expect(spyStoreLoadPage).toHaveBeenCalledWith({
                    url: '/a-new-url'
                });
            });

            it('should mantain the current URL as queryParam when the URL property is changed and is a URLContentMap', () => {
                jest.spyOn(store, 'pageAPIResponse').mockReturnValue(PAGE_RESPONSE_URL_CONTENT_MAP);

                const spyStoreLoadPage = jest.spyOn(store, 'loadPageAsset');

                spectator.detectChanges();

                spectator.triggerEventHandler(DotEmaDialogComponent, 'action', {
                    event: new CustomEvent('ng-event', {
                        detail: {
                            name: NG_CUSTOM_EVENTS.URL_IS_CHANGED,
                            payload: {
                                htmlPageReferer: '/a-new-url'
                            }
                        }
                    }),
                    actionPayload: PAYLOAD_MOCK,
                    form: {
                        status: FormStatus.SAVED,
                        isTranslation: false
                    },
                    clientAction: CLIENT_ACTIONS.NOOP
                });
                spectator.detectChanges();

                expect(spyStoreLoadPage).toHaveBeenCalledWith({
                    url: '/test-url'
                });
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
