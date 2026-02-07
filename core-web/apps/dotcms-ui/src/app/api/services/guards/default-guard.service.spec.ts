import { of } from 'rxjs';

import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot } from '@angular/router';

import { DotRouterService } from '@dotcms/data-access';

import { DefaultGuardService } from './default-guard.service';

import { DOTTestBed } from '../../../test/dot-test-bed';
import { DotMenuService } from '../dot-menu.service';
import { DynamicRouteService } from '../dynamic-route.service';

describe('DefaultGuardService', () => {
    let guard: DefaultGuardService;
    let dotRouterService: DotRouterService;
    let router: Router;
    let dynamicRouteService: DynamicRouteService;
    let dotMenuService: DotMenuService;

    const mockMenus = [
        {
            id: 'menu-1',
            name: 'Test Menu',
            tabDescription: '',
            tabName: '',
            tabOrder: 0,
            url: '',
            menuItems: [
                {
                    id: 'my-dynamic-portlet',
                    label: 'Dynamic Portlet',
                    url: '/my-dynamic-portlet',
                    ajax: false,
                    angular: true,
                    angularModule: 'remote:http://localhost:4201/remoteEntry.js|myPlugin|./Routes'
                }
            ]
        }
    ];

    const mockRoute = {} as ActivatedRouteSnapshot;

    beforeEach(() => {
        DOTTestBed.configureTestingModule({
            providers: [
                DefaultGuardService,
                {
                    provide: DotMenuService,
                    useValue: {
                        loadMenu: jest.fn().mockReturnValue(of(mockMenus))
                    }
                },
                {
                    provide: DynamicRouteService,
                    useValue: {
                        registerRoutesFromMenuItems: jest.fn().mockReturnValue(1),
                        isRouteRegistered: jest.fn()
                    }
                }
            ]
        });

        guard = TestBed.inject(DefaultGuardService);
        dotRouterService = TestBed.inject(DotRouterService);
        router = TestBed.inject(Router);
        dynamicRouteService = TestBed.inject(DynamicRouteService);
        dotMenuService = TestBed.inject(DotMenuService);

        jest.spyOn(router, 'navigateByUrl').mockImplementation();
    });

    it('should redirect to dynamic route when URL matches a registered dynamic route', (done) => {
        (dynamicRouteService.isRouteRegistered as jest.Mock).mockReturnValue(true);
        const state = { url: '/my-dynamic-portlet' } as RouterStateSnapshot;

        guard.canActivate(mockRoute, state).subscribe((result) => {
            expect(dotMenuService.loadMenu).toHaveBeenCalled();
            expect(dynamicRouteService.registerRoutesFromMenuItems).toHaveBeenCalledWith(
                mockMenus[0].menuItems
            );
            expect(dynamicRouteService.isRouteRegistered).toHaveBeenCalledWith(
                'my-dynamic-portlet'
            );
            expect(router.navigateByUrl).toHaveBeenCalledWith('/my-dynamic-portlet');
            expect(dotRouterService.goToMain).not.toHaveBeenCalled();
            expect(result).toBe(false);
            done();
        });
    });

    it('should redirect to main when URL does not match a dynamic route', (done) => {
        (dynamicRouteService.isRouteRegistered as jest.Mock).mockReturnValue(false);
        const state = { url: '/some-unknown-path' } as RouterStateSnapshot;

        guard.canActivate(mockRoute, state).subscribe((result) => {
            expect(dotMenuService.loadMenu).toHaveBeenCalled();
            expect(dynamicRouteService.registerRoutesFromMenuItems).toHaveBeenCalled();
            expect(dynamicRouteService.isRouteRegistered).toHaveBeenCalledWith('some-unknown-path');
            expect(router.navigateByUrl).not.toHaveBeenCalled();
            expect(dotRouterService.goToMain).toHaveBeenCalled();
            expect(result).toBe(true);
            done();
        });
    });
});
