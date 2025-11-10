/* eslint-disable @typescript-eslint/no-explicit-any */

import { Observable, of, Subject } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed, waitForAsync } from '@angular/core/testing';
import { Title } from '@angular/platform-browser';
import { NavigationEnd, Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import { skip } from 'rxjs/operators';

import {
    DotEventsService,
    DotIframeService,
    DotRouterService,
    DotSystemConfigService
} from '@dotcms/data-access';
import { Auth, DotcmsEventsService, LoginService } from '@dotcms/dotcms-js';
import { DotMenu } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';
import { LoginServiceMock } from '@dotcms/utils-testing';

import { DotNavigationService } from './dot-navigation.service';

import { DotMenuService } from '../../../../api/services/dot-menu.service';

class RouterMock {
    _events: Subject<any> = new Subject();
    _routerState: any;

    url = '';

    getCurrentNavigation() {
        return {
            extras: {
                state: {
                    menuId: '123'
                }
            }
        };
    }

    get events() {
        return this._events.asObservable();
    }

    get routerState() {
        return {
            snapshot: {
                url: 'hello/world'
            }
        };
    }

    triggerNavigationEnd(url: string): void {
        this._events.next(new NavigationEnd(0, url || '/url/789', url || '/url/789'));
    }

    navigateByUrl() {
        /* */
    }
}
export class DotMenuServiceMock {
    loadMenu(_force?: boolean): Observable<DotMenu[]> {
        return of([
            {
                ...dotMenuMock(),
                active: true,
                isOpen: true
            },
            dotMenuMock1()
        ]);
    }

    reloadMenu(): Observable<DotMenu[]> {
        return of([dotMenuMock(), dotMenuMock1()]);
    }
}

class TitleServiceMock {
    getTitle(): string {
        return 'dotCMS platform';
    }

    setTitle(_title: string): void {
        /* */
    }
}

class DotcmsEventsServiceMock {
    _events: Subject<any> = new Subject();

    subscribeTo() {
        return this._events;
    }

    trigger() {
        this._events.next();
    }
}

export const dotMenuMock = () => {
    return {
        active: false,
        id: '123',
        isOpen: false,
        menuItems: [
            {
                active: false,
                ajax: true,
                angular: true,
                id: '123',
                label: 'Label 1',
                url: 'url/one',
                menuLink: 'url/link1'
            },
            {
                active: false,
                ajax: true,
                angular: true,
                id: '456',
                label: 'Label 2',
                url: 'url/two',
                menuLink: 'url/link2'
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
    let service: DotNavigationService;
    let dotRouterService: DotRouterService;
    let dotcmsEventsService: DotcmsEventsService;
    let dotEventService: DotEventsService;
    let dotMenuService: DotMenuService;
    let loginService: LoginService;
    let router;
    let titleService: Title;

    beforeEach(waitForAsync(() => {
        const testbed = TestBed.configureTestingModule({
            providers: [
                DotEventsService,
                DotNavigationService,
                {
                    provide: DotcmsEventsService,
                    useClass: DotcmsEventsServiceMock
                },
                {
                    provide: Title,
                    useClass: TitleServiceMock
                },
                {
                    provide: DotMenuService,
                    useClass: DotMenuServiceMock
                },
                {
                    provide: LoginService,
                    useClass: LoginServiceMock
                },
                {
                    provide: Router,
                    useClass: RouterMock
                },
                {
                    provide: DotIframeService,
                    useValue: {
                        reload: jest.fn()
                    }
                },
                {
                    provide: DotRouterService,
                    useValue: {
                        currentPortlet: {
                            id: '123-567'
                        },
                        reloadCurrentPortlet: jest.fn(),
                        gotoPortlet: jest
                            .fn()
                            .mockReturnValue(new Promise((resolve) => resolve(true)))
                    }
                },
                {
                    provide: DotSystemConfigService,
                    useValue: { getSystemConfig: () => of({}) }
                },
                GlobalStore,
                provideHttpClient(),
                provideHttpClientTesting()
            ],
            imports: [RouterTestingModule]
        });

        service = testbed.inject(DotNavigationService);
        dotRouterService = testbed.inject(DotRouterService);
        dotcmsEventsService = testbed.inject(DotcmsEventsService);
        dotMenuService = testbed.inject(DotMenuService);
        loginService = testbed.inject(LoginService);
        dotEventService = testbed.inject(DotEventsService);
        router = testbed.inject(Router);
        titleService = testbed.inject(Title);

        jest.spyOn(titleService, 'setTitle');
        jest.spyOn(dotEventService, 'notify');
        jest.spyOn(dotMenuService, 'reloadMenu');
        localStorage.clear();
    }));

    describe('goToFirstPortlet', () => {
        it('should go to first portlet: ', () => {
            service.goToFirstPortlet();
            expect(dotRouterService.gotoPortlet).toHaveBeenCalledWith('url/one');
            expect(dotRouterService.gotoPortlet).toHaveBeenCalledTimes(1);
        });
    });

    describe('reloadCurrentPortlet', () => {
        it('should reload current portlet', () => {
            service.reloadCurrentPortlet('123-567');
            expect(dotRouterService.reloadCurrentPortlet).toHaveBeenCalledWith('123-567');
            expect(dotRouterService.reloadCurrentPortlet).toHaveBeenCalledTimes(1);
        });

        it('should NOT reload current portlet', () => {
            service.reloadCurrentPortlet('123');
            expect(dotRouterService.reloadCurrentPortlet).not.toHaveBeenCalled();
        });
    });

    describe('closeAllSections', () => {
        it('should close all the menu sections', () => {
            let counter = 0;
            service.items$.subscribe((menus: DotMenu[]) => {
                if (counter === 0) {
                    expect(menus.map((menu: DotMenu) => menu.isOpen)).toEqual([true, false]);
                } else {
                    expect(menus.map((menu: DotMenu) => menu.isOpen)).toEqual([false, false]);
                }

                counter++;
            });
            service.collapseMenu();
        });
    });

    describe('collapseMenu', () => {
        it('should collapse menu and call closeAllSections', () => {
            expect(service.collapsed$.getValue()).toBe(true);
            jest.spyOn(service, 'closeAllSections');
            service.collapseMenu();
            expect(service.collapsed$.getValue()).toBe(true);
            expect(service.closeAllSections).toHaveBeenCalledTimes(1);
        });
    });

    describe('expandMenu', () => {
        it('should expand active menu section', () => {
            expect(service.collapsed$.getValue()).toBe(true);

            let counter = 0;
            service.items$.subscribe((menus: DotMenu[]) => {
                if (counter === 0) {
                    expect(menus.map((menu: DotMenu) => menu.isOpen)).toEqual([true, false]);
                } else {
                    expect(menus.map((menu: DotMenu) => menu.isOpen)).toEqual([false, true]);
                }

                counter++;
            });

            service.expandMenu();
            expect(service.collapsed$.getValue()).toBe(false);
        });
    });

    describe('setOpen', () => {
        it('should set isOpen attribute to expecific menu section', () => {
            let counter = 0;
            service.items$.subscribe((menus: DotMenu[]) => {
                if (counter === 0) {
                    expect(menus.map((menu: DotMenu) => menu.isOpen)).toEqual([true, false]);
                } else {
                    expect(menus.map((menu: DotMenu) => menu.isOpen)).toEqual([false, true]);
                }

                counter++;
            });

            service.setOpen('456');
        });
    });

    describe('toggle', () => {
        it('should toggle the menu', () => {
            let counter = 0;
            service.items$.pipe(skip(1)).subscribe((menus: DotMenu[]) => {
                if (counter === 0) {
                    expect(menus.map((menu: DotMenu) => menu.isOpen)).toEqual([false, true]);
                } else {
                    expect(menus.map((menu: DotMenu) => menu.isOpen)).toEqual([false, false]);
                }

                counter++;
            });

            service.toggle();
            expect(service.collapsed$.getValue()).toBe(false);

            service.toggle();
            expect(service.collapsed$.getValue()).toBe(true);

            expect(dotEventService.notify).toHaveBeenCalledTimes(2);
        });
    });

    describe('goTo', () => {
        it('should go to url', () => {
            service.goTo('hello/world');
            expect(dotRouterService.gotoPortlet).toHaveBeenCalledWith('hello/world');
            expect(dotRouterService.gotoPortlet).toHaveBeenCalledTimes(1);
        });
    });

    it('should go to first portlet on auth change', () => {
        (loginService as unknown as LoginServiceMock).triggerNewAuth(baseMockAuth);

        jest.spyOn(dotMenuService, 'loadMenu').mockReturnValue(
            of([
                {
                    active: false,
                    id: '123',
                    isOpen: false,
                    menuItems: [],
                    name: 'Nav 1',
                    tabDescription: 'Navigation 1',
                    tabIcon: 'icon',
                    tabName: 'name',
                    url: 'abc-def'
                }
            ])
        );

        expect(dotRouterService.gotoPortlet).toHaveBeenCalledWith('url/one');
        expect(dotRouterService.gotoPortlet).toHaveBeenCalledTimes(1);
    });

    it('should expand and set active menu option by url when is not collapsed', () => {
        let counter = 0;

        service.items$.subscribe((menus: DotMenu[]) => {
            if (counter === 0) {
                expect(menus[0].isOpen).toBe(true);
                expect(menus[0].menuItems[0].active).toBe(false);
            } else {
                expect(menus[1].isOpen).toBe(false);
                expect(menus[1].menuItems[0].active).toBe(false);
            }

            counter++;
        });

        router.triggerNavigationEnd('/123');
    });

    it('should set Page title based on url', () => {
        router.triggerNavigationEnd('url/link1');
        expect(titleService.setTitle).toHaveBeenCalledWith('Label 1 - dotCMS platform');
        expect(titleService.setTitle).toHaveBeenCalledTimes(1);
    });

    // TODO: needs to fix this, looks like the dotcmsEventsService instance is different here not sure why.
    xit('should subscribe to UPDATE_PORTLET_LAYOUTS websocket event', () => {
        expect(dotcmsEventsService.subscribeTo).toHaveBeenCalledWith('UPDATE_PORTLET_LAYOUTS');
        expect(dotcmsEventsService.subscribeTo).toHaveBeenCalledTimes(1);
    });
});
