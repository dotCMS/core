import { describe, expect } from '@jest/globals';
import { SpectatorRouting, byTestId, createRoutingFactory } from '@ngneat/spectator/jest';
import { MockComponent } from 'ng-mocks';
import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { By } from '@angular/platform-browser';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import { ConfirmationService, MessageService } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';
import { ToastModule } from 'primeng/toast';

import {
    DotLanguagesService,
    DotLicenseService,
    DotMessageService,
    DotPersonalizeService
} from '@dotcms/data-access';
import { SiteService, mockSites } from '@dotcms/dotcms-js';
import { DotPageToolsSeoComponent } from '@dotcms/portlets/dot-ema/ui';
import { DotNotLicenseComponent } from '@dotcms/ui';
import {
    DotLanguagesServiceMock,
    DotPersonalizeServiceMock,
    SiteServiceMock
} from '@dotcms/utils-testing';

import { EditEmaNavigationBarComponent } from './components/edit-ema-navigation-bar/edit-ema-navigation-bar.component';
import { DotEmaShellComponent } from './dot-ema-shell.component';
import { EditEmaStore } from './store/dot-ema.store';

import { DotEmaDialogComponent } from '../components/dot-ema-dialog/dot-ema-dialog.component';
import { DotActionUrlService } from '../services/dot-action-url/dot-action-url.service';
import { DotPageApiService } from '../services/dot-page-api.service';
import { DEFAULT_PERSONA, WINDOW } from '../shared/consts';
import { NG_CUSTOM_EVENTS } from '../shared/enums';

describe('DotEmaShellComponent', () => {
    let spectator: SpectatorRouting<DotEmaShellComponent>;
    let store: EditEmaStore;
    let siteService: SiteServiceMock;
    let router: Router;

    const createComponent = createRoutingFactory({
        component: DotEmaShellComponent,
        imports: [RouterTestingModule, HttpClientTestingModule],
        detectChanges: false,
        providers: [{ provide: SiteService, useClass: SiteServiceMock }],
        declarations: [MockComponent(DotEmaDialogComponent)],
        componentProviders: [
            MessageService,
            EditEmaStore,
            ConfirmationService,
            DotActionUrlService,
            DotMessageService,
            DialogService,
            {
                provide: DotLanguagesService,
                useValue: new DotLanguagesServiceMock()
            },
            {
                provide: DotPageApiService,
                useValue: {
                    get() {
                        return of({
                            page: {
                                title: 'hello world',
                                identifier: '123',
                                inode: '123',
                                canEdit: true,
                                canRead: true
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
                            site: mockSites[0]
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
                provide: WINDOW,
                useValue: window
            },
            {
                provide: DotPersonalizeService,
                useValue: new DotPersonalizeServiceMock()
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
            store = spectator.inject(EditEmaStore, true);
            router = spectator.inject(Router);
            jest.spyOn(store, 'load');

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
                        href: 'content'
                    },
                    {
                        icon: 'pi-table',
                        label: 'editema.editor.navbar.layout',
                        href: 'layout',
                        isDisabled: false
                    },
                    {
                        icon: 'pi-sliders-h',
                        label: 'editema.editor.navbar.rules',
                        href: `rules/123`,
                        isDisabled: false
                    },
                    {
                        iconURL: 'experiments',
                        label: 'editema.editor.navbar.experiments',
                        href: 'experiments'
                    },
                    {
                        icon: 'pi-th-large',
                        label: 'editema.editor.navbar.page-tools',
                        action: expect.any(Function)
                    },
                    {
                        icon: 'pi-ellipsis-v',
                        label: 'editema.editor.navbar.properties',
                        action: expect.any(Function)
                    }
                ]);
            });
        });

        describe('router', () => {
            it('should trigger an store load with default values', () => {
                spectator.detectChanges();

                expect(store.load).toHaveBeenCalledWith({
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
                expect(store.load).toHaveBeenCalledWith({
                    clientHost: 'http://localhost:3000',
                    language_id: 2,
                    url: 'my-awesome-page',
                    'com.dotmarketing.persona.id': 'SomeCoolDude'
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
            it('should trigger a navigate when saving and the url changed', () => {
                const navigate = jest.spyOn(router, 'navigate');

                spectator.detectChanges();

                const dialog = spectator.debugElement.query(By.css('[data-testId="ema-dialog"]'));

                spectator.triggerEventHandler(dialog, 'action', {
                    event: new CustomEvent('ng-event', {
                        detail: {
                            name: NG_CUSTOM_EVENTS.SAVE_PAGE,
                            payload: {
                                htmlPageReferer: '/my-awesome-page'
                            }
                        }
                    })
                });
                spectator.detectChanges();

                expect(navigate).toHaveBeenCalledWith([], {
                    queryParams: {
                        url: 'my-awesome-page'
                    },
                    queryParamsHandling: 'merge'
                });
            });

            it('should trigger a store load if the url is the same', () => {
                const loadMock = jest.spyOn(store, 'load');

                spectator.detectChanges();

                const dialog = spectator.debugElement.query(By.css('[data-testId="ema-dialog"]'));

                spectator.triggerEventHandler(dialog, 'action', {
                    event: new CustomEvent('ng-event', {
                        detail: {
                            name: NG_CUSTOM_EVENTS.SAVE_PAGE,
                            payload: {
                                htmlPageReferer: '/my-awesome-page'
                            }
                        }
                    })
                });

                spectator.detectChanges();

                expect(loadMock).toHaveBeenCalledWith({
                    clientHost: 'http://localhost:3000',
                    language_id: 1,
                    url: 'index',
                    'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
                });
            });
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
                                    site: mockSites[0]
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

        it('should not render components', () => {
            spectator.detectChanges();
            expect(spectator.query(EditEmaNavigationBarComponent)).toBeNull();
            expect(spectator.query(ToastModule)).toBeNull();
            expect(spectator.query(DotPageToolsSeoComponent)).toBeNull();
        });
    });

    describe('without license ', () => {
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
                                    site: mockSites[0]
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
});
