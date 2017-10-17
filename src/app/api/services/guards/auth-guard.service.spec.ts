import { TestBed } from '@angular/core/testing';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Observable';
import { AuthGuardService } from './auth-guard.service';
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
    goToLogin = jasmine.createSpy('goToLogin');
}

describe('ValidAuthGuardService', () => {
    let authGuardService: AuthGuardService;
    let dotRouterService: DotRouterService;
    let loginService: LoginService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                AuthGuardService,
                { provide: DotRouterService, useClass: MockDotRouterService },
                { provide: LoginService, useClass: MockLoginService },
            ]
        });

        authGuardService = TestBed.get(AuthGuardService);
        dotRouterService = TestBed.get(DotRouterService);
        loginService = TestBed.get(LoginService);
    });

    it('should allow access to the requested route, User is logged in', () => {
        let result: boolean;
        spyOnProperty(loginService, 'isLogin$', 'get').and.returnValue(Observable.of(true));
        authGuardService
            .canActivate()
            .subscribe(res => (result = res));
        expect(result).toBe(true);
    });

    it('should denied access to the requested route, User is NOT logged in', () => {
        let result: boolean;
        spyOnProperty(loginService, 'isLogin$', 'get').and.returnValue(Observable.of(false));
        authGuardService
            .canActivate()
            .subscribe(res => (result = res));
        expect(dotRouterService.goToLogin).toHaveBeenCalled();
        expect(result).toBe(false);
    });
});
