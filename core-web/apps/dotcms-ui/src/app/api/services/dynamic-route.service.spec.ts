import { createServiceFactory, SpectatorService } from '@ngneat/spectator/jest';

import { Component } from '@angular/core';
import { Route, Router } from '@angular/router';

import { LoggerService } from '@dotcms/dotcms-js';

import { DynamicRouteService } from './dynamic-route.service';
import { MenuGuardService } from './guards/menu-guard.service';

@Component({ template: '' })
class MockComponent {}

describe('DynamicRouteService', () => {
    let spectator: SpectatorService<DynamicRouteService>;
    let service: DynamicRouteService;
    let router: Router;

    const mockMainRoute: Route = {
        path: '',
        canActivate: [{ name: 'AuthGuardService' } as never],
        children: []
    };

    const createService = createServiceFactory({
        service: DynamicRouteService,
        mocks: [LoggerService],
        providers: [
            {
                provide: Router,
                useValue: {
                    config: [mockMainRoute],
                    resetConfig: jest.fn()
                }
            }
        ]
    });

    beforeEach(() => {
        spectator = createService();
        service = spectator.service;
        router = spectator.inject(Router);

        // Reset children array before each test
        mockMainRoute.children = [];
    });

    describe('registerRoute', () => {
        it('should register a route with a component', () => {
            const result = service.registerRoute({
                path: 'test-portlet',
                component: MockComponent
            });

            expect(result).toBe(true);
            expect(mockMainRoute.children).toHaveLength(1);
            expect(mockMainRoute.children![0].path).toBe('test-portlet');
            expect(mockMainRoute.children![0].component).toBe(MockComponent);
        });

        it('should register a route with loadComponent', () => {
            const loadFn = () => Promise.resolve(MockComponent);

            const result = service.registerRoute({
                path: 'lazy-portlet',
                loadComponent: loadFn
            });

            expect(result).toBe(true);
            expect(mockMainRoute.children![0].loadComponent).toBe(loadFn);
        });

        it('should add MenuGuardService by default', () => {
            service.registerRoute({
                path: 'guarded-portlet',
                component: MockComponent
            });

            expect(mockMainRoute.children![0].canActivate).toContain(MenuGuardService);
            expect(mockMainRoute.children![0].canActivateChild).toContain(MenuGuardService);
        });

        it('should not add guards when canActivate is false', () => {
            service.registerRoute({
                path: 'unguarded-portlet',
                component: MockComponent,
                canActivate: false
            });

            expect(mockMainRoute.children![0].canActivate).toBeUndefined();
        });

        it('should not register duplicate routes', () => {
            service.registerRoute({
                path: 'duplicate-portlet',
                component: MockComponent
            });

            const result = service.registerRoute({
                path: 'duplicate-portlet',
                component: MockComponent
            });

            expect(result).toBe(false);
            expect(mockMainRoute.children).toHaveLength(1);
        });

        it('should reset router config after registration', () => {
            service.registerRoute({
                path: 'test-portlet',
                component: MockComponent
            });

            expect(router.resetConfig).toHaveBeenCalledWith(router.config);
        });

        it('should include custom data in route', () => {
            service.registerRoute({
                path: 'data-portlet',
                component: MockComponent,
                data: { customKey: 'customValue' }
            });

            expect(mockMainRoute.children![0].data).toEqual({
                reuseRoute: false,
                customKey: 'customValue'
            });
        });
    });

    describe('unregisterRoute', () => {
        beforeEach(() => {
            service.registerRoute({
                path: 'removable-portlet',
                component: MockComponent
            });
        });

        it('should remove a registered route', () => {
            const result = service.unregisterRoute('removable-portlet');

            expect(result).toBe(true);
            expect(mockMainRoute.children).toHaveLength(0);
        });

        it('should return false for non-existent routes', () => {
            const result = service.unregisterRoute('non-existent');

            expect(result).toBe(false);
        });

        it('should reset router config after unregistration', () => {
            jest.clearAllMocks();
            service.unregisterRoute('removable-portlet');

            expect(router.resetConfig).toHaveBeenCalled();
        });
    });

    describe('isRouteRegistered', () => {
        it('should return true for registered routes', () => {
            service.registerRoute({
                path: 'check-portlet',
                component: MockComponent
            });

            expect(service.isRouteRegistered('check-portlet')).toBe(true);
        });

        it('should return false for unregistered routes', () => {
            expect(service.isRouteRegistered('unknown-portlet')).toBe(false);
        });
    });

    describe('getRegisteredRoutes', () => {
        it('should return all registered route paths', () => {
            service.registerRoute({ path: 'portlet-1', component: MockComponent });
            service.registerRoute({ path: 'portlet-2', component: MockComponent });

            const routes = service.getRegisteredRoutes();

            expect(routes).toContain('portlet-1');
            expect(routes).toContain('portlet-2');
            expect(routes).toHaveLength(2);
        });

        it('should return empty array when no routes registered', () => {
            expect(service.getRegisteredRoutes()).toEqual([]);
        });
    });
});
