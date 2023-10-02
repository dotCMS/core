import { ComponentStore } from '@ngrx/component-store';
import { Observable, of } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { catchError, switchMap, tap } from 'rxjs/operators';

import { DotGlobalMessageService } from '@components/_common/dot-global-message/dot-global-message.service';
import { DotContentTypeService, DotMessageService } from '@dotcms/data-access';
import { DotCMSContentType, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';

import { DotVariableContent, FilteredFieldTypes } from '../dot-add-variable.models';

export interface DotAddVariableState {
    variables: DotVariableContent[];
}

@Injectable()
export class DotAddVariableStore extends ComponentStore<DotAddVariableState> {
    constructor(
        private dotContentTypeService: DotContentTypeService,
        private dotGlobalMessageService: DotGlobalMessageService,
        private dotHttpErrorManagerService: DotHttpErrorManagerService,
        private dotMessage: DotMessageService
    ) {
        super({
            variables: []
        });
    }

    readonly vm$ = this.select(({ variables }) => {
        return {
            variables
        };
    });

    readonly updateVariables = this.updater<DotCMSContentTypeField[]>(
        (state: DotAddVariableState, variables: DotCMSContentTypeField[]) => {
            return {
                ...state,
                variables: variables.reduce(
                    (acc, field) => {
                        const { fieldType, name, variable, fieldTypeLabel } = field;

                        if ((Object.values(FilteredFieldTypes) as string[]).includes(fieldType)) {
                            return acc;
                        }

                        acc.push(
                            // This try to find the extra fields for the field type, if it doesn't exist it will use the default one
                            ...(this.extraFields[fieldType]?.(field) ?? [
                                {
                                    name,
                                    variable,
                                    fieldTypeLabel,
                                    codeTemplate: this.getCodeTemplate.default(variable)
                                }
                            ])
                        );

                        return acc;
                    },
                    [
                        // We initialize the array with the Content Identifier field
                        {
                            name: this.dotMessage.get('Content-Identifier-value'),
                            variable: 'ContentIdentifier',
                            fieldTypeLabel: this.dotMessage.get('Content-Identifier'),
                            codeTemplate: this.getCodeTemplate.default('ContentIdentifier')
                        }
                    ] as DotVariableContent[]
                )
            };
        }
    );

    readonly getVariables = this.effect((origin$: Observable<string>) => {
        return origin$.pipe(
            switchMap((containerVariable) => {
                return this.dotContentTypeService.getContentType(containerVariable);
            }),
            tap((contentType: DotCMSContentType) => {
                this.updateVariables(contentType.fields);
            }),
            catchError((err: HttpErrorResponse) => {
                this.dotGlobalMessageService.error(err.statusText);
                this.dotHttpErrorManagerService.handle(err);

                return of(null);
            })
        );
    });

    // You can add here a new fieldType and add the fields that is has
    private readonly extraFields: Record<
        string,
        (variableContent: DotVariableContent) => DotVariableContent[]
    > = {
        Image: ({ variable, name, fieldTypeLabel }) => [
            {
                name: `${name}: ${this.dotMessage.get('Image-Identifier')}`,
                variable: `${variable}ImageIdentifier`,
                codeTemplate: this.getCodeTemplate.default(`${variable}ImageIdentifier)`),
                fieldTypeLabel
            },
            {
                name: `${name}: ${this.dotMessage.get('Image')}`,
                variable: `${variable}Image`,
                codeTemplate: this.getCodeTemplate.Image(variable),
                fieldTypeLabel
            },
            {
                name: `${name}: ${this.dotMessage.get('Image-Title')}`,
                variable: `${variable}ImageTitle`,
                codeTemplate: this.getCodeTemplate.default(`${variable}ImageTitle)`),
                fieldTypeLabel
            },
            {
                name: `${name}: ${this.dotMessage.get('Image-Extension')}`,
                variable: `${variable}ImageExtension`,
                codeTemplate: this.getCodeTemplate.default(`${variable}ImageExtension)`),
                fieldTypeLabel
            },
            {
                name: `${name}: ${this.dotMessage.get('Image-Width')}`,
                variable: `${variable}ImageWidth`,
                codeTemplate: this.getCodeTemplate.default(`${variable}ImageWidth)`),
                fieldTypeLabel
            },
            {
                name: `${name}: ${this.dotMessage.get('Image-Height')}`,
                variable: `${variable}ImageHeight`,
                codeTemplate: this.getCodeTemplate.default(`${variable}ImageHeight)`),
                fieldTypeLabel
            }
        ]
    };

    // You can add here a new variable and add the custom code that it has
    private readonly getCodeTemplate: Record<string, (variable: string) => string> = {
        Image: (variable) =>
            `#if ($UtilMethods.isSet(\${${variable}ImageURI}))\n    <img src="$!{dotContentMap.${variable}ImageURI}" alt="$!{dotContentMap.${variable}ImageTitle}" />\n#end`,
        default: (variable) => `$!{dotContentMap.${variable}}`
    };
}
