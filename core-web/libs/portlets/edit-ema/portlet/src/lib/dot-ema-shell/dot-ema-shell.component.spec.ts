import { describe, expect } from '@jest/globals';
import { ActivatedRouteStub, createMouseEvent } from '@ngneat/spectator';
import {
    SpectatorRouting,
    byTestId,
    createRoutingFactory,
    SpyObject
} from '@ngneat/spectator/jest';
import { MockComponent } from 'ng-mocks';
import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { fakeAsync, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { ActivatedRoute, ActivatedRouteSnapshot, Router, UrlSegment } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import { ConfirmationService, MessageService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogService } from 'primeng/dynamicdialog';
import { ToastModule } from 'primeng/toast';

import {
    DotContentletLockerService,
    DotExperimentsService,
    DotLanguagesService,
    DotLicenseService,
    DotMessageService,
    DotWorkflowActionsFireService,
    PushPublishService
} from '@dotcms/data-access';
import {
    DotcmsConfigService,
    DotcmsEventsService,
    LoginService,
    SiteService,
    mockSites
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
import { PAGE_RESPONSE_BY_LANGUAGE_ID, PAYLOAD_MOCK } from '../shared/mocks';
import { UVEStore } from '../store/dot-uve.store';

describe('DotEmaShellComponent', () => {
    let spectator: SpectatorRouting<DotEmaShellComponent>;
    let store: SpyObject<InstanceType<typeof UVEStore>>;

    let siteService: SiteServiceMock;
    let confirmationService: SpyObject<ConfirmationService>;
    let confirmationServiceSpy: jest.SpyInstance;
    let router: Router;
    let route: ActivatedRoute;

    const createComponent = createRoutingFactory({
        component: DotEmaShellComponent,
        imports: [RouterTestingModule, HttpClientTestingModule, ConfirmDialogModule],
        detectChanges: false,
        firstChild: new ActivatedRouteStub({
            url: [new UrlSegment('content', {})]
        }),
        providers: [
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
        declarations: [MockComponent(DotEmaDialogComponent)],
        componentProviders: [
            MessageService,
            UVEStore,
            ConfirmationService,
            DotActionUrlService,
            DotMessageService,
            DialogService,
            DotWorkflowActionsFireService,
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
                        return PAGE_RESPONSE_BY_LANGUAGE_ID[language_id];
                    },
                    getClientPage({ language_id }, _clientConfig) {
                        return PAGE_RESPONSE_BY_LANGUAGE_ID[language_id];
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

    describe('with queryParams', () => {
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
            confirmationService = spectator.inject(ConfirmationService, true);
            jest.spyOn(store, 'init');
            confirmationServiceSpy = jest.spyOn(confirmationService, 'confirm');

            spectator.triggerNavigation({
                url: [],
                queryParams: {
                    language_id: 1,
                    url: 'index',
                    'com.dotmarketing.persona.id': 'modes.persona.no.persona'
                },
                data: {
                    data: {
                        url: 'http://localhost:3000'
                    }
                }
            });
        });

        describe('DOM', () => {
            it('should have a navigation bar', () => {
                spectator.detectChanges();
                expect(spectator.query(byTestId('ema-nav-bar'))).not.toBeNull();
            });

            it('should have nav bar with items', () => {
                const navBarComponent = spectator.query(EditEmaNavigationBarComponent);

                expect(navBarComponent.items).toEqual([
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
                ]);
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

        describe('router', () => {
            it('should trigger an store load with default values', () => {
                spectator.detectChanges();

                expect(store.init).toHaveBeenCalledWith({
                    clientHost: 'http://localhost:3000',
                    language_id: 1,
                    url: 'index',
                    'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
                });
            });

            it('should trigger a load when changing the queryParams', () => {
                spectator.triggerNavigation({
                    url: [],
                    queryParams: {
                        language_id: 2,
                        url: 'my-awesome-page',
                        'com.dotmarketing.persona.id': 'SomeCoolDude'
                    }
                });

                spectator.detectChanges();
                expect(store.init).toHaveBeenCalledWith({
                    clientHost: 'http://localhost:3000',
                    language_id: 2,
                    url: 'my-awesome-page',
                    'com.dotmarketing.persona.id': 'SomeCoolDude'
                });
            });

            it("should not trigger a load when the queryParams didn't change", () => {
                spectator.triggerNavigation({
                    url: [],
                    queryParams: {
                        language_id: 1,
                        url: 'index',
                        'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
                    }
                });

                spectator.detectChanges();
                expect(store.init).not.toHaveBeenCalledTimes(2); // The first call is on the beforeEach
            });

            it('should trigger a load when changing the clientHost and it is on the allowedDevURLs', () => {
                spectator.triggerNavigation({
                    url: [],
                    queryParams: {
                        clientHost: 'http://localhost:1111',
                        language_id: 1,
                        url: 'index',
                        'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
                    },
                    data: {
                        data: {
                            options: {
                                allowedDevURLs: ['http://localhost:1111']
                            }
                        }
                    }
                });

                spectator.detectChanges();
                expect(store.init).toHaveBeenLastCalledWith({
                    clientHost: 'http://localhost:1111',
                    language_id: 1,
                    url: 'index',
                    'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
                });
            });

            it('should trigger a load when changing the clientHost and it is on the allowedDevURLs with a slash at the end', () => {
                spectator.triggerNavigation({
                    url: [],
                    queryParams: {
                        clientHost: 'http://localhost:1111',
                        language_id: 1,
                        url: 'index',
                        'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
                    },
                    data: {
                        data: {
                            options: {
                                allowedDevURLs: ['http://localhost:1111/']
                            }
                        }
                    }
                });

                spectator.detectChanges();
                expect(store.init).toHaveBeenLastCalledWith({
                    clientHost: 'http://localhost:1111',
                    language_id: 1,
                    url: 'index',
                    'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
                });
            });

            it('should trigger a load when changing the clientHost has an slash at the and it is on the allowedDevURLs without the slash at the end', () => {
                spectator.triggerNavigation({
                    url: [],
                    queryParams: {
                        clientHost: 'http://localhost:1111/',
                        language_id: 1,
                        url: 'index',
                        'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
                    },
                    data: {
                        data: {
                            options: {
                                allowedDevURLs: ['http://localhost:1111']
                            }
                        }
                    }
                });

                spectator.detectChanges();
                expect(store.init).toHaveBeenLastCalledWith({
                    clientHost: 'http://localhost:1111/',
                    language_id: 1,
                    url: 'index',
                    'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
                });
            });

            it('should trigger a load when changing the clientHost has an slash at the and it is on the allowedDevURLs with the slash at the end', () => {
                spectator.triggerNavigation({
                    url: [],
                    queryParams: {
                        clientHost: 'http://localhost:1111/',
                        language_id: 1,
                        url: 'index',
                        'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
                    },
                    data: {
                        data: {
                            options: {
                                allowedDevURLs: ['http://localhost:1111/']
                            }
                        }
                    }
                });

                spectator.detectChanges();
                expect(store.init).toHaveBeenLastCalledWith({
                    clientHost: 'http://localhost:1111/',
                    language_id: 1,
                    url: 'index',
                    'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
                });
            });

            it('should trigger a navigate without the clientHost queryParam when the url is not in the allowedDevURLs', () => {
                spectator.triggerNavigation({
                    url: [],
                    queryParams: {
                        clientHost: 'http://localhost:1111',
                        language_id: 1,
                        url: 'index',
                        'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
                    },
                    data: {
                        data: {
                            options: {
                                allowedDevURLs: ['http://localhost:4200']
                            }
                        }
                    }
                });

                spectator.detectChanges();

                expect(router.navigate).toHaveBeenCalledWith([], {
                    queryParams: {
                        clientHost: null,
                        'com.dotmarketing.persona.id': 'modes.persona.no.persona',
                        language_id: 1,
                        url: 'index'
                    },
                    queryParamsHandling: 'merge'
                });

                expect(store.init).toHaveBeenLastCalledWith({
                    clientHost: 'http://localhost:3000',
                    language_id: 1,
                    url: 'index',
                    'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
                });
            });

            it('should trigger a navigate without the clientHost queryParam when the allowedDevURLs is empty', () => {
                spectator.triggerNavigation({
                    url: [],
                    queryParams: {
                        clientHost: 'http://localhost:1111',
                        language_id: 1,
                        url: 'index',
                        'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
                    },
                    data: {
                        data: {
                            options: {
                                allowedDevURLs: []
                            }
                        }
                    }
                });

                spectator.detectChanges();

                expect(router.navigate).toHaveBeenCalledWith([], {
                    queryParams: {
                        clientHost: null,
                        'com.dotmarketing.persona.id': 'modes.persona.no.persona',
                        language_id: 1,
                        url: 'index'
                    },
                    queryParamsHandling: 'merge'
                });

                expect(store.init).toHaveBeenLastCalledWith({
                    clientHost: 'http://localhost:3000',
                    language_id: 1,
                    url: 'index',
                    'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
                });
            });
            it('should trigger a navigate without the clientHost queryParam when the allowedDevURLs is has a wrong data type', () => {
                spectator.triggerNavigation({
                    url: [],
                    queryParams: {
                        clientHost: 'http://localhost:1111',
                        language_id: 1,
                        url: 'index',
                        'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
                    },
                    data: {
                        data: {
                            options: {
                                allowedDevURLs: "I'm not an array"
                            }
                        }
                    }
                });

                spectator.detectChanges();

                expect(router.navigate).toHaveBeenCalledWith([], {
                    queryParams: {
                        clientHost: null,
                        'com.dotmarketing.persona.id': 'modes.persona.no.persona',
                        language_id: 1,
                        url: 'index'
                    },
                    queryParamsHandling: 'merge'
                });

                expect(store.init).toHaveBeenLastCalledWith({
                    clientHost: 'http://localhost:3000',
                    language_id: 1,
                    url: 'index',
                    'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
                });
            });

            it('should trigger a navigate without the clientHost queryParam when the allowedDevURLs is is not present', () => {
                spectator.triggerNavigation({
                    url: [],
                    queryParams: {
                        clientHost: 'http://localhost:1111',
                        language_id: 1,
                        url: 'index',
                        'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
                    },
                    data: {
                        data: {
                            options: {
                                someRandomOption: 'Hello from the other side'
                            }
                        }
                    }
                });

                spectator.detectChanges();

                expect(router.navigate).toHaveBeenCalledWith([], {
                    queryParams: {
                        clientHost: null,
                        'com.dotmarketing.persona.id': 'modes.persona.no.persona',
                        language_id: 1,
                        url: 'index'
                    },
                    queryParamsHandling: 'merge'
                });

                expect(store.init).toHaveBeenLastCalledWith({
                    clientHost: 'http://localhost:3000',
                    language_id: 1,
                    url: 'index',
                    'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
                });
            });

            it('should trigger a navigate without the clientHost queryParam when the options are not present', () => {
                spectator.triggerNavigation({
                    url: [],
                    queryParams: {
                        clientHost: 'http://localhost:1111',
                        language_id: 1,
                        url: 'index',
                        'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
                    },
                    data: {
                        data: {
                            url: 'http://localhost:3000',
                            pattern: '.*'
                        }
                    }
                });

                spectator.detectChanges();

                expect(router.navigate).toHaveBeenCalledWith([], {
                    queryParams: {
                        clientHost: null,
                        'com.dotmarketing.persona.id': 'modes.persona.no.persona',
                        language_id: 1,
                        url: 'index'
                    },
                    queryParamsHandling: 'merge'
                });

                expect(store.init).toHaveBeenLastCalledWith({
                    clientHost: 'http://localhost:3000',
                    language_id: 1,
                    url: 'index',
                    'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
                });
            });
            it('should trigger a navigate without the clientHost queryParam when the data is not present', () => {
                spectator.triggerNavigation({
                    url: [],
                    queryParams: {
                        clientHost: 'http://localhost:1111',
                        language_id: 1,
                        url: 'index',
                        'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
                    },
                    data: {}
                });

                spectator.detectChanges();

                expect(router.navigate).toHaveBeenCalledWith([], {
                    queryParams: {
                        clientHost: null,
                        'com.dotmarketing.persona.id': 'modes.persona.no.persona',
                        language_id: 1,
                        url: 'index'
                    },
                    queryParamsHandling: 'merge'
                });

                expect(store.init).toHaveBeenLastCalledWith({
                    clientHost: 'http://localhost:3000',
                    language_id: 1,
                    url: 'index',
                    'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
                });
            });

            it('should trigger a navigate without the clientHost queryParam when there is no data in activated route', () => {
                spectator.triggerNavigation({
                    url: [],
                    queryParams: {
                        clientHost: 'http://localhost:1111',
                        language_id: 1,
                        url: 'index',
                        'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
                    }
                });

                spectator.detectChanges();

                expect(router.navigate).toHaveBeenCalledWith([], {
                    queryParams: {
                        clientHost: null,
                        'com.dotmarketing.persona.id': 'modes.persona.no.persona',
                        language_id: 1,
                        url: 'index'
                    },
                    queryParamsHandling: 'merge'
                });

                expect(store.init).toHaveBeenLastCalledWith({
                    clientHost: 'http://localhost:3000',
                    language_id: 1,
                    url: 'index',
                    'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
                });
            });
        });

        describe('language checking', () => {
            it('should not trigger the confirmation service if the page is translated to the current language', () => {
                spectator.detectChanges();

                expect(confirmationServiceSpy).not.toHaveBeenCalled();
            });

            it('should not trigger the confirmation service if the page dont have current language', () => {
                spectator.triggerNavigation({
                    url: [],
                    queryParams: {
                        language_id: 3,
                        url: 'index',
                        'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
                    }
                });

                spectator.detectChanges();

                expect(confirmationServiceSpy).not.toHaveBeenCalled();
            });

            it("should trigger the confirmation service if the page isn't translated to the current language", fakeAsync(() => {
                spectator.triggerNavigation({
                    url: [],
                    queryParams: {
                        language_id: 2,
                        url: 'index',
                        'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
                    }
                });

                spectator.detectChanges();

                tick();

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
                spectator.triggerNavigation({
                    url: [],
                    queryParams: {
                        language_id: 2,
                        url: 'index',
                        'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
                    }
                });

                spectator.detectChanges();

                tick(1000);
                spectator.detectChanges();

                const confirmDialog = spectator.query(byTestId('confirm-dialog'));

                const clickEvent = createMouseEvent('click');

                confirmDialog.querySelector('.p-confirm-dialog-reject').dispatchEvent(clickEvent);

                spectator.detectChanges();

                expect(router.navigate).toHaveBeenCalledWith([], {
                    queryParams: { language_id: 1 },
                    queryParamsHandling: 'merge'
                });
            }));

            it('should open a dialog to create the page in the new language when the user accepts the creation', fakeAsync(() => {
                spectator.triggerNavigation({
                    url: [],
                    queryParams: {
                        language_id: 2,
                        url: 'index',
                        'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
                    }
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
                spectator.triggerNavigation({
                    url: [],
                    queryParams: {
                        language_id: 2,
                        url: 'index',
                        'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
                    }
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
                    payload: PAYLOAD_MOCK,
                    form: {
                        status: FormStatus.DIRTY,
                        isTranslation: true
                    }
                });

                expect(router.navigate).toHaveBeenCalledWith([], {
                    queryParams: { language_id: 1 },
                    queryParamsHandling: 'merge'
                });
            }));

            it('should open a dialog to create the page and navigate to default language if the user closes the dialog without saving and without editing ', fakeAsync(() => {
                spectator.triggerNavigation({
                    url: [],
                    queryParams: {
                        language_id: 2,
                        url: 'index',
                        'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
                    }
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
                    payload: PAYLOAD_MOCK,
                    form: {
                        status: FormStatus.PRISTINE,
                        isTranslation: true
                    }
                });

                expect(router.navigate).toHaveBeenCalledWith([], {
                    queryParams: { language_id: 1 },
                    queryParamsHandling: 'merge'
                });
            }));

            it('should open a dialog to create the page and do nothing when the user creates the page correctly with SAVE_PAGE and closes the dialog', fakeAsync(() => {
                spectator.triggerNavigation({
                    url: [],
                    queryParams: {
                        language_id: 2,
                        url: 'index',
                        'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
                    }
                });

                spectator.detectChanges();

                tick(1000);
                spectator.detectChanges();

                const confirmDialog = spectator.query(byTestId('confirm-dialog'));

                const clickEvent = createMouseEvent('click');

                confirmDialog.querySelector('.p-confirm-dialog-accept').dispatchEvent(clickEvent);

                spectator.detectChanges();

                spectator.detectChanges();
                spectator.triggerEventHandler(DotEmaDialogComponent, 'action', {
                    event: new CustomEvent('ng-event', {
                        detail: {
                            name: NG_CUSTOM_EVENTS.DIALOG_CLOSED
                        }
                    }),
                    payload: PAYLOAD_MOCK,
                    form: {
                        isTranslation: true,
                        status: FormStatus.SAVED
                    }
                });

                spectator.detectChanges();

                expect(router.navigate).not.toHaveBeenCalled();
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
            it('should trigger a navigate when saving and the url changed', () => {
                const navigate = jest.spyOn(router, 'navigate');

                spectator.detectChanges();

                spectator.triggerEventHandler(DotEmaDialogComponent, 'action', {
                    event: new CustomEvent('ng-event', {
                        detail: {
                            name: NG_CUSTOM_EVENTS.SAVE_PAGE,
                            payload: {
                                htmlPageReferer: '/my-awesome-page'
                            }
                        }
                    }),
                    payload: PAYLOAD_MOCK,
                    form: {
                        status: FormStatus.SAVED,
                        isTranslation: false
                    }
                });
                spectator.detectChanges();

                expect(navigate).toHaveBeenCalledWith([], {
                    queryParams: {
                        url: 'my-awesome-page'
                    },
                    queryParamsHandling: 'merge'
                });
            });

            it('should trigger a store reload if the url is the same', () => {
                const reloadMock = jest.spyOn(store, 'reload');

                spectator.detectChanges();

                spectator.triggerEventHandler(DotEmaDialogComponent, 'action', {
                    event: new CustomEvent('ng-event', {
                        detail: {
                            name: NG_CUSTOM_EVENTS.SAVE_PAGE,
                            payload: {
                                htmlPageReferer: 'index'
                            }
                        }
                    }),
                    payload: PAYLOAD_MOCK,
                    form: {
                        status: FormStatus.SAVED,
                        isTranslation: false
                    }
                });

                spectator.detectChanges();

                expect(reloadMock).toHaveBeenCalled();
            });

            it('should reload content from dialog', () => {
                const reloadSpy = jest.spyOn(store, 'reload');

                spectator.triggerEventHandler(DotEmaDialogComponent, 'reloadFromDialog', null);

                expect(reloadSpy).toHaveBeenCalled();
            });
        });
    });

    describe('without license', () => {
        beforeEach(() => {
            spectator = createComponent({
                providers: [
                    {
                        provide: DotPageApiService,
                        useValue: {
                            get() {
                                return of({
                                    page: {
                                        title: 'hello world',
                                        identifier: '123',
                                        inode: '123',
                                        canEdit: false,
                                        canRead: false
                                    },
                                    viewAs: {
                                        language: {
                                            id: 1,
                                            language: 'English',
                                            countryCode: 'US',
                                            languageCode: 'EN',
                                            country: 'United States'
                                        },
                                        persona: DEFAULT_PERSONA
                                    },
                                    site: mockSites[0],
                                    template: { drawed: true }
                                });
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
                        provide: DotLicenseService,
                        useValue: {
                            isEnterprise: () => of(false),
                            canAccessEnterprisePortlet: () => of(false)
                        }
                    }
                ]
            });
        });

        it('should render not-license component', () => {
            spectator.detectChanges();
            expect(spectator.query(DotNotLicenseComponent)).toBeDefined();
        });
    });

    describe('without read permission', () => {
        beforeEach(() => {
            spectator = createComponent({
                providers: [
                    {
                        provide: DotPageApiService,
                        useValue: {
                            get() {
                                return of({
                                    page: {
                                        title: 'hello world',
                                        identifier: '123',
                                        inode: '123',
                                        canEdit: false,
                                        canRead: false
                                    },
                                    viewAs: {
                                        language: {
                                            id: 1,
                                            language: 'English',
                                            countryCode: 'US',
                                            languageCode: 'EN',
                                            country: 'United States'
                                        },
                                        persona: DEFAULT_PERSONA
                                    },
                                    site: mockSites[0],
                                    template: { drawed: true }
                                });
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
                        provide: DotLicenseService,
                        useValue: {
                            isEnterprise: () => of(true),
                            canAccessEnterprisePortlet: () => of(true)
                        }
                    }
                ]
            });
            route = spectator.inject(ActivatedRoute);
            jest.spyOn(route.snapshot, 'firstChild', 'get').mockReturnValue({
                routeConfig: { path: 'content' }
            } as ActivatedRouteSnapshot);
        });

        it('should not render components', () => {
            spectator.detectChanges();
            expect(spectator.query(EditEmaNavigationBarComponent)).toBeNull();
            expect(spectator.query(ToastModule)).toBeNull();
            expect(spectator.query(DotPageToolsSeoComponent)).toBeNull();
        });
    });
});
