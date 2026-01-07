import { Observable } from 'rxjs';

import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

import { DotPageRender, DotTemplateDesigner, DotCMSAPIResponse } from '@dotcms/dotcms-models';

import { DotSessionStorageService } from '../dot-session-storage/dot-session-storage.service';

/**
 * Provide util methods interact with layout API
 *
 * @export
 * @class DotPageLayoutService
 */
@Injectable()
export class DotPageLayoutService {
    readonly #http = inject(HttpClient);
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

        let httpParams = new HttpParams();
        if (currentVariantName) {
            httpParams = httpParams.set('variantName', currentVariantName);
        }

        return this.#http
            .post<DotCMSAPIResponse<DotPageRender>>(url, dotLayout, { params: httpParams })
            .pipe(map((response) => new DotPageRender(response.entity)));
    }
}
