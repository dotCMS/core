import { DOTTestBed } from './../../../test/dot-test-bed';
import { of as observableOf } from 'rxjs';
import { TestBed } from '@angular/core/testing';
import { Injectable } from '@angular/core';
import { DotMenuService } from '../dot-menu.service';
import { DotNavigationService } from '@components/dot-navigation/services/dot-navigation.service';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';

import { MenuGuardService } from './menu-guard.service';

@Injectable()
class MockDotMenuService {
    isPortletInMenu() {}
}

@Injectable()
class MockDotNavigationService {
    goToFirstPortlet = jasmine.createSpy('goToFirstPortlet');
}

describe('ValidMenuGuardService', () => {
    let menuGuardService: MenuGuardService;
    let dotMenuService: DotMenuService;
    let dotNavigationService: DotNavigationService;
    let mockRouterStateSnapshot: RouterStateSnapshot;
    let mockActivatedRouteSnapshot: ActivatedRouteSnapshot;

    beforeEach(() => {
        DOTTestBed.configureTestingModule({
            providers: [
                MenuGuardService,
                { provide: DotMenuService, useClass: MockDotMenuService },
                {
                    provide: DotNavigationService,
                    useClass: MockDotNavigationService
                }
            ]
        });

        menuGuardService = TestBed.get(MenuGuardService);
        dotMenuService = TestBed.get(DotMenuService);
        dotNavigationService = TestBed.get(DotNavigationService);
        mockRouterStateSnapshot = jasmine.createSpyObj<RouterStateSnapshot>('RouterStateSnapshot', [
            'toString'
        ]);
        mockActivatedRouteSnapshot = jasmine.createSpyObj<ActivatedRouteSnapshot>(
            'ActivatedRouteSnapshot',
            ['toString']
        );
    });

    it('should allow access to Menu Portlets', () => {
        let result: boolean;
        mockRouterStateSnapshot.url = '/test';
        spyOn(dotMenuService, 'isPortletInMenu').and.returnValue(observableOf(true));
        menuGuardService
            .canActivate(mockActivatedRouteSnapshot, mockRouterStateSnapshot)
            .subscribe((res) => (result = res));
        expect(dotMenuService.isPortletInMenu).toHaveBeenCalledWith('test');
        expect(result).toBe(true);
    });

    it('should prevent access to Menu Portlets', () => {
        let result: boolean;
        mockRouterStateSnapshot.url = '/test';
        spyOn(dotMenuService, 'isPortletInMenu').and.returnValue(observableOf(false));
        menuGuardService
            .canActivate(mockActivatedRouteSnapshot, mockRouterStateSnapshot)
            .subscribe((res) => (result = res));
        expect(dotMenuService.isPortletInMenu).toHaveBeenCalledWith('test');
        expect(dotNavigationService.goToFirstPortlet).toHaveBeenCalled();
        expect(result).toBe(false);
    });

    it('should allow children access to Menu Portlets', () => {
        let result: boolean;
        mockRouterStateSnapshot.url = '/test';
        spyOn(dotMenuService, 'isPortletInMenu').and.returnValue(observableOf(true));
        menuGuardService
            .canActivateChild(mockActivatedRouteSnapshot, mockRouterStateSnapshot)
            .subscribe((res) => (result = res));
        expect(dotMenuService.isPortletInMenu).toHaveBeenCalledWith('test');
        expect(result).toBe(true);
    });

    it('should prevent children access to Menu Portlets', () => {
        let result: boolean;
        mockRouterStateSnapshot.url = '/test';
        spyOn(dotMenuService, 'isPortletInMenu').and.returnValue(observableOf(false));
        menuGuardService
            .canActivateChild(mockActivatedRouteSnapshot, mockRouterStateSnapshot)
            .subscribe((res) => (result = res));
        expect(dotMenuService.isPortletInMenu).toHaveBeenCalledWith('test');
        expect(dotNavigationService.goToFirstPortlet).toHaveBeenCalled();
        expect(result).toBe(false);
    });
});
