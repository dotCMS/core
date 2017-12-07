import { RequestMethod } from '@angular/http';
import { CoreWebService } from 'dotcms-js/dotcms-js';
import { Observable } from 'rxjs/Observable';
import { Injectable } from '@angular/core';
import { DotPageView } from '../../../portlets/dot-edit-page/shared/models/dot-page-view.model';
import { DotLayout } from '../../../portlets/dot-edit-page/shared/models/dot-layout.model';

/**
 * Provide util methods to get and save a PageView object
 * @export
 * @class PageViewService
 */
@Injectable()
export class PageViewService {
    constructor(private coreWebService: CoreWebService) {}

    /**
     * Get a PageView object from url endpoint
     * @param {string} url
     * @returns {Observable<PageView>}
     * @memberof PageViewService
     */
    get(url: string): Observable<DotPageView> {
        return this.coreWebService.requestView({
            method: RequestMethod.Get,
            url: `v1/page/render/${url.replace(/^\//, '')}?live=false`
        }).pluck('bodyJsonObject');
    }

    /**
     * Will do a POST request and save the PageView layout object
     * @param {PageView} pageView
     * @returns {Observable<any>}
     * @memberof PageViewService
     */
    save(pageViewIdentifier: string, dotLayout: DotLayout): Observable<DotPageView> {
        return this.coreWebService.requestView({
            body: dotLayout,
            method: RequestMethod.Post,
            url: `v1/page/${pageViewIdentifier}/layout`
        }).pluck('entity');
    }
}
