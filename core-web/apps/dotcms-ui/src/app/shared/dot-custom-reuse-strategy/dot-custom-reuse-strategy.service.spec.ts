import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot } from '@angular/router';

import { DotCustomReuseStrategyService } from './dot-custom-reuse-strategy.service';

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
                renewRoute as unknown as ActivatedRouteSnapshot
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

    /**
     * Pinned because UVE depends on it (#38303): `/edit-page` reads its page from the URL once,
     * when it is built, and writes page changes with `Location.go`. Browser Back to another page,
     * and the vanity-URL redirect (`router.navigate([], { queryParams: { url } })`), load the
     * right page only because a query-only navigation rebuilds a `reuseRoute: false` route. A
     * screen that must survive Back with a side panel open drops the flag from its route instead.
     */
    it('should rebuild a `reuseRoute: false` route on a navigation that only changes its query params', () => {
        const config = { path: 'content' };
        const current = {
            routeConfig: config,
            params: {},
            queryParams: { url: '/about-us' },
            data: { reuseRoute: false }
        };
        const future = { ...current, queryParams: { url: '/blog' } };

        expect(
            service.shouldReuseRoute(
                future as unknown as ActivatedRouteSnapshot,
                current as unknown as ActivatedRouteSnapshot
            )
        ).toEqual(false);
    });
});
