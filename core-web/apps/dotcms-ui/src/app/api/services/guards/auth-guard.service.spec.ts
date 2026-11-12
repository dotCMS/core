import { Observable, of as observableOf } from 'rxjs';

import { Injectable } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';

import { DotMessageService, DotRouterService } from '@dotcms/data-access';
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

const ensureLoaded = jasmine.createSpy('ensureLoaded').and.returnValue(observableOf(true));

describe('ValidAuthGuardService', () => {
    let authGuardService: AuthGuardService;
    let dotRouterService: DotRouterService;
    let loginService: LoginService;
    let mockRouterStateSnapshot: RouterStateSnapshot;
    let mockActivatedRouteSnapshot: ActivatedRouteSnapshot;

    beforeEach(() => {
        DOTTestBed.configureTestingModule({
            providers: [
                AuthGuardService,
                { provide: LoginService, useClass: MockLoginService },
                { provide: DotMessageService, useValue: { ensureLoaded } }
            ]
        });

        authGuardService = TestBed.get(AuthGuardService);
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

    beforeEach(() => ensureLoaded.calls.reset());

    it('should wait for the messages and then allow access, User is logged in', () => {
        let result: boolean;
        spyOnProperty(loginService, 'isLogin$', 'get').and.returnValue(observableOf(true));
        authGuardService
            .canActivate(mockActivatedRouteSnapshot, mockRouterStateSnapshot)
            .subscribe((res) => (result = res));
        expect(ensureLoaded).toHaveBeenCalled();
        expect(result).toBe(true);
    });

    it('should not load the messages when the User is NOT logged in', () => {
        spyOnProperty(loginService, 'isLogin$', 'get').and.returnValue(observableOf(false));
        spyOn(dotRouterService, 'goToLogin');
        authGuardService
            .canActivate(mockActivatedRouteSnapshot, mockRouterStateSnapshot)
            .subscribe();
        expect(ensureLoaded).not.toHaveBeenCalled();
    });

    it('should allow access to the requested route, User is logged in', () => {
        let result: boolean;
        spyOnProperty(loginService, 'isLogin$', 'get').and.returnValue(observableOf(true));
        authGuardService
            .canActivate(mockActivatedRouteSnapshot, mockRouterStateSnapshot)
            .subscribe((res) => (result = res));
        expect(result).toBe(true);
    });

    it('should denied access to the requested route, User is NOT logged in', () => {
        let result: boolean;
        spyOnProperty(loginService, 'isLogin$', 'get').and.returnValue(observableOf(false));
        authGuardService
            .canActivate(mockActivatedRouteSnapshot, mockRouterStateSnapshot)
            .subscribe((res) => (result = res));
        expect(dotRouterService.goToLogin).toHaveBeenCalled();
        expect(result).toBe(false);
    });
});
