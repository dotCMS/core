import { of } from 'rxjs';

import { Injectable } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';

import { DotContentTypeService } from '@dotcms/data-access';

import { ContentletGuardService } from './contentlet-guard.service';

import { DotNavigationService } from '../../../view/components/dot-navigation/services/dot-navigation.service';

@Injectable()
class MockDotContentTypeService {
    isContentTypeInMenu() {
        of(true);
    }
}

@Injectable()
class MockDotNavigationService {
    goToFirstPortlet = jest.fn();
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
        mockRouterStateSnapshot = jest.fn<RouterStateSnapshot>('RouterStateSnapshot', ['toString']);
        mockActivatedRouteSnapshot = jest.fn<ActivatedRouteSnapshot>('ActivatedRouteSnapshot', [
            'toString'
        ]);
    });

    it('should allow children access to Content Types Portlets', () => {
        let result: boolean;
        mockActivatedRouteSnapshot.params = { id: 'banner' };
        jest.spyOn(dotContentletService, 'isContentTypeInMenu').mockReturnValue(of(true));
        contentletGuardService
            .canActivateChild(mockActivatedRouteSnapshot, mockRouterStateSnapshot)
            .subscribe((res) => (result = res));
        expect(dotContentletService.isContentTypeInMenu).toHaveBeenCalledWith('banner');
        expect(result).toBe(true);
    });

    it('should prevent children access to Content Types Portlets', () => {
        let result: boolean;
        mockActivatedRouteSnapshot.params = { id: 'banner' };
        jest.spyOn(dotContentletService, 'isContentTypeInMenu').mockReturnValue(of(false));
        contentletGuardService
            .canActivateChild(mockActivatedRouteSnapshot, mockRouterStateSnapshot)
            .subscribe((res) => (result = res));
        expect(dotContentletService.isContentTypeInMenu).toHaveBeenCalledWith('banner');
        expect(dotNavigationService.goToFirstPortlet).toHaveBeenCalled();
        expect(result).toBe(false);
    });
});
