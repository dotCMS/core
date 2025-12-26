import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

import {
    DotCMSContentTypeField,
    DotCMSContentTypeLayoutRow,
    DotCMSResponse
} from '@dotcms/dotcms-models';

import { FIELD_ICONS } from '../content-types-fields-list/content-types-fields-icon-map';
import { FieldType } from '../models';

/**
 * Provide method to handle with the Field Types
 */
@Injectable()
export class FieldService {
    private http = inject(HttpClient);

    /**
     * Get the field types
     *
     * @return {*}  {Observable<FieldType[]>}
     * @memberof FieldService
     */
    loadFieldTypes(): Observable<FieldType[]> {
        return this.http
            .get<DotCMSResponse<FieldType[]>>('/api/v1/fieldTypes')
            .pipe(map((response) => response.entity));
    }

    /**
     * Save content type's layout.
     * @param string contentTypeId Content Type'id
     * @param DotContentTypeField[] fields fields to add
     * @returns Observable<any>
     * @memberof FieldService
     */
    saveFields(
        contentTypeId: string,
        fields: DotCMSContentTypeLayoutRow[]
    ): Observable<DotCMSContentTypeLayoutRow[]> {
        return this.http
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
     * @memberof FieldService
     */
    deleteFields(
        contentTypeId: string,
        fields: DotCMSContentTypeField[]
    ): Observable<{ fields: DotCMSContentTypeLayoutRow[]; deletedIds: string[] }> {
        return this.http
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
     * Get Field icon by field's class
     *
     * @param {string} fieldClazz
     * @returns {string}
     * @memberof FieldService
     */
    getIcon(fieldClazz: string): string {
        return FIELD_ICONS[fieldClazz];
    }

    /**
     * Update a field
     *
     * @param {string} contentTypeId content type's id
     * @param {DotCMSContentTypeField} field field to update
     * @returns {Observable<DotCMSContentTypeLayoutRow[]>}
     * @memberof FieldService
     */
    updateField(
        contentTypeId: string,
        field: DotCMSContentTypeField
    ): Observable<DotCMSContentTypeLayoutRow[]> {
        return this.http
            .put<
                DotCMSResponse<DotCMSContentTypeLayoutRow[]>
            >(`/api/v3/contenttype/${contentTypeId}/fields/${field.id}`, { field: field })
            .pipe(map((response) => response.entity));
    }
}
