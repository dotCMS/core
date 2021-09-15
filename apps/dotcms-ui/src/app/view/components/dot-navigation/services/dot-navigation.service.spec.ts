import { TestBed, waitForAsync } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { Router, NavigationEnd } from '@angular/router';

import { LoginServiceMock } from '@dotcms/app/test/login-service.mock';
import { DotEventsService } from '@services/dot-events/dot-events.service';
import { DotMenu } from '@models/navigation';
import { DotMenuService } from '@services/dot-menu.service';
import { DotNavigationService } from './dot-navigation.service';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { DotIframeService } from '../../_common/iframe/service/dot-iframe/dot-iframe.service';
import { DotcmsEventsService, LoginService, Auth } from '@dotcms/dotcms-js';

import { Observable, Subject, of } from 'rxjs';
import { skip } from 'rxjs/operators';

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

    navigateByUrl() {}
}
class DotMenuServiceMock {
    loadMenu(): Observable<DotMenu[]> {
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

    beforeEach(
        waitForAsync(() => {
            const testbed = TestBed.configureTestingModule({
                providers: [
                    DotEventsService,
                    DotNavigationService,
                    {
                        provide: DotcmsEventsService,
                        useClass: DotcmsEventsServiceMock
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
                            reload: jasmine.createSpy()
                        }
                    },
                    {
                        provide: DotRouterService,
                        useValue: {
                            currentPortlet: {
                                id: '123-567'
                            },
                            reloadCurrentPortlet: jasmine.createSpy(),
                            gotoPortlet: jasmine
                                .createSpy()
                                .and.returnValue(new Promise((resolve) => resolve(true)))
                        }
                    }
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

            spyOn(dotEventService, 'notify');
            spyOn(dotMenuService, 'reloadMenu').and.callThrough();
            localStorage.clear();
        })
    );

    describe('goToFirstPortlet', () => {
        it('should go to first portlet: ', () => {
            service.goToFirstPortlet();
            expect(dotRouterService.gotoPortlet).toHaveBeenCalledWith('url/one');
        });
    });

    describe('reloadCurrentPortlet', () => {
        it('should reload current portlet', () => {
            service.reloadCurrentPortlet('123-567');
            expect(dotRouterService.reloadCurrentPortlet).toHaveBeenCalledWith('123-567');
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
            spyOn(service, 'closeAllSections');
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
        });
    });

    it('should go to first portlet on auth change', () => {
        ((loginService as unknown) as LoginServiceMock).triggerNewAuth(baseMockAuth);

        spyOn(dotMenuService, 'loadMenu').and.returnValue(
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

    // TODO: needs to fix this, looks like the dotcmsEventsService instance is different here not sure why.
    xit('should subscribe to UPDATE_PORTLET_LAYOUTS websocket event', () => {
        expect(dotcmsEventsService.subscribeTo).toHaveBeenCalledWith('UPDATE_PORTLET_LAYOUTS');
    });
});
