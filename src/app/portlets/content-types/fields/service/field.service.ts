import { pluck } from 'rxjs/operators';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { DotContentTypeField, FieldType, DotContentTypeLayoutDivider } from '../models';
import { CoreWebService } from 'dotcms-js';
import { RequestMethod } from '@angular/http';
import { FIELD_ICONS } from '../content-types-fields-list/content-types-fields-icon-map';

/**
 * Provide method to handle with the Field Types
 */
@Injectable()
export class FieldService {
    constructor(private coreWebService: CoreWebService) {}

    loadFieldTypes(): Observable<FieldType[]> {
        return this.coreWebService
            .requestView({
                method: RequestMethod.Get,
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
    saveFields(contentTypeId: string, fields: DotContentTypeLayoutDivider[]): Observable<DotContentTypeLayoutDivider[]> {

        return this.coreWebService
            .requestView({
                body: {
                    layout: fields
                },
                method: RequestMethod.Put,
                url: `v3/contenttype/${contentTypeId}/fields/move`
            })
            .pipe(pluck('entity'));
    }

    /**
     * Delete fields
     *
     * @param {string} contentTypeId content types's id that contains the fields
     * @param {DotContentTypeField[]} fields Fields to delete
     * @returns {Observable<{ fields: DotContentTypeLayoutDivider[]; deletedIds: string[] }>}
     * @memberof FieldService
     */
    deleteFields(
        contentTypeId: string,
        fields: DotContentTypeField[]
    ): Observable<{ fields: DotContentTypeLayoutDivider[]; deletedIds: string[] }> {
        return this.coreWebService
            .requestView({
                body: {
                    fieldsID: fields.map((field: DotContentTypeField) => field.id)
                },
                method: RequestMethod.Delete,
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
     * @param {DotContentTypeField} field field to update
     * @returns {Observable<DotContentTypeLayoutDivider[]>}
     * @memberof FieldService
     */
    updateField(contentTypeId: string, field: DotContentTypeField): Observable<DotContentTypeLayoutDivider[]> {
        return this.coreWebService
            .requestView({
                body: {
                    field: field
                },
                method: RequestMethod.Put,
                url: `v3/contenttype/${contentTypeId}/fields/${field.id}`
            })
            .pipe(pluck('entity'));
    }
}
