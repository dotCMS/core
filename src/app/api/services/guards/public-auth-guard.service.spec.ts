import { TestBed } from '@angular/core/testing';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Observable';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { PublicAuthGuardService } from './public-auth-guard.service';
import { LoginService } from 'dotcms-js/dotcms-js';
import { DotRouterService } from './../dot-router-service';

@Injectable()
class MockLoginService {
    private _isLogin$: Observable<boolean>;
    get isLogin$() {
        return this._isLogin$;
    }
}

@Injectable()
class MockDotRouterService {
    goToMain = jasmine.createSpy('goToMain');
}

describe('ValidPublicAuthGuardService', () => {
    let publicAuthGuardService: PublicAuthGuardService;
    let dotRouterService: DotRouterService;
    let loginService: LoginService;
    let mockRouterStateSnapshot: RouterStateSnapshot;
    let mockActivatedRouteSnapshot: ActivatedRouteSnapshot;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                PublicAuthGuardService,
                { provide: DotRouterService, useClass: MockDotRouterService },
                { provide: LoginService, useClass: MockLoginService }
            ]
        });

        publicAuthGuardService = TestBed.get(PublicAuthGuardService);
        dotRouterService = TestBed.get(DotRouterService);
        loginService = TestBed.get(LoginService);
        mockRouterStateSnapshot = jasmine.createSpyObj<RouterStateSnapshot>('RouterStateSnapshot', ['toString']);
        mockActivatedRouteSnapshot = jasmine.createSpyObj<ActivatedRouteSnapshot>('ActivatedRouteSnapshot', [
            'toString'
        ]);
    });

    it('should redirect to to Main Portlet if User is logged in', () => {
        let result: boolean;
        spyOnProperty(loginService, 'isLogin$', 'get').and.returnValue(Observable.of(true));
        publicAuthGuardService
            .canActivate(mockActivatedRouteSnapshot, mockRouterStateSnapshot)
            .subscribe(res => (result = res));
        expect(dotRouterService.goToMain).toHaveBeenCalled();
        expect(result).toBe(false);
    });

    it('should allow access to the requested route if User is NOT logged in', () => {
        let result: boolean;
        spyOnProperty(loginService, 'isLogin$', 'get').and.returnValue(Observable.of(false));
        publicAuthGuardService
            .canActivate(mockActivatedRouteSnapshot, mockRouterStateSnapshot)
            .subscribe(res => (result = res));
        expect(result).toBe(true);
    });
});
