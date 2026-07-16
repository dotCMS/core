import { Observable } from 'rxjs';

import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

import {
    DotCMSResponse,
    DotPageRender,
    DotPageRenderParameters,
    DotTemplateDesigner
} from '@dotcms/dotcms-models';

import { DotSessionStorageService } from '../dot-session-storage/dot-session-storage.service';

/**
 * Provide util methods interact with layout API
 *
 * @export
 * @class DotPageLayoutService
 */
@Injectable()
export class DotPageLayoutService {
    private http = inject(HttpClient);
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
        const url = `/api/v1/page/${pageIdentifier}/layout`;
        const currentVariantName = this.dotSessionStorageService.getVariationId();

        let params = new HttpParams();
        if (currentVariantName) {
            params = params.set('variantName', currentVariantName);
        }

        return this.http
            .post<DotCMSResponse<DotPageRenderParameters>>(url, dotLayout, { params })
            .pipe(
                map((response) => response.entity),
                map(
                    (dotPageRenderResponse: DotPageRenderParameters) =>
                        new DotPageRender(dotPageRenderResponse)
                )
            );
    }
}
