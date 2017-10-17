import { TestBed } from '@angular/core/testing';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Observable';
import { DotNavigationService } from '../../../view/components/dot-navigation/dot-navigation.service';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { ContentletGuardService } from './contentlet-guard.service';
import { DotContentletService } from '../dot-contentlet.service';

@Injectable()
class MockDotContentletService {
    isContentTypeInMenu() {}
}

@Injectable()
class MockDotNavigationService {
    goToFirstPortlet = jasmine.createSpy('goToFirstPortlet');
}

describe('ValidContentletGuardService', () => {
    let contentletGuardService: ContentletGuardService;
    let dotNavigationService: DotNavigationService;
    let dotContentletService: DotContentletService;
    let mockRouterStateSnapshot: RouterStateSnapshot;
    let mockActivatedRouteSnapshot: ActivatedRouteSnapshot;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                ContentletGuardService,
                {
                    provide: DotContentletService,
                    useClass: MockDotContentletService
                },
                {
                    provide: DotNavigationService,
                    useClass: MockDotNavigationService
                }
            ]
        });

        contentletGuardService = TestBed.get(ContentletGuardService);
        dotContentletService = TestBed.get(DotContentletService);
        dotNavigationService = TestBed.get(DotNavigationService);
        mockRouterStateSnapshot = jasmine.createSpyObj<RouterStateSnapshot>('RouterStateSnapshot', ['toString']);
        mockActivatedRouteSnapshot = jasmine.createSpyObj<ActivatedRouteSnapshot>('ActivatedRouteSnapshot', [
            'toString'
        ]);
    });

    it('should allow children access to Content Types Portlets', () => {
        let result: boolean;
        mockActivatedRouteSnapshot.params = { id: 'banner' };
        spyOn(dotContentletService, 'isContentTypeInMenu').and.returnValue(Observable.of(true));
        contentletGuardService
            .canActivateChild(mockActivatedRouteSnapshot, mockRouterStateSnapshot)
            .subscribe(res => (result = res));
        expect(dotContentletService.isContentTypeInMenu).toHaveBeenCalledWith('banner');
        expect(result).toBe(true);
    });

    it('should prevent children access to Content Types Portlets', () => {
        let result: boolean;
        mockActivatedRouteSnapshot.params = { id: 'banner' };
        spyOn(dotContentletService, 'isContentTypeInMenu').and.returnValue(Observable.of(false));
        contentletGuardService
            .canActivateChild(mockActivatedRouteSnapshot, mockRouterStateSnapshot)
            .subscribe(res => (result = res));
        expect(dotContentletService.isContentTypeInMenu).toHaveBeenCalledWith('banner');
        expect(dotNavigationService.goToFirstPortlet).toHaveBeenCalled();
        expect(result).toBe(false);
    });
});
