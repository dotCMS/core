import { Observable } from 'rxjs';

import { Injectable, inject } from '@angular/core';

import { pluck } from 'rxjs/operators';

import { CoreWebService } from '@dotcms/dotcms-js';
import { DotCMSContentTypeField, DotCMSContentTypeLayoutRow } from '@dotcms/dotcms-models';

import { FIELD_ICONS } from '../content-types-fields-list/content-types-fields-icon-map';
import { FieldType } from '../models';

/**
 * Provide method to handle with the Field Types
 */
@Injectable()
export class FieldService {
    private coreWebService = inject(CoreWebService);

    /**
     * Get the field types
     *
     * @return {*}  {Observable<FieldType[]>}
     * @memberof FieldService
     */
    loadFieldTypes(): Observable<FieldType[]> {
        return this.coreWebService
            .requestView({
                url: 'v1/fieldTypes'
            })
            .pipe(pluck('entity'));
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
        return this.coreWebService
            .requestView({
                body: {
                    layout: fields
                },
                method: 'PUT',
                url: `v3/contenttype/${contentTypeId}/fields/move`
            })
            .pipe(pluck('entity'));
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
        return this.coreWebService
            .requestView({
                body: {
                    fieldsID: fields.map((field: DotCMSContentTypeField) => field.id)
                },
                method: 'DELETE',
                url: `v3/contenttype/${contentTypeId}/fields`
            })
            .pipe(pluck('entity'));
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
        return this.coreWebService
            .requestView({
                body: {
                    field: field
                },
                method: 'PUT',
                url: `v3/contenttype/${contentTypeId}/fields/${field.id}`
            })
            .pipe(pluck('entity'));
    }
}
