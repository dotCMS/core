import { Observable } from 'rxjs';
import { ActivatedRouteSnapshot, Resolve } from '@angular/router';
import { PageViewService } from './../../api/services/page-view/page-view.service';
import { Injectable } from '@angular/core';
import { DotPageView } from './shared/models/dot-page-view.model';

@Injectable()
export class PageViewResolver implements Resolve<any> {
    constructor(private pageViewService: PageViewService) {}

    /**
     * Route resolver for layout/:url that resolves into a PageView object
     * @param {ActivatedRouteSnapshot} route
     * @returns {Observable<any>}
     * @memberof PageViewResolver
     */
    resolve(route: ActivatedRouteSnapshot): Observable<DotPageView> {
        return this.pageViewService.get(route.queryParams.url);
    }
}
