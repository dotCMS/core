import { Injectable } from '@angular/core';

import { Observable } from 'rxjs';
import { pluck, map } from 'rxjs/operators';

import { CoreWebService } from 'dotcms-js';

import { DotPageRender } from '@models/dot-page/dot-rendered-page.model';
import { DotLayout } from '@shared/models/dot-edit-layout-designer';

/**
 * Provide util methods interact with layout API
 *
 * @export
 * @class DotPageLayoutService
 */
@Injectable()
export class DotPageLayoutService {
    constructor(private coreWebService: CoreWebService) {}

    /**
     * Save the layout of a page
     *
     * @param {string} pageIdentifier
     * @param {DotLayout} dotLayout
     * @returns {Observable<DotPageRender>}
     * @memberof DotPageLayoutService
     */
    save(pageIdentifier: string, dotLayout: DotLayout): Observable<DotPageRender> {
        return this.coreWebService
            .requestView({
                body: dotLayout,
                method: 'POST',
                url: `v1/page/${pageIdentifier}/layout`
            })
            .pipe(
                pluck('entity'),
                map(
                    (dotPageRenderResponse: DotPageRender.Parameters) =>
                        new DotPageRender(dotPageRenderResponse)
                )
            );
    }
}
