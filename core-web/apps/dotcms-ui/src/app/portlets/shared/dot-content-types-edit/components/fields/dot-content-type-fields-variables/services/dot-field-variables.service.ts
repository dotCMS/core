import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

import { DotCMSContentTypeField, DotCMSResponse, DotFieldVariable } from '@dotcms/dotcms-models';

/**
 * Provide method to handle with the Field Variables
 */
@Injectable()
export class DotFieldVariablesService {
    private http = inject(HttpClient);

    /**
     * Load Field Variables.
     * @param {DotCMSContentTypeField} field field to get variables
     * @returns {Observable<DotFieldVariable[]>}
     * @memberof FieldVariablesService
     */
    load(field: DotCMSContentTypeField): Observable<DotFieldVariable[]> {
        return this.http
            .get<
                DotCMSResponse<DotFieldVariable[]>
            >(`/api/v1/contenttype/${field.contentTypeId}/fields/id/${field.id}/variables`)
            .pipe(map((response) => response.entity));
    }

    /**
     * Save Field Variables.
     *
     * @param {DotCMSContentTypeField} field field where the variable is added
     * @param {DotFieldVariable} variable variable to be save
     * @returns {Observable<DotFieldVariable>}
     * @memberof DotFieldVariablesService
     */
    save(field: DotCMSContentTypeField, variable: DotFieldVariable): Observable<DotFieldVariable> {
        return this.http
            .post<DotCMSResponse<DotFieldVariable>>(
                `/api/v1/contenttype/${field.contentTypeId}/fields/id/${field.id}/variables`,
                {
                    key: variable.key,
                    value: variable.value,
                    clazz: 'com.dotcms.contenttype.model.field.FieldVariable',
                    fieldId: field.id
                }
            )
            .pipe(map((response) => response.entity));
    }

    /**
     * Delete Field Variables.
     *
     * @param {DotCMSContentTypeField} field where the variable is removed
     * @param {DotFieldVariable} variable variable to delete
     * @returns {Observable<DotFieldVariable>}
     * @memberof DotFieldVariablesService
     */
    delete(
        field: DotCMSContentTypeField,
        variable: DotFieldVariable
    ): Observable<DotFieldVariable> {
        return this.http
            .delete<
                DotCMSResponse<DotFieldVariable>
            >(`/api/v1/contenttype/${field.contentTypeId}/fields/id/${field.id}/variables/id/${variable.id}`)
            .pipe(map((response) => response.entity));
    }
}
