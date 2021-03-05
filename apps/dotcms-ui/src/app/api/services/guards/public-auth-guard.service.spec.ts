import { DotRouterService } from '@services/dot-router/dot-router.service';
import { DOTTestBed } from '@tests/dot-test-bed';
import { of as observableOf, Observable } from 'rxjs';
import { TestBed } from '@angular/core/testing';
import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { PublicAuthGuardService } from './public-auth-guard.service';
import { LoginService } from '@dotcms/dotcms-js';

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

        publicAuthGuardService = TestBed.get(PublicAuthGuardService);
        dotRouterService = TestBed.get(DotRouterService);
        loginService = TestBed.get(LoginService);
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
