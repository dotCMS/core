import { Observable, of as observableOf } from 'rxjs';

import { Injectable } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';

import { DotRouterService } from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';

import { AuthGuardService } from './auth-guard.service';

import { DOTTestBed } from '../../../test/dot-test-bed';

@Injectable()
class MockLoginService {
    private _isLogin$: Observable<boolean>;
    get isLogin$() {
        return this._isLogin$;
    }
}

describe('ValidAuthGuardService', () => {
    let authGuardService: AuthGuardService;
    let dotRouterService: DotRouterService;
    let loginService: LoginService;
    let mockRouterStateSnapshot: RouterStateSnapshot;
    let mockActivatedRouteSnapshot: ActivatedRouteSnapshot;

    beforeEach(() => {
        DOTTestBed.configureTestingModule({
            providers: [AuthGuardService, { provide: LoginService, useClass: MockLoginService }]
        });

        authGuardService = TestBed.inject(AuthGuardService);
        dotRouterService = TestBed.inject(DotRouterService);
        loginService = TestBed.inject(LoginService);
        mockRouterStateSnapshot = jest.fn<RouterStateSnapshot>('RouterStateSnapshot', ['toString']);
        mockActivatedRouteSnapshot = jest.fn<ActivatedRouteSnapshot>('ActivatedRouteSnapshot', [
            'toString'
        ]);
    });

    it('should allow access to the requested route, User is logged in', () => {
        let result: boolean;
        jest.spyOn(loginService, 'isLogin$', 'get', 'get').mockReturnValue(observableOf(true));
        authGuardService
            .canActivate(mockActivatedRouteSnapshot, mockRouterStateSnapshot)
            .subscribe((res) => (result = res));
        expect(result).toBe(true);
    });

    it('should denied access to the requested route, User is NOT logged in', () => {
        let result: boolean;
        jest.spyOn(loginService, 'isLogin$', 'get', 'get').mockReturnValue(observableOf(false));
        authGuardService
            .canActivate(mockActivatedRouteSnapshot, mockRouterStateSnapshot)
            .subscribe((res) => (result = res));
        expect(dotRouterService.goToLogin).toHaveBeenCalled();
        expect(result).toBe(false);
    });
});
