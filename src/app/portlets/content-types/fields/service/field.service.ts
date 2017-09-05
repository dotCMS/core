import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Observable';
import { Field, FieldType, FieldRow } from '../shared';
import { CoreWebService } from 'dotcms-js/dotcms-js';
import { RequestMethod } from '@angular/http';
import { FieldUtil } from '../util/field-util';

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
                url: '/v1/fieldTypes'
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
        const observables: Observable<any>[] = fields.map((field, index) => {
            const fieldToSend = Object.assign({}, field, {
                contentTypeId: contentTypeId,
                sortOrder: index + 1
            });

            if (FieldUtil.isColumn(fieldToSend) || FieldUtil.isRow(fieldToSend)) {
                fieldToSend.name = `fields-${index}`;
            }

            if (fieldToSend['dataType'] === '') {
                delete fieldToSend['dataType'];
            }

            if (!fieldToSend.id) {
                return this.coreWebService
                    .requestView({
                        body: fieldToSend,
                        method: RequestMethod.Post,
                        url: `v1/contenttype/${contentTypeId}/fields`
                    })
                    .pluck('entity');
            } else {
                return this.coreWebService
                    .requestView({
                        body: fieldToSend,
                        method: RequestMethod.Put,
                        url: `v1/contenttype/${contentTypeId}/fields/id/${fieldToSend.id}`
                    })
                    .pluck('entity');
            }
            return Observable.of(fieldToSend);
        });

        return Observable.forkJoin(observables);
    }
}
