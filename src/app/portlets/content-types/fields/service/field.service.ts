import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Observable';
import { Field, FieldType, FieldRow, TAB_DIVIDER, LINE_DIVIDER } from '../common';
import { CoreWebService } from '../../../../api/services/core-web-service';
import { RequestMethod } from '@angular/http';

/**
 * Provide method to handle with the Field Types
 */
@Injectable()
export class FieldService {

    constructor(private coreWebService: CoreWebService) {

    }

    loadFieldTypes(): Observable<FieldType[]> {
        return this.coreWebService.requestView({
            method: RequestMethod.Get,
            url: '/v1/fieldTypes'
        }).pluck('entity');
    }

    /**
     * Save fields.
     * @param {string} contentTypeId Content Type'id
     * @param {Field[]} fields fields to add
     * @returns {Observable<any>}
     * @memberof FieldService
     */
    saveFields(contentTypeId: string, fields: Field[]): Observable<any> {

        let observables: Observable<any>[] = fields.map((field, index) => {
            let fieldToSend = Object.assign({}, field, {
                'contentTypeId': contentTypeId,
                'sortOrder': index + 1
            });

            if (fieldToSend.clazz === TAB_DIVIDER.clazz || fieldToSend.clazz === LINE_DIVIDER.clazz) {
                fieldToSend.name = `fields-${index}`;
            }

            if (!fieldToSend.id) {
                return this.coreWebService.requestView({
                    body: fieldToSend,
                    method: RequestMethod.Post,
                    url: `v1/contenttype/${contentTypeId}/fields`
                }).pluck('entity');
            } else {
                return this.coreWebService.requestView({
                    body: fieldToSend,
                    method: RequestMethod.Put,
                    url: `v1/contenttype/${contentTypeId}/fields/id/${fieldToSend.id}`
                }).pluck('entity');
            }
        });

        return Observable.forkJoin(observables);
    }
}