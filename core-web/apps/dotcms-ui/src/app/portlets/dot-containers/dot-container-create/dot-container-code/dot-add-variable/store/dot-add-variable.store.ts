import { ComponentStore } from '@ngrx/component-store';
import { Observable, of } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { catchError, switchMap, tap } from 'rxjs/operators';

import {
    DotContentTypeService,
    DotHttpErrorManagerService,
    DotGlobalMessageService
} from '@dotcms/data-access';
import { DotCMSContentType, DotCMSContentTypeField } from '@dotcms/dotcms-models';

import { DotFieldContent, FilteredFieldTypes } from '../dot-add-variable.models';
import { DotFieldsService } from '../services/dot-fields.service';

export interface DotAddVariableState {
    fields: DotFieldContent[];
}

@Injectable()
export class DotAddVariableStore extends ComponentStore<DotAddVariableState> {
    private dotContentTypeService = inject(DotContentTypeService);
    private dotGlobalMessageService = inject(DotGlobalMessageService);
    private dotHttpErrorManagerService = inject(DotHttpErrorManagerService);
    private dotFieldsService = inject(DotFieldsService);

    constructor() {
        super({
            fields: []
        });
    }

    readonly vm$ = this.select(({ fields }) => {
        return {
            fields
        };
    });

    readonly updateFields = this.updater<DotCMSContentTypeField[]>(
        (state: DotAddVariableState, fields: DotCMSContentTypeField[]) => {
            return {
                ...state,
                fields: fields.reduce(this.reduceFields, [
                    // We initialize the array with the Content Identifier field
                    this.dotFieldsService.contentIdentifierField
                ])
            };
        }
    );

    readonly getFields = this.effect((origin$: Observable<string>) => {
        return origin$.pipe(
            switchMap((containerVariable) => {
                return this.dotContentTypeService.getContentType(containerVariable);
            }),
            tap((contentType: DotCMSContentType) => {
                this.updateFields(contentType.fields);
            }),
            catchError((err: HttpErrorResponse) => {
                this.dotGlobalMessageService.error(err.statusText);
                this.dotHttpErrorManagerService.handle(err);

                return of(null);
            })
        );
    });

    /**
     * This function will reduce the fields and return the new array of fields
     *
     * @private
     * @param {DotFieldContent[]} fields
     * @param {DotCMSContentTypeField} currentField
     * @memberof DotAddVariableStore
     */
    private reduceFields = (fields: DotFieldContent[], currentField: DotCMSContentTypeField) => {
        const { fieldType } = currentField;

        // If you want to filter a new field type, add it to the FilteredFieldTypes enum
        if ((Object.values(FilteredFieldTypes) as string[]).includes(fieldType)) {
            return fields;
        }

        fields.push(
            // This will try to find the fields by field type, if it doesn't exist it will use the default one
            ...(this.dotFieldsService.fields[fieldType]?.(currentField) ??
                this.dotFieldsService.fields.default(currentField))
        );

        return fields;
    };
}
