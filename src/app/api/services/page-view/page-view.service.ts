import { RequestMethod } from '@angular/http';
import { Injectable } from '@angular/core';

import { Observable } from 'rxjs/Observable';

import { CoreWebService } from 'dotcms-js/dotcms-js';

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
}
