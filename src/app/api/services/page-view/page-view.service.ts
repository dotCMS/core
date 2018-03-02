import { RequestMethod } from '@angular/http';
import { Injectable } from '@angular/core';

import { Observable } from 'rxjs/Observable';

import { CoreWebService } from 'dotcms-js/dotcms-js';

import { DotPageView } from '../../../portlets/dot-edit-page/shared/models/dot-page-view.model';
import { DotLayout } from '../../../portlets/dot-edit-page/shared/models/dot-layout.model';
import { getTemplateTypeFlag } from '../../util/lib';

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
        return this.coreWebService
            .requestView({
                method: RequestMethod.Get,
                url: `v1/page/json/${url.replace(/^\//, '')}?live=false`
            })
            .pluck('bodyJsonObject');
    }

    /**
     * Will do a POST request and save the PageView layout object
     * @param {PageView} pageView
     * @returns {Observable<any>}
     * @memberof PageViewService
     */
    save(pageIdentifier: string, dotLayout: DotLayout): Observable<DotPageView> {
        return this.coreWebService
            .requestView({
                body: dotLayout,
                method: RequestMethod.Post,
                url: `v1/page/${pageIdentifier}/layout`
            })
            .pluck('entity');
    }

    /**
     * Get a flag for the type of template in a page
     *
     * @param {string} url
     * @returns {Observable<boolean>}
     * @memberof PageViewService
     */
    isTemplateAdvanced(url: string): Observable<boolean> {
        return this.get(url).let(getTemplateTypeFlag);
    }
}
