import { Observable } from 'rxjs';

import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

import { CoreWebService, DotRequestOptionsArgs } from '@dotcms/dotcms-js';
import { DotPageRender, DotPageRenderParameters, DotTemplateDesigner } from '@dotcms/dotcms-models';

import { DotSessionStorageService } from '../dot-session-storage/dot-session-storage.service';

/**
 * Provide util methods interact with layout API
 *
 * @export
 * @class DotPageLayoutService
 */
@Injectable()
export class DotPageLayoutService {
    private coreWebService = inject(CoreWebService);
    private readonly dotSessionStorageService = inject(DotSessionStorageService);

    /**
     * Save the layout of a page
     *
     * @param {string} pageIdentifier
     * @param {DotLayout} DotTemplateDesignerPayload
     * @returns {Observable<DotPageRender>}
     * @memberof DotPageLayoutService
     */
    save(pageIdentifier: string, dotLayout: DotTemplateDesigner): Observable<DotPageRender> {
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
            map((x) => x?.entity),
            map(
                (dotPageRenderResponse: DotPageRenderParameters) =>
                    new DotPageRender(dotPageRenderResponse)
            )
        );
    }
}
