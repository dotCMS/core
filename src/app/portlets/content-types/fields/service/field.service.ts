import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Observable';
import { Field, FieldType, FieldRow } from '../shared';
import { CoreWebService } from 'dotcms-js/dotcms-js';
import { RequestMethod } from '@angular/http';
import { FieldUtil } from '../util/field-util';
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
            .pluck('entity');
    }

    /**
     * Save fields.
     * @param {string} contentTypeId Content Type'id
     * @param {Field[]} fields fields to add
     * @returns {Observable<any>}
     * @memberof FieldService
     */
    saveFields(contentTypeId: string, fields: Field[]): Observable<any> {
        fields.forEach((field, index) => {
            field.contentTypeId = contentTypeId;

            if (FieldUtil.isRowOrColumn(field)) {
                field.name = `fields-${index}`;
            }

            if (field.dataType === '') {
                delete field.dataType;
            }
        });

        return this.coreWebService.requestView({
            body: fields,
            method: RequestMethod.Put,
            url: `v1/contenttype/${contentTypeId}/fields`
        }).pluck('entity');
    }

    /**
     * Delete fields
     * @param contentTypeId content types's id that contains the fields
     * @param fields Fields to delete
     */
    deleteFields(contentTypeId: string, fields: Field[]): Observable<{fields: Field[], deletedIds: string[]}> {
        return this.coreWebService.requestView({
            body: fields.map(field => field.id),
            method: RequestMethod.Delete,
            url: `v1/contenttype/${contentTypeId}/fields`
        }).pluck('entity');
    }

    /**
     * Get Field icon by field's class
     * @param fieldClazz
     */
    getIcon(fieldClazz: string): string {
        return FIELD_ICONS[fieldClazz];
    }
}
