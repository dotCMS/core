import { Observable } from 'rxjs';

import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { map } from 'rxjs/operators';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';

export type DotFieldFilter =
    | 'SHOW_IN_LIST'
    | 'REQUIRED'
    | 'USER_SEARCHABLE'
    | 'SYSTEM_INDEXED'
    | 'UNIQUE';
@Injectable({
    providedIn: 'root'
})
export class DotFieldService {
    readonly #http = inject(HttpClient);

    /**
     * Get all fields for a specific content type with optional filtering
     * @param contentType - The content type identifier
     * @param filter - Optional filter to apply
     * @returns Observable of DotCMSContentTypeField array
     */
    getFields(contentType: string, filter?: DotFieldFilter): Observable<DotCMSContentTypeField[]> {
        const params = filter ? new HttpParams().set('filter', filter) : new HttpParams();

        return this.#http
            .get<{
                entity: DotCMSContentTypeField[];
            }>(`/api/v3/contenttype/${contentType}/fields/allfields`, { params })
            .pipe(map((x: { entity: DotCMSContentTypeField[] }) => x?.entity));
    }
}
