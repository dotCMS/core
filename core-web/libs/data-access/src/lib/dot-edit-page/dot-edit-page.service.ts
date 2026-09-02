import { Observable } from 'rxjs';

import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

import { DotCMSResponse, DotPageContainer, DotWhatChanged } from '@dotcms/dotcms-models';

import { DotSessionStorageService } from '../dot-session-storage/dot-session-storage.service';

@Injectable()
export class DotEditPageService {
    private http = inject(HttpClient);
    private readonly dotSessionStorageService = inject(DotSessionStorageService);

    /**
     * Save a page's content
     *
     * @param string pageId
     * @param DotPageContainer[] content
     * @returns Observable<string>
     * @memberof DotEditPageService
     */
    save(pageId: string, content: DotPageContainer[]): Observable<string> {
        const url = `/api/v1/page/${pageId}/content`;
        const currentVariantName = this.dotSessionStorageService.getVariationId();

        let params = new HttpParams();
        if (currentVariantName) {
            params = params.set('variantName', currentVariantName);
        }

        return this.http
            .post<DotCMSResponse<string>>(url, content, { params })
            .pipe(map((response) => response.entity));
    }

    /**
     * Get the live and working version markup of specific page.
     *
     * @param string pageId
     * @param string language
     * @returns Observable<DotWhatChanged>
     * @memberof DotEditPageService
     */
    whatChange(pageId: string, languageId: string): Observable<DotWhatChanged> {
        return this.http
            .get<
                DotCMSResponse<DotWhatChanged>
            >(`/api/v1/page/${pageId}/render/versions?langId=${languageId}`)
            .pipe(map((response) => response.entity));
    }
}
