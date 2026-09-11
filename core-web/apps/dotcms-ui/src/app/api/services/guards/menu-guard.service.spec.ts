import { mockProvider } from '@openng/spectator/vitest';
import { of as observableOf } from 'rxjs';
import { vi } from 'vitest';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Injectable } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';

import { DotRouterService, DotSessionStorageService } from '@dotcms/data-access';

import { MenuGuardService } from './menu-guard.service';

import { DotNavigationService } from '../../../view/components/dot-navigation/services/dot-navigation.service';
import { DotMenuService } from '../dot-menu.service';

@Injectable()
class MockDotMenuService {
    isPortletInMenu() {
        //
    }
}

@Injectable()
class MockDotNavigationService {
    goToFirstPortlet = vi.fn();
}

describe('ValidMenuGuardService', () => {
    let menuGuardService: MenuGuardService;
    let dotMenuService: DotMenuService;
    let dotNavigationService: DotNavigationService;
    let dotRouterService: DotRouterService;
    let mockRouterStateSnapshot: RouterStateSnapshot;
    let mockActivatedRouteSnapshot: ActivatedRouteSnapshot;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [
                MenuGuardService,
                {
                    provide: DotRouterService,
                    useValue: {
                        getPortletId: () => 'test',
                        isJSPPortletURL: () => false
                    }
                },
                { provide: DotMenuService, useClass: MockDotMenuService },
                {
                    provide: DotNavigationService,
                    useClass: MockDotNavigationService
                },
                mockProvider(DotSessionStorageService)
            ]
        });

        menuGuardService = TestBed.inject(MenuGuardService);
        dotMenuService = TestBed.inject(DotMenuService);
        dotRouterService = TestBed.inject(DotRouterService);
        dotNavigationService = TestBed.inject(DotNavigationService);
        mockRouterStateSnapshot = { toString: vi.fn() } as unknown as RouterStateSnapshot;
        mockActivatedRouteSnapshot = { toString: vi.fn() } as unknown as ActivatedRouteSnapshot;
    });

    it('should allow access to Menu Portlets', () => {
        let result: boolean;
        mockRouterStateSnapshot.url = '/test';
        vi.spyOn(dotMenuService, 'isPortletInMenu').mockReturnValue(observableOf(true));
        menuGuardService
            .canActivate(mockActivatedRouteSnapshot, mockRouterStateSnapshot)
            .subscribe((res) => (result = res));
        expect(dotMenuService.isPortletInMenu).toHaveBeenCalledWith('test', false);
        expect(dotMenuService.isPortletInMenu).toHaveBeenCalledTimes(1);
        expect(result).toBe(true);
    });

    it('should prevent access to Menu Portlets', () => {
        let result: boolean;
        mockRouterStateSnapshot.url = '/test';
        vi.spyOn(dotMenuService, 'isPortletInMenu').mockReturnValue(observableOf(false));
        menuGuardService
            .canActivate(mockActivatedRouteSnapshot, mockRouterStateSnapshot)
            .subscribe((res) => (result = res));
        expect(dotMenuService.isPortletInMenu).toHaveBeenCalledWith('test', false);
        expect(dotMenuService.isPortletInMenu).toHaveBeenCalledTimes(1);
        expect(dotNavigationService.goToFirstPortlet).toHaveBeenCalled();
        expect(result).toBe(false);
    });

    it('should allow children access to Menu Portlets', () => {
        let result: boolean;
        mockRouterStateSnapshot.url = '/test';
        vi.spyOn(dotMenuService, 'isPortletInMenu').mockReturnValue(observableOf(true));
        menuGuardService
            .canActivateChild(mockActivatedRouteSnapshot, mockRouterStateSnapshot)
            .subscribe((res) => (result = res));
        expect(dotMenuService.isPortletInMenu).toHaveBeenCalledWith('test', false);
        expect(dotMenuService.isPortletInMenu).toHaveBeenCalledTimes(1);
        expect(result).toBe(true);
    });

    it('should prevent children access to Menu Portlets', () => {
        let result: boolean;
        mockRouterStateSnapshot.url = '/test';
        vi.spyOn(dotMenuService, 'isPortletInMenu').mockReturnValue(observableOf(false));
        menuGuardService
            .canActivateChild(mockActivatedRouteSnapshot, mockRouterStateSnapshot)
            .subscribe((res) => (result = res));
        expect(dotMenuService.isPortletInMenu).toHaveBeenCalledWith('test', false);
        expect(dotMenuService.isPortletInMenu).toHaveBeenCalledTimes(1);
        expect(dotNavigationService.goToFirstPortlet).toHaveBeenCalled();
        expect(result).toBe(false);
    });

    describe('JSPPortlet', () => {
        beforeEach(() => {
            vi.spyOn(dotRouterService, 'isJSPPortletURL').mockReturnValue(true);
            mockRouterStateSnapshot.url = '/c/test';
        });

        it('should allow children access to Menu Portlets if JSPPortlet is in menu', () =>
            new Promise<void>((done) => {
                const spy = vi
                    .spyOn(dotMenuService, 'isPortletInMenu')
                    .mockReturnValue(observableOf(true));
                menuGuardService
                    .canActivateChild(mockActivatedRouteSnapshot, mockRouterStateSnapshot)
                    .subscribe((res) => {
                        expect(res).toBe(true);
                        done();
                    });
                expect(spy).toHaveBeenCalledWith('test', true);
                expect(spy).toHaveBeenCalledTimes(1);
                expect(dotNavigationService.goToFirstPortlet).not.toHaveBeenCalled();
            }));

        it('should prevent children access to Menu Portlets if JSPPortlet is in menu', () =>
            new Promise<void>((done) => {
                const spy = vi
                    .spyOn(dotMenuService, 'isPortletInMenu')
                    .mockReturnValue(observableOf(false));
                menuGuardService
                    .canActivateChild(mockActivatedRouteSnapshot, mockRouterStateSnapshot)
                    .subscribe((res) => {
                        expect(res).toBe(false);
                        done();
                    });
                expect(spy).toHaveBeenCalledWith('test', true);
                expect(spy).toHaveBeenCalledTimes(1);
                expect(dotNavigationService.goToFirstPortlet).toHaveBeenCalled();
            }));
    });
});
