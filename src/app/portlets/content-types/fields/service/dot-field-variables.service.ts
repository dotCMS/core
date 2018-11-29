import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CoreWebService } from 'dotcms-js';
import { RequestMethod } from '@angular/http';
import { pluck } from 'rxjs/operators';
import { DotFieldVariable } from '../shared/dot-field-variable.interface';

export interface DotFieldVariableParams {
    contentTypeId: string;
    fieldId: string;
    variable?: DotFieldVariable;
}

/**
 * Provide method to handle with the Field Variables
 */
@Injectable()
export class DotFieldVariablesService {
    constructor(private coreWebService: CoreWebService) {}

    /**
     * Load Field Variables.
     * @param {FieldVariableParams} params Variable params to get id of variables to be listed
     * @returns {Observable<DotFieldVariable[]>}
     * @memberof FieldVariablesService
     */
    load(params: DotFieldVariableParams): Observable<DotFieldVariable[]> {
        return this.coreWebService
            .requestView({
                method: RequestMethod.Get,
                url: `v1/contenttype/${params.contentTypeId}/fields/id/${params.fieldId}/variables`
            })
            .pipe(pluck('entity'));
    }

    /**
     * Save Field Variables.
     * @param {FieldVariableParams} params Variable params to be saved
     * @returns {Observable<DotFieldVariable>}
     * @memberof FieldVariablesService
     */
    save(params: DotFieldVariableParams): Observable<DotFieldVariable> {
        return this.coreWebService
            .requestView({
                body: {
                    'key': params.variable.key,
                    'value': params.variable.value,
                    'clazz': 'com.dotcms.contenttype.model.field.FieldVariable',
                    'fieldId': params.fieldId
                },
                method: RequestMethod.Post,
                url: `v1/contenttype/${params.contentTypeId}/fields/id/${params.fieldId}/variables`
            })
            .pipe(pluck('entity'));
    }

    /**
     * Delete Field Variables.
     * @param {FieldVariableParams} params Variable params to be deleted
     * @returns {Observable<DotFieldVariable>}
     * @memberof FieldVariablesService
     */
    delete(params: DotFieldVariableParams): Observable<DotFieldVariable> {
        return this.coreWebService
            .requestView({
                method: RequestMethod.Delete,
                url: `v1/contenttype/${params.contentTypeId}/fields/id/${params.fieldId}/variables/id/${params.variable.id}`
            })
            .pipe(pluck('entity'));
    }

}
