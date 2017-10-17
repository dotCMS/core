import { TestBed } from '@angular/core/testing';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Observable';
import { DotMenuService } from '../dot-menu.service';
import { DotRouterService } from '../dot-router-service';
import { DotNavigationService } from '../../../view/components/dot-navigation/dot-navigation.service';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';

import { MenuGuardService } from './menu-guard.service';

@Injectable()
class MockDotMenuService {
    isPortletInMenu() {}
}

@Injectable()
class MockDotRouterService {
    getPortletId = jasmine.createSpy('getPortletId').and.returnValue('test');
}

@Injectable()
class MockDotNavigationService {
    goToFirstPortlet = jasmine.createSpy('goToFirstPortlet');
}

describe('ValidMenuGuardService', () => {
    let menuGuardService: MenuGuardService;
    let dotMenuService: DotMenuService;
    let dotRouterService: DotRouterService;
    let dotNavigationService: DotNavigationService;
    let mockRouterStateSnapshot: RouterStateSnapshot;
    let mockActivatedRouteSnapshot: ActivatedRouteSnapshot;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                MenuGuardService,
                { provide: DotMenuService, useClass: MockDotMenuService },
                { provide: DotRouterService, useClass: MockDotRouterService },
                {
                    provide: DotNavigationService,
                    useClass: MockDotNavigationService
                }
            ]
        });

        menuGuardService = TestBed.get(MenuGuardService);
        dotMenuService = TestBed.get(DotMenuService);
        dotRouterService = TestBed.get(DotRouterService);
        dotNavigationService = TestBed.get(DotNavigationService);
        mockRouterStateSnapshot = jasmine.createSpyObj<RouterStateSnapshot>('RouterStateSnapshot', ['toString']);
        mockActivatedRouteSnapshot = jasmine.createSpyObj<ActivatedRouteSnapshot>('ActivatedRouteSnapshot', [
            'toString'
        ]);
    });

    it('should allow access to Menu Portlets', () => {
        let result: boolean;
        mockRouterStateSnapshot.url = '/test';
        spyOn(dotMenuService, 'isPortletInMenu').and.returnValue(Observable.of(true));
        menuGuardService
            .canActivate(mockActivatedRouteSnapshot, mockRouterStateSnapshot)
            .subscribe(res => (result = res));
        expect(dotMenuService.isPortletInMenu).toHaveBeenCalledWith('test');
        expect(result).toBe(true);
    });

    it('should prevent access to Menu Portlets', () => {
        let result: boolean;
        mockRouterStateSnapshot.url = '/test';
        spyOn(dotMenuService, 'isPortletInMenu').and.returnValue(Observable.of(false));
        menuGuardService
            .canActivate(mockActivatedRouteSnapshot, mockRouterStateSnapshot)
            .subscribe(res => (result = res));
        expect(dotMenuService.isPortletInMenu).toHaveBeenCalledWith('test');
        expect(dotNavigationService.goToFirstPortlet).toHaveBeenCalled();
        expect(result).toBe(false);
    });

    it('should allow children access to Menu Portlets', () => {
        let result: boolean;
        mockRouterStateSnapshot.url = '/test';
        spyOn(dotMenuService, 'isPortletInMenu').and.returnValue(Observable.of(true));
        menuGuardService
            .canActivateChild(mockActivatedRouteSnapshot, mockRouterStateSnapshot)
            .subscribe(res => (result = res));
        expect(dotMenuService.isPortletInMenu).toHaveBeenCalledWith('test');
        expect(result).toBe(true);
    });

    it('should prevent children access to Menu Portlets', () => {
        let result: boolean;
        mockRouterStateSnapshot.url = '/test';
        spyOn(dotMenuService, 'isPortletInMenu').and.returnValue(Observable.of(false));
        menuGuardService
            .canActivateChild(mockActivatedRouteSnapshot, mockRouterStateSnapshot)
            .subscribe(res => (result = res));
        expect(dotMenuService.isPortletInMenu).toHaveBeenCalledWith('test');
        expect(dotNavigationService.goToFirstPortlet).toHaveBeenCalled();
        expect(result).toBe(false);
    });
});
