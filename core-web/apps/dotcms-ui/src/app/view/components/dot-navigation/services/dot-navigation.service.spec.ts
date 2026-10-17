/* eslint-disable @typescript-eslint/no-explicit-any */

import { of, Subject } from 'rxjs';
import { vi } from 'vitest';

import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Title } from '@angular/platform-browser';
import { NavigationEnd, Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import {
    DotCurrentUserService,
    DotEventsService,
    DotEventsSocket,
    DotSystemEventType,
    DotIframeService,
    DotRouterService,
    DotSystemConfigService
} from '@dotcms/data-access';
import { Auth, LoginService } from '@dotcms/dotcms-js';
import { GlobalStore } from '@dotcms/store';
import { DotCurrentUserServiceMock, LoginServiceMock } from '@dotcms/utils-testing';

import { DotNavigationService } from './dot-navigation.service';

class RouterMock {
    _events: Subject<any> = new Subject();
    _routerState: any;
    _currentNavigation: any = {
        extras: {
            state: {
                menuId: '123'
            }
        }
    };

    url = '';

    getCurrentNavigation() {
        return this._currentNavigation;
    }

    get events() {
        return this._events.asObservable();
    }

    get routerState() {
        return {
            snapshot: {
                url: this.url || 'hello/world'
            }
        };
    }

    triggerNavigationEnd(url: string): void {
        this.url = url || '/url/789';
        this._events.next(new NavigationEnd(0, this.url, this.url));
    }

    navigateByUrl() {
        /* */
    }
}
class TitleServiceMock {
    getTitle(): string {
        return 'dotCMS platform';
    }

    setTitle = vi.fn();
}

export const dotMenuMock = () => {
    return {
        active: false,
        id: '123',
        isOpen: false,
        label: 'Name',
        menuItems: [
            {
                active: false,
                ajax: true,
                angular: true,
                id: '123',
                label: 'Label 1',
                url: 'url/one',
                menuLink: 'url/one',
                parentMenuId: '123'
            },
            {
                active: false,
                ajax: true,
                angular: true,
                id: '456',
                label: 'Label 2',
                url: 'url/two',
                menuLink: 'url/two',
                parentMenuId: '123'
            }
        ],
        name: 'Menu 1',
        tabDescription: 'Description',
        tabIcon: 'icon',
        tabName: 'Name',
        url: '/url/index'
    };
};

export const dotMenuMock1 = () => {
    return {
        ...dotMenuMock(),
        active: false,
        id: '456',
        name: 'Menu 2',
        url: '/url/456',
        menuItems: [
            {
                ...dotMenuMock().menuItems[0],
                active: true,
                id: '789'
            },
            {
                ...dotMenuMock().menuItems[1],
                active: false,
                id: '000'
            }
        ]
    };
};

const basemockUser = {
    admin: true,
    emailAddress: 'admin@dotcms.com',
    firstName: 'Admin',
    lastName: 'Admin',
    loggedInDate: 123456789,
    userId: '123'
};
const baseMockAuth: Auth = {
    loginAsUser: null,
    user: basemockUser
};

describe('DotNavigationService', () => {
    vi.setConfig({ testTimeout: 10000 }); // Increase timeout for this test suite

    const MENU_URL = '/api/v1/menu';

    let service: DotNavigationService;
    let dotRouterService: DotRouterService;
    let loginService: LoginService;
    let router: RouterMock;
    let titleService: Title;
    let httpMock: HttpTestingController;
    let globalStore: InstanceType<typeof GlobalStore>;
    let layoutUpdated$: Subject<void>;
    let dotRouterServiceMock: any;

    const menuResponse = (menus = [dotMenuMock(), dotMenuMock1()]) => ({ entity: menus });

    /** Answers the single menu request that is pending and lets the store settle. */
    const flushMenu = (menus = [dotMenuMock(), dotMenuMock1()]) => {
        httpMock.expectOne(MENU_URL).flush(menuResponse(menus));
    };

    const setup = (url = '') => {
        router = new RouterMock();
        router.url = url;
        layoutUpdated$ = new Subject<void>();
        const getPortletIdFn = vi.fn((routeUrl: string) => {
            // Extract portlet id from URL like /c/123 -> '123'
            if (!routeUrl) return '123-567';
            routeUrl = decodeURIComponent(routeUrl);
            if (routeUrl.indexOf('?') > 0) {
                routeUrl = routeUrl.substring(0, routeUrl.indexOf('?'));
            }
            const urlSegments = routeUrl
                .split('/')
                .filter((item) => item !== '' && item !== '#' && item !== 'c');
            return urlSegments.indexOf('add') > -1
                ? urlSegments.splice(-1)[0]
                : urlSegments[0] || '123-567';
        });

        dotRouterServiceMock = {
            get currentPortlet() {
                const routeUrl = router.routerState.snapshot.url;
                return {
                    id: getPortletIdFn(routeUrl),
                    url: routeUrl
                };
            },
            get queryParams() {
                return { mId: '123' };
            },
            reloadCurrentPortlet: vi.fn(),
            gotoPortlet: vi.fn().mockReturnValue(new Promise((resolve) => resolve(true))),
            getPortletId: getPortletIdFn
        };

        TestBed.configureTestingModule({
            providers: [
                DotEventsService,
                DotNavigationService,
                { provide: Title, useClass: TitleServiceMock },
                { provide: LoginService, useClass: LoginServiceMock },
                { provide: Router, useValue: router },
                { provide: DotIframeService, useValue: { reload: vi.fn() } },
                { provide: DotRouterService, useValue: dotRouterServiceMock },
                { provide: DotSystemConfigService, useValue: { getSystemConfig: () => of({}) } },
                { provide: DotCurrentUserService, useClass: DotCurrentUserServiceMock },
                {
                    provide: DotEventsSocket,
                    useValue: {
                        connect: vi.fn().mockReturnValue(of(null)),
                        status$: vi.fn().mockReturnValue(of('connected')),
                        messages: vi.fn().mockReturnValue(of()),
                        on: vi.fn((event: string) =>
                            event === DotSystemEventType.UPDATE_PORTLET_LAYOUTS
                                ? layoutUpdated$.asObservable()
                                : of()
                        ),
                        destroy: vi.fn()
                    }
                },
                GlobalStore,
                provideHttpClient(),
                provideHttpClientTesting()
            ],
            imports: [RouterTestingModule]
        });

        httpMock = TestBed.inject(HttpTestingController);
        globalStore = TestBed.inject(GlobalStore);
        service = TestBed.inject(DotNavigationService);
        dotRouterService = TestBed.inject(DotRouterService);
        loginService = TestBed.inject(LoginService);
        titleService = TestBed.inject(Title);

        vi.spyOn(titleService, 'setTitle');
        localStorage.clear();
    };

    beforeEach(() => setup());

    describe('loading', () => {
        it('asks the store for the menu once when it is created', () => {
            expect(httpMock.match(MENU_URL)).toHaveLength(1);
        });

        it('leaves the store ready with the menu it received', () => {
            flushMenu();

            expect(globalStore.status()).toBe('ready');
            expect(globalStore.menuGroup().map((group) => group.id)).toEqual(['123', '456']);
        });

        it('does not send a menu request on NavigationEnd, however many times it fires', () => {
            flushMenu();

            router.triggerNavigationEnd('/c/123');
            router.triggerNavigationEnd('/c/456');
            router.triggerNavigationEnd('url/one');

            httpMock.expectNone(MENU_URL);
        });

        it('highlights the portlet of the current url once the first load ends', () => {
            TestBed.resetTestingModule();
            setup('/c/123');

            flushMenu();
            TestBed.tick();

            expect(globalStore.activeMenuItem()?.id).toBe('123');
        });

        it('does not drive the highlight itself: the store owns it', () => {
            expect('setActiveMenu' in globalStore).toBe(false);
        });
    });

    describe('UPDATE_PORTLET_LAYOUTS', () => {
        it('keeps the highlight of the portlet the user is on, even when the url has no mId', () => {
            TestBed.resetTestingModule();
            setup('/c/123');
            flushMenu();
            TestBed.tick();

            layoutUpdated$.next();
            httpMock.expectOne(MENU_URL).flush(menuResponse([dotMenuMock(), dotMenuMock1()]));
            TestBed.tick();

            expect(globalStore.activeMenuItem()?.id).toBe('123');
        });

        it('leaves nothing highlighted when the update removes that portlet', () => {
            TestBed.resetTestingModule();
            setup('/c/123');
            flushMenu();
            TestBed.tick();

            // The fixtures reuse labels between menus; rename them so a label cannot stand in for the portlet.
            const withoutThePortlet = {
                ...dotMenuMock1(),
                menuItems: dotMenuMock1().menuItems.map((item) => ({
                    ...item,
                    label: `Other ${item.id}`
                }))
            };

            layoutUpdated$.next();
            httpMock.expectOne(MENU_URL).flush(menuResponse([withoutThePortlet]));
            TestBed.tick();

            expect(globalStore.activeMenuItem()).toBeNull();
        });

        it('reloads the menu exactly once and replaces the groups', () => {
            flushMenu();

            layoutUpdated$.next();
            const requests = httpMock.match(MENU_URL);

            expect(requests).toHaveLength(1);
            requests[0].flush(menuResponse([dotMenuMock1()]));
            expect(globalStore.menuGroup().map((group) => group.id)).toEqual(['456']);
        });
    });

    describe('goToFirstPortlet', () => {
        it('goes to the first portlet with its group id as mId', () => {
            flushMenu();

            service.goToFirstPortlet();

            expect(dotRouterService.gotoPortlet).toHaveBeenCalledWith('url/one', {
                queryParams: { mId: '123' }
            });
            expect(dotRouterService.gotoPortlet).toHaveBeenCalledTimes(1);
        });

        it('resolves false when there is no menu', () => {
            return expect(service.goToFirstPortlet()).resolves.toBe(false);
        });
    });

    describe('login and login-as', () => {
        it('reloads the menu and then goes to the first portlet of the new menu', () => {
            flushMenu();
            vi.mocked(dotRouterService.gotoPortlet).mockClear();

            (loginService as unknown as LoginServiceMock).triggerNewAuth(baseMockAuth);
            const requests = httpMock.match(MENU_URL);

            expect(requests).toHaveLength(1);
            expect(dotRouterService.gotoPortlet).not.toHaveBeenCalled();

            requests[0].flush(menuResponse([dotMenuMock1()]));

            expect(dotRouterService.gotoPortlet).toHaveBeenCalledWith('url/one', {
                queryParams: { mId: '456' }
            });
            expect(dotRouterService.gotoPortlet).toHaveBeenCalledTimes(1);
        });

        it('does the same when the new auth is a login-as', () => {
            flushMenu();
            vi.mocked(dotRouterService.gotoPortlet).mockClear();

            (loginService as unknown as LoginServiceMock).triggerNewAuth({
                ...baseMockAuth,
                loginAsUser: { ...basemockUser, userId: '456' } as never
            });
            httpMock.expectOne(MENU_URL).flush(menuResponse([dotMenuMock1()]));

            expect(dotRouterService.gotoPortlet).toHaveBeenCalledTimes(1);
            expect(globalStore.menuGroup().map((group) => group.id)).toEqual(['456']);
        });

        it('ignores an auth event that carries no user', () => {
            flushMenu();

            (loginService as unknown as LoginServiceMock).triggerNewAuth({
                loginAsUser: null,
                user: null
            } as unknown as Auth);

            httpMock.expectNone(MENU_URL);
        });
    });

    describe('document title', () => {
        it('is set from the menu item of the url, once', () => {
            flushMenu();

            router.triggerNavigationEnd('url/one');

            expect(titleService.setTitle).toHaveBeenCalledWith('Label 1 - dotCMS platform');
            expect(titleService.setTitle).toHaveBeenCalledTimes(1);
        });
    });

    describe('when the menu request fails', () => {
        const failRequest = () =>
            httpMock.expectOne(MENU_URL).flush('x', { status: 500, statusText: 'Server Error' });

        it('records the error and throws nothing when the first load fails', () => {
            expect(() => failRequest()).not.toThrow();

            expect(globalStore.status()).toBe('error');
            expect(globalStore.menuGroup()).toEqual([]);
        });

        it('has nowhere to go when there is no menu', async () => {
            failRequest();

            await expect(service.goToFirstPortlet()).resolves.toBe(false);
            expect(dotRouterService.gotoPortlet).not.toHaveBeenCalled();
        });

        it('keeps the menu that was showing when a layout update reload fails', () => {
            flushMenu();

            layoutUpdated$.next();
            expect(() => failRequest()).not.toThrow();

            expect(globalStore.status()).toBe('error');
            expect(globalStore.menuGroup().map((group) => group.id)).toEqual(['123', '456']);
        });

        it('does not go to a first portlet of a stale menu when the login reload fails', () => {
            flushMenu();
            vi.mocked(dotRouterService.gotoPortlet).mockClear();

            (loginService as unknown as LoginServiceMock).triggerNewAuth(baseMockAuth);
            failRequest();

            expect(dotRouterService.gotoPortlet).not.toHaveBeenCalled();
        });

        it('tries again on the next navigation and recovers', () => {
            failRequest();

            router.triggerNavigationEnd('url/one');
            flushMenu();

            expect(globalStore.status()).toBe('ready');
            expect(titleService.setTitle).toHaveBeenCalledWith('Label 1 - dotCMS platform');
        });
    });
});
