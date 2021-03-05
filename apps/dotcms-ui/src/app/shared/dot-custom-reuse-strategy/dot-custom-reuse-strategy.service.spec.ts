import { TestBed } from '@angular/core/testing';

import { DotCustomReuseStrategyService } from './dot-custom-reuse-strategy.service';
import { ActivatedRouteSnapshot } from '@angular/router';

describe('DotCustomReuseStrategyService', () => {
    let service: DotCustomReuseStrategyService;
    const regularRoute = { routeConfig: null, data: {} };
    const regularRoute2 = { routeConfig: { path: 'test' }, data: {} };
    const regularRoute3 = { routeConfig: null, data: {} };
    const renewRoute = { routeConfig: null, data: { reuseRoute: false } };

    beforeEach(() => {
        TestBed.configureTestingModule({ providers: [DotCustomReuseStrategyService] });
        service = TestBed.inject(DotCustomReuseStrategyService);
    });

    it('should return angular default behavior on retrieve', () => {
        expect(service.retrieve(regularRoute as ActivatedRouteSnapshot)).toBeNull();
    });

    it('should return angular default behavior on shouldAttach', () => {
        expect(service.shouldAttach(regularRoute as ActivatedRouteSnapshot)).toEqual(false);
    });

    it('should return angular default behavior on shouldDetach', () => {
        expect(service.shouldDetach(regularRoute as ActivatedRouteSnapshot)).toEqual(false);
    });

    it('should return true if routeConfig match', () => {
        expect(
            service.shouldReuseRoute(
                regularRoute as ActivatedRouteSnapshot,
                regularRoute3 as ActivatedRouteSnapshot
            )
        ).toEqual(true);
    });

    it('should return false if routeConfig match but reUseRoute is false', () => {
        expect(
            service.shouldReuseRoute(
                regularRoute as ActivatedRouteSnapshot,
                (renewRoute as unknown) as ActivatedRouteSnapshot
            )
        ).toEqual(true);
    });

    it('should return false if routeConfig dont match', () => {
        expect(
            service.shouldReuseRoute(
                regularRoute as ActivatedRouteSnapshot,
                regularRoute2 as ActivatedRouteSnapshot
            )
        ).toEqual(false);
    });
});
