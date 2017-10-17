import { TestBed } from '@angular/core/testing';
import { Injectable } from '@angular/core';
import { DefaultGuardService } from './default-guard.service';
import { DotRouterService } from './../dot-router-service';

@Injectable()
class MockDotRouterService {
    goToMain = jasmine.createSpy('goToMain');
}

describe('ValidDefaultGuardService', () => {
    let defaultGuardService: DefaultGuardService;
    let dotRouterService: DotRouterService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [DefaultGuardService, { provide: DotRouterService, useClass: MockDotRouterService }]
        });

        defaultGuardService = TestBed.get(DefaultGuardService);
        dotRouterService = TestBed.get(DotRouterService);
    });

    it('should redirect to to Main Portlet always', () => {
        let result = defaultGuardService.canActivate();
        expect(dotRouterService.goToMain).toHaveBeenCalled();
        expect(result).toBe(true);
    });
});
