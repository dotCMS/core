import { Observable } from 'rxjs';

import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

import { DotCMSPersonalizedItem, DotCMSResponse } from '@dotcms/dotcms-models';

import { DotSessionStorageService } from '../dot-session-storage/dot-session-storage.service';

@Injectable()
export class DotPersonalizeService {
    private http = inject(HttpClient);
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

        let params = new HttpParams();
        if (currentVariantName) {
            params = params.set('variantName', currentVariantName);
        }

        return this.http
            .post<
                DotCMSResponse<DotCMSPersonalizedItem[]>
            >('/api/v1/personalization/pagepersonas', { pageId, personaTag }, { params })
            .pipe(map((response) => response.entity));
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

        let params = new HttpParams();
        if (currentVariantName) {
            params = params.set('variantName', currentVariantName);
        }

        return this.http
            .delete<
                DotCMSResponse<string>
            >(`/api/v1/personalization/pagepersonas/page/${pageId}/personalization/${personaTag}`, { params })
            .pipe(map((response) => response.entity));
    }
}
