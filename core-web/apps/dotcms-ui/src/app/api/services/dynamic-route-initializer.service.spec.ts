import { of, throwError } from 'rxjs';

import { TestBed } from '@angular/core/testing';

import { LoggerService } from '@dotcms/dotcms-js';

import { DotMenuService } from './dot-menu.service';
import { DynamicRouteInitializerService } from './dynamic-route-initializer.service';
import { DynamicRouteService } from './dynamic-route.service';

describe('DynamicRouteInitializerService', () => {
    let service: DynamicRouteInitializerService;
    let menuService: DotMenuService;
    let dynamicRouteService: DynamicRouteService;

    const mockMenus = [
        {
            id: 'menu-1',
            name: 'Menu',
            tabDescription: '',
            tabName: '',
            tabOrder: 0,
            url: '',
            menuItems: [
                {
                    id: 'portlet-1',
                    label: 'Portlet',
                    url: '/portlet-1',
                    ajax: false,
                    angular: true,
                    angularModule: 'remote:http://localhost:4201/remoteEntry.js|p|./Routes'
                }
            ]
        }
    ];

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                DynamicRouteInitializerService,
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
                        getRegisteredRoutes: jest.fn().mockReturnValue(['portlet-1'])
                    }
                },
                {
                    provide: LoggerService,
                    useValue: {
                        info: jest.fn(),
                        error: jest.fn(),
                        warn: jest.fn()
                    }
                }
            ]
        });

        service = TestBed.inject(DynamicRouteInitializerService);
        menuService = TestBed.inject(DotMenuService);
        dynamicRouteService = TestBed.inject(DynamicRouteService);
    });

    it('should register routes on first initialization', async () => {
        const count = await service.initialize();

        expect(menuService.loadMenu).toHaveBeenCalledWith(false);
        expect(dynamicRouteService.registerRoutesFromMenuItems).toHaveBeenCalledWith(
            mockMenus[0].menuItems
        );
        expect(count).toBe(1);
        expect(service.isInitialized()).toBe(true);
    });

    it('should be a no-op on repeated calls without force', async () => {
        await service.initialize();
        jest.clearAllMocks();

        const count = await service.initialize();

        expect(menuService.loadMenu).not.toHaveBeenCalled();
        expect(dynamicRouteService.registerRoutesFromMenuItems).not.toHaveBeenCalled();
        expect(count).toBe(0);
    });

    it('should re-initialize when force=true', async () => {
        await service.initialize();
        jest.clearAllMocks();

        (dynamicRouteService.registerRoutesFromMenuItems as jest.Mock).mockReturnValue(2);
        const count = await service.initialize(true);

        expect(menuService.loadMenu).toHaveBeenCalledWith(true);
        expect(dynamicRouteService.registerRoutesFromMenuItems).toHaveBeenCalled();
        expect(count).toBe(2);
    });

    it('should resolve to 0 on error', async () => {
        (menuService.loadMenu as jest.Mock).mockReturnValue(throwError(() => new Error('fail')));

        const count = await service.initialize();

        expect(count).toBe(0);
        expect(service.isInitialized()).toBe(false);
    });
});
