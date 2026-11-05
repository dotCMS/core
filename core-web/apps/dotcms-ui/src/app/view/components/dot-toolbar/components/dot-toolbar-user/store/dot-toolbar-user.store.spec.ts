import { createServiceFactory, SpectatorService } from '@openng/spectator/vitest';
import { vi } from 'vitest';

import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';

import { take } from 'rxjs/operators';

import {
    DotCurrentUserService,
    DotEventsService,
    DotMessageService,
    DotRouterService,
    DotSystemConfigService,
    DotIframeService
} from '@dotcms/data-access';
import { DotcmsConfigService, LoggerService, LoginService, StringUtils } from '@dotcms/dotcms-js';
import { GlobalStore } from '@dotcms/store';
import {
    DOT_SYSTEM_CONFIG_SERVICE_MOCK,
    DotCurrentUserServiceMock,
    LoginServiceMock,
    mockAuth
} from '@dotcms/utils-testing';

import { DotToolbarUserStore } from './dot-toolbar-user.store';

import { DotMenuService } from '../../../../../../api/services/dot-menu.service';
import { LOCATION_TOKEN } from '../../../../../../providers';
import { DotNavigationService } from '../../../../dot-navigation/services/dot-navigation.service';

describe('DotToolbarUserStore', () => {
    let spectator: SpectatorService<DotToolbarUserStore>;
    let store: DotToolbarUserStore;
    let loginService: LoginService;
    let locationService: Location;
    let dotNavigationService: DotNavigationService;

    const createService = createServiceFactory({
        service: DotToolbarUserStore,
        imports: [RouterTestingModule],
        providers: [
            LoggerService,
            DotMessageService,
            DotNavigationService,
            DotEventsService,
            DotIframeService,
            DotMenuService,
            DotcmsConfigService,
            StringUtils,
            DotRouterService,
            { provide: DotCurrentUserService, useClass: DotCurrentUserServiceMock },
            {
                provide: LOCATION_TOKEN,
                useValue: {
                    reload() {
                        return;
                    }
                }
            },
            { provide: LoginService, useClass: LoginServiceMock },
            {
                provide: DotSystemConfigService,
                useValue: DOT_SYSTEM_CONFIG_SERVICE_MOCK
            },
            GlobalStore,
            provideHttpClient(),
            provideHttpClientTesting()
        ]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
        loginService = spectator.inject(LoginService);
        locationService = spectator.inject(LOCATION_TOKEN);
        dotNavigationService = spectator.inject(DotNavigationService);
    });

    it('should be created', () => {
        expect(store).toBeTruthy();
    });

    it('should set the initial state when init is called', () => {
        store.init();

        store
            .select((s) => s)
            .subscribe((state) => {
                const { items, userData, showLoginAs, showMyAccount } = state;

                expect(items.length).toBeTruthy();
                expect(userData).toEqual({
                    email: mockAuth.loginAsUser.emailAddress,
                    name: mockAuth.loginAsUser.name
                });
                expect(showLoginAs).toBe(false);
                expect(showMyAccount).toBe(false);
            });
    });

    it('should include report an issue action in the menu', () => {
        store.init();

        store
            .select((s) => s)
            .pipe(take(1))
            .subscribe((state) => {
                const reportIssueItem = state.items.find(
                    (item) => item.id === 'dot-toolbar-user-link-report-issue'
                );

                expect(reportIssueItem).toBeTruthy();
                expect(reportIssueItem?.label).toBe('report-an-issue');
                expect(reportIssueItem?.icon).toBe('pi pi-wrench');
            });
    });

    it('should trigger loginService logoutAs, navigate to first portlet and reload the page when logoutAs is called', fakeAsync(() => {
        vi.spyOn(dotNavigationService, 'goToFirstPortlet').mockReturnValue(
            new Promise((resolve) => {
                resolve(true);
            })
        );

        vi.spyOn(loginService, 'logoutAs');
        vi.spyOn(locationService, 'reload');

        store.logoutAs();

        expect(loginService.logoutAs).toHaveBeenCalled();
        expect(dotNavigationService.goToFirstPortlet).toHaveBeenCalled();

        tick();
        expect(locationService.reload).toHaveBeenCalled();
    }));

    describe('bundle recovery', () => {
        const version = 'a'.repeat(64);
        const url = '/api/v2/languages/default/keys';
        let messages: DotMessageService;
        let http: HttpTestingController;

        const installBundle = () => {
            messages.init({ languageBundleVersion: version });
            http.expectOne(url).flush({
                entity: {
                    ...Object.fromEntries(
                        Array.from({ length: 101 }, (_, i) => [`key.${i}`, `Message ${i}`])
                    ),
                    'my-account': 'My account',
                    'login-as': 'Login as',
                    'report-an-issue': 'Report an issue',
                    Logout: 'Log out',
                    'logout-as': 'Log out as'
                },
                languageBundleVersion: version
            });
        };
        const state = () => store.selectSignal((s) => s)();

        beforeEach(() => {
            localStorage.clear();
            messages = spectator.inject(DotMessageService);
            http = spectator.inject(HttpTestingController);
            // Navigation providers eagerly request data unrelated to bundle recovery.
            http.match((request) =>
                ['/api/v1/site/currentSite', '/api/v1/menu'].includes(request.url)
            );
        });

        afterEach(() => {
            http.verify();
            localStorage.clear();
        });

        it('should translate existing items after an initial failure and a child retry without another auth update', () => {
            messages.init({ languageBundleVersion: version });
            http.expectOne(url).flush(null, { status: 500, statusText: 'Server Error' });
            store.init();
            store.showMyAccount(true);
            TestBed.tick();
            const initial = state();
            expect(initial.items[2].label).toBe('my-account');

            installBundle();
            TestBed.tick();

            expect(
                state()
                    .items.filter((item) => item.icon)
                    .map((item) => item.label)
            ).toEqual(['My account', 'Login as', 'Report an issue', 'Log out', 'Log out as']);
            expect(state().userData).toEqual(initial.userData);
            expect(state().showMyAccount).toBe(true);
            const items = state().items;
            messages.init({ languageBundleVersion: version });
            TestBed.tick();
            expect(state().items).toBe(items);
        });

        it('should not rebuild when auth initialization already used the installed bundle', () => {
            installBundle();
            store.init();
            const items = state().items;
            TestBed.tick();
            expect(state().items).toBe(items);
        });

        it('should not populate items before initialization', () => {
            installBundle();
            TestBed.tick();
            expect(state().items).toEqual([]);
        });

        it('should not rebuild for a user who has logged out before recovery', () => {
            store.init();
            const items = state().items;
            vi.spyOn(loginService, 'auth', 'get').mockReturnValue(null);
            installBundle();
            TestBed.tick();
            expect(state().items).toBe(items);
        });

        it('should use the current impersonated user when recovery runs', () => {
            store.init();
            const auth = {
                ...mockAuth,
                isLoginAs: true,
                loginAsUser: { ...mockAuth.loginAsUser, name: 'Current impersonated user' }
            };
            vi.spyOn(loginService, 'auth', 'get').mockReturnValue(auth);
            installBundle();
            TestBed.tick();
            expect(state().items[0].label).toContain('Current impersonated user');
            expect(
                state().items.find((item) => item.id === 'dot-toolbar-user-link-logout')?.visible
            ).toBe(false);
            expect(
                state().items.find((item) => item.id === 'dot-toolbar-user-link-logout-as')?.visible
            ).toBe(true);
        });
    });

    describe('showLoginAs method', () => {
        it('should change its state value to true', () => {
            store.showLoginAs(true);
            store
                .select((s) => s)
                .subscribe((state) => {
                    expect(state.showLoginAs).toBe(true);
                });
        });

        it('should change its state value to false', () => {
            store.showLoginAs(false);
            store
                .select((s) => s)
                .subscribe((state) => {
                    expect(state.showLoginAs).toBe(false);
                });
        });
    });

    describe('showMyAccount method', () => {
        it('should change its state value to true', () => {
            store.showMyAccount(true);
            store
                .select((s) => s)
                .subscribe((state) => {
                    expect(state.showMyAccount).toBe(true);
                });
        });

        it('should change its state value to false', () => {
            store.showMyAccount(false);
            store
                .select((s) => s)
                .subscribe((state) => {
                    expect(state.showMyAccount).toBe(false);
                });
        });
    });
});
