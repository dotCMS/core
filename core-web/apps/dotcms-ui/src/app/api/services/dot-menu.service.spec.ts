import { HttpMethod, SpectatorHttp, createHttpFactory } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { DotMenu } from '@dotcms/dotcms-models';

import { DotMenuService } from './dot-menu.service';

const STATER_ANGULAR_ITEM_MOCK = {
    ajax: false,
    angular: true,
    id: 'starter',
    label: 'Welcome',
    url: '/starter',
    menuLink: '/starter',
    active: false
};

const ACTIVITY_JSP_ITEM_MOCK = {
    ajax: false,
    angular: false,
    id: 'c_Activities',
    label: 'Activities',
    url: '/c/portlet/c_Activities',
    menuLink: '/c/c_Activities',
    active: false
};

const ACTIVITY_MENU_ID = '71b8a1ca-37b6-4b6e-a43b-c7482f28db6c';

const MENU_MOCK: DotMenu[] = [
    {
        id: '2df9f117-b140-44bf-93d7-5b10a36fb7f9',
        menuItems: [STATER_ANGULAR_ITEM_MOCK],
        name: 'Getting Started',
        tabIcon: 'whatshot',
        tabName: 'Getting Started',
        url: '/starter',
        active: false
    } as DotMenu,
    {
        id: ACTIVITY_MENU_ID,
        menuItems: [
            ACTIVITY_JSP_ITEM_MOCK,
            {
                ajax: false,
                angular: false,
                id: 'c_Banners',
                label: 'Banners',
                url: '/c/portlet/c_Banners',
                menuLink: '/c/c_Banners',
                active: false
            }
        ],
        name: 'Content',
        tabIcon: 'format_align_left',
        tabName: 'Content',
        url: '/c/portal/layout?p_l_id=71b8a1ca-37b6-4b6e-a43b-c7482f28db6c&p_p_id=c_Activities&p_p_action=0&&dm_rlout=1&r=1698954909392',
        active: false
    } as DotMenu
];

describe('DotMenuService', () => {
    let spectator: SpectatorHttp<DotMenuService>;
    const createHttp = createHttpFactory(DotMenuService);

    beforeEach(() => (spectator = createHttp()));

    describe('loadMenu', () => {
        it('should load menu', (done) => {
            spectator.service.loadMenu().subscribe((menu) => {
                expect(menu).toEqual(MENU_MOCK);
                done();
            });
            spectator.expectOne('/api/v1/menu', HttpMethod.GET).flush({
                entity: MENU_MOCK
            });
        });

        it('should not load menu if already loaded', () => {
            spectator.service.menu$ = of(MENU_MOCK);

            spectator.service.loadMenu().subscribe(() => {
                spectator.service.loadMenu().subscribe((menu) => {
                    expect(menu).toEqual(MENU_MOCK);
                });
            });
            spectator.controller.expectNone('/api/v1/menu');
        });

        it('should load menu if the param `force` is passed even if it is already loaded', () => {
            spectator.service.menu$ = of([MENU_MOCK[0]]);

            spectator.service.loadMenu(true).subscribe(() => {
                spectator.service.loadMenu().subscribe((menu) => {
                    expect(menu).toEqual(MENU_MOCK);
                });
            });
            spectator.expectOne('/api/v1/menu', HttpMethod.GET).flush({
                entity: MENU_MOCK
            });
        });
    });

    describe('getUrlById', () => {
        it('should get URL of JSP menu item', (done) => {
            jest.spyOn(spectator.service, 'loadMenu').mockReturnValue(of(MENU_MOCK));

            spectator.service.getUrlById(ACTIVITY_JSP_ITEM_MOCK.id).subscribe((url) => {
                expect(url).toEqual(ACTIVITY_JSP_ITEM_MOCK.url);
                done();
            });
        });

        it('should not return anything if the menu item is an Angular URL', (done) => {
            jest.spyOn(spectator.service, 'loadMenu').mockReturnValue(of(MENU_MOCK));

            spectator.service.getUrlById(STATER_ANGULAR_ITEM_MOCK.id).subscribe((url) => {
                expect(url).toBe('');
                done();
            });
        });
    });

    describe('isPortletInMenu', () => {
        it('should return true if the menu item is in the menu', (done) => {
            jest.spyOn(spectator.service, 'loadMenu').mockReturnValue(of(MENU_MOCK));

            spectator.service.isPortletInMenu(ACTIVITY_JSP_ITEM_MOCK.id).subscribe((isInMenu) => {
                expect(isInMenu).toBe(true);
                done();
            });
        });

        it('should return false if the menu item is not in the menu', (done) => {
            jest.spyOn(spectator.service, 'loadMenu').mockReturnValue(of(MENU_MOCK));

            spectator.service.isPortletInMenu(ACTIVITY_JSP_ITEM_MOCK.id).subscribe((isInMenu) => {
                expect(isInMenu).toBe(true);
                done();
            });
        });

        describe('JSPPortlet', () => {
            it('should return true if the menu item is in the menu and is a JSP Portlet', (done) => {
                jest.spyOn(spectator.service, 'loadMenu').mockReturnValue(of(MENU_MOCK));

                spectator.service
                    .isPortletInMenu(ACTIVITY_JSP_ITEM_MOCK.id, true)
                    .subscribe((isInMenu) => {
                        expect(isInMenu).toBe(true);
                        done();
                    });
            });

            it('should return false if the menu item is not in the menu and is a JSP Portlet', (done) => {
                jest.spyOn(spectator.service, 'loadMenu').mockReturnValue(of(MENU_MOCK));

                spectator.service
                    .isPortletInMenu(STATER_ANGULAR_ITEM_MOCK.id, true)
                    .subscribe((isInMenu) => {
                        expect(isInMenu).toBe(false);
                        done();
                    });
            });
        });
    });

    describe('reloadMenu', () => {
        it('should set menu items to null and force reaload', (done) => {
            spectator.service.menu$ = of([MENU_MOCK[0]]);

            spectator.service.reloadMenu().subscribe((menu) => {
                expect(menu).toEqual(MENU_MOCK);
                done();
            });

            spectator.expectOne('/api/v1/menu', HttpMethod.GET).flush({
                entity: MENU_MOCK
            });
        });
    });

    describe('getDotMenuId', () => {
        it('should return the id of the menu', (done) => {
            jest.spyOn(spectator.service, 'loadMenu').mockReturnValue(of(MENU_MOCK));

            spectator.service.getDotMenuId(ACTIVITY_JSP_ITEM_MOCK.id).subscribe((id) => {
                expect(id).toBe(ACTIVITY_MENU_ID);
                done();
            });
        });
    });

    afterEach(() => spectator.controller.verify());
});
