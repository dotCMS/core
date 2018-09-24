import { pluck } from 'rxjs/operators';
import { RequestMethod } from '@angular/http';
import { Injectable } from '@angular/core';

import { Observable } from 'rxjs';

import { CoreWebService } from 'dotcms-js/dotcms-js';

import { DotLayout } from '@portlets/dot-edit-page/shared/models/dot-layout.model';
import { DotRenderedPage } from '@portlets/dot-edit-page/shared/models/dot-rendered-page.model';

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
    save(pageIdentifier: string, dotLayout: DotLayout): Observable<DotRenderedPage> {
        return this.coreWebService
            .requestView({
                body: dotLayout,
                method: RequestMethod.Post,
                url: `v1/page/${pageIdentifier}/layout`
            })
            .pipe(pluck('entity'));
    }
}
