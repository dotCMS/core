import { async } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { Router, NavigationEnd } from '@angular/router';

import { LoginServiceMock } from '../../../../test/login-service.mock';
import { DOTTestBed } from '../../../../test/dot-test-bed';
import { DotEventsService } from '@services/dot-events/dot-events.service';
import { DotMenu } from '@models/navigation';
import { DotMenuService } from '@services/dot-menu.service';
import { DotNavigationService } from './dot-navigation.service';
import { DotRouterService } from '@services/dot-router/dot-router.service';

import { DotcmsEventsService, LoginService, Auth } from 'dotcms-js';

import { Observable, Subject, of } from 'rxjs';
import { skip } from 'rxjs/operators';

class RouterMock {
    _events: Subject<any> = new Subject();
    _routerState: any;

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

const baseMockUser = {
    emailAddress: 'admin@dotcms.com',
    firstName: 'Admin',
    lastName: 'Admin',
    loggedInDate: 123456789,
    userId: '123'
};
const baseMockAuth: Auth = {
    loginAsUser: null,
    user: baseMockUser
};

describe('DotNavigationService', () => {
    let service: DotNavigationService;
    let dotRouterService: DotRouterService;
    let dotcmsEventsService: DotcmsEventsService;
    let dotEventService: DotEventsService;
    let dotMenuService: DotMenuService;
    let loginService: LoginServiceMock;
    let router;

    beforeEach(
        async(() => {
            const testbed = DOTTestBed.configureTestingModule({
                providers: [
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
                    }
                ],
                imports: [RouterTestingModule]
            });

            service = testbed.get(DotNavigationService);
            dotRouterService = testbed.get(DotRouterService);
            dotcmsEventsService = testbed.get(DotcmsEventsService);
            dotMenuService = testbed.get(DotMenuService);
            loginService = testbed.get(LoginService);
            dotEventService = testbed.get(DotEventsService);
            router = testbed.get(Router);

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
        spyOn(service, 'goToFirstPortlet');
        loginService.triggerNewAuth(baseMockAuth);
        expect(service.goToFirstPortlet).toHaveBeenCalledTimes(1);
        expect(dotMenuService.reloadMenu).toHaveBeenCalledTimes(1);
    });

    it('should expand and set active menu option by url when is not collapsed', () => {
        let counter = 0;

        service.items$.subscribe((menus: DotMenu[]) => {
            if (counter === 0) {
                expect(menus[1].isOpen).toBe(false);
                expect(menus[1].menuItems[0].active).toBe(true);
            } else {
                expect(menus[1].isOpen).toBe(false);
                expect(menus[1].menuItems[0].active).toBe(true);
            }
            counter++;
        });

        router.triggerNavigationEnd('/789');
    });

    // TODO: needs to fix this, looks like the dotcmsEventsService instance is different here not sure why.
    xit('should subscribe to UPDATE_PORTLET_LAYOUTS websocket event', () => {
        expect(dotcmsEventsService.subscribeTo).toHaveBeenCalledWith('UPDATE_PORTLET_LAYOUTS');
    });
});
