import { ActivatedRouteSnapshot, DetachedRouteHandle, RouteReuseStrategy } from '@angular/router';

export class DotCustomReuseStrategyService implements RouteReuseStrategy {
    constructor() {}

    retrieve(_route: ActivatedRouteSnapshot): DetachedRouteHandle | null {
        return null;
    }

    shouldAttach(_route: ActivatedRouteSnapshot): boolean {
        return false;
    }

    shouldDetach(_route: ActivatedRouteSnapshot): boolean {
        return false;
    }

    shouldReuseRoute(future: ActivatedRouteSnapshot, curr: ActivatedRouteSnapshot): boolean {
        return future.routeConfig === curr.routeConfig
            ? future.data.reuseRoute === false
                ? false
                : true
            : false;
    }

    store(_route: ActivatedRouteSnapshot, _handle: DetachedRouteHandle | null): void {}
}
