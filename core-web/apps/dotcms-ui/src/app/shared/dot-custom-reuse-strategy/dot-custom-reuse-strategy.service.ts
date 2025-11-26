import { ActivatedRouteSnapshot, DetachedRouteHandle, RouteReuseStrategy } from '@angular/router';

export class DotCustomReuseStrategyService implements RouteReuseStrategy {
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
        if (future.routeConfig !== curr.routeConfig) {
            return false;
        }

        // If it's not explicitly set to false, reuse the route
        return future.data.reuseRoute !== false;
    }

    store(_route: ActivatedRouteSnapshot, _handle: DetachedRouteHandle | null): void {
        /* */
    }
}
