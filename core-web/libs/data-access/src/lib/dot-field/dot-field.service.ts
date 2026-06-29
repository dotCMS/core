import { Observable } from 'rxjs';

import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { map } from 'rxjs/operators';

import {
    DotCMSAPIResponse,
    DotCMSContentTypeField,
    DotCMSContentTypeLayoutRow,
    DotCMSResponse,
    FieldType
} from '@dotcms/dotcms-models';

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
            .get<
                DotCMSAPIResponse<DotCMSContentTypeField[]>
            >(`/api/v3/contenttype/${contentType}/fields/allfields`, { params })
            .pipe(map((x) => x?.entity));
    }

    /**
     * Get the field types
     *
     * @return {*}  {Observable<FieldType[]>}
     * @memberof DotFieldService
     */
    loadFieldTypes(): Observable<FieldType[]> {
        return this.#http
            .get<DotCMSResponse<FieldType[]>>('/api/v1/fieldTypes')
            .pipe(map((response) => response.entity));
    }

    /**
     * Save content type's layout.
     * @param string contentTypeId Content Type'id
     * @param DotContentTypeField[] fields fields to add
     * @returns Observable<DotCMSContentTypeLayoutRow[]>
     * @memberof DotFieldService
     */
    saveFields(
        contentTypeId: string,
        fields: DotCMSContentTypeLayoutRow[]
    ): Observable<DotCMSContentTypeLayoutRow[]> {
        return this.#http
            .put<
                DotCMSResponse<DotCMSContentTypeLayoutRow[]>
            >(`/api/v3/contenttype/${contentTypeId}/fields/move`, { layout: fields })
            .pipe(map((response) => response.entity));
    }

    /**
     * Delete fields
     *
     * @param {string} contentTypeId content types's id that contains the fields
     * @param {DotCMSContentTypeField[]} fields Fields to delete
     * @returns {Observable<{ fields: DotCMSContentTypeLayoutRow[]; deletedIds: string[] }>}
     * @memberof DotFieldService
     */
    deleteFields(
        contentTypeId: string,
        fields: DotCMSContentTypeField[]
    ): Observable<{ fields: DotCMSContentTypeLayoutRow[]; deletedIds: string[] }> {
        return this.#http
            .request<
                DotCMSResponse<{ fields: DotCMSContentTypeLayoutRow[]; deletedIds: string[] }>
            >('DELETE', `/api/v3/contenttype/${contentTypeId}/fields`, {
                body: {
                    fieldsID: fields.map((field: DotCMSContentTypeField) => field.id)
                }
            })
            .pipe(map((response) => response.entity));
    }

    /**
     * Update a field
     *
     * @param {string} contentTypeId content type's id
     * @param {DotCMSContentTypeField} field field to update
     * @returns {Observable<DotCMSContentTypeLayoutRow[]>}
     * @memberof DotFieldService
     */
    updateField(
        contentTypeId: string,
        field: DotCMSContentTypeField
    ): Observable<DotCMSContentTypeLayoutRow[]> {
        return this.#http
            .put<
                DotCMSResponse<DotCMSContentTypeLayoutRow[]>
            >(`/api/v3/contenttype/${contentTypeId}/fields/${field.id}`, { field: field })
            .pipe(map((response) => response.entity));
    }
}
