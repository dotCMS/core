import { Observable } from 'rxjs';

import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

import { CoreWebService } from '@dotcms/dotcms-js';
import { DotCMSContentTypeField, DotFieldVariable } from '@dotcms/dotcms-models';

/**
 * Provide method to handle with the Field Variables
 */
@Injectable()
export class DotFieldVariablesService {
    private coreWebService = inject(CoreWebService);

    /**
     * Load Field Variables.
     * @param {DotCMSContentTypeField} field field to get variables
     * @returns {Observable<DotFieldVariable[]>}
     * @memberof FieldVariablesService
     */
    load(field: DotCMSContentTypeField): Observable<DotFieldVariable[]> {
        return this.coreWebService
            .requestView({
                url: `v1/contenttype/${field.contentTypeId}/fields/id/${field.id}/variables`
            })
            .pipe(map((x) => x?.entity));
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
        return this.coreWebService
            .requestView({
                body: {
                    key: variable.key,
                    value: variable.value,
                    clazz: 'com.dotcms.contenttype.model.field.FieldVariable',
                    fieldId: field.id
                },
                method: 'POST',
                url: `v1/contenttype/${field.contentTypeId}/fields/id/${field.id}/variables`
            })
            .pipe(map((x) => x?.entity));
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
        return this.coreWebService
            .requestView({
                method: 'DELETE',
                url: `v1/contenttype/${field.contentTypeId}/fields/id/${field.id}/variables/id/${variable.id}`
            })
            .pipe(map((x) => x?.entity));
    }
}
