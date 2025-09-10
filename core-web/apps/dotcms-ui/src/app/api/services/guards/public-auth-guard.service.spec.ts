import { Observable, of as observableOf } from 'rxjs';

import { Injectable } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';

import { DotRouterService } from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';

import { PublicAuthGuardService } from './public-auth-guard.service';

import { DOTTestBed } from '../../../test/dot-test-bed';

@Injectable()
class MockLoginService {
    private _isLogin$: Observable<boolean>;
    get isLogin$() {
        return this._isLogin$;
    }
}

describe('ValidPublicAuthGuardService', () => {
    let publicAuthGuardService: PublicAuthGuardService;
    let dotRouterService: DotRouterService;
    let loginService: LoginService;
    let mockRouterStateSnapshot: RouterStateSnapshot;
    let mockActivatedRouteSnapshot: ActivatedRouteSnapshot;

    beforeEach(() => {
        DOTTestBed.configureTestingModule({
            providers: [
                PublicAuthGuardService,
                { provide: LoginService, useClass: MockLoginService }
            ]
        });

        publicAuthGuardService = TestBed.inject(PublicAuthGuardService);
        dotRouterService = TestBed.inject(DotRouterService);
        loginService = TestBed.inject(LoginService);
        mockRouterStateSnapshot = jasmine.createSpyObj<RouterStateSnapshot>('RouterStateSnapshot', [
            'toString'
        ]);
        mockActivatedRouteSnapshot = jasmine.createSpyObj<ActivatedRouteSnapshot>(
            'ActivatedRouteSnapshot',
            ['toString']
        );
    });

    it('should redirect to to Main Portlet if User is logged in', () => {
        let result: boolean;
        spyOnProperty(loginService, 'isLogin$', 'get').and.returnValue(observableOf(true));
        publicAuthGuardService
            .canActivate(mockActivatedRouteSnapshot, mockRouterStateSnapshot)
            .subscribe((res) => (result = res));
        expect(dotRouterService.goToMain).toHaveBeenCalled();
        expect(result).toBe(false);
    });

    it('should allow access to the requested route if User is NOT logged in', () => {
        let result: boolean;
        spyOnProperty(loginService, 'isLogin$', 'get').and.returnValue(observableOf(false));
        publicAuthGuardService
            .canActivate(mockActivatedRouteSnapshot, mockRouterStateSnapshot)
            .subscribe((res) => (result = res));
        expect(result).toBe(true);
    });
});
