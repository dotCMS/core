import { Auth } from 'dotcms-js/core/login.service';
import { DOTTestBed } from '../../../../test/dot-test-bed';
import { DotMenu } from '../../../../shared/models/navigation';
import { DotMenuService } from '../../../../api/services/dot-menu.service';
import { DotNavigationService } from './dot-navigation.service';
import { DotRouterService } from '../../../../api/services/dot-router/dot-router.service';
import { DotcmsEventsService, LoginService } from 'dotcms-js/dotcms-js';
import { LoginServiceMock } from '../../../../test/login-service.mock';
import { Observable } from 'rxjs/Observable';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { async } from '@angular/core/testing';
import { of } from 'rxjs/observable/of';
import { Subject } from 'rxjs';

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
    let loginService: LoginServiceMock;

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
                        useValue: {
                            routerState: {
                                snapshot: {
                                    url: 'hello/world'
                                }
                            },
                            events: Observable.of({})
                        }
                    }
                ],
                imports: [RouterTestingModule]
            });

            service = testbed.get(DotNavigationService);
            dotRouterService = testbed.get(DotRouterService);
            dotcmsEventsService = testbed.get(DotcmsEventsService);
            loginService = testbed.get(LoginService);

            spyOn(dotRouterService, 'gotoPortlet').and.callFake(() => new Promise((resolve) => resolve(true)));
            spyOn(dotRouterService, 'reloadCurrentPortlet');
            // spyOn(dotcmsEventsService, 'subscribeTo').and.callThrough();
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
            service.reloadCurrentPortlet('hello');
            expect(dotRouterService.reloadCurrentPortlet).toHaveBeenCalledWith('hello');
        });

        it('should NOT reload current portlet', () => {
            service.reloadCurrentPortlet('123');
            expect(dotRouterService.reloadCurrentPortlet).not.toHaveBeenCalled();
        });
    });

    describe('collapseMenu', () => {
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

    describe('expandMenu', () => {
        it('should expand active menu section', () => {
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
        });
    });

    describe('setOpen', () => {
        it('should expand expecific menu section', () => {
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

    describe('goTo', () => {
        it ('should go to url', () => {
            service.goTo('hello/world');
            expect(dotRouterService.gotoPortlet).toHaveBeenCalledWith('hello/world');
        });
    });


    it('should go to first portlet on auth change', () => {
        spyOn(service, 'goToFirstPortlet');
        loginService.triggerNewAuth(baseMockAuth);
        expect(service.goToFirstPortlet).toHaveBeenCalledTimes(1);
    });

    // TODO: needs to fix this, looks like the dotcmsEventsService instance is different here not sure why.
    xit('should subscribe to UPDATE_PORTLET_LAYOUTS websocket event', () => {
        expect(dotcmsEventsService.subscribeTo).toHaveBeenCalledWith('UPDATE_PORTLET_LAYOUTS');
    });
});
