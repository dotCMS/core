import { Observable } from 'rxjs';

import { Injectable, inject } from '@angular/core';

import { pluck } from 'rxjs/operators';

import { CoreWebService } from '@dotcms/dotcms-js';
import { DotCMSPersonalizedItem } from '@dotcms/dotcms-models';

import { DotSessionStorageService } from '../dot-session-storage/dot-session-storage.service';

@Injectable()
export class DotPersonalizeService {
    private coreWebService = inject(CoreWebService);
    private dotSessionStorageService = inject(DotSessionStorageService);

    /**
     * Set a personalized page for the persona passed
     *
     * @param {string} pageId
     * @param {string} personaTag
     * @returns {Observable<DotCMSPersonalizedItem[]>}
     * @memberof DotPersonalizeService
     */
    personalized(pageId: string, personaTag: string): Observable<DotCMSPersonalizedItem[]> {
        const currentVariantName = this.dotSessionStorageService.getVariationId();

        return this.coreWebService
            .requestView({
                method: 'POST',
                url: `/api/v1/personalization/pagepersonas`,
                params: {
                    variantName: currentVariantName
                },
                body: {
                    pageId,
                    personaTag
                }
            })
            .pipe(pluck('entity'));
    }

    /**
     * Remove the personalization of the page for the persona passed
     *
     * @param {string} pageId
     * @param {string} personaTag
     * @returns {Observable<string>}
     * @memberof DotPersonalizeService
     */
    despersonalized(pageId: string, personaTag: string): Observable<string> {
        const currentVariantName = this.dotSessionStorageService.getVariationId();

        return this.coreWebService
            .requestView({
                method: 'DELETE',
                url: `/api/v1/personalization/pagepersonas/page/${pageId}/personalization/${personaTag}`,
                params: {
                    variantName: currentVariantName
                }
            })
            .pipe(pluck('entity'));
    }
}
