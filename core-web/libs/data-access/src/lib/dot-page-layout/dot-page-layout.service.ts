import { Observable } from 'rxjs';

import { Injectable } from '@angular/core';

import { pluck, map } from 'rxjs/operators';

import { CoreWebService, DotRequestOptionsArgs } from '@dotcms/dotcms-js';
import { DotLayout, DotPageRender, DotPageRenderParameters } from '@dotcms/dotcms-models';

import { DotSessionStorageService } from '../dot-session-storage/dot-session-storage.service';

/**
 * Provide util methods interact with layout API
 *
 * @export
 * @class DotPageLayoutService
 */
@Injectable()
export class DotPageLayoutService {
    constructor(
        private coreWebService: CoreWebService,
        private readonly dotSessionStorageService: DotSessionStorageService
    ) {}

    /**
     * Save the layout of a page
     *
     * @param {string} pageIdentifier
     * @param {DotLayout} dotLayout
     * @returns {Observable<DotPageRender>}
     * @memberof DotPageLayoutService
     */
    save(pageIdentifier: string, dotLayout: DotLayout): Observable<DotPageRender> {
        const requestOptions: DotRequestOptionsArgs = {
            body: dotLayout,
            method: 'POST',
            url: `v1/page/${pageIdentifier}/layout`
        };

        const currentVariantName = this.dotSessionStorageService.getVariationId();

        if (currentVariantName) {
            requestOptions.params = {
                variantName: currentVariantName
            };
        }

        return this.coreWebService.requestView(requestOptions).pipe(
            pluck('entity'),
            map(
                (dotPageRenderResponse: DotPageRenderParameters) =>
                    new DotPageRender(dotPageRenderResponse)
            )
        );
    }
}
