import { of } from 'rxjs';
import { TestBed } from '@angular/core/testing';
import { Injectable } from '@angular/core';
import { DotNavigationService } from '@components/dot-navigation/services/dot-navigation.service';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { ContentletGuardService } from './contentlet-guard.service';
import { DotContentTypeService } from '@services/dot-content-type/dot-content-type.service';

@Injectable()
class MockDotContentTypeService {
    isContentTypeInMenu() {
        of(true);
    }
}

@Injectable()
class MockDotNavigationService {
    goToFirstPortlet = jasmine.createSpy('goToFirstPortlet');
}

describe('ValidContentletGuardService', () => {
    let contentletGuardService: ContentletGuardService;
    let dotNavigationService: DotNavigationService;
    let dotContentletService: DotContentTypeService;
    let mockRouterStateSnapshot: RouterStateSnapshot;
    let mockActivatedRouteSnapshot: ActivatedRouteSnapshot;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                ContentletGuardService,
                {
                    provide: DotContentTypeService,
                    useClass: MockDotContentTypeService
                },
                {
                    provide: DotNavigationService,
                    useClass: MockDotNavigationService
                }
            ]
        });

        contentletGuardService = TestBed.inject(ContentletGuardService);
        dotContentletService = TestBed.inject(DotContentTypeService);
        dotNavigationService = TestBed.inject(DotNavigationService);
        mockRouterStateSnapshot = jasmine.createSpyObj<RouterStateSnapshot>('RouterStateSnapshot', [
            'toString'
        ]);
        mockActivatedRouteSnapshot = jasmine.createSpyObj<ActivatedRouteSnapshot>(
            'ActivatedRouteSnapshot',
            ['toString']
        );
    });

    it('should allow children access to Content Types Portlets', () => {
        let result: boolean;
        mockActivatedRouteSnapshot.params = { id: 'banner' };
        spyOn(dotContentletService, 'isContentTypeInMenu').and.returnValue(of(true));
        contentletGuardService
            .canActivateChild(mockActivatedRouteSnapshot, mockRouterStateSnapshot)
            .subscribe((res) => (result = res));
        expect(dotContentletService.isContentTypeInMenu).toHaveBeenCalledWith('banner');
        expect(result).toBe(true);
    });

    it('should prevent children access to Content Types Portlets', () => {
        let result: boolean;
        mockActivatedRouteSnapshot.params = { id: 'banner' };
        spyOn(dotContentletService, 'isContentTypeInMenu').and.returnValue(of(false));
        contentletGuardService
            .canActivateChild(mockActivatedRouteSnapshot, mockRouterStateSnapshot)
            .subscribe((res) => (result = res));
        expect(dotContentletService.isContentTypeInMenu).toHaveBeenCalledWith('banner');
        expect(dotNavigationService.goToFirstPortlet).toHaveBeenCalled();
        expect(result).toBe(false);
    });
});
